package nl.mpcjanssen.simpletask.adapters

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.buildware.widget.indeterm.IndeterminateCheckBox
import nl.mpcjanssen.simpletask.R
import java.util.*

class ItemDialogAdapter// Provide a suitable constructor (depends on the kind of dataset)
(private val mItems: ArrayList<String>,
 private val onAll: HashSet<String>,
 private val onSome: HashSet<String>
 ) : RecyclerView.Adapter<ItemDialogAdapter.ViewHolder>() {

    val checkboxes = SparseArray<IndeterminateCheckBox>()

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // each data item is just a string in this case
        var mCheckBox: IndeterminateCheckBox

        init {
            mCheckBox = v.findViewById(R.id.indeterm_checkbox) as IndeterminateCheckBox
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
        // - replace the contents of the view with that element
        val viewItem = mItems[position]
        holder.mCheckBox.text = viewItem
        holder.mCheckBox.setIndeterminateUsed(false)
        if (viewItem in onAll) {
            holder.mCheckBox.state = true
        } else if (viewItem in onSome) {
            holder.mCheckBox.state = null
            holder.mCheckBox.setIndeterminateUsed(true)
        } else {
            holder.mCheckBox.state = false
        }
        checkboxes.put(position, holder.mCheckBox)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mItems.size
    }
}