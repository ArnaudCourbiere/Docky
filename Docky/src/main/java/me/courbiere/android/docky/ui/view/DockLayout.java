package me.courbiere.android.docky.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
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

    /**
     * Indicates that the dock is in an idle, settled state. No animation is in progress.
     */
    private static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;

    /**
     * Indicates that the dock is currently being dragged by the user.
     */
    private static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;

    /**
     * Indicates that the dock is in the process of settling to a final position.
     */
    private static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    private static final float TOUCH_SLOP_SENSITIVITY = 1f;

    /**
     * Position layout param.
     */
    private int mPosition;

    /**
     * Dock view contained within the DockLayout.
     */
    private View mDock;

    private int mDockState;
    private WindowManager mWindowManager;

    private final ViewDragHelper mDragger;
    private final ViewDragCallback mDragCallback;


    private float mInitialMotionX;
    private float mInitialMotionY;

    public DockLayout(Context context) {
        this(context, null);
    }

    public DockLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DockLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final float density = getResources().getDisplayMetrics().density;
        final float minVel = MIN_FLING_VELOCITY * density;

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDockState = DOCK_OPENED;

        mDragCallback = new ViewDragCallback();
        mDragger = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mDragCallback);
        mDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
        mDragger.setMinVelocity(minVel);
        // TODO: Check implementation.
        // mDragCallback.setDragger(mDragger);

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
        final int dockLayoutWidth = (int) getResources().getDimension(R.dimen.dock_layout_width);

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                dockLayoutWidth,
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
    protected void onAttachedToWindow() {
        mDock = findViewById(R.id.dock);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        final boolean interceptForDrag = mDragger.shouldInterceptTouchEvent(ev);

        boolean interceptForTap = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                final float x = ev.getRawX();
                final float y = ev.getRawY();
                mInitialMotionX = x;
                mInitialMotionY = y;

                // Should not be needed since we don't have a content view.
                /*
                final View child = mDragger.findTopChildUnder((int) ev.getX(), (int) ev.getY());

                if (child != null && child.getId() != R.id.dock) {
                    interceptForTap = true;
                }
                */

                break;

            case MotionEvent.ACTION_MOVE:
                /*
                if (mDragger.checkTouchSlop(ViewDragHelper.DIRECTION_ALL)) {
                    mDragger.removeCallbacks();
                }
                */
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                close();
                break;
        }

        return interceptForDrag || interceptForTap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mDragger.processTouchEvent(ev);

        int leftMargin;
        int rightMargin;
        final View dock = getDockView();
        final LayoutParams dockLp = (LayoutParams) dock.getLayoutParams();

        final int action = ev.getActionMasked();
        boolean wantTouchEvents = true;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                LOGD(TAG, "DOWN");
                mInitialMotionX = ev.getRawX();
                mInitialMotionY = ev.getRawY();

                // If DockLayout is folded, unfold.
                LOGD(TAG, Integer.toString(this.getLeft()));
                final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) getLayoutParams();

                if (dockLayoutLp.x == -mDock.getWidth()) {
                    dockLayoutLp.x = 0;
                    mWindowManager.updateViewLayout(DockLayout.this, dockLayoutLp);
                    mDock.offsetLeftAndRight(this.getWidth());

                    LOGD(TAG, Integer.toString(mDock.getLeft()));
                }

                break;

            /*
            case MotionEvent.ACTION_MOVE:
                // LOGD(TAG, "MOVE");
                int distance = (int) (ev.getRawX() - mInitialMotionX);
                mInitialMotionX = ev.getRawX();
                mInitialMotionY = ev.getRawY();

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

                return false;
            */

            case MotionEvent.ACTION_UP:
                LOGD(TAG, "UP");
                invalidate(); // TODO: remove if not needed.

                break;

            case MotionEvent.ACTION_CANCEL:
                close();
                break;

            case MotionEvent.ACTION_OUTSIDE:
                close();
                break;
        }

        return wantTouchEvents;
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

    private float getDockOffset() {
        final int dockWidth = mDock.getWidth();
        return 1 - ((float) (mDock.getLeft() - (getWidth() - dockWidth)) / dockWidth);
    }

    /**
     * Draw one child of this View Group. This method is responsible for getting
     * the canvas in the right state. This includes clipping, translating so
     * that the child's scrolled origin is at 0, 0, and applying any animation
     * transformations.
     *
     * @param canvas      The canvas on which to draw the child
     * @param child       Who to draw
     * @param drawingTime The time at which draw is occurring
     * @return True if an invalidate() was issued
     */
    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (mDragger.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }

        return super.drawChild(canvas, child, drawingTime);
    }

    public void open() {
        /*
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
        */
    }

    public void close() {
        /*
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

                    try {
                        mWindowManager.updateViewLayout(DockLayout.this, dockLayoutLp);
                    } catch (IllegalArgumentException e) {
                        // TODO: Find another way to handle stopping the service when the dock is out.
                    }
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
        */
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child.getId() == R.id.dock;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            switch (state) {
                case STATE_IDLE:
                    // If drawer is closed, fold container.
                    if (mDock.getLeft() == getWidth()) {
                        final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) getLayoutParams();
                        dockLayoutLp.x = -mDock.getWidth();
                        mWindowManager.updateViewLayout(DockLayout.this, dockLayoutLp);
                    }

                    break;
            }

            mDockState = state;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final float offset = getDockOffset();
            final int childWidth = releasedChild.getWidth();

            final int width = getWidth();
            final int left = xvel < 0 || xvel == 0 && offset > 0.5f ? width - childWidth : width;

            mDragger.settleCapturedViewAt(left, releasedChild.getTop());
            invalidate();
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            super.onEdgeTouched(edgeFlags, pointerId);
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            mDragger.captureChildView(getDockView(), pointerId);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return child.getWidth();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            final int width = getWidth();
            return Math.max(width - child.getWidth(), Math.min(left, width));
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            invalidate();
        }
    }
}
