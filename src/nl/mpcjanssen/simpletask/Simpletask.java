/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask;

import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Events;
import android.text.SpannableString;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.task.*;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletaskdonate.R;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class Simpletask extends ListActivity  {

    final static String TAG = Simpletask.class.getSimpleName();
    private final static int REQUEST_FILTER = 1;
    private final static int REQUEST_PREFERENCES = 2;

    private TaskBag taskBag;
    ProgressDialog m_ProgressDialog = null;
    String m_DialogText = "";
    Boolean m_DialogActive = false;
    Menu options_menu;
    MainApplication m_app;


    // filter variables
    private ArrayList<Priority> m_prios = new ArrayList<Priority>();
    private ArrayList<String> m_contexts = new ArrayList<String>();
    private ArrayList<String> m_projects = new ArrayList<String>();
    private ArrayList<String> m_sorts = new ArrayList<String>();
    private boolean m_projectsNot = false;
    private String m_search;
    private boolean m_priosNot;
    private boolean m_contextsNot;

    TaskAdapter m_adapter;

    private BroadcastReceiver m_broadcastReceiver;



    private ActionMode actionMode;
    private Task m_selectedTask;


    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == REQUEST_FILTER) {
            if (resultCode == RESULT_OK) {
                setIntent(data);
                handleIntent(null);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        m_app = (MainApplication) getApplication();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.INTENT_UPDATE_UI);


        m_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(
                        Constants.INTENT_UPDATE_UI)) {
                    m_adapter.setFilteredTasks(true);
                }
            }
        };
        registerReceiver(m_broadcastReceiver, intentFilter);

        handleIntent(savedInstanceState);
    }

    private void handleIntent(Bundle savedInstanceState) {

        setContentView(R.layout.main);

        taskBag = m_app.getTaskBag();

        // Show search or filter results
        clearFilter(false);

        Intent intent = getIntent();
        if (savedInstanceState != null) {
            m_prios = Priority.toPriority(savedInstanceState
                    .getStringArrayList("m_prios"));
            m_contexts = savedInstanceState.getStringArrayList("m_contexts");
            m_projects = savedInstanceState.getStringArrayList("m_projects");
            m_search = savedInstanceState.getString("m_search");
            m_contextsNot = savedInstanceState.getBoolean("m_contextsNot");
            m_priosNot = savedInstanceState.getBoolean("m_priosNot");
            m_projectsNot = savedInstanceState.getBoolean("m_projectsNot");
            m_sorts = savedInstanceState.getStringArrayList("m_sorts");
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            m_search = intent.getStringExtra(SearchManager.QUERY);
            Log.v(TAG, "Searched for " + m_search);
        } else if (intent.getExtras() != null) {
            Log.v(TAG, "Launched with filter:");

            // handle different versions of shortcuts
            String prios;
            String projects;
            String contexts;
            String sorts;

            prios = intent.getStringExtra(Constants.INTENT_PRIORITIES_FILTER);
            projects = intent.getStringExtra(Constants.INTENT_PROJECTS_FILTER);
            contexts = intent.getStringExtra(Constants.INTENT_CONTEXTS_FILTER);
            sorts = intent.getStringExtra(Constants.INTENT_SORT_ORDER);
            m_priosNot = intent.getBooleanExtra(
                    Constants.INTENT_PRIORITIES_FILTER_NOT, false);
            m_projectsNot = intent.getBooleanExtra(
                    Constants.INTENT_PROJECTS_FILTER_NOT, false);
            m_contextsNot = intent.getBooleanExtra(
                    Constants.INTENT_CONTEXTS_FILTER_NOT, false);

            Log.v(TAG, "\t sort:" + sorts);
            if (sorts != null && !sorts.equals("")) {
                m_sorts = new ArrayList<String>(Arrays.asList(sorts
                        .split("\n")));
                Log.v(TAG, "\t sorts:" + m_sorts);
            }
            if (prios != null && !prios.equals("")) {
                m_prios = Priority.toPriority(Arrays.asList(prios.split("\n")));
                Log.v(TAG, "\t prio:" + m_prios);
            }
            if (projects != null && !projects.equals("")) {
                m_projects = new ArrayList<String>(Arrays.asList(projects
                        .split("\n")));
                Log.v(TAG, "\t projects:" + m_projects);
            }
            if (contexts != null && !contexts.equals("")) {
                m_contexts = new ArrayList<String>(Arrays.asList(contexts
                        .split("\n")));
                Log.v(TAG, "\t contexts:" + m_contexts);
            }
        } else {
            // Set previous filters and sort
            m_sorts = new ArrayList<String>();
            m_sorts.addAll(Arrays.asList(m_app.m_prefs.getString("m_sorts", "").split("\n")));

            Log.v(TAG, "Got sort from app prefs: " + m_sorts);

            m_contexts = new ArrayList<String>(m_app.m_prefs.getStringSet("m_contexts", Collections.<String>emptySet()));
            m_prios = Priority.toPriority(new ArrayList<String>(
                    m_app.m_prefs.getStringSet("m_prios", Collections.<String>emptySet())));
            m_projects = new ArrayList<String>(m_app.m_prefs.getStringSet("m_projects", Collections.<String>emptySet()));
            m_contextsNot = m_app.m_prefs.getBoolean("m_contextsNot", false);
            m_priosNot = m_app.m_prefs.getBoolean("m_priosNot", false);
            m_projectsNot = m_app.m_prefs.getBoolean("m_projectsNot", false);

        }

        if (m_sorts==null || m_sorts.size()==0 || Strings.isEmptyOrNull(m_sorts.get(0))) {
            // Set a default sort
            m_sorts = new ArrayList<String>();
            for (String type : getResources().getStringArray(R.array.sortKeys)) {
                m_sorts.add(Constants.NORMAL_SORT + Constants.SORT_SEPARATOR + type);
            }

        }
        // Initialize Adapter
        if (m_adapter == null) {
            m_adapter = new TaskAdapter(this, R.layout.list_item,
                    getLayoutInflater(), getListView());
            setListAdapter(this.m_adapter);
        }
        m_adapter.setFilteredTasks(true);

        // listen to the ACTION_LOGOUT intent, if heard display LoginScreen
        // and finish() current activity


        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setMultiChoiceModeListener(new ActionBarListener());

        // Did we launch with a selected task set?
        if (intent.hasExtra(Constants.INTENT_SELECTED_TASK_ID)) {
            long taskId = intent.getLongExtra(Constants.INTENT_SELECTED_TASK_ID, -1);
            String taskText = intent.getStringExtra(Constants.INTENT_SELECTED_TASK_TEXT);
            Log.v(TAG, "Task selected: #" + taskId + "," + taskText );
            m_adapter.select(new Task(taskId, taskText));
            intent.removeExtra(Constants.INTENT_SELECTED_TASK_ID);
            intent.removeExtra(Constants.INTENT_SELECTED_TASK_TEXT);
            this.setIntent(intent);
        }
    }

    private void updateFilterBar() {
        ListView lv = getListView();
        int index = lv.getFirstVisiblePosition();
        View v = lv.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();
        lv.setSelectionFromTop(index, top);

        final ImageButton actionbar_clear = (ImageButton) findViewById(R.id.actionbar_clear);
        final TextView filterText = (TextView) findViewById(R.id.filter_text);
        if (m_contexts.size() + m_projects.size() + m_prios.size() > 0
                || !Strings.isEmptyOrNull(m_search)) {
            String filterTitle = getString(R.string.title_filter_applied);
            if (m_prios.size() > 0) {
                filterTitle += " " + getString(R.string.priority_prompt);
            }

            if (m_projects.size() > 0) {
                filterTitle += " " + getString(R.string.project_prompt);
            }

            if (m_contexts.size() > 0) {
                filterTitle += " " + getString(R.string.context_prompt);
            }
            if (!Strings.isEmptyOrNull(m_search)) {
                filterTitle += " " + getString(R.string.search);
            }

            actionbar_clear.setVisibility(View.VISIBLE);
            filterText.setText(filterTitle);

        } else {
            actionbar_clear.setVisibility(View.GONE);
            filterText.setText("No filter");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(m_broadcastReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("m_prios", Priority.inCode(m_prios));
        outState.putStringArrayList("m_contexts", m_contexts);
        outState.putStringArrayList("m_projects", m_projects);
        outState.putBoolean("m_contextsNot", m_contextsNot);
        outState.putStringArrayList("m_sorts", m_sorts);
        outState.putBoolean("m_priosNot", m_priosNot);
        outState.putBoolean("m_projectsNot", m_projectsNot);
        outState.putString("m_search", m_search);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v(TAG, "onRestart: " + getIntent().getExtras());
        handleIntent(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (actionMode != null) {
            actionMode.finish();
        }
        SharedPreferences.Editor editor = m_app.m_prefs.edit();
        Log.v(TAG, "Storing sort in prefs: " + m_sorts);
        editor.putString("m_sorts", Util.join(m_sorts, "\n"));
        editor.putStringSet("m_contexts", new HashSet<String>(m_contexts));
        editor.putStringSet("m_prios", new HashSet<String>(Priority.inCode(m_prios)));
        editor.putStringSet("m_projects", new HashSet<String>(m_projects));
        editor.putBoolean("m_contextsNot", m_contextsNot);
        editor.putBoolean("m_priosNot", m_priosNot);
        editor.putBoolean("m_projectsNot", m_projectsNot);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget;
        // expand it by default


        this.options_menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // toggle selected state
        l.setItemChecked(position, !l.isItemChecked(position));
    }

    private Task getTaskAt(final int pos) {
        return m_adapter.getTask(pos);
    }

    private void shareTodoList() {
        String text = "";
        for (int i = 0; i < m_adapter.getCount(); i++) {
            Task task = m_adapter.getItem(i);
            if (task != null) {
                text = text + (task.inFileFormat()) + "\n";
            }
        }

        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Simpletask list");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);

        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    private void prioritizeTasks(final List<Task> tasks) {
        List<String> strings = Priority
                .rangeInCode(Priority.NONE, Priority.Z);
        final String[] prioArr = strings.toArray(new String[strings.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select priority");
        builder.setSingleChoiceItems(prioArr, 0, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {

                dialog.dismiss();
                for (Task task : tasks) {
                    if (task != null) {
                        task.setPriority(Priority.toPriority(prioArr[which]));
                    }
                }
                taskBag.store();
                m_app.updateWidgets();
                // We have change the data, views should refresh
                m_adapter.setFilteredTasks(false);
            }
        });
        builder.show();

    }

    private void completeTasks(List<Task> tasks) {
        for (Task t : tasks) {
            if (t != null && !t.isCompleted()) {
                t.markComplete(new Date());
            }
        }
        taskBag.store();
        m_app.updateWidgets();
        // We have change the data, views should refresh
        m_adapter.setFilteredTasks(true);
    }

    private void undoCompleteTasks(List<Task> tasks) {
        for (Task t : tasks) {
            if (t != null && t.isCompleted()) {
                t.markIncomplete();
            }
        }
        taskBag.store();
        m_app.updateWidgets();
        // We have change the data, views should refresh
        m_adapter.setFilteredTasks(true);
    }

    private void deferTasks(List<Task> tasks) {
        String[] keys = getResources().getStringArray(R.array.deferOptions);
        SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT,Locale.US);
        Date now = new Date();
        final List<Task> tasksToDefer = tasks;
        String today = formatter.format(now);
        String oneWeek = formatter.format(Util.addWeeksToDate(now, 7));
        String twoWeeks = formatter.format(Util.addWeeksToDate(now, 14));
        String oneMonth = formatter.format(Util.addMonthsToDate(now, 1));
        String[] values = {today, oneWeek,twoWeeks,oneMonth};

        Dialog d = Util.createSingleChoiceDialog(this,keys,values, 1, R.string.defertask, null,
                new Util.OnSingleChoiceDialogListener() {
                    @Override
                    public void onClick(String selected) {
                        for (Task t : tasksToDefer) {
                            if (t != null) {
                                t.setPrependedDate(selected);
                            }
                        }
                        m_adapter.setFilteredTasks(false);
                        taskBag.store();
                        m_app.updateWidgets();
                    }
                }
        );
        d.show();
    }
    private void deleteTasks(List<Task> tasks) {
        final List<Task> tasksToDelete = tasks;
        Util.showDeleteConfirmationDialog( this, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (Task t : tasksToDelete) {
                    if (t != null) {
                        taskBag.delete(t);
                    }
                }
                m_adapter.setFilteredTasks(false);
                taskBag.store();
                m_app.updateWidgets();
            }
        });
    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
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
            case R.id.quickfilter:
                changeList();
                break;
            case R.id.archive:
                taskBag.archive();
                m_adapter.setFilteredTasks(true);
                break;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
        return true;
    }

    private void changeList() {
        final ArrayList<String> contexts = taskBag.getContexts(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(contexts.toArray(new String[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int which) {
                        clearFilter(true);
                        m_contexts.add(contexts.get(which));
                        m_adapter.setFilteredTasks(false);
                    }
                });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.context_prompt);
        dialog.show();
    }

    private void startAddTaskActivity(Task task) {
        Log.v(TAG, "Starting addTask activity");
        Intent intent = new Intent(this, AddTask.class);
        intent.putExtra(Constants.EXTRA_TASK, task);
        intent.putExtra(Constants.EXTRA_CONTEXTS_SELECTED, m_contexts);
        intent.putExtra(Constants.EXTRA_PROJECTS_SELECTED, m_projects);
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
        // End current activity if it's search results
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            finish();
        } else { // otherwise just clear the filter in the current activity
            clearFilter(true);
            m_adapter.setFilteredTasks(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getExtras() != null) {
            // Only change intent if it actually contains a filter
            setIntent(intent);
        }
        Log.v(TAG, "onNewIntent: " + intent);
        //handleIntent(null);
    }

    void clearFilter(boolean clearIntent) {

        if (clearIntent) {
            // Also clear the intent so we wont get the old filter after
            // switching back to app later fixes [1c5271ee2e]
            Intent intent = new Intent();
            intent.putExtra(Constants.INTENT_SORT_ORDER, Util.join(m_sorts, "\n"));
            setIntent(intent);
        }
        m_prios = new ArrayList<Priority>();
        m_contexts = new ArrayList<String>();
        m_projects = new ArrayList<String>();
        m_projectsNot = false;
        m_search = null;
        m_priosNot = false;
        m_contextsNot = false;
    }



    public class TaskAdapter extends BaseAdapter implements ListAdapter, Filterable {

        private LayoutInflater m_inflater;
        int vt;
        ArrayList<Task> visibleTasks = new ArrayList<Task>();
        Set<DataSetObserver> obs = new HashSet<DataSetObserver>();
        SparseArray<String> headerTitles = new SparseArray<String>();
        SparseArray<Long> positionToIndex = new SparseArray<Long>();
        SparseArray<Long> indexToPosition = new SparseArray<Long>();
        int size = 0;

        public TaskAdapter(Context context, int textViewResourceId,
                           LayoutInflater inflater, ListView view) {
            this.m_inflater = inflater;
            this.vt = textViewResourceId;

        }

        void setFilteredTasks(boolean reload) {
            Log.v(TAG, "setFilteredTasks called, reload: " + reload);
            if (reload) {
                taskBag.reload();
            }

            AndFilter filter = new AndFilter();
            visibleTasks.clear();
            for (Task t : taskBag.getTasks()) {
                if (filter.apply(t)) {
                    visibleTasks.add(t);
                }
            }
            Collections.sort(visibleTasks, MultiComparator.create(m_sorts));
            positionToIndex.clear();
            indexToPosition.clear();
            headerTitles.clear();
            String header = "";
            int index = 0;
            int position = 0;
            int firstGroupSortIndex = 0;

            if (m_sorts.size() > 1 && m_sorts.get(0).contains("completed") || m_sorts.get(0).contains("future")) {
                firstGroupSortIndex++;
                if (m_sorts.size() > 2 && m_sorts.get(1).contains("completed") || m_sorts.get(1).contains("future")) {
                    firstGroupSortIndex++;
                }
            }

            String firstSort = m_sorts.get(firstGroupSortIndex);
            for (Task t : visibleTasks) {
                if (firstSort.contains("by_context")) {
                    List<String> taskItems = t.getContexts();
                    String newHeader;
                    if (taskItems == null || taskItems.size() == 0) {
                        newHeader = getString(R.string.no_context);
                    } else {
                        newHeader = taskItems.get(0);
                    }
                    if (!header.equals(newHeader)) {
                        header = newHeader;

                        // Log.v(TAG, "Start of header: " + header +
                        // " at position: " + position);
                        headerTitles.put(position,header);
                        positionToIndex.put(position,null);
                        position++;
                    }
                } else if (firstSort.contains("by_project")) {
                    List<String> taskItems = t.getProjects();
                    String newHeader;
                    if (taskItems == null || taskItems.size() == 0) {
                        newHeader = getString(R.string.no_project);
                    } else {
                        newHeader = taskItems.get(0);
                    }
                    if (!header.equals(newHeader)) {
                        header = newHeader;
                        Log.v(TAG, "Start of header: " + header
                                + " at position: " + position);
                        headerTitles.put(position,header);
                        positionToIndex.put(position,null);
                        position++;
                    }
                }
                positionToIndex.put(position,new Long(index));
                indexToPosition.put(index,new Long(position));
                index++;
                position++;

            }
            size = position;
            for (DataSetObserver ob : obs) {
                ob.onChanged();
            }
            updateFilterBar();

        }


        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            obs.add(observer);
            return;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            obs.remove(observer);
            return;
        }

        @Override
        public int getCount() {
            return size;
        }

        @Override
        public Task getItem(int position) {
            if (positionToIndex.get(position) == null) {
                return null;
            }
            return visibleTasks.get(positionToIndex.get(position).intValue());
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (headerTitles.get(position,null) != null) {
                convertView = m_inflater.inflate(R.layout.list_header, null);
                TextView t = (TextView) convertView
                        .findViewById(R.id.list_header_title);
                t.setText(headerTitles.get(position));
                t.setTextSize(m_app.headerFontSize());

            } else {
                final ViewHolder holder;
                if (convertView == null) {
                    convertView = m_inflater.inflate(R.layout.list_item, null);
                    holder = new ViewHolder();
                    holder.tasktext = (TextView) convertView
                            .findViewById(R.id.tasktext);
                    holder.taskage = (TextView) convertView
                            .findViewById(R.id.taskage);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }
                Task task;
                task = getItem(position);

                if (task != null) {
                    SpannableString ss = new SpannableString(
                            task.inScreenFormat());

                    ArrayList<String> colorizeStrings = new ArrayList<String>();
                    for (String context : task.getContexts()) {
                        colorizeStrings.add("@" + context);
                    }
                    Util.setColor(ss, Color.GRAY, colorizeStrings);
                    colorizeStrings.clear();
                    for (String project : task.getProjects()) {
                        colorizeStrings.add("+" + project);
                    }
                    Util.setColor(ss, Color.GRAY, colorizeStrings);

                    Resources res = getResources();
                    int prioColor;
                    switch (task.getPriority()) {
                        case A:
                            prioColor = res.getColor(R.color.green);
                            break;
                        case B:
                            prioColor = res.getColor(R.color.blue);
                            break;
                        case C:
                            prioColor = res.getColor(R.color.orange);
                            break;
                        case D:
                            prioColor = res.getColor(R.color.gold);
                            break;
                        default:
                            prioColor = res.getColor(R.color.black);
                    }
                    Util.setColor(ss, prioColor, task.getPriority().inFileFormat());
                    holder.tasktext.setText(ss);
                    holder.tasktext.setTextColor(res.getColor(R.color.black));

                    if (task.isCompleted()) {
                        // Log.v(TAG, "Striking through " + task.getText());
                        holder.tasktext.setPaintFlags(holder.tasktext
                                .getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        holder.taskage.setPaintFlags(holder.taskage
                                .getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        holder.tasktext
                                .setPaintFlags(holder.tasktext.getPaintFlags()
                                        & ~Paint.STRIKE_THRU_TEXT_FLAG);
                        holder.taskage
                                .setPaintFlags(holder.taskage.getPaintFlags()
                                        & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    }

                    if (!Strings.isEmptyOrNull(task.getRelativeAge())) {
                        holder.taskage.setText(task.getRelativeAge());
                        holder.taskage.setVisibility(View.VISIBLE);
                    } else {
                        holder.tasktext.setPadding(
                                holder.tasktext.getPaddingLeft(),
                                holder.tasktext.getPaddingTop(),
                                holder.tasktext.getPaddingRight(), 4);
                        holder.taskage.setText("");
                        holder.taskage.setVisibility(View.GONE);
                    }
                }
                holder.tasktext.setTextSize(m_app.taskTextFontSize());
                holder.taskage.setTextSize(m_app.taskAgeFontSize());
            }
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            if (headerTitles.get(position) != null) {
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
            return visibleTasks.size() == 0;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (headerTitles.get(position,null) != null) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    m_search = charSequence.toString();
                    Log.v(TAG, "performFiltering: " + charSequence.toString());
                    return null;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    setFilteredTasks(false);
                }
            };
        }

        public void select(Task selectedTask) {
            int index = visibleTasks.indexOf(selectedTask);
            Log.v(TAG, "Selected task found at: " + index);
            if (index == -1) {
                Log.w(TAG, "Selected task not found");
                return;
            } else {
                Log.v(TAG, "Selected task found at: " + index);
            }
            int position = indexToPosition.get(index).intValue();
            ListView lv = getListView();
            lv.setItemChecked(position,true);
            lv.smoothScrollToPosition(position);
        }

        public Task getTask(int pos) {
            return visibleTasks.get(positionToIndex.get(pos).intValue());
        }
    }

    private static class ViewHolder {
        private TextView tasktext;
        private TextView taskage;
    }

    public void startFilterActivity() {
        Intent i = new Intent(this, FilterActivity.class);

        i.putStringArrayListExtra(Constants.EXTRA_PRIORITIES,
                Priority.inCode(taskBag.getPriorities()));
        i.putStringArrayListExtra(Constants.EXTRA_PROJECTS,
                taskBag.getProjects(true));
        i.putStringArrayListExtra(Constants.EXTRA_CONTEXTS,
                taskBag.getContexts(true));

        i.putStringArrayListExtra(Constants.EXTRA_PRIORITIES_SELECTED,
                Priority.inCode(m_prios));
        i.putStringArrayListExtra(Constants.EXTRA_PROJECTS_SELECTED, m_projects);
        i.putStringArrayListExtra(Constants.EXTRA_CONTEXTS_SELECTED, m_contexts);
        i.putStringArrayListExtra(Constants.EXTRA_SORTS_SELECTED, m_sorts);
        i.putExtra(Constants.EXTRA_CONTEXTS + "not", m_contextsNot);
        i.putExtra(Constants.EXTRA_PRIORITIES + "not", m_priosNot);
        i.putExtra(Constants.EXTRA_PROJECTS + "not", m_projectsNot);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivityForResult(i, REQUEST_FILTER);
    }

    class ActionBarListener implements AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            getListView().invalidateViews();
            rebuildMenuWithSelection(mode, mode.getMenu());
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.task_context, menu);
            rebuildMenuWithSelection(mode, menu);
            actionMode = mode;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // To change body of implemented methods use File |
            // Settings | File Templates.
        }

        private void rebuildMenuWithSelection(ActionMode mode, Menu menu) {
            List<Task> checkedTasks = getCheckedTasks();
            int numSelected = checkedTasks.size();
            String title = "";
            title = title + numSelected;
            title = title + " " + getString(R.string.selected);
            mode.setTitle(title);
            if (numSelected == 1) {
                // show the edit menu item and hide the appropriate complete/uncomplete item
                menu.findItem(R.id.update).setVisible(true);
                if (checkedTasks.get(0).isCompleted()) {
                    menu.findItem(R.id.done).setVisible(false);
                } else {
                    menu.findItem(R.id.uncomplete).setVisible(false);
                }
            } else {
                menu.findItem(R.id.update).setVisible(false);
                menu.findItem(R.id.done).setVisible(true);
                menu.findItem(R.id.uncomplete).setVisible(true);
            }
            menu.removeGroup(Menu.CATEGORY_SECONDARY);
            for (Task t : getCheckedTasks()) {
                for (String s : t.getPhoneNumbers()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.phone_number, Menu.NONE,
                            s);
                }
                for (String s : t.getMailAddresses()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.mail, Menu.NONE,
                            s);
                }
                for (URL u : t.getLinks()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.url, Menu.NONE,
                            u.toString());
                }
            }

        }


        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            List<Task> checkedTasks = getCheckedTasks();
            int menuid = item.getItemId();
            Intent intent;
            switch (menuid) {
                case R.id.update:
                    startAddTaskActivity(checkedTasks.get(0));
                    break;
                case R.id.done:
                    completeTasks(checkedTasks);
                    break;
                case R.id.delete:
                    deleteTasks(checkedTasks);
                    break;
                case R.id.defer:
                    deferTasks(checkedTasks);
                    break;
                case R.id.uncomplete:
                    undoCompleteTasks(checkedTasks);
                    break;
                case R.id.priority:
                    prioritizeTasks(checkedTasks);
                    break;
                case R.id.share:
                    String shareText = selectedTasksAsString();
                    intent = new Intent(android.content.Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(android.content.Intent.EXTRA_SUBJECT,
                                    "Todo.txt task")
                            .putExtra(android.content.Intent.EXTRA_TEXT, shareText);
                    startActivity(Intent.createChooser(intent, "Share"));
                    break;
                case R.id.calendar:
                    List<Task> selectedTasks = getCheckedTasks();
                    String calendarTitle = getString(R.string.calendar_title);
                    String calendarDescription = "";
                    if (selectedTasks.size() == 1) {
                        // Set the task as title
                        calendarTitle = selectedTasks.get(0).getText();
                    } else {
                        // Set the tasks as description
                        calendarDescription = selectedTasksAsString();

                    }
                    intent = new Intent(android.content.Intent.ACTION_EDIT)
                            .setType(Constants.ANDROID_EVENT)
                            .putExtra(Events.TITLE, calendarTitle)
                            .putExtra(Events.DESCRIPTION, calendarDescription);
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
                    intent = new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + encodedNumber));
                    startActivity(intent);
                    break;
            }
            mode.finish();
            m_adapter.setFilteredTasks(false);
            return true;
        }

        private String selectedTasksAsString() {
            List<String> result = new ArrayList<String>();
            for (Task t : getCheckedTasks()) {
                result.add(t.inFileFormat());
            }
            return Util.join(result, "\n");
        }

        private List<Task> getCheckedTasks() {
            ArrayList<Task> checkedTasks = new ArrayList<Task>();
            SparseBooleanArray checkedItems = getListView()
                    .getCheckedItemPositions();
            for (int i = 0; i < checkedItems.size(); i++) {
                if (checkedItems.valueAt(i) == true) {
                    checkedTasks.add(getTaskAt(checkedItems.keyAt(i)));
                }
            }
            return checkedTasks;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            m_adapter.setFilteredTasks(false);
            return;
        }
    }

    private class AndFilter {
        private ArrayList<TaskFilter> filters = new ArrayList<TaskFilter>();

        private AndFilter() {
            this.filters = filters;
            filters.clear();
            if (m_prios.size() > 0) {
                addFilter(new ByPriorityFilter(m_prios, m_priosNot));
            }
            if (m_contexts.size() > 0) {
                addFilter(new ByContextFilter(m_contexts, m_contextsNot));
            }
            if (m_projects.size() > 0) {
                addFilter(new ByProjectFilter(m_projects, m_projectsNot));
            }

            if (!Strings.isEmptyOrNull(m_search)) {
                addFilter(new ByTextFilter(m_search, false));
            }
        }

        public void addFilter(TaskFilter filter) {
            if (filter != null) {
                filters.add(filter);
            }
        }

        public boolean apply(Task input) {
            for (TaskFilter f : filters) {
                if (!f.apply(input)) {
                    return false;
                }
            }
            return true;
        }
    }
}
