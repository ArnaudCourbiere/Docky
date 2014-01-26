package me.courbiere.android.docky.ui.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import me.courbiere.android.docky.R;

/**
 * Fragment to display settings.
 */
public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
