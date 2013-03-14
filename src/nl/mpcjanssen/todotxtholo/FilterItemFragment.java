package nl.mpcjanssen.todotxtholo;

import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.Arrays;

public class FilterItemFragment extends Fragment {
    private final static String TAG = TodoTxtTouch.class.getSimpleName();
    private final static String STATE_ITEMS = "items";
    private final static String STATE_SELECTED = "selectedItem";

    private String selectedItem;
    private int items;
    private ListView lv;
    private GestureDetector gestureDetector;
    private ActionBar actionbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        actionbar = getActivity().getActionBar();
        if (savedInstanceState != null) {
            items = savedInstanceState.getInt(STATE_ITEMS);
            selectedItem = savedInstanceState.getString(STATE_SELECTED);
        } else {
            selectedItem = arguments.getString(Constants.INITIAL_SELECTED_ITEMS);
            items = arguments.getInt(Constants.ITEMS);
        }

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.single_filter,
                container, false);

        lv = (ListView) layout.findViewById(R.id.listview);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        lv.setAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.simple_list_item_single_choice, getResources().getStringArray(items)));

        int index = Arrays.asList(getResources().getStringArray(R.array.sortValues)).indexOf(selectedItem);
        lv.setItemChecked(index, true);
        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ITEMS, items);
        outState.putString(STATE_SELECTED, selectedItem);
    }


    public String getSelectedItem() {
        if (lv != null) {
            return getResources().getStringArray(R.array.sortValues)[lv.getCheckedItemPosition()];
        } else {
            return selectedItem;
        }
    }

}
