package me.courbiere.android.docky.ui.dialog;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.SeekBar;
import android.content.SharedPreferences;

import me.courbiere.android.docky.R;
import me.courbiere.android.docky.ui.activity.SettingsActivity;

import static me.courbiere.android.docky.util.LogUtils.*;

/**
 * Slider Preference.
 */
public class SliderPreference extends DialogPreference {
    private static final String TAG = "SliderPreference";

    /**
     * The slider displayed in the dialog.
     */
    private SeekBar mSlider;

    /**
     * Slider's default value.
     */
    public static final int DEFAULT_VALUE = 20;

    /**
     * Max value.
     */
    public static final int MAX_VALUE = 20;

    /**
     * Slider's current value.
     */
    private int mCurrentValue;

    public SliderPreference(Context context) {
        this(context, null);
    }

    public SliderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setDialogLayoutResource(R.layout.slider_preference);
        setPositiveButtonText(android.R.string.ok);
        setDialogIcon(null);
    }

    /**
     * Binds views in the content View of the dialog to data.
     * <p/>
     * Make sure to call through to the superclass implementation.
     *
     * @param view The content View of the dialog, if it is custom.
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSlider = (SeekBar) view.findViewById(R.id.slider);

        mSlider.setProgress(mCurrentValue);
        mSlider.setMax(MAX_VALUE);
    }

    /**
     * Called when the dialog is dismissed and should be used to save data to
     * the {@link SharedPreferences}.
     *
     * @param positiveResult Whether the positive button was clicked (true), or
     *                       the negative button was clicked or the dialog was canceled (false).
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mSlider != null) {
            mCurrentValue = mSlider.getProgress();
            persistValue(mCurrentValue);
        }
    }

    /**
     * Implement this to set the initial value of the Preference.
     * <p/>
     * If <var>restorePersistedValue</var> is true, you should restore the
     * Preference value from the {@link android.content.SharedPreferences}. If
     * <var>restorePersistedValue</var> is false, you should set the Preference
     * value to defaultValue that is given (and possibly store to SharedPreferences
     * if {@link #shouldPersist()} is true).
     * <p/>
     * This may not always be called. One example is if it should not persist
     * but there is no default value given.
     *
     * @param restorePersistedValue True to restore the persisted value;
     *                              false to use the given <var>defaultValue</var>.
     * @param defaultValue          The default value for this Preference. Only use this
     */
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mCurrentValue = this.getPersistedInt(DEFAULT_VALUE);
        } else {
            mCurrentValue = (Integer) defaultValue;
            persistValue(mCurrentValue);
        }
    }

    /**
     * Called when a Preference is being inflated and the default value
     * attribute needs to be read. Since different Preference types have
     * different value types, the subclass should get and return the default
     * value which will be its value type.
     * <p/>
     * For example, if the value type is String, the body of the method would
     * proxy to {@link android.content.res.TypedArray#getString(int)}.
     *
     * @param a     The set of attributes.
     * @param index The index of the default value attribute.
     * @return The default value of this preference type.
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    private void persistValue(int value) {
        final SharedPreferences.Editor editor = getSharedPreferences().edit();

        editor.putInt(SettingsActivity.PREFERENCES_DRAG_HANDLE_WIDTH, value);
        editor.commit();
    }

    /* Lifecycle methods */

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        if (isPersistent()) {
            return superState;
        }

        final SavedState state = new SavedState(superState);

        state.value = mCurrentValue;

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mSlider.setProgress(savedState.value);
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeInt(value);
        }

        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
