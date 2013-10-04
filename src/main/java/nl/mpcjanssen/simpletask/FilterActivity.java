package nl.mpcjanssen.simpletask;

import android.app.*;
import android.app.ActionBar.Tab;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.util.Util;

import java.util.ArrayList;
import java.util.HashSet;

public class FilterActivity extends Activity {

    final static String TAG = FilterActivity.class.getSimpleName();
    final static String CONTEXT_TAB = "context";
    final static String PROJECT_TAB = "project";
    final static String PRIO_TAB = "prio";
    final static String OTHER_TAB = "other";
    final static String SORT_TAB = "sort";

    // Constants for saving state
    public static final String FILTER_ITEMS = "items";
    public static final String INITIAL_SELECTED_ITEMS = "initialSelectedItems";
    public static final String INITIAL_NOT = "initialNot";

    boolean asWidgetConfigure = false;
    ActiveFilter mFilter;

    Menu menu;
    private ActionBar actionbar;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        TodoApplication app = (TodoApplication) getApplication();
        if (app.isBackSaving()) {
            applyFilter();
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {    	
    	Log.v(TAG, "Called with intent: " + getIntent().toString());
        TodoApplication  m_app = (TodoApplication) getApplication();
        m_app.setActionBarStyle(getWindow());
        super.onCreate(savedInstanceState);

        // Set the proper theme
        setTheme(m_app.getActiveTheme());
        setContentView(R.layout.filter);

        Bundle arguments;
        actionbar = getActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        Intent intent = getIntent();
        mFilter = new ActiveFilter(getResources());
        mFilter.initFromIntent(intent);
        TaskBag taskBag = ((TodoApplication)getApplication()).getTaskBag();
        if (intent.getAction()!=null) {
        	asWidgetConfigure = getIntent().getAction().equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        }
        // Fill arguments for fragment
        arguments = new Bundle();        
        arguments.putStringArrayList(FILTER_ITEMS, taskBag.getContexts(true));
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, mFilter.getContexts());
        arguments.putBoolean(INITIAL_NOT, mFilter.getContextsNot());
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.context_prompt))
                .setTabListener(new MyTabsListener(this, CONTEXT_TAB, FilterListFragment.class, arguments))
                .setTag(CONTEXT_TAB));

        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putStringArrayList(FILTER_ITEMS, taskBag.getProjects(true));
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, mFilter.getProjects());
        arguments.putBoolean(INITIAL_NOT, mFilter.getProjectsNot());
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.project_prompt))
                .setTabListener(new MyTabsListener(this, PROJECT_TAB, FilterListFragment.class, arguments))
                .setTag(PROJECT_TAB));

        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putStringArrayList(FILTER_ITEMS, Priority.inCode(taskBag.getPriorities()));
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
        arguments.putStringArrayList(FILTER_ITEMS,mFilter.getSort());
        actionbar.addTab(sortTab);
        
        if(intent.getBooleanExtra(Constants.INTENT_OPEN_SORT_TAB, false)) {
            actionbar.selectTab(sortTab);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.filter, menu);
        if (asWidgetConfigure) {
        	menu.findItem(R.id.menu_add_filter_shortcut).setVisible(false);		
        	menu.findItem(R.id.menu_filter_action).setTitle(R.string.create_widget);
        }
        this.menu = menu;
        if (actionbar.getSelectedNavigationIndex() == actionbar.getTabCount()-1) {
            menu.findItem(R.id.menu_default_sort).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter_action:
            	if (asWidgetConfigure) {
            		askWidgetName();
            	} else {
            		applyFilter();
            	}
                break;
            case R.id.menu_add_filter_shortcut:
                createFilterShortcut();
                break;
            case R.id.menu_default_sort:
            	defaultSort();
            	break;
        }
        return true;
    }

    private void defaultSort() {
		// Find the fragment
    	FilterSortFragment fr = (FilterSortFragment)this.getFragmentManager().findFragmentByTag(SORT_TAB);
		fr.defaultSort();
	}
    

	// Safe the active tab on configuration changes
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("active_tab", actionbar.getSelectedNavigationIndex());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        actionbar.setSelectedNavigationItem(savedInstanceState.getInt("active_tab"));
    }

    private Intent createFilterIntent() {
        Intent target = new Intent(Constants.INTENT_START_FILTER);
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

        items = getSelectedSort();
        if (items!=null) {
            mFilter.setSort(items);
        }
    }

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
    private boolean getNot(String tag, boolean current) {
        FilterListFragment fr;
        fr = (FilterListFragment) this.getFragmentManager().findFragmentByTag(tag);
        boolean not;
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
   		setResult(RESULT_OK,data);
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
                    Util.showToastShort(getApplicationContext(), R.string.shortcut_name_empty);
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


    private void createFilterShortcut() {
        final Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        Intent target = createFilterIntent();

        // Setup target intent for shortcut
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target);

        // Set shortcut icon
        Intent.ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(this, R.drawable.icon);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Create shortcut");
        alert.setMessage("Shortcut name");

// Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);
        input.setText(mFilter.getProposedName());

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if (value.equals("")) {
                    Util.showToastShort(getApplicationContext(), R.string.shortcut_name_empty);
                } else {
                    shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, value);
                    sendBroadcast(shortcut);
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

    private class MyTabsListener<T extends Fragment> implements ActionBar.TabListener {

        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Bundle mArguments;
        private Class<T> mClz;

        public MyTabsListener(Activity activity, String tag, Class<T> clz, Bundle arguments) {
            mActivity = activity;
            mTag = tag;
            mArguments = arguments;
            mClz = clz;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment == null) {
                // If not, instantiate and add it to the activity
                mFragment = Fragment.instantiate(mActivity, mClz.getName(), mArguments);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.attach(mFragment);
            }
            if (menu==null) {
            	return;            		
            }
            MenuItem mnuDefaultSort = menu.findItem(R.id.menu_default_sort);
            if (tab.getTag().equals(SORT_TAB)) {
            	mnuDefaultSort.setVisible(true);
            } else {
            	mnuDefaultSort.setVisible(false);
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(mFragment);
            }
        }
    }
}


