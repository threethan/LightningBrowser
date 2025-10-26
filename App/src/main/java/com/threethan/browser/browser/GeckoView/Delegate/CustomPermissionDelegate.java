package com.threethan.browser.browser.GeckoView.Delegate;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.browser.browser.BrowserActivity;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

import java.util.ArrayList;
import java.util.List;

public class CustomPermissionDelegate implements GeckoSession.PermissionDelegate {
    public BrowserActivity mActivity;

    public CustomPermissionDelegate(BrowserActivity mActivity) {
        this.mActivity = mActivity;
    }


    @Override
    public void onAndroidPermissionsRequest(@NonNull final GeckoSession session,
                                            final String[] permissions,
                                            @NonNull final Callback callback) {
        mActivity.requestPermissions(permissions, session, callback);
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

        List<String> neededPermissions = new ArrayList<>();
        if (video != null) neededPermissions.add(Manifest.permission.CAMERA);
        if (audio != null) neededPermissions.add(Manifest.permission.RECORD_AUDIO);

        mActivity.requestPermissions(neededPermissions.toArray(new String[0]), session,
                new GeckoSession.PermissionDelegate.Callback() {
            @Override
            public void grant() {
                if (video != null)
                    for (MediaSource source : video)
                        callback.grant(source, null);
                if (audio != null)
                    for (MediaSource source : audio)
                        callback.grant(null, source);
            }

            @Override
            public void reject() {
                callback.reject();
            }
        });
    }
}

