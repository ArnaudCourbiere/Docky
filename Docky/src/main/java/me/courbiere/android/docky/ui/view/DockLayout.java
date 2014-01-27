package me.courbiere.android.docky.ui.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.ui.activity.SettingsActivity;

import static me.courbiere.android.docky.util.LogUtils.LOGD;

/**
 * Dock Layout. This View is responsible for managing the dock positioning, it sits on top of every
 * other window. This View is intercepting motion events and determines if the motion events
 * should be forwarded to the actual dock (ex: click on a list item) or if the user is trying
 * to drag the dock in and out from the edge.
 */
public class DockLayout extends RelativeLayout {
    private static final String TAG = "DockLayout";

    /**
     * Indicates that the dock is in an idle, settled state. No animation is in progress.
     */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;

    /**
     * Indicates that the dock is currently being dragged by the user.
     */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;

    /**
     * Indicates that the dock is in the process of settling to a final position.
     */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    /**
     * The dock is unlocked.
     */
    public static final int LOCK_MODE_UNLOCKED = 0;

    /**
     * The dock is locked in closed mode. The app may open it, not the user.
     */
    public static final int LOCK_MODE_LOCKED_CLOSED = 1;

    /**
     * The dock is locked open, the app may close it, not the user.
     */
    public static final int LOCK_MODE_LOCKED_OPEN = 2;

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    /**
     * Multiplier for how sensitive the helper should be about detecting the start of a drag.
     * Larger values are more sensitive. 1.0f is normal.
     */
    private static final float TOUCH_SLOP_SENSITIVITY = 1f;

    /**
     * Position layout param.
     * TODO: Remove this layout parameter
     */
    private int mPosition;

    /**
     * Dock Layout width.
     */
    private final int mDockLayoutWidth;

    /**
     * Dock Layout height(used to restore layout width on unfold).
     */
    private int mDockLayoutHeight;

    /**
     * Dock view contained within the DockLayout.
     */
    private View mDock;

    /**
     * Window Manager. Needed to attach the DockLayout to the window (on top of every other window)
     * and to resize the DockLayout.
     */
    private WindowManager mWindowManager;

    /**
     * DragHelper used to interpret motion events and drag the dock.
     */
    private final ViewDragHelper mDragger;

    /**
     * ViewDragHelper.Callback used to communicate with the ViewDragHelper and receive callbacks.
     */
    private final ViewDragCallback mDragCallback;

    /**
     * Listener for shared preferences changes.
     */
    private final PreferenceChangeListener mPreferenceChangeListener;

    /**
     * Dock current lock mode.
     */
    private int mLockMode = LOCK_MODE_UNLOCKED;

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

        mDockLayoutWidth = (int) getResources().getDimension(R.dimen.dock_layout_width);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mPreferenceChangeListener = new PreferenceChangeListener();
        mDragCallback = new ViewDragCallback();
        mDragger = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mDragCallback);
        mDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT | ViewDragHelper.EDGE_LEFT);
        mDragger.setMinVelocity(minVel);

        // TODO: Use when implementing choice of dock position (left or right).
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

    // TODO: Remove get and set position as well as custom view attribute.
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

    /**
     * Attaches this DockLayout to the window. This DockLayout will be drawn on top of all other windows.
     */
    public void attachToWindow() {
        mDock = findViewById(R.id.dock);
        setDockBackground(PreferenceManager.getDefaultSharedPreferences(getContext()));

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                mDockLayoutWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT);

        layoutParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        layoutParams.setTitle(getContext().getString(R.string.app_name));
        mWindowManager.addView(this, layoutParams);
    }

    /**
     * Helper function to set the dock background from preferences.
     */
    private void setDockBackground(SharedPreferences sharedPreferences) {
        int drawableId;

        switch (sharedPreferences.getString(SettingsActivity.PREFERENCES_STYLE, SettingsActivity.STYLE_WHITE)) {
            case SettingsActivity.STYLE_BLACK:
                drawableId = R.drawable.dock_background_black_rounded;
                break;

            case SettingsActivity.STYLE_WHITE:
            default:
                drawableId = R.drawable.dock_background_white_rounded;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mDock.setBackground(getResources().getDrawable(drawableId));
        } else {
            mDock.setBackgroundDrawable(getResources().getDrawable(drawableId));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    }

    /*
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChild(mDock, widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mDockLayoutWidth, mDock.getMeasuredHeight());
    }
    */

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int marginTop= (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());

        int left = getMeasuredWidth() - mDock.getMeasuredWidth();
        int right = mDock.getMeasuredWidth() + left;

        if (mDock.getLeft() != (getWidth() - mDock.getWidth()) * 2) {
            left += mDock.getLeft();
            right += mDock.getLeft();
        }

        mDock.layout(left, t + marginTop, right, b);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        final boolean interceptForDrag = mDragger.shouldInterceptTouchEvent(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) getLayoutParams();

                // If DockLayout is folded, unfold.
                if (dockLayoutLp.x == -mDock.getWidth()) {
                    unfoldContainer();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (getLockMode() == LOCK_MODE_UNLOCKED) {
                    close();
                }
                break;
        }

        return interceptForDrag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mDragger.processTouchEvent(ev);

        final int action = ev.getActionMasked();
        boolean wantTouchEvents = true;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;

            case MotionEvent.ACTION_UP:
                final int dragState = mDragger.getViewDragState();

                if ((dragState == STATE_IDLE || dragState == STATE_SETTLING)
                        && (mDock.getLeft() >= getWidth())) {
                    foldContainer();
                }

                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                if (getLockMode() == LOCK_MODE_UNLOCKED) {
                    close();
                }
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

    /**
     * Returns the dock positioning offset, 1 is fully open, 0 is fully closed.
     *
     * @return dock positioning offset.
     */
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

    /**
     * Enable or disable interaction with the dock.
     *
     * @param lockMode The new lock mode for the dock.
     *
     * @see #LOCK_MODE_UNLOCKED
     * @see #LOCK_MODE_LOCKED_CLOSED
     * @see #LOCK_MODE_LOCKED_OPEN
     */
    public void setLockMode(int lockMode) {
        mLockMode = lockMode;

        if (lockMode != LOCK_MODE_UNLOCKED) {
            mDragger.cancel();
        }

        switch (lockMode) {
            case LOCK_MODE_LOCKED_OPEN:
                open();
                break;
            case LOCK_MODE_LOCKED_CLOSED:
                close();
                break;
        }
    }

    public int getLockMode() {
        return mLockMode;
    }

    /**
     * Smoothly open the dock.
     */
    public void open() {
        mDragger.smoothSlideViewTo(mDock, getWidth() - mDock.getWidth(), mDock.getTop());
        invalidate();
    }

    /**
     * Smoothly close the dock.
     */
    public void close() {
        mDragger.smoothSlideViewTo(mDock, getWidth(), mDock.getTop());
        invalidate();
    }

    /**
     * Fold this DockLayout. This function is called when the dock has settled in closed position.
     */
    private void foldContainer() {
        mDockLayoutHeight = getHeight(); // TODO: remove if not needed.
        final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) getLayoutParams();
        dockLayoutLp.x = -mDock.getWidth();
//        dockLayoutLp.width= mDockLayoutWidth;
//        dockLayoutLp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dockLayoutLp.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        mWindowManager.updateViewLayout(DockLayout.this, dockLayoutLp);
        mDock.setVisibility(INVISIBLE);
    }

    /**
     * Unfold this DockLayout. This function is called when the dock has settled in opened position.
     */
    private void unfoldContainer() {
        final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) getLayoutParams();
        dockLayoutLp.x = 0;
//        dockLayoutLp.width= WindowManager.LayoutParams.MATCH_PARENT;
//        dockLayoutLp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dockLayoutLp.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        mWindowManager.updateViewLayout(DockLayout.this, dockLayoutLp);
    }

    /**
     * Listener used to communicate with the ViewDragHelper.
     */
    private class ViewDragCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child.getId() == R.id.dock && getLockMode() == LOCK_MODE_UNLOCKED;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            switch (state) {
                case STATE_IDLE:
                    // If drawer is closed, fold container.
                    if (mDock.getLeft() >= getWidth()) {
                        foldContainer();
                        /*
                    } else {
                        final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) getLayoutParams();
                        dockLayoutLp.width = mDockLayoutWidth;
                        dockLayoutLp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        mWindowManager.updateViewLayout(DockLayout.this, dockLayoutLp);
                        */
                    }

                    break;
            }
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
            if (getLockMode() == LOCK_MODE_UNLOCKED) {
                mDragger.captureChildView(getDockView(), pointerId);
            }
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
            if (mDock.getVisibility() == INVISIBLE) {
                mDock.setVisibility(VISIBLE);
            }

            invalidate();
        }
    }

    /**
     * Listener used to update the dock on preference changes.
     */
    private class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        /**
         * Called when a shared preference is changed, added, or removed. This
         * may be called even if a preference is set to its existing value.
         * <p/>
         * <p>This callback will be run on your main thread.
         *
         * @param sharedPreferences The {@link android.content.SharedPreferences} that received
         *                          the change.
         * @param key               The key of the preference that was changed, added, or
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (mDock != null && key.equals(SettingsActivity.PREFERENCES_STYLE)) {
                setDockBackground(sharedPreferences);
            }
        }
    }
}
