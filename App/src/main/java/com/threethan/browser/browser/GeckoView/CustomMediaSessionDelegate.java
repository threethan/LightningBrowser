package com.threethan.browser.browser.GeckoView;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.browser.R;
import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.browser.BrowserService;
import com.threethan.browser.helper.MediaButtonReceiver;

import org.mozilla.geckoview.GeckoSession;

import java.lang.ref.WeakReference;

public class CustomMediaSessionDelegate implements org.mozilla.geckoview.MediaSession.Delegate {
    private static final String CHANNEL_ID = "media_playback_channel";
    private static final int NOTIFICATION_ID = 1;

    public BrowserActivity mActivity;
    private MediaSession mediaSessionAndroid;
    private org.mozilla.geckoview.MediaSession mediaSessionGecko;
    private NotificationManager notificationManager;
    private boolean isPlaying = false;
    private static String lastTabId = null;

    public CustomMediaSessionDelegate(BrowserActivity mActivity) {
        this.mActivity = mActivity;
        setupMediaSessionAndroid();
        setupNotificationChannel();
    }
    public boolean isPlaying() {
        return isPlaying;
    }

    private static WeakReference<CustomMediaSessionDelegate> playingInstance;

    public static @Nullable CustomMediaSessionDelegate getPlayingInstance() {
        return playingInstance == null ? null : playingInstance.get();
    }

    public static @Nullable String getPlayingTabId() {
        if (playingInstance == null) return null;
        else return lastTabId;
    }

    private void setupMediaSessionAndroid() {
        mediaSessionAndroid = new MediaSession(mActivity, "BrowserMediaSession");
        mediaSessionAndroid.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSessionAndroid.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                mediaSessionGecko.play();
            }

            @Override
            public void onPause() {
                mediaSessionGecko.pause();
            }

            @Override
            public void onStop() {
                mediaSessionGecko.stop();
                lastTabId = null;
            }

            @Override
            public void onSkipToNext() {
                Log.i("MediaSession", "onSkipToNext called");
                mediaSessionGecko.seekForward();
            }

            @Override
            public void onSkipToPrevious() {
                Log.i("MediaSession", "onSkipToPrevious called");
                mediaSessionGecko.seekBackward();
            }
        });
    }

    private void setupNotificationChannel() {
        notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Media playback controls");
            notificationManager.createNotificationChannel(channel);
        }
    }

    /** @noinspection deprecation*/
    private void showNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(mActivity, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(mActivity);
        }

        Intent contentIntent = new Intent(mActivity, BrowserActivity.class);
        contentIntent.setAction(Intent.ACTION_VIEW);
        contentIntent.setData(Uri.parse(mActivity.tabId));
        contentIntent.putExtra("isTab", true);

        PendingIntent pendingContentIntent = PendingIntent.getActivity(
                mActivity,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder
                .setSmallIcon(R.drawable.ic_app_icon_noti)
                .setContentIntent(pendingContentIntent)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mActivity, PlaybackState.ACTION_STOP))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mediaSessionAndroid.getSessionToken())
                        .setShowActionsInCompactView(0, 1));

        if (isPlaying) {
            builder.addAction(R.drawable.ic_media_pause, "Pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                            mActivity, PlaybackState.ACTION_PAUSE));
        } else {
            builder.addAction(R.drawable.ic_media_play, "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                            mActivity, PlaybackState.ACTION_PLAY));
        }

        builder.addAction(R.drawable.ic_media_stop, "Stop",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mActivity, PlaybackState.ACTION_STOP));

        notificationManager.notify(NOTIFICATION_ID, builder.build());
        checkUpdateLastTabId();
    }

    private void checkUpdateLastTabId() {
        if (lastTabId == null || !lastTabId.equals(mActivity.tabId)) {
            if (lastTabId != null) BrowserService.tabUpdate(lastTabId);
            lastTabId = mActivity.tabId;
            BrowserService.tabUpdate(lastTabId);
        }
    }

    private void hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
        mediaSessionAndroid.setActive(false);
    }

    @Override
    public void onActivated(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {
        playingInstance = new WeakReference<>(this);

        this.mediaSessionAndroid.setActive(true);
        this.mediaSessionGecko = mediaSession;
        updatePlaybackState(PlaybackState.STATE_PLAYING);
    }

    @Override
    public void onDeactivated(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {
        hideNotification();
        if (lastTabId != null) BrowserService.tabUpdate(lastTabId);
        lastTabId = null;
    }

    @Override
    public void onMetadata(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession, @NonNull org.mozilla.geckoview.MediaSession.Metadata meta) {
        playingInstance = new WeakReference<>(this);

        Log.i("MediaSession", "onMetadata called: " + meta.title);
        org.mozilla.geckoview.MediaSession.Delegate.super.onMetadata(session, mediaSession, meta);

        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, meta.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, meta.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, meta.album);

        try {
            assert meta.artwork != null;
            new Thread(() -> {
                try {
                    Bitmap bitmap = meta.artwork.getBitmap(512).poll(2500);
                    Log.i("MediaSession", "Artwork bitmap retrieved: " + (bitmap != null));
                    if (bitmap != null) {
                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
                        this.mediaSessionAndroid.setMetadata(metadataBuilder.build());
                        showNotification();
                    }
                } catch (Throwable e) {
                    Log.w("MediaSession", "Error setting artwork", e);
                }
            }).start();
        } catch (Throwable e) {
            Log.w("MediaSession", "Error setting artwork", e);
        }

        this.mediaSessionAndroid.setMetadata(metadataBuilder.build());
        showNotification();
    }

    @Override
    public void onPlay(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {
        Log.i("MediaSession", "onPlay called");
        isPlaying = true;
        updatePlaybackState(PlaybackState.STATE_PLAYING);
        BrowserService.tabUpdate(lastTabId);
        showNotification();
    }

    @Override
    public void onPause(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {
        isPlaying = false;
        updatePlaybackState(PlaybackState.STATE_PAUSED);
        BrowserService.tabUpdate(lastTabId);
        showNotification();
    }

    @Override
    public void onStop(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {
        isPlaying = false;
        updatePlaybackState(PlaybackState.STATE_STOPPED);
        hideNotification();
    }

    private void updatePlaybackState(int state) {
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY |
                        PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_STOP)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f);

        mediaSessionAndroid.setPlaybackState(stateBuilder.build());
    }

    public void handleMediaButtonEvent(KeyEvent keyEvent) {
        mediaSessionAndroid.getController().dispatchMediaButtonEvent(keyEvent);
    }

    public void play() {
        PendingIntent intent = MediaButtonReceiver.buildMediaButtonPendingIntent(
                mActivity, PlaybackState.ACTION_PLAY);
        if (intent != null) try {
            intent.send();
        } catch (PendingIntent.CanceledException ignored) {}
    }
    public void pause() {
        PendingIntent intent = MediaButtonReceiver.buildMediaButtonPendingIntent(
                mActivity, PlaybackState.ACTION_PAUSE);
        if (intent != null) try {
            intent.send();
        } catch (PendingIntent.CanceledException ignored) {}
    }
}