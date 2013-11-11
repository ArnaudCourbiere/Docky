package me.courbiere.android.docky.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.ui.adapter.DockItemArrayAdapter;
import me.courbiere.android.docky.ui.view.DockView;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Bootstraps the dock View and Gesture Listeners.
 */
public class BootService extends Service {
    private static final String TAG = "BootService";

    /**
     * Dock.
     */
    private ListView mDock;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        // TODO
        LOGD(TAG, "onCreate()");

        initListView();
        final int dockWidth = (int) getResources().getDimension(R.dimen.dock_width);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dockWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        params.setTitle(getString(R.string.app_name));
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mDock, params);
    }

    public void initListView() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mDock = (ListView) inflater.inflate(R.layout.dock_layout, null);
        final String[] values = new String[] { "Arnaud", "Julien", "Andre", "Dominique" };
        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            list.add(values[i]);
        }
        final ArrayAdapter<String> adapter = new DockItemArrayAdapter(this, R.layout.dock_item_layout, list);
        mDock.setAdapter(adapter);

        mDock.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                LOGD(TAG, "Touch");
                return false;
            }
        });
    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link android.content.Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        // TODO
        LOGD(TAG, "onDestroy()");
        if (mDock != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDock);
            mDock = null;
        }
    }
}
