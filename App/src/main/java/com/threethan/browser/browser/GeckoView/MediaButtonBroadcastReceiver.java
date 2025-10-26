package com.threethan.browser.browser.GeckoView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaButtonBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                if (CustomMediaSessionDelegate.getPlayingInstance() == null) return;
                CustomMediaSessionDelegate.getPlayingInstance().handleMediaButtonEvent(keyEvent);
            }
        }
    }
}
