package me.courbiere.android.docky.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import me.courbiere.android.docky.R;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Dock View.
 */
public class DockView extends ViewGroup {
    private static final String TAG = "DockView";

    public static final int POSITION_LEFT = 0;
    public static final int POSITION_RIGHT = 1;

    private int mPosition;

    public DockView(Context context) {
        super(context);
    }

    public DockView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Retrieve custom attributes
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DockView, 0, 0);

        try {
            mPosition = a.getInteger(R.styleable.DockView_position, 0);
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        LOGD(TAG, "onTouchEvent()");

        return super.onTouchEvent(event);
    }
}
