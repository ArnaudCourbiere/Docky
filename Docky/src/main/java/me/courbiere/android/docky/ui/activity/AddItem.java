package me.courbiere.android.docky.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.ListView;

import me.courbiere.android.docky.R;

/**
 * Activity that allows the user to pick apps in order to add them to the dock.
 */
public class AddItem extends Activity {
    private static final String TAG = "AddItem";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additem);

        final GridView appList = (GridView) findViewById(R.id.app_list);
    }
}
