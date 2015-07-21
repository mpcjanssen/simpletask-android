package nl.mpcjanssen.simpletask.adapters;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import nl.mpcjanssen.simpletask.R;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
    private List<String> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        // each data item is just a string in this case
        public TextView mTextView;
        public ViewHolder(View v, TextView tv) {
            super(v);
            mView = v;
            mTextView = tv;
        }
        public void bindLog(String line) {
            mTextView.setText(line);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LogAdapter(List<String> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        LinearLayout v =(LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.log_item, parent, false);
        // set the view's size, margins, paddings and layout parameters
        TextView tv = (TextView) v.findViewById(R.id.logtext);
        ViewHolder vh = new ViewHolder(v,tv);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.bindLog(mDataset.get(position));

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}