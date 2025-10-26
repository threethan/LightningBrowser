package com.threethan.browser.browser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.threethan.browser.R;
import com.threethan.browser.browser.GeckoView.BrowserWebView;
import com.threethan.browser.browser.GeckoView.Delegate.CustomNavigationDelegate;
import com.threethan.browser.helper.BookmarkManager;
import com.threethan.browser.helper.CustomDialog;
import com.threethan.browser.helper.Dialog;
import com.threethan.browser.helper.FaviconLoader;
import com.threethan.browser.helper.Keyboard;
import com.threethan.browser.helper.PermissionManager;
import com.threethan.browser.helper.TabManager;
import com.threethan.browser.lib.StringLib;
import com.threethan.browser.updater.BrowserUpdater;
import com.threethan.browser.wrapper.BoundActivity;
import com.threethan.browser.wrapper.EditTextWatched;

import org.mozilla.geckoview.GeckoSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BrowserActivity extends BoundActivity {
    private BrowserWebView w;
    TextView urlPre;
    TextView urlMid;
    TextView urlEnd;
    ImageView favicon;
    public String tabId = null;
    View back;
    View forward;
    View background;
    ProgressBar loading;
    View bookmarkAdd;
    View bookmarkRem;
    View permissionButton;
    protected final BookmarkManager bookmarkManager = new BookmarkManager(this);
    private boolean isEphemeral;
    private boolean isTab;
    private boolean isTopBarForciblyHidden;
    private final String DEFAULT_URL = "https://www.google.com/";
    private final PermissionManager permissionManager = new PermissionManager(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bind();

        Log.v("Lightning Browser", "Starting Browser Activity");

        setContentView(R.layout.activity_browser);
        getWindow().setStatusBarColor(Color.parseColor("#11181f"));

        background = findViewById(R.id.container);
        loading = findViewById(R.id.loading);

        urlPre = findViewById(R.id.urlPre);
        urlMid = findViewById(R.id.urlMid);
        urlEnd = findViewById(R.id.urlEnd);

        favicon = findViewById(R.id.favicon);
        favicon.setClipToOutline(true);

        Uri uri = getIntent().getData();
        if (uri != null && !uri.toString().isEmpty()) currentUrl = uri.toString();
        else currentUrl = DEFAULT_URL;

        isTab = getIntent().getBooleanExtra("isTab", false);

        if (getIntent().getExtras() != null) {
            isEphemeral = getIntent().getBooleanExtra("isEphemeral", false);
            if (isTab) {
                if (currentUrl.startsWith(BrowserService.TAB_PREFIX)) {
                    tabId = currentUrl;
                    currentUrl = BrowserService.getUrl(tabId);
                } else tabId = BrowserService.getNewTabId();
            }
        }
        if (tabId == null) tabId = BrowserService.TAB_PREFIX + "ext::" + currentUrl;
        Log.v("Lightning Browser", "... with url " + currentUrl + (isTab ? ", is a tab" : ", not a tab") + ", assigned id " + tabId);

        if (!isTab) {
            isTopBarForciblyHidden = true;
            hideTopBar();
        }

        // Back/Forward Buttons
        back = findViewById(R.id.back);
        forward = findViewById(R.id.forward);

        back.setOnClickListener((view) -> {
            if (w == null) return;
            if (w.canGoBack()) w.goBack();
            updateButtonsAndUrl();
        });
        forward.setOnClickListener((view) -> {
            if (w == null) return;
            if (w.canGoForward()) w.goForward();
            updateButtonsAndUrl();
        });

        back.setOnLongClickListener((view -> {
            if (w == null) return false;
            w.forwardFull();
            updateButtonsAndUrl();
            return true;
        }));
        forward.setOnLongClickListener((view -> {
            if (w == null) return false;
            w.backFull();
            updateButtonsAndUrl();
            return true;
        }));

        // Refresh Button
        View refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener((view) -> reload());
        refresh.setOnLongClickListener((view) -> {
            w.loadUrl(currentUrl);
            w.clearQueued = true;
            updateButtonsAndUrl(currentUrl);
            return true;
        });

        View exit = findViewById(R.id.exit);
        exit.setOnClickListener((View) -> finish());

        // Bookmark Button
        bookmarkAdd = findViewById(R.id.addBookmark);
        bookmarkRem = findViewById(R.id.removeBookmark);
        bookmarkAdd.setOnClickListener((view) -> {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.VISIBLE);
            bookmarkManager.addBookmark(currentUrl, BrowserService.getTitle(tabId));
        });
        bookmarkRem.setOnClickListener((view) -> {
            bookmarkRem.setVisibility(View.GONE);
            bookmarkAdd.setVisibility(View.VISIBLE);
            bookmarkManager.removeBookmark(currentUrl);
        });

        // Permission Button
        permissionButton = findViewById(R.id.permissionButton);
        permissionButton.setOnClickListener((view) -> {
            String origin = "";
            if (w.getSession() != null
                    && w.getSession().getNavigationDelegate()
                    instanceof CustomNavigationDelegate navDelegate) {
                origin = navDelegate.getOrigin();
            }

            String[] permissions = permissionManager.getPermissionsForOrigin(origin);

            String finalOrigin = origin;
            new CustomDialog.Builder(this)
                    .setTitle(R.string.permission_manage)
                    .setMessage(getString(R.string.permission_manage_message, origin,
                            StringLib.buildPermissionNamesList(permissions, this)))
                    .setPositiveButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .setNegativeButton(R.string.permission_revoke, (dialog, which) -> {
                        for (String permission : PermissionManager.KNOWN_PERMISSIONS) {
                            permissionManager.setPermission(finalOrigin, permission, false);
                        }
                        permissionButton.setVisibility(View.GONE);
                        // Refresh page to apply permission changes
                        reload();
                        dialog.dismiss();
                    })
                    .show();
        });


        // Edit URL
        View urlLayout = findViewById(R.id.urlLayout);
        EditTextWatched urlEdit = findViewById(R.id.urlEdit);
        View clearUrl = findViewById(R.id.clear);
        View confirm = findViewById(R.id.confirm);
        urlEdit.setOnEdited((string) -> {
            clearUrl.setVisibility(string.isEmpty() ? View.GONE : View.VISIBLE);
            confirm.setVisibility(string.isEmpty() ? View.GONE : View.VISIBLE);
        });
        clearUrl.setOnClickListener((view) -> urlEdit.setText(""));

        View topBar = findViewById(R.id.topBar);
        View topBarEdit = findViewById(R.id.topBarEdit);
        urlLayout.setOnClickListener((view) -> {
            topBar.setVisibility(View.GONE);
            topBarEdit.setVisibility(View.VISIBLE);
            urlEdit.setText(currentUrl);
            urlEdit.post(urlEdit::requestFocus);
            urlEdit.postDelayed(() -> Keyboard.show(this), 100);
        });
        urlEdit.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on Enter key press
                topBarEdit.findViewById(R.id.confirm).callOnClick();
                return true;
            }
            return false;
        });
        confirm.setOnClickListener((view) -> {
            String nUrl = urlEdit.getText().toString();

            nUrl = StringLib.toValidUrl(nUrl);

            w.loadUrl(nUrl);
            updateButtonsAndUrl(nUrl);
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
            Keyboard.hide(this, topBar);
        });
        topBarEdit.findViewById(R.id.cancel).setOnClickListener((view) -> {
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
            Keyboard.hide(this, topBar);
        });

        findViewById(R.id.extensions).setOnClickListener(v -> BrowserService.ManageExtensions());

        new BrowserUpdater(this).checkAppUpdateInteractive();
    }

    private void updateButtonsAndUrl() {
        updateButtonsAndUrl(w.getUrl());
    }

    public void updateButtonsAndUrl(String url) {
        if (w == null) return;
        updateUrl(url);
    }

    private void reload() {
        w.reload();
    }

    public void startLoading() {
        loading.setVisibility(View.VISIBLE);
        loading.setIndeterminate(true);
        back.setAlpha(0.5f);
        forward.setAlpha(0.5f);
    }


    public void setLoadingProgress(int progress) {
        loading.setVisibility(View.VISIBLE);
        if (progress <= 0 || progress >= 100) {
            loading.setIndeterminate(true);
        } else {
            loading.setIndeterminate(false);
            loading.setProgress(progress);
        }
    }

    public void stopLoading() {
        loading.setIndeterminate(true);
        loading.setVisibility(View.GONE);
        // Update navigation
        back.setAlpha(1f);
        forward.setAlpha(1f);
        back.setVisibility(w.canGoBack() && !w.clearQueued ? View.VISIBLE : View.GONE);
        forward.setVisibility(w.canGoForward() && !w.clearQueued ? View.VISIBLE : View.GONE);
    }

    protected void setGeckoViewTop(int top) {
        if (w != null) {
            if (w.getLayoutParams() instanceof FrameLayout.LayoutParams flp) {
                flp.topMargin = top;
                flp.bottomMargin = top > 0 ? (-getTopLayoutHeight() + top) : 0;
                w.setLayoutParams(flp);
            }
        } else {
            new Handler().postDelayed(() -> setGeckoViewTop(top), 100);
        }
    }

    public void showTopBar() {
        if (isTopBarForciblyHidden) return;
        findViewById(R.id.topBar).setVisibility(View.VISIBLE);
        findViewById(R.id.topBarEdit).setVisibility(View.GONE);

        setGeckoViewTop(getTopLayoutHeight());
    }

    public void hideTopBar() {
        findViewById(R.id.topBar).setVisibility(View.GONE);
        findViewById(R.id.topBarEdit).setVisibility(View.GONE);

        setGeckoViewTop(0);
    }

    public String currentUrl = "";

    // Splits the URL into parts and updates the URL display
    @SuppressLint("SetTextI18n") // It wants me to use a string resource to add a dot
    private void updateUrl(String url) {
        if (url == null) url = DEFAULT_URL;

        StringLib.ParititionedUrl pUrl = new StringLib.ParititionedUrl(url);

        urlPre.setText(pUrl.prefix);
        urlMid.setText(pUrl.middle);
        urlEnd.setText(pUrl.suffix);

        currentUrl = url;
        BrowserService.putUrl(tabId, url);

        if (bookmarkManager.getBookmarks().contains(currentUrl)) {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.VISIBLE);
        } else {
            bookmarkRem.setVisibility(View.GONE);
            bookmarkAdd.setVisibility(View.VISIBLE);
        }

        FaviconLoader.loadFavicon(this, currentUrl, favicon::setImageDrawable);
        permissionButton.setVisibility(permissionManager.hasPermissionsForOrigin(StringLib.getOrigin(url)) ? View.VISIBLE : View.GONE);
        startLoading();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (isTopBarForciblyHidden) {
            showTopBar();
        } else if (isFullScreen()) {
            fullScreenSession.exitFullScreen();
            setFullScreenSession(null);
        } else if (findViewById(R.id.topBarEdit).getVisibility() == View.VISIBLE)
            findViewById(R.id.cancel).callOnClick();
        else {
            if (w.canGoBack()) {
                w.goBack();
                updateButtonsAndUrl();
            } else finish();
        }
    }

    @Override
    protected void
    onDestroy() {
        if (wService == null) return;
        if (isFinishing()) {
            // Don't keep search views in background
            if (isEphemeral) wService.killWebView(tabId);
            else {
                TabManager tabManager = new TabManager(this);

                if (tabManager.shouldUseSuspendedTabs(this)) {
                    String title = BrowserService.getTitle(tabId);
                    String url = BrowserService.getUrl(tabId);
                    wService.killWebView(tabId);
                    tabManager.addSuspendedTab(tabId, url, title);
                } else {
                    wService.setWebViewActive(tabId, false);
                }
            }
        }
        wService.removeActivity(this);
        super.onDestroy();
    }

    public void loadUrl(String url) {
        if (url == null) {
            Log.w("Lightning Browser", "Tried to load null URL");
            return;
        }
        w.loadUrl(url);
        updateButtonsAndUrl(url);
    }

    // Sets the WebView when the service is bound
    @Override
    protected void onBound() {
        assert wService != null;
        if (tabId == null) tabId = DEFAULT_URL;

        // Show conditional buttons
        View kill = findViewById(R.id.kill);
        if (isTab) {
            kill.setVisibility(View.GONE);
        } else {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.GONE);
            kill.setVisibility(View.VISIBLE);
            kill.setOnClickListener((view) -> wService.killWebView(tabId));
        }

        w = wService.getWebView(this);
        w.setOnScrollInterceptor(this::handleScrollChanged);
        CursorLayout container = findViewById(R.id.container);
        setGeckoViewTop(getTopLayoutHeight());
        container.addView(w);
        container.targetView = w;

        updateButtonsAndUrl();
    }

    @Override
    protected void onResume() {
        Dialog.setActivityContext(this);
        super.onResume();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) onBackPressed();
        return true;
    }

    private float accumulatedScrollY = 0;

    public boolean handleScrollChanged(int deltaX, int deltaY) {
        if (isTopBarForciblyHidden || isFullScreen()) return false;

        float density = getResources().getDisplayMetrics().density;


        float topLayoutHeight = getTopLayoutHeight();

        if (accumulatedScrollY > topLayoutHeight * 2) accumulatedScrollY = topLayoutHeight * 2;
        if (accumulatedScrollY < -topLayoutHeight) accumulatedScrollY = -topLayoutHeight;

        float prevTop = -accumulatedScrollY + topLayoutHeight;
        if (prevTop < 0) prevTop = 0;
        if (prevTop > topLayoutHeight) prevTop = topLayoutHeight;

        accumulatedScrollY += deltaY * density;

        float top = -accumulatedScrollY + topLayoutHeight;
        if (top < 0) top = 0;
        if (top > topLayoutHeight) top = topLayoutHeight;

        if (top != prevTop) {
            setGeckoViewTop((int) top);
            return true;
        } else {
            return false;
        }
    }


    private int getTopLayoutHeight() {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (42 * scale + 0.5f);
    }

    GeckoSession fullScreenSession = null;

    protected boolean isFullScreen() {
        return fullScreenSession != null;
    }

    public void setFullScreenSession(GeckoSession geckoSession) {
        this.fullScreenSession = geckoSession;
        if (isFullScreen()) hideTopBar();
        else showTopBar();
    }

    private GeckoSession.PermissionDelegate.Callback pendingPermissionCallback = null;
    private String pendingPermissionOrigin = null;

    public void requestPermissions(String[] permissions, GeckoSession
            session, GeckoSession.PermissionDelegate.Callback callback) {

        if (Arrays.asList(permissions).contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Remove ACCESS_FINE_LOCATION as we only show COARSE_LOCATION to users
            permissions = Stream.of(permissions)
                    .filter(p -> !p.equals(Manifest.permission.ACCESS_FINE_LOCATION))
                    .toArray(String[]::new);
            if (permissions.length == 0) {
                // If no permissions left, just grant
                callback.grant();
                return;
            }
        }

        String origin = "";
        if (session.getNavigationDelegate() instanceof CustomNavigationDelegate navDelegate) {
            origin = navDelegate.getOrigin();
        }

        String[] androidPermissions = Stream.of(permissions)
                .filter(p -> p.startsWith("android.permission.")).toArray(String[]::new);
        boolean granted = true;
        for (String permission : androidPermissions) {
            if (!permissionManager.getPermission(origin, permission)
                    || checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }

        if (granted) {
            callback.grant();
            return;
        }

        String finalOrigin = origin;
        String[] finalPermissions = permissions;
        new CustomDialog.Builder(this)
                .setTitle(R.string.permission_manage)
                .setMessage(getString(R.string.permission_request_message, origin,
                        StringLib.buildPermissionNamesList(permissions, this)))
                .setPositiveButton(R.string.permission_allow, (dialog, which) -> {
                    for (String permission : androidPermissions) {
                        permissionManager.setPermission(finalOrigin, permission, true);
                        if (checkSelfPermission(permission)
                                != PackageManager.PERMISSION_GRANTED) {
                            pendingPermissionCallback = callback;
                            pendingPermissionOrigin = finalOrigin;
                            requestPermissions(finalPermissions, 1);
                            return;
                        }
                    }
                    permissionButton.setVisibility(View.VISIBLE);
                    callback.grant();
                })
                .setNegativeButton(R.string.permission_deny, (dialog, which)
                        -> callback.reject())
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            if (pendingPermissionCallback != null) {
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    for (String permission : permissions)
                        permissionManager.setPermission(pendingPermissionOrigin, permission, true);
                    pendingPermissionCallback.grant();
                    permissionButton.setVisibility(View.VISIBLE);
                    if (wService != null) wService.restartForeground();
                } else {
                    pendingPermissionCallback.reject();
                }
                pendingPermissionCallback = null;
            }
        } catch (Exception e) {
            Log.e("BrowserActivity", "Error handling permission result", e);
        }
    }
}
