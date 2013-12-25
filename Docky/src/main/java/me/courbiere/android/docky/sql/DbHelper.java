package me.courbiere.android.docky.sql;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.courbiere.android.docky.provider.DockItemsContract;
import me.courbiere.android.docky.util.ImageUtils;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Database Helper.
 */
public class DbHelper extends SQLiteOpenHelper {
    private static final String TAG = "DbHelper";

    /**
     * Database version.
     */
    public static final int VERSION = 1;

    /**
     * Database name.
     */
    private static final String DB_NAME = "docky";

    /**
     * Application context.
     */
    private final Context mContext;

    /**
     * Array of schema definitions for the database.
     * Used to create the tables the first time the database is opened.
     */
    private static final String[] SCHEMAS = {
            "CREATE TABLE IF NOT EXISTS " + DockItemsContract.DockItems.TABLE_NAME + " (" +
            DockItemsContract.DockItems._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            DockItemsContract.DockItems.TITLE + " TEXT," +
            DockItemsContract.DockItems.INTENT + " TEXT," +
            DockItemsContract.DockItems.ITEM_TYPE + " INTEGER," +
            DockItemsContract.DockItems.ICON_TYPE + " INTEGER," +
            DockItemsContract.DockItems.ICON_PACKAGE + " TEXT," +
            DockItemsContract.DockItems.ICON_RESOURCE + " TEXT," +
            DockItemsContract.DockItems.ICON + " BLOB," +
            DockItemsContract.DockItems.POSITION+ " INTEGER NOT NULL DEFAULT 9999);" };

    /**
     * Create a helper object to create, open, and/or manage a database.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context to use to open or create the database
     */
    public DbHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        mContext = context;
    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     * <p/>
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        LOGD(TAG, "onCreate()");
        for (String schema : SCHEMAS) {
            db.execSQL(schema);
        }

        // Debug stuff.
        final PackageManager manager = mContext.getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        if (apps != null) {
            final int count = apps.size();

            for (ResolveInfo info : apps) {
                final String title = info.loadLabel(manager).toString();

                final ComponentName className = new ComponentName(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name);
                final int launchFlags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;
                final Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(className);
                intent.setFlags(launchFlags);

                final Drawable fullResIcon = ImageUtils.getFullResIcon(mContext, info.activityInfo);
                final Bitmap iconBitmap = ImageUtils.createIconBitmap(mContext, fullResIcon);
                final byte[] flattenedIcon = ImageUtils.flattenBitmap(iconBitmap);

                ContentValues values = new ContentValues();
                values.put(DockItemsContract.DockItems.TITLE, title);
                values.put(DockItemsContract.DockItems.INTENT, intent.toUri(0));
                values.put(DockItemsContract.DockItems.ICON, flattenedIcon);

                db.insert(DockItemsContract.DockItems.TABLE_NAME, null, values);
//
//
//                final AppInfo application = new AppInfo();
//
//                application.title = info.loadLabel(manager);
//
//                application.setActivity(new ComponentName(
//                        info.activityInfo.applicationInfo.packageName,
//                        info.activityInfo.name),
//                        Intent.FLAG_ACTIVITY_NEW_TASK
//                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
//                    final int iconId = info.getIconResource();
//                    final ActivityManager activityManager = (ActivityManager)
//                            this.getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
//                    final int iconDpi = activityManager.getLauncherLargeIconDensity();
//
//                    try {
//                        final Resources resources = manager.getResourcesForApplication(
//                                info.activityInfo.applicationInfo);
//                        application.icon = resources.getDrawableForDensity(iconId, iconDpi);
//                    } catch (PackageManager.NameNotFoundException e) {
//                        application.icon = info.activityInfo.loadIcon(manager);
//                    } catch (RuntimeException e) {
//                        // TODO: Look back at example for handling resource not found.
//                    }
//                } else {
//                    application.icon = info.loadIcon(manager);
//                }
//
//                mApplications.add(application);
            }
        }
    }
}
