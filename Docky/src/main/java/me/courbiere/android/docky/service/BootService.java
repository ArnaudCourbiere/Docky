package me.courbiere.android.docky.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import me.courbiere.android.docky.R;
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
    private View mDock;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        // TODO
        LOGD(TAG, "onCreate()");
        mDock = new DockView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER;
        params.setTitle(getString(R.string.app_name));
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mDock, params);
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
