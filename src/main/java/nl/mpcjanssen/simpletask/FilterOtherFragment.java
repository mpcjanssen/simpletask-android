package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;



public class FilterOtherFragment extends Fragment {

    final static String TAG = FilterOtherFragment.class.getSimpleName();
    private CheckBox cbHideCompleted;
    private CheckBox cbHideFuture;
    private CheckBox cbHideLists;
    private CheckBox cbHideTags;
    private CheckBox cbHideCreateDate;
    private CheckBox cbHideHidden;
    @Nullable
    ActionBar actionbar;
    private Logger log;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = Logger.INSTANCE;
        log.debug(TAG, "onCreate() this:" + this);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        log.debug(TAG, "onDestroy() this:" + this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        log.debug(TAG, "onSaveInstanceState() this:" + this);
        outState.putBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, getHideCompleted());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, getHideFuture());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, getHideLists());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, getHideTags());
        outState.putBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, getHideCreateDate());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        log.debug(TAG, "onCreateView() this:" + this + " savedInstance:" + savedInstanceState);

        Bundle arguments = getArguments();
        actionbar = getActivity().getActionBar();
        log.debug(TAG, "Fragment bundle:" + this + " arguments:" + arguments);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.other_filter,
                container, false);

        cbHideCompleted = (CheckBox) layout.findViewById(R.id.cb_show_completed);
        cbHideFuture = (CheckBox) layout.findViewById(R.id.cb_show_future);
        cbHideLists = (CheckBox) layout.findViewById(R.id.cb_show_lists);
        cbHideTags = (CheckBox) layout.findViewById(R.id.cb_show_tags);
        cbHideCreateDate = (CheckBox) layout.findViewById(R.id.cb_show_create_date);
        cbHideHidden = (CheckBox) layout.findViewById(R.id.cb_show_hidden);
        if (savedInstanceState != null) {
            cbHideCompleted.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false));
            cbHideFuture.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false));
            cbHideLists.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false));
            cbHideTags.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false));
            cbHideCreateDate.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false));
            cbHideHidden.setChecked(!savedInstanceState.getBoolean(ActiveFilter.INTENT_HIDE_HIDDEN_FILTER, true));
        } else {
            cbHideCompleted.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false));
            cbHideFuture.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false));
            cbHideLists.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false));
            cbHideTags.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false));
            cbHideCreateDate.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false));
            cbHideHidden.setChecked(!arguments.getBoolean(ActiveFilter.INTENT_HIDE_HIDDEN_FILTER, true));
        }

        return layout;
    }

    public boolean getHideCompleted() {
        Bundle arguments = getArguments();
        if (cbHideCompleted == null) {
            return arguments.getBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, false);
        } else {
            return !cbHideCompleted.isChecked();
        }
    }

    public boolean getHideFuture() {
        Bundle arguments = getArguments();
        if (cbHideCompleted == null) {
            return arguments.getBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, false);
        } else {
            return !cbHideFuture.isChecked();
        }
    }

    public boolean getHideHidden() {
        Bundle arguments = getArguments();
        if (cbHideHidden == null) {
            return arguments.getBoolean(ActiveFilter.INTENT_HIDE_HIDDEN_FILTER, true);
        } else {
            return !cbHideHidden.isChecked();
        }
    }

    public boolean getHideLists() {
        Bundle arguments = getArguments();
        if (cbHideCompleted == null) {
            return arguments.getBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, false);
        } else {
            return !cbHideLists.isChecked();
        }
    }
    public boolean getHideTags() {
        Bundle arguments = getArguments();
        if (cbHideCompleted == null) {
            return arguments.getBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, false);
        } else {
            return !cbHideTags.isChecked();
        }
    }
    public boolean getHideCreateDate() {
        Bundle arguments = getArguments();
        if (cbHideCreateDate == null) {
            return arguments.getBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, false);
        } else {
            return !cbHideCreateDate.isChecked();
        }
    }
}
