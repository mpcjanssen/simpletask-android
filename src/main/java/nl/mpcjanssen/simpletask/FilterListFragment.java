package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;


import java.util.ArrayList;

public class FilterListFragment extends Fragment {

    final static String TAG = FilterListFragment.class.getSimpleName();
    private ListView lv;
    private CheckBox cb;
    private GestureDetector gestureDetector;
    @Nullable
    ActionBar actionbar;
    String type;
    private ArrayList<String> selectedItems;
    private boolean not;
    private Logger log;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = Logger.INSTANCE;
        log.debug(TAG, "onCreate() this:" + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.debug(TAG, "onDestroy() this:" + this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        log.debug(TAG, "onSaveInstanceState() this:" + this);
        outState.putStringArrayList("selectedItems", getSelectedItems());
        outState.putBoolean("not", getNot());

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        log.debug(TAG, "onCreateView() this:" + this + " savedInstance:" + savedInstanceState);

        Bundle arguments = getArguments();
        ArrayList<String> items = arguments.getStringArrayList(FilterActivity.FILTER_ITEMS);
        actionbar = getActivity().getActionBar();

        if (savedInstanceState != null) {
            selectedItems = savedInstanceState.getStringArrayList("selectedItems");
            not = savedInstanceState.getBoolean("not");
        } else {
            selectedItems = arguments.getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS);
            not = arguments.getBoolean(FilterActivity.INITIAL_NOT);
        }

        log.debug(TAG, "Fragment bundle:" + this + " arguments:" + arguments);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.multi_filter,
                container, false);

        cb = (CheckBox) layout.findViewById(R.id.checkbox);

        lv = (ListView) layout.findViewById(R.id.listview);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        lv.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.simple_list_item_multiple_choice, items));

        for (int i = 0; i < items.size(); i++) {
            if (selectedItems!=null && selectedItems.contains(items.get(i))) {
                lv.setItemChecked(i, true);
            }
        }

        cb.setChecked(not);
        return layout;
    }

    public boolean getNot() {
        if (selectedItems == null) {
            // Tab was not displayed so no selections were changed
            return  getArguments().getBoolean(FilterActivity.INITIAL_NOT);
        } else {
            return cb.isChecked();
        }
    }

    public ArrayList<String> getSelectedItems() {

        ArrayList<String> arr = new ArrayList<>();
        if (selectedItems == null) {
            // Tab was not displayed so no selections were changed
            return getArguments().getStringArrayList(FilterActivity.INITIAL_SELECTED_ITEMS);
        }
        int size = lv.getCount();
        for (int i = 0; i < size; i++) {
            if (lv.isItemChecked(i)) {
                arr.add((String) lv.getAdapter().getItem(i));
            }
        }
        return arr;
    }
}
