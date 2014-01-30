package me.courbiere.android.docky.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
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
import me.courbiere.android.docky.ui.view.DockLayout;

import static me.courbiere.android.docky.util.LogUtils.*;

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
        initListView();

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

    private void initListView() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        mDockLayout = (DockLayout) inflater.inflate(R.layout.dock_layout, null);
        mDock = (LinearLayout) mDockLayout.findViewById(R.id.dock);
        mItemList = (ListView) mDockLayout.findViewById(R.id.dock_item_list);
        mItemList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getBaseContext(), "Long click", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        final String[] from = new String[] {DockItemsContract.DockItems.ICON };
        final int[] to = new int[] { R.id.app_icon };

        mCursor = getContentResolver().query(DockItemsContract.DockItems.CONTENT_URI,
                DOCK_ITEM_PROJECTION, null, null, DockItemsContract.DockItems.POSITION + " ASC");
        mItemsAdapter = new SimpleCursorAdapter(getBaseContext(), R.layout.dock_item_layout,
                mCursor, from, to, 0);

        // TODO: Improvement: use the holder pattern.

        // Use custom binder.
        final SimpleCursorAdapter.ViewBinder binder = new DockItemViewBinder();

        mItemsAdapter.setViewBinder(binder);
        mItemList.setAdapter(mItemsAdapter);
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
        private HashMap<ComponentName, Bitmap> mCache = new HashMap<>(50);

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            final String columnName = cursor.getColumnName(columnIndex);

            if (columnName.equals(DockItemsContract.DockItems.ICON)) {
                final ImageView imageView = (ImageView) view;
                final String intentUri = cursor.getString(
                        cursor.getColumnIndex(DockItemsContract.DockItems.INTENT));
                final String appName = cursor.getString(
                        cursor.getColumnIndex(DockItemsContract.DockItems.TITLE));

                try {
                    final Intent intent = Intent.parseUri(intentUri, 0);
                    final ComponentName component = intent.getComponent();
                    Bitmap iconBitmap = mCache.get(component);

                    if (iconBitmap == null) {
                        final byte[] icon = cursor.getBlob(columnIndex);
                        iconBitmap = BitmapFactory.decodeByteArray(icon, 0, icon.length);
                        mCache.put(component, iconBitmap);
                    }

                    imageView.setImageBitmap(iconBitmap);
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getBaseContext().startActivity(intent);

                            // Display a message if the app is not launching right away.
                            final Handler handler = new Handler(Looper.getMainLooper());
                            final Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    ActivityManager activityManager =
                                            (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                                    ActivityManager.RunningTaskInfo runningTaskInfo =
                                            activityManager.getRunningTasks(1).get(0);

                                    final String sourcePackage = component.getPackageName();
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
                            handler.postDelayed(runnable, 100);

                        }
                    });
                } catch (URISyntaxException e) {
                    return true;
                }

                return true;
            }

            return false;
        }
    }
}
