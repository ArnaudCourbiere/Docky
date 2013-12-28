package me.courbiere.android.docky.ui.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.net.URISyntaxException;
import java.util.Arrays;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.provider.DockItemsContract;
import me.courbiere.android.docky.ui.activity.AddItem;

import static me.courbiere.android.docky.util.LogUtils.*;

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
        final int dockLayoutWidth = (int) getResources().getDimension(R.dimen.dock_layout_width);

        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                dockLayoutWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
//                WindowManager.LayoutParams.TYPE_PRIORITY_PHONE,
//                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
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

    @Override
    protected void onAttachedToWindow() {
        mDock = findViewById(R.id.dock);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // If the dock is invisible, don't recompute the View's layout to prevent the
        // dock to be repositioned in its initial location. The dock is set to invisible when closed.
        if (mDock.getVisibility() != INVISIBLE) {
            super.onLayout(changed, l, t, r, b);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        final boolean interceptForDrag = mDragger.shouldInterceptTouchEvent(ev);

        boolean interceptForTap = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) getLayoutParams();

                // If DockLayout is folded, unfold.
                if (dockLayoutLp.x == -mDock.getWidth()) {
                    unfoldContainer();
                }
                break;

            case MotionEvent.ACTION_UP:
                final float x = ev.getRawX();
                final float y = ev.getRawY();
                boolean close = true;
                final ListView itemList = (ListView) findViewById(R.id.dock_item_list);
                final int start = itemList.getFirstVisiblePosition();
                final int end = itemList.getLastVisiblePosition();
                final ListAdapter adapter = itemList.getAdapter();

                for (int i = start; i <= end; i++) {
                    Cursor c = (Cursor) adapter.getItem(i);

                    if (c != null) {
                        Intent intent;
                        String intentUri = c.getString(
                                c.getColumnIndex(DockItemsContract.DockItems.INTENT));

                        try {
                            intent = Intent.parseUri(intentUri, 0);
                        } catch (URISyntaxException e) {
                            throw new IllegalArgumentException("Invalid URI found: " + intentUri);
                        }

                        if (intent != null) {
                            String className = intent.getComponent().getClassName();

                            if (className != null && className.equals(AddItem.class.getName())) {
                                LOGD(TAG, "got view");
                                View itemView = itemList.getChildAt(i - start);

                                if (itemView != null) {
                                    int[] location = new int[2];
                                    itemView.getLocationOnScreen(location);

                                    if (x >= location[0] && x <= location[0] + itemView.getWidth()
                                            && y >= location[1] && y <= location[1] + itemView.getHeight()) {
                                        close = false;
                                    }
                                }
                            }
                        }
                    }
                }

                if (close) {
                    close();
                }
                break;
        }

        return interceptForDrag || interceptForTap;
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

                if (dragState == STATE_IDLE && (mDock.getLeft() == getWidth())) {
                    foldContainer();
                }

                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
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
        mDockLayoutHeight = getHeight();
        final WindowManager.LayoutParams dockLayoutLp = (WindowManager.LayoutParams) getLayoutParams();
        dockLayoutLp.x = -mDock.getWidth();
        //dockLayoutLp.height = 100;
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
        //dockLayoutLp.height = mDockLayoutHeight;
        dockLayoutLp.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        mWindowManager.updateViewLayout(DockLayout.this, dockLayoutLp);
    }

    /**
     * Listener used to communicate with the ViewDragHelper.
     */
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
                        foldContainer();
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
            if (mDock.getVisibility() == INVISIBLE) {
                mDock.setVisibility(VISIBLE);
            }

            invalidate();
        }
    }
}
