package nl.mpcjanssen.simpletask;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.mobeta.android.dslv.DragSortListView;
import nl.mpcjanssen.simpletask.util.Strings;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterSortFragment extends Fragment {
    
    private final static String STATE_SELECTED = "selectedItem";
    final static String TAG = FilterActivity.class.getSimpleName();

    private ArrayList<String> originalItems;
    private DragSortListView lv;
    SortItemAdapter adapter;
    @NonNull
    ArrayList<String> directions = new ArrayList<>();
    @NonNull
    ArrayList<String> adapterList = new ArrayList<>();
    int sortUpId;
    int sortDownId;

    TodoApplication m_app;

    @NonNull
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

    @NonNull
    private DragSortListView.RemoveListener onRemove =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    adapter.remove(adapter.getItem(which));
                }
            };
    private Logger log;

    protected int getLayout() {
        // this DSLV xml declaration does not call for the use
        // of the default DragSortController; therefore,
        // DSLVFragment has a buildController() method.
        return R.layout.simple_list_item_single_choice;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        log = Logger.INSTANCE;

        Bundle arguments = getArguments();
        if (originalItems == null) {
            if (savedInstanceState != null) {
                originalItems = savedInstanceState.getStringArrayList(STATE_SELECTED);
            } else {
                originalItems = arguments.getStringArrayList(FilterActivity.FILTER_ITEMS);
            }
        }
        log.debug(TAG, "Created view with: " + originalItems);
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
                adapterList.add(sortType);
                directions.add(sortDirection);
                keys[index]=null;
            }
        }

        // Add sorts not already in the sortlist
        for (String item : keys) {
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_SELECTED, getSelectedItem());
    }

    @Override
    public void onDestroyView () {
        originalItems = getSelectedItem();
        super.onDestroyView();
    }

    @NonNull
    public ArrayList<String> getSelectedItem() {
        ArrayList<String> multiSort = new ArrayList<>();
        if (lv != null) {
            for (int i = 0 ; i< adapter.getCount() ; i++) {
               multiSort.add(directions.get(i) + ActiveFilter.SORT_SEPARATOR + adapter.getSortType(i));
            }
        } else if (originalItems !=null ) {
            multiSort.addAll(originalItems);
        } else {
            multiSort.addAll(getArguments().getStringArrayList(FilterActivity.FILTER_ITEMS));
        }
        return multiSort;
    }

    public class SortItemAdapter extends ArrayAdapter<String> {

        private final String[] names;

        public SortItemAdapter(@NonNull Context context, int resource, int textViewResourceId, List<String> objects) {
            super(context, resource, textViewResourceId, objects);
            names = getResources().getStringArray(R.array.sort);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);
            ImageButton reverseButton = (ImageButton)row.findViewById(R.id.reverse_button);
            TextView label = (TextView)row.findViewById(R.id.text);
            label.setText(m_app.getSortString(adapterList.get(position)));

            if (directions.get(position).equals(ActiveFilter.REVERSED_SORT)) {
                reverseButton.setBackgroundResource(sortUpId);
            } else {
                reverseButton.setBackgroundResource(sortDownId);
            }
            return row;
        }

        public String getSortType(int position) {
            return adapterList.get(position);
        }
    }
}
