package com.threethan.browser.helper;

import android.app.Activity;
import android.util.Log;

import com.threethan.browser.browser.BrowserService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BookmarkManager {
    private static DataStoreEditor dataStoreEditor;
    private static boolean hasLoaded = false;
    private static final String KEY_BOOKMARK_LIST = "KEY_BOOKMARK_LIST";
    private static final String KEY_BOOKMARK_TITLE_PREFIX= "titleof_";
    private static final Map<String, String> bookmarkTitlesByUrl = new ConcurrentHashMap<>();
    public BookmarkManager(Activity activity) {
        dataStoreEditor = new DataStoreEditor(activity);
        if (hasLoaded) return;
        Set<String> urls = dataStoreEditor.getStringSet(KEY_BOOKMARK_LIST, new HashSet<>());
        for (String url : urls) {
            final String title = dataStoreEditor.getString(KEY_BOOKMARK_TITLE_PREFIX+url, url);
            bookmarkTitlesByUrl.put(url, title);
        }
        hasLoaded = true;
    }
    public Set<String> getBookmarks() {
        return bookmarkTitlesByUrl.keySet();
    }
    public String getBookmarkTitle(String url) {
        return bookmarkTitlesByUrl.get(url);
    }

    public void setBookmarkTitle(String url, String title) {
        if (bookmarkTitlesByUrl.containsKey(url)) {
            bookmarkTitlesByUrl.put(url, title);
            dataStoreEditor.putString(KEY_BOOKMARK_TITLE_PREFIX+url, title);
            Log.i("Bookmarks", "Bookmark Title Updated for url "+url+" to title "+title);
            BrowserService.bookmarkUpdate();
        }
    }

    public void addBookmark(String url, String title) {
        bookmarkTitlesByUrl.put(url, title);
        dataStoreEditor.putStringSet(KEY_BOOKMARK_LIST, bookmarkTitlesByUrl.keySet());
        dataStoreEditor.putString(KEY_BOOKMARK_TITLE_PREFIX+url, title);
        Log.i("Bookmarks", "Bookmark Added for url "+url+" with title "+title);
        BrowserService.bookmarkUpdate();
    }
    public void removeBookmark(String url) {
        bookmarkTitlesByUrl.remove(url);
        dataStoreEditor.putStringSet(KEY_BOOKMARK_LIST, bookmarkTitlesByUrl.keySet());
        dataStoreEditor.removeString(KEY_BOOKMARK_TITLE_PREFIX+url);
        Log.i("Bookmarks", "Bookmark Removed from url "+url);
        BrowserService.bookmarkUpdate();
    }
}
