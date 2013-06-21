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
import nl.mpcjanssen.simpletaskdonate.R;

import java.util.ArrayList;
import java.util.HashSet;

public class FilterActivity extends Activity {

    final static String TAG = FilterActivity.class.getSimpleName();
    boolean asWidgetConfigure = false;

    Menu menu;
    private ActionBar actionbar;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {    	
    	Log.v(TAG, "Called with intent: " + getIntent().toString());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter);
        Bundle arguments;
        actionbar = getActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        TaskBag taskBag = ((MainApplication)getApplication()).getTaskBag();
        if (getIntent().getAction()!=null) {
        	asWidgetConfigure = getIntent().getAction().equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        }
        // Fill arguments for fragment
        arguments = new Bundle();        
        if(asWidgetConfigure) { 
        	arguments.putStringArrayList(Constants.FILTER_ITEMS, taskBag.getContexts(true));
        } else {
        	arguments.putStringArrayList(Constants.FILTER_ITEMS, getIntent().getStringArrayListExtra(Constants.EXTRA_CONTEXTS));
        }
        arguments.putStringArrayList(Constants.INITIAL_SELECTED_ITEMS, getIntent().getStringArrayListExtra(Constants.EXTRA_CONTEXTS_SELECTED));
        arguments.putBoolean(Constants.INITIAL_NOT, getIntent().getBooleanExtra(Constants.EXTRA_CONTEXTS + "not", false));
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.context_prompt))
                .setTabListener(new MyTabsListener(this, Constants.EXTRA_CONTEXTS, FilterListFragment.class, arguments))
                .setTag(Constants.EXTRA_CONTEXTS));

        // Fill arguments for fragment
        arguments = new Bundle();
        if(asWidgetConfigure) { 
        	arguments.putStringArrayList(Constants.FILTER_ITEMS, taskBag.getProjects(true));
        } else {
        	arguments.putStringArrayList(Constants.FILTER_ITEMS, getIntent().getStringArrayListExtra(Constants.EXTRA_PROJECTS));
        }
        arguments.putStringArrayList(Constants.INITIAL_SELECTED_ITEMS, getIntent().getStringArrayListExtra(Constants.EXTRA_PROJECTS_SELECTED));
        arguments.putBoolean(Constants.INITIAL_NOT, getIntent().getBooleanExtra(Constants.EXTRA_PROJECTS + "not", false));
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.project_prompt))
                .setTabListener(new MyTabsListener(this, Constants.EXTRA_PROJECTS, FilterListFragment.class, arguments))
                .setTag(Constants.EXTRA_PROJECTS));

        // Fill arguments for fragment
        arguments = new Bundle();
        if(asWidgetConfigure) { 
        	arguments.putStringArrayList(Constants.FILTER_ITEMS, Priority.inCode(taskBag.getPriorities()));        	
        } else {
        	arguments.putStringArrayList(Constants.FILTER_ITEMS, getIntent().getStringArrayListExtra(Constants.EXTRA_PRIORITIES));
        }
        arguments.putStringArrayList(Constants.INITIAL_SELECTED_ITEMS, getIntent().getStringArrayListExtra(Constants.EXTRA_PRIORITIES_SELECTED));
        arguments.putBoolean(Constants.INITIAL_NOT, getIntent().getBooleanExtra(Constants.EXTRA_PRIORITIES + "not", false));
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.priority_short_prompt))
                .setTabListener(new MyTabsListener(this, Constants.EXTRA_PRIORITIES, FilterListFragment.class, arguments))
                .setTag(Constants.EXTRA_PRIORITIES));

        // Fill arguments for fragment
        arguments = new Bundle();
        if(asWidgetConfigure) {
            ArrayList<String> defaultSorts = new ArrayList<String>();
            for (String type : getResources().getStringArray(R.array.sortKeys)) {
                defaultSorts.add(Constants.NORMAL_SORT + Constants.SORT_SEPARATOR +  type);
            }
            arguments.putStringArrayList(Constants.INTENT_SORT_ORDER, defaultSorts);
        } else {
            arguments.putStringArrayList(Constants.INTENT_SORT_ORDER, getIntent().getStringArrayListExtra(Constants.EXTRA_SORTS_SELECTED));
        }
        actionbar.addTab(actionbar.newTab()
                .setText(getString(R.string.sort))
                .setTabListener(new MyTabsListener(this, getString(R.string.sort), FilterSortFragment.class, arguments))
                .setTag(getString(R.string.sort)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.filter, menu);
        if (asWidgetConfigure) {
        	menu.findItem(R.id.menu_add_filter_shortcut).setVisible(false);
        }
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_apply_filter:
            	if (asWidgetConfigure) {
            		askWidgetName();
            	} else {
            		applyFilter();
            	}
                break;
            case R.id.menu_select_all:
                selectAll();
                break;
            case R.id.menu_clear_all:
                clearAll();
                break;
            case R.id.menu_add_filter_shortcut:
                createFilterShortcut();
                break;
        }
        return true;
    }

    private void selectAll() {
        String tag = (String) actionbar.getSelectedTab().getTag();
        if (!tag.equals(getString(R.string.sort))) {
            FilterListFragment fr = (FilterListFragment) getFragmentManager().findFragmentByTag(tag);
            fr.selectAll();
        }
    }

    private void clearAll() {
        String tag = (String) actionbar.getSelectedTab().getTag();
        if (!tag.equals(getString(R.string.sort))) {
            FilterListFragment fr = (FilterListFragment) getFragmentManager().findFragmentByTag(tag);
            fr.clearAll();
        }
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
        String name = "";
        ArrayList<String> appliedFilters = new ArrayList<String>();
        ArrayList<String> contextFilter = getFilter(Constants.EXTRA_CONTEXTS);
        ArrayList<String> projectsFilter = getFilter(Constants.EXTRA_PROJECTS);
        ArrayList<String> prioritiesFilter = getFilter(Constants.EXTRA_PRIORITIES);
        appliedFilters.addAll(contextFilter);
        appliedFilters.addAll(prioritiesFilter);
        appliedFilters.addAll(projectsFilter);


        target.putExtra(Constants.INTENT_CONTEXTS_FILTER, Util.join(contextFilter, "\n"));
        target.putExtra(Constants.INTENT_CONTEXTS_FILTER_NOT, getNot(Constants.EXTRA_CONTEXTS));
        target.putExtra(Constants.INTENT_PROJECTS_FILTER, Util.join(projectsFilter, "\n"));
        target.putExtra(Constants.INTENT_PROJECTS_FILTER_NOT, getNot(Constants.EXTRA_PROJECTS));
        target.putExtra(Constants.INTENT_PRIORITIES_FILTER, Util.join(prioritiesFilter, "\n"));
        target.putExtra(Constants.INTENT_PRIORITIES_FILTER_NOT, getNot(Constants.EXTRA_PRIORITIES));
        target.putExtra(Constants.INTENT_SORT_ORDER, Util.join(getSelectedSort(), "\n"));

        if (appliedFilters.size() == 1) {
            name = appliedFilters.get(0);
        }
        target.putExtra("name", name);
        return target;
    }

    private ArrayList<String> getFilter(String tag) {
        FilterListFragment fr;
        fr = (FilterListFragment) this.getFragmentManager().findFragmentByTag(tag);
        ArrayList<String> filter;
        if (fr == null) {
            // fragment was never intialized
            filter = getIntent().getStringArrayListExtra(tag + "_SELECTED");
        } else {
            filter = fr.getSelectedItems();
        }
        if (filter == null) {
        	filter = new ArrayList<String>();
        }
        return filter;
    }

    private ArrayList<String> getSelectedSort() {
        FilterSortFragment fr;
        fr = (FilterSortFragment) this.getFragmentManager().findFragmentByTag(getString(R.string.sort));
        if (fr == null) {
            // fragment was never intialized
            return getIntent().getStringArrayListExtra(Constants.INTENT_SORT_ORDER);
        } else {
            return fr.getSelectedItem();
        }
    }

    private boolean getNot(String tag) {
        FilterListFragment fr;
        fr = (FilterListFragment) this.getFragmentManager().findFragmentByTag(tag);
        boolean not;
        if (fr == null) {
            // fragment was never intialized
            not = getIntent().getBooleanExtra(tag + "not", false);
        } else {
            not = fr.getNot();
        }
        return not;
    }

    
    private void createWidget(String name) {
    	int mAppWidgetId;

    	Intent intent = getIntent();
    	Bundle extras = intent.getExtras();
    	if (extras != null) {
    		mAppWidgetId = extras.getInt(
    				AppWidgetManager.EXTRA_APPWIDGET_ID, 
    				AppWidgetManager.INVALID_APPWIDGET_ID);

    		// Store widget filter
    		SharedPreferences preferences = getApplicationContext().getSharedPreferences("" + mAppWidgetId, MODE_PRIVATE);
    		Editor editor = preferences.edit();
            editor.putString(Constants.INTENT_TITLE, name);
    		editor.putStringSet(Constants.INTENT_CONTEXTS_FILTER, new HashSet<String>(getFilter(Constants.EXTRA_CONTEXTS)));
    		editor.putBoolean(Constants.INTENT_CONTEXTS_FILTER_NOT, getNot(Constants.EXTRA_CONTEXTS));
    		editor.putStringSet(Constants.INTENT_PROJECTS_FILTER, new HashSet<String>(getFilter(Constants.EXTRA_PROJECTS)));
    		editor.putBoolean(Constants.INTENT_PROJECTS_FILTER_NOT, getNot(Constants.EXTRA_PROJECTS));
    		editor.putStringSet(Constants.INTENT_PRIORITIES_FILTER, new HashSet<String>(getFilter(Constants.EXTRA_PRIORITIES)));
    		editor.putBoolean(Constants.INTENT_PRIORITIES_FILTER_NOT, getNot(Constants.EXTRA_PRIORITIES));
            editor.putString(Constants.INTENT_SORT_ORDER, Util.join(getSelectedSort(), "\n"));
    		editor.commit();

            // onUpdate is not called on adding, launch it manually
            Context context = FilterActivity.this;
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            MyAppWidgetProvider.updateAppWidget(context, appWidgetManager,
                    mAppWidgetId, name);

    		Intent resultValue = new Intent(getApplicationContext(), AppWidgetService.class);
    		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            resultValue.putExtra(Constants.INTENT_TITLE, name);
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

        ArrayList<String> appliedFilters = new ArrayList<String>();
        ArrayList<String> contextFilter = getFilter(Constants.EXTRA_CONTEXTS);
        ArrayList<String> projectsFilter = getFilter(Constants.EXTRA_PROJECTS);
        ArrayList<String> prioritiesFilter = getFilter(Constants.EXTRA_PRIORITIES);
        appliedFilters.addAll(contextFilter);
        appliedFilters.addAll(prioritiesFilter);
        appliedFilters.addAll(projectsFilter);
        if (appliedFilters.size() == 1) {
            name = appliedFilters.get(0);
        } else {
            name = "";
        }

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
        input.setText(target.getStringExtra("name"));

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


