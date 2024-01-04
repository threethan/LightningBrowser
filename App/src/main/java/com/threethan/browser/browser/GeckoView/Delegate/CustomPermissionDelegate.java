package com.threethan.browser.browser.GeckoView.Delegate;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

public class CustomPermissionDelegate implements GeckoSession.PermissionDelegate {
    public int androidPermissionRequestCode = 1;
    public Activity mActivity;

    public CustomPermissionDelegate(Activity mActivity) {
        this.mActivity = mActivity;
    }


    @Override
    public void onAndroidPermissionsRequest(@NonNull final GeckoSession session, final String[] permissions,
                                            @NonNull final Callback callback) {
        // requestPermissions was introduced in API 23.
        mActivity.requestPermissions(permissions, androidPermissionRequestCode);
        callback.grant();
    }

    @Nullable
    @Override
    public GeckoResult<Integer> onContentPermissionRequest(@NonNull GeckoSession session, @NonNull ContentPermission perm) {
        return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
    }

    @Override
    public void onMediaPermissionRequest(@NonNull final GeckoSession session, @NonNull final String uri,
                                         final MediaSource[] video, final MediaSource[] audio,
                                         @NonNull final MediaCallback callback) {
        // If we don't have device permissions at this point, just automatically reject the request
        // as we will have already have requested device permissions before getting to this point
        // and if we've reached here and we don't have permissions then that means that the user
        // denied them.

        if ((audio != null
                && ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                || (video != null
                && ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            callback.reject();
            return;
        }

        if (video != null)
            for (MediaSource source : video)
                callback.grant(source, null);
        if (audio != null)
            for (MediaSource source : audio)
                callback.grant(null, source);
    }
}

