package com.threethan.browser.helper;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import com.threethan.browser.browser.BrowserService;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TabManager {
    private static DataStoreEditor dataStoreEditor;
    private static boolean hasLoaded = false;
    private static final String KEY_SUSPENDED_TAB_LIST = "KEY_SUSPENDED_TAB_LIST";
    private static final String KEY_TAB_TITLE_PREFIX = "titleof_tab_";
    private static final String KEY_TAB_URL_PREFIX = "urlof_tab_";
    private static final String KEY_USE_SUSPENDED_TABS = "KEY_USE_SUSPENDED_TABS";
    private static final Map<String, String> tabTitlesByUid = new ConcurrentHashMap<>();
    public TabManager(Activity activity) {
        dataStoreEditor = new DataStoreEditor(activity);
        if (hasLoaded) return;
        Set<String> urls = dataStoreEditor.getStringSet(KEY_SUSPENDED_TAB_LIST, new HashSet<>());
        for (String url : urls) {
            final String title = dataStoreEditor.getString(KEY_TAB_TITLE_PREFIX +url, url);
            tabTitlesByUid.put(url, title);
        }
        hasLoaded = true;
    }

    public String getTitle(String tabIdOrUrl) {
        String properTitle = BrowserService.getTitleOrNull(tabIdOrUrl);
        return properTitle == null ? tabTitlesByUid.getOrDefault(tabIdOrUrl, "Untitled Tab") : properTitle;
    }

    public String getUrl(String tabIdOrUrl) {
        String properUrl = BrowserService.getUrlOrNull(tabIdOrUrl);
        return properUrl == null ? dataStoreEditor.getString(KEY_TAB_URL_PREFIX +tabIdOrUrl, "---") : properUrl;
    }

    public Set<String> getSuspendedTabs() {
        return tabTitlesByUid.keySet();
    }

    public void addSuspendedTab(String tabId, String url, String title) {
        tabTitlesByUid.put(tabId, title);
        dataStoreEditor.putStringSet(KEY_SUSPENDED_TAB_LIST, tabTitlesByUid.keySet());
        dataStoreEditor.putString(KEY_TAB_TITLE_PREFIX +tabId, title);
        dataStoreEditor.putString(KEY_TAB_URL_PREFIX +tabId, url);
        BrowserService.tabUpdate(null);
    }
    public void removeSuspendedTab(String tabId) {
        tabTitlesByUid.remove(tabId);
        dataStoreEditor.putStringSet(KEY_SUSPENDED_TAB_LIST, tabTitlesByUid.keySet());
        dataStoreEditor.removeString(KEY_TAB_TITLE_PREFIX +tabId);
        Log.i("SuspendedTabs", "Suspended Tab Removed from tabId "+tabId);
        BrowserService.bookmarkUpdate();
    }

    public void clearSuspendedTabs() {
        tabTitlesByUid.clear();
        dataStoreEditor.putStringSet(KEY_SUSPENDED_TAB_LIST, tabTitlesByUid.keySet());
        BrowserService.tabUpdate(null);
    }

    public boolean shouldUseSuspendedTabs(Context context) {
        boolean isTv = (((UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE))
                .getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION);
        return dataStoreEditor.getBoolean(KEY_USE_SUSPENDED_TABS, isTv);
    }

    public void setUseSuspendedTabs(boolean useSuspendedTabs) {
        dataStoreEditor.putBoolean(KEY_USE_SUSPENDED_TABS, useSuspendedTabs);
    }
}
