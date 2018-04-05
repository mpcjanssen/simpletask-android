package nl.mpcjanssen.simpletask.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.buildware.widget.indeterm.IndeterminateCheckBox
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.*
import java.util.*

// Provide a suitable constructor (depends on the kind of dataset)
class ItemDialogAdapter(
        tasks: List<Task>,
        allItems: Collection<String>,
        retrieveFromTask: (Task) -> Set<String>
) : RecyclerView.Adapter<ItemDialogAdapter.ViewHolder>() {

    private val mItems: MutableList<Item>

    init {
        val checkedTaskItems = tasks.map { retrieveFromTask(it) }

        val onAny = checkedTaskItems.reduce { a, b -> a union b }
        val onAll = checkedTaskItems.reduce { a, b -> a intersect b }
        val onSome = onAny - onAll
        val onNone = allItems - onAny

        val sortedItems = alfaSort(onAll) + alfaSort(onSome) + alfaSort(onNone)

        /* val itemAdapter = ItemDialogAdapter(sortedAllItems, onAll, onSome) */

        mItems = sortedItems.map { item ->
            val state = when(item) {
                in onAll -> true
                in onSome -> null
                else -> false
            }
            Item(item, state)
        }.toMutableList()
    }

    fun changedItems(): List<Item> = mItems.filter { it.state != it.initialState }

    fun addItems(items: List<String>) {
        items.reversed().forEach { item ->
            val i = mItems.indexOfFirst { it.item == item }
            if (i == -1) {
                mItems.add(0, Item(item, true))
                notifyItemInserted(0)
            } else {
                val oldItem = mItems.removeAt(i)
                mItems.add(0, oldItem.apply { state = true })
                notifyItemMoved(i, 0)
                notifyItemChanged(0)
            }
        }
    }

    data class Item(val item: String, var state: Boolean?) {
        val initialState: Boolean? = state
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // each data item is just a string in this case
        var mCheckBox: IndeterminateCheckBox

        init {
            mCheckBox = v.findViewById<IndeterminateCheckBox>(R.id.indeterm_checkbox)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ItemDialogAdapter.ViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent.context).inflate(R.layout.keep_dialog_item, parent, false)
        // set the view's size, margins, paddings and layout parameters

        val vh = ViewHolder(v)
        return vh
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        val item = mItems[position]
        Logger.info("ItemAdapter", "onBindViewHolder $position : $item, was:${item.initialState}")

        // - replace the contents of the view with that element
        holder.mCheckBox.setOnStateChangedListener(null)
        holder.mCheckBox.text = item.item
        holder.mCheckBox.setIndeterminateUsed(item.initialState==null)
        holder.mCheckBox.state = item.state
        holder.mCheckBox.setOnStateChangedListener { _, b ->
            Logger.info("ItemAdapter", "state chaged $position:$item, new state: $b")
            item.state = b
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mItems.size
    }
}
