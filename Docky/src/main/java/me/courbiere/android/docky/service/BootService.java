package me.courbiere.android.docky.service;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

import me.courbiere.android.docky.MainActivity;
import me.courbiere.android.docky.R;
import me.courbiere.android.docky.ui.adapter.DockItemArrayAdapter;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Bootstraps the dock View and Gesture Listeners.
 */
public class BootService extends Service {
    private static final String TAG = "BootService";

    private static final int DOCK_CLOSED = 0;
    private static final int DOCK_OPENED = 1;

    /**
     * Dock state
     */
    private int mDockState;

    /**
     * Window Manager.
     */
    private WindowManager mWindowManager;

    /**
     * Dock Layout.
     */
    private LinearLayout mDockLayout;

    /**
     * Dock.
     */
    private ListView mDock;

    /**
     * Drag handle.
     */
    private View mDragHandle;

    /**
     * Dock Layout layout params.
     */
    private WindowManager.LayoutParams mDockLayoutLp;

    /**
     * Dock layout parameters.
     */
    private LinearLayout.LayoutParams mDockLp;

    /**
     * Drag Handle layout parameters.
     */
    private LinearLayout.LayoutParams mDragHandleLp;

    /**
     * Gesture Detector used to swipe Dock in and out.
     */
    private GestureDetector mDetector;

    /**
     * On Touch listener to handle Dock dragging.
     */
    private View.OnTouchListener mDragListener;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        // TODO
        LOGD(TAG, "onCreate()");

        goToForeground();
        initListView();
        initListeners();

        mDockState = DOCK_OPENED;

        final int dockWidth = (int) getResources().getDimension(R.dimen.dock_layout_width);

        mDockLayoutLp = new WindowManager.LayoutParams(
                dockWidth,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mDockLayoutLp.gravity = Gravity.RIGHT;
        mDockLayoutLp.setTitle(getString(R.string.app_name));
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mDockLayout, mDockLayoutLp);
    }

    private void goToForeground() {
        final Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Aloha!");

        final Notification notification = notificationBuilder.getNotification();
        notification.priority |= Notification.PRIORITY_MIN;

        final Intent notificationIntent = new Intent(this, MainActivity.class);
        final PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(notificationPendingIntent);
        startForeground(1337, notification);
    }

    private void initListView() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        mDockLayout = (LinearLayout) inflater.inflate(R.layout.dock_layout, null);
        mDock = (ListView) mDockLayout.findViewById(R.id.dock);
        mDragHandle = mDockLayout.findViewById(R.id.drag_handle);

        mDockLp = (LinearLayout.LayoutParams) mDock.getLayoutParams();
        mDragHandleLp = (LinearLayout.LayoutParams) mDragHandle.getLayoutParams();

        // Settings fake values in dock for development.
        final String[] values = new String[] { "", "", "", "", "", "" };
        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            list.add(values[i]);
        }
        final ArrayAdapter<String> adapter = new DockItemArrayAdapter(this, R.layout.dock_item_layout, list);
        mDock.setAdapter(adapter);
    }

    private void initListeners() {
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
         mDragListener = new View.OnTouchListener() {
            private float mInitialTouchX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //return mDetector.onTouchEvent(event);

                int leftMargin;
                int rightMargin;

                final int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        LOGD(TAG, "DOWN");
                        mInitialTouchX = event.getRawX();

                        /*
                        dockLayoutLp.width = dockLayoutWidth;
                        mWindowManager.updateViewLayout(mDockLayout, dockLayoutLp);
                        */

                        if (v.getId() == R.id.drag_handle) {
                            return true;
                        } else {
                            return false;
                        }

                    case MotionEvent.ACTION_MOVE:
                        LOGD(TAG, "MOVE");
                        int distance = (int) (event.getRawX() - mInitialTouchX);
                        mInitialTouchX = event.getRawX();

                        if (mDockState == DOCK_OPENED) {
                            // Move dock and drag handle inside dock layout.
                            leftMargin = mDockLp.leftMargin + distance;
                            rightMargin = mDockLp.rightMargin - distance;

                            if (leftMargin < 0) {
                                leftMargin = 0;
                                rightMargin = 0;
                            }
                            if (leftMargin > mDock.getWidth()) {
                                leftMargin = mDock.getWidth();
                                rightMargin = -mDock.getWidth();
                            }

                            // Update dock position.
                            mDockLp.setMargins(
                                    leftMargin,
                                    mDockLp.topMargin,
                                    rightMargin,
                                    mDockLp.bottomMargin);

                            mDock.setLayoutParams(mDockLp);

                            // Update drag handle position.
                            mDragHandleLp.setMargins(leftMargin,
                                    mDragHandleLp.topMargin,
                                    rightMargin,
                                    mDragHandleLp.bottomMargin);

                            mDragHandle.setLayoutParams(mDragHandleLp);
                        } else {
                            // Slide dock layout inside window on the x axis.
                            mDockLayoutLp.x -= distance;

                            if (mDockLayoutLp.x > 0) {
                                mDockLayoutLp.x = 0;
                            }

                            mWindowManager.updateViewLayout(mDockLayout, mDockLayoutLp);
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

                        return true;

                    case MotionEvent.ACTION_UP:
                        LOGD(TAG, "UP");

                        if (mDockLp.leftMargin > mDock.getWidth() / 2) {
                            closeDock();
                        } else {
                            openDock();
                        }

                        return false;
                }

                return false;
            }
        };

        mDock.setOnTouchListener(mDragListener);
        mDragHandle.setOnTouchListener(mDragListener);

        mDockLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                    closeDock();
                }

                return false;
            }
        });
    }

    private void openDock() {

        // Slide Dock Layout into the window.
        mDockLayoutLp.x = 0;
        mWindowManager.updateViewLayout(mDockLayout, mDockLayoutLp);

        // Update dock position.
        mDockLp.setMargins(0, mDockLp.topMargin, 0, mDockLp.bottomMargin);
        mDock.setLayoutParams(mDockLp);

        // Update drag handle position.
        mDragHandleLp.setMargins(0, mDragHandleLp.topMargin, 0, mDragHandleLp.bottomMargin);
        mDragHandle.setLayoutParams(mDragHandleLp);

        mDockState = DOCK_OPENED;
    }

    private void closeDock() {
        float startFactor = mDockLp.leftMargin / (float) mDock.getWidth();

        ValueAnimator val = ValueAnimator.ofFloat(startFactor, 1f);
        val.setDuration(100);
        val.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float factor = (float) animation.getAnimatedValue();

                int leftMargin = (int) (mDock.getWidth() * factor);
                int rightMargin = -leftMargin;

                // Slide Dock Layout off the window.
                if (factor == 1f) {
                    leftMargin = 0;
                    rightMargin = 0;

                    if (mDockLayout != null) {
                        mDockLayoutLp.x = -mDock.getWidth();
                        mWindowManager.updateViewLayout(mDockLayout, mDockLayoutLp);
                    }
                }

                // Update dock position.
                mDockLp.setMargins(
                        leftMargin,
                        mDockLp.topMargin,
                        rightMargin,
                        mDockLp.bottomMargin);
                mDock.setLayoutParams(mDockLp);

                // Update drag handle position.
                mDragHandleLp.setMargins(
                        leftMargin,
                        mDragHandleLp.topMargin,
                        rightMargin,
                        mDragHandleLp.bottomMargin);
                mDragHandle.setLayoutParams(mDragHandleLp);
            }
        });

        val.start();

        mDockState = DOCK_CLOSED;
    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link android.content.Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        // TODO
        LOGD(TAG, "onDestroy()");
        if (mDockLayout != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDockLayout);
            mDockLayout = null;
        }
    }
}
