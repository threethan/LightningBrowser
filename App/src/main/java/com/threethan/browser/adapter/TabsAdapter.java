package com.threethan.browser.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.threethan.browser.R;
import com.threethan.browser.browser.BrowserService;
import com.threethan.browser.helper.FaviconLoader;
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

    public String getUrl(String tabIdOrUrl) {
        return BrowserService.getUrl(tabIdOrUrl);
    }
    @Override
    public void onBindViewHolder(@NonNull TabHolder holder, int position) {
        String tabIdOrUrl = items.get(position);
        holder.titleText.setText(BrowserService.getTitle(tabIdOrUrl));
        final String url = getUrl(tabIdOrUrl);
        holder.urlText.setText(url);
        FaviconLoader.loadFavicon(wrapperActivity, url, holder.favicon::setImageDrawable);

        holder.closeBtn.setOnClickListener(v -> {
            int index = items.indexOf(tabIdOrUrl);
            items.remove(tabIdOrUrl);
            notifyItemRemoved(index);
            wrapperActivity.wService.killWebView(tabIdOrUrl);
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
            wrapperActivity.bookmarkManager.addBookmark(url, BrowserService.getTitle(tabIdOrUrl));
        });
        holder.bookmarkRemBtn.setOnClickListener((view) -> {
            holder.bookmarkRemBtn.setVisibility(View.GONE);
            holder.bookmarkAddBtn.setVisibility(View.VISIBLE);
            wrapperActivity.bookmarkManager.removeBookmark(url);
        });

        holder.view.setOnClickListener(v -> wrapperActivity.open(tabIdOrUrl));
    }
    protected static class TabHolder extends RecyclerView.ViewHolder {
        final View view;

        final TextView titleText;
        final TextView urlText;
        final View closeBtn;
        final View renameBtn;
        final View bookmarkAddBtn;
        final View bookmarkRemBtn;
        final ImageView favicon;
        public TabHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            titleText = itemView.findViewById(R.id.title);
            urlText = itemView.findViewById(R.id.url);
            closeBtn = itemView.findViewById(R.id.close);
            renameBtn = itemView.findViewById(R.id.rename);
            bookmarkAddBtn = itemView.findViewById(R.id.addBookmark);
            bookmarkRemBtn = itemView.findViewById(R.id.removeBookmark);
            favicon = itemView.findViewById(R.id.favicon);
            favicon.setClipToOutline(true);
        }
    }
}
