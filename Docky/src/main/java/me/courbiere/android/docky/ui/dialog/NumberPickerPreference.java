package me.courbiere.android.docky.ui.dialog;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import me.courbiere.android.docky.R;

/**
 * A {@link android.preference.DialogPreference} that provides a user with the means to select an
 * integer from a {@link android.widget.NumberPicker}
 */
public class NumberPickerPreference extends DialogPreference {
    private static final String TAG = "NumberPickerPreference";

    public static final int DEFAULT_MIN_VALUE = 0;
    public static final int DEFAULT_MAX_VALUE = 20;
    public static final int DEFAULT_VALUE = 0;

    private int mValue;
    private NumberPicker mNumberPicker;

    public NumberPickerPreference(Context context) {
        this(context, null);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setDialogLayoutResource(R.layout.number_picker_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedInt(DEFAULT_VALUE) : (Integer) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView dialogMessageText = (TextView) view.findViewById(R.id.text_dialog_message);
        dialogMessageText.setText(getDialogMessage());

        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        mNumberPicker.setMinValue(DEFAULT_MIN_VALUE);
        mNumberPicker.setMaxValue(DEFAULT_MAX_VALUE);
        mNumberPicker.setValue(mValue);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int numberPickerValue = mNumberPicker.getValue();

            if (callChangeListener(numberPickerValue)) {
                setValue(numberPickerValue);
            }
        }
    }

    public void setValue(int value) {
        value = Math.max(Math.min(value, DEFAULT_MAX_VALUE), DEFAULT_MIN_VALUE);

        if (value != mValue) {
            mValue = value;
            persistInt(value);
            notifyChanged();
        }
    }
}
