package me.courbiere.android.docky.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.ui.activity.SettingsActivity;
import me.courbiere.android.docky.ui.dialog.SliderPreference;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Fragment to display settings.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        final SharedPreferences sharedPreferences =
                getPreferenceScreen().getSharedPreferences();

        setSummary(sharedPreferences, SettingsActivity.PREFERENCES_STYLE);
        setSummary(sharedPreferences, SettingsActivity.PREFERENCES_DRAG_HANDLE_WIDTH);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary(sharedPreferences, key);
    }

    private void setSummary(SharedPreferences sharedPreferences, String key) {
        final Preference pref = findPreference(key);
        switch (key) {
            case SettingsActivity.PREFERENCES_STYLE:
                switch (sharedPreferences.getString(key, SettingsActivity.STYLE_WHITE)) {
                    case SettingsActivity.STYLE_BLACK:
                        pref.setSummary(getString(R.string.black));
                        break;

                    case SettingsActivity.STYLE_WHITE:
                    default:
                        pref.setSummary(getString(R.string.white));
                }
                break;

            case SettingsActivity.PREFERENCES_DRAG_HANDLE_WIDTH:
                final int dragHandleWidth =
                        sharedPreferences.getInt(key, SliderPreference.DEFAULT_VALUE);

                pref.setSummary(dragHandleWidth * (100 / SliderPreference.MAX_VALUE) + "%");
                break;
        }
    }
}
