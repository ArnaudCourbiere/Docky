package me.courbiere.android.docky.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract definition for the supported URIs and columns.
 */
public class DockItemsContract {
    private static final String TAG = "DockItemsContract";

    /** The authority for the items provider. */
    public static final String AUTHORITY = "me.courbiere.android.docky.content.dockitems";

    /** A content:// style uri to the authority for the items provider */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * A query parameter that limits the number of results returned.
     * The parameter value should be an integer.
     *
     * TODO: Remove if not needed.
     */
    public static final String LIMIT_PARAM_KEY = "limit";

    /**
     * Constants for the dockitems table.
     */
    public static class DockItems implements BaseColumns, DockItemsColumns {

        /**
         * This class cannot be instantiated.
         */
        private DockItems() {};

        /**
         * The content:// style URI for this table.
         */
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, "dockitems");

        /**
         * The MIME type of the CONTENT_URI providing a directory of dock items.
         */
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + ".dockitem";

        /**
         * The MIME type of the CONTENT_URI providing a single dock item.
         */
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + AUTHORITY + ".dockitem";

        /**
         * Table name where the records are stored for the DockItems resource.
         */
        public static final String TABLE_NAME = "dockitems";
    }

    /**
     * Columns for the dockitems table.
     */
    protected interface DockItemsColumns {

        /** The item title */
        public static final String TITLE = "title";

        /** The item intent */
        public static final String INTENT = "intent";

        /** The item type */
        public static final String ITEM_TYPE = "itemType";

        /** The icon type */
        public static final String ICON_TYPE = "iconType";

        /** The icon package */
        public static final String ICON_PACKAGE = "iconPackage";

        /** The icon resource */
        public static final String ICON_RESOURCE = "iconResource";

        /** The icon */
        public static final String ICON = "icon";

        /** The item position in the dock */
        public static final String POSITION = "position";
    }
}
