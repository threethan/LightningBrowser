package com.threethan.browser.wrapper;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.threethan.browser.R;
import com.threethan.browser.adapter.ActiveTabsAdapter;
import com.threethan.browser.adapter.BookmarksAdapter;
import com.threethan.browser.adapter.TabsAdapter;
import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.browser.BrowserService;
import com.threethan.browser.helper.BookmarkManager;
import com.threethan.browser.helper.TabManager;
import com.threethan.browser.lib.StringLib;

import java.util.ArrayList;

public class WrapperActivity extends BoundActivity {
    public BookmarkManager bookmarkManager;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bind();

        setContentView(R.layout.activity_wrapper);

        getWindow().setStatusBarColor(Color.parseColor("#11181f"));

        View confirm = findViewById(R.id.confirm);
        EditTextWatched searchText = findViewById(R.id.urlEdit);
        searchText.setOnEdited((var) -> confirm.setEnabled(!var.isEmpty()));
        searchText.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Perform action on Enter key press
                confirm.callOnClick();
                return true;
            }
            return false;
        });
        confirm.setOnClickListener((v) -> {
            String url = searchText.getText().toString();
            if (!url.isEmpty()) {
                open(StringLib.toValidUrl(url));
                searchText.setText("");
            }
        });
    }
    private TabsAdapter tabsAdapterActive;
    private TabsAdapter tabsAdapterSuspended;
    private BookmarksAdapter bookmarksAdapter;
    private View tabsSectionActive;
    private View tabsSectionSuspended;
    private View bookmarksSection;
    @Override
    protected void onBound() {
        BrowserService.watchingActivities.add(this);

        // Init bookmarks list
        bookmarkManager = new BookmarkManager(this);
        bookmarksAdapter = new BookmarksAdapter(this);
        bookmarksAdapter.setItems(new ArrayList<>(bookmarkManager.getBookmarks()));
        RecyclerView bookmarks = findViewById(R.id.bookmarkList);
        bookmarks.setLayoutManager(new LinearLayoutManager(this));
        bookmarks.setAdapter(bookmarksAdapter);
        bookmarksSection = findViewById(R.id.bookmarksSection);
        bookmarksSection.setVisibility(bookmarkManager.getBookmarks().isEmpty() ? View.GONE : View.VISIBLE);

        // Init active tabs list
        tabsAdapterActive = new ActiveTabsAdapter(this);
        if (wService != null) tabsAdapterActive.setItems(new ArrayList<>(wService.listWebViews()));
        RecyclerView activeTabs = findViewById(R.id.tabListActive);
        activeTabs.setLayoutManager(new LinearLayoutManager(this));
        activeTabs.setAdapter(tabsAdapterActive);
        tabsSectionActive = findViewById(R.id.tabsSectionActive);

        // Init suspended tabs list
        TabManager tabManager = new TabManager(this);
        boolean useSuspendedTabs = tabManager.shouldUseSuspendedTabs(this);

        tabsAdapterSuspended = new TabsAdapter(this);

        tabsAdapterSuspended.setItems(new ArrayList<>(tabManager.getSuspendedTabs()));

        RecyclerView suspendedTabs = findViewById(R.id.tabListSuspended);
        suspendedTabs.setLayoutManager(new LinearLayoutManager(this));
        suspendedTabs.setAdapter(tabsAdapterSuspended);
        tabsSectionSuspended = findViewById(R.id.tabsSectionSuspended);

        findViewById(R.id.tabsInfoSuspendedChange).setOnClickListener(v
                -> updateUseSuspendedTabs(tabManager, false)
        );
        findViewById(R.id.tabsInfoActiveDisable).setOnClickListener(v
                -> updateUseSuspendedTabs(tabManager, true)
        );

        updateUseSuspendedTabs(tabManager, useSuspendedTabs);
    }

    @Override
    public void onTabUpdate(@Nullable String lastUpdateTabId) {
        if (wService == null) return;

        tabsAdapterActive.setItems(wService.listWebViews());
        if (lastUpdateTabId != null) tabsAdapterActive.notifyItemChanged(lastUpdateTabId);
        else tabsAdapterActive.notifyAllChanged();
        tabsSectionActive.setVisibility(
                wService.listWebViews().isEmpty() ? View.GONE : View.VISIBLE
        );

        TabManager tabManager = new TabManager(this);
        tabsAdapterSuspended.setItems(new ArrayList<>(tabManager.getSuspendedTabs()));
        if (lastUpdateTabId != null) tabsAdapterSuspended.notifyItemChanged(lastUpdateTabId);
        else tabsAdapterSuspended.notifyAllChanged();
        tabsSectionSuspended.setVisibility(
                !tabManager.shouldUseSuspendedTabs(this)
                || tabManager.getSuspendedTabs().isEmpty() ? View.GONE : View.VISIBLE
        );
    }
    @Override
    public void onBookmarkUpdate() {
        bookmarksAdapter.setItems(new ArrayList<>(bookmarkManager.getBookmarks()));
        bookmarksAdapter.notifyAllChanged();
        tabsAdapterActive.notifyAllChanged();
        bookmarksSection.setVisibility(bookmarkManager.getBookmarks().isEmpty() ? View.GONE : View.VISIBLE);
        onTabUpdate(null);
    }

    public void open(String urlOrTabId) {
        bind(); // If we open a tab, the service will be started anyways

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlOrTabId));
        intent.setClass(this, BrowserActivity.class);
        intent.putExtra("isTab", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);

        new TabManager(this).removeSuspendedTab(urlOrTabId);
    }
    protected void updateUseSuspendedTabs(TabManager tabManager,
                                          boolean useSuspendedTabs) {
        // Update visibilities
        findViewById(R.id.tabsInfoActive).setVisibility(useSuspendedTabs ? View.GONE : View.VISIBLE);
        findViewById(R.id.tabsInfoSuspendedChange).setVisibility(useSuspendedTabs ? View.VISIBLE : View.GONE);

        // Update TabManager setting
        tabManager.setUseSuspendedTabs(useSuspendedTabs);

        if (wService != null && useSuspendedTabs) {
            // Move active tabs to suspended tabs
            for (String tabId : wService.listWebViews()) {
                String title = BrowserService.getTitle(tabId);
                wService.setWebViewActive(tabId, false);
                tabManager.addSuspendedTab(tabId, BrowserService.getUrl(tabId), title);
                wService.killWebView(tabId);
            }
        }

        new Handler().postDelayed(() -> {
            // Notify changes
            BrowserService.tabUpdate(null);
        }, 250);
    }
}
