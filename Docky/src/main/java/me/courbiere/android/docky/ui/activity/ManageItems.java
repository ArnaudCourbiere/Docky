package me.courbiere.android.docky.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.item.AppInfo;
import me.courbiere.android.docky.provider.DockItemsContract;
import me.courbiere.android.docky.ui.adapter.GridItemArrayAdapter;
import me.courbiere.android.docky.util.ImageUtils;

import static me.courbiere.android.docky.util.LogUtils.*;

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

    /*
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
    */

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        /**
         * Key used to save and retrieve app info list from state Bundles.
         */
        public static final String KEY_APP_INFO_LIST = "AppInfoList";

        /**
         * Key used to save and retrieve dock item list from state Bundles.
         */
        public static final String KEY_DOCK_ITEM_LIST = "DockItemList";

        /**
         * Identifies the Loader being used.
         */
        public static final int URL_LOADER = 0;

        private Set<String> mDockItems;
        private ArrayList<AppInfo> mApplications;
        private boolean mDockItemsLoaded;
        private boolean mApplicationsLoaded;
        private ProgressBar mLoader;
        private GridView mAppGrid;
        private int mShortAnimDuration;

        public PlaceholderFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mDockItemsLoaded = false;
            mApplicationsLoaded = false;
            mShortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

            View rootView = inflater.inflate(R.layout.fragment_item_grid, container, false);
            mLoader = (ProgressBar) rootView.findViewById(R.id.app_list_loader);
            mAppGrid = (GridView) rootView.findViewById(R.id.app_list);
            mAppGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final AppInfo appInfo = mApplications.get(position);
                    final String intentUri = appInfo.intent.toUri(0);
                    final ContentResolver contentResolver = getActivity().getContentResolver();
                    final AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(contentResolver) {};

                    if (mAppGrid.isItemChecked(position)) {
                        final Bitmap bitmapIcon = ImageUtils.createIconBitmap(getActivity(), appInfo.icon);
                        final byte[] flattenedIcon = ImageUtils.flattenBitmap(bitmapIcon);

                        final ContentValues values = new ContentValues();
                        values.put(DockItemsContract.DockItems.TITLE, appInfo.title.toString());
                        values.put(DockItemsContract.DockItems.INTENT, intentUri);
                        values.put(DockItemsContract.DockItems.ICON, flattenedIcon);
                        //values.put(DockItemsContract.DockItems.POSITION, position);

                        asyncQueryHandler.startInsert(
                                0,
                                null,
                                DockItemsContract.DockItems.CONTENT_URI,
                                values);
                    } else {
                        final String where = DockItemsContract.DockItems.INTENT + " = ?";
                        final String[] selectionArgs = { intentUri };

                        asyncQueryHandler.startDelete(
                                0,
                                null,
                                DockItemsContract.DockItems.CONTENT_URI,
                                where,
                                selectionArgs);
                    }
                }
            });

            // Load all apps.
            if (savedInstanceState != null) {
                mApplications = savedInstanceState.getParcelableArrayList(KEY_APP_INFO_LIST);

                final String[] dockItems = savedInstanceState.getStringArray(KEY_DOCK_ITEM_LIST);
                mDockItems = new HashSet<>(Arrays.asList(dockItems));

                displayAppList();
            } else {
                new AppsLoader().execute();
                getLoaderManager().initLoader(URL_LOADER, null, this);
            }

            return rootView;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putParcelableArrayList(KEY_APP_INFO_LIST, mApplications);

            final String[] dockItems = new String[mDockItems.size()];
            outState.putStringArray(KEY_DOCK_ITEM_LIST, mDockItems.toArray(dockItems));
        }

        /**
         * Removes the loading indicator and displays the applications in the GridView.
         */
        private void displayAppList() {
            final ActionBar actionBar = getActivity().getActionBar();
            final int numItems = mDockItems.size() - 1;

            if (numItems == 0) {
                actionBar.setSubtitle(R.string.dock_empty);
            } else {
                final int stringId = numItems == 1
                        ? R.string.manage_dock_items_subtitle_singular
                        : R.string.manage_dock_items_subtitle_plural;

                actionBar.setSubtitle(String.format(getString(stringId), numItems));
            }

            if (mLoader.getVisibility() != View.GONE) {
                mAppGrid.setAdapter(new GridItemArrayAdapter(getActivity(), R.layout.grid_item_layout, mApplications));
                int i = 0;

                for (AppInfo appInfo : mApplications) {
                    if (mDockItems.contains(appInfo.intent.toUri(0))) {
                        mAppGrid.setItemChecked(i, true);
                    }

                    i++;
                }

                mAppGrid.setAlpha(0f);
                mAppGrid.setVisibility(View.VISIBLE);
                mAppGrid.animate()
                        .alpha(1f)
                        .setDuration(mShortAnimDuration)
                        .setListener(null);

                mLoader.animate()
                        .alpha(0f)
                        .setDuration(mShortAnimDuration)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mLoader.setVisibility(View.GONE);
                            }
                        });
            }
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

        /**
         * Loads the application list in the background and displays them in the GridView when done.
         */
        private class AppsLoader extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... params) {
                loadApplications(true);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                boolean displayAppList = false;

                synchronized (this) {
                    mApplicationsLoaded = true;

                    if (mDockItemsLoaded) {
                        displayAppList = true;
                    }
                }

                if (displayAppList) {
                    displayAppList();
                }
            }
        }

        /* Loader callbacks */

        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            final String[] projection = {
                    DockItemsContract.DockItems._ID,
                    DockItemsContract.DockItems.INTENT
            };

            switch (loaderId) {
                case URL_LOADER:
                    return new CursorLoader(
                            getActivity(),
                            DockItemsContract.DockItems.CONTENT_URI,
                            projection,
                            null, // Selection clause
                            null, // Selection arguments
                            null  // Default sort order
                    );

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mDockItems = new HashSet<String>();
            boolean displayAppList = false;

            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                mDockItems.add(cursor.getString(
                        cursor.getColumnIndex(DockItemsContract.DockItems.INTENT)));

                cursor.moveToNext();
            }

            synchronized (this) {
                mDockItemsLoaded = true;

                if (mApplicationsLoaded) {
                    displayAppList = true;
                }
            }

            if (displayAppList) {
                displayAppList();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
            mDockItems.clear();
        }
    }
}
