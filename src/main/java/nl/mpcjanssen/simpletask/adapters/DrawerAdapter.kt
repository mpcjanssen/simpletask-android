package nl.mpcjanssen.simpletask.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import nl.mpcjanssen.simpletask.R

import java.util.ArrayList

class DrawerAdapter(private val m_inflater: LayoutInflater,
                    contextHeader: String,
                    contexts: List<String>,
                    projectHeader: String,
                    projects: List<String>) : BaseAdapter(), ListAdapter {

    internal var items: ArrayList<String>
    var contextHeaderPosition: Int = 0
        internal set
    var projectsHeaderPosition: Int = 0
        internal set

    init {
        this.items = ArrayList<String>()
        this.items.add(contextHeader)
        contextHeaderPosition = 0
        this.items.addAll(contexts)
        projectsHeaderPosition = items.size
        this.items.add(projectHeader)
        this.items.addAll(projects)
    }

    private fun isHeader(position: Int): Boolean {
        return position == contextHeaderPosition || position == projectsHeaderPosition
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): String {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true // To change body of implemented methods use File |
        // Settings | File Templates.
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val tv: TextView
        if (isHeader(position)) {
            convertView = m_inflater.inflate(R.layout.drawer_list_header, parent, false)
            tv = convertView as TextView
            val lv = parent as ListView
            if (lv.isItemChecked(position)) {
                tv.text = items[position] + " inverted"
            } else {
                tv.text = items[position]
            }

        } else {
            if (convertView == null) {
                convertView = m_inflater.inflate(R.layout.drawer_list_item_checked, parent, false)
            }
            tv = convertView as TextView
            tv.text = items[position].substring(1)
        }

        return convertView
    }

    override fun getItemViewType(position: Int): Int {
        if (isHeader(position)) {
            return 0
        } else {
            return 1
        }
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun isEmpty(): Boolean {
        return items.size == 0
    }

    override fun areAllItemsEnabled(): Boolean {
        return true
    }

    override fun isEnabled(position: Int): Boolean {
        return true
    }

    fun getIndexOf(item: String): Int {
        return items.indexOf(item)
    }
}
