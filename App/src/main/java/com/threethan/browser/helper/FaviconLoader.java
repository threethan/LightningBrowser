package com.threethan.browser.helper;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.threethan.browser.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public abstract class FaviconLoader {
    private static final String ICON_URL = "https://www.google.com/s2/favicons?domain=%s&sz=128";
    protected static final Map<String, Drawable> iconByBaseUrl = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    public static void loadFavicon(final Activity activity, String url, final IconCallback callback) {
        String tldUrl;
        try {
            tldUrl = new URL(url).getHost();
        } catch (MalformedURLException e) {
            tldUrl = url;
        }
        if (iconByBaseUrl.containsKey(tldUrl)) callback.onLoad(iconByBaseUrl.get(tldUrl));
        else download(activity, tldUrl, callback);
    }

    public interface IconCallback {
        void onLoad(Drawable icon);
    }

    // Starts the download and handles threading
    private static void download(final Activity activity, String baseUrl, final IconCallback callback) {
        new Thread(() -> {
            Object lock = locks.putIfAbsent(baseUrl, new Object());
            if (lock == null) {
                lock = locks.get(baseUrl);
            }
            synchronized (Objects.requireNonNull(lock)) {
                Drawable drawable = downloadIconFromUrl(String.format(ICON_URL, baseUrl));
                if (drawable == null) drawable = ContextCompat.getDrawable(activity, R.drawable.web_no_favicon);
                iconByBaseUrl.put(baseUrl, drawable);
                Drawable finalDrawable = drawable;
                activity.runOnUiThread(() -> callback.onLoad(finalDrawable));
                locks.remove(baseUrl);
            }
        }).start();
    }

    private static Drawable downloadIconFromUrl(String url) {
        try (InputStream inputStream = new URL(url).openStream()) {
            return new BitmapDrawable(Resources.getSystem(), BitmapFactory.decodeStream(inputStream));
        } catch (IOException ignored) {}
        return null;
    }
}
