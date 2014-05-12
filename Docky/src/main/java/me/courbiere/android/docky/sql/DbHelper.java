package me.courbiere.android.docky.sql;

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.provider.DockItemsContract;
import me.courbiere.android.docky.ui.activity.ManageItemsActivity;
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
    private static final int VERSION = 3;

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
            DockItemsContract.DockItems.POSITION + " INTEGER NOT NULL DEFAULT 9999," +
            DockItemsContract.DockItems.STICKY + " SMALLINT NOT NULL DEFAULT 0);" };

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

        // Update the dock item launching the ManageItemsActivity (class renamed).
        if (oldVersion < 2 && newVersion >= 2) {
            final String whereClause = DockItemsContract.DockItems.TITLE + " = ?";
            final String[] whereArgs = { "Add" };

            db.delete(DockItemsContract.DockItems.TABLE_NAME, whereClause, whereArgs);

            // Add item that allows to add more apps with updated Class name.
            final String addTitle = "Add";
            final Intent addIntent = new Intent(mContext, ManageItemsActivity.class);
            addIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            final ActivityManager activityManager = (ActivityManager)
                    mContext.getSystemService(Context.ACTIVITY_SERVICE);
            final int iconDpi = activityManager.getLauncherLargeIconDensity();
            final Drawable addIcon = mContext.getResources().getDrawableForDensity(R.drawable.dock_action_new, iconDpi);
            final Bitmap addBitmap = ImageUtils.createIconBitmap(mContext, addIcon);
            final byte[] flattenedAddIcon = ImageUtils.flattenBitmap(addBitmap);
            final int position = 10000;
            final ContentValues addValues = new ContentValues();
            addValues.put(DockItemsContract.DockItems.TITLE, addTitle);
            addValues.put(DockItemsContract.DockItems.INTENT, addIntent.toUri(0));
            addValues.put(DockItemsContract.DockItems.ICON, flattenedAddIcon);
            addValues.put(DockItemsContract.DockItems.POSITION, position);
            addValues.put(DockItemsContract.DockItems.STICKY, true);
            db.insert(DockItemsContract.DockItems.TABLE_NAME, null, addValues);
        }

        // Re-compact items positions starting at 0.
        if (oldVersion < 3 && newVersion >= 3) {
            final String[] projection = { DockItemsContract.DockItems._ID, };
            final String selection = DockItemsContract.DockItems.STICKY + " = ?";
            final String[] selectionArgs = { "0" };

            Cursor cursor = db.query(
                    DockItemsContract.DockItems.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            cursor.moveToFirst();
            int position = 0;

            while (!cursor.isAfterLast()) {
                final int id = cursor.getInt(cursor.getColumnIndex(DockItemsContract.DockItems._ID));
                final ContentValues values = new ContentValues();
                final String where = DockItemsContract.DockItems._ID + " = ?";
                final String[] whereArgs = { Integer.toString(id)};

                values.put(DockItemsContract.DockItems.POSITION, position);
                db.update(DockItemsContract.DockItems.TABLE_NAME, values, where, whereArgs);

                position++;
                cursor.moveToNext();
            }
        }
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        for (String schema : SCHEMAS) {
            db.execSQL(schema);
        }

        // Add item that allows to add more apps.
        final String addTitle = "Add";
        final Intent addIntent = new Intent(mContext, ManageItemsActivity.class);
        addIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final ActivityManager activityManager = (ActivityManager)
                mContext.getSystemService(Context.ACTIVITY_SERVICE);
        final int iconDpi = activityManager.getLauncherLargeIconDensity();
        final Drawable addIcon = mContext.getResources().getDrawableForDensity(R.drawable.dock_action_new, iconDpi);
        final Bitmap addBitmap = ImageUtils.createIconBitmap(mContext, addIcon);
        final byte[] flattenedAddIcon = ImageUtils.flattenBitmap(addBitmap);
        final int position = 10000;
        final ContentValues addValues = new ContentValues();
        addValues.put(DockItemsContract.DockItems.TITLE, addTitle);
        addValues.put(DockItemsContract.DockItems.INTENT, addIntent.toUri(0));
        addValues.put(DockItemsContract.DockItems.ICON, flattenedAddIcon);
        addValues.put(DockItemsContract.DockItems.POSITION, position);
        addValues.put(DockItemsContract.DockItems.STICKY, true);
        db.insert(DockItemsContract.DockItems.TABLE_NAME, null, addValues);

        /*
        // Debug stuff.
        final PackageManager manager = mContext.getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        if (apps != null) {
            final int count = apps.size();

            for (int i = 0; i < count; i++) {
                ResolveInfo info = apps.get(i);
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
                values.put(DockItemsContract.DockItems.POSITION, i);

                db.insert(DockItemsContract.DockItems.TABLE_NAME, null, values);
            }
        }
        */
    }
}
