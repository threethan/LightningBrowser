package com.threethan.browser.browser.GeckoView;

import androidx.annotation.NonNull;

import com.threethan.browser.browser.BrowserActivity;

import org.mozilla.geckoview.GeckoSession;

public class CustomMediaDelegate implements GeckoSession.MediaDelegate {
    public CustomMediaDelegate(BrowserActivity mActivity) {    }

    @Override
    public void onRecordingStatusChanged(@NonNull GeckoSession session, @NonNull RecordingDevice[] devices) {
        //TODO: Indicators?
        GeckoSession.MediaDelegate.super.onRecordingStatusChanged(session, devices);
    }
}
