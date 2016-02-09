/**

 * Copyright (c) 2015 Vojtech Kral

 * LICENSE:

 * Simpletas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Simpletask is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Sinpletask.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Vojtech Kral
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2015 Vojtech Kral
 */

package nl.mpcjanssen.simpletask

import android.content.Context
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View


class TimePreference @JvmOverloads constructor(ctx: Context, attrs: AttributeSet?, defStyle: Int = android.R.attr.dialogPreferenceStyle) : DialogPreference(ctx, attrs, defStyle) {
    private var m_minutes = 0
    private var m_picker: android.widget.TimePicker? = null

    // Constructor is used from preferences.xml
    @SuppressWarnings("unused")
    constructor(ctx: Context) : this(ctx, null) {
    }

    init {
        setPositiveButtonText(R.string.ok)
        setNegativeButtonText(R.string.cancel)
    }

    override fun onCreateDialogView(): View {
        val picker = android.widget.TimePicker(context)
        picker.setIs24HourView(DateFormat.is24HourFormat(context))
        m_picker = picker
        return picker
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)
        m_picker!!.currentHour = m_minutes / 60
        m_picker!!.currentMinute = m_minutes % 60
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        if (positiveResult) {
            m_minutes = m_picker!!.currentHour * 60 + m_picker!!.currentMinute

            if (callChangeListener(m_minutes)) {
                persistInt(m_minutes)
                notifyChanged()
            }
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInt(index, 0)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        if (restoreValue) {
            m_minutes = getPersistedInt(0)
        } else {
            if (defaultValue == null) {
                m_minutes = 0
            } else {
                m_minutes = defaultValue as Int
            }
        }
    }
}
