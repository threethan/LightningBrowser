package com.threethan.browser.browser.GeckoView.Delegate;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.browser.R;
import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.browser.BrowserService;
import com.threethan.browser.helper.Dialog;
import com.threethan.browser.lib.FileLib;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebResponse;

import java.io.File;

public class CustomContentDelegate implements GeckoSession.ContentDelegate {
    public BrowserActivity mActivity;

    public CustomContentDelegate(BrowserActivity mActivity) {
        this.mActivity = mActivity;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onExternalResponse(@NonNull GeckoSession session, @NonNull WebResponse response) {

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(response.uri));

        final String filename= URLUtil.guessFileName(response.uri,
                response.headers.get("Content-Disposition"),
                response.headers.get("Content-Type"));

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!

        if (filename.endsWith(".apk")) {
            final File apkFile = new File(mActivity.getExternalCacheDir()+"/"+BrowserService.APK_DIR, filename);
            FileLib.delete(apkFile);
            request.setDestinationUri(Uri.fromFile(apkFile));
        }
        else try {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        } catch (IllegalStateException ignored) {
            // If we can't access downloads dir
            request.setDestinationInExternalFilesDir(mActivity, Environment.DIRECTORY_DOWNLOADS, filename);
        }

        DownloadManager manager = (DownloadManager) mActivity.getSystemService(DOWNLOAD_SERVICE);

        if (mActivity.wService != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mActivity.registerReceiver(mActivity.wService.onDownloadComplete,
                        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                        Context.RECEIVER_EXPORTED);
            } else {
                mActivity.registerReceiver(mActivity.wService.onDownloadComplete,
                        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        }


        final long id = manager.enqueue(request);
        BrowserService.downloadFilenameById.put(id, filename);
        BrowserService.downloadActivityById.put(id, mActivity);

        Dialog.toast(mActivity.getString(R.string.web_download_started), filename, true);
        GeckoSession.ContentDelegate.super.onExternalResponse(session, response);
    }

    @Override
    public void onFullScreen(@NonNull GeckoSession session, boolean fullScreen) {
        GeckoSession.ContentDelegate.super.onFullScreen(session, fullScreen);
        mActivity.setFullScreenSession(fullScreen ? session : null);
    }

    @Override
    public void onTitleChange(@NonNull GeckoSession session, @Nullable String title) {
        BrowserService.putTitle(mActivity.tabId, title);
        GeckoSession.ContentDelegate.super.onTitleChange(session, title);
    }
}
