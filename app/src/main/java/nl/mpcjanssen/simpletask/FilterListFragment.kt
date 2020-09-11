package nl.mpcjanssen.simpletask

import android.app.ActionBar
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*
import kotlin.collections.ArrayList

class FilterListFragment : Fragment() {
    private var lv: ListView? = null
    private var cb: CheckBox? = null
    private val gestureDetector: GestureDetector? = null
    internal var actionbar: ActionBar? = null
    private var mSelectedItems: ArrayList<String>? = null
    private var not: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() this:" + this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() this:" + this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState() this:" + this)
        outState.putStringArrayList("selectedItems", getSelectedItems())
        outState.putBoolean("not", getNot())

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView() this:" + this + " savedInstance:" + savedInstanceState)

        val arguments = arguments
        val items = arguments?.getStringArrayList(FilterActivity.FILTER_ITEMS) ?: emptyList<String>()
        actionbar = activity?.actionBar

        if (savedInstanceState != null) {
            mSelectedItems = savedInstanceState.getStringArrayList("selectedItems")
            not = savedInstanceState.getBoolean("not")
        } else {
            mSelectedItems = arguments?.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS)
            not = arguments?.getBoolean(FilterActivity.INITIAL_NOT) ?: false
        }

        Log.d(TAG, "Fragment bundle:" + this + " arguments:" + arguments)
        val layout = inflater.inflate(R.layout.multi_filter,
                container, false) as LinearLayout

        cb = layout.findViewById(R.id.checkbox) as CheckBox

        lv = layout.findViewById(R.id.listview) as ListView
        lv!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE

        lv!!.adapter = activity?.let {
            ArrayAdapter(it,
                R.layout.simple_list_item_multiple_choice, items)
        }

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
            return arguments?.getBoolean(FilterActivity.INITIAL_NOT)?: false
        } else {
            return cb!!.isChecked
        }
    }

    fun getSelectedItems(): ArrayList<String> {

        val arr = ArrayList<String>()
        if (mSelectedItems == null || lv == null) {
            // Tab was not displayed so no selections were changed
            return arguments?.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS) ?: ArrayList<String>()
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
