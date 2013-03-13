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
package nl.mpcjanssen.todotxtholo;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.*;
import android.content.DialogInterface.OnClickListener;
import android.database.DataSetObserver;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.CalendarContract.Events;
import android.text.SpannableString;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import nl.mpcjanssen.todotxtholo.sort.*;
import nl.mpcjanssen.todotxtholo.task.*;
import nl.mpcjanssen.todotxtholo.util.Strings;
import nl.mpcjanssen.todotxtholo.util.Util;

import java.net.URL;
import java.util.*;

public class TodoTxtTouch extends ListActivity {

    final static String TAG = TodoApplication.TAG;
    Menu options_menu;

    private BroadcastReceiver m_broadcastReceiver;

    // filter variables
    private ArrayList<Priority> m_prios = new ArrayList<Priority>();
    private ArrayList<String> m_contexts = new ArrayList<String>();
    private ArrayList<String> m_projects = new ArrayList<String>();
    private boolean m_projectsNot = false;
    private boolean m_priosNot;
    private boolean m_contextsNot;
    private String m_search;
    private int sort = 0;

    TaskAdapter m_adapter;

    private TodoApplication m_app;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(m_broadcastReceiver);
        m_app.watchDropbox(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_app.watchDropbox(false);
    }

    private boolean loginIfNeeded() {
        m_app = (TodoApplication) getApplication();
        if (!m_app.isAuthenticated()) {
            Intent i = new Intent(this, LoginScreen.class);
            startActivity(i);
            finish();
            return true;
        }
	return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate with intent: " + getIntent());
        m_app = (TodoApplication) getApplication();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.INTENT_RELOAD_TASKBAG);
        m_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(
                        Constants.INTENT_RELOAD_TASKBAG)) {
                    m_app.initTaskBag();
                    m_adapter.setFilteredTasks();
                }
            }
        };
        registerReceiver(m_broadcastReceiver, intentFilter);

	if(loginIfNeeded()) {
		return;
	}
        setContentView(R.layout.main);

        m_app.watchDropbox(true);


        // Initialize Adapter
        ListView lv = getListView();
        m_adapter = new TaskAdapter(this, R.layout.list_item,
                getLayoutInflater(), lv);
        setListAdapter(this.m_adapter);

        handleIntent(getIntent(), savedInstanceState);

        lv.setTextFilterEnabled(true);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setMultiChoiceModeListener(new ActionBarListener());
        m_adapter.setFilteredTasks();
    }

    private void handleIntent(Intent intent, Bundle savedInstanceState) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            m_search = intent.getStringExtra(SearchManager.QUERY);
            Log.v(TAG, "Searched for " + m_search);
        } else if (intent.getExtras()!=null) {
            Log.v(TAG, "Launched with extras, setting filter");

            String prios;
            String projects;
            String contexts;

            prios = intent
                    .getStringExtra(Constants.INTENT_PRIORITIES_FILTER);
            projects = intent
                    .getStringExtra(Constants.INTENT_PROJECTS_FILTER);
            contexts = intent
                    .getStringExtra(Constants.INTENT_CONTEXTS_FILTER);
            sort = intent.getIntExtra(Constants.INTENT_ACTIVE_SORT,
                    Constants.SORT_UNSORTED);
            m_priosNot = intent.getBooleanExtra(
                    Constants.INTENT_PRIORITIES_FILTER_NOT, false);
            m_projectsNot = intent.getBooleanExtra(
                    Constants.INTENT_PROJECTS_FILTER_NOT, false);
            m_contextsNot = intent.getBooleanExtra(
                    Constants.INTENT_CONTEXTS_FILTER_NOT, false);

            if (prios != null && !prios.equals("")) {
                m_prios = Priority.toPriority(Arrays.asList(prios.split("\n")));
            }
            if (projects != null && !projects.equals("")) {
                m_projects = new ArrayList<String>(Arrays.asList(projects
                        .split("\n")));
            }
            if (contexts != null && !contexts.equals("")) {
                m_contexts = new ArrayList<String>(Arrays.asList(contexts
                        .split("\n")));
            }
        }  else if (savedInstanceState != null) {
            m_prios = Priority.toPriority(savedInstanceState
                    .getStringArrayList("m_prios"));
            m_contexts = savedInstanceState.getStringArrayList("m_contexts");
            m_projects = savedInstanceState.getStringArrayList("m_projects");
            m_search = savedInstanceState.getString("m_search");
            m_projectsNot = savedInstanceState.getBoolean("m_projectsNot");
            m_priosNot = savedInstanceState.getBoolean("m_priosNot");
            m_contextsNot = savedInstanceState.getBoolean("m_contextsNot");
            sort = savedInstanceState.getInt("sort", Constants.SORT_UNSORTED);
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
            if (!Strings.isEmptyOrNull(m_search) ) {
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("m_prios", Priority.inCode(m_prios));
        outState.putStringArrayList("m_contexts", m_contexts);
        outState.putStringArrayList("m_projects", m_projects);
        outState.putBoolean("m_projectsNot", m_projectsNot);
        outState.putBoolean("m_priosNot", m_priosNot);
        outState.putBoolean("m_contextsNot", m_contextsNot);
        outState.putString("m_search", m_search);
        outState.putInt("sort", sort);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.v(TAG, "Calling with new intent: " + intent );
	if(loginIfNeeded()) {
		return;
	}
        if(intent.getExtras()!=null) {
            handleIntent(intent, null);
        }
	m_app.watchDropbox(true);
        m_adapter.setFilteredTasks();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.v(TAG, "Loading from saved instance state");
        m_prios = Priority.toPriority(savedInstanceState
                .getStringArrayList("m_prios"));
        m_contexts = savedInstanceState.getStringArrayList("m_contexts");
        m_projects = savedInstanceState.getStringArrayList("m_projects");
        m_search = savedInstanceState.getString("m_search");
        m_projectsNot = savedInstanceState.getBoolean("m_projectsNot");
        m_priosNot = savedInstanceState.getBoolean("m_priosNot");
        m_contextsNot = savedInstanceState.getBoolean("m_contextsNot");
        sort = savedInstanceState.getInt("sort", Constants.SORT_UNSORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
	if (loginIfNeeded()) {
		return;
	}
        m_app.watchDropbox(true);
        handleIntent(getIntent(),null);
        m_adapter.setFilteredTasks();
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
        l.setItemChecked(position, true);
    }

    private Task getTaskAt(final int pos) {
        return m_adapter.getItem(pos);
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
        final String[] prioArr = Priority
                .rangeInCodeArray(Priority.NONE, Priority.E);

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
                m_app.storeTaskbag();
                m_adapter.setFilteredTasks();
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
        m_app.storeTaskbag();
        m_adapter.setFilteredTasks();
    }

    private void undoCompleteTasks(List<Task> tasks) {
        for (Task t : tasks) {
            if (t != null && t.isCompleted()) {
                t.markIncomplete();
            }
        }
        m_app.storeTaskbag();
        m_adapter.setFilteredTasks();
    }

    private void editTask(Task task) {
        Intent intent = new Intent(this, AddTask.class);
        intent.putExtra(Constants.EXTRA_TASK, task);
        startActivity(intent);
    }

    private void deleteTasks(List<Task> tasks) {
        m_app.getTaskBag().deleteTasks(tasks);
        m_app.storeTaskbag();
        m_adapter.setFilteredTasks();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_new:
                startAddTaskActivity();
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
            default:
                return super.onMenuItemSelected(featureId, item);
        }
        return true;
    }

    private void startAddTaskActivity() {
        Intent intent = new Intent(this, AddTask.class);
        intent.putExtra(Constants.EXTRA_CONTEXTS_SELECTED, m_contexts);
        intent.putExtra(Constants.EXTRA_PROJECTS_SELECTED, m_projects);
        startActivity(intent);
    }

    private void startPreferencesActivity() {
        Intent settingsActivity = new Intent(getBaseContext(),
                Preferences.class);
        startActivity(settingsActivity);
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
            clearFilter();
        }
    }

    void clearSelection() {
        ListView lv = getListView();
        lv.getCheckedItemPositions();
        SparseBooleanArray checkedItems = getListView()
                .getCheckedItemPositions();
        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.valueAt(i)) {
                lv.setItemChecked(checkedItems.keyAt(i), false);
            }
        }
    }

    void clearFilter() {
        m_contexts = new ArrayList<String>();
        m_prios = new ArrayList<Priority>();
        m_projects = new ArrayList<String>();
        m_priosNot = false;
        m_projectsNot = false;
        m_contextsNot = false;
        m_search = null;
        m_adapter.setFilteredTasks();
    }

    private MultiComparator getActiveSort() {
        List<Comparator<?>> comparators = new ArrayList<Comparator<?>>();
        // Sort completed last
        comparators.add(new CompletedComparator());
        switch (sort) {
            case Constants.SORT_UNSORTED:
                break;
            case Constants.SORT_REVERSE:
                comparators.add(Collections.reverseOrder());
                break;
            case Constants.SORT_ALPHABETICAL:
                comparators.add(new AlphabeticalComparator());
                break;
            case Constants.SORT_CONTEXT:
                comparators.add(new ContextComparator());
                comparators.add(new AlphabeticalComparator());
                break;
            case Constants.SORT_PRIORITY:
                comparators.add(new PriorityComparator());
                comparators.add(new AlphabeticalComparator());
                break;
            case Constants.SORT_PROJECT:
                comparators.add(new ProjectComparator());
                comparators.add(new AlphabeticalComparator());
                break;
        }
        return (new MultiComparator(comparators));
    }

    public class TaskAdapter extends BaseAdapter implements ListAdapter, Filterable {

        private LayoutInflater m_inflater;
        int vt;
        ArrayList<Task> visibleTasks = new ArrayList<Task>();
        Set<DataSetObserver> obs = new HashSet<DataSetObserver>();
        ArrayList<String> headerTitles = new ArrayList<String>();
        SparseArray<String> headerAtPostion = new SparseArray<String>();
        int headersShow = 0;

        public TaskAdapter(Context context, int textViewResourceId,
                           LayoutInflater inflater, ListView view) {
            this.m_inflater = inflater;
            this.vt = textViewResourceId;
        }

        void setFilteredTasks() {
            clearSelection();
            AndFilter filter = new AndFilter();
            visibleTasks.clear();
            for (Task t : m_app.getTaskBag().getTasks()) {
                if (filter.apply(t)) {
                    visibleTasks.add(t);
                }
            }
            Collections.sort(visibleTasks, getActiveSort());
            headerAtPostion.clear();
            String header = "";
            int position = 0;
            switch (sort) {
                case Constants.SORT_CONTEXT:
                    for (Task t : visibleTasks) {
                        List<String> taskItems = t.getContexts();
                        String newHeader;
                        if (taskItems == null || taskItems.size() == 0) {
                            newHeader = getString(R.string.no_context);
                        } else {
                            newHeader = taskItems.get(0);
                        }
                        if (!header.equals(newHeader)) {
                            header = newHeader;
                            headerAtPostion.put(position, header);
                            position++;
                        }
                        position++;
                    }
                    break;
                case Constants.SORT_PROJECT:
                    for (Task t : visibleTasks) {
                        List<String> taskItems = t.getProjects();
                        String newHeader;
                        if (taskItems == null || taskItems.size() == 0) {
                            newHeader = getString(R.string.no_project);
                        } else {
                            newHeader = taskItems.get(0);
                        }
                        if (!header.equals(newHeader)) {
                            header = newHeader;
                            headerAtPostion.put(position, header);
                            position++;
                        }
                        position++;
                    }
                    break;

            }
            for (DataSetObserver ob : obs) {
                ob.onChanged();
            }
            updateFilterBar();
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            obs.add(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            obs.remove(observer);
        }

        @Override
        public int getCount() {
            return visibleTasks.size() + headerAtPostion.size();
        }

        private int headersBeforePosition(int position) {
            int smaller = 0;
            for (int index = 0; index < headerAtPostion.size(); index++) {
                if (headerAtPostion.keyAt(index) < position) {
                    smaller++;
                }
            }
            return smaller;
        }

        @Override
        public Task getItem(int position) {
            if (headerAtPostion.get(position) != null) {
                return null;
            }
            return visibleTasks.get(position - headersBeforePosition(position));
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (headerAtPostion.get(position) != null) {
                convertView = m_inflater.inflate(R.layout.list_header, null);
                TextView t = (TextView) convertView
                        .findViewById(R.id.list_header_title);
                t.setText(headerAtPostion.get(position));

            } else {
                final ViewHolder holder;
                if (convertView == null) {
                    convertView = m_inflater.inflate(R.layout.list_item, null);
                    holder = new ViewHolder();
                    holder.taskprio = (TextView) convertView
                            .findViewById(R.id.taskprio);
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
                    holder.taskprio.setText(task.getPriority().inListFormat());
                    SpannableString ss = new SpannableString(
                            task.inScreenFormat());
                    holder.tasktext.setText(ss);

                    if (task.isCompleted()) {
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

                    if (!Strings.isEmptyOrNull(task.getRelativeAge(getApplicationContext()))) {
                        holder.taskage.setText(task.getRelativeAge(getApplicationContext()));
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

            }
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            if (headerAtPostion.get(position) != null) {
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
            return (headerAtPostion.get(position)==null);
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    m_search = charSequence.toString();
                    Log.v(TAG,"performFiltering: " + charSequence.toString());
                    return null;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    setFilteredTasks();
                }
            };
        }
    }

    private static class ViewHolder {
        private TextView taskprio;
        private TextView tasktext;
        private TextView taskage;
    }

    public void startFilterActivity() {
        Intent i = new Intent(this, FilterActivity.class);

        i.putStringArrayListExtra(Constants.EXTRA_PRIORITIES,
                Priority.inCode(m_app.getTaskBag().getPriorities()));
        i.putStringArrayListExtra(Constants.EXTRA_PROJECTS,
                m_app.getTaskBag().getProjects());
        i.putStringArrayListExtra(Constants.EXTRA_CONTEXTS,
                m_app.getTaskBag().getContexts());

        i.putStringArrayListExtra(Constants.EXTRA_PRIORITIES_SELECTED,
                Priority.inCode(m_prios));
        i.putStringArrayListExtra(Constants.EXTRA_PROJECTS_SELECTED, m_projects);
        i.putStringArrayListExtra(Constants.EXTRA_CONTEXTS_SELECTED, m_contexts);
        i.putExtra(Constants.EXTRA_SORT_SELECTED, sort);
        i.putExtra(Constants.EXTRA_CONTEXTS + "not", m_contextsNot);
        i.putExtra(Constants.EXTRA_PRIORITIES + "not", m_priosNot);
        i.putExtra(Constants.EXTRA_PROJECTS + "not", m_projectsNot);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(i);
    }

    class ActionBarListener implements AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            rebuildMenuWithSelection(mode, mode.getMenu());
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.task_context, menu);
            rebuildMenuWithSelection(mode, menu);
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
                Task task = checkedTasks.get(0);
                menu.findItem(R.id.update).setVisible(true);
                for (URL url : task.getLinks()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.url, Menu.NONE,
                            url.toString());
                }
                for (String s1 : task.getMailAddresses()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.mail, Menu.NONE, s1);
                }
                for (String s : task.getPhoneNumbers()) {
                    menu.add(Menu.CATEGORY_SECONDARY, R.id.phone_number,
                            Menu.NONE, s);
                }
            } else {
                menu.findItem(R.id.update).setVisible(false);
                menu.removeGroup(Menu.CATEGORY_SECONDARY);
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            List<Task> checkedTasks = getCheckedTasks();
            int menuid = item.getItemId();
            Intent intent;
            switch (menuid) {
                case R.id.done:
                    completeTasks(checkedTasks);
                    break;
                case R.id.update:
                    if (checkedTasks.size() == 1) {
                        editTask(checkedTasks.get(0));
                    } else {
                        Log.w(TAG,
                                "More than one task was selected while handling update menu");
                    }
                    break;
                case R.id.delete:
                    deleteTasks(checkedTasks);
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
                    intent = new Intent(android.content.Intent.ACTION_INSERT)
                            .setData(Events.CONTENT_URI)
                            .putExtra(Events.TITLE, calendarTitle)
                            .putExtra(Events.DESCRIPTION, calendarDescription);
                    startActivity(intent);
                    break;
                case R.id.url:
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item
                            .getTitle().toString()));
                    startActivity(intent);
                    break;
                case R.id.mail:
                    intent = new Intent(Intent.ACTION_SEND, Uri.parse(item
                            .getTitle().toString()));
                    intent.putExtra(android.content.Intent.EXTRA_EMAIL,
                            new String[]{item.getTitle().toString()});
                    intent.setType("text/plain");
                    startActivity(intent);
                    break;
                case R.id.phone_number:
                    intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"
                            + item.getTitle().toString()));
                    startActivity(intent);
                    break;
            }
            // Close CAB
            mode.finish();
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
                if (checkedItems.valueAt(i)) {
                    checkedTasks.add(getTaskAt(checkedItems.keyAt(i)));
                }
            }
            return checkedTasks;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Nothing to do here
        }
    }

    private class AndFilter {
        private ArrayList<TaskFilter> filters = new ArrayList<TaskFilter>();

        public void addFilter(TaskFilter filter) {
            if (filter != null) {
                filters.add(filter);
            }
        }

        private boolean apply(Task input) {
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
            for (TaskFilter f : filters) {
                if (!f.apply(input)) {
                    return false;
                }
            }
            return true;
        }
    }
}
