package com.threethan.browser.browser;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.threethan.browser.R;
import com.threethan.browser.browser.GeckoView.BrowserWebView;
import com.threethan.browser.helper.BookmarkManager;
import com.threethan.browser.helper.Dialog;
import com.threethan.browser.helper.FaviconLoader;
import com.threethan.browser.helper.Keyboard;
import com.threethan.browser.lib.StringLib;
import com.threethan.browser.updater.BrowserUpdater;
import com.threethan.browser.wrapper.BoundActivity;
import com.threethan.browser.wrapper.EditTextWatched;

import org.mozilla.geckoview.GeckoSession;

public class BrowserActivity extends BoundActivity {
    private BrowserWebView w;
    TextView urlPre;
    TextView urlMid;
    TextView urlEnd;
    ImageView favicon;
    public String tabId = null;
    View back;
    View forward;
    View background;
    View loading;
    View bookmarkAdd;
    View bookmarkRem;
    protected final BookmarkManager bookmarkManager = new BookmarkManager(this);
    private boolean isEphemeral;
    private boolean isTab;
    private boolean isTopBarForcablyHidden;
    private final String DEFAULT_URL = "https://www.google.com/";
    private CursorLayout container;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bind();

        Log.v("Lightning Browser", "Starting Browser Activity");

        setContentView(R.layout.activity_browser);
        getWindow().setStatusBarColor(Color.parseColor("#11181f"));

        background = findViewById(R.id.container);
        loading = findViewById(R.id.loading);

        urlPre = findViewById(R.id.urlPre);
        urlMid = findViewById(R.id.urlMid);
        urlEnd = findViewById(R.id.urlEnd);

        favicon = findViewById(R.id.favicon);
        favicon.setClipToOutline(true);

        Uri uri = getIntent().getData();
        if (uri != null && !uri.toString().isEmpty()) currentUrl = uri.toString();
        else currentUrl = DEFAULT_URL;

        isTab = getIntent().getBooleanExtra("isTab", false);

        if (getIntent().getExtras() != null) {
            isEphemeral = getIntent().getBooleanExtra("isEphemeral", false);
            if (isTab) {
                if (currentUrl.startsWith(BrowserService.TAB_PREFIX)) {
                    tabId = currentUrl;
                    currentUrl = BrowserService.getUrl(tabId);
                } else tabId = BrowserService.getNewTabId();
            }
        }
        if (tabId == null) tabId = BrowserService.TAB_PREFIX+"ext::"+currentUrl;
        Log.v("Lightning Browser", "... with url " + currentUrl + (isTab ? ", is a tab":", not a tab") + ", assigned id "+tabId);

        if (!isTab) {
            isTopBarForcablyHidden = true;
            hideTopBar();
        }

        // Back/Forward Buttons
        back = findViewById(R.id.back);
        forward = findViewById(R.id.forward);

        back.setOnClickListener((view) -> {
            if (w == null) return;
            if (w.canGoBack()) w.goBack();
            updateButtonsAndUrl();
        });
        forward.setOnClickListener((view) -> {
            if (w == null) return;
            if (w.canGoForward()) w.goForward();
            updateButtonsAndUrl();
        });

        back.setOnLongClickListener((view -> {
            if (w == null) return false;
            w.forwardFull();
            updateButtonsAndUrl();
            return true;
        }));
        forward.setOnLongClickListener((view -> {
            if (w == null) return false;
            w.backFull();
            updateButtonsAndUrl();
            return true;
        }));

        // Refresh Button
        View refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener((view) -> reload());
        refresh.setOnLongClickListener((view) -> {
            w.loadUrl(currentUrl);
            w.clearQueued = true;
            updateButtonsAndUrl(currentUrl);
            return true;
        });

        View exit = findViewById(R.id.exit);
        exit.setOnClickListener((View) -> finish());

        // Bookmark Button
        bookmarkAdd = findViewById(R.id.addBookmark);
        bookmarkRem = findViewById(R.id.removeBookmark);
        bookmarkAdd.setOnClickListener((view) -> {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.VISIBLE);
            bookmarkManager.addBookmark(currentUrl, BrowserService.getTitle(tabId));
        });
        bookmarkRem.setOnClickListener((view) -> {
            bookmarkRem.setVisibility(View.GONE);
            bookmarkAdd.setVisibility(View.VISIBLE);
            bookmarkManager.removeBookmark(currentUrl);
        });

        // Edit URL
        View urlLayout = findViewById(R.id.urlLayout);
        EditTextWatched urlEdit = findViewById(R.id.urlEdit);
        View clearUrl = findViewById(R.id.clear);
        View confirm = findViewById(R.id.confirm);
        urlEdit.setOnEdited((string) -> {
            clearUrl.setVisibility(string.isEmpty() ? View.GONE : View.VISIBLE);
            confirm.setVisibility(string.isEmpty() ? View.GONE : View.VISIBLE);
        });
        clearUrl.setOnClickListener((view) -> urlEdit.setText(""));

        View topBar = findViewById(R.id.topBar);
        View topBarEdit = findViewById(R.id.topBarEdit);
        urlLayout.setOnClickListener((view) -> {
            topBar    .setVisibility(View.GONE);
            topBarEdit.setVisibility(View.VISIBLE);
            urlEdit.setText(currentUrl);
            urlEdit.post(urlEdit::requestFocus);
            urlEdit.postDelayed(() -> Keyboard.show(this), 100);
        });
        urlEdit.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on Enter key press
                topBarEdit.findViewById(R.id.confirm).callOnClick();
                return true;
            }
            return false;
        });
        confirm.setOnClickListener((view) -> {
            String nUrl = urlEdit.getText().toString();

            nUrl = StringLib.toValidUrl(nUrl);

            w.loadUrl(nUrl);
            updateButtonsAndUrl(nUrl);
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
            Keyboard.hide(this, topBar);
        });
        topBarEdit.findViewById(R.id.cancel).setOnClickListener((view) -> {
            topBar.setVisibility(View.VISIBLE);
            topBarEdit.setVisibility(View.GONE);
            Keyboard.hide(this, topBar);
        });

        findViewById(R.id.extensions).setOnClickListener(v -> BrowserService.ManageExtensions());

        new BrowserUpdater(this).checkAppUpdateInteractive();
    }

    private void updateButtonsAndUrl() {
        updateButtonsAndUrl(w.getUrl());
    }
    public void updateButtonsAndUrl(String url) {
        if (w == null) return;
        updateUrl(url);
    }

    private void reload() {
        w.reload();
    }

    public void startLoading() {
        loading.setVisibility(View.VISIBLE);
        back.setAlpha(0.5f);
        forward.setAlpha(0.5f);
    }
    public void stopLoading() {
        loading.setVisibility(View.GONE);
        // Update navigation
        back.setAlpha(1f);
        forward.setAlpha(1f);
        back.setVisibility(w.canGoBack()       && !w.clearQueued ? View.VISIBLE : View.GONE);
        forward.setVisibility(w.canGoForward() && !w.clearQueued ? View.VISIBLE : View.GONE);
    }

    public void showTopBar() {
        if (isTopBarForcablyHidden) return;
        findViewById(R.id.topBar).setVisibility(View.VISIBLE);
        findViewById(R.id.topBarEdit).setVisibility(View.GONE);
    }
    public void hideTopBar() {
        findViewById(R.id.topBar).setVisibility(View.GONE);
        findViewById(R.id.topBarEdit).setVisibility(View.GONE);
    }

    public String currentUrl = "";

    // Splits the URL into parts and updates the URL display
    @SuppressLint("SetTextI18n") // It wants me to use a string resource to add a dot
    private void updateUrl(String url) {
        if (url == null) url = DEFAULT_URL;

        StringLib.ParititionedUrl pUrl = new StringLib.ParititionedUrl(url);

        urlPre.setText(pUrl.prefix);
        urlMid.setText(pUrl.middle);
        urlEnd.setText(pUrl.suffix);

        currentUrl = url;
        BrowserService.putUrl(tabId, url);

        if (bookmarkManager.getBookmarks().contains(currentUrl)) {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.VISIBLE);
        } else {
            bookmarkRem.setVisibility(View.GONE);
            bookmarkAdd.setVisibility(View.VISIBLE);
        }

        FaviconLoader.loadFavicon(this, currentUrl, favicon::setImageDrawable);
    }

    @Override
    public void onBackPressed() {
        if (isTopBarForcablyHidden) {
            showTopBar();
        } else if (isFullScreen()) {
            fullScreenSession.exitFullScreen();
            setFullScreenSession(null);
        } else if (findViewById(R.id.topBarEdit).getVisibility() == View.VISIBLE)
            findViewById(R.id.cancel).callOnClick();
        else {
            if (w.canGoBack()) {
                w.goBack();
                updateButtonsAndUrl();
            }
            else finish();
        }
    }

    @Override
    protected void
    onDestroy() {
        if (isFinishing()) {
            // Don't keep search views in background
            if (isEphemeral) wService.killWebView(tabId);
        }
        wService.removeActivity(this);
        super.onDestroy();
    }
    public void loadUrl(String url) {
        if (url == null) {
            Log.w("Lightning Browser", "Tried to load null URL");
            return;
        }
        w.loadUrl(url);
        updateButtonsAndUrl(url);
    }

    // Sets the WebView when the service is bound
    @Override
    protected void onBound() {
        if (tabId == null) tabId = DEFAULT_URL;

        // Show conditional buttons
        View kill = findViewById(R.id.kill);
        if (isTab) {
            kill.setVisibility(View.GONE);
        } else {
            bookmarkAdd.setVisibility(View.GONE);
            bookmarkRem.setVisibility(View.GONE);
            kill.setVisibility(View.VISIBLE);
            kill.setOnClickListener((view) -> wService.killWebView(tabId));
        }

        w = wService.getWebView(this);
        container = findViewById(R.id.container);
        container.addView(w);
        container.targetView = w;

        updateButtonsAndUrl();
    }

    @Override
    protected void onResume() {
        Dialog.setActivityContext(this);
        super.onResume();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) onBackPressed();
        return true;
    }

    private int accumulatedScrollY = 0;
    private boolean topBarOnCooldown = false;
    public void handleScrollChanged(int deltaY) {
        if (topBarOnCooldown || isTopBarForcablyHidden || isFullScreen()) return;

        final int THRESH = 50;

        accumulatedScrollY += deltaY;

        boolean hide;

        if (accumulatedScrollY < -THRESH) {
            hide = false;
            accumulatedScrollY = -THRESH;
        } else if (accumulatedScrollY > THRESH) {
            hide = true;
            accumulatedScrollY = THRESH;
        } else return;

        int topBarNewVis = hide ? View.GONE : View.VISIBLE;
        if (findViewById(R.id.topBar).getVisibility() != topBarNewVis) {
            if (topBarNewVis == View.VISIBLE) {
                showTopBar();
                container.scrollBy(0, -getTopLayoutHeight());
            } else {
                hideTopBar();
                container.scrollBy(0, getTopLayoutHeight());
            }
            topBarOnCooldown = true;
            container.postDelayed(() -> {
                accumulatedScrollY = 0;
                topBarOnCooldown = false;
            }, 1000);
        }
    }

    private int getTopLayoutHeight() {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (42 * scale + 0.5f);
    }

    GeckoSession fullScreenSession = null;
    protected boolean isFullScreen() {
        return fullScreenSession != null;
    }

    public void setFullScreenSession(GeckoSession geckoSession) {
        this.fullScreenSession = geckoSession;
        if (isFullScreen()) hideTopBar();
        else showTopBar();
    }
}
