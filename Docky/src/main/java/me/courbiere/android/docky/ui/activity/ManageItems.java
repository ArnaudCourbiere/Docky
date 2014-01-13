package me.courbiere.android.docky.ui.activity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.item.AppInfo;
import me.courbiere.android.docky.ui.adapter.GridItemArrayAdapter;

/**
 * Allowing user to manage items that appear in the dock.
 */
public class ManageItems extends FragmentActivity {
    private static final String TAG = "ManageItems";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_items);

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
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private ArrayList<AppInfo> mApplications;
        private ProgressBar mLoader;
        private GridView mAppGrid;

        public PlaceholderFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_item_grid, container, false);
            mLoader = (ProgressBar) rootView.findViewById(R.id.app_list_loader);
            mAppGrid = (GridView) rootView.findViewById(R.id.app_list);

            new AppsLoader().execute();

            return rootView;
        }

        /**
         * Loads the list of installed applications in mApplications.
         *
         * @param isLaunching indicates whether the Activity is launching or not.
         */
        private void loadApplications(boolean isLaunching) {
            if (isLaunching && mApplications != null) {
                return;
            }

            final PackageManager manager = getActivity().getPackageManager();

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
            Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

            if (apps != null) {
                final int count = apps.size();

                if (mApplications == null) {
                    mApplications = new ArrayList<>(count);
                }

                mApplications.clear();

                for (int i = 0; i < count; i++) {
                    final AppInfo application = new AppInfo();
                    final ResolveInfo info = apps.get(i);

                    application.title = info.loadLabel(manager);

                    application.setActivity(new ComponentName(
                            info.activityInfo.applicationInfo.packageName,
                            info.activityInfo.name),
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        final int iconId = info.getIconResource();
                        final ActivityManager activityManager = (ActivityManager)
                                getActivity().getSystemService(Context.ACTIVITY_SERVICE);
                        final int iconDpi = activityManager.getLauncherLargeIconDensity();

                        try {
                            final Resources resources = manager.getResourcesForApplication(
                                    info.activityInfo.applicationInfo);
                            application.icon = resources.getDrawableForDensity(iconId, iconDpi);
                        } catch (PackageManager.NameNotFoundException e) {
                            application.icon = info.activityInfo.loadIcon(manager);
                        } catch (RuntimeException e) {
                            // TODO: Look back at example for handling resource not found.
                        }
                    } else {
                        application.icon = info.loadIcon(manager);
                    }

                    mApplications.add(application);
                }
            }
        }

        private class AppsLoader extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... params) {
                loadApplications(true);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mLoader.setVisibility(View.GONE);
                mAppGrid.setAdapter(new GridItemArrayAdapter(getActivity(), R.layout.grid_item_layout, mApplications));
                mAppGrid.setVisibility(View.VISIBLE);
            }
        }
    }
}
