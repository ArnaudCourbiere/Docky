package me.courbiere.android.docky.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
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
    private RelativeLayout mDockLayout;

    /**
     * Dock.
     */
    private ListView mDock;

    /**
     * Gesture Detector used to swipe Dock in and out.
     */
    private GestureDetector mDetector;

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

        final int dockWidth = (int) getResources().getDimension(R.dimen.dock_width);

        mParams = new WindowManager.LayoutParams(
                dockWidth,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
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
        mDockLayout = (RelativeLayout) inflater.inflate(R.layout.dock_layout, null);
        mDock = (ListView) mDockLayout.findViewById(R.id.dock);
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

        mDock.setOnTouchListener(new View.OnTouchListener() {
            private int mRightBound = 0;
            private int mLeftBound = mDock.getWidth();
            private float mInitialTouchX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //return mDetector.onTouchEvent(event);

                final int action = event.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mInitialTouchX = event.getRawX();
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        int distance = (int) (event.getRawX() - mInitialTouchX);
                        mInitialTouchX = event.getRawX();
                        mRightBound += distance;
                        mLeftBound += distance;
                        final RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mDock.getLayoutParams();
                        int leftMargin = lp.leftMargin + distance;
                        int rightMargin = lp.rightMargin - distance;

                        if (leftMargin < 0) {
                            leftMargin = 0;
                            rightMargin = 0;
                        }
                        if (leftMargin > mDock.getWidth()) {
                            leftMargin = mDock.getWidth();
                            leftMargin = -mDock.getWidth();
                        }

                        lp.setMargins(leftMargin, lp.topMargin, rightMargin, lp.bottomMargin);
                        mDock.setLayoutParams(lp);

                        //if (mParams.x < 0) {
                            //mParams.x = 0;
                        //}

                        //mDock.layout(mLeftBound, 0, mRightBound, 0);
                        //mWindowManager.updateViewLayout(mDock, mParams);
                        return true;
                }

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
