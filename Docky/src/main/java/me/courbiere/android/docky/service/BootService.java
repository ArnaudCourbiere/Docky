package me.courbiere.android.docky.service;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.courbiere.android.docky.MainActivity;
import me.courbiere.android.docky.R;
import me.courbiere.android.docky.item.AppInfo;
import me.courbiere.android.docky.ui.adapter.DockItemArrayAdapter;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Bootstraps the dock View and Gesture Listeners.
 */
public class BootService extends Service {
    private static final String TAG = "BootService";

    private static final int DOCK_CLOSED = 0;
    private static final int DOCK_OPENED = 1;

    private ArrayList<AppInfo> mApplications;

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
    private LinearLayout mDock;

    /**
     * Dock item list
     */
    private ListView mItemList;

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

        loadApplications(true);

        goToForeground();
        initListView();
        initListeners();

        mDockState = DOCK_OPENED;

        final int dockWidth = (int) getResources().getDimension(R.dimen.dock_layout_width);

        mDockLayoutLp = new WindowManager.LayoutParams(
                dockWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mDockLayoutLp.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
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
        mDock = (LinearLayout) mDockLayout.findViewById(R.id.dock);
        mItemList = (ListView) mDockLayout.findViewById(R.id.dock_item_list);
        mDragHandle = mDockLayout.findViewById(R.id.drag_handle);

        mDockLp = (LinearLayout.LayoutParams) mDock.getLayoutParams();
        mDragHandleLp = (LinearLayout.LayoutParams) mDragHandle.getLayoutParams();

        final ArrayAdapter<AppInfo> adapter = new DockItemArrayAdapter(this, R.layout.dock_item_layout, mApplications);
        mItemList.setAdapter(adapter);
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

                        return false;

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
        mItemList.setOnTouchListener(mDragListener);

        mDockLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                    closeDock();
                }

                return false;
            }
        });

        mItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final AppInfo app = (AppInfo) parent.getAdapter().getItem(position);
                closeDock();
                startActivity(app.intent);
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
     * Loads the list of installed applications in mApplications.
     *
     * @param isLaunching indicates whether the Activity is launching or not.
     */
    private void loadApplications(boolean isLaunching) {
        if (isLaunching && mApplications != null) {
            return;
        }

        final PackageManager manager = getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        if (apps != null) {
            final int count = apps.size();

            if (mApplications == null) {
                mApplications = new ArrayList<>(count);
            }

            mApplications.clear();

            for (int i = 0; i < count; i++) {
                final AppInfo application = new AppInfo();
                final ResolveInfo info = apps.get(i);

                application.title = info.loadLabel(manager);

                application.setActivity(new ComponentName(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name),
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                final int iconId = info.getIconResource();
                final ActivityManager activityManager = (ActivityManager)
                        this.getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
                final int iconDpi = activityManager.getLauncherLargeIconDensity();

                try {
                    final Resources resources = manager.getResourcesForApplication(
                            info.activityInfo.applicationInfo);
                    application.icon = resources.getDrawableForDensity(iconId, iconDpi);
                } catch (PackageManager.NameNotFoundException e) {
                    application.icon = info.activityInfo.loadIcon(manager);
                }

                mApplications.add(application);
            }
        }
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
