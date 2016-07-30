package nl.mpcjanssen.simpletask


import android.app.ActionBar
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*

class FilterListFragment : Fragment() {
    private var lv: ListView? = null
    private var cb: CheckBox? = null
    private val gestureDetector: GestureDetector? = null
    internal var actionbar: ActionBar? = null
    private var mSelectedItems: ArrayList<String>? = null
    private var not: Boolean = false
    private var log: Logger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log = Logger
        log!!.debug(TAG, "onCreate() this:" + this)
    }

    override fun onDestroy() {
        super.onDestroy()
        log!!.debug(TAG, "onDestroy() this:" + this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        log!!.debug(TAG, "onSaveInstanceState() this:" + this)
        outState.putStringArrayList("selectedItems", getSelectedItems())
        outState.putBoolean("not", getNot())

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        log!!.debug(TAG, "onCreateView() this:" + this + " savedInstance:" + savedInstanceState)

        val arguments = arguments
        val items = arguments.getStringArrayList(FilterActivity.FILTER_ITEMS)
        actionbar = activity.actionBar

        if (savedInstanceState != null) {
            mSelectedItems = savedInstanceState.getStringArrayList("selectedItems")
            not = savedInstanceState.getBoolean("not")
        } else {
            mSelectedItems = arguments.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS)
            not = arguments.getBoolean(FilterActivity.INITIAL_NOT)
        }

        log!!.debug(TAG, "Fragment bundle:" + this + " arguments:" + arguments)
        val layout = inflater.inflate(R.layout.multi_filter,
                container, false) as LinearLayout

        cb = layout.findViewById(R.id.checkbox) as CheckBox

        lv = layout.findViewById(R.id.listview) as ListView
        lv!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE

        lv!!.adapter = ArrayAdapter(activity,
                R.layout.simple_list_item_multiple_choice, items)

        for (i in items.indices) {
            if (mSelectedItems != null && mSelectedItems!!.contains(items[i])) {
                lv!!.setItemChecked(i, true)
            }
        }

        cb!!.isChecked = not
        return layout
    }

    fun getNot(): Boolean {
        if (mSelectedItems == null) {
            // Tab was not displayed so no selections were changed
            return arguments.getBoolean(FilterActivity.INITIAL_NOT)
        } else {
            return cb!!.isChecked
        }
    }

    fun getSelectedItems(): ArrayList<String> {

        val arr = ArrayList<String>()
        if (mSelectedItems == null) {
            // Tab was not displayed so no selections were changed
            return arguments.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS)
        }
        val size = lv!!.count
        for (i in 0..size - 1) {
            if (lv!!.isItemChecked(i)) {
                arr.add(lv!!.adapter.getItem(i) as String)
            }
        }
        return arr
    }

    companion object {

        internal val TAG = FilterListFragment::class.java.simpleName
    }
}
