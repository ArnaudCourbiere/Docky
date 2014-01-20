package me.courbiere.android.docky.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.net.URISyntaxException;
import java.util.HashMap;

import me.courbiere.android.docky.MainActivity;
import me.courbiere.android.docky.R;
import me.courbiere.android.docky.provider.DockItemsContract;
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
     * Icon cache.
     */
    private HashMap<ComponentName, Bitmap> mCache = new HashMap<>(50);

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

        final String[] from = new String[] {DockItemsContract.DockItems.ICON };
        final int[] to = new int[] { R.id.app_icon };

        mCursor = getContentResolver().query(DockItemsContract.DockItems.CONTENT_URI,
                DOCK_ITEM_PROJECTION, null, null, DockItemsContract.DockItems.POSITION + " ASC");
        mItemsAdapter = new SimpleCursorAdapter(getBaseContext(), R.layout.dock_item_layout,
                mCursor, from, to, 0);

        // TODO: Improvement: use the holder pattern.

        // Use custom binder.
        final SimpleCursorAdapter.ViewBinder binder = new SimpleCursorAdapter.ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                final String columnName = cursor.getColumnName(columnIndex);

                if (columnName.equals(DockItemsContract.DockItems.ICON)) {
                    final ImageView imageView = (ImageView) view;
                    final String intentUri = cursor.getString(
                            cursor.getColumnIndex(DockItemsContract.DockItems.INTENT));

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
                            }
                        });
                    } catch (URISyntaxException e) {
                        return true;
                    }

                    return true;
                }

                return false;
            }
        };

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
}
