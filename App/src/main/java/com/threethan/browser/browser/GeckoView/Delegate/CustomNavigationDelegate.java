package com.threethan.browser.browser.GeckoView.Delegate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.browser.GeckoView.CustomScrollDelegate;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;

import java.util.List;

public class CustomNavigationDelegate implements GeckoSession.NavigationDelegate {
    private final CustomScrollDelegate mScrollDelegate;
    public boolean canGoBack = false;
    public boolean canGoForward = false;
    public String currentUrl = "";

    public BrowserActivity mActivity;
    public CustomNavigationDelegate(BrowserActivity activity, CustomScrollDelegate scrollDelegate) {
        super();
        this.mActivity = activity;
        this.mScrollDelegate = scrollDelegate;
    }
    @Override
    public void onCanGoBack(@NonNull GeckoSession session, boolean canGoBack) {
        this.canGoBack = canGoBack;
    }
    @Override
    public void onCanGoForward(@NonNull GeckoSession session, boolean canGoForward) {
        this.canGoForward = canGoForward;
    }

    @Override
    public void onLocationChange(@NonNull GeckoSession session, @Nullable String url, @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms, @NonNull Boolean hasUserGesture) {
        GeckoSession.NavigationDelegate.super.onLocationChange(session, url, perms, hasUserGesture);

        if (url != null && !url.isEmpty() && !url.equals("about:blank")) {
            currentUrl = url;
            mActivity.updateButtonsAndUrl(currentUrl);
        }
    }

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
        // Spotify-specific patch
        if (request.uri.contains("open.spotify.com"))
            session.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        else
            session.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);

        this.currentUrl = request.uri;
        this.mActivity.updateButtonsAndUrl(currentUrl);

        mScrollDelegate.resetScroll();
        mActivity.showTopBar();

        return GeckoSession.NavigationDelegate.super.onLoadRequest(session, request);
    }
}