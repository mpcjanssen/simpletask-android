package nl.mpcjanssen.simpletask;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortListView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.mpcjanssen.simpletask.util.Strings;

public class FilterSortFragment extends Fragment {
    
    private final static String STATE_SELECTED = "selectedItem";
    final static String TAG = FilterActivity.class.getSimpleName();

    private ArrayList<String> originalItems;
    private DragSortListView lv;
    SortItemAdapter adapter;
    @NotNull
    ArrayList<String> directions = new ArrayList<String>();
    @NotNull
    ArrayList<String> adapterList = new ArrayList<String>();
    int sortUpId;
    int sortDownId;

    TodoApplication m_app;

    @NotNull
    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                        String item = adapter.getItem(from);
                        adapter.remove(item);
                        adapter.insert(item, to);
                        String sortItem = directions.get(from);
                        directions.remove(from);
                        directions.add(to,sortItem);
                    }
                }
            };

    @NotNull
    private DragSortListView.RemoveListener onRemove =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    adapter.remove(adapter.getItem(which));
                }
            };

    protected int getLayout() {
        // this DSLV xml declaration does not call for the use
        // of the default DragSortController; therefore,
        // DSLVFragment has a buildController() method.
        return R.layout.simple_list_item_single_choice;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (originalItems == null) {
            if (savedInstanceState != null) {
                originalItems = savedInstanceState.getStringArrayList(STATE_SELECTED);
            } else {
                originalItems = arguments.getStringArrayList(FilterActivity.FILTER_ITEMS);
            }
        }
        Log.v(TAG, "Created view with: " + originalItems);
        m_app = (TodoApplication) getActivity().getApplication();

        // Set the proper theme
        if (m_app.isDarkTheme()) {
            sortDownId = R.drawable.ic_action_sort_down_dark;
            sortUpId = R.drawable.ic_action_sort_up_dark;
        } else {
            sortDownId = R.drawable.ic_action_sort_down;
            sortUpId = R.drawable.ic_action_sort_up;
        }

        adapterList.clear();
        LinearLayout layout;

            layout = (LinearLayout) inflater.inflate(R.layout.single_filter,
                    container, false);

        String[] values = getResources().getStringArray(R.array.sort);
        String[] keys = getResources().getStringArray(R.array.sortKeys);
        for (String item : originalItems) {
            String[] parts =  item.split("!");
            String sortType ;
            String sortDirection;
            if (parts.length==1) {
               sortType = parts[0];
               sortDirection = ActiveFilter.NORMAL_SORT;
            } else {
                sortDirection = parts[0];
                sortType = parts[1];
                if (Strings.isEmptyOrNull(sortDirection) || !sortDirection.equals(ActiveFilter.REVERSED_SORT)) {
                    sortDirection = ActiveFilter.NORMAL_SORT;
                }
            }

            int index = Arrays.asList(keys).indexOf(sortType);
            if (index!=-1) {
                adapterList.add(values[index]);
                directions.add(sortDirection);
                values[index]=null;
            }
        }

        // Add sorts not already in the sortlist
        for (String item : values) {
            if (item!=null) {
                adapterList.add(item);
                directions.add(ActiveFilter.NORMAL_SORT);
            }
        }

        lv = (DragSortListView) layout.findViewById(R.id.dslistview);
        lv.setDropListener(onDrop);
        lv.setRemoveListener(onRemove);

        adapter = new SortItemAdapter(getActivity(), R.layout.sort_list_item, R.id.text, adapterList);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String direction = directions.get(position);
                if (direction.equals(ActiveFilter.REVERSED_SORT)) {
                    direction = ActiveFilter.NORMAL_SORT;
                } else {
                    direction = ActiveFilter.REVERSED_SORT;
                }
                directions.remove(position);
                directions.add(position,direction);
                adapter.notifyDataSetChanged();
            }
        });
        return layout;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_SELECTED, getSelectedItem());
    }

    @Override
    public void onDestroyView () {
        originalItems = getSelectedItem();
        super.onDestroyView();
    }

    @NotNull
    public ArrayList<String> getSelectedItem() {
        ArrayList<String> multiSort = new ArrayList<String>();
        if (lv != null) {
            for (int i = 0 ; i< adapter.getCount() ; i++) {
               multiSort.add(directions.get(i) + ActiveFilter.SORT_SEPARATOR + adapter.getSortType(i));
            }
        } else if (originalItems !=null ) {
            multiSort.addAll(originalItems);
        }
        return multiSort;
    }

    public class SortItemAdapter extends ArrayAdapter<String> {

        public SortItemAdapter(@NotNull Context context, int resource, int textViewResourceId, List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);
            ImageButton reverseButton = (ImageButton)row.findViewById(R.id.reverse_button);

            if (directions.get(position).equals(ActiveFilter.REVERSED_SORT)) {
                reverseButton.setBackgroundResource(sortUpId);
            } else {
                reverseButton.setBackgroundResource(sortDownId);
            }
            return row;
        }

        public String getSortType(int position) {
            String[] values = getResources().getStringArray(R.array.sort);
            String[] keys = getResources().getStringArray(R.array.sortKeys);
            View row = this.getView(position, null, null);
            TextView text = (TextView)row.findViewById(R.id.text);
            int index = Arrays.asList(values).indexOf(text.getText().toString());
            return keys[index];
        }
    }

	public void defaultSort() {
		adapterList.clear();
		directions.clear();
		for (String value : getResources().getStringArray(R.array.sort)) {
			adapterList.add(value);
			directions.add(ActiveFilter.NORMAL_SORT);
		}
		adapter.notifyDataSetChanged();
		
	}
}
