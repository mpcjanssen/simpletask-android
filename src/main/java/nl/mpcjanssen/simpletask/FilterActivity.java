package nl.mpcjanssen.simpletask;


import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.design.widget.TabLayout;

import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import nl.mpcjanssen.simpletask.remote.FileStore;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilterActivity extends ThemedActivity {

    final static String TAG = FilterActivity.class.getSimpleName();
    public static final String TAB_TYPE = "type";
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

    TodoApplication m_app;
    SharedPreferences prefs;

    @Nullable
    private ViewPager pager;
    private Menu m_menu;
    private Logger log;
    private ScreenSlidePagerAdapter pagerAdapter;
    private FilterScriptFragment scriptFragment;

    @Override
    public void onBackPressed() {
        if (m_app.isBackSaving()) {
            applyFilter();
        }
        super.onBackPressed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = LoggerFactory.getLogger(this.getClass());
        log.info("Called with intent: " + getIntent().toString());
        m_app = (TodoApplication) getApplication();
        prefs = TodoApplication.getPrefs();

        setContentView(R.layout.filter);

        Bundle arguments;

        Intent intent = getIntent();

        if (intent.getAction() != null) {
            asWidgetConfigure = getIntent().getAction().equals(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        }

        mFilter = new ActiveFilter();

        if (asWidgetConfigure) {
            mFilter.initFromPrefs(prefs);
        } else {
            mFilter.initFromIntent(intent);
        }


        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putStringArrayList(FILTER_ITEMS,
                Util.sortWithPrefix(m_app.getTodoList().getContexts(), m_app.sortCaseSensitive(), "-"));
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, mFilter.getContexts());
        arguments.putBoolean(INITIAL_NOT, mFilter.getContextsNot());
        arguments.putString(TAB_TYPE, CONTEXT_TAB);
        Fragment contextTab = new FilterListFragment();
        contextTab.setArguments(arguments);
        pagerAdapter.add(contextTab);


        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putStringArrayList(FILTER_ITEMS,
                Util.sortWithPrefix(m_app.getTodoList().getProjects(), m_app.sortCaseSensitive(), "-"));
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, mFilter.getProjects());
        arguments.putBoolean(INITIAL_NOT, mFilter.getProjectsNot());
        arguments.putString(TAB_TYPE, PROJECT_TAB);
        Fragment projectTab = new FilterListFragment();
        projectTab.setArguments(arguments);
        pagerAdapter.add(projectTab);

        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putStringArrayList(FILTER_ITEMS, Priority.inCode(m_app.getTodoList().getPriorities()));
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, Priority.inCode(mFilter.getPriorities()));
        arguments.putBoolean(INITIAL_NOT, mFilter.getPrioritiesNot());
        arguments.putString(TAB_TYPE, PRIO_TAB);
        Fragment prioTab = new FilterListFragment();
        prioTab.setArguments(arguments);
        pagerAdapter.add(prioTab);

        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, mFilter.getHideCompleted());
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, mFilter.getHideFuture());
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, mFilter.getHideLists());
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, mFilter.getHideTags());
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, mFilter.getHideCreateDate());
        arguments.putString(TAB_TYPE, OTHER_TAB);
        Fragment otherTab = new FilterOtherFragment();
        otherTab.setArguments(arguments);
        pagerAdapter.add(otherTab);


        // Fill arguments for fragment
        arguments = new Bundle();
        arguments.putStringArrayList(FILTER_ITEMS, mFilter.getSort(m_app.getDefaultSorts()));
        arguments.putString(TAB_TYPE, SORT_TAB);
        Fragment sortTab = new FilterSortFragment();
        sortTab.setArguments(arguments);
        pagerAdapter.add(sortTab);

        arguments = new Bundle();
        arguments.putString(ActiveFilter.INTENT_SCRIPT_FILTER, mFilter.getScript());
        arguments.putString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER, mFilter.getScriptTestTask());
        arguments.putString(TAB_TYPE, SCRIPT_TAB);
        FilterScriptFragment scriptTab = new FilterScriptFragment();
        scriptFragment = scriptTab;
        scriptTab.setArguments(arguments);
        pagerAdapter.add(scriptTab);


        pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(pagerAdapter);
        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(pager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filter, menu);
        m_menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter_action:
                if (asWidgetConfigure) {
                    askWidgetName();
                } else {
                    applyFilter();
                }
                break;
            case R.id.menu_filter_load_script:
                openScript(new FileStoreInterface.FileReadListener() {
                    @Override
                    public void fileRead(final String contents) {
                        Util.runOnMainThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        setScript(contents);
                                    }
                                }
                        );
                    }
                });
                break;
        }
        return true;
    }

    private void openScript(final FileStoreInterface.FileReadListener file_read) {
        Util.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                FileStore.FileDialog dialog = new FileStore.FileDialog(FilterActivity.this, new File(m_app.getTodoFileName()).getParent(), false);
                dialog.addFileListener(new FileStoreInterface.FileSelectedListener() {
                    @Override
                    public void fileSelected(final String file) {
                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {

                                    m_app.getFileStore().readFile(file, file_read);
                                } catch (IOException e) {
                                    Util.showToastShort(FilterActivity.this, "Failed to load script.");
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                });
                dialog.createFileDialog(FilterActivity.this, m_app.getFileStore());
            }
        });
    }

    @NonNull
    private Intent createFilterIntent() {
        Intent target = new Intent(this, Simpletask.class);
        target.setAction(Constants.INTENT_START_FILTER);
        updateFilterFromFragments();
        mFilter.setName(mFilter.getProposedName());
        mFilter.saveInIntent(target);

        target.putExtra("name", mFilter.getProposedName());
        return target;
    }

    private void updateFilterFromFragments() {
        ArrayList<String> items;
        for (Fragment f : pagerAdapter.getFragments()) {
            switch (f.getArguments().getString(TAB_TYPE,"")) {
                case "":
                    break;
                case OTHER_TAB:
                    FilterOtherFragment of = (FilterOtherFragment) f;
                    mFilter.setHideCompleted(of.getHideCompleted());
                    mFilter.setHideFuture(of.getHideFuture());
                    mFilter.setHideLists(of.getHideLists());
                    mFilter.setHideTags(of.getHideTags());
                    mFilter.setHideCreateDate(of.getHideCreateDate());
                    break;
                case CONTEXT_TAB:
                    FilterListFragment lf = (FilterListFragment) f;
                    mFilter.setContexts(lf.getSelectedItems());
                    mFilter.setContextsNot(lf.getNot());
                    break;
                case PROJECT_TAB:
                    FilterListFragment pf = (FilterListFragment) f;
                    mFilter.setProjects(pf.getSelectedItems());
                    mFilter.setProjectsNot(pf.getNot());
                    break;
                case PRIO_TAB:
                    FilterListFragment prf = (FilterListFragment) f;
                    mFilter.setPriorities(prf.getSelectedItems());
                    mFilter.setPrioritiesNot(prf.getNot());
                    break;
                case SORT_TAB:
                    FilterSortFragment sf = (FilterSortFragment) f;
                    mFilter.setSort(sf.getSelectedItem());
                    break;
                case SCRIPT_TAB:
                    FilterScriptFragment scrf = (FilterScriptFragment) f;
                    mFilter.setScript(scrf.getScript());
                    mFilter.setScriptTestTask(scrf.getTestTask());
                    break;

            }
        }
    }


    private void setScript(String script) {
        if (scriptFragment == null) {
            // fragment was never intialized
            Util.showToastShort(this, "Script tab not visible??");
        } else {
            scriptFragment.setScript(script);
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


    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private final ArrayList<Fragment> fragments;
        private FragmentManager fm;

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
            this.fm=fm;
            fragments = new ArrayList<>();
        }

        public void add(Fragment frag) {
            fragments.add(frag);
        }

        @Override
        public CharSequence	getPageTitle(int position) {
            Fragment f = fragments.get(position);
            String type = f.getArguments().getString(TAB_TYPE,"unknown");
            switch ( type) {
                case PROJECT_TAB:
                    return m_app.getTagTerm();
                case CONTEXT_TAB:
                    return m_app.getListTerm();
                default:
                    return type;
            }
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        public ArrayList<Fragment> getFragments() {
            return fragments;
        }
    }
}


