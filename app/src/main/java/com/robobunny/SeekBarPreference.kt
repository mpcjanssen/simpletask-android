package com.robobunny

import android.content.Context
import android.content.res.TypedArray
import android.preference.Preference
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import nl.mpcjanssen.simpletask.R

class SeekBarPreference : Preference, OnSeekBarChangeListener {

    private val TAG = javaClass.name

    private var mMaxValue = 100
    private var mMinValue = 0
    private var mInterval = 1
    private var mCurrentValue: Int = 0
    private var mUnitsRight = ""
    private var mSeekBar: SeekBar? = null

    private var mStatusText: TextView? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initPreference(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initPreference(context, attrs)
    }

    private fun initPreference(context: Context, attrs: AttributeSet) {
        setValuesFromXml(attrs)
        mSeekBar = SeekBar(context, attrs)
        mSeekBar!!.max = mMaxValue - mMinValue
        mSeekBar!!.setOnSeekBarChangeListener(this)

        layoutResource = R.layout.seek_bar_preference
    }

    private fun setValuesFromXml(attrs: AttributeSet) {
        mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 100)
        mMinValue = attrs.getAttributeIntValue(APPLICATIONNS, "min", 0)

        val units = getAttributeStringValue(attrs, APPLICATIONNS, "units", "")
        mUnitsRight = getAttributeStringValue(attrs, APPLICATIONNS, "unitsRight", units)

        try {
            val newInterval = attrs.getAttributeValue(APPLICATIONNS, "interval")
            if (newInterval != null)
                mInterval = Integer.parseInt(newInterval)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid interval value", e)
        }

    }

    private fun getAttributeStringValue(attrs: AttributeSet, namespace: String, name: String, defaultValue: String): String {
        return attrs.getAttributeValue(namespace, name) ?: defaultValue
    }

    public override fun onBindView(view: View) {
        super.onBindView(view)

        try {
            // move our seekbar to the new view we've been given
            val oldContainer = mSeekBar!!.parent
            val newContainer = view.findViewById(R.id.seekBarPrefBarContainer) as ViewGroup

            if (oldContainer !== newContainer) {
                // remove the seekbar from the old view
                if (oldContainer != null) {
                    (oldContainer as ViewGroup).removeView(mSeekBar)
                }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer.removeAllViews()
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error binding view: " + ex.toString())
        }

        //if dependency is false from the beginning, disable the seek bar
        if (!view.isEnabled) {
            mSeekBar!!.isEnabled = false
        }

        updateView(view)
    }

    /**
     * Update a SeekBarPreference view with our current state
     * @param view
     */
    protected fun updateView(view: View) {

        try {
            mStatusText = view.findViewById(R.id.seekBarPrefValue) as TextView

            mStatusText!!.text = mCurrentValue.toString()
            mStatusText!!.minimumWidth = 30

            mSeekBar!!.progress = mCurrentValue - mMinValue

            val unitsRight = view.findViewById(R.id.seekBarPrefUnitsRight) as TextView
            unitsRight.text = mUnitsRight

        } catch (e: Exception) {
            Log.e(TAG, "Error updating seek bar preference", e)
        }

    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        var newValue = progress + mMinValue

        if (newValue > mMaxValue)
            newValue = mMaxValue
        else if (newValue < mMinValue)
            newValue = mMinValue
        else if (mInterval != 1 && newValue % mInterval != 0)
            newValue = Math.round(newValue.toFloat() / mInterval) * mInterval

        // change rejected, revert to the previous value
        if (!callChangeListener(newValue)) {
            seekBar.progress = mCurrentValue - mMinValue
            return
        }

        // change accepted, store it
        mCurrentValue = newValue
        mStatusText!!.text = newValue.toString()
        persistInt(newValue)

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        notifyChanged()
    }

    override fun onGetDefaultValue(ta: TypedArray, index: Int): Any {

        return ta.getInt(index, DEFAULT_VALUE)

    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {

        if (restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue)
        } else {
            var temp = 0
            try {
                temp = defaultValue as Int
            } catch (ex: Exception) {
                Log.e(TAG, "Invalid default value: " + defaultValue.toString())
            }

            persistInt(temp)
            mCurrentValue = temp
        }

    }

    /**
     * make sure that the seekbar is disabled if the preference is disabled
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        mSeekBar!!.isEnabled = enabled
    }

    override fun onDependencyChanged(dependency: Preference, disableDependent: Boolean) {
        super.onDependencyChanged(dependency, disableDependent)

        //Disable movement of seek bar when dependency is false
        if (mSeekBar != null) {
            mSeekBar!!.isEnabled = !disableDependent
        }
    }

    companion object {

        private val ANDROIDNS = "http://schemas.android.com/apk/res/android"
        private val APPLICATIONNS = "http://robobunny.com"
        private val DEFAULT_VALUE = 50
    }
}
