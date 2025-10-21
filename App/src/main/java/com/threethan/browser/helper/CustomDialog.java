package com.threethan.browser.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.threethan.browser.R;

public abstract class CustomDialog {
    public static class Builder extends AlertDialog.Builder {
        public Builder(Context context) {
            super(context, R.style.DialogStyle);
        }

        @Override
        public AlertDialog show() {
            AlertDialog dialog = super.show();
            Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.sansserif);
            TextView message = dialog.findViewById(android.R.id.message);
            message.setTypeface(typeface);
            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            return dialog;
        }
    }
}
