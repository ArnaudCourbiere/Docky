package me.courbiere.android.docky;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import me.courbiere.android.docky.service.DockService;
import me.courbiere.android.docky.ui.activity.ManageItemsActivity;
import me.courbiere.android.docky.ui.activity.SettingsActivity;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            final Button toggleButon = (Button) rootView.findViewById(R.id.toggle);
            final Button manageItemsButton = (Button) rootView.findViewById(R.id.manage_items);
            final ActionBar actionBar = getActivity().getActionBar();

            if (DockService.isRunning()) {
                toggleButon.setText(R.string.stop);
                actionBar.setSubtitle(R.string.running);
            } else {
                toggleButon.setText(R.string.start);
                actionBar.setSubtitle(R.string.stopped);
            }

            toggleButon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (DockService.isRunning()) {
                        getActivity().stopService(new Intent(getActivity(), DockService.class));
                        toggleButon.setText(R.string.start);
                        actionBar.setSubtitle(R.string.stopped);
                    } else {
                        getActivity().startService(new Intent(getActivity(), DockService.class));
                        toggleButon.setText(R.string.stop);
                        actionBar.setSubtitle(R.string.running);
                    }
                }
            });

            manageItemsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(getActivity(), ManageItemsActivity.class);
                    startActivity(intent);
                }
            });

            return rootView;
        }
    }

}
