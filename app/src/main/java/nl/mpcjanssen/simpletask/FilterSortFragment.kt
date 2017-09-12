package nl.mpcjanssen.simpletask

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.single_filter.*
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.isEmptyOrNull
import java.util.*

class FilterSortFragment : Fragment() {

    private var originalItems: ArrayList<String>? = null
    internal lateinit var adapter: SortItemAdapter
    internal var directions = ArrayList<String>()
    internal var adapterList = ArrayList<String>()
    internal var sortUpId: Int = 0
    internal var sortDownId: Int = 0

    internal lateinit var m_app: TodoApplication

    private var log: Logger? = null

    private // this DSLV xml declaration does not call for the use
            // of the default DragSortController; therefore,
            // DSLVFragment has a buildController() method.
    val layout: Int
        get() = R.layout.simple_list_item_single_choice

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        log = Logger

        val arguments = arguments
        if (originalItems == null) {
            if (savedInstanceState != null) {
                originalItems = savedInstanceState.getStringArrayList(STATE_SELECTED)
            } else {
                originalItems = arguments.getStringArrayList(FilterActivity.FILTER_ITEMS)
            }
        }
        log!!.debug(TAG, "Created view with: " + originalItems!!)
        m_app = activity.application as TodoApplication

        // Set the proper theme
        if (Config.isDarkTheme) {
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
                sortDirection = ActiveFilter.NORMAL_SORT
            } else {
                sortDirection = parts[0]
                sortType = parts[1]
                if (isEmptyOrNull(sortDirection) || sortDirection != ActiveFilter.REVERSED_SORT) {
                    sortDirection = ActiveFilter.NORMAL_SORT
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
                directions.add(ActiveFilter.NORMAL_SORT)
            }
        }

        val lv = layout.findViewById(R.id.dslistview) as ListView


        adapter = SortItemAdapter(activity, R.layout.sort_list_item, R.id.text, adapterList)
        lv.adapter = adapter
        lv.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            var direction = directions[position]
            if (direction == ActiveFilter.REVERSED_SORT) {
                direction = ActiveFilter.NORMAL_SORT
            } else {
                direction = ActiveFilter.REVERSED_SORT
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

            if (dslistview != null) {
                for (i in 0..adapter.count - 1) {
                    multiSort.add(directions[i] + ActiveFilter.SORT_SEPARATOR + adapter.getSortType(i))
                }
            } else if (originalItems != null) {
                multiSort.addAll(originalItems as ArrayList<String>)
            } else {
                multiSort.addAll(arguments.getStringArrayList(FilterActivity.FILTER_ITEMS))
            }
            return multiSort
        }

    inner class SortItemAdapter(context: Context, resource: Int, textViewResourceId: Int, objects: List<String>) : ArrayAdapter<String>(context, resource, textViewResourceId, objects) {

        private val names: Array<String>

        init {
            names = resources.getStringArray(R.array.sort)
        }

        fun moveItemUp(position: Int) {
            val above = adapterList[position-1]
            adapterList[position-1] = adapterList[position]
            adapterList[position] = above

        }
        fun moveItemDown(position: Int) {
            val below = adapterList[position+1]
            adapterList[position+1] = adapterList[position]
            adapterList[position] = below
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row = super.getView(position, convertView, parent)
            val reverseButton = row.findViewById(R.id.reverse_button) as ImageButton
            val upButton = row.findViewById(R.id.up_button) as ImageButton
            val downButton = row.findViewById(R.id.down_button) as ImageButton
            val label = row.findViewById(R.id.text) as TextView
            label.text = m_app.getSortString(adapterList[position])
            if (position == adapter.count -1) {
                downButton.isEnabled = false
                downButton.isClickable = false
            } else {
                downButton.isEnabled = true
                downButton.isClickable = true
            }
            if (position == 0) {
                upButton.isEnabled = false
                upButton.isClickable = false
            } else {
                upButton.isEnabled = true
                upButton.isClickable = true
            }
            upButton.setBackgroundResource(sortUpId)
            downButton.setBackgroundResource(sortDownId)
            if (directions[position] == ActiveFilter.REVERSED_SORT) {
                reverseButton.setBackgroundResource(sortUpId)
            } else {
                reverseButton.setBackgroundResource(sortDownId)
            }
            upButton.setOnClickListener {
                moveItemUp(position)
                notifyDataSetChanged()
            }
            downButton.setOnClickListener {
                moveItemDown(position)
                notifyDataSetChanged()
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
