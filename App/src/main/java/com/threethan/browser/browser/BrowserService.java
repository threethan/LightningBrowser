package com.threethan.browser.browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.threethan.browser.R;
import com.threethan.browser.browser.GeckoView.BrowserWebView;
import com.threethan.browser.browser.GeckoView.Delegate.ExtensionPromptDelegate;
import com.threethan.browser.helper.Dialog;
import com.threethan.browser.lib.FileLib;
import com.threethan.browser.lib.StringLib;
import com.threethan.browser.wrapper.BoundActivity;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BrowserService extends Service {
    private final IBinder binder = new LocalBinder();
    private final static Map<String, BrowserWebView> webViewByTabId = new ConcurrentHashMap<>();
    private final static Map<String, Activity> activityByTabId = new ConcurrentHashMap<>();
    private final static Map<String, String> titleByTabId = new ConcurrentHashMap<>();
    private final static Map<String, String> urlByTabId = new ConcurrentHashMap<>();
    public final static Set<BoundActivity> watchingActivities = new HashSet<>();
    private static int currentTabIdIndex = 0;
    public static final String TAB_PREFIX = "$tab::";
    public final static String APK_DIR = "DownloadedAPKs/";

    // Arbitrary ID for the persistent notification
    private final static int NOTIFICATION_ID = 42;
    private static final ExtensionPromptDelegate extensionPromptDelegate = new ExtensionPromptDelegate();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public class LocalBinder extends Binder {
        public BrowserService getService() {
            return BrowserService.this;
        }
    }
    public static GeckoRuntime sRuntime;
    public static GeckoRuntime getRuntime() {
        return sRuntime;
    }

    private static final String EXTENSION_LOCATION = "resource://android/assets/internalwebext/";
    private static final String EXTENSION_ID = "fixes@internal.ext";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public static void ManageExtensions() {
        extensionPromptDelegate.showList();
    }

    // Javascript is important
    public BrowserWebView getWebView(BrowserActivity activity) {
        BrowserWebView webView;
        final String tabId = activity.tabId;
        if (hasWebView(tabId)) {
            webView = webViewByTabId.get(tabId);

            assert webView != null;
            LinearLayout parent = (LinearLayout) webView.getParent();
            if (parent != null) parent.removeView(webView);

            Activity owner = activityByTabId.get(tabId);
            if (owner != null && owner != activity) {
                owner.finish();
                activityByTabId.put(tabId, activity);
            }
            webView.updateActivity(activity);
        } else {
            if (BrowserService.sRuntime == null) initRuntime();

            webView = new BrowserWebView(activity, activity);
            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            webViewByTabId.put(tabId, webView);
            activityByTabId.put(tabId, activity);

            activity.findViewById(R.id.loading).setVisibility(View.VISIBLE);
            webView.loadUrl(activity.currentUrl);
        }
        updateStatus();

        return webView;
    }

    private void initRuntime() {
        // GeckoRuntime can only be initialized once per process
        GeckoRuntimeSettings.Builder set = new GeckoRuntimeSettings.Builder()
                .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                .consoleOutput(false)
                .loginAutofillEnabled(true)
                .extensionsProcessEnabled(true)
                .extensionsWebAPIEnabled(true)
                .aboutConfigEnabled(true);
        BrowserService.sRuntime = GeckoRuntime.create(this, set.build());
        // Custom Fixes
        sRuntime.getWebExtensionController()
                .ensureBuiltIn(EXTENSION_LOCATION, EXTENSION_ID)
                .accept(
                        extension -> Log.i("MessageDelegate", "Extension installed: " + extension),
                        e -> Log.e("MessageDelegate", "Error registering WebExtension", e)
                );
        // Install Prompts
        sRuntime.getWebExtensionController().setPromptDelegate(extensionPromptDelegate);
    }
    // Downloads
    public static Map<Long, String> downloadFilenameById = new ConcurrentHashMap<>();
    public static Map<Long, Activity> downloadActivityById = new ConcurrentHashMap<>();
    public BroadcastReceiver onDownloadComplete=new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            String filename = downloadFilenameById.get(id);
            downloadFilenameById.remove(id);

            if (filename == null) return;

            if (filename.endsWith(".apk")) {
                final File path = getApplicationContext().getExternalFilesDir(APK_DIR);
                final File file = new File(path, filename);

                if (Dialog.getActivityContext() == null) {
                    // If we can't show an alert, copy AND prompt install
                    copyToDownloads(file);
                    promptInstallApk(file);
                }
                AlertDialog dialog = Dialog.build(Dialog.getActivityContext(), R.layout.dialog_downloaded_apk);
                if (dialog == null) return;
                dialog.findViewById(R.id.install).setOnClickListener(v -> {
                    promptInstallApk(file);
                    dialog.dismiss();
                });
                dialog.findViewById(R.id.save).setOnClickListener(v -> {
                    copyToDownloads(file);
                    dialog.dismiss();
                });
                dialog.findViewById(R.id.delete).setOnClickListener(v -> {
                    final boolean ignored = file.delete();
                    dialog.dismiss();
                });
                ((TextView) dialog.findViewById(R.id.downloadMessage)).setText(getString(R.string.web_apk_prompt_message_pre, filename));
            } else {
                final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                final File file = new File(path, filename);

                // provider is already included in the imagepicker lib
                Uri fileURI = FileProvider.getUriForFile(getBaseContext(), getApplicationContext().getPackageName() + ".imagepicker.provider", file);

                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(fileURI, getContentResolver().getType(fileURI));
                openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivity(openIntent);
                } catch (ActivityNotFoundException ignored) {
                    Dialog.toast(getString(R.string.web_download_finished), filename, true);
                }
            }
        }
    };
    private void copyToDownloads(File file) {
        // Copy to downloads
        // Original file will be deleted next time the updater is called
        File dlPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dlFile = new File(dlPath, file.getName());
        FileLib.copy(file, dlFile);
    }
    private void promptInstallApk(File file) {
        if(!file.exists()) return;

        // provider is already included in the imagepicker lib
        Uri apkURI = FileProvider.getUriForFile(getBaseContext(), getApplicationContext().getPackageName() + ".imagepicker.provider", file);

        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(apkURI, "application/vnd.android.package-archive");
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

        startActivity(openIntent);
    }
    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(onDownloadComplete);
        } catch (Exception ignored) {}
        super.onDestroy();
    }
    public void removeActivity(BrowserActivity activity) {
        activityByTabId.remove(activity.tabId);
    }
    public boolean hasWebView(String tabId) {
        return webViewByTabId.containsKey(tabId);
    }
    public void killWebView(String tabId) {
        if (!hasWebView(tabId)) return;
        BrowserWebView webView = webViewByTabId.get(tabId);
        if (webView == null) return;
        webView.kill();
        webViewByTabId.remove(tabId);
        if (activityByTabId.get(tabId) != null) {
            Objects.requireNonNull(activityByTabId.get(tabId)).finish();
            activityByTabId.remove(tabId);
        }
        System.gc();
        updateStatus();
    }

    // Data for wrapper
    public List<String> listWebViews() {
        return new ArrayList<>(webViewByTabId.keySet());
    }
    public static String getTitle(String tabId) {
        if (titleByTabId.containsKey(tabId)) {
            if (Objects.requireNonNull(titleByTabId.get(tabId)).isEmpty())
                return new StringLib.ParititionedUrl(getUrl(tabId)).middle;
            else
                return titleByTabId.get(tabId);
        } else return "Untitled Tab";
    }
    public static String getUrl(String tabId) {
        if (urlByTabId.containsKey(tabId)) return urlByTabId.get(tabId);
        else return "---";
    }
    public static void putTitle(String tabId, String title) {
        titleByTabId.put(tabId, title);
        tabUpdate(tabId);
    }
    public static void putUrl(String tabId, String url) {
        urlByTabId.put(tabId, url);
        tabUpdate(tabId);
    }
    public static String getNewTabId() {
        currentTabIdIndex++;
        return TAB_PREFIX+currentTabIdIndex;
    }
    public static void tabUpdate(@Nullable String lastUpdatedTabId) {
        for (BoundActivity activity : watchingActivities) activity.onTabUpdate(lastUpdatedTabId);
    }
    public static void bookmarkUpdate() {
        for (BoundActivity activity : watchingActivities) activity.onBookmarkUpdate();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, getNotification());
        return super.onStartCommand(intent, flags, startId);
    }

    private static final int BIND_FLAGS = BIND_ABOVE_CLIENT | BIND_IMPORTANT;
    public static void bind(Activity activity, ServiceConnection connection, boolean needed){
        Intent intent = new Intent(activity, BrowserService.class);
        if (amRunning(activity)) bindTo(activity, intent, connection, BIND_FLAGS);
        else if (needed) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent);
                bindTo(activity, intent, connection, BIND_FLAGS);
            } else bindTo(activity, intent, connection, BIND_AUTO_CREATE | BIND_FLAGS);
        }
    }
    private static void bindTo(Activity activity, Intent intent, ServiceConnection connection,
                               int flags) {
        try {
            // Add additional priority, may fail on some devices
            activity.bindService(intent, connection, flags | 0x000010000 | 0x10000000);
        } catch (SecurityException ignored) {
            activity.bindService(intent, connection, flags);
        }
    }

    private static boolean amRunning(Activity activity) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BrowserService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void updateStatus() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification());

        if (webViewByTabId.size() < 1) stopSelf();
        tabUpdate(null);
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, BrowserActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);
        final int n = webViewByTabId.size();
        return new Notification.Builder(this)
                .setContentTitle( n == 0 ? getString(R.string.notification_title_n) :
                        (n == 1 ? getString(R.string.notification_title_s) :
                        getString(R.string.notification_title_p, n) ))
                .setContentText(getText(R.string.notification_content))
                .setSmallIcon(R.drawable.ic_app_icon)
                .setContentIntent(pendingIntent)
                .build();
    }
}