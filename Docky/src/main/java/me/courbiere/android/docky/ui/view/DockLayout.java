package me.courbiere.android.docky.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import me.courbiere.android.docky.R;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Dock View.
 */
public class DockLayout extends RelativeLayout {
    private static final String TAG = "DockLayout";

    public static final int POSITION_LEFT = 0;
    public static final int POSITION_RIGHT = 1;

    private static final int DOCK_CLOSED = 0;
    private static final int DOCK_OPENED = 1;

    private int mPosition;

    /**
     * Gesture Detector used to swipe Dock in and out.
     */
    private GestureDetector mDetector;

    private int mDockState;
    private float mInitialTouchX;
    private WindowManager mWindowManager;

    public DockLayout(Context context) {
        this(context, null);
    }

    public DockLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DockLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDockState = DOCK_OPENED;

        /*
        mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent downEvent, MotionEvent motionEvent, float distanceX, float distanceY) {
                LOGD(TAG, "here");
                return true;
            }

            @Override
            public boolean onFling(MotionEvent downEvent, MotionEvent motionEvent, float velocityX, float velocityY) {
                LOGD(TAG, "here");
                return true;
            }
        });
        */

        // Retrieve custom attributes
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DockLayout, 0, 0);

        try {
            mPosition = a.getInteger(R.styleable.DockLayout_position, 0);
        } finally {

            // TypeArray objects are shared resource and must recycled after use.
            a.recycle();
        }
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        if (position != 0 && position != 1) {
            throw new IllegalArgumentException("Invalid position specified.");
        }

        mPosition = position;
        invalidate();
        requestLayout();
    }

    public void attachToWindow() {
        final int dockWidth = (int) getResources().getDimension(R.dimen.dock_layout_width);

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                dockWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        layoutParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        layoutParams.setTitle(getContext().getString(R.string.app_name));
        mWindowManager.addView(this, layoutParams);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //return mDetector.onTouchEvent(event);

        int leftMargin;
        int rightMargin;
        final View dock = getDockView();
        final LayoutParams dockLp = (LayoutParams) dock.getLayoutParams();

        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                LOGD(TAG, "DOWN");
                mInitialTouchX = event.getRawX();

                        /*
                        dockLayoutLp.width = dockLayoutWidth;
                        mWindowManager.updateViewLayout(mDockLayout, dockLayoutLp);
                        */

                return false;

            case MotionEvent.ACTION_MOVE:
                // LOGD(TAG, "MOVE");
                int distance = (int) (event.getRawX() - mInitialTouchX);
                mInitialTouchX = event.getRawX();

                if (mDockState == DOCK_OPENED) {
                    // Move dock and drag handle inside dock layout.
                    leftMargin = dockLp.leftMargin + distance;
                    rightMargin = dockLp.rightMargin - distance;

                    if (leftMargin < 0) {
                        leftMargin = 0;
                        rightMargin = 0;
                    }
                    if (leftMargin > dock.getWidth()) {
                        leftMargin = dock.getWidth();
                        rightMargin = -dock.getWidth();
                    }

                    // Update dock position.
                    dockLp.setMargins(
                            leftMargin,
                            dockLp.topMargin,
                            rightMargin,
                            dockLp.bottomMargin);

                    dock.setLayoutParams(dockLp);
                } else {
                    final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) this.getLayoutParams();
                    // Slide dock layout inside window on the x axis.
                    dockLayoutLp.x -= distance;

                    if (dockLayoutLp.x > 0) {
                        dockLayoutLp.x = 0;
                    }

                    mWindowManager.updateViewLayout(this, dockLayoutLp);
                }

                        /*
                        if (dockLayoutLp.width - distance > dockLayoutWidth) {
                            dockLayoutLp.width = dockLayoutWidth;
                        } else if (dockLayoutLp.width - distance < dragHandleWidth) {
                            dockLayoutLp.width = dragHandleWidth;
                        } else {
                            dockLayoutLp.width -= distance;
                        }

                        mWindowManager.updateViewLayout(mDockLayout, dockLayoutLp);
                        */

                return false;

            case MotionEvent.ACTION_UP:
                LOGD(TAG, "UP");

                if (dockLp.leftMargin > dock.getWidth() / 2) {
                    close();
                } else {
                    open();
                }

                return false;

            case MotionEvent.ACTION_OUTSIDE:
                close();
                break;
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Finds and returns the Dock View contained in this DockLayout.
     *
     * @return the Dock View.
     */
    private View getDockView() {
        final int childCount = getChildCount();

        if (childCount == 0) {
            return null;
        }

        return getChildAt(0);
    }

    public void open() {
        final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) this.getLayoutParams();
        final View dock = getDockView();
        final LayoutParams dockLp = (LayoutParams) dock.getLayoutParams();

        // Slide Dock Layout into the window.
        dockLayoutLp.x = 0;
        mWindowManager.updateViewLayout(this, dockLayoutLp);

        // Update dock position.
        dockLp.setMargins(0, dockLp.topMargin, 0, dockLp.bottomMargin);
        dock.setLayoutParams(dockLp);

        mDockState = DOCK_OPENED;
    }

    public void close() {
        final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) this.getLayoutParams();
        final View dock = getDockView();
        final LayoutParams dockLp = (LayoutParams) dock.getLayoutParams();
        float startFactor = dockLp.leftMargin / (float) dock.getWidth();

        ValueAnimator val = ValueAnimator.ofFloat(startFactor, 1f);
        val.setDuration(100);
        val.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float factor = (float) animation.getAnimatedValue();

                int leftMargin = (int) (dock.getWidth() * factor);
                int rightMargin = -leftMargin;

                // Slide Dock Layout off the window.
                if (factor == 1f) {
                    leftMargin = 0;
                    rightMargin = 0;

                    dockLayoutLp.x = -dock.getWidth();
                    mWindowManager.updateViewLayout(DockLayout.this, dockLayoutLp);
                }

                // Update dock position.
                dockLp.setMargins(
                        leftMargin,
                        dockLp.topMargin,
                        rightMargin,
                        dockLp.bottomMargin);
                dock.setLayoutParams(dockLp);
            }
        });

        val.start();

        mDockState = DOCK_CLOSED;
    }
}
