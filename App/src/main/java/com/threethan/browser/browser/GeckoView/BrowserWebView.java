package com.threethan.browser.browser.GeckoView;

import android.annotation.SuppressLint;
import android.content.Context;

import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.browser.BrowserService;
import com.threethan.browser.browser.GeckoView.Delegate.CustomContentDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomHistoryDelgate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomNavigationDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomPermissionDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomProgressDelegate;
import com.threethan.browser.browser.GeckoView.Delegate.CustomPromptDelegate;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.util.Objects;

/*
    BrowserWebView

    A customized version of GeckoView which keeps media playing in the background.
 */
@SuppressLint("ViewConstructor")
public class BrowserWebView extends ScrollHandlingGeckoView {
    // Delegates
    private final CustomNavigationDelegate navigationDelegate;
    private final CustomHistoryDelgate historyDelegate;
    private final CustomProgressDelegate progressDelegate;
    private final CustomPromptDelegate promptDelegate;
    private final CustomContentDelegate contentDelegate;
    private final CustomPermissionDelegate permissionDelegate;
    private final CustomMediaSessionDelegate mediaSessionDelegate;

    // Functions
    public void goBack() {
        if (getSession() == null) return;
        getSession().goBack();
    }
    public void goForward() {
        if (getSession() == null) return;
        getSession().goForward();
    }

    public boolean canGoBack() {
        return navigationDelegate.canGoBack;
    }
    public boolean canGoForward() {
        return navigationDelegate.canGoForward;
    }
    public boolean clearQueued = false;
    public void backFull() {
        if (getSession() == null) return;
        getSession().gotoHistoryIndex(0);
    }
    public void forwardFull() {
        if (getSession() == null) return;
        getSession().gotoHistoryIndex(historyDelegate.historyList.size()-1);
    }
    public String getUrl() {
        return navigationDelegate.currentUrl;
    }

    public void loadUrl(String url) {
        if (getSession() == null) return;
        getSession().load(new GeckoSession.Loader().uri(url).flags(GeckoSession.LOAD_FLAGS_BYPASS_CACHE | GeckoSession.LOAD_FLAGS_FORCE_ALLOW_DATA_URI | GeckoSession.LOAD_FLAGS_BYPASS_CACHE | GeckoSession.LOAD_FLAGS_ALLOW_POPUPS));
    }
    public void reload() {
        if (getSession() == null) return;
        getSession().reload();
    }
    public void kill() {
        Objects.requireNonNull(getSession()).close();
        releaseSession();
    }

    // Startups

    public BrowserWebView(Context context, BrowserActivity mActivity) {
        super(context);

        GeckoSession session = new GeckoSession();
        session.open(BrowserService.getRuntime());

        GeckoSessionSettings sessionSettings = session.getSettings();
        sessionSettings.setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP);
        sessionSettings.setUseTrackingProtection(true);

        session.setPriorityHint(GeckoSession.PRIORITY_HIGH);
        historyDelegate = new CustomHistoryDelgate(mActivity);
        progressDelegate = new CustomProgressDelegate(mActivity);
        promptDelegate = new CustomPromptDelegate(mActivity);
        contentDelegate = new CustomContentDelegate(mActivity);
        permissionDelegate = new CustomPermissionDelegate(mActivity);
        mediaSessionDelegate = new CustomMediaSessionDelegate(mActivity);

        navigationDelegate = new CustomNavigationDelegate(mActivity);

        session.setHistoryDelegate(historyDelegate);
        session.setPermissionDelegate(permissionDelegate);
        session.setProgressDelegate(progressDelegate);
        session.setContentDelegate(contentDelegate);
        session.setPromptDelegate(promptDelegate);
        session.setNavigationDelegate(navigationDelegate);
        session.setMediaSessionDelegate(mediaSessionDelegate);
        setSession(session);
        Objects.requireNonNull(mSession).getCompositorController().setClearColor(0xFF2A2A2E);
        coverUntilFirstPaint(0xFF2A2A2E);
    }
    
    public void updateActivity(BrowserActivity mActivity) {
        navigationDelegate.mActivity = mActivity;
        progressDelegate.mActivity = mActivity;
        promptDelegate.mActivity = mActivity;
        contentDelegate.mActivity = mActivity;
        permissionDelegate.mActivity = mActivity;
        mediaSessionDelegate.mActivity = mActivity;
    }

    public void setActive(boolean active) {
        if (getSession() != null) getSession().setActive(active);
    }
}

