package me.courbiere.android.docky.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.ui.fragment.SettingsFragment;

/**
 * User settings.
 */
public class SettingsActivity extends Activity {
    private static final String TAG = "SettingsActivity";

    /* Constants used to retrieve user preferences */
    public static final String PREFERENCES_STYLE = "preferences_style";
    public static final String PREFERENCES_START_DOCK_ON_BOOT = "preferences_start_dock_on_boot";

    /* Dock style constants (used to store style in preferences) */
    public static final String STYLE_WHITE = "STYLE_WHITE";
    public static final String STYLE_BLACK = "STYLE_BLACK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    */
}
