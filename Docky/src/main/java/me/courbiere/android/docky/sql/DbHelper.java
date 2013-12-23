package me.courbiere.android.docky.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import me.courbiere.android.docky.provider.DockItemsContract;

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
            DockItemsContract.DockItems.PACKAGE_NAME + " TEXT NOT NULL," +
            DockItemsContract.DockItems.POSITION+ " INTEGER NOT NULL DEFAULT 9999);" };

    /**
     * Create a helper object to create, open, and/or manage a database.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context to use to open or create the database
     * @param name    of the database file, or null for an in-memory database
     * @param factory to use for creating cursor objects, or null for the default
     * @param version number of the database (starting at 1); if the database is older,
     *                {@link #onUpgrade} will be used to upgrade the database; if the database is
     *                newer, {@link #onDowngrade} will be used to downgrade the database
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
        for (String schema : SCHEMAS) {
            db.execSQL(schema);
        }
    }
}
