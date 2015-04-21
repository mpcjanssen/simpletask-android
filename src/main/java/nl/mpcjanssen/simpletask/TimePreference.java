/**
 *
 * Copyright (c) 2015 Vojtech Kral
 *
 * LICENSE:
 *
 * Simpletas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Simpletask is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Sinpletask.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Vojtech Kral
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2015 Vojtech Kral
 */

package nl.mpcjanssen.simpletask;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;
import android.text.format.DateFormat;
import org.jetbrains.annotations.NotNull;


public class TimePreference extends DialogPreference {
    private int m_minutes = 0;
    private TimePicker m_picker = null;

    @SuppressWarnings("unused")
    public TimePreference(Context ctx) {
        this(ctx, null);
    }

    public TimePreference(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public TimePreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);

        setPositiveButtonText(R.string.ok);
        setNegativeButtonText(R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {
        m_picker = new TimePicker(getContext());
        m_picker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        return m_picker;
    }

    @Override
    protected void onBindDialogView(@NotNull View v) {
        super.onBindDialogView(v);
        m_picker.setCurrentHour(m_minutes / 60);
        m_picker.setCurrentMinute(m_minutes % 60);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            m_minutes = m_picker.getCurrentHour() * 60 + m_picker.getCurrentMinute();

            if (callChangeListener(m_minutes)) {
                persistInt(m_minutes);
                notifyChanged();
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            m_minutes = getPersistedInt(0);
        } else {
            if (defaultValue == null) {
                m_minutes = 0;
            } else {
                m_minutes = (Integer) defaultValue;
            }
        }
    }
}
