package me.courbiere.android.docky.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.service.DockService;
import me.courbiere.android.docky.ui.fragment.SettingsFragment;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * User settings.
 */
public class SettingsActivity extends Activity {
    private static final String TAG = "SettingsActivity";

    /* Constants used to retrieve user preferences */
    public static final String PREFERENCES_STYLE = "preferences_style";
    public static final String PREFERENCES_DRAG_HANDLE_WIDTH = "preferences_drag_handle_width";
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.toggle_dock, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem toggleItem = menu.findItem(R.id.action_toggle_dock);

        if (DockService.isRunning()) {
            toggleItem.setTitle(getString(R.string.stop_dock));
        } else {
            toggleItem.setTitle(getString(R.string.start_dock));
        }

        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle_dock:
                if (DockService.isRunning()) {
                    stopService(new Intent(this, DockService.class));
                } else {
                    startService(new Intent(this, DockService.class));
                }

                return true;

            case android.R.id.home:
                final Intent upIntent = NavUtils.getParentActivityIntent(this);
                final String action = getIntent().getAction();

                if (action != null && action.equals(Intent.ACTION_VIEW)) {
                    TaskStackBuilder.create(this)
                            .addNextIntentWithParentStack(upIntent)
                            .startActivities();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
