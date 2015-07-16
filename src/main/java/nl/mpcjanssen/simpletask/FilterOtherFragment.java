package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.*;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FilterOtherFragment extends Fragment {

    final static String TAG = FilterOtherFragment.class.getSimpleName();
    private CheckBox cbHideCompleted;
    private CheckBox cbHideFuture;
    private CheckBox cbHideLists;
    private CheckBox cbHideTags;
    private GestureDetector gestureDetector;
    @Nullable
    ActionBar actionbar;
    private Logger log;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = LoggerFactory.getLogger(this.getClass());
        log.debug("onCreate() this:" + this);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        log.debug("onDestroy() this:" + this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        log.debug("onSaveInstanceState() this:" + this);
        outState.putBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, getHideCompleted());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, getHideFuture());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, getHideLists());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, getHideTags());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        log.debug("onCreateView() this:" + this + " savedInstance:" + savedInstanceState);

        Bundle arguments = getArguments();
        actionbar = getActivity().getActionBar();
        log.debug("Fragment bundle:" + this + " arguments:" + arguments);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.other_filter,
                container, false);

        cbHideCompleted = (CheckBox) layout.findViewById(R.id.cb_show_completed);
        cbHideFuture = (CheckBox) layout.findViewById(R.id.cb_show_future);
        cbHideLists = (CheckBox) layout.findViewById(R.id.cb_show_lists);
        cbHideTags = (CheckBox) layout.findViewById(R.id.cb_show_tags);
        if (savedInstanceState != null) {
            cbHideCompleted.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false));
            cbHideFuture.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false));
            cbHideLists.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false));
            cbHideTags.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false));
        } else {
            cbHideCompleted.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false));
            cbHideFuture.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false));
            cbHideLists.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false));
            cbHideTags.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false));
        }

        gestureDetector = new GestureDetector(TodoApplication.getAppContext(),
                new FilterGestureDetector());
        OnTouchListener gestureListener = new OnTouchListener() {
            @Override
            public boolean onTouch(@NonNull View v, @NonNull MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    v.onTouchEvent(cancelEvent);
                    return true;
                }
                return false;
            }
        };

        layout.setOnTouchListener(gestureListener);
        return layout;
    }

    public boolean getHideCompleted() {
        Bundle arguments = getArguments();
        if (cbHideCompleted == null) {
            return !arguments.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false);
        } else {
            return !cbHideCompleted.isChecked();
        }
    }

    public boolean getHideFuture() {
        Bundle arguments = getArguments();
        if (cbHideCompleted == null) {
            return !arguments.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false);
        } else {
            return !cbHideFuture.isChecked();
        }
    }

    public boolean getHideLists() {
        Bundle arguments = getArguments();
        if (cbHideCompleted == null) {
            return !arguments.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false);
        } else {
            return !cbHideLists.isChecked();
        }
    }
    public boolean getHideTags() {
        Bundle arguments = getArguments();
        if (cbHideCompleted == null) {
            return !arguments.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false);
        } else {
            return !cbHideTags.isChecked();
        }
    }

    class FilterGestureDetector extends SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX,
                               float velocityY) {

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            if (actionbar==null) {
                return false;
            }
            int index = actionbar.getSelectedNavigationIndex();
            // right to left swipe
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                log.debug("Fling left");
                if (index < actionbar.getTabCount() - 1)
                    index++;
                actionbar.setSelectedNavigationItem(index);
                return true;
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // left to right swipe
                log.debug("Fling right");
                if (index > 0)
                    index--;
                actionbar.setSelectedNavigationItem(index);
                return true;
            }
            return false;
        }
    }
}
