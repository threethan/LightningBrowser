package com.threethan.browser.browser;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.PanZoomController;
import org.mozilla.geckoview.ScreenLength;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

// ADAPTED FROM https://gist.github.com/iyashamihsan/1ab5c1cfa47dea735ea46d8943a1bde4
// Replaces d-pad navigation with an on-screen cursor that behaves like a mouse
// This is necessary for web browsing to work, as many sites try to hook the dpad

public class CursorLayout extends LinearLayout {
    private static final float CURSOR_ACCEL = 900f;
    private static final float CURSOR_FRICTION = 20f;
    private static final float MAX_CURSOR_SPEED = 20000.0f;
    private static final float MIN_CURSOR_SPEED = 180f;
    private static final float SCROLL_START_MARGIN = 0.05f;
    private static int CURSOR_RADIUS = 0;
    private static float CURSOR_STROKE_WIDTH = 0f;
    private static final float SCROLL_MULT = 1f;
    private static final float SCROLL_SPEED_THRESHOLD = MIN_CURSOR_SPEED * 2f;
    private static final float PHYSICS_SUBSTEPS = 1;
    private final Point cursorDirection = new Point(0, 0);
    private final PointF cursorPosition = new PointF(0.0f, 0.0f);
    private final PointF cursorSpeed = new PointF(0.0f, 0.0f);
    private float sizeMult = 0.0f;
    private float holdMult = 1.0f;
    public View targetView;

    private final Runnable updateCursorRunnable = this::updateCursor;
    private boolean hovering;

    private synchronized void updateCursor() {
        long currentTimeMillis = System.currentTimeMillis();
        float deltaTime = (currentTimeMillis - lastCursorUpdate)/1000f;
        lastCursorUpdate = currentTimeMillis;
        CursorLayout cursorLayout = CursorLayout.this;


        float f = (deltaTime/PHYSICS_SUBSTEPS) * CURSOR_ACCEL;
        for (int i=0; i<PHYSICS_SUBSTEPS; i++) {
            float xSpeed = cursorSpeed.x;
            float xSpeedBound = cursorLayout.bound(xSpeed + (bound((float) cursorDirection.x, 1.0f) * f), CursorLayout.MAX_CURSOR_SPEED);
            cursorSpeed.set(xSpeedBound, bound(cursorSpeed.y + (bound((float) cursorDirection.y, 1.0f) * f), CursorLayout.MAX_CURSOR_SPEED));
        }

        if (Math.abs(cursorSpeed.x) < 0.1f) cursorSpeed.x = 0.0f;
        else if (Math.abs(cursorSpeed.x) < MIN_CURSOR_SPEED && cursorDirection.x != 0)
            cursorSpeed.x += MIN_CURSOR_SPEED * (cursorDirection.x > 0 ? 1 : -1);

        if (Math.abs(cursorSpeed.y) < 0.1f) cursorSpeed.y = 0.0f;
        else if (Math.abs(cursorSpeed.y) < MIN_CURSOR_SPEED && cursorDirection.y != 0)
            cursorSpeed.y += MIN_CURSOR_SPEED * (cursorDirection.y > 0 ? 1 : -1);

        if (cursorDirection.x == 0)
            for (int i=0; i<PHYSICS_SUBSTEPS; i++)
                cursorSpeed.x -= cursorSpeed.x * Math.min(1f, deltaTime/PHYSICS_SUBSTEPS * CURSOR_FRICTION);
        if (cursorDirection.y == 0)
            for (int i=0; i<PHYSICS_SUBSTEPS; i++)
                cursorSpeed.y -= cursorSpeed.y * Math.min(1f, deltaTime/PHYSICS_SUBSTEPS * CURSOR_FRICTION);

        if (cursorDirection.x == 0 && cursorDirection.y == 0 && cursorSpeed.x == 0.0f && cursorSpeed.y == 0.0f) {
            if (getHandler() != null) {
                // Hide cursor after timeout
                getHandler().postDelayed(CursorLayout.this::visUpdate, 5000);
            }
            return;
        }

        tmpPointF.set(cursorPosition);
        cursorPosition.offset(cursorSpeed.x*deltaTime, cursorSpeed.y*deltaTime);

        if (cursorPosition.x < 0.0f) cursorPosition.x = 0.1f; // Cap to > 0 to avoid scroll lock-up
        else if (cursorPosition.x > ((float) (getMeasuredWidth() - 1)))  cursorPosition.x = (float) (getMeasuredWidth() - 1);
        if (cursorPosition.y < 0.0f) cursorPosition.y = 0.1f; // Cap to > 0 to avoid scroll lock-up
        else if (cursorPosition.y > ((float) (getMeasuredHeight() - 1))) cursorPosition.y = (float) (getMeasuredHeight() - 1);
        if (!tmpPointF.equals(cursorPosition)) {
            if (centerPressed) {
                dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_MOVE); // Drag
            } else {
                if (hovering) {
                    dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_HOVER_MOVE); // Hover
                } else {
                    dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_HOVER_ENTER); // Hover enter
                    hovering = true;
                }
            }
        }
        if (targetView != null) {
            checkScrollTarget(deltaTime);
        }
        post(this::updateCursor);
        visUpdate();
    }

    private void checkScrollTarget(float deltaTime) {
        try {
            float deltaX = 0;
            float deltaY = 0;
            float mHeight = getMeasuredHeight();
            float mWidth = getMeasuredWidth();
            if (cursorPosition.y > (mHeight * (1f-SCROLL_START_MARGIN))) {
                if (cursorSpeed.y > SCROLL_SPEED_THRESHOLD) {
                    deltaY += cursorPosition.y * deltaTime * SCROLL_MULT;
                }
            } else if (cursorPosition.y < (mHeight * SCROLL_START_MARGIN) && cursorSpeed.y < -SCROLL_SPEED_THRESHOLD) {
                deltaY += cursorSpeed.y * deltaTime * SCROLL_MULT;
            }
            if (cursorPosition.x > (mWidth * (1f-SCROLL_START_MARGIN))) {
                if (cursorSpeed.x > SCROLL_SPEED_THRESHOLD) {
                    deltaX += cursorSpeed.x * deltaTime * SCROLL_MULT;
                }
            } else if (cursorPosition.x < (mWidth * SCROLL_START_MARGIN) && cursorSpeed.x < -SCROLL_SPEED_THRESHOLD) {
                deltaX += cursorSpeed.x * deltaTime * SCROLL_MULT;
            }
            if (deltaX != 0 || deltaY != 0)
                scrollTargetBy(deltaX, deltaY);
        } catch (Exception e) {
            Log.w("CursorLayout", "Exception during scroll", e);
        }
    }

    private boolean centerPressed;
    private long downTime;

    private int geckoAccumulatedX = 0;
    private int geckoAccumulatedY = 0;
    private static final Rect mTempRect = new Rect();
    private synchronized void scrollTargetBy(float deltaX, float deltaY) {
        if (targetView instanceof GeckoView geckoView) {
            PanZoomController pzc = geckoView.getPanZoomController();

            MotionEvent dispatchEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_SCROLL, deltaX, deltaY*100, 0);
            pzc.onMotionEvent(dispatchEvent);
            dispatchEvent.recycle();

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
                assert geckoView.getSession() != null;
                geckoView.getSession().getSurfaceBounds(mTempRect);
                float x = cursorPosition.x - mTempRect.left;
                float y = cursorPosition.y - mTempRect.top;
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
                return;
            } catch (Throwable e) {
                Log.w("CursorLayout", "Failed to invoke native scroll handler, falling back", e);
            }

            // Avoiding small scroll increments means better performance/smooth scroll
            int GECKO_MIN_SCROLL_INCREMENT = getMeasuredHeight() / 25;

            if (geckoAccumulatedX > GECKO_MIN_SCROLL_INCREMENT || geckoAccumulatedX < -GECKO_MIN_SCROLL_INCREMENT
                || geckoAccumulatedY > GECKO_MIN_SCROLL_INCREMENT || geckoAccumulatedY < -GECKO_MIN_SCROLL_INCREMENT) {
                pzc.scrollBy(ScreenLength.fromPixels(geckoAccumulatedX), ScreenLength.fromPixels(geckoAccumulatedY),
                        PanZoomController.SCROLL_BEHAVIOR_AUTO);
                geckoAccumulatedX = 0;
                geckoAccumulatedY = 0;
            }
            geckoAccumulatedY += (int) deltaY;
            geckoAccumulatedX += (int) deltaX;
        } else {
            targetView.scrollBy((int) deltaX, (int) deltaY);
        }
    }

    private final Runnable visUpdateRunnable = this::visUpdate;
    private void visUpdate() {
        invalidate();

        float cursorVisTarget = isCursorVisible() ? 1.0f : 0.0f;
        if (Math.abs(sizeMult - cursorVisTarget) < 0.1) sizeMult = cursorVisTarget;
        else {
            sizeMult += (cursorVisTarget-sizeMult) * (isCursorVisible() ? 0.02f : 0.2f);
            removeCallbacks(visUpdateRunnable);
            post(visUpdateRunnable);
        }
    }

    /* access modifiers changed from: private */
    public long lastCursorUpdate = System.currentTimeMillis() - 5000;
    private final Paint paint = new Paint();
    PointF tmpPointF = new PointF();

    private float bound(float val, float bound) {
        if (val > bound) return bound;
        return Math.max(val, -bound);
    }

    public CursorLayout(Context context) {
        super(context);
        init();
    }

    public CursorLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        if (isInEditMode()) return;
        this.paint.setAntiAlias(true);
        setWillNotDraw(false);
        Display defaultDisplay = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        CURSOR_RADIUS = (int) (point.x / 150f);
        CURSOR_STROKE_WIDTH = CURSOR_RADIUS / 5f;
        this.post(() -> cursorPosition.y = getHeight() / 2f);

        // Drag&Drop can eat all input as we don't have a mouse on the system level
        setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                cancelDragAndDrop();
                cancelLongPress();
            }
            return false;
        });
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
        if (!isInEditMode()) {
            this.cursorPosition.set(((float) w) / 2.0f, ((float) oldWidth) / 2.0f);
            if (getHandler() != null) {
                getHandler().postDelayed(this::updateCursor, 5000);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (!(keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) &&
        (keyEvent.getAction() == KeyEvent.ACTION_DOWN || keyEvent.getAction() == KeyEvent.ACTION_UP)) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP -> {
                    if (keyEvent.getAction() == 0) {
                        if (this.cursorPosition.y <= 0.0f)
                            return super.dispatchKeyEvent(keyEvent);
                        handleDirectionKeyEvent(keyEvent, -100, -1);
                    } else
                        handleDirectionKeyEvent(keyEvent, -100, 0);
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (keyEvent.getAction() == 0) {
                        if (this.cursorPosition.y >= ((float) getMeasuredHeight()))
                            return super.dispatchKeyEvent(keyEvent);
                        handleDirectionKeyEvent(keyEvent, -100, 1);
                    } else
                        handleDirectionKeyEvent(keyEvent, -100, 0);
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (keyEvent.getAction() == 0) {
                        if (this.cursorPosition.x <= 0.0f)
                            return super.dispatchKeyEvent(keyEvent);
                        handleDirectionKeyEvent(keyEvent, -1, -100);
                    } else
                        handleDirectionKeyEvent(keyEvent, 0, -100);
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (keyEvent.getAction() == 0) {
                        if (this.cursorPosition.x >= ((float) getMeasuredWidth()))
                            return super.dispatchKeyEvent(keyEvent);
                        handleDirectionKeyEvent(keyEvent, 1, -100);
                    } else
                        handleDirectionKeyEvent(keyEvent, 0, -100);
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_UP_LEFT -> {
                    if (keyEvent.getAction() == 0)
                        handleDirectionKeyEvent(keyEvent, -1, -1);
                    else
                        handleDirectionKeyEvent(keyEvent, 0, 0);
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> {
                    if (keyEvent.getAction() == 0)
                        handleDirectionKeyEvent(keyEvent, -1, 1);
                    else
                        handleDirectionKeyEvent(keyEvent, 0, 0);
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_UP_RIGHT -> {
                    if (keyEvent.getAction() == 0)
                        handleDirectionKeyEvent(keyEvent, 1, -1);
                    else
                        handleDirectionKeyEvent(keyEvent, 0, 0);
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT -> {
                    if (keyEvent.getAction() == 0)
                        handleDirectionKeyEvent(keyEvent, 1, 1);
                    else
                        handleDirectionKeyEvent(keyEvent, 0, 0);
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_CENTER -> {
                    if (isCursorVisible()) {

                        // Click animation
                        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && !getKeyDispatcherState().isTracking(keyEvent)) {
                            centerPressed = true;

                            getKeyDispatcherState().startTracking(keyEvent, this);
                            dispatchMotionEvent(this.cursorPosition.x, this.cursorPosition.y, MotionEvent.ACTION_DOWN);

                            ValueAnimator viewAnimator = ValueAnimator.ofFloat(sizeMult, 0.7f);
                            viewAnimator.setDuration(250);
                            viewAnimator.setInterpolator(new OvershootInterpolator());
                            viewAnimator.addUpdateListener(animation -> {
                                sizeMult = (float) animation.getAnimatedValue();
                                this.lastCursorUpdate = System.currentTimeMillis();
                                invalidate();
                            });
                            viewAnimator.start();
                            postDelayed(() -> {
                                ValueAnimator viewAnimator2 = ValueAnimator.ofFloat(sizeMult, 1.0f);
                                viewAnimator2.setDuration(250);
                                viewAnimator2.setInterpolator(new OvershootInterpolator());
                                viewAnimator2.addUpdateListener(animation -> {
                                    sizeMult = (float) animation.getAnimatedValue();
                                    this.lastCursorUpdate = System.currentTimeMillis();
                                    invalidate();
                                });
                                viewAnimator2.start();

                                if (centerPressed) {
                                    ValueAnimator holdAnimator = ValueAnimator.ofFloat(holdMult, 0.7f);
                                    holdAnimator.addUpdateListener(animation -> holdMult = (float) animation.getAnimatedValue());
                                    holdAnimator.setDuration(100);
                                    holdAnimator.start();
                                }
                            }, 250);


                        } else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                            centerPressed = false;

                            getKeyDispatcherState().handleUpEvent(keyEvent);
                            dispatchMotionEvent(this.cursorPosition.x, this.cursorPosition.y, MotionEvent.ACTION_UP);

                            ValueAnimator holdAnimator = ValueAnimator.ofFloat(holdMult, 1.0f);
                            holdAnimator.addUpdateListener(animation -> {
                                holdMult = (float) animation.getAnimatedValue();
                                invalidate();
                            });
                            holdAnimator.setDuration(150);
                            holdAnimator.start();
                        }
                        invalidate();
                        return true;
                    }
                }
            }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    protected void dispatchMotionEvent(float x, float y, int action) {
        if (action == MotionEvent.ACTION_DOWN) downTime = SystemClock.uptimeMillis();

        long eventTime = SystemClock.uptimeMillis();

        boolean isHoverEvent = action ==
                MotionEvent.ACTION_HOVER_ENTER
                || action == MotionEvent.ACTION_HOVER_MOVE
                || action == MotionEvent.ACTION_HOVER_EXIT;

        MotionEvent dispatchEvent = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        if (isHoverEvent) {
            dispatchGenericPointerEvent(dispatchEvent);
        } else {
            dispatchTouchEvent(dispatchEvent);
        }
        dispatchEvent.recycle();
    }

    protected void handleDirectionKeyEvent(KeyEvent keyEvent, int x, int y) {
        this.lastCursorUpdate = System.currentTimeMillis();

        Handler handler = getHandler();
        handler.removeCallbacks(updateCursorRunnable);
        handler.post(updateCursorRunnable);

        try {
            getKeyDispatcherState().startTracking(keyEvent, this);
        } catch (Exception ignored) {}

        Point point = this.cursorDirection;
        if (x == -100) x = point.x;
        if (y == -100) y = point.y;
        point.set(x, y);
    }
    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        if (!isInEditMode()) {

            float x = this.cursorPosition.x;
            float y = this.cursorPosition.y;

            float adjustedRadius = CURSOR_RADIUS * sizeMult * holdMult;
            // Shadow
            this.paint.setColor(Color.argb(10, 0, 0, 0));
            this.paint.setStyle(Style.FILL);
            canvas.drawCircle(x, y+CURSOR_STROKE_WIDTH, adjustedRadius, this.paint);
            this.paint.setColor(Color.argb(10, 0, 0, 0));
            this.paint.setStyle(Style.FILL);
            canvas.drawCircle(x, y+CURSOR_STROKE_WIDTH, adjustedRadius + CURSOR_STROKE_WIDTH, this.paint);
            // Cursor
            this.paint.setColor(Color.argb(128, 255, 255, 255));
            this.paint.setStyle(Style.FILL);
            canvas.drawCircle(x, y, adjustedRadius, this.paint);
            // Outline
            this.paint.setColor(Color.argb(200, 150, 150, 150));
            this.paint.setStrokeWidth(CURSOR_STROKE_WIDTH);
            this.paint.setStyle(Style.STROKE);
            canvas.drawCircle(x, y, adjustedRadius, this.paint);
        }

    }
    protected boolean isCursorVisible() {
        return System.currentTimeMillis() - this.lastCursorUpdate <= 4000;
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollTargetBy(x, y);
    }
}