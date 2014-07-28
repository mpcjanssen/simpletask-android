package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilterOtherFragment extends Fragment {

    final static String TAG = FilterOtherFragment.class.getSimpleName();
    private CheckBox cbHideCompleted;
    private CheckBox cbHideFuture;
    private CheckBox cbHideLists;
    private CheckBox cbHideTags;
    private EditText txtJavaScript;
    private GestureDetector gestureDetector;
    @Nullable
    ActionBar actionbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate() this:" + this);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.v(TAG, "onDestroy() this:" + this);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(TAG, "onSaveInstanceState() this:" + this);
        outState.putBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, getHideCompleted());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, getHideFuture());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, getHideLists());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, getHideTags());
        outState.putString(ActiveFilter.INTENT_JAVASCRIPT_FILTER,getJavascript());
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView() this:" + this + " savedInstance:" + savedInstanceState);

        Bundle arguments = getArguments();
        TodoApplication app  = (TodoApplication) getActivity().getApplication();
        actionbar = getActivity().getActionBar();
        Log.v(TAG, "Fragment bundle:" + this + " arguments:" + arguments);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.other_filter,
                container, false);

        cbHideCompleted = (CheckBox) layout.findViewById(R.id.cb_show_completed);
        cbHideFuture = (CheckBox) layout.findViewById(R.id.cb_show_future);
        cbHideLists = (CheckBox) layout.findViewById(R.id.cb_show_lists);
        cbHideTags = (CheckBox) layout.findViewById(R.id.cb_show_tags);
        txtJavaScript = (EditText) layout.findViewById(R.id.txt_javascript);
        if (app.useRhino()) {
            txtJavaScript.setVisibility(View.VISIBLE);
        }

        if (savedInstanceState != null) {
            cbHideCompleted.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false));
            cbHideFuture.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false));
            cbHideLists.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false));
            cbHideTags.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false));
            txtJavaScript.setText(savedInstanceState.getString(ActiveFilter.INTENT_JAVASCRIPT_FILTER,""));
        } else {
            cbHideCompleted.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false));
            cbHideFuture.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false));
            cbHideLists.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false));
            cbHideTags.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false));
            txtJavaScript.setText(arguments.getString(ActiveFilter.INTENT_JAVASCRIPT_FILTER,""));
        }

        gestureDetector = new GestureDetector(TodoApplication.getAppContext(),
                new FilterGestureDetector());
        OnTouchListener gestureListener = new OnTouchListener() {
            @Override
            public boolean onTouch(@NotNull View v, @NotNull MotionEvent event) {
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

    public String getJavascript() {
        Bundle arguments = getArguments();
        if (txtJavaScript == null) {
            return arguments.getString(ActiveFilter.INTENT_JAVASCRIPT_FILTER, "");
        } else {
            return txtJavaScript.getText().toString();
        }
    }

    class FilterGestureDetector extends SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(@NotNull MotionEvent e1, @NotNull MotionEvent e2, float velocityX,
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
