/**
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @author Mark Janssen
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
 */

package nl.mpcjanssen.simpletask;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.SpannableString;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.adapters.DrawerAdapter;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;
import nl.mpcjanssen.simpletask.task.token.Token;
import nl.mpcjanssen.simpletask.util.DateStrings;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;


public class Simpletask extends ThemedListActivity implements
        AdapterView.OnItemLongClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    final static String TAG = Simpletask.class.getSimpleName();

    private final static int REQUEST_PREFERENCES = 2;

    Menu options_menu;
    TodoApplication m_app;
    ActiveFilter mFilter;
    TaskAdapter m_adapter;
    private BroadcastReceiver m_broadcastReceiver;
    private LocalBroadcastManager localBroadcastManager;
    @Nullable
    private ActionMode actionMode;
    // Drawer vars
    private ListView m_leftDrawerList;
    private ListView m_rightDrawerList;
    private DrawerLayout m_drawerLayout;
    private ActionBarDrawerToggle m_drawerToggle;
    private Bundle m_savedInstanceState;
    private ProgressDialog m_sync_dialog;

    private void showHelp() {
        Intent i = new Intent(this, HelpScreen.class);
        startActivity(i);
    }

    @Override
    public boolean onSearchRequested() {
        MenuItem searchMenuItem = options_menu.findItem(R.id.search);
        searchMenuItem.expandActionView();

        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (m_drawerToggle != null) {
            m_drawerToggle.syncState();
        }
    }

    @NotNull
    private List<Task> getCheckedTasks() {
        ArrayList<Task> checkedTasks = new ArrayList<Task>();
        SparseBooleanArray checkedItems = getListView()
                .getCheckedItemPositions();
        if (checkedItems==null) {
            return checkedTasks;
        }
        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.valueAt(i)) {
                Task t = getTaskAt(checkedItems.keyAt(i));
                // Ignore headers
                if (t!=null) {
                    checkedTasks.add(getTaskAt(checkedItems.keyAt(i)));
                }
            }
        }
        return checkedTasks;
    }

    @NotNull
    private String selectedTasksAsString() {
        List<String> result = new ArrayList<String>();
        for (Task t : getCheckedTasks()) {
            result.add(t.inFileFormat());
        }
        return Util.join(result, "\n");
    }

    private void selectAllTasks() {
        ListView lv = getListView();
        int itemCount = lv.getCount();
        for(int i=0 ; i < itemCount ; i++){
            // Only check tasks that are not checked yet
            // and skip headers
            // This prevents double counting in the CAB title
            Task t = getTaskAt(i);
            if (t != null && !lv.isItemChecked(i)) {
                lv.setItemChecked(i, true);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (m_drawerToggle != null) {
            m_drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (m_drawerToggle != null && m_drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        m_app = (TodoApplication) getApplication();
        m_app.setActionBarStyle(getWindow());
        m_savedInstanceState = savedInstanceState;

        super.onCreate(savedInstanceState);

        m_app.prefsChangeListener(this);

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_ACTION_ARCHIVE);
        intentFilter.addAction(Constants.BROADCAST_ACTION_LOGOUT);
        intentFilter.addAction(Constants.BROADCAST_UPDATE_UI);
        intentFilter.addAction(Constants.BROADCAST_SYNC_START);
        intentFilter.addAction(Constants.BROADCAST_SYNC_DONE);

        localBroadcastManager = m_app.getLocalBroadCastManager();

        m_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NotNull Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_ACTION_ARCHIVE)) {
                    // archive
                    // refresh screen to remove completed tasks
                    // push to remote
                    archiveTasks(null);
                } else if (intent.getAction().equals(Constants.BROADCAST_ACTION_LOGOUT)) {
                    Log.v(TAG, "Logging out from Dropbox");
                    m_app.deauthenticate();
                    Intent i = new Intent(context, LoginScreen.class);
                    startActivity(i);
                    finish();
                } else if (intent.getAction().equals(Constants.BROADCAST_UPDATE_UI)) {
                    int position = getListView().getFirstVisiblePosition();
                    Log.v(TAG, "Updating UI because of broadcast");
                    handleIntent();
                    getListView().setSelectionFromTop(position,0);
                } else if (intent.getAction().equals(Constants.BROADCAST_SYNC_START)) {
                    setProgressBarIndeterminateVisibility(true);
                } else if (intent.getAction().equals(Constants.BROADCAST_SYNC_DONE)) {
                    setProgressBarIndeterminateVisibility(false);
                }
            }
        };
        localBroadcastManager.registerReceiver(m_broadcastReceiver, intentFilter);


        // Set the proper theme
        setTheme(m_app.getActiveTheme());
        if (m_app.hasLandscapeDrawers()) {
            setContentView(R.layout.main_landscape);
        } else {
            setContentView(R.layout.main);
        }

        // Replace drawables if the theme is dark
        if (m_app.isDarkTheme()) {
            ImageView actionBarClear = (ImageView) findViewById(R.id.actionbar_clear);
            if (actionBarClear != null) {
                actionBarClear.setImageResource(R.drawable.cancel);
            }
        }
        setProgressBarIndeterminateVisibility(false);
    }

    private void handleIntent() {
        if (!m_app.isAuthenticated()) {
            Log.v(TAG, "handleIntent: not authenticated");
            startLogin();
            return;
        }
        if (!m_app.initialSyncDone()) {
            m_sync_dialog = new ProgressDialog(this,m_app.getActiveTheme());
            m_sync_dialog.setIndeterminate(true);
            m_sync_dialog.setMessage("Initial Dropbox sync in progress, please wait....");
            m_sync_dialog.setCancelable(false);
            m_sync_dialog.show();
        } else if (m_sync_dialog!=null) {
            m_sync_dialog.cancel();
        }

        mFilter = new ActiveFilter();

        m_leftDrawerList = (ListView) findViewById(R.id.left_drawer);
        m_rightDrawerList = (ListView) findViewById(R.id.right_drawer_list);

        m_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Set the list's click listener
        m_leftDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (m_drawerLayout != null) {
            m_drawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                    m_drawerLayout, /* DrawerLayout object */
                    R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
                    R.string.changelist, /* "open drawer" description */
                    R.string.app_label /* "close drawer" description */
            ) {

                /**
                 * Called when a drawer has settled in a completely closed
                 * state.
                 */
                public void onDrawerClosed(View view) {
                    // setTitle(R.string.app_label);
                }

                /** Called when a drawer has settled in a completely open state. */
                public void onDrawerOpened(View drawerView) {
                    // setTitle(R.string.changelist);
                }
            };

            // Set the drawer toggle as the DrawerListener
            m_drawerLayout.setDrawerListener(m_drawerToggle);
            ActionBar actionBar = getActionBar();
            if (actionBar!=null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeButtonEnabled(true);
            }
            m_drawerToggle.syncState();
        }

        // Show search or filter results
        Intent intent = getIntent();
        if (Constants.INTENT_START_FILTER.equals(intent.getAction())) {
            mFilter.initFromIntent(intent);
            Log.v(TAG, "handleIntent: launched with filter" + mFilter);
            Log.v(TAG, "handleIntent: saving filter in prefs");
            mFilter.saveInPrefs(TodoApplication.getPrefs());
        } else {
            // Set previous filters and sort
            Log.v(TAG, "handleIntent: from m_prefs state");
            mFilter.initFromPrefs(TodoApplication.getPrefs());
        }

        // Initialize Adapter
        if (m_adapter == null) {
            m_adapter = new TaskAdapter(getLayoutInflater());
        }
        m_adapter.setFilteredTasks();

        setListAdapter(this.m_adapter);

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setMultiChoiceModeListener(new ActionBarListener());
        // If we were started with a selected task,
        // select it now and clear it from the intent
        String selectedTask = intent.getStringExtra(Constants.INTENT_SELECTED_TASK);
        if (!Strings.isEmptyOrNull(selectedTask)) {
            String[] parts = selectedTask.split(":", 2);
            setSelectedTask(Integer.valueOf(parts[0]), parts[1]);
            intent.removeExtra(Constants.INTENT_SELECTED_TASK);
            setIntent(intent);
        } else {
            // Set the adapter for the list view
            updateDrawers();
        }
        if (m_savedInstanceState != null) {
            ArrayList<String> selection = m_savedInstanceState.getStringArrayList("selection");
            int position = m_savedInstanceState.getInt("position");
            if (selection != null) {
                for (String selected : selection) {
                    String[] parts = selected.split(":", 2);
                    setSelectedTask(Integer.valueOf(parts[0]), parts[1]);
                }
            }
            lv.setSelectionFromTop(position,0);
        }
    }

    private void setSelectedTask(int index, @NotNull String selectedTask) {
        Log.v(TAG, "Selected task: " + selectedTask);
        Task task = new Task(index, selectedTask);
        int position = m_adapter.getPosition(task);
        if (position != -1) {
            ListView lv = getListView();
            lv.setItemChecked(position, true);
            lv.setSelection(position);
        }

    }

    private void updateFilterBar() {
        ListView lv = getListView();
        int index = lv.getFirstVisiblePosition();
        View v = lv.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();
        lv.setSelectionFromTop(index, top);

        final LinearLayout actionbar = (LinearLayout) findViewById(R.id.actionbar);
        final TextView filterText = (TextView) findViewById(R.id.filter_text);
        if (mFilter.hasFilter()) {
            actionbar.setVisibility(View.VISIBLE);
        } else {
            actionbar.setVisibility(View.GONE);
        }
        int count = m_adapter!=null ? m_adapter.getCountVisbleTasks() : 0;
        int total = getTaskBag()!=null ? getTaskBag().size() : 0;

        filterText.setText(mFilter.getTitle(
                count,
                total,
                getText(R.string.priority_prompt),
                getText(R.string.project_prompt),
                getText(R.string.context_prompt),
                getText(R.string.search),
                getText(R.string.script),
                getText(R.string.title_filter_applied),
                getText(R.string.no_filter)
        ));
    }

    private void startLogin() {
        Intent intent = new Intent(this, LoginScreen.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(m_broadcastReceiver);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // just update all
        if ("theme".equals(key) || "fontsize".equals(key)) {
            this.recreate();
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> selection = new ArrayList<String>();
        for (Task t : getCheckedTasks()) {
            selection.add("" + t.getId() + ":" + t.inFileFormat());
        }
        outState.putStringArrayList("selection", selection);
        outState.putInt("position", getListView().getFirstVisiblePosition());
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finishActionmode();
    }

    @Override
    public boolean onCreateOptionsMenu(@NotNull final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (m_app.isDarkActionbar()) {
            inflater.inflate(R.menu.main, menu);
        } else {
            inflater.inflate(R.menu.main_light, menu);
        }

        if (!m_app.fileStoreCanSync()) {
            MenuItem mItem = menu.findItem(R.id.sync);
            mItem.setVisible(false);
        }
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));

        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean m_ignoreSearchChangeCallback;

            @Override
            public boolean onQueryTextSubmit(String query) {
                // Stupid searchview code will call onQueryTextChange callback
                // When the actionView collapse and the textview is reset
                // ugly global hack around this
                m_ignoreSearchChangeCallback = true;
                menu.findItem(R.id.search).collapseActionView();
                m_ignoreSearchChangeCallback = false;
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!m_ignoreSearchChangeCallback) {
                    if (mFilter == null) {
                        mFilter = new ActiveFilter();
                    }
                    mFilter.setSearch(newText);
                    mFilter.saveInPrefs(TodoApplication.getPrefs());
                    if (m_adapter!=null) {
                        m_adapter.setFilteredTasks();
                    }
                }
                return true;
            }
        });
        this.options_menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        onItemLongClick(l, v, position, id);
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ListView l = (ListView) parent;
        l.setItemChecked(position, !l.isItemChecked(position));
        return true;
    }

    @Nullable
    private Task getTaskAt(final int pos) {
        return m_adapter.getItem(pos);
    }

    private void shareTodoList() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < m_adapter.getCount(); i++) {
            Task task = m_adapter.getItem(i);
            if (task != null) {
                text.append(task.inFileFormat() + "\n");
            }
        }
        shareText(text.toString());
    }

    private void shareText(String text) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Simpletask list");

        // If text is small enough SEND it directly
        if (text.length()<50000) {
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,text);
        } else {

            // Create a cache file to pass in EXTRA_STREAM
            try {
                Util.createCachedFile(this,
                            Constants.SHARE_FILE_NAME, text);
                Uri fileUri  = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/"
                        + Constants.SHARE_FILE_NAME);
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri );
            } catch (Exception e) {
                Log.w(TAG, "Failed to create file for sharing");
            }
        }
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    private void prioritizeTasks(@NotNull final List<Task> tasks) {
        List<String> strings = Priority.rangeInCode(Priority.NONE, Priority.Z);
        final String[] prioArr = strings.toArray(new String[strings.size()]);

        int prioIdx = 0;
        if (tasks.size()==1) {
            prioIdx = strings.indexOf(tasks.get(0).getPriority().getCode());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_priority);
        builder.setSingleChoiceItems(prioArr, prioIdx, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NotNull DialogInterface dialog, final int which) {
                dialog.dismiss();
                Priority prio = Priority.toPriority(prioArr[which]);
                ArrayList<String> originalTasks = Util.tasksToString(tasks);
                for (Task t : tasks) {
                    t.setPriority(prio);
                }
                getTaskBag().modify(originalTasks,tasks,null,null);
                finishActionmode();
            }
        });
        builder.show();

    }

    private void completeTasks(@NotNull List<Task> tasks) {
        getTaskBag().complete(tasks, m_app.hasRecurOriginalDates(), m_app.hasKeepPrio());
        if (m_app.isAutoArchive()) {
            archiveTasks(null);
        }
    }

    private void undoCompleteTasks(@NotNull List<Task> tasks) {
        getTaskBag().undoComplete(tasks);
    }

    private void deferTasks(List<Task> tasks, final int dateType) {
        final List<Task> tasksToDefer = tasks;
        Dialog d = Util.createDeferDialog(this, dateType, true, new Util.InputDialogListener() {
            @Override
            public void onClick(@Nullable String selected) {
                if (selected==null) {
                    Log.w(TAG, "Can't defer, selected is null. This should not happen");
                    return;
                }
                if (selected.equals("pick")) {
                    final DateTime today = DateTime.today(TimeZone.getDefault());
                    DatePickerDialog dialog = new DatePickerDialog(Simpletask.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                            month++;

                            DateTime date = DateTime.forDateOnly(year, month, day);
                            getTaskBag().defer(date.format(Constants.DATE_FORMAT), tasksToDefer, dateType);

                        }
                    },
                            today.getYear(),
                            today.getMonth()-1,
                            today.getDay()
                    );
                    boolean showCalendar = m_app.showCalendar();

                    dialog.getDatePicker().setCalendarViewShown(showCalendar);
                    dialog.getDatePicker().setSpinnersShown(!showCalendar);
                    dialog.show();
                } else {
                    getTaskBag().defer(selected, tasksToDefer, dateType);
                }
            }
        });
        d.show();
    }

    private void deleteTasks(final List<Task> tasks) {
        m_app.showConfirmationDialog(this, R.string.delete_task_message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                m_app.getTaskCache().modify(null,null,null,tasks);
                // We have change the data, views should refresh
            }
        }, R.string.delete_task_title);
    }

    private void archiveTasks(final List<Task> tasksToArchive) {
        getTaskBag().archive(m_app.getDoneFileName(), tasksToArchive);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NotNull MenuItem item) {
        Log.v(TAG, "onMenuItemSelected: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.add_new:
                startAddTaskActivity(null);
                break;
            case R.id.search:
                break;
            case R.id.preferences:
                startPreferencesActivity();
                break;
            case R.id.filter:
                startFilterActivity();
                break;
            case R.id.share:
                shareTodoList();
                break;
            case R.id.help:
                showHelp();
                break;
            case R.id.sync:
                m_app.sync();
                break;
            case R.id.archive:
                m_app.showConfirmationDialog(this, R.string.delete_task_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        archiveTasks(null);
                    }
                }, R.string.archive_task_title);
                break;
            case R.id.open_file:
                m_app.browseForNewFile(this);
                break;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
        return true;
    }

    private void startAddTaskActivity(List<Task> tasks) {
        Log.v(TAG, "Starting addTask activity");
        getTaskBag().setTasksToUpdate(tasks);
        Intent intent = new Intent(this, AddTask.class);
        mFilter.saveInIntent(intent);
        startActivity(intent);
    }

    private void startPreferencesActivity() {
        Intent settingsActivity = new Intent(getBaseContext(),
                Preferences.class);
        startActivityForResult(settingsActivity, REQUEST_PREFERENCES);
    }

    /**
     * Handle clear filter click *
     */
    public void onClearClick(View v) {
        // Collapse the actionview if we are searching
        Intent intent = getIntent();
        clearFilter();
        m_adapter.setFilteredTasks();
    }

    @NotNull
    public ArrayList<ActiveFilter> getSavedFilter() {
        ArrayList<ActiveFilter> saved_filters = new ArrayList<ActiveFilter>();
        SharedPreferences saved_filter_ids = getSharedPreferences("filters", MODE_PRIVATE);
        Set<String> filterIds = saved_filter_ids.getStringSet("ids", new HashSet<String>());
        for (String id : filterIds) {
            SharedPreferences filter_pref = getSharedPreferences(id, MODE_PRIVATE);
            ActiveFilter filter = new ActiveFilter();
            filter.initFromPrefs(filter_pref);
            filter.setPrefName(id);
            saved_filters.add(filter);
        }
        return saved_filters;
    }

    /**
     * Handle add filter click *
     */
    public void onAddFilterClick(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.save_filter);
        alert.setMessage(R.string.save_filter_message);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);
        input.setText(mFilter.getProposedName());

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable text = input.getText();
                        String value;
                        if (text == null) {
                            value = "";
                        } else {
                            value = text.toString();
                        }
                        if (value.equals("")) {
                            Util.showToastShort(getApplicationContext(), R.string.filter_name_empty);
                        } else {
                            SharedPreferences saved_filters = getSharedPreferences("filters", MODE_PRIVATE);
                            int newId = saved_filters.getInt("max_id", 1) + 1;
                            Set<String> filters = saved_filters.getStringSet("ids", new HashSet<String>());
                            filters.add("filter_" + newId);
                            saved_filters.edit().putStringSet("ids", filters).putInt("max_id", newId).apply();
                            SharedPreferences test_filter_prefs = getSharedPreferences("filter_" + newId, MODE_PRIVATE);
                            mFilter.setName(value);
                            mFilter.saveInPrefs(test_filter_prefs);
                            updateRightDrawer();
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

    @Override
    public void onBackPressed() {
        if (m_drawerLayout!=null) {
            if (m_drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                m_drawerLayout.closeDrawer(Gravity.LEFT);
                return;
            }
            if (m_drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                m_drawerLayout.closeDrawer(Gravity.RIGHT);
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(@NotNull Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Intent currentIntent = getIntent();
            currentIntent.putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY));
            setIntent(currentIntent);
            options_menu.findItem(R.id.search).collapseActionView();
        } else if (intent.getExtras() != null) {
            // Only change intent if it actually contains a filter
            setIntent(intent);
        }
        Log.v(TAG, "onNewIntent: " + intent);

    }

    void clearFilter() {
        // Also clear the intent so we wont get the old filter after
        // switching back to app later fixes [1c5271ee2e]
        Intent intent = new Intent();
        mFilter.clear();
        mFilter.saveInIntent(intent);
        mFilter.saveInPrefs(TodoApplication.getPrefs());
        setIntent(intent);
        finishActionmode();
        updateDrawers();
    }

    private void updateDrawers() {
        updateLeftDrawer();
        updateRightDrawer();
    }

    private void updateRightDrawer() {
        ArrayList<String> names = new ArrayList<String>();
        final ArrayList<ActiveFilter> filters = getSavedFilter();
        Collections.sort(filters, new Comparator<ActiveFilter>() {
            public int compare(@NotNull ActiveFilter f1, @NotNull ActiveFilter f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        for (ActiveFilter f : filters) {
            names.add(f.getName());
        }
        m_rightDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, names));
        m_rightDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        m_leftDrawerList.setLongClickable(true);
        m_rightDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mFilter = filters.get(position);
                Intent intent = getIntent();
                mFilter.saveInIntent(intent);
                setIntent(intent);
                mFilter.saveInPrefs(TodoApplication.getPrefs());
                m_adapter.setFilteredTasks();
                if (m_drawerLayout != null) {
                    m_drawerLayout.closeDrawer(Gravity.RIGHT);
                }
                finishActionmode();
                updateDrawers();
            }
        });
        m_rightDrawerList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final ActiveFilter filter = filters.get(position);
                final String prefsName = filter.getPrefName();
                PopupMenu popupMenu = new PopupMenu(Simpletask.this, view);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(@NotNull MenuItem item) {
                        int menuid = item.getItemId();
                        switch (menuid) {
                            case R.id.menu_saved_filter_delete:
                                deleteSavedFilter(prefsName);
                                break;
                            case R.id.menu_saved_filter_shortcut:
                                createFilterShortcut(filter);
                                break;
                            case R.id.menu_saved_filter_rename:
                                renameSavedFilter(prefsName);
                                break;
                            case R.id.menu_saved_filter_update:
                                updateSavedFilter(prefsName);
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.saved_filter, popupMenu.getMenu());
                popupMenu.show();
                return true;
            }
        });
    }

    public void createFilterShortcut(@NotNull ActiveFilter filter) {
        final Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        Intent target = new Intent(Constants.INTENT_START_FILTER);
        filter.saveInIntent(target);

        target.putExtra("name", filter.getName());

        // Setup target intent for shortcut
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target);

        // Set shortcut icon
        Intent.ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, filter.getName());
        sendBroadcast(shortcut);
    }

    private void deleteSavedFilter(String prefsName) {
        SharedPreferences saved_filters = getSharedPreferences("filters", MODE_PRIVATE);
        HashSet<String> ids = new HashSet<String>();
        ids.addAll(saved_filters.getStringSet("ids", new HashSet<String>()));
        ids.remove(prefsName);
        saved_filters.edit().putStringSet("ids", ids).apply();
        SharedPreferences filter_prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
        ActiveFilter deleted_filter = new ActiveFilter();
        deleted_filter.initFromPrefs(filter_prefs);
        filter_prefs.edit().clear().apply();
        File prefs_path = new File(this.getFilesDir(), "../shared_prefs");
        File prefs_xml = new File(prefs_path, prefsName + ".xml");
        final boolean deleted = prefs_xml.delete();
        if (!deleted) {
            Log.w(TAG, "Failed to delete saved filter: " + deleted_filter.getName());
        }
        updateRightDrawer();
    }

    private void updateSavedFilter(String prefsName) {
        SharedPreferences filter_pref = getSharedPreferences(prefsName, MODE_PRIVATE);
        ActiveFilter old_filter = new ActiveFilter();
        old_filter.initFromPrefs(filter_pref);
        String filterName = old_filter.getName();
        mFilter.setName(filterName);
        mFilter.saveInPrefs(filter_pref);
        updateRightDrawer();
    }

    private void renameSavedFilter(String prefsName) {
        final SharedPreferences filter_pref = getSharedPreferences(prefsName, MODE_PRIVATE);
        ActiveFilter old_filter = new ActiveFilter();
        old_filter.initFromPrefs(filter_pref);
        String filterName = old_filter.getName();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.rename_filter);
        alert.setMessage(R.string.rename_filter_message);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);
        input.setText(filterName);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable text = input.getText();
                        String value;
                        if (text == null) {
                            value = "";
                        } else {
                            value = text.toString();
                        }
                        if (value.equals("")) {
                            Util.showToastShort(getApplicationContext(), R.string.filter_name_empty);
                        } else {
                            mFilter.setName(value);
                            mFilter.saveInPrefs(filter_pref);
                            updateRightDrawer();
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


    private void updateLeftDrawer() {
        TaskCache taskBag = getTaskBag();
        ArrayList<String> decoratedContexts = Util.sortWithPrefix(taskBag.getDecoratedContexts(), m_app.sortCaseSensitive(), "@-");
        ArrayList<String> decoratedProjects = Util.sortWithPrefix(taskBag.getDecoratedProjects(), m_app.sortCaseSensitive(), "+-");
        DrawerAdapter drawerAdapter = new DrawerAdapter(getLayoutInflater(),
							getString(R.string.context_prompt),
							decoratedContexts,
							getString(R.string.project_prompt),
							decoratedProjects);

        m_leftDrawerList.setAdapter(drawerAdapter);
        m_leftDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        m_leftDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        for (String context : mFilter.getContexts()) {
            int position = drawerAdapter.getIndexOf("@" + context);
            if (position != -1) {
                m_leftDrawerList.setItemChecked(position, true);
            }
        }

        for (String project : mFilter.getProjects()) {
            int position = drawerAdapter.getIndexOf("+" + project);
            if (position != -1) {
                m_leftDrawerList.setItemChecked(position, true);
            }
        }
        m_leftDrawerList.setItemChecked(drawerAdapter.getContextHeaderPosition(), mFilter.getContextsNot());
        m_leftDrawerList.setItemChecked(drawerAdapter.getProjectsHeaderPosition(), mFilter.getProjectsNot());
    }

    private TaskCache getTaskBag() {
        return m_app.getTaskCache();
    }

    public void startFilterActivity() {
        Intent i = new Intent(this, FilterActivity.class);
        mFilter.saveInIntent(i);
        startActivity(i);
    }

    private static class ViewHolder {
        private TextView tasktext;
        private TextView taskage;
        private TextView taskdue;
        private TextView taskthreshold;
        private CheckBox cbCompleted;
    }

    public class TaskAdapter extends BaseAdapter implements ListAdapter {
        public class VisibleLine {
            @NotNull
            private Task task;
            private boolean header = false;

            public VisibleLine(@NotNull String title) {
                this.task = new Task(0,title);
                this.header = true;
            }

            public VisibleLine(@NotNull Task task) {
                this.task = task;
                this.header = false;
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;

                VisibleLine other = (VisibleLine) obj;
                return other.header == this.header && this.task.equals(other.task);
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                int headerHash = header ? 1231 : 1237;
                result = prime * result + headerHash;
                result = prime * result + task.hashCode();
                return result;
            }
        }

        @NotNull
        ArrayList<VisibleLine> visibleLines = new ArrayList<VisibleLine>();
        @NotNull
        Set<DataSetObserver> obs = new HashSet<DataSetObserver>();
        private LayoutInflater m_inflater;
        private int countVisbleTasks;

        public TaskAdapter(LayoutInflater inflater) {
            this.m_inflater = inflater;
        }

        void setFilteredTasks() {
            ArrayList<Task> visibleTasks;
            countVisbleTasks = 0;
            Log.v(TAG, "setFilteredTasks called: " + getTaskBag());
            ArrayList<String> sorts = mFilter.getSort(m_app.getDefaultSorts());
            visibleTasks = getTaskBag().getTasks(mFilter, sorts);
            visibleLines.clear();

            String header = "";
            String newHeader;
            int firstGroupSortIndex = 0;

            if (sorts.size() > 1 && sorts.get(0).contains("completed")
                    || sorts.get(0).contains("future")) {
                firstGroupSortIndex++;
                if (sorts.size() > 2 && sorts.get(1).contains("completed")
                        || sorts.get(1).contains("future")) {
                    firstGroupSortIndex++;
                }
            }
            String firstSort = sorts.get(firstGroupSortIndex);
            for (Task t : visibleTasks) {
                newHeader = t.getHeader(firstSort, getString(R.string.no_header));
                if (!header.equals(newHeader)) {
                    VisibleLine headerLine = new VisibleLine(newHeader);
                    int last = visibleLines.size() - 1;
                    if (last != -1 && visibleLines.get(last).header && !m_app.showEmptyLists()) {
                        visibleLines.set(last, headerLine);
                    } else {
                        visibleLines.add(headerLine);
                    }
                    header = newHeader;
                }

                if (t.isVisible() || m_app.showHidden()) {
                    // enduring tasks should not be displayed
                    VisibleLine taskLine = new VisibleLine(t);
                    visibleLines.add(taskLine);
                    countVisbleTasks++;
                }
            }
            for (DataSetObserver ob : obs) {
                ob.onChanged();
            }
            updateFilterBar();

        }

        public int getCountVisbleTasks() {
            return countVisbleTasks;
        }
        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            obs.add(observer);
        }

        /*
        ** Get the adapter position for task
        */
        public int getPosition(Task task) {
            VisibleLine line = new VisibleLine(task);
            return visibleLines.indexOf(line);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            obs.remove(observer);
        }

        @Override
        public int getCount() {
            return visibleLines.size();
        }

        @Nullable
        @Override
        public Task getItem(int position) {
            VisibleLine line = visibleLines.get(position);
            if (line.header) {
                return null;
            }
            return line.task;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true; // To change body of implemented methods use File |
            // Settings | File Templates.
        }

        @Nullable
        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            VisibleLine line = visibleLines.get(position);
            if (line.header) {
                if (convertView == null) {
                    convertView = m_inflater.inflate(R.layout.list_header, parent, false);
                }
                TextView t = (TextView) convertView
                        .findViewById(R.id.list_header_title);
                t.setText(line.task.inFileFormat());

            } else {
                final ViewHolder holder;
                if (convertView == null) {
                    convertView = m_inflater.inflate(R.layout.list_item, parent, false);
                    holder = new ViewHolder();
                    holder.tasktext = (TextView) convertView
                            .findViewById(R.id.tasktext);
                    holder.taskage = (TextView) convertView
                            .findViewById(R.id.taskage);
                    holder.taskdue = (TextView) convertView
                            .findViewById(R.id.taskdue);
                    holder.taskthreshold = (TextView) convertView
                            .findViewById(R.id.taskthreshold);
                    holder.cbCompleted = (CheckBox) convertView
                            .findViewById(R.id.checkBox);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                Task task;
                task = line.task;
                if (m_app.showCompleteCheckbox()) {
                    holder.cbCompleted.setVisibility(View.VISIBLE);
                } else {
                    holder.cbCompleted.setVisibility(View.GONE);
                }
                int tokensToShow = Token.SHOW_ALL;
                tokensToShow = tokensToShow & ~Token.CREATION_DATE;
                tokensToShow = tokensToShow & ~Token.COMPLETED;
                tokensToShow = tokensToShow & ~Token.COMPLETED_DATE;
                tokensToShow = tokensToShow & ~Token.THRESHOLD_DATE;
                tokensToShow = tokensToShow & ~Token.DUE_DATE;
                if (mFilter.getHideLists()) {
                    tokensToShow = tokensToShow & ~Token.LIST;
                }
                if (mFilter.getHideTags()) {
                    tokensToShow = tokensToShow & ~Token.TTAG;
                }
                SpannableString ss = new SpannableString(
                        task.showParts(tokensToShow).trim());
                ArrayList<String> colorizeStrings = new ArrayList<String>();
                for (String context : task.getLists()) {
                    colorizeStrings.add("@" + context);
                }
                Util.setColor(ss, Color.GRAY, colorizeStrings);
                colorizeStrings.clear();
                for (String project : task.getTags()) {
                    colorizeStrings.add("+" + project);
                }
                Util.setColor(ss, Color.GRAY, colorizeStrings);

                Resources res = getResources();
                int prioColor;
                switch (task.getPriority()) {
                    case A:
                        prioColor = res.getColor(android.R.color.holo_red_dark);
                        break;
                    case B:
                        prioColor = res.getColor(android.R.color.holo_orange_dark);
                        break;
                    case C:
                        prioColor = res.getColor(android.R.color.holo_green_dark);
                        break;
                    case D:
                        prioColor = res.getColor(android.R.color.holo_blue_dark);
                        break;
                    default:
                        prioColor = res.getColor(android.R.color.darker_gray);
                }
                Util.setColor(ss, prioColor, task.getPriority()
                        .inFileFormat());
                holder.tasktext.setText(ss);
                final ArrayList<Task> tasks = new ArrayList<Task>();
                tasks.add(task);
                if (task.isCompleted()) {
                    // Log.v(TAG, "Striking through " + task.getText());
                    holder.tasktext.setPaintFlags(holder.tasktext
                            .getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.taskage.setPaintFlags(holder.taskage
                            .getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.cbCompleted.setChecked(true);
                    holder.cbCompleted.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            undoCompleteTasks(tasks);
                            finishActionmode();
                        }
                    });
                } else {
                    holder.tasktext
                            .setPaintFlags(holder.tasktext.getPaintFlags()
                                    & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.taskage
                            .setPaintFlags(holder.taskage.getPaintFlags()
                                    & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.cbCompleted.setChecked(false);
                    holder.cbCompleted.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            completeTasks(tasks);
                            finishActionmode();
                        }
                    });
                }

                Context mContext = TodoApplication.getAppContext();

                String relAge = task.getRelativeAge(mContext);
                SpannableString relDue = task.getRelativeDueDate(mContext, res.getColor(android.R.color.holo_green_light),
                        res.getColor(android.R.color.holo_red_light),
                        m_app.hasColorDueDates());
                String relThres = task.getRelativeThresholdDate(mContext);
                if (!Strings.isEmptyOrNull(relAge)) {
                    holder.taskage.setText(relAge);
                    holder.taskage.setVisibility(View.VISIBLE);
                } else {
                    holder.taskage.setText("");
                    holder.taskage.setVisibility(View.GONE);
                }
                if (relDue != null) {
                    holder.taskdue.setText(relDue);
                    holder.taskdue.setVisibility(View.VISIBLE);
                } else {
                    holder.taskdue.setText("");
                    holder.taskdue.setVisibility(View.GONE);
                }
                if (!Strings.isEmptyOrNull(relThres)) {
                    holder.taskthreshold.setText(relThres);
                    holder.taskthreshold.setVisibility(View.VISIBLE);
                } else {
                    holder.taskthreshold.setText("");
                    holder.taskthreshold.setVisibility(View.GONE);
                }
            }
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            VisibleLine line = visibleLines.get(position);
            if (line.header) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return visibleLines.size() == 0;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            VisibleLine line = visibleLines.get(position);
            return !line.header;
        }
    }

    class ActionBarListener implements AbsListView.MultiChoiceModeListener {
        int numSelected;
        Menu menu;

        @Override
        public void onItemCheckedStateChanged(@NotNull ActionMode mode, int position,
                                              long id, boolean checked) {
            Task t = getTaskAt(position);
            if(checked) {
                numSelected++;
            } else {
                numSelected--;
            }
            if (numSelected==0) {
                return;
            }
            String title = "" + numSelected;
            mode.setTitle(title);
            if (numSelected==1 && t!=null && menu!=null) {
                menu.removeGroup(Menu.CATEGORY_SECONDARY);
                for (String s : t.getPhoneNumbers()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.phone_number,
                            Menu.NONE, s);
                }
                for (String s : t.getMailAddresses()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.mail, Menu.NONE, s);
                }
                for (URL u : t.getLinks()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.url, Menu.NONE,
                            u.toString());
                }
                menu.setGroupVisible(Menu.CATEGORY_SECONDARY, true);
            } else if (menu!=null) {
                menu.setGroupVisible(Menu.CATEGORY_SECONDARY, false);
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, @NotNull Menu menu) {
            MenuInflater inflater = getMenuInflater();
            if (m_app.isDarkActionbar()) {
                inflater.inflate(R.menu.task_context, menu);
            } else {
                inflater.inflate(R.menu.task_context_light, menu);
            }
            actionMode = mode;
            if (!m_app.showCompleteCheckbox()) {
                menu.findItem(R.id.complete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.uncomplete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            numSelected = 0;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            this.menu = menu;
            return true;
        }

        @Override
        public boolean onActionItemClicked(@NotNull ActionMode mode, @NotNull MenuItem item) {
            List<Task> checkedTasks = getCheckedTasks();
            int menuid = item.getItemId();
            Intent intent;
            switch (menuid) {
                case R.id.complete:
                    completeTasks(checkedTasks);
                    break;
                case R.id.select_all:
                    selectAllTasks();
                    return true;
                case R.id.uncomplete:
                    undoCompleteTasks(checkedTasks);
                    break;
                case R.id.update:
                    startAddTaskActivity(checkedTasks);
                    break;
                case R.id.delete:
                    deleteTasks(checkedTasks);
                    break;
                case R.id.archive:
                    archiveTasks(checkedTasks);
                    break;
                case R.id.defer_due:
                    deferTasks(checkedTasks, Task.DUE_DATE);
                    break;
                case R.id.defer_threshold:
                    deferTasks(checkedTasks, Task.THRESHOLD_DATE);
                    break;
                case R.id.priority:
                    prioritizeTasks(checkedTasks);
                    return true;
                case R.id.share:
                    String shareText = selectedTasksAsString();
                    shareText(shareText);
                    break;
                case R.id.calendar:
                    String calendarTitle = getString(R.string.calendar_title);
                    String calendarDescription = "";
                    if (checkedTasks.size() == 1) {
                        // Set the task as title
                        calendarTitle = checkedTasks.get(0).getText();
                    } else {
                        // Set the tasks as description
                        calendarDescription = selectedTasksAsString();

                    }
                    intent = new Intent(android.content.Intent.ACTION_EDIT)
                            .setType(Constants.ANDROID_EVENT)
                            .putExtra(Events.TITLE, calendarTitle)
                            .putExtra(Events.DESCRIPTION, calendarDescription);
                    // Explicitly set start and end date/time.
                    // Some calendar providers need this.
                    GregorianCalendar calDate = new GregorianCalendar();
                    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                            calDate.getTimeInMillis());
                    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                            calDate.getTimeInMillis() + 60 * 60 * 1000);
                    startActivity(intent);
                    break;
                case R.id.url:
                    Log.v(TAG, "url: " + item.getTitle().toString());
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item
                            .getTitle().toString()));
                    startActivity(intent);
                    break;
                case R.id.mail:
                    Log.v(TAG, "mail: " + item.getTitle().toString());
                    intent = new Intent(Intent.ACTION_SEND, Uri.parse(item
                            .getTitle().toString()));
                    intent.putExtra(android.content.Intent.EXTRA_EMAIL,
                            new String[]{item.getTitle().toString()});
                    intent.setType("text/plain");
                    startActivity(intent);
                    break;
                case R.id.phone_number:
                    Log.v(TAG, "phone_number");
                    String encodedNumber = Uri.encode(item.getTitle().toString());
                    intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"
                            + encodedNumber));
                    startActivity(intent);
                    break;
                case R.id.update_lists:
                    updateLists(checkedTasks);
                    return true;
                case R.id.update_tags:
                    updateTags(checkedTasks);
                    return true;
            }
            mode.finish();
            return true;
        }


        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            if (m_drawerLayout != null) {
                m_drawerLayout.closeDrawers();
            }
            updateDrawers();
        }
    }

    private void updateLists(@NotNull final List<Task> checkedTasks) {
        final ArrayList<String> contexts = new ArrayList<String>();
        Set<String> selectedContexts = new HashSet<String>();
        final TaskCache taskbag = getTaskBag();
        contexts.addAll(Util.sortWithPrefix(taskbag.getContexts(), m_app.sortCaseSensitive(), null));
        for (Task t : checkedTasks) {
            selectedContexts.addAll(t.getLists());
        }


        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.tag_dialog, null, false);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_multiple_choice,
                contexts.toArray(new String[contexts.size()])));
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        for (String context : selectedContexts) {
            int position = contexts.indexOf(context);
            if (position != -1) {
                lv.setItemChecked(position, true);
            }
        }
        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(this);

        final EditText ed = (EditText) view.findViewById(R.id.editText);
        m_app.setEditTextHint(ed, R.string.new_list_name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> originalLines = new ArrayList<String>();
                originalLines.addAll(Util.tasksToString(getCheckedTasks()));
                ArrayList<String> items = new ArrayList<String>();
                ArrayList<String> uncheckedItesm = new ArrayList<String>();
                uncheckedItesm.addAll(Util.getCheckedItems(lv, false));
                items.addAll(Util.getCheckedItems(lv, true));
                String newText = ed.getText().toString();
                if (!newText.equals("")) {
                    items.add(ed.getText().toString());
                }
                for (String item : items) {
                    for (Task t : checkedTasks) {
                        t.addList(item);
                    }
                }
                for (String item : uncheckedItesm) {
                    for (Task t : checkedTasks) {
                        t.removeTag("@" + item);
                    }
                }
                finishActionmode();
                m_app.getTaskCache().modify(
                        originalLines,
                        checkedTasks,
                        null,
                        null
                        );
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.update_lists);
        dialog.show();
    }

    private void updateTags(@NotNull final List<Task> checkedTasks) {
        final ArrayList<String> projects = new ArrayList<String>();
        Set<String> selectedProjects = new HashSet<String>();
        final TaskCache taskbag = getTaskBag();
        projects.addAll(Util.sortWithPrefix(taskbag.getProjects(), m_app.sortCaseSensitive(), null));
        for (Task t : checkedTasks) {
            selectedProjects.addAll(t.getTags());
        }


        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.tag_dialog, null, false);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_multiple_choice,
                projects.toArray(new String[projects.size()])));
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        for (String context : selectedProjects) {
            int position = projects.indexOf(context);
            if (position != -1) {
                lv.setItemChecked(position, true);
            }
        }

        final EditText ed = (EditText) view.findViewById(R.id.editText);
        m_app.setEditTextHint(ed, R.string.new_tag_name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> items = new ArrayList<String>();
                ArrayList<String> originalLines = new ArrayList<String>();
                originalLines.addAll(Util.tasksToString(checkedTasks));
                ArrayList<String> uncheckedItems = new ArrayList<String>();
                uncheckedItems.addAll(Util.getCheckedItems(lv, false));
                items.addAll(Util.getCheckedItems(lv, true));
                String newText = ed.getText().toString();
                if (!newText.equals("")) {
                    items.add(ed.getText().toString());
                }
                for (String item : items) {
                    for (Task t : checkedTasks) {
                        t.addTag(item);
                    }
                }
                for (String item : uncheckedItems) {
                    for (Task t : checkedTasks) {
                        t.removeTag("+" + item);
                    }
                }
                finishActionmode();
                m_app.getTaskCache().modify(
                        originalLines,
                        checkedTasks,
                        null,
                        null
                );
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.update_tags);
        dialog.show();
    }

    private class DrawerItemClickListener implements
            AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            ArrayList<String> tags;
            ListView lv = (ListView) parent;
            DrawerAdapter adapter = (DrawerAdapter) lv.getAdapter();
            if (adapter.getProjectsHeaderPosition() == position) {
                mFilter.setProjectsNot(!mFilter.getProjectsNot());
            }
            if (adapter.getContextHeaderPosition() == position) {
                mFilter.setContextsNot(!mFilter.getContextsNot());
            } else {
                tags = Util.getCheckedItems(lv, true);
                ArrayList<String> filteredContexts = new ArrayList<String>();
                ArrayList<String> filteredProjects = new ArrayList<String>();

                for (String tag : tags) {
                    if (tag.startsWith("+")) {
                        filteredProjects.add(tag.substring(1));
                    } else if (tag.startsWith("@")) {
                        filteredContexts.add(tag.substring(1));
                    }
                }
                mFilter.setContexts(filteredContexts);
                mFilter.setProjects(filteredProjects);
            }
            Intent intent = getIntent();
            mFilter.saveInIntent(intent);
            mFilter.saveInPrefs(TodoApplication.getPrefs());
            setIntent(intent);
            finishActionmode();
            m_adapter.setFilteredTasks();
            adapter.notifyDataSetChanged();
        }
    }

    private void finishActionmode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }
}
