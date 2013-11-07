package me.courbiere.android.docky.test;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import me.courbiere.android.docky.MainActivity;

/**
 * Unit test for me.courbiere.android.docky.MainActivity class.
 */
public class TestMainActivity extends ActivityInstrumentationTestCase2<MainActivity> {

    /**
     * The context being tested.
     */
    private Context mTargetContext;

    /**
     * The Activity to test
     */
    private MainActivity mActivity;

    public TestMainActivity() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() {
        // Need to turn off touch mode to send key events to the app.
        setActivityInitialTouchMode(false);

        mTargetContext = getInstrumentation().getTargetContext();
        mActivity = getActivity();
    }

    public void testPreConditions() {
        assertTrue(mTargetContext != null);
        assertTrue(mActivity != null);
    }
}
