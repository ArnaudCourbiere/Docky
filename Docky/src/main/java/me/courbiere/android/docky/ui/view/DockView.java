package me.courbiere.android.docky.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Dock View.
 */
public class DockView extends View {
    private static final String TAG = "DockView";
    private Paint mPaint;

    public DockView(Context context) {
        super(context);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(80);
        mPaint.setARGB(255, 255, 0, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText("Hello Workd!", 5, 15, mPaint);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        LOGD(TAG, "onTouchEvent()");
        return false;
    }
}
