package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout


class FilterOtherFragment : Fragment() {
    private var cbHideCompleted: CheckBox? = null
    private var cbHideFuture: CheckBox? = null
    private var cbHideLists: CheckBox? = null
    private var cbHideTags: CheckBox? = null
    private var cbHideCreateDate: CheckBox? = null
    private var cbHideHidden: CheckBox? = null
    private var cbCreateAsThreshold: CheckBox? = null
    private var cbSortCaseSensitive: CheckBox? = null
    private var log: Logger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log = Logger
        log!!.debug(TAG, "onCreate() this:" + this)
    }

    override fun onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy()
        log!!.debug(TAG, "onDestroy() this:" + this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        log!!.debug(TAG, "onSaveInstanceState() this:" + this)
        outState.putBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, hideCompleted)
        outState.putBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, hideFuture)
        outState.putBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, hideLists)
        outState.putBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, hideTags)
        outState.putBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, hideCreateDate)
        outState.putBoolean(ActiveFilter.INTENT_CREATE_AS_THRESHOLD, createAsThreshold)
        outState.putBoolean(ActiveFilter.INTENT_SORT_CASE_SENSITIVE, sortCaseSensitive)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        log!!.debug(TAG, "onCreateView() this:" + this + " savedInstance:" + savedInstanceState)

        val arguments = arguments

        log!!.debug(TAG, "Fragment bundle:" + this + " arguments:" + arguments)
        val layout = inflater.inflate(R.layout.other_filter,
                container, false) as LinearLayout

        cbHideCompleted = layout.findViewById(R.id.cb_show_completed) as CheckBox
        cbHideFuture = layout.findViewById(R.id.cb_show_future) as CheckBox
        cbHideLists = layout.findViewById(R.id.cb_show_lists) as CheckBox
        cbHideTags = layout.findViewById(R.id.cb_show_tags) as CheckBox
        cbHideCreateDate = layout.findViewById(R.id.cb_show_create_date) as CheckBox
        cbHideHidden = layout.findViewById(R.id.cb_show_hidden) as CheckBox
        cbCreateAsThreshold = layout.findViewById(R.id.cb_create_is_threshold) as CheckBox
        cbSortCaseSensitive = layout.findViewById(R.id.cb_sort_case_sensitive) as CheckBox
        if (savedInstanceState != null) {
            cbHideCompleted!!.isChecked = !savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false)
            cbHideFuture!!.isChecked = !savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false)
            cbHideLists!!.isChecked = !savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false)
            cbHideTags!!.isChecked = !savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false)
            cbHideCreateDate!!.isChecked = !savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false)
            cbHideHidden!!.isChecked = !savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_HIDDEN_FILTER, true)
            cbCreateAsThreshold!!.isChecked = savedInstanceState.getBoolean(ActiveFilter.INTENT_CREATE_AS_THRESHOLD, false)
            cbCreateAsThreshold!!.isChecked = savedInstanceState.getBoolean(ActiveFilter.INTENT_SORT_CASE_SENSITIVE, false)
        } else {
            cbHideCompleted!!.isChecked = !arguments.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false)
            cbHideFuture!!.isChecked = !arguments.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false)
            cbHideLists!!.isChecked = !arguments.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false)
            cbHideTags!!.isChecked = !arguments.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false)
            cbHideCreateDate!!.isChecked = !arguments.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false)
            cbHideHidden!!.isChecked = !arguments.getBoolean(ActiveFilter.INTENT_HIDE_HIDDEN_FILTER, true)
            cbCreateAsThreshold!!.isChecked = arguments.getBoolean(ActiveFilter.INTENT_CREATE_AS_THRESHOLD, true)
            cbSortCaseSensitive!!.isChecked = arguments.getBoolean(ActiveFilter.INTENT_SORT_CASE_SENSITIVE, true)
        }

        return layout
    }

    val hideCompleted: Boolean
        get() {
            if (cbHideCompleted == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false)
            } else {
                return !cbHideCompleted!!.isChecked
            }
        }

    val hideFuture: Boolean
        get() {
            if (cbHideCompleted == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false)
            } else {
                return !cbHideFuture!!.isChecked
            }
        }

    val hideHidden: Boolean
        get() {
            if (cbHideHidden == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_HIDE_HIDDEN_FILTER, true)
            } else {
                return !cbHideHidden!!.isChecked
            }
        }

    val hideLists: Boolean
        get() {
            if (cbHideCompleted == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false)
            } else {
                return !cbHideLists!!.isChecked
            }
        }
    val hideTags: Boolean
        get() {
            if (cbHideCompleted == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false)
            } else {
                return !cbHideTags!!.isChecked
            }
        }
    val hideCreateDate: Boolean
        get() {
            if (cbHideCreateDate == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false)
            } else {
                return !cbHideCreateDate!!.isChecked
            }
        }

    val createAsThreshold: Boolean
        get() {
            if (cbCreateAsThreshold == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_CREATE_AS_THRESHOLD, false)
            } else {
                return cbCreateAsThreshold!!.isChecked
            }
        }

    val sortCaseSensitive: Boolean
        get() {
            if (cbSortCaseSensitive == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_SORT_CASE_SENSITIVE, false)
            } else {
                return cbSortCaseSensitive!!.isChecked
            }
        }

    companion object {

        internal val TAG = FilterOtherFragment::class.java.simpleName
    }
}
