package nl.mpcjanssen.simpletask

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.mobeta.android.dslv.DragSortListView
import nl.mpcjanssen.simpletask.util.Config
import java.util.*
import kotlin.collections.ArrayList

class FilterSortFragment : Fragment() {

    private var originalItems: ArrayList<String>? = null
    private var lv: DragSortListView? = null
    internal lateinit var adapter: SortItemAdapter
    internal var directions = ArrayList<String>()
    internal var adapterList = ArrayList<String>()
    internal var sortUpId: Int = 0
    internal var sortDownId: Int = 0

    internal lateinit var m_app: TodoApplication

    private val onDrop = DragSortListView.DropListener { from, to ->
        if (from != to) {
            val item = adapter.getItem(from)
            adapter.remove(item)
            adapter.insert(item, to)
            val sortItem = directions[from]
            directions.removeAt(from)
            directions.add(to, sortItem)
        }
    }

    private val onRemove = DragSortListView.RemoveListener { which -> adapter.remove(adapter.getItem(which)) }

    private // this DSLV xml declaration does not call for the use
            // of the default DragSortController; therefore,
            // DSLVFragment has a buildController() method.
    val layout: Int
        get() = R.layout.simple_list_item_single_choice

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val arguments = arguments
        if (originalItems == null) {
            if (savedInstanceState != null) {
                originalItems = savedInstanceState.getStringArrayList(STATE_SELECTED)
            } else {
                originalItems = arguments?.getStringArrayList(FilterActivity.FILTER_ITEMS)?: ArrayList()
            }
        }
        Log.d(TAG, "Created view with: " + originalItems)
        m_app = TodoApplication.app

        // Set the proper theme
        if (TodoApplication.config.isDarkTheme || TodoApplication.config.isBlackTheme) {
            sortDownId = R.drawable.ic_action_sort_down_dark
            sortUpId = R.drawable.ic_action_sort_up_dark
        } else {
            sortDownId = R.drawable.ic_action_sort_down
            sortUpId = R.drawable.ic_action_sort_up
        }

        adapterList.clear()
        val layout: LinearLayout

        layout = inflater.inflate(R.layout.single_filter,
                container, false) as LinearLayout

        val keys = resources.getStringArray(R.array.sortKeys)
        for (item in originalItems!!) {
            val parts = item.split("!".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val sortType: String
            var sortDirection: String
            if (parts.size == 1) {
                sortType = parts[0]
                sortDirection = Query.NORMAL_SORT
            } else {
                sortDirection = parts[0]
                sortType = parts[1]
                if (sortDirection.isEmpty() || sortDirection != Query.REVERSED_SORT) {
                    sortDirection = Query.NORMAL_SORT
                }
            }

            val index = Arrays.asList(*keys).indexOf(sortType)
            if (index != -1) {
                adapterList.add(sortType)
                directions.add(sortDirection)
                keys[index] = null
            }
        }

        // Add sorts not already in the sortlist
        for (item in keys) {
            if (item != null) {
                adapterList.add(item)
                directions.add(Query.NORMAL_SORT)
            }
        }

        lv = layout.findViewById(R.id.dslistview) as DragSortListView
        lv!!.setDropListener(onDrop)
        lv!!.setRemoveListener(onRemove)

        adapter = SortItemAdapter(activity, R.layout.sort_list_item, R.id.text, adapterList)
        lv!!.adapter = adapter
        lv!!.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            var direction = directions[position]
            if (direction == Query.REVERSED_SORT) {
                direction = Query.NORMAL_SORT
            } else {
                direction = Query.REVERSED_SORT
            }
            directions.removeAt(position)
            directions.add(position, direction)
            adapter.notifyDataSetChanged()
        }
        return layout
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_SELECTED, selectedItem)
    }

    override fun onDestroyView() {
        originalItems = selectedItem
        super.onDestroyView()
    }

    val selectedItem: ArrayList<String>
        get() {
            val multiSort = ArrayList<String>()
            when {
                lv != null -> for (i in 0..adapter.count - 1) {
                    multiSort.add(directions[i] + Query.SORT_SEPARATOR + adapter.getSortType(i))
                }
                originalItems != null -> multiSort.addAll(originalItems as ArrayList<String>)
                else -> multiSort.addAll(arguments?.getStringArrayList(FilterActivity.FILTER_ITEMS) ?: java.util.ArrayList())
            }
            return multiSort
        }

    inner class SortItemAdapter(context: Context?, resource: Int, textViewResourceId: Int, objects: List<String>) : ArrayAdapter<String>(context, resource, textViewResourceId, objects) {

        private val names: Array<String>

        init {
            names = resources.getStringArray(R.array.sort)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row = super.getView(position, convertView, parent)
            val reverseButton = row.findViewById(R.id.reverse_button) as ImageButton
            val label = row.findViewById(R.id.text) as TextView
            label.text = TodoApplication.config.getSortString(adapterList[position])

            if (directions[position] == Query.REVERSED_SORT) {
                reverseButton.setBackgroundResource(sortUpId)
            } else {
                reverseButton.setBackgroundResource(sortDownId)
            }
            return row
        }

        fun getSortType(position: Int): String {
            return adapterList[position]
        }
    }

    companion object {

        private val STATE_SELECTED = "selectedItem"
        internal val TAG = FilterActivity::class.java.simpleName
    }
}
