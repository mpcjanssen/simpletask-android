package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.util.Util;

public class FilterActivity extends ThemedActivity {

    final static String TAG = FilterActivity.class.getSimpleName();
    final static String CONTEXT_TAB = "context";
    final static String PROJECT_TAB = "project";
    final static String PRIO_TAB = "prio";
    final static String OTHER_TAB = "other";
    final static String SORT_TAB = "sort";
    final static String SCRIPT_TAB = "script";

    // Constants for saving state
    public static final String FILTER_ITEMS = "items";
    public static final String INITIAL_SELECTED_ITEMS = "initialSelectedItems";
    public static final String INITIAL_NOT = "initialNot";

    boolean asWidgetConfigure = false;
    ActiveFilter mFilter;

    TodoApplication  m_app;
    SharedPreferences prefs;

    @Nullable
    private ActionBar actionbar;

    private int getLastActiveTab() {
        return prefs.getInt(getString(R.string.last_open_filter_tab), 0);
    }

    private void saveActiveTab(int i) {
        prefs.edit()
                .putInt(getString(R.string.last_open_filter_tab), i)
                .apply();
    }

    @Override
    protected void onDestroy() {
        saveActiveTab(actionbar.getSelectedNavigationIndex());
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (m_app.isBackSaving()) {
            applyFilter();
        }
        super.onBackPressed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {    	
    	Log.v(TAG, "Called with intent: " + getIntent().toString());
        m_app = (TodoApplication) getApplication();
        prefs = getPreferences(MODE_PRIVATE);
        m_app.setActionBarStyle(getWindow());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.filter);

        Bundle arguments;
        actionbar = getActionBar();
        if (actionbar!=null) {
            actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }
        Intent intent = getIntent();
        mFilter = new ActiveFilter();
        mFilter.initFromIntent(intent);
        if (intent.getAction()!=null) {
        	asWidgetConfigure = getIntent().getAction().equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        }
        // Fill arguments for fragment
        arguments = new Bundle();        
        arguments.putStringArrayList(FILTER_ITEMS, 
                Util.sortWithNone(m_app.getTaskCache().getContexts(), m_app.sortCaseSensitive(), false));
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, mFilter.getContexts());
        arguments.putBoolean(INITIAL_NOT, mFilter.getContextsNot());
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.context_prompt))
                .setTabListener(new MyTabsListener(this, CONTEXT_TAB, FilterListFragment.class, arguments))
                .setTag(CONTEXT_TAB));

        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putStringArrayList(FILTER_ITEMS, 
                Util.sortWithNone(m_app.getTaskCache().getProjects(), m_app.sortCaseSensitive(), false));
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, mFilter.getProjects());
        arguments.putBoolean(INITIAL_NOT, mFilter.getProjectsNot());
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.project_prompt))
                .setTabListener(new MyTabsListener(this, PROJECT_TAB, FilterListFragment.class, arguments))
                .setTag(PROJECT_TAB));

        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putStringArrayList(FILTER_ITEMS, Priority.inCode(m_app.getTaskCache().getPriorities()));
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, Priority.inCode(mFilter.getPriorities()));
        arguments.putBoolean(INITIAL_NOT, mFilter.getPrioritiesNot());
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.priority_short_prompt))
                .setTabListener(new MyTabsListener(this, PRIO_TAB, FilterListFragment.class, arguments))
                .setTag(PRIO_TAB));

        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, mFilter.getHideCompleted());
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, mFilter.getHideFuture());
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, mFilter.getHideLists());
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, mFilter.getHideTags());

        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.filter_show_prompt))
                .setTabListener(new MyTabsListener(this, OTHER_TAB, FilterOtherFragment.class, arguments))
                .setTag(OTHER_TAB));

        // Fill arguments for fragment
        arguments = new Bundle();
        Tab sortTab = actionbar.newTab()
                .setText(getString(R.string.sort))
                .setTabListener(new MyTabsListener(this, SORT_TAB, FilterSortFragment.class, arguments))
                .setTag(SORT_TAB);
        arguments.putStringArrayList(FILTER_ITEMS,mFilter.getSort(m_app.getDefaultSorts()));
        actionbar.addTab(sortTab);

        if (m_app.useRhino()) {
            arguments = new Bundle();
            Tab scriptTab = actionbar.newTab()
                    .setText(getString(R.string.script))
                    .setTabListener(new MyTabsListener(this, SCRIPT_TAB, FilterScriptFragment.class, arguments))
                    .setTag(SCRIPT_TAB);
            arguments.putString(ActiveFilter.INTENT_JAVASCRIPT_FILTER, mFilter.getJavascript());
            arguments.putString(ActiveFilter.INTENT_JAVASCRIPT_TEST_TASK_FILTER, mFilter.getJavascriptTestTask());
            actionbar.addTab(scriptTab);
        }
        int previousTab = getLastActiveTab();
        if (previousTab < actionbar.getTabCount()) {
            actionbar.setSelectedNavigationItem(previousTab);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        if (m_app.isDarkActionbar()) {
            inflater.inflate(R.menu.filter, menu);
        } else {
            inflater.inflate(R.menu.filter_light, menu);
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NotNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter_action:
            	if (asWidgetConfigure) {
            		askWidgetName();
            	} else {
            		applyFilter();
            	}
                break;
        }
        return true;
    }

    @NotNull
    private Intent createFilterIntent() {
        Intent target = new Intent(this, Simpletask.class);
        target.setAction(Constants.INTENT_START_FILTER);
        updateFilterFromFragments();
        mFilter.setName(mFilter.getProposedName());
        mFilter.saveInIntent(target);

        target.putExtra("name", mFilter.getProposedName());
        return target;
    }

    private void updateFilterFromFragments () {
        ArrayList<String> items;
        items = getFragmentFilter(CONTEXT_TAB);
        if (items!=null) {
            mFilter.setContexts(items);
        }
        mFilter.setContextsNot(getNot(CONTEXT_TAB,mFilter.getContextsNot()));

        items = getFragmentFilter(PROJECT_TAB);
        if (items!=null) {
            mFilter.setProjects(items);
        }
        mFilter.setProjectsNot(getNot(PROJECT_TAB,mFilter.getProjectsNot()));

        items = getFragmentFilter(PRIO_TAB);
        if (items!=null) {
            mFilter.setPriorities(items);
        }
        mFilter.setPrioritiesNot(getNot(PRIO_TAB,mFilter.getPrioritiesNot()));

        mFilter.setHideCompleted(getHideCompleted());
        mFilter.setHideFuture(getHideFuture());
        mFilter.setHideLists(getHideLists());
        mFilter.setHideTags(getHideTags());
        mFilter.setJavascript(getJavascript());
        mFilter.setJavascriptTestTask(getJavascriptTestTask());

        items = getSelectedSort();
        if (items!=null) {
            mFilter.setSort(items);
        }
    }

    @Nullable
    private ArrayList<String> getFragmentFilter(String tag) {
        FilterListFragment fr;
        fr = (FilterListFragment) this.getFragmentManager().findFragmentByTag(tag);
        if (fr == null) {
            // fragment was not initialized so no update
            return null;
        } else {
            return fr.getSelectedItems();
        }
    }

    @Nullable
    private ArrayList<String> getSelectedSort() {
        FilterSortFragment fr;
        fr = (FilterSortFragment) this.getFragmentManager().findFragmentByTag(SORT_TAB);
        if (fr == null) {
            // fragment was never intialized
            return null;
        } else {
            return fr.getSelectedItem();
        }
    }

    private boolean getHideCompleted() {
        FilterOtherFragment fr;
        fr = (FilterOtherFragment) this.getFragmentManager().findFragmentByTag(OTHER_TAB);
        if (fr == null) {
            // fragment was never intialized
            return mFilter.getHideCompleted();
        } else {
            return fr.getHideCompleted();
        }
    }

    private boolean getHideFuture() {
        FilterOtherFragment fr;
        fr = (FilterOtherFragment) this.getFragmentManager().findFragmentByTag(OTHER_TAB);
        if (fr == null) {
            // fragment was never intialized
            return mFilter.getHideFuture();
        } else {
            return fr.getHideFuture();
        }
    }

    private boolean getHideLists() {
        FilterOtherFragment fr;
        fr = (FilterOtherFragment) this.getFragmentManager().findFragmentByTag(OTHER_TAB);
        if (fr == null) {
            // fragment was never intialized
            return mFilter.getHideLists();
        } else {
            return fr.getHideLists();
        }
    }

    private boolean getHideTags() {
        FilterOtherFragment fr;
        fr = (FilterOtherFragment) this.getFragmentManager().findFragmentByTag(OTHER_TAB);
        if (fr == null) {
            // fragment was never intialized
            return mFilter.getHideTags();
        } else {
            return fr.getHideTags();
        }
    }

    private String getJavascript() {
        FilterScriptFragment fr;
        fr = (FilterScriptFragment) this.getFragmentManager().findFragmentByTag(SCRIPT_TAB);
        if (fr == null) {
            // fragment was never intialized
            return mFilter.getJavascript();
        } else {
            return fr.getJavascript();
        }
    }

    private String getJavascriptTestTask() {
        FilterScriptFragment fr;
        fr = (FilterScriptFragment) this.getFragmentManager().findFragmentByTag(SCRIPT_TAB);
        if (fr == null) {
            // fragment was never intialized
            return mFilter.getJavascriptTestTask();
        } else {
            return fr.getTestTask();
        }
    }

    private boolean getNot(String tag, boolean current) {
        FilterListFragment fr;
        fr = (FilterListFragment) this.getFragmentManager().findFragmentByTag(tag);
        if (fr == null) {
            // fragment was never intialized
            return current;
        } else {
            return fr.getNot();
        }
    }

    
    private void createWidget(String name) {
    	int mAppWidgetId;

    	Intent intent = getIntent();
    	Bundle extras = intent.getExtras();
        updateFilterFromFragments();
    	if (extras != null) {
    		mAppWidgetId = extras.getInt(
    				AppWidgetManager.EXTRA_APPWIDGET_ID, 
    				AppWidgetManager.INVALID_APPWIDGET_ID);

            Context context = TodoApplication.getAppContext();

    		// Store widget filter
    		SharedPreferences preferences = context.getSharedPreferences("" + mAppWidgetId, MODE_PRIVATE);
            mFilter.setName(name);
            mFilter.saveInPrefs(preferences);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            MyAppWidgetProvider.updateAppWidget(context, appWidgetManager,
                    mAppWidgetId, name);

    		Intent resultValue = new Intent(getApplicationContext(), AppWidgetService.class);
    		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
    		setResult(RESULT_OK, resultValue);
    		finish();
    	}
    }

	private void applyFilter() {
   		Intent data = createFilterIntent();
   		startActivity(data);
        finish();
    }

    private void askWidgetName() {
        String name;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Create widget");
        alert.setMessage("Widget title");
        updateFilterFromFragments();
        name = mFilter.getProposedName();

// Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);
        input.setText(name);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if (value.equals("")) {
                    Util.showToastShort(getApplicationContext(), R.string.widget_name_empty);
                } else {
                    createWidget(value);
                }
            }
        }
        );

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();

    }


    private class MyTabsListener implements ActionBar.TabListener {

        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Bundle mArguments;
        private Class mClz;

        public MyTabsListener(Activity activity, String tag, Class clz, Bundle arguments) {
            mActivity = activity;
            mTag = tag;
            mArguments = arguments;
            mClz = clz;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }

        @Override
        public void onTabSelected(Tab tab, @NotNull FragmentTransaction ft) {
            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment == null) {
                // If not, instantiate and add it to the activity
                Log.v(TAG,"Created new fragment: " + mClz.getName());
                mFragment = Fragment.instantiate(mActivity, mClz.getName(), mArguments);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.attach(mFragment);
            }
        }

        @Override
        public void onTabUnselected(Tab tab, @NotNull FragmentTransaction ft) {
            if (mFragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(mFragment);
            }
        }
    }
}


