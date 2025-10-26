package com.threethan.browser.wrapper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.threethan.browser.browser.BrowserService;

public class BoundActivity extends Activity {
    public @Nullable BrowserService wService;
    protected final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to BrowserService, cast the IBinder and get the BrowserService instance.
            BrowserService.LocalBinder binder = (BrowserService.LocalBinder) service;
            wService = binder.getService();
            onBound();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {wService = null;}
    };
    protected void onBound() {}
    @Override
    protected void onDestroy() {
        if (isFinishing()) unbindService(connection);
        BrowserService.watchingActivities.remove(this);
        super.onDestroy();
    }
    protected void bind() {
        // Bind to BrowserService
        BrowserService.bind(this, connection, true);
    }
    public void onTabUpdate(@Nullable String lastUpdatedTabId) {}
    public void onBookmarkUpdate() {}
}
