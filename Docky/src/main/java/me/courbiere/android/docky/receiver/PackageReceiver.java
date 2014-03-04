package me.courbiere.android.docky.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.courbiere.android.docky.provider.DockItemsContract;

/**
 * Receives the ACTION_PACKAGE_ADDED and ACTION_PACKAGE_FULLY_REMOVED broadcast intents.
 */
public class PackageReceiver extends BroadcastReceiver {
    private static final String TAG = "PackageReceiver";

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent
     * broadcast.  During this time you can use the other methods on
     * BroadcastReceiver to view/modify the current result values.  This method
     * is always called within the settings thread of its process, unless you
     * explicitly asked for it to be scheduled on a different thread using
     * {@link android.content.Context#registerReceiver(android.content.BroadcastReceiver,
     * android.content.IntentFilter, String, android.os.Handler)}.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final String packageName = intent.getData().getSchemeSpecificPart();

        switch (intent.getAction()) {
            case Intent.ACTION_PACKAGE_ADDED:
                break;
            case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                // All intents referencing this package name need to be removed from the dock items.
                final String where = DockItemsContract.DockItems.INTENT + " like ?";
                final String[] whereArgs = { "%" + packageName + "%" };
                context.getContentResolver().delete(
                        DockItemsContract.DockItems.CONTENT_URI, where, whereArgs);
                break;
        }
    }
}
