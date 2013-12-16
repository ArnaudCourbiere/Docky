package me.courbiere.android.docky;

import android.app.Activity;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.courbiere.android.docky.item.AppInfo;
import me.courbiere.android.docky.service.BootService;
import me.courbiere.android.docky.ui.view.DockLayout;

import static me.courbiere.android.docky.util.LogUtils.*;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
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
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startDocky(View v) {
        LOGD(TAG, "startDocky()");
        startService(new Intent(this, BootService.class));
    }

    public void stopDocky(View v) {
        LOGD(TAG, "stopDocky()");
        stopService(new Intent(this, BootService.class));
    }

    public void pickApp(View v) {
        LOGD(TAG, "pickApp()");
        /*
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        */
        //Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        //intent.addCategory(Intent.CATEGORY_LAUNCHER);
        //startActivityForResult(intent, 1234);
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
            return rootView;
        }
    }

}
