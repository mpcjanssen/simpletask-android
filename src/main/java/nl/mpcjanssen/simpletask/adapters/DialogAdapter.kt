package nl.mpcjanssen.simpletask.adapters

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView

import java.util.ArrayList
import java.util.HashSet

import nl.mpcjanssen.simpletask.R

class DialogAdapter// Provide a suitable constructor (depends on the kind of dataset)
(private val mItems: ArrayList<String>, private val mSelected: HashSet<String>) : RecyclerView.Adapter<DialogAdapter.ViewHolder>() {

    val spinners = SparseArray<Spinner>()

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        // each data item is just a string in this case
        var mTextView: TextView
        var mSpinner: Spinner

        init {
            mTextView = v.findViewById(R.id.itemName) as TextView
            mSpinner = v.findViewById(R.id.action_spinner) as Spinner
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): DialogAdapter.ViewHolder {
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
        holder.mTextView.text = viewItem
        if (viewItem in mSelected) {
            holder.mSpinner.setSelection(1)
        }
        spinners.put(position, holder.mSpinner)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mItems.size
    }
}