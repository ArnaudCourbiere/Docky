package me.courbiere.android.docky.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import me.courbiere.android.docky.MainActivity;
import me.courbiere.android.docky.R;
import me.courbiere.android.docky.ui.adapter.DockItemArrayAdapter;
import me.courbiere.android.docky.ui.view.DockView;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Bootstraps the dock View and Gesture Listeners.
 */
public class BootService extends Service {
    private static final String TAG = "BootService";

    /**
     * Window Manager.
     */
    private WindowManager mWindowManager;

    /**
     * Dock's layout params.
     */
    private WindowManager.LayoutParams mParams;

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

        final int dockWidth = (int) getResources().getDimension(R.dimen.dock_layout_width);

        mParams = new WindowManager.LayoutParams(
                dockWidth,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                //WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mParams.gravity = Gravity.RIGHT;
        mParams.setTitle(getString(R.string.app_name));
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mDockLayout, mParams);
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
        final String[] values = new String[] { "Arnaud", "Julien", "Andre", "Dominique" };
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

                final WindowManager.LayoutParams dockLayoutLp =
                        (WindowManager.LayoutParams) mDockLayout.getLayoutParams();
                final LinearLayout.LayoutParams dockLp =
                        (LinearLayout.LayoutParams) mDock.getLayoutParams();
                final LinearLayout.LayoutParams dragHandleLp =
                        (LinearLayout.LayoutParams) mDragHandle.getLayoutParams();

                final int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        LOGD(TAG, "DOWN");
                        mInitialTouchX = event.getRawX();

                        if (v.getId() == R.id.drag_handle) {
                            return true;
                        } else {
                            return false;
                        }

                    case MotionEvent.ACTION_MOVE:
                        LOGD(TAG, "MOVE");
                        int distance = (int) (event.getRawX() - mInitialTouchX);
                        mInitialTouchX = event.getRawX();
                        leftMargin = dockLp.leftMargin + distance;
                        rightMargin = dockLp.rightMargin - distance;

                        if (leftMargin < 0) {
                            leftMargin = 0;
                            rightMargin = 0;
                        }
                        if (leftMargin > mDock.getWidth()) {
                            leftMargin = mDock.getWidth();
                            rightMargin = -mDock.getWidth();
                        }

                        /*
                        dockLp.setMargins(leftMargin, dockLp.topMargin, rightMargin, dockLp.bottomMargin);
                        mDock.setLayoutParams(dockLp);

                        // Update drag handle position.
                        dragHandleLp.setMargins(leftMargin, dragHandleLp.topMargin, rightMargin, dragHandleLp.bottomMargin);
                        mDragHandle.setLayoutParams(dragHandleLp);
                        */

                        /*
                        final int dockLayoutWidth = (int) getResources().getDimension(R.dimen.dock_layout_width);
                        final int dragHandleWidth = (int) getResources().getDimension(R.dimen.drag_handle_width);

                        if (dockLayoutLp.width - distance > dockLayoutWidth) {
                            dockLayoutLp.width = dockLayoutWidth;
                        } else if (dockLayoutLp.width - distance < dragHandleWidth) {
                            dockLayoutLp.width = dragHandleWidth;
                        } else {
                            dockLayoutLp.width -= distance;
                        }

                        mWindowManager.updateViewLayout(mDockLayout, dockLayoutLp);
                        */

                        dockLayoutLp.x -= distance;
                        mWindowManager.updateViewLayout(mDockLayout, dockLayoutLp);

                        return true;

                    case MotionEvent.ACTION_UP:
                        LOGD(TAG, "UP");
                        leftMargin = dockLp.leftMargin;
                        rightMargin = dockLp.rightMargin;

                        if (leftMargin > mDock.getWidth() / 2) {
                            leftMargin = mDock.getWidth();
                            rightMargin = -mDock.getWidth();
                        } else {
                            leftMargin = 0;
                            rightMargin = 0;
                        }

                        /*
                        dockLp.setMargins(leftMargin, dockLp.topMargin, rightMargin, dockLp.bottomMargin);
                        mDock.setLayoutParams(dockLp);

                        // Update drag handle position.
                        dragHandleLp.setMargins(leftMargin, dragHandleLp.topMargin, rightMargin, dragHandleLp.bottomMargin);
                        mDragHandle.setLayoutParams(dragHandleLp);
                        */

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
                return false;
            }
        });
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
