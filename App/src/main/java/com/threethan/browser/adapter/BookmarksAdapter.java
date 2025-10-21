package com.threethan.browser.adapter;

import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.threethan.browser.R;
import com.threethan.browser.helper.Dialog;
import com.threethan.browser.wrapper.EditTextWatched;
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
        holder.closeBtn.setVisibility(View.GONE);

        final String url = items.get(position);

        holder.renameBtn.setOnClickListener(v -> {
            AlertDialog dialog = Dialog.build(wrapperActivity, R.layout.dialog_bookmark_details);
            assert dialog != null;
            ((TextView) dialog.findViewById(R.id.url)).setText(url);
            EditTextWatched nameEdit = dialog.findViewById(R.id.name);
            View clear = dialog.findViewById(R.id.clear);
            View confirm = dialog.findViewById(R.id.confirm);
            nameEdit.setOnEdited((string) -> {
                clear.setVisibility(string.isEmpty() ? View.GONE : View.VISIBLE);
                confirm.setEnabled(!string.isEmpty());
            });
            nameEdit.setText(wrapperActivity.bookmarkManager.getBookmarkTitle(url));
            dialog.findViewById(R.id.clear).setOnClickListener((view)
                    -> nameEdit.setText(""));
            dialog.findViewById(R.id.cancel).setOnClickListener((view) -> dialog.dismiss());
            confirm.setOnClickListener((view) -> {
                final String name = nameEdit.getText().toString();
                wrapperActivity.bookmarkManager.setBookmarkTitle(url, name);
                dialog.dismiss();
            });
        });

        super.onBindViewHolder(holder, position);

        holder.titleText.setText(wrapperActivity.bookmarkManager.getBookmarkTitle(url));

        holder.renameBtn.setVisibility(View.VISIBLE);

        holder.mainBtn.setOnLongClickListener(v -> {
            if (holder.closeBtn.getVisibility() == View.VISIBLE) {
                holder.closeBtn.callOnClick();
            } else {
                holder.renameBtn.callOnClick();
            }
            return true;
        });
    }
}
