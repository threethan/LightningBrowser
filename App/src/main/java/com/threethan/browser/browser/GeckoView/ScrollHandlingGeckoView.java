package com.threethan.browser.browser.GeckoView;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.PanZoomController;
import org.mozilla.geckoview.ScreenLength;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ScrollHandlingGeckoView extends GeckoView {
    public ScrollHandlingGeckoView(Context context) {
        super(context);
    }

    public ScrollHandlingGeckoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @FunctionalInterface
    public interface onScrollInterceptor {
        /**
         * Called when scroll happens
         * @param deltaX Scroll delta in X direction
         * @param deltaY Scroll delta in Y direction
         * @return true to consume the scroll event, false to let GeckoView handle it
         */
        boolean onScrollChanged(int deltaX, int deltaY);
    }

    private float cursorX = 0;
    private float cursorY = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        cursorX = event.getX();
        cursorY = event.getY();
        return super.onTouchEvent(event);
    }

    @Override
    protected boolean dispatchGenericPointerEvent(MotionEvent event) {
        cursorX = event.getX();
        cursorY = event.getY();

        return super.dispatchGenericPointerEvent(event);
    }

    private int geckoAccumulatedX = 0;
    private int geckoAccumulatedY = 0;
    private static final Rect mTempRect = new Rect();
    private onScrollInterceptor scrollInterceptor = null;
    public void setOnScrollInterceptor(onScrollInterceptor interceptor) {
        this.scrollInterceptor = interceptor;
    }

    @Override
    public void scrollBy(int deltaX, int deltaY) {
        dispatchScrollInternal(deltaX, deltaY);
    }

    protected void dispatchScrollInternal(int deltaX, int deltaY) {
        if (scrollInterceptor != null) {
            boolean consumed = scrollInterceptor.onScrollChanged(deltaX, deltaY);
            if (consumed) {
                return;
            }
        }

        PanZoomController pzc = getPanZoomController();

        MotionEvent dispatchEvent = MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), MotionEvent.ACTION_SCROLL, deltaX, deltaY * 100, 0);

        // Hack into native scroll handler for better response
        try {
            Field nativeField = PanZoomController.class.getDeclaredField("mNative");
            nativeField.setAccessible(true);
            Object nativeObject = nativeField.get(pzc);


            assert nativeObject != null;
            Field attachedField = PanZoomController.class.getDeclaredField("mAttached");
            attachedField.setAccessible(true);
            //noinspection DataFlowIssue
            boolean isAttached = (boolean) attachedField.get(pzc);
            if (!isAttached) {
                // Prevent crash and fall back to normal scroll
                throw new IllegalStateException("PanZoomController not attached yet");
            }

            Method handleScrollMethod = nativeObject.getClass().getDeclaredMethod(
                    "handleScrollEvent",
                    long.class, int.class, float.class, float.class, float.class, float.class
            );
            handleScrollMethod.setAccessible(true);
            assert getSession() != null;
            getSession().getSurfaceBounds(mTempRect);


            float x = cursorX - mTempRect.left;
            float y = cursorY - mTempRect.top;
            if (y > mTempRect.bottom) y = mTempRect.bottom - 1;
            if (x > mTempRect.right) x = mTempRect.right - 1;
            handleScrollMethod.invoke(
                    nativeObject,
                    dispatchEvent.getEventTime(),
                    dispatchEvent.getMetaState(),
                    x,
                    y,
                    -deltaX,
                    -deltaY
            );
        } catch (Throwable e) {
            Log.w("ScrollGeckoView", "Failed to invoke native scroll handler, falling back", e);
            // Avoiding small scroll increments means better performance/smooth scroll
            int GECKO_MIN_SCROLL_INCREMENT = getMeasuredHeight() / 25;

            if (geckoAccumulatedX > GECKO_MIN_SCROLL_INCREMENT || geckoAccumulatedX < -GECKO_MIN_SCROLL_INCREMENT
                    || geckoAccumulatedY > GECKO_MIN_SCROLL_INCREMENT || geckoAccumulatedY < -GECKO_MIN_SCROLL_INCREMENT) {
                pzc.scrollBy(ScreenLength.fromPixels(geckoAccumulatedX), ScreenLength.fromPixels(geckoAccumulatedY),
                        PanZoomController.SCROLL_BEHAVIOR_AUTO);
                geckoAccumulatedX = 0;
                geckoAccumulatedY = 0;
            }
            geckoAccumulatedX += deltaX;
            geckoAccumulatedY += deltaY;
        } finally {
            dispatchEvent.recycle();
        }


    }
}
