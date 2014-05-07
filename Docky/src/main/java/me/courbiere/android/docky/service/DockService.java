package me.courbiere.android.docky.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.HashMap;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.provider.DockItemsContract;
import me.courbiere.android.docky.ui.activity.ManageItemsActivity;
import me.courbiere.android.docky.ui.activity.SettingsActivity;
import me.courbiere.android.docky.ui.adapter.SortableCursorAdapter;
import me.courbiere.android.docky.ui.view.DockLayout;

/**
 * Attaches the DockLayout to the window.
 */
public class DockService extends Service {
    private static final String TAG = "DockService";

    public static final String[] DOCK_ITEM_PROJECTION = {
            DockItemsContract.DockItems._ID,
            DockItemsContract.DockItems.INTENT,
            DockItemsContract.DockItems.TITLE,
            DockItemsContract.DockItems.ICON,
            DockItemsContract.DockItems.STICKY };

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
     * Adapter used to display the dock items.
     */
    private SimpleCursorAdapter mItemsAdapter;

    /**
     * Cursor used with the SimpleCursorAdapter.
     */
    private Cursor mCursor;

    /**
     * Instance variable to check if the service is running.
     * TODO: Find a better way to check if the service is running.
     */
    private static boolean sRunning = false;

    /**
     * DockItem content observer.
     */
    private ContentObserver mDockItemObserver;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        sRunning = true;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREFERENCES_START_DOCK_ON_BOOT, true).commit();

        goToForeground();
        initViews();
        initListeners();

        // Observer for addition and deletion of dock items.
        mDockItemObserver = new DockItemObserver(new Handler());
        getContentResolver().registerContentObserver(
                DockItemsContract.DockItems.CONTENT_URI, true, mDockItemObserver);

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

    /**
     * Runs the service in the foreground (this prevents the service from being terminated
     * when the device is low on memory).
     */
    private void goToForeground() {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_subtitle));

        final Intent notificationIntent = new Intent(this, ManageItemsActivity.class);
        final TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(ManageItemsActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        final PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(
                0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(notificationPendingIntent);

        final Notification notification = notificationBuilder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification.priority |= Notification.PRIORITY_MIN;
        }

        startForeground(1337, notification);
    }

    /**
     * Queries the dock items and sets the adapter on the dock list view.
     */
    private void initViews() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        mDockLayout = (DockLayout) inflater.inflate(R.layout.dock_layout, null);
        mDock = (LinearLayout) mDockLayout.findViewById(R.id.dock);
        mItemList = (ListView) mDockLayout.findViewById(R.id.dock_item_list);

        final String[] from = new String[] {DockItemsContract.DockItems.ICON };
        final int[] to = new int[] { R.id.app_icon };

        mCursor = getContentResolver().query(DockItemsContract.DockItems.CONTENT_URI,
                DOCK_ITEM_PROJECTION, null, null, DockItemsContract.DockItems.POSITION + " ASC");
        mItemsAdapter = new SortableCursorAdapter(getBaseContext(), R.layout.dock_item_layout,
                mCursor, from, to, 0);

        // TODO: Improvement: use the holder pattern.

        // Use custom binder.
        final SimpleCursorAdapter.ViewBinder binder = new DockItemViewBinder();

        mItemsAdapter.setViewBinder(binder);
        mItemList.setAdapter(mItemsAdapter);
    }

    /**
     * Sets up the click listener responsible for launching apps.
     */
    private void initListeners() {
        mItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                final String intentUri = cursor.getString(
                        cursor.getColumnIndex(DockItemsContract.DockItems.INTENT));
                final String appName = cursor.getString(
                        cursor.getColumnIndex(DockItemsContract.DockItems.TITLE));

                try {
                    final Intent intent = Intent.parseUri(intentUri, 0);
                    final Handler handler = new Handler(Looper.getMainLooper());
                    final Runnable checkAppOnTop = new Runnable() {
                        @Override
                        public void run() {
                            ActivityManager activityManager =
                                    (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                            ActivityManager.RunningTaskInfo runningTaskInfo =
                                    activityManager.getRunningTasks(1).get(0);

                            final String sourcePackage = intent.getComponent().getPackageName();
                            final String targetPackage =
                                    runningTaskInfo.topActivity.getPackageName();

                            if (!sourcePackage.equals(targetPackage)) {
                                Toast.makeText(
                                        DockService.this,
                                        "Launching " + appName,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    final Runnable launchApp = new Runnable() {
                        @Override
                        public void run() {
                            final Bundle options = ActivityOptionsCompat
                                    .makeScaleUpAnimation(view, 0, 0, 0, 0)
                                    .toBundle();
                            startActivity(intent, options);
                            handler.postDelayed(checkAppOnTop, 100);
                        }
                    };

                    handler.postDelayed(launchApp, 200);
                } catch (URISyntaxException e) {

                }
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
        sRunning = false;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREFERENCES_START_DOCK_ON_BOOT, false).commit();

        if (mDockLayout != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDockLayout);
            mDockLayout = null;
        }

        if (mCursor != null) {
            mCursor.close();
        }

        if (mDockItemObserver != null) {
            getContentResolver().unregisterContentObserver(mDockItemObserver);
        }
    }

    /**
     * Listens for addition and deletion of dock items and updates the dock.
     */
    private class DockItemObserver extends ContentObserver {
        public DockItemObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mCursor = getContentResolver().query(DockItemsContract.DockItems.CONTENT_URI,
                    DOCK_ITEM_PROJECTION, null, null, DockItemsContract.DockItems.POSITION + " ASC");
            mItemsAdapter.changeCursor(mCursor);
        }
    }

    /**
     * Custom ViewBinder used to set the icon of the dock items.
     */
    private class DockItemViewBinder implements SimpleCursorAdapter.ViewBinder {

        /**
         * Icon cache.
         */
        private HashMap<String, Bitmap> mCache = new HashMap<>(50);

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            final String columnName = cursor.getColumnName(columnIndex);

            if (columnName.equals(DockItemsContract.DockItems.ICON)) {
                final ImageView imageView = (ImageView) view;
                final String intentUri = cursor.getString(
                        cursor.getColumnIndex(DockItemsContract.DockItems.INTENT));

                Bitmap iconBitmap = mCache.get(intentUri);

                if (iconBitmap == null) {
                    final byte[] icon = cursor.getBlob(columnIndex);
                    iconBitmap = BitmapFactory.decodeByteArray(icon, 0, icon.length);
                    mCache.put(intentUri, iconBitmap);
                }

                imageView.setImageBitmap(iconBitmap);

                return true;
            }

            return false;
        }
    }
}
