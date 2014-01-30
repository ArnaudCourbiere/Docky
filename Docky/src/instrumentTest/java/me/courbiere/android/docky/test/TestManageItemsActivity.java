package me.courbiere.android.docky.test;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import me.courbiere.android.docky.ui.activity.ManageItemsActivity;

/**
 * Unit test for me.courbiere.android.docky.ui.activity.ManageItemsActivity class.
 */
public class TestManageItemsActivity extends ActivityInstrumentationTestCase2<ManageItemsActivity> {

    /**
     * The context being tested.
     */
    private Context mTargetContext;

    /**
     * The Activity to test
     */
    private ManageItemsActivity mActivity;

    public TestManageItemsActivity() {
        super(ManageItemsActivity.class);
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
