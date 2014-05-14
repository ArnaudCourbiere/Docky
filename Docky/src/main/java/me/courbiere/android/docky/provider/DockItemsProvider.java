package me.courbiere.android.docky.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import me.courbiere.android.docky.sql.DbHelper;

/**
 * Dock Item Content Provider.
 *
 * TODO: Add support for URL GET parameter controlling notification.
 */
public class DockItemsProvider extends ContentProvider {
    private static final String TAG = "DockItemsProvider";

    /**
     * Db Helper.
     */
    private DbHelper mDbHelper;

    /**
     * Uri matcher.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /**
     * Regex used to split query strings.
     */
    public static final String QUERY_TOKENIZER_REGEX = "[^\\w@]+";

    // Directory values MUST BE EVEN, Item values MUST BE ODD.
    private static final int DOCK_ITEMS = 1000;      // Directory
    private static final int DOCK_ITEMS_ID = 1001;   // Item

    // Query Interfaces
    private interface DockItemQuery {
        public static final String TABLE = "dockitems";
        public static final String PK = DockItemsContract.DockItems._ID;

        public static final String[] PROJECTION = new String [] {
                DockItemsContract.DockItems.TITLE,
                DockItemsContract.DockItems.INTENT,
                DockItemsContract.DockItems.ITEM_TYPE,
                DockItemsContract.DockItems.ICON_TYPE,
                DockItemsContract.DockItems.ICON_PACKAGE,
                DockItemsContract.DockItems.ICON_RESOURCE,
                DockItemsContract.DockItems.ICON,
                DockItemsContract.DockItems.POSITION,
                DockItemsContract.DockItems.STICKY };
    }

    // URI matching table.
    static {
        sUriMatcher.addURI(DockItemsContract.AUTHORITY, "dockitems", DOCK_ITEMS);
        sUriMatcher.addURI(DockItemsContract.AUTHORITY, "dockitems/#", DOCK_ITEMS_ID);
    }

    /**
     * Initializes content provider on startup. This method is called for all registered content
     * providers on the application settings thread at application launch time. It must not perform
     * lengthy operations, or application startup will be delayed.
     *
     * @return true if the provider was successfully loaded, false otherwise.
     */
    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mDbHelper = new DbHelper(context);
        return true;
    }

    /**
     * Retrieves the MIME of the data at the given URI.
     * Ex: vnd.android.cursor.dir/vnd.me.courbiere.android.docky.content.dockitems.dockitems
     * See the official Android documentation on Content Providers for more info.
     *
     * @param uri The URI to query.
     * @return A MIME type string.
     */
    @Override
    public String getType(final Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case DOCK_ITEMS:
                return DockItemsContract.DockItems.CONTENT_TYPE;
            case DOCK_ITEMS_ID:
                return DockItemsContract.DockItems.CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    /**
     * Handles query requests from clients. This method can be called from multiple threads.
     * TODO: Check if/when/why/how implement the overloaded method with cancellation signal.
     *
     * @param uri The URI to query.
     * @param projection The list of columns to put into the cursor.
     * @param selection A selection criteria to apply when filtering rows.
     * @param selectionArgs Arguments for prepared queries. The values will be bound as Strings.
     * @param sortOrder Sort order.
     * @return A Cursor or null.
     */
    @Override
    public Cursor query(
            final Uri uri,
            String[] projection,
            final String selection,
            final String[] selectionArgs,
            String sortOrder) {

        // TODO: See waitForAccess in ContactsProvider2.java and check if needed.

        // Use default projection if none is supplied.
        if (projection == null) {
            projection = getDefaultProjection(uri);
        }

        // Use default sort order if non is supplied.
        if (sortOrder == null) {
            sortOrder = DockItemsContract.DockItems.POSITION;
        }

        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(this.getTable(uri));

        // Item match values are odd.
        if (sUriMatcher.match(uri) % 2 != 0) {
            queryBuilder.appendWhere(BaseColumns._ID + "=" + uri.getLastPathSegment());
        } else {

            // Querying a directory without specifying a sort order.
            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = BaseColumns._ID + " ASC";
            }
        }

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs,
                null, null, sortOrder);

        // Tell the cursor to watch for changes on the following uri.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Handles requests to insert new rows. This method can be called from multiple threads.
     *
     * @param uri The content:// URI of the insertion request. This must not be null.
     * @param values A set of columns_name/value pairs to add to the database.
     *               This must not be null.
     * @return The URI for the newly inserted item.
     */
    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Uri insertUri;

        try {
            final int match = sUriMatcher.match(uri);

            switch (match) {
                case DOCK_ITEMS:
                case DOCK_ITEMS_ID:
                    db.insertOrThrow(DockItemsContract.DockItems.TABLE_NAME, null, values);
                    insertUri =
                            DockItemsContract.DockItems.CONTENT_URI.buildUpon().appendPath(
                                    values.getAsString(DockItemsContract.DockItems._ID)).build();
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        } finally {
            db.close();
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return insertUri;
    }

    /**
     * Override this to handle requests to insert a set of new rows, or the
     * default implementation will iterate over the values and call
     * {@link #insert} on each of them.
     * As a courtesy, call {@link ContentResolver#notifyChange(android.net.Uri, android.database.ContentObserver) notifyChange()}
     * after inserting.
     * This method can be called from multiple threads, as described in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
     * and Threads</a>.
     *
     * @param uri    The content:// URI of the insertion request.
     * @param values An array of sets of column_name/value pairs to add to the database.
     *               This must not be {@code null}.
     * @return The number of values that were inserted.
     */
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final String tableName = getTable(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            for (ContentValues value : values) {
                final long newId = db.insertOrThrow(tableName, null, value);

                if (newId < 0) {
                    // Can't throw a checked exception because the parent method doesn't throw one.
                    throw new RuntimeException("Failed to insert row into " + uri.toString());
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return values.length;
    }

    /**
     * Handles requests to update rows. This method can be called from multiple threads.
     *
     * @param uri The URI to query. Can have a record ID if this is an update for a specific row.
     * @param values A set of column_name/value pairs to update in the database.
     *               This must not be null.
     * @param selection An optional filter to match rows to update.
     * @param selectionArgs Arguments for prepared queries.
     * @return The number or rows affected.
     */
    @Override
    public int update(
            final Uri uri,
            final ContentValues values,
            final String selection,
            final String[] selectionArgs) {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int rowsAffected;

        try {
            final int match = sUriMatcher.match(uri);

            switch (match) {
                case DOCK_ITEMS:
                case DOCK_ITEMS_ID:
                    rowsAffected = db.update(
                            DockItemsContract.DockItems.TABLE_NAME, values, selection, selectionArgs);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        } finally {
            db.close();
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsAffected;
    }

    /**
     * Handles requests to delete one or more rows. This method can be called from multiple threads.
     *
     * @param uri The full URI to query, including a row ID if a specific record is requested.
     * @param selection Optional restriction to apply to the rows being deleted.
     * @param selectionArgs Arguments for prepared queries.
     * @return The number of rows affected.
     */
    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        // TODO: Check against example app.

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int rowsAffected;

        try {
            final int match = sUriMatcher.match(uri);

            switch (match) {
                case DOCK_ITEMS:
                case DOCK_ITEMS_ID:
                    rowsAffected = db.delete(DockItemsContract.DockItems.TABLE_NAME, selection, selectionArgs);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        } finally {
            db.close();
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsAffected;
    }

    /**
     * Retrieve the underlying table for a given URI.
     *
     * @param uri The URI to query.
     * @return The table name.
     */
    public String getTable(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case DOCK_ITEMS:
            case DOCK_ITEMS_ID:
                return DockItemQuery.TABLE;
            default:
                throw new IllegalArgumentException(
                        "Invalid URI specified: " + uri.toString() + ".");
        }
    }

    /**
     * Retrieves the default projection for a given URI.
     *
     * @param uri The URI to query.
     * @return The default projection.
     */
    public String[] getDefaultProjection(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case DOCK_ITEMS:
            case DOCK_ITEMS_ID:
                return DockItemQuery.PROJECTION;
            default:
                throw new IllegalArgumentException(
                        "Invalid URI specified: " + uri.toString() + ".");
        }
    }
}
