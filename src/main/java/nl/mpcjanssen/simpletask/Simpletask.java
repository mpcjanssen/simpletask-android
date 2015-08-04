/**
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @author Mark Janssen
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
 * @copyright 2015 Vojtech Kral
 */

package nl.mpcjanssen.simpletask;

import android.annotation.SuppressLint;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;

import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableString;

import android.view.*;
import android.widget.*;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.adapters.DrawerAdapter;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;
import nl.mpcjanssen.simpletask.task.token.Token;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;


public class Simpletask extends ThemedActivity implements
        AbsListView.OnScrollListener, AdapterView.OnItemLongClickListener {

    private final static int REQUEST_SHARE_PARTS = 1;
    private final static int REQUEST_PREFERENCES = 2;

    private final static String ACTION_LINK = "link";
    private final static String ACTION_PHONE = "phone";
    private final static String ACTION_MAIL = "mail";

    public final static Uri URI_BASE = Uri.fromParts("simpletask", "", null);
    public final static Uri URI_SEARCH = Uri.withAppendedPath(URI_BASE, "search");

    @Nullable
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
    int m_scrollPosition = 0;
    private Dialog mOverlayDialog;
    private boolean mIgnoreScrollEvents = false;
    private org.slf4j.Logger log;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = LoggerFactory.getLogger(this.getClass());
        log.info("onCreate");
        m_app = (TodoApplication) getApplication();
        m_savedInstanceState = savedInstanceState;
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_ACTION_ARCHIVE);
        intentFilter.addAction(Constants.BROADCAST_ACTION_LOGOUT);
        intentFilter.addAction(Constants.BROADCAST_UPDATE_UI);
        intentFilter.addAction(Constants.BROADCAST_SYNC_START);
        intentFilter.addAction(Constants.BROADCAST_SYNC_DONE);


        localBroadcastManager = m_app.getLocalBroadCastManager();

        m_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_ACTION_ARCHIVE)) {
                    // archive
                    // refresh screen to remove completed tasks
                    // push to remote
                    archiveTasks(null);
                } else if (intent.getAction().equals(Constants.BROADCAST_ACTION_LOGOUT)) {
                    log.info("Logging out from Dropbox");
                    getFileStore().logout();
                    Intent i = new Intent(context, LoginScreen.class);
                    startActivity(i);
                    finish();
                } else if (intent.getAction().equals(Constants.BROADCAST_UPDATE_UI)) {
                    log.info("Updating UI because of broadcast");
                    if (m_adapter==null) {
                        return;
                    }
                    m_adapter.setFilteredTasks();
                    updateDrawers();
                } else if (intent.getAction().equals(Constants.BROADCAST_SYNC_START)) {
                    mOverlayDialog = Util.showLoadingOverlay(Simpletask.this, mOverlayDialog, true);
                } else if (intent.getAction().equals(Constants.BROADCAST_SYNC_DONE)) {
                    mOverlayDialog = Util.showLoadingOverlay(Simpletask.this, mOverlayDialog, false);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SHARE_PARTS:
                if (resultCode != Activity.RESULT_CANCELED) {
                    int flags = resultCode - Activity.RESULT_FIRST_USER;
                    shareTodoList(flags);
                }
                break;
            case REQUEST_PREFERENCES:
                if (resultCode == Preferences.RESULT_RECREATE_ACTIVITY) {
                    Intent i = new Intent(getApplicationContext(), Simpletask.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    finish();
                    m_app.reloadTheme();
                    m_app.startActivity(i);
                }
                break;
        }
    }


    private void showHelp() {
        Intent i = new Intent(this, HelpScreen.class);
        startActivity(i);
    }

    @Override
    public boolean onSearchRequested() {
        if (options_menu==null) {
            return false;
        }
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

    @NonNull
    private String selectedTasksAsString() {
        List<String> result = new ArrayList<>();
        for (Task t : getTodoList().getSelectedTasks()) {
            result.add(t.inFileFormat());
        }
        return Util.join(result, "\n");
    }

    private void selectAllTasks() {
        ArrayList<Task> selectedTasks = new ArrayList<>();
        ListView lv = getListView();
        int itemCount = lv.getCount();
        for (int i = 0; i < itemCount; i++) {
            // Only check tasks that are not checked yet
            // and skip headers
            // This prevents double counting in the CAB title
            Task t = getTaskAt(i);
            if (t == null) {
                continue;
            }
            selectedTasks.add(t);
            if (!lv.isItemChecked(i)) {
                lv.setItemChecked(i, true);
            }
        }
        getTodoList().setSelectedTasks(selectedTasks);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (m_drawerToggle != null) {
            m_drawerToggle.onConfigurationChanged(newConfig);
        }
    }


    private void handleIntent() {
        if (!m_app.isAuthenticated()) {
            log.info("handleIntent: not authenticated");
            startLogin();
            return;
        }
        mOverlayDialog = Util.showLoadingOverlay(this, mOverlayDialog, m_app.isLoading());

        mFilter = new ActiveFilter();

        m_leftDrawerList = (ListView) findViewById(R.id.left_drawer);
        m_rightDrawerList = (ListView) findViewById(R.id.right_drawer_list);

        m_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Set the list's click listener
        m_leftDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (m_drawerLayout != null) {
            m_drawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                    m_drawerLayout, /* DrawerLayout object */
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
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeButtonEnabled(true);
                m_drawerToggle.setDrawerIndicatorEnabled(true);
            }
            m_drawerToggle.syncState();
        }

        // Show search or filter results
        Intent intent = getIntent();
        if (Constants.INTENT_START_FILTER.equals(intent.getAction())) {
            mFilter.initFromIntent(intent);
            log.info("handleIntent: launched with filter" + mFilter);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    if (value != null) {
                        log.debug(String.format("%s %s (%s)", key,
                                value.toString(), value.getClass().getName()));
                    } else {
                        log.debug(String.format("%s %s)", key, "<null>"));
                    }

                }

            }
            log.info("handleIntent: saving filter in prefs");
            mFilter.saveInPrefs(TodoApplication.getPrefs());
        } else {
            // Set previous filters and sort
            log.info("handleIntent: from m_prefs state");
            mFilter.initFromPrefs(TodoApplication.getPrefs());
        }

        // Initialize Adapter
        if (m_adapter == null) {
            m_adapter = new TaskAdapter(getLayoutInflater());
        }
        m_adapter.setFilteredTasks();

        getListView().setAdapter(this.m_adapter);

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setClickable(true);
        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(this);


        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ArrayList<String> links = new ArrayList<>();
                final ArrayList<String> actions = new ArrayList<>();
                getListView().setItemChecked(position, !getListView().isItemChecked(position));
                if (getTodoList().getSelectedTasks().size() > 0) {
                    onItemLongClick(parent, view, position, id);
                    return;
                }
                Task t = getTaskAt(position);
                if (t != null) {
                    for (String link : t.getLinks()) {
                        actions.add(ACTION_LINK);
                        links.add(link);
                    }
                    for (String number : t.getPhoneNumbers()) {
                        actions.add(ACTION_PHONE);
                        links.add(number);
                    }
                    for (String mail : t.getMailAddresses()) {
                        actions.add(ACTION_MAIL);
                        links.add(mail);
                    }
                }
                final String[] linksArray = links.toArray(new String[links.size()]);
                if (linksArray.length == 0) {
                    onItemLongClick(parent, view, position, id);
                } else {
                    AlertDialog.Builder build = new AlertDialog.Builder(Simpletask.this);
                    build.setTitle(R.string.task_action);
                    build.setItems(linksArray, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent;
                            String url = links.get(which);
                            log.info("" + actions.get(which) + ": " + url);
                            switch (actions.get(which)) {
                                case ACTION_LINK:
                                    if (url.startsWith("todo://")) {
                                        File todoFolder = m_app.getTodoFile().getParentFile();
                                        File newName = new File(todoFolder, url.substring(7));
                                        m_app.switchTodoFile(newName.getAbsolutePath(),true);
                                    } else {
                                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        startActivity(intent);
                                    }
                                    break;
                                case ACTION_PHONE:
                                    String encodedNumber = Uri.encode(url);
                                    intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"
                                            + encodedNumber));
                                    startActivity(intent);
                                    break;
                                case ACTION_MAIL:
                                    intent = new Intent(Intent.ACTION_SEND, Uri.parse(url));
                                    intent.putExtra(android.content.Intent.EXTRA_EMAIL,
                                            new String[]{url});
                                    intent.setType("text/plain");
                                    startActivity(intent);

                            }
                        }
                    });
                    build.create().show();
                }
            }
        });

        mIgnoreScrollEvents = true;
        // Setting a scroll listener reset the scroll
        lv.setOnScrollListener(this);
        mIgnoreScrollEvents = false;
        if (m_savedInstanceState != null) {
            m_scrollPosition = m_savedInstanceState.getInt("position");
        }


        // If we were started with a selected task,
        // select it now and clear it from the intent
        int selectedTask = intent.getIntExtra(Constants.INTENT_SELECTED_TASK_POSITION, -1);
        if (selectedTask != -1) {
            m_scrollPosition = selectedTask;
            getTodoList().clearSelectedTasks();
            getTodoList().selectTask(m_scrollPosition);
            intent.removeExtra(Constants.INTENT_SELECTED_TASK_POSITION);
            setSelectedTasks(getTodoList().getSelectedTasks());
            setIntent(intent);
        } else {
            setSelectedTasks(getTodoList().getSelectedTasks());
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        lv.setSelectionFromTop(m_scrollPosition, 0);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAddTaskActivity(null);
            }
        });
        if(getTodoList().getSelectedTasks().size()==0) {
            closeSelectionMode();
        }
        updateDrawers();
    }

    private void setSelectedTasks(List<Task> tasks) {
        for (Task t : tasks) {
            int position = m_adapter.getPosition(t);
            if (position != -1) {
                ListView lv = getListView();
                lv.setItemChecked(position, true);
                lv.setSelection(position);
            }
        }
    }

    private void updateFilterBar() {
        ListView lv = getListView();
        if (lv == null) {
            return;
        }
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
        int count = m_adapter != null ? m_adapter.getCountVisbleTasks() : 0;
        int total = getTodoList().size();

        filterText.setText(mFilter.getTitle(
                count,
                total,
                getText(R.string.priority_prompt),
                m_app.getTagTerm(),
                m_app.getListTerm(),
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
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
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {

        this.options_menu = menu;
        if (getTodoList().getSelectedTasks().size()> 0) {
            openSelectionMode();
        } else {
            populateMainMenu(menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void populateMainMenu(@Nullable final Menu menu) {

        if (menu==null) {
            log.warn("Menu was null");
            return;
        }
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        if (!getFileStore().supportsSync()) {
            MenuItem mItem = menu.findItem(R.id.sync);
            mItem.setVisible(false);
        }
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenu = menu.findItem(R.id.search);

        SearchView searchView = (SearchView) searchMenu.getActionView();
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
                    if (m_adapter != null) {
                        m_adapter.setFilteredTasks();
                    }
                }
                return true;
            }
        });
    }

    @Nullable
    private Task getTaskAt(final int pos) {
        return m_adapter.getItem(pos);
    }

    private void shareTodoList(int format) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < m_adapter.getCount(); i++) {
            Task task = m_adapter.getItem(i);
            if (task != null) {
                text.append(task.showParts(format)).append("\n");
            }
        }
        Util.shareText(this, text.toString());
    }



    private void prioritizeTasks(@NonNull final List<Task> tasks) {
        List<String> strings = Priority.rangeInCode(Priority.NONE, Priority.Z);
        final String[] prioArr = strings.toArray(new String[strings.size()]);

        int prioIdx = 0;
        if (tasks.size() == 1) {
            prioIdx = strings.indexOf(tasks.get(0).getPriority().getCode());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_priority);
        builder.setSingleChoiceItems(prioArr, prioIdx, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, final int which) {
                dialog.dismiss();
                Priority prio = Priority.toPriority(prioArr[which]);
                getTodoList().prioritize(tasks, prio);
                getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);
                closeSelectionMode();
            }
        });
        builder.show();

    }

    private void completeTasks(@NonNull Task task) {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(task);
        completeTasks(tasks);
    }

    private void completeTasks(@NonNull List<Task> tasks) {
        for (Task t : tasks) {
            getTodoList().complete(t, m_app.hasKeepPrio());
        }
        if (m_app.isAutoArchive()) {
            archiveTasks(null);
        }
        closeSelectionMode();
        getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);
    }

    private void undoCompleteTasks(@NonNull Task task) {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(task);
        undoCompleteTasks(tasks);
    }

    private void undoCompleteTasks(@NonNull List<Task> tasks) {
        getTodoList().undoComplete(tasks);
        closeSelectionMode();
        getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);
    }

    private void deferTasks(List<Task> tasks, final int dateType) {
        final List<Task> tasksToDefer = tasks;
        Dialog d = Util.createDeferDialog(this, dateType, true, new Util.InputDialogListener() {
            @Override
            public void onClick(@Nullable String selected) {
                if (selected == null) {
                    log.warn("Can't defer, selected is null. This should not happen");
                    return;
                }
                if (selected.equals("pick")) {
                    final DateTime today = DateTime.today(TimeZone.getDefault());
                    DatePickerDialog dialog = new DatePickerDialog(Simpletask.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                            month++;

                            DateTime date = DateTime.forDateOnly(year, month, day);
                            for (Task t : tasksToDefer) {
                                m_app.getTodoList().defer(date.format(Constants.DATE_FORMAT), t, dateType);
                            }
                            closeSelectionMode();
                            getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);

                        }
                    },
                            today.getYear(),
                            today.getMonth() - 1,
                            today.getDay()
                    );
                    boolean showCalendar = m_app.showCalendar();

                    dialog.getDatePicker().setCalendarViewShown(showCalendar);
                    dialog.getDatePicker().setSpinnersShown(!showCalendar);
                    dialog.show();
                } else {
                    for (Task t : tasksToDefer) {
                        m_app.getTodoList().defer(selected, t, dateType);
                    }
                    closeSelectionMode();
                    getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app,true );

                }

            }
        });
        d.show();
    }

    private void deleteTasks(final List<Task> tasks) {
        m_app.showConfirmationDialog(this, R.string.delete_task_message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (Task t : tasks) {
                    m_app.getTodoList().remove(t);
                }
                closeSelectionMode();
                getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);

            }
        }, R.string.delete_task_title);
    }

    private void archiveTasks(List<Task> tasksToArchive) {
        if (m_app.getTodoFileName().equals(m_app.getDoneFileName())) {
            Util.showToastShort(this, "You have the done.txt file opened.");
            return;
        }
        getTodoList().archive(getFileStore(), m_app.getTodoFileName(), m_app.getDoneFileName(), tasksToArchive, m_app.getEol());
        closeSelectionMode();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (m_drawerToggle != null && m_drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        log.info("onMenuItemSelected: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.search:
                break;
            case R.id.preferences:
                startPreferencesActivity();
                break;
            case R.id.filter:
                startFilterActivity();
                break;
            case R.id.share:
                startActivityForResult(new Intent(getBaseContext(), TaskDisplayActivity.class), REQUEST_SHARE_PARTS);
                break;
            case R.id.help:
                showHelp();
                break;
            case R.id.sync:
                getFileStore().sync();
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
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void startAddTaskActivity(List<Task> tasks) {
        log.info("Starting addTask activity");
        getTodoList().setSelectedTasks(tasks);
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
        clearFilter();
    }

    @NonNull
    public ArrayList<ActiveFilter> getSavedFilter() {
        ArrayList<ActiveFilter> saved_filters = new ArrayList<>();
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
        if (m_drawerLayout != null) {
            if (m_drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                m_drawerLayout.closeDrawer(Gravity.LEFT);
                return;
            }
            if (m_drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                m_drawerLayout.closeDrawer(Gravity.RIGHT);
                return;
            }
        }
        if (getTodoList().getSelectedTasks().size() > 0) {
            closeSelectionMode();
            return;
        }
        if (m_app.backClearsFilter() && mFilter != null && mFilter.hasFilter()) {
            clearFilter();
            onNewIntent(getIntent());
            return;
        }

        super.onBackPressed();
    }

    private void closeSelectionMode() {
        getTodoList().clearSelectedTasks();
        getListView().clearChoices();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.GONE);
        //getTodoList().clearSelectedTasks();
        populateMainMenu(options_menu);
        //updateDrawers();

    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Intent currentIntent = getIntent();
            currentIntent.putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY));
            setIntent(currentIntent);
            if (options_menu==null) {
                return;
            }
            options_menu.findItem(R.id.search).collapseActionView();

        } else if (CalendarContract.ACTION_HANDLE_CUSTOM_EVENT.equals(intent.getAction())) {
            // Uri uri = Uri.parse(intent.getStringExtra(CalendarContract.EXTRA_CUSTOM_APP_URI));
            log.warn("Not implenented search");
        } else if (intent.getExtras() != null) {
            // Only change intent if it actually contains a filter
            setIntent(intent);
        }
        log.info("onNewIntent: " + intent);

    }

    void clearFilter() {
        // Also clear the intent so we wont get the old filter after
        // switching back to app later fixes [1c5271ee2e]
        Intent intent = new Intent();
        mFilter.clear();
        mFilter.saveInIntent(intent);
        mFilter.saveInPrefs(TodoApplication.getPrefs());
        setIntent(intent);
        closeSelectionMode();
        updateDrawers();
        m_adapter.setFilteredTasks();
    }

    private void updateDrawers() {
        updateLeftDrawer();
        updateRightDrawer();
    }

    private void updateRightDrawer() {
        ArrayList<String> names = new ArrayList<>();
        final ArrayList<ActiveFilter> filters = getSavedFilter();
        Collections.sort(filters, new Comparator<ActiveFilter>() {
            public int compare(@NonNull ActiveFilter f1, @NonNull ActiveFilter f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        for (ActiveFilter f : filters) {
            names.add(f.getName());
        }
        m_rightDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, names));
        m_rightDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        m_rightDrawerList.setLongClickable(true);
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
                    public boolean onMenuItemClick(@NonNull MenuItem item) {
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

    public void createFilterShortcut(@NonNull ActiveFilter filter) {
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
        HashSet<String> ids = new HashSet<>();
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
            log.warn("Failed to delete saved filter: " + deleted_filter.getName());
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
        TodoList taskBag = getTodoList();
        ArrayList<String> decoratedContexts = Util.sortWithPrefix(taskBag.getDecoratedContexts(), m_app.sortCaseSensitive(), "@-");
        ArrayList<String> decoratedProjects = Util.sortWithPrefix(taskBag.getDecoratedProjects(), m_app.sortCaseSensitive(), "+-");
        DrawerAdapter drawerAdapter = new DrawerAdapter(getLayoutInflater(),
                m_app.getListTerm(),
                decoratedContexts,
                m_app.getTagTerm(),
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

    private TodoList getTodoList() {
        return m_app.getTodoList();
    }

    private FileStoreInterface getFileStore() {
        return m_app.getFileStore();
    }

    public void startFilterActivity() {
        Intent i = new Intent(this, FilterActivity.class);
        mFilter.saveInIntent(i);
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Task t = getTaskAt(position);
        boolean selected = !getListView().isItemChecked(position);
        if (selected) {
            getTodoList().selectTask(t);
        } else {
            getTodoList().unSelectTask(t);
        }
        getListView().setItemChecked(position, selected);
        int numSelected = getTodoList().getSelectedTasks().size();
        if (numSelected == 0) {
            closeSelectionMode();
        } else {
            openSelectionMode();
        }
        return true;
    }

    private void openSelectionMode() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (options_menu==null) {
            return;
        }
        options_menu.clear();
        MenuInflater inflater = getMenuInflater();
        Menu menu = toolbar.getMenu();
        menu.clear();
        inflater.inflate(R.menu.task_context, toolbar.getMenu());

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                List<Task> checkedTasks = getTodoList().getSelectedTasks();
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
                        Util.shareText(Simpletask.this, shareText);
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
                        intent = new Intent(Intent.ACTION_EDIT)
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
                    case R.id.update_lists:
                        updateLists(checkedTasks);
                        return true;
                    case R.id.update_tags:
                        updateTags(checkedTasks);
                        return true;
                }
                return true;
            }
        });
        if (!m_app.showCompleteCheckbox()) {
            menu.findItem(R.id.complete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.uncomplete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        toolbar.setVisibility(View.VISIBLE);
    }


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (!mIgnoreScrollEvents) {
            m_scrollPosition = firstVisibleItem;
        }
    }

    public ListView getListView() {
        View lv = findViewById(android.R.id.list);
        return (ListView) lv;
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
            private Task task;
            private String title = "";
            private boolean header = false;

            public VisibleLine(@NonNull String title) {
                this.title = title;
                this.header = true;
            }

            public VisibleLine(@NonNull Task task) {
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

        @NonNull
        ArrayList<VisibleLine> visibleLines = new ArrayList<>();
        @NonNull
        private LayoutInflater m_inflater;
        private int countVisbleTasks;

        public TaskAdapter(@NonNull LayoutInflater inflater) {
            this.m_inflater = inflater;
        }

        void setFilteredTasks() {
            if (m_app.showTodoPath()) {
                setTitle(m_app.getTodoFileName().replaceAll("([^/])[^/]*/", "$1/"));
            } else {
                setTitle(R.string.app_label);
            }
            List<Task> visibleTasks;
            countVisbleTasks = 0;
            log.info("setFilteredTasks called: " + getTodoList());
            ArrayList<String> sorts = mFilter.getSort(m_app.getDefaultSorts());
            visibleTasks = getTodoList().getSortedTasksCopy(mFilter, sorts, m_app.sortCaseSensitive());
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
            notifyDataSetChanged();
            updateFilterBar();
        }

        public int getCountVisbleTasks() {
            return countVisbleTasks;
        }

        /*
        ** Get the adapter position for task
        */
        public int getPosition(Task task) {
            VisibleLine line = new VisibleLine(task);
            return visibleLines.indexOf(line);
        }

        @Override
        public int getCount() {
            return visibleLines.size()+1;
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
            if (position == visibleLines.size()) {
                if (convertView==null) {
                    convertView = m_inflater.inflate(R.layout.empty_list_item, parent, false);
                }
                return convertView;
            }
            VisibleLine line = visibleLines.get(position);
            if (line.header) {
                if (convertView == null) {
                    convertView = m_inflater.inflate(R.layout.list_header, parent, false);
                }
                TextView t = (TextView) convertView
                        .findViewById(R.id.list_header_title);
                t.setText(line.title);

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
                final Task task = line.task;
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
                ArrayList<String> colorizeStrings = new ArrayList<>();
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

                if (task.isCompleted()) {
                    // log.info( "Striking through " + task.getText());
                    holder.tasktext.setPaintFlags(holder.tasktext
                            .getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.taskage.setPaintFlags(holder.taskage
                            .getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.cbCompleted.setChecked(true);
                    holder.cbCompleted.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            undoCompleteTasks(task);
                            closeSelectionMode();
                            getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);

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
                            completeTasks(task);
                            closeSelectionMode();
                            getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);
                        }
                    });
                }

                Context mContext = TodoApplication.getAppContext();

                String relAge = task.getRelativeAge(mContext);
                SpannableString relDue = task.getRelativeDueDate(mContext, res.getColor(android.R.color.holo_green_light),
                        res.getColor(android.R.color.holo_red_light),
                        m_app.hasColorDueDates());
                String relThres = task.getRelativeThresholdDate(mContext);
                if (!Strings.isEmptyOrNull(relAge) && !mFilter.getHideCreateDate()) {
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
            if (position == visibleLines.size()) {
                return 2;
            }
            VisibleLine line = visibleLines.get(position);
            if (line.header) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 3;
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
            if (position == visibleLines.size()) {
                return false;
            }
            VisibleLine line = visibleLines.get(position);
            return !line.header;
        }
    }

    private void updateLists(@NonNull final List<Task> checkedTasks) {
        final ArrayList<String> contexts = new ArrayList<>();
        Set<String> selectedContexts = new HashSet<>();
        final TodoList todoList = getTodoList();
        contexts.addAll(Util.sortWithPrefix(todoList.getContexts(), m_app.sortCaseSensitive(), null));
        for (Task t : checkedTasks) {
            selectedContexts.addAll(t.getLists());
        }


        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.tag_dialog, null, false);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        lv.setAdapter(new ArrayAdapter<>(this, R.layout.simple_list_item_multiple_choice,
                contexts.toArray(new String[contexts.size()])));
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        for (String context : selectedContexts) {
            int position = contexts.indexOf(context);
            if (position != -1) {
                lv.setItemChecked(position, true);
            }
        }

        final EditText ed = (EditText) view.findViewById(R.id.editText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> items = new ArrayList<>();
                ArrayList<String> uncheckedItems = new ArrayList<>();
                uncheckedItems.addAll(Util.getCheckedItems(lv, false));
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
                for (String item : uncheckedItems) {
                    for (Task t : checkedTasks) {
                        t.removeTag("@" + item);
                    }
                }
                getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);
                closeSelectionMode();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(m_app.getListTerm());
        dialog.show();
    }

    private void updateTags(@NonNull final List<Task> checkedTasks) {
        final ArrayList<String> projects = new ArrayList<>();
        Set<String> selectedProjects = new HashSet<>();
        final TodoList taskbag = getTodoList();
        projects.addAll(Util.sortWithPrefix(taskbag.getProjects(), m_app.sortCaseSensitive(), null));
        for (Task t : checkedTasks) {
            selectedProjects.addAll(t.getTags());
        }


        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.tag_dialog, null, false);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        lv.setAdapter(new ArrayAdapter<>(this, R.layout.simple_list_item_multiple_choice,
                projects.toArray(new String[projects.size()])));
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        for (String context : selectedProjects) {
            int position = projects.indexOf(context);
            if (position != -1) {
                lv.setItemChecked(position, true);
            }
        }

        final EditText ed = (EditText) view.findViewById(R.id.editText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> items = new ArrayList<>();
                ArrayList<String> uncheckedItems = new ArrayList<>();
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
                getTodoList().notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);
                closeSelectionMode();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(m_app.getTagTerm());
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
                updateDrawers();
            }
            if (adapter.getContextHeaderPosition() == position) {
                mFilter.setContextsNot(!mFilter.getContextsNot());
                updateDrawers();
            } else {
                tags = Util.getCheckedItems(lv, true);
                ArrayList<String> filteredContexts = new ArrayList<>();
                ArrayList<String> filteredProjects = new ArrayList<>();

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
            closeSelectionMode();
            m_adapter.setFilteredTasks();
        }
    }
}
