package com.threethan.browser.helper;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.threethan.browser.R;

import java.lang.ref.WeakReference;

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

    public static void toast(String stringMain, String stringBold, boolean isLong) {
        if (getActivityContext() == null) return;

        // Fake toast for the Quest
        AlertDialog dialog = new CustomDialog.Builder(getActivityContext()).setView(R.layout.dialog_toast).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog_transparent);
            dialog.getWindow().setDimAmount(0.0f);
            final int FLAGS = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            dialog.getWindow().setFlags(FLAGS, FLAGS);
        }
        dialog.show();

        ((TextView) dialog.findViewById(R.id.toastTextMain)).setText(stringMain);
        ((TextView) dialog.findViewById(R.id.toastTextBold)).setText(stringBold);

        // Dismiss if not done automatically
        dialog.findViewById(R.id.toastTextMain).postDelayed(dialog::dismiss,
                isLong ? 5000 : 1750);
    }
}
