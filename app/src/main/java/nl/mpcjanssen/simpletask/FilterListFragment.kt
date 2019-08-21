package nl.mpcjanssen.simpletask

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.multi_filter.*
import kotlin.collections.ArrayList

class FilterListFragment : Fragment() {
    private var mSelectedItems: ArrayList<String>? = null
    private var not: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() this:$this")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() this:$this")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState() this:$this")
        outState.putStringArrayList("selectedItems", getSelectedItems())
        outState.putBoolean("not", getNot())

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView() this:$this savedInstance:$savedInstanceState")
        Log.d(TAG, "Fragment bundle:$this arguments:$arguments")
        val layout = inflater.inflate(R.layout.multi_filter,
                container, false) as LinearLayout

        if (savedInstanceState != null) {
            mSelectedItems = savedInstanceState.getStringArrayList("selectedItems")
            not = savedInstanceState.getBoolean("not")
        } else {
            mSelectedItems = arguments?.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS)
            not = arguments?.getBoolean(FilterActivity.INITIAL_NOT) ?: false
        }

        return layout
    }
    override fun onResume() {
        super.onResume()

        activity?.let { act ->
            listview.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            val items = arguments?.getStringArrayList(FilterActivity.FILTER_ITEMS) ?: emptyList<String>()
            listview.adapter = ArrayAdapter(act,
                    R.layout.simple_list_item_multiple_choice, items)
            for (i in items.indices) {
                if (mSelectedItems != null && mSelectedItems!!.contains(items[i])) {
                    listview.setItemChecked(i, true)
                }
            }
            checkbox.isChecked = not
        }
    }

    fun getNot(): Boolean {
        return if (mSelectedItems == null) {
            // Tab was not displayed so no selections were changed
            arguments?.getBoolean(FilterActivity.INITIAL_NOT)?: false
        } else {
            checkbox.isChecked
        }
    }

    fun getSelectedItems(): ArrayList<String> {
        val arr = ArrayList<String>()
        if (mSelectedItems == null || listview == null) {
            // Tab was not displayed so no selections were changed
            return arguments?.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS) ?: ArrayList()
        }
        val size = listview.count
        for (i in 0 until size) {
            if (listview.isItemChecked(i)) {
                arr.add(listview.adapter.getItem(i) as String)
            }
        }
        return arr
    }

    companion object {
        internal val TAG = FilterListFragment::class.java.simpleName
    }
}
