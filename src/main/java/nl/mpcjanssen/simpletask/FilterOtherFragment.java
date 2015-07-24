package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.*;
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
    private CheckBox cbHideCreateDate;
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
        outState.putBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, getHideTags());
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
        cbHideCreateDate = (CheckBox) layout.findViewById(R.id.cb_show_create_date);
        if (savedInstanceState != null) {
            cbHideCompleted.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false));
            cbHideFuture.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false));
            cbHideLists.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false));
            cbHideTags.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false));
            cbHideCreateDate.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false));
        } else {
            cbHideCompleted.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false));
            cbHideFuture.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false));
            cbHideLists.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false));
            cbHideTags.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false));
            cbHideCreateDate.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false));
        }

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
    public boolean getHideCreateDate() {
        Bundle arguments = getArguments();
        if (cbHideCreateDate == null) {
            return !arguments.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false);
        } else {
            return !cbHideCreateDate.isChecked();
        }
    }
}
