/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Reimplemented without AndroidX dependencies by github.com/threethan
 */

package com.threethan.browser.helper;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Build;
import android.service.media.MediaBrowserService;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;

import android.media.session.PlaybackState;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A media button receiver receives and helps translate hardware media playback buttons, such as
 * those found on wired and wireless headsets, into the appropriate callbacks in your app.
 *
 * <p>You can add this MediaButtonReceiver to your app by adding it directly to your
 * AndroidManifest.xml:
 *
 * <pre>
 * &lt;receiver android:name="androidx.media.session.MediaButtonReceiver" &gt;
 *   &lt;intent-filter&gt;
 *     &lt;action android:name="android.intent.action.MEDIA_BUTTON" /&gt;
 *   &lt;/intent-filter&gt;
 * &lt;/receiver&gt;
 * </pre>
 *
 * This class assumes you have a {@link Service} in your app that controls media playback via a
 * {@link MediaSession}. Once a key event is received by MediaButtonReceiver, this class tries
 * to find a {@link Service} that can handle {@link Intent#ACTION_MEDIA_BUTTON}, and a {@link
 * MediaBrowserService} in turn. If an appropriate service is found, this class forwards the
 * key event to the service. If neither is available or more than one valid service/media browser
 * service is found, an {@link IllegalStateException} will be thrown. Thus, your app should have one
 * of the following services to get a key event properly.
 *
 * <p>
 *
 * <h4>Service Handling ACTION_MEDIA_BUTTON</h4>
 *
 * A service can receive a key event by including an intent filter that handles {@link
 * Intent#ACTION_MEDIA_BUTTON}:
 *
 * <pre>
 * &lt;service android:name="com.example.android.MediaPlaybackService" &gt;
 *   &lt;intent-filter&gt;
 *     &lt;action android:name="android.intent.action.MEDIA_BUTTON" /&gt;
 *   &lt;/intent-filter&gt;
 * &lt;/service&gt;
 * </pre>
 */
public class MediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())
                || !intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
            Log.d(TAG, "Ignore unsupported intent: " + intent);
            return;
        }
        ComponentName mediaButtonServiceComponentName =
                getServiceComponentByAction(context, Intent.ACTION_MEDIA_BUTTON);
        if (mediaButtonServiceComponentName != null) {
            intent.setComponent(mediaButtonServiceComponentName);
            try {
                ContextCompat.startForegroundService(context, intent);
            } catch (/* ForegroundServiceStartNotAllowedException */ IllegalStateException e) {
                if (Build.VERSION.SDK_INT >= 31
                        && Api31.instanceOfForegroundServiceStartNotAllowedException(e)) {
                    onForegroundServiceStartNotAllowedException(
                            Api31.castToForegroundServiceStartNotAllowedException(e));
                } else {
                    throw e;
                }
            }
            return;
        }
        ComponentName mediaBrowserServiceComponentName = getServiceComponentByAction(context,
                MediaBrowserService.SERVICE_INTERFACE);
        if (mediaBrowserServiceComponentName != null) {
            PendingResult pendingResult = goAsync();
            Context applicationContext = context.getApplicationContext();
            MediaButtonConnectionCallback connectionCallback =
                    new MediaButtonConnectionCallback(applicationContext, intent, pendingResult);
            MediaBrowser mediaBrowser = new MediaBrowser(applicationContext,
                    mediaBrowserServiceComponentName, connectionCallback, null);
            connectionCallback.setMediaBrowser(mediaBrowser);
            mediaBrowser.connect();
            return;
        }
        throw new IllegalStateException("Could not find any Service that handles "
                + Intent.ACTION_MEDIA_BUTTON + " or implements a media browser service.");
    }

    /**
     * This method is called when an exception is thrown when calling {@link
     * Context#startForegroundService(Intent)} as a result of receiving a media button event.
     *
     * <p>By default, this method only logs the exception and it can be safely overridden. Apps
     * that find that such a media button event has been legitimately sent, may choose to
     * override this method and take the opportunity to post a notification from where the user
     * journey can continue.
     *
     * <p>This exception can be thrown if a broadcast media button event is received and a media
     * service is found in the manifest that is registered to handle {@link
     * Intent#ACTION_MEDIA_BUTTON}. If this happens on API 31+ and the app is in the background then
     * an exception is thrown.
     *
     * <p>Normally, a media button intent should only be required to be sent by the system in case
     * of a Bluetooth media button event that wants to restart the app. However, in such a case the
     * app gets an exemption and is allowed to start the foreground service. In this case this
     * method will never be called.
     *
     * <p>In all other cases, apps should use a {@linkplain MediaBrowser media browser} to
     * bind to and start the service instead of broadcasting an intent.
     *
     * @param e      The exception thrown by the system and caught by this broadcast receiver.
     */
    @RequiresApi(31)
    protected void onForegroundServiceStartNotAllowedException(
            @NonNull ForegroundServiceStartNotAllowedException e) {
        Log.e(
                TAG,
                "caught exception when trying to start a foreground service from the "
                        + "background: " + e.getMessage());
    }

    private static class MediaButtonConnectionCallback extends
            MediaBrowser.ConnectionCallback {
        private final Context mContext;
        private final Intent mIntent;
        private final PendingResult mPendingResult;

        private MediaBrowser mMediaBrowser;

        MediaButtonConnectionCallback(Context context, Intent intent, PendingResult pendingResult) {
            mContext = context;
            mIntent = intent;
            mPendingResult = pendingResult;
        }

        void setMediaBrowser(MediaBrowser mediaBrowser) {
            mMediaBrowser = mediaBrowser;
        }
        @Override
        public void onConnected() {
            MediaController mediaController = new MediaController(mContext,
                    mMediaBrowser.getSessionToken());
            KeyEvent ke = mIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (ke != null) mediaController.dispatchMediaButtonEvent(ke);
            finish();
        }

        @Override
        public void onConnectionSuspended() {
            finish();
        }

        @Override
        public void onConnectionFailed() {
            finish();
        }

        private void finish() {
            mMediaBrowser.disconnect();
            mPendingResult.finish();
        }
    }

    /**
     * Creates a broadcast pending intent that will send a media button event. The {@code action}
     * will be translated to the appropriate {@link KeyEvent}, and it will be sent to the
     * registered media button receiver in the given context. The {@code action} should be one of
     * the following:
     * <ul>
     * <li>{@link PlaybackState#ACTION_PLAY}</li>
     * <li>{@link PlaybackState#ACTION_PAUSE}</li>
     * <li>{@link PlaybackState#ACTION_SKIP_TO_NEXT}</li>
     * <li>{@link PlaybackState#ACTION_SKIP_TO_PREVIOUS}</li>
     * <li>{@link PlaybackState#ACTION_STOP}</li>
     * <li>{@link PlaybackState#ACTION_FAST_FORWARD}</li>
     * <li>{@link PlaybackState#ACTION_REWIND}</li>
     * <li>{@link PlaybackState#ACTION_PLAY_PAUSE}</li>
     * </ul>
     *
     * @param context The context of the application.
     * @param action The action to be sent via the pending intent.
     * @return Created pending intent, or null if cannot find a unique registered media button
     *         receiver or if the {@code action} is unsupported/invalid.
     */
    public static PendingIntent buildMediaButtonPendingIntent(Context context, long action) {
        ComponentName mbrComponent = getMediaButtonReceiverComponent(context);
        if (mbrComponent == null) {
            Log.w(TAG, "A unique media button receiver could not be found in the given context, so "
                    + "couldn't build a pending intent.");
            return null;
        }
        return buildMediaButtonPendingIntent(context, mbrComponent, action);
    }

    /**
     * Creates a broadcast pending intent that will send a media button event. The {@code action}
     * will be translated to the appropriate {@link KeyEvent}, and sent to the provided media
     * button receiver via the pending intent. The {@code action} should be one of the following:
     * <ul>
     * <li>{@link PlaybackState#ACTION_PLAY}</li>
     * <li>{@link PlaybackState#ACTION_PAUSE}</li>
     * <li>{@link PlaybackState#ACTION_SKIP_TO_NEXT}</li>
     * <li>{@link PlaybackState#ACTION_SKIP_TO_PREVIOUS}</li>
     * <li>{@link PlaybackState#ACTION_STOP}</li>
     * <li>{@link PlaybackState#ACTION_FAST_FORWARD}</li>
     * <li>{@link PlaybackState#ACTION_REWIND}</li>
     * <li>{@link PlaybackState#ACTION_PLAY_PAUSE}</li>
     * </ul>
     *
     * @param context The context of the application.
     * @param mbrComponent The full component name of a media button receiver where you want to send
     *            this intent.
     * @param action The action to be sent via the pending intent.
     * @return Created pending intent, or null if the given component name is null or the
     *         {@code action} is unsupported/invalid.
     */
    public static PendingIntent buildMediaButtonPendingIntent(Context context,
                                                              ComponentName mbrComponent, long action) {
        if (mbrComponent == null) {
            Log.w(TAG, "The component name of media button receiver should be provided.");
            return null;
        }
        int keyCode = playbackStateToKeyCode(action);
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            Log.w(TAG,
                    "Cannot build a media button pending intent with the given action: " + action);
            return null;
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setComponent(mbrComponent);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(context, keyCode, intent,
                Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
    }

    // KeyEvent constants only available on API 11+
    private static final int KEYCODE_MEDIA_PAUSE = 127;
    private static final int KEYCODE_MEDIA_PLAY = 126;


    /**
     * Indicates this session supports the play/pause toggle command.
     */
    public static int playbackStateToKeyCode(long action) {
        if (action == PlaybackState.ACTION_PLAY) {
            return KEYCODE_MEDIA_PLAY;
        } else if (action == PlaybackState.ACTION_PAUSE) {
            return KEYCODE_MEDIA_PAUSE;
        } else if (action == PlaybackState.ACTION_SKIP_TO_NEXT) {
            return KeyEvent.KEYCODE_MEDIA_NEXT;
        } else if (action == PlaybackState.ACTION_SKIP_TO_PREVIOUS) {
            return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
        } else if (action == PlaybackState.ACTION_STOP) {
            return KeyEvent.KEYCODE_MEDIA_STOP;
        } else if (action == PlaybackState.ACTION_FAST_FORWARD) {
            return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
        } else if (action == PlaybackState.ACTION_REWIND) {
            return KeyEvent.KEYCODE_MEDIA_REWIND;
        } else if (action == PlaybackState.ACTION_PLAY_PAUSE) {
            return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        }
        return KeyEvent.KEYCODE_UNKNOWN;
    }
    /**
     */
    @RestrictTo(LIBRARY)
    public static ComponentName getMediaButtonReceiverComponent(Context context) {
        Intent queryIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        queryIntent.setPackage(context.getPackageName());
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryBroadcastReceivers(queryIntent, 0);
        if (resolveInfos.size() == 1) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            return new ComponentName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);
        } else if (resolveInfos.size() > 1) {
            Log.w(TAG, "More than one BroadcastReceiver that handles "
                    + Intent.ACTION_MEDIA_BUTTON + " was found, returning null.");
        }
        return null;
    }

    private static ComponentName getServiceComponentByAction(Context context, String action) {
        PackageManager pm = context.getPackageManager();
        Intent queryIntent = new Intent(action);
        queryIntent.setPackage(context.getPackageName());
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(queryIntent, 0 /* flags */);
        if (resolveInfos.size() == 1) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            return new ComponentName(resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
        } else if (resolveInfos.isEmpty()) {
            return null;
        } else {
            throw new IllegalStateException("Expected 1 service that handles " + action + ", found "
                    + resolveInfos.size());
        }
    }

    @RequiresApi(31)
    private static final class Api31 {
        /**
         * Returns true if the passed exception is a
         * {@link ForegroundServiceStartNotAllowedException}.
         */
        public static boolean instanceOfForegroundServiceStartNotAllowedException(
                IllegalStateException e) {
            return e instanceof ForegroundServiceStartNotAllowedException;
        }

        /**
         * Casts the {@link IllegalStateException} to a
         * {@link ForegroundServiceStartNotAllowedException} and throws an exception if the cast
         * fails.
         */
        public static ForegroundServiceStartNotAllowedException
        castToForegroundServiceStartNotAllowedException(IllegalStateException e) {
            return (ForegroundServiceStartNotAllowedException) e;
        }
    }
}
