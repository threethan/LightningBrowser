package com.threethan.browser.wrapper;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.threethan.browser.R;
import com.threethan.browser.adapter.BookmarksAdapter;
import com.threethan.browser.adapter.TabsAdapter;
import com.threethan.browser.browser.BrowserActivity;
import com.threethan.browser.browser.BrowserService;
import com.threethan.browser.helper.BookmarkManager;
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

        View confirm = findViewById(R.id.cancel);
        EditTextWatched searchText = findViewById(R.id.urlEdit);
        searchText.setOnEdited((var) -> confirm.setAlpha(var.isEmpty() ? 0.5f : 1f));
        searchText.setOnEdited((var) -> confirm.setFocusable(var.isEmpty()));
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
    private TabsAdapter tabsAdapter;
    private BookmarksAdapter bookmarksAdapter;
    private View tabsSection;
    private View bookmarksSection;
    @Override
    protected void onBound() {
        BrowserService.watchingActivities.add(this);
        // Init tabs list
        tabsAdapter = new TabsAdapter(this);
        tabsAdapter.setItems(wService.listWebViews());
        RecyclerView tabs = findViewById(R.id.tabList);
        tabs.setLayoutManager(new LinearLayoutManager(this));
        tabs.setAdapter(tabsAdapter);
        tabsSection = findViewById(R.id.tabsSection);
        tabsSection.setVisibility(wService.listWebViews().isEmpty() ? View.GONE : View.VISIBLE);

        // Init bookmarks list
        bookmarkManager = new BookmarkManager(this);
        bookmarksAdapter = new BookmarksAdapter(this);
        bookmarksAdapter.setItems(new ArrayList<>(bookmarkManager.getBookmarks()));
        RecyclerView bmarks = findViewById(R.id.bookmarkList);
        bmarks.setLayoutManager(new LinearLayoutManager(this));
        bmarks.setAdapter(bookmarksAdapter);
        bookmarksSection = findViewById(R.id.bookmarksSection);
        bookmarksSection.setVisibility(bookmarkManager.getBookmarks().isEmpty() ? View.GONE : View.VISIBLE);

    }

    @Override
    public void onTabUpdate(@Nullable String lastUpdateTabId) {
        tabsAdapter.setItems(wService.listWebViews());
        if (lastUpdateTabId != null) tabsAdapter.notifyItemChanged(lastUpdateTabId);
        tabsSection.setVisibility(wService.listWebViews().isEmpty() ? View.GONE : View.VISIBLE);
    }
    @Override
    public void onBookmarkUpdate() {
        bookmarksAdapter.setItems(new ArrayList<>(bookmarkManager.getBookmarks()));
        bookmarksAdapter.notifyAllChanged();
        tabsAdapter.notifyAllChanged();
        bookmarksSection.setVisibility(bookmarkManager.getBookmarks().isEmpty() ? View.GONE : View.VISIBLE);
    }

    public void open(String urlOrTabId) {
        bind(); // If we open a tab, the service will be started anyways

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlOrTabId));
        intent.setClass(this, BrowserActivity.class);
        intent.putExtra("isTab", true);
        startActivity(intent);
    }
}
