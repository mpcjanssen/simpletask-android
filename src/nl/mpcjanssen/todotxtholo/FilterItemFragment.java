package nl.mpcjanssen.todotxtholo;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;
import java.util.Arrays;

public class FilterItemFragment extends Fragment {
    private final static String TAG = TodoTxtTouch.class.getSimpleName();
    private final static String STATE_ITEMS = "items";
    private final static String STATE_SELECTED = "selectedItem";

    private ArrayList<String> selectedItems;
    ArrayList<Spinner> selectedFilters = new ArrayList<Spinner>();;
    private int itemsId;
    private LinearLayout layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (savedInstanceState != null) {
            itemsId = savedInstanceState.getInt(STATE_ITEMS);
            selectedItems = savedInstanceState.getStringArrayList(STATE_SELECTED);
        } else {
            selectedItems = arguments.getStringArrayList(Constants.INITIAL_SELECTED_ITEMS);
            itemsId = arguments.getInt(Constants.ITEMS);
        }

        layout = (LinearLayout) inflater.inflate(R.layout.single_filter,
                container, false);
        String[] itemValues = getResources().getStringArray(R.array.sortValues);
        for (String item : selectedItems) {
            Spinner spin = new Spinner(this.getActivity(), Spinner.MODE_DROPDOWN);
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
                    android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.sort));
            int index = Arrays.asList(itemValues).indexOf(item);
            spin.setAdapter(dataAdapter);
            spin.setSelection(index);
            selectedFilters.add(spin);
            layout.addView(spin);
        }



        layout.findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // End current activity if it's search results
                Spinner spin = new Spinner(getActivity(), Spinner.MODE_DROPDOWN);
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.sort));
                spin.setAdapter(dataAdapter);
                selectedFilters.add(spin);
                layout.addView(spin);
                updateRemoveBtn();
            }
        });


        layout.findViewById(R.id.btnRemove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // End current activity if it's search results
                int last = selectedFilters.size()-1;
                Spinner lastSpin = selectedFilters.get(last);
                layout.removeView(lastSpin);
                selectedFilters.remove(lastSpin);
                updateRemoveBtn();
            }
        });
        updateRemoveBtn();
        return layout;
    }

    private void updateRemoveBtn() {
        if (selectedFilters.size()>1) {
            layout.findViewById(R.id.btnRemove).setEnabled(true);
        } else {
            layout.findViewById(R.id.btnRemove).setEnabled(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ITEMS, itemsId);
        outState.putStringArrayList(STATE_SELECTED, selectedItems);
    }


    public ArrayList<String> getSelectedItems() {
        ArrayList<String> arr = new ArrayList<String>();
        String[] itemValues = getResources().getStringArray(R.array.sortValues);
        if (selectedFilters.size()!=0) {
            for (Spinner spin : selectedFilters) {
                arr.add(itemValues[spin.getSelectedItemPosition()]);
            }
        } else {
            arr.addAll(selectedItems);
        }
        return arr;
    }
}
