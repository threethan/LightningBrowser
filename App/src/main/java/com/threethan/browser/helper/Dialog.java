package com.threethan.browser.helper;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.threethan.browser.R;

import java.lang.ref.WeakReference;
import java.util.Objects;

/*
    Dialog

    This provides a wrapper for AlertDialog.Builder that makes it even easier to create an alert
    dialog from a layout resource
 */
public abstract class Dialog {
    private static WeakReference<Activity> activityContextWeakReference;

    @Nullable
    public static Activity getActivityContext() {
        return activityContextWeakReference.get();
    }
    public static void setActivityContext(Activity activityContext) {
        activityContextWeakReference = new WeakReference<>(activityContext);
    }

    @Nullable
    public static AlertDialog build(Context context, int resource) {
        AlertDialog dialog = new CustomDialog.Builder(context).setView(resource).create();

        if (dialog.getWindow() == null) return null;
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        dialog.getWindow().setDimAmount(0.3f);
        final View rootView = dialog.getWindow().getDecorView().findViewById(android.R.id.content).getRootView();
        rootView.setLayerType(View.LAYER_TYPE_HARDWARE, new Paint());

        ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, "TranslationY", 100, 0);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();

        dialog.show();
        return dialog;

    }

    /** @noinspection unused*/
    public static void toast(String string) {
        toast(string, "", false);
    }

    // Custom dialog that catches exceptions on dismiss
    protected static class ToastDialog extends AlertDialog {
        protected ToastDialog(Context context) {
            super(context);
        }

        @Override
        public void dismiss() {
            try {
                super.dismiss();
            } catch (Exception ignored) {}
        }
    }
    public static void toast(CharSequence stringMain, CharSequence stringBold, boolean isLong) {
        Log.d("Toast", stringMain + " " + stringBold);

        try {
            LayoutInflater inflater =
                    Objects.requireNonNull(getActivityContext())
                            .getLayoutInflater();
            @SuppressLint("InflateParams")
            View layout = inflater.inflate(R.layout.dialog_toast, null);

            TextView textMain = layout.findViewById(R.id.toastTextMain);
            TextView textBold = layout.findViewById(R.id.toastTextBold);
            textMain.setText(stringMain);
            textBold.setText(stringBold);

            AlertDialog dialog = new ToastDialog(getActivityContext());
            dialog.setView(layout);
            dialog.setCancelable(true);

            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    0xFFFFFFFF
            );
            textMain.postDelayed(dialog::dismiss, isLong ? 3500 : 2000);
            dialog.show();
        } catch (Exception e) {
            Log.w("Toast", "Failed to show toast", e);
        }
    }

}
