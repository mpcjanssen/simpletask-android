package nl.mpcjanssen.simpletask.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.task.Task;

public class DrawerAdapter extends BaseAdapter implements ListAdapter {

    ArrayList<String> items;
    int contextHeaderPos;
    int projectHeaderPos;
    private LayoutInflater m_inflater;

    public DrawerAdapter(LayoutInflater inflater, List<String> contexts, List<String> projects) {
        this.m_inflater = inflater;
        this.items = new ArrayList<String>();
        this.items.add("Lists");
        contextHeaderPos = 0;
        this.items.addAll(contexts);
        projectHeaderPos = items.size();
        this.items.add("Tags");
        this.items.addAll(projects);
    }

    private boolean isHeader(int position) {
        return (position == contextHeaderPos || position == projectHeaderPos);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public String getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true; // To change body of implemented methods use File |
        // Settings | File Templates.
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv;
        if (isHeader(position)) {
            convertView = m_inflater.inflate(R.layout.drawer_list_header, null);
            tv = (TextView) convertView;
            ListView lv = (ListView) parent;
            if (lv.isItemChecked(position)) {
                tv.setText(items.get(position) + " inverted");
            } else {
                tv.setText(items.get(position));
            }

        } else {
            if (convertView == null) {
                convertView = m_inflater.inflate(R.layout.drawer_list_item, null);
            }
            tv = (TextView) convertView;
            tv.setText(items.get(position).substring(1));
        }

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeader(position)) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return items.size() == 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
            return true;
    }

    public int getIndexOf(String item) {
        return items.indexOf(item);
    }

    public int getContextHeaderPosition () {
        return contextHeaderPos;
    }

    public int getProjectsHeaderPosition () {
        return projectHeaderPos;
    }
}
