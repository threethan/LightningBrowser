package com.threethan.browser.adapter;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.threethan.browser.R;
import com.threethan.browser.helper.FaviconLoader;
import com.threethan.browser.helper.TabManager;
import com.threethan.browser.wrapper.WrapperActivity;

public class TabsAdapter extends ArrayListAdapter<String, TabsAdapter.TabHolder> {
    protected final WrapperActivity wrapperActivity;
    public TabsAdapter(WrapperActivity wrapperActivity) {
        this.wrapperActivity = wrapperActivity;
    }
    @NonNull
    @Override
    public TabHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) wrapperActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = layoutInflater.inflate(R.layout.lv_tab, parent, false);

        return new TabHolder(itemView);
    }

    public String getDisplayUrl(String tabIdOrUrl, TabManager tabManager) {
        return tabManager.getUrl(tabIdOrUrl);
    }
    public String getOpenUrl(String tabIdOrUrl, TabManager tabManager) {
        return tabManager.getUrl(tabIdOrUrl);
    }
    @Override
    public void onBindViewHolder(@NonNull TabHolder holder, int position) {
        TabManager tabManager = new TabManager(wrapperActivity);
        String tabIdOrUrl = items.get(position);
        holder.titleText.setText(tabManager.getTitle(tabIdOrUrl));
        final String url = getDisplayUrl(tabIdOrUrl, tabManager);
        holder.urlText.setText(url);
        FaviconLoader.loadFavicon(
                wrapperActivity, url, favicon
                -> holder.favicon.setImageDrawable(copy(favicon))
        );

        holder.closeBtn.setOnClickListener(v -> {
            int index = items.indexOf(tabIdOrUrl);
            items.remove(tabIdOrUrl);
            notifyItemRemoved(index);
            if (wrapperActivity.wService != null)
                wrapperActivity.wService.killWebView(tabIdOrUrl);
            tabManager.removeSuspendedTab(tabIdOrUrl);
        });

        if (wrapperActivity.bookmarkManager.getBookmarks().contains(url)) {
            holder.bookmarkRemBtn.setVisibility(View.VISIBLE);
            holder.bookmarkAddBtn.setVisibility(View.GONE);
        } else {
            holder.bookmarkAddBtn.setVisibility(View.VISIBLE);
            holder.bookmarkRemBtn.setVisibility(View.GONE);
        }

        holder.bookmarkAddBtn.setOnClickListener((view) -> {
            holder.bookmarkAddBtn.setVisibility(View.GONE);
            holder.bookmarkRemBtn.setVisibility(View.VISIBLE);
            wrapperActivity.bookmarkManager.addBookmark(url, tabManager.getTitle(tabIdOrUrl));
        });
        holder.bookmarkRemBtn.setOnClickListener((view) -> {
            holder.bookmarkRemBtn.setVisibility(View.GONE);
            holder.bookmarkAddBtn.setVisibility(View.VISIBLE);
            wrapperActivity.bookmarkManager.removeBookmark(url);
        });

        holder.mainBtn.setOnClickListener(v -> wrapperActivity.open(
                getOpenUrl(tabIdOrUrl, tabManager))
        );
        holder.mainBtn.setOnLongClickListener(v -> {
            if (holder.closeBtn.getVisibility() == View.VISIBLE) {
                holder.closeBtn.callOnClick();
                return true;
            } else {
                return false;
            }
        });

        // Fix focus oddities
        View lastFocusable = null;
        for (int i = 0; i < holder.buttonsViewGroup.getChildCount(); i++) {
            holder.buttonsViewGroup.getChildAt(i).setNextFocusLeftId(
                    lastFocusable != null ? lastFocusable.getId() : holder.mainBtn.getId()
            );
            if (holder.buttonsViewGroup.getChildAt(i).isFocusable())
                lastFocusable = holder.buttonsViewGroup.getChildAt(i);
        }
    }

    private Drawable copy(Drawable favicon) {
        if (favicon instanceof BitmapDrawable bitmapDrawable) {
            return new BitmapDrawable(wrapperActivity.getResources(), bitmapDrawable.getBitmap());
        }
        return favicon;
    }

    public static class TabHolder extends RecyclerView.ViewHolder {
        final View view;
        final View mainBtn;

        final TextView titleText;
        final TextView urlText;
        final View closeBtn;
        final View renameBtn;
        final View bookmarkAddBtn;
        final View bookmarkRemBtn;
        final ImageView favicon;
        final ViewGroup buttonsViewGroup;
        protected TabHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            titleText = itemView.findViewById(R.id.title);
            urlText = itemView.findViewById(R.id.url);
            closeBtn = itemView.findViewById(R.id.close);
            renameBtn = itemView.findViewById(R.id.rename);
            bookmarkAddBtn = itemView.findViewById(R.id.addBookmark);
            bookmarkRemBtn = itemView.findViewById(R.id.removeBookmark);
            mainBtn = itemView.findViewById(R.id.main);
            favicon = itemView.findViewById(R.id.favicon);
            favicon.setClipToOutline(true);
            buttonsViewGroup = itemView.findViewById(R.id.buttons);
        }
    }
}
