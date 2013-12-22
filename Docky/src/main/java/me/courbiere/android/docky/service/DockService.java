package me.courbiere.android.docky.service;

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
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.courbiere.android.docky.MainActivity;
import me.courbiere.android.docky.R;
import me.courbiere.android.docky.item.AppInfo;
import me.courbiere.android.docky.ui.adapter.DockItemArrayAdapter;
import me.courbiere.android.docky.ui.view.DockLayout;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Attaches the DockLayout to the window.
 */
public class DockService extends Service {
    private static final String TAG = "DockService";

    private ArrayList<AppInfo> mApplications;

    /**
     * Dock Layout.
     */
    private DockLayout mDockLayout;

    /**
     * Dock.
     */
    private LinearLayout mDock;

    /**
     * Dock item list
     */
    private ListView mItemList;

    /**
     * Instance variable to check if the service is running.
     * TODO: Find a better way to check if the service is running.
     */
    private static boolean sRunning = false;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        // TODO
        LOGD(TAG, "onCreate()");
        sRunning = true;

        loadApplications(true);

        goToForeground();
        initListView();
        // initListeners();

        mDockLayout.attachToWindow();
    }

    /**
     * Determines if the DockService is running.
     *
     * @return returns true if the service is running, false otherwise.
     */
    public static boolean isRunning() {
        return sRunning;
    }

    private void goToForeground() {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Aloha!");

        final Notification notification = notificationBuilder.getNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification.priority |= Notification.PRIORITY_MIN;
        }

        final Intent notificationIntent = new Intent(this, MainActivity.class);
        final PendingIntent notificationPendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(notificationPendingIntent);

        startForeground(1337, notification);
        /*
        Notification notification = new Notification(R.drawable.ic_launcher, getText(R.string.app_name),
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getText(R.string.app_name),
                "Aloha!", pendingIntent);
        startForeground(1337, notification);
        */
    }

    private void initListView() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        mDockLayout = (DockLayout) inflater.inflate(R.layout.dock_layout, null);
        mDock = (LinearLayout) mDockLayout.findViewById(R.id.dock);
        mItemList = (ListView) mDockLayout.findViewById(R.id.dock_item_list);

        final ArrayAdapter<AppInfo> adapter = new DockItemArrayAdapter(this, R.layout.dock_item_layout, mApplications);
        mItemList.setAdapter(adapter);
    }

    private void initListeners() {
        mItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LOGD(TAG, "item click");
                final AppInfo app = (AppInfo) parent.getAdapter().getItem(position);
                startActivity(app.intent);
            }
        });
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
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
                    } catch (RuntimeException e) {
                        // TODO: Look back at example for handling resource not found.
                    }
                } else {
                    application.icon = info.loadIcon(manager);
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
        sRunning = false;

        if (mDockLayout != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDockLayout);
            mDockLayout = null;
        }
    }
}
