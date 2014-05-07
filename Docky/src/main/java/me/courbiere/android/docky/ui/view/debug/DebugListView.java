package me.courbiere.android.docky.ui.view.debug;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Created by arnaud on 5/6/14.
 */
public class DebugListView extends ListView {
    private static final String TAG = "DebugListView";

    public DebugListView(Context context) {
        super(context);
    }

    public DebugListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DebugListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        LOGD(TAG, "onLayout()");
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void layoutChildren() {
        LOGD(TAG, "layoutChildren()");
        super.layoutChildren();
    }

    @Override
    public void removeViews(int start, int count) {
        LOGD(TAG, "removeViews()");
        super.removeViews(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        LOGD(TAG, "removeViewsInLayout()");
        super.removeViewsInLayout(start, count);
    }

    @Override
    protected boolean addViewInLayout(View child, int index, ViewGroup.LayoutParams params, boolean preventRequestLayout) {
        LOGD(TAG, "addViewInLayout()");
        return super.addViewInLayout(child, index, params, preventRequestLayout);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        LOGD(TAG, "addView()");
        super.addView(child, index, params);
    }

    @Override
    protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
        LOGD(TAG, "attachViewToParent()");
        super.attachViewToParent(child, index, params);
    }

    @Override
    protected void detachAllViewsFromParent() {
        LOGD(TAG, "detachAllViewsFromParent()");
        super.detachAllViewsFromParent();
    }

    @Override
    protected void detachViewFromParent(int index) {
        LOGD(TAG, "detachViewFromParent()");
        super.detachViewFromParent(index);
    }

    @Override
    protected void detachViewsFromParent(int start, int count) {
        LOGD(TAG, "detachViewsFromParent()");
        super.detachViewsFromParent(start, count);
    }

    @Override
    protected void detachViewFromParent(View child) {
        LOGD(TAG, "detachViewFromParent()");
        super.detachViewFromParent(child);
    }

    @Override
    public void removeAllViewsInLayout() {
        LOGD(TAG, "removeAllViewsInLayout()");
        super.removeAllViewsInLayout();
    }

    @Override
    public void bringChildToFront(View child) {
        LOGD(TAG, "bringChildToFront()");
        super.bringChildToFront(child);
    }

    @Override
    public void requestLayout() {
        LOGD(TAG, "requestLayout()");
        super.requestLayout();
    }
}
