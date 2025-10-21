package com.threethan.browser.browser.GeckoView;

import androidx.annotation.NonNull;

import com.threethan.browser.browser.BrowserActivity;

import org.mozilla.geckoview.GeckoSession;

public class CustomScrollDelegate implements GeckoSession.ScrollDelegate {
    private int lastScrollY = 0;
    private final BrowserActivity mActivity;

    public CustomScrollDelegate(BrowserActivity mActivity) {
        this.mActivity = mActivity;
    }

    @Override
    public void onScrollChanged(@NonNull GeckoSession session, int scrollX, int scrollY) {
        int deltaY = scrollY - lastScrollY;
        lastScrollY = scrollY;
        GeckoSession.ScrollDelegate.super.onScrollChanged(session, scrollX, scrollY);
        mActivity.handleScrollChanged(deltaY);
    }

    public void resetScroll() {
        lastScrollY = 0;
    }
}
