package com.threethan.browser.adapter;

import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.threethan.browser.R;
import com.threethan.browser.helper.Dialog;
import com.threethan.browser.wrapper.WrapperActivity;

public class BookmarksAdapter extends TabsAdapter {

    public BookmarksAdapter(WrapperActivity wrapperActivity) {
        super(wrapperActivity);
    }

    @Override
    public String getUrl(String tabIdOrUrl) {
        return tabIdOrUrl;
    }

    @Override
    public void onBindViewHolder(@NonNull TabHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.closeBtn.setVisibility(View.GONE);

        final String url = items.get(position);
        holder.titleText.setText(wrapperActivity.bookmarkManager.getBookmarkTitle(url));

        holder.renameBtn.setVisibility(View.VISIBLE);
        holder.renameBtn.setOnClickListener(v -> {
            AlertDialog dialog = Dialog.build(wrapperActivity, R.layout.dialog_bookmark_details);
            assert dialog != null;
            ((TextView) dialog.findViewById(R.id.url)).setText(url);
            ((EditText) dialog.findViewById(R.id.name))
                    .setText(wrapperActivity.bookmarkManager.getBookmarkTitle(url));
            dialog.findViewById(R.id.cancel).setOnClickListener((view) -> dialog.dismiss());
            dialog.findViewById(R.id.confirm).setOnClickListener((view) -> {
                final String name = ((EditText) dialog.findViewById(R.id.name)).getText().toString();
                wrapperActivity.bookmarkManager.addBookmark(url, name);
                dialog.dismiss();
            });
        });
    }
}
