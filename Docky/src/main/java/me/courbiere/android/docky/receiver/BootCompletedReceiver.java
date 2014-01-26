package me.courbiere.android.docky.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import me.courbiere.android.docky.service.DockService;
import me.courbiere.android.docky.ui.activity.SettingsActivity;

/**
 * Receives the BOOT_COMPLETED broadcast and starts Dock depending on user preferences.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent
     * broadcast.  During this time you can use the other methods on
     * BroadcastReceiver to view/modify the current result values.  This method
     * is always called within the main thread of its process, unless you
     * explicitly asked for it to be scheduled on a different thread using
     * {@link android.content.Context#registerReceiver(android.content.BroadcastReceiver,
     * IntentFilter, String, android.os.Handler)}.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            if (prefs.getBoolean(SettingsActivity.PREFERENCES_START_DOCK_ON_BOOT, false)) {
                context.startService(new Intent(context, DockService.class));
            }
        }
    }
}
