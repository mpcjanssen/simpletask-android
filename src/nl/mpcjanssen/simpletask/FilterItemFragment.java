package nl.mpcjanssen.simpletask;

import nl.mpcjanssen.simpletask.R;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class FilterItemFragment extends Fragment {
    private final static String TAG = Simpletask.class.getSimpleName();
    private final static String STATE_ITEMS = "items";
    private final static String STATE_SELECTED = "selectedItem";

    private int selectedItem;
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
            selectedItem = savedInstanceState.getInt(STATE_SELECTED);
        } else {
            selectedItem = arguments.getInt(Constants.ACTIVE_SORT);
            items = arguments.getInt(Constants.FILTER_ITEMS);
        }

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.single_filter,
                container, false);

        lv = (ListView) layout.findViewById(R.id.listview);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        lv.setAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.simple_list_item_single_choice, getResources().getStringArray(items)));

        lv.setItemChecked(selectedItem, true);

        gestureDetector = new GestureDetector(MainApplication.appContext,
                new FilterGestureDetector());
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    v.onTouchEvent(cancelEvent);
                    return true;
                }
                return false;
            }
        };

        lv.setOnTouchListener(gestureListener);
        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ITEMS, items);
        outState.putInt(STATE_SELECTED, selectedItem);
    }


    public int getSelectedItem() {
        if (lv != null) {
            return lv.getCheckedItemPosition();
        } else {
            return selectedItem;
        }
    }

    class FilterGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;

            int index = actionbar.getSelectedNavigationIndex();
            // right to left swipe
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                Log.v(TAG, "Fling left");
                if (index < actionbar.getTabCount() - 1)
                    index++;
                actionbar.setSelectedNavigationItem(index);
                return true;
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // left to right swipe
                Log.v(TAG, "Fling right");
                if (index > 0)
                    index--;
                actionbar.setSelectedNavigationItem(index);
                return true;
            }
            return false;
        }
    }
}
