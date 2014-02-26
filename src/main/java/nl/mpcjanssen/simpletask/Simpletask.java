/**
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @author Mark Janssen
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
 */

package nl.mpcjanssen.simpletask;

import nl.mpcjanssen.simpletask.adapters.DrawerAdapter;
import nl.mpcjanssen.simpletask.remote.RemoteClient;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletask.util.Util.OnMultiChoiceDialogListener;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.SpannableString;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;


import java.io.File;
import java.net.URL;
import java.util.*;

import static java.lang.Thread.sleep;


public class Simpletask extends ThemedListActivity  implements AdapterView.OnItemLongClickListener {

    final static String TAG = Simpletask.class.getSimpleName();
    private final static int REQUEST_FILTER = 1;
    private final static int REQUEST_PREFERENCES = 2;
    private final static int DRAWER_CONTEXT = 1;
    private final static int DRAWER_PROJECT = 2;
    private static final int SYNC_CHOICE_DIALOG = 100;
    private static final int SYNC_CONFLICT_DIALOG = 101;
    Menu options_menu;
    TodoApplication m_app;
    ActiveFilter mFilter;
    TaskAdapter m_adapter;
    private BroadcastReceiver m_broadcastReceiver;
    private ActionMode actionMode;
    // Drawer vars
    private ListView m_leftDrawerList;
    private ListView m_rightDrawerList;
    private DrawerLayout m_drawerLayout;
    private ActionBarDrawerToggle m_drawerToggle;
    
    private void showHelp() {
        Intent i = new Intent(this, HelpScreen.class);
        startActivity(i);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
	super.onPostCreate(savedInstanceState);
	// Sync the toggle state after onRestoreInstanceState has occurred.
	if (m_drawerToggle!=null) {
	    m_drawerToggle.syncState();
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
	if (m_drawerToggle!=null && m_drawerToggle.onOptionsItemSelected(item)) {
	    return true;
	}
	// Handle your other action bar items...

	return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	if (requestCode == REQUEST_FILTER) {
	    if (resultCode == RESULT_OK) {
		setIntent(data);
		handleIntent(null);
	    }
	}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.v(TAG, "onCreate");
	m_app = (TodoApplication) getApplication();
        m_app.setActionBarStyle(getWindow());

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

	final IntentFilter intentFilter = new IntentFilter();
	intentFilter.addAction(getPackageName()+Constants.BROADCAST_ACTION_ARCHIVE);
	intentFilter.addAction(getPackageName()+Constants.BROADCAST_SYNC_CONFLICT);
	intentFilter.addAction(getPackageName()+Constants.BROADCAST_ACTION_LOGOUT);
	intentFilter.addAction(getPackageName()+Constants.BROADCAST_UPDATE_UI);
	intentFilter.addAction(getPackageName()+Constants.BROADCAST_SYNC_START);
	intentFilter.addAction(getPackageName()+Constants.BROADCAST_SYNC_DONE);

	m_broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		    if (intent.getAction().endsWith(
						    Constants.BROADCAST_ACTION_ARCHIVE)) {
			// archive
			// refresh screen to remove completed tasks
			// push to remote
			archiveTasks(null);
		    } else if (intent.getAction().endsWith(
							   Constants.BROADCAST_ACTION_LOGOUT)) {
			Log.v(TAG, "Logging out from Dropbox");
			m_app.getRemoteClientManager().getRemoteClient()
			    .deauthenticate();
			m_app.setManualMode(false);
			Intent i = new Intent(context, LoginScreen.class);
			startActivity(i);
			finish();
		    } else if (intent.getAction().endsWith(
							   Constants.BROADCAST_UPDATE_UI)) {
			handleIntent(null);
		    } else if (intent.getAction().endsWith(
							   Constants.BROADCAST_SYNC_CONFLICT)) {
			handleSyncConflict();
		    } else if (intent.getAction().endsWith(
							   Constants.BROADCAST_SYNC_START) && !m_app.isCloudLess()) {
			setProgressBarIndeterminateVisibility(true);
		    } else if (intent.getAction().endsWith(
							   Constants.BROADCAST_SYNC_DONE) && !m_app.isCloudLess()) {
			setProgressBarIndeterminateVisibility(false);
		    }
		}
	    };
	registerReceiver(m_broadcastReceiver, intentFilter);


	// Set the proper theme
	setTheme(m_app.getActiveTheme());
        if (m_app.hasLandscapeDrawers()) {
	    setContentView(R.layout.main_landscape);
        } else {
            setContentView(R.layout.main);
        }

	// Replace drawables if the theme is dark
	if (m_app.isDarkTheme()) {
	    ImageView actionBarIcon = (ImageView) findViewById(R.id.actionbar_icon);
	    if (actionBarIcon!=null) {
		actionBarIcon.setImageResource(R.drawable.labels);
	    }
	    ImageView actionBarClear = (ImageView) findViewById(R.id.actionbar_clear);
	    if (actionBarClear!=null) {
		actionBarClear.setImageResource(R.drawable.cancel);
	    }
	}
	setProgressBarIndeterminateVisibility(false);
	getTaskBag().reload();
        if(m_app.hasSyncOnResume()) {
            syncClient(false);
        }
	handleIntent(savedInstanceState);
    }

    private TaskBag getTaskBag() {
        return m_app.getTaskBag();
    }

    private void handleIntent(Bundle savedInstanceState) {
	if (!m_app.isCloudLess()) {
	    RemoteClient remoteClient = m_app.getRemoteClientManager()
		.getRemoteClient();
	    // Keep allowing use of the app in manual unauthenticated mode
	    // This will probably be removed in the future.
	    if (!remoteClient.isAuthenticated() && !m_app.isManualMode()) {
		startLogin();
		return;
	    }
	}

	mFilter = new ActiveFilter(getResources());

	m_leftDrawerList = (ListView) findViewById(R.id.left_drawer);
        m_rightDrawerList = (ListView) findViewById(R.id.right_drawer_list);

	m_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

	if (m_drawerLayout == null) {
	    // In tablet landscape mode
            ViewGroup m_container = (ViewGroup) findViewById(R.id.tablet_drawer_layout);
	} else {
	    // Not in tablet landscape mode
            ViewGroup m_container = m_drawerLayout;
	}
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
	    getActionBar().setDisplayHomeAsUpEnabled(true);
	    getActionBar().setHomeButtonEnabled(true);
	    m_drawerToggle.syncState();
	}

	// Show search or filter results
	Intent intent = getIntent();
	if (savedInstanceState != null) {
	    Log.v(TAG, "handleIntent: savedInstance state");
	    mFilter.initFromBundle(savedInstanceState);

	} else if (intent.getExtras() != null) {
	    Log.v(TAG, "handleIntent launched with filter:" + intent.getExtras().keySet());
	    mFilter.initFromIntent(intent);
	} else {
	    // Set previous filters and sort
	    Log.v(TAG, "handleIntent: from m_prefs state");
	    mFilter.initFromPrefs(TodoApplication.getPrefs());


	}

	// Initialize Adapter
	if (m_adapter == null) {
	    m_adapter = new TaskAdapter(this, R.layout.list_item,
					getLayoutInflater(), getListView());
	}
	m_adapter.setFilteredTasks(true);

	setListAdapter(this.m_adapter);

	ListView lv = getListView();
	lv.setTextFilterEnabled(true);
	lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
	lv.setMultiChoiceModeListener(new ActionBarListener());

	// If we were started with a selected task,
	// select it now and clear it from the intent
	String selectedTask = intent.getStringExtra(Constants.INTENT_SELECTED_TASK);
	if(selectedTask!=null) {
	    String[] parts = selectedTask.split(":", 2);
	    setSelectedTask(Integer.valueOf(parts[0]), parts[1]);
	    intent.removeExtra(Constants.INTENT_SELECTED_TASK);
	    setIntent(intent);
	} else {
	    // Set the adapter for the list view
	    updateDrawers();
	}

    }

    private void setSelectedTask(int index,String selectedTask) {
	Log.v(TAG, "Selected task: " + selectedTask );
	Task task = new Task(index,selectedTask);
	int position = m_adapter.getPosition(task);
	if (position!=-1) {
	    ListView lv = getListView();
	    lv.setItemChecked(position,true);
	    lv.setSelection(position);
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
	if (mFilter.hasFilter()) {
	    actionbar_clear.setVisibility(View.VISIBLE);
	} else {
	    actionbar_clear.setVisibility(View.GONE);
	}
	filterText.setText(mFilter.getTitle());
    }

    private void startLogin() {
	Intent intent = new Intent(this, LoginScreen.class);
	startActivity(intent);
	finish();
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
	unregisterReceiver(m_broadcastReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);
	mFilter.saveInBundle(outState);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }

    @Override
    protected void onStop() {
	super.onStop();
        finishActionmode();
        mFilter.saveInPrefs(TodoApplication.getPrefs());
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
	if (m_app.isCloudLess()) {
	    menu.findItem(R.id.sync).setVisible(false);
	}
	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        onItemLongClick(l,v,position,id);
    }



    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ListView l = (ListView) parent;
        l.setItemChecked(position, !l.isItemChecked(position));
        return true;
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

    private void removeTaskTags(final List<Task> tasks) {
	LinkedHashSet<String> contexts = new LinkedHashSet<String>();
	LinkedHashSet<String> projects = new LinkedHashSet<String>();
	for (Task task : tasks) {
	    for (String s : task.getLists()) {
		contexts.add("@" + s);
	    }
	    for (String s : task.getTags()) {
		projects.add("+"+s);
	    }
	}
	final ArrayList<String> items = new ArrayList<String>();
	items.addAll(contexts);
	items.addAll(projects);
	if (items.size()==0) {
	    showToast(R.string.not_tagged);
	    return;
	}
	String[] values = items.toArray(new String[items.size()]);
	Dialog tagChooser = Util.createMultiChoiceDialog(this,values,null, R.string.remove_list_or_tag,
							 null, new OnMultiChoiceDialogListener() {
								 @Override
								 public void onClick(boolean[] selected) {

								     for (int i = 0 ; i < selected.length ; i++) {
									 if (selected[i]) {
									     for (Task t : tasks) {
										 t.removeTag(items.get(i));
									     }
									 }
								     }
								     getTaskBag().store();
								     m_app.updateWidgets();
								     m_app.setNeedToPush(true);
								     // We have change the data, views should refresh
								     m_adapter.setFilteredTasks(false);
								     sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));

								 }
							     });
	tagChooser.show();
    }

    private void tagTasks(final List<Task> tasks) {
        TaskBag taskBag = getTaskBag();
	List<String> strings = new ArrayList<String>();
	for (String s: taskBag.getContexts(false)) {
	    strings.add("@"+s);
	}
	for (String s: taskBag.getProjects(false)) {
	    strings.add("+"+s);
	}
	final String[] items = strings.toArray(new String[strings.size()]);
	Dialog tagChooser = Util.createMultiChoiceDialog(this,items,null, R.string.add_list_or_tag,
							 null, new OnMultiChoiceDialogListener() {
								 @Override
								 public void onClick(boolean[] selected) {

								     for (int i = 0 ; i < selected.length ; i++) {
									 if (selected[i]) {
									     for (Task t : tasks) {
										 t.append(items[i]);
									     }
									 }
								     }
								     getTaskBag().store();
								     m_app.updateWidgets();
								     m_app.setNeedToPush(true);
								     // We have change the data, views should refresh
								     m_adapter.setFilteredTasks(false);
								     sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));

								 }
							     });
	tagChooser.show();
    }

    private void prioritizeTasks(final List<Task> tasks) {
	List<String> strings = Priority.rangeInCode(Priority.NONE, Priority.Z);
	final String[] prioArr = strings.toArray(new String[strings.size()]);

	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle(R.string.select_priority);
	builder.setSingleChoiceItems(prioArr, 0, new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, final int which) {

		    dialog.dismiss();
		    for (Task task : tasks) {
			if (task != null) {
			    task.setPriority(Priority.toPriority(prioArr[which]));
			}
		    }
		    finishActionmode();
		    getTaskBag().store();
		    m_app.updateWidgets();
		    m_app.setNeedToPush(true);
		    // We have change the data, views should refresh
		    m_adapter.setFilteredTasks(false);
		    sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
		}
	    });
	builder.show();

    }

    private void completeTasks(List<Task> tasks) {
        TaskBag taskBag = getTaskBag();
	for (Task t : tasks) {
	    if (t != null && !t.isCompleted()) {
                if (t.getRecurrencePattern()!=null) {
                    Task newTask = taskBag.addAsTask(t.withoutCreateAndCompletionDate());
                    boolean fromOriginalDate = m_app.hasRecurOriginalDates();
                    if (newTask.getDueDate()==null && newTask.getThresholdDate()==null) {
                        newTask.deferDueDate(t.getRecurrencePattern(),fromOriginalDate);
                    } else {
                        if (newTask.getDueDate()!=null) {
                            newTask.deferDueDate(t.getRecurrencePattern(),fromOriginalDate);
                        }
                        if (newTask.getThresholdDate()!=null) {
                            newTask.deferThresholdDate(t.getRecurrencePattern(),fromOriginalDate);
                        }
                    }
                }
                t.markComplete(new DateTime());
	    }
	}
        taskBag.store();
	if (m_app.isAutoArchive()) {
	    taskBag.archive(null);
	}
	m_app.updateWidgets();
	m_app.setNeedToPush(true);
	// We have change the data, views should refresh
	m_adapter.setFilteredTasks(true);
	sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
    }

    private void undoCompleteTasks(List<Task> tasks) {
	for (Task t : tasks) {
	    if (t != null && t.isCompleted()) {
		t.markIncomplete();
	    }
	}
	getTaskBag().store();
	m_app.updateWidgets();
	m_app.setNeedToPush(true);
	// We have change the data, views should refresh
	m_adapter.setFilteredTasks(true);
	sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
    }

    private void deferTasks(List<Task> tasks, final int dateType ) {
        final List<Task> tasksToDefer = tasks;
	Dialog d =  Util.createDeferDialog(this, dateType, true, new Util.InputDialogListener() {
		@Override
		public void onClick(String selected) {
		    if (selected!=null && selected.equals("pick")) {
			final DateTime today = new DateTime();
			DatePickerDialog dialog = new DatePickerDialog(Simpletask.this, new DatePickerDialog.OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                                    month++;

                                    DateTime date = ISODateTimeFormat.date().parseDateTime(year + "-" + month + "-" + day);
				    deferTasks(date, tasksToDefer, dateType);

				}
			    },
			    today.getYear(),
			    today.getMonthOfYear()-1,
			    today.getDayOfMonth());

			dialog.show();
		    } else {
			deferTasks(selected, tasksToDefer, dateType);
		    }
		}
	    });
	d.show();
    }

    private void deferTasks(DateTime selected, List<Task> tasksToDefer, int type) {
	for (Task t : tasksToDefer) {
	    if (t != null) {
                if (type==Task.DUE_DATE) {
		    t.setDueDate(selected);
                } else {
                    t.setThresholdDate(selected);
                }
	    }
	}
	m_adapter.setFilteredTasks(false);
	getTaskBag().store();
	m_app.updateWidgets();
	m_app.setNeedToPush(true);
	// We have change the data, views should refresh
	sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
    }

    private void deferTasks(String selected, List<Task> tasksToDefer, int type) {
	for (Task t : tasksToDefer) {
	    if (t != null) {
                if (type==Task.DUE_DATE) {
		    t.deferDueDate(selected, false);
                } else {
                    t.deferThresholdDate(selected,false );
                }
	    }
	}
	m_adapter.setFilteredTasks(false);
	getTaskBag().store();
	m_app.updateWidgets();
	m_app.setNeedToPush(true);
	// We have change the data, views should refresh
	sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
    }

    private void deleteTasks(final List<Task> tasks) {
	m_app.showConfirmationDialog(this, R.string.delete_task_message, new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialogInterface, int i) {
		    for (Task t : tasks) {
			if (t != null) {
			    getTaskBag().delete(t);
			}
		    }
		    m_adapter.setFilteredTasks(false);
		    getTaskBag().store();
		    m_app.updateWidgets();
		    m_app.setNeedToPush(true);
		    updateDrawers();
		    // We have change the data, views should refresh
		    sendBroadcast(new Intent(getPackageName() + Constants.BROADCAST_START_SYNC_TO_REMOTE));
		}
	    }, R.string.delete_task_title);
    }

    private void archiveTasks( final List<Task> tasksToArchive) {
	new AsyncTask<Void, Void, Boolean>() {

	    @Override
		protected Boolean doInBackground(Void... params) {
		try {
		    getTaskBag().archive(tasksToArchive);
		    return true;
		} catch (Exception e) {
		    Log.e(TAG, e.getMessage(), e);
		    return false;
		}
	    }

	    @Override
		protected void onPostExecute(Boolean result) {
		if (result) {
		    Util.showToastLong(Simpletask.this,
				       "Archived tasks");
		    m_adapter.setFilteredTasks(false);
		    m_app.updateWidgets();
		    m_app.setNeedToPush(true);
		    updateDrawers();
		    sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
		} else {
		    Util.showToastLong(Simpletask.this,
				       "Could not archive tasks");
		}
	    }
	}.execute();
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
	case R.id.sync:
	    syncClient(false);
	    break;
	case R.id.help:
	    showHelp();
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
	    if (m_app.isCloudLess()) {
		m_app.openCloudlessFile(this);
	    } else {
		m_app.showConfirmationDialog(this, R.string.dropbox_open, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			    m_app.openDropboxFile(Simpletask.this);
			}
		    }, R.string.dropbox);
	    }
	    break;
	default:
	    return super.onMenuItemSelected(featureId, item);
	}
	return true;
    }

    private void startAddTaskActivity(List<Task> tasks) {
	Log.v(TAG, "Starting addTask activity");
	ArrayList<String> payload = new ArrayList<String>();
	if (tasks!=null) {
	    for (Task t : tasks) {
		payload.add("" + t.getId() + ":" + t.inFileFormat()); 
	    }
	}
	Intent intent = new Intent(this, AddTask.class);
	intent.putExtra(Constants.EXTRA_TASK, Util.join(payload, "\n"));
	mFilter.saveInIntent(intent);
	startActivity(intent);
    }
    
    private void startPreferencesActivity() {
	Intent settingsActivity = new Intent(getBaseContext(),
					     Preferences.class);
	startActivityForResult(settingsActivity, REQUEST_PREFERENCES);
    }

    /**
     * Called when we can't sync due to a merge conflict. Prompts the user to
     * force an upload or download.
     */
    private void handleSyncConflict() {
	m_app.m_pushing = false;
	m_app.m_pulling = false;
	showDialog(SYNC_CONFLICT_DIALOG);
    }

    /**
     * Sync with remote client.
     * <p/>
     * <ul>
     * <li>Will Pull in auto mode.
     * <li>Will ask "push or pull" in manual mode.
     * </ul>
     *
     * @param force
     *            true to force pull
     */
    private void syncClient(boolean force) {
	if (isManualMode()) {
	    Log.v(TAG,
		  "Manual mode, choice forced; prompt user to ask which way to sync");
	    showDialog(SYNC_CHOICE_DIALOG);
	} else {
	    Log.i(TAG, "auto sync mode; should automatically sync; force = "
		  + force);
	    Intent i = new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_WITH_REMOTE);
	    if (force) {
		i.putExtra(Constants.EXTRA_FORCE_SYNC, true);
	    }
	    sendBroadcast(i);
	}
    }

    private boolean isManualMode() {
	return m_app.isManualMode();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
	final Dialog d;
	if (id == SYNC_CHOICE_DIALOG) {
	    Log.v(TAG, "Time to show the sync choice dialog");
	    AlertDialog.Builder upDownChoice = new AlertDialog.Builder(this);
	    upDownChoice.setTitle(R.string.sync_dialog_title);
	    upDownChoice.setMessage(R.string.sync_dialog_msg);
	    upDownChoice.setPositiveButton(R.string.sync_dialog_upload,
					   new DialogInterface.OnClickListener() {
					       @Override
					       public void onClick(DialogInterface arg0, int arg1) {
						   sendBroadcast(new Intent(
									    getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE)
								 .putExtra(Constants.EXTRA_FORCE_SYNC, true));
						   // backgroundPushToRemote();
						   showToast(getString(R.string.sync_upload_message));
						   removeDialog(SYNC_CHOICE_DIALOG);
					       }
					   });
	    upDownChoice.setNegativeButton(R.string.sync_dialog_download,
					   new DialogInterface.OnClickListener() {
					       @Override
					       public void onClick(DialogInterface arg0, int arg1) {
						   sendBroadcast(new Intent(
									    getPackageName()+Constants.BROADCAST_START_SYNC_FROM_REMOTE)
								 .putExtra(Constants.EXTRA_FORCE_SYNC, true));
						   // backgroundPullFromRemote();
						   showToast(getString(R.string.sync_download_message));
						   removeDialog(SYNC_CHOICE_DIALOG);
					       }
					   });
	    return upDownChoice.show();
	} else if (id == SYNC_CONFLICT_DIALOG) {
	    Log.v(TAG, "Time to show the sync conflict dialog");
	    AlertDialog.Builder upDownChoice = new AlertDialog.Builder(this);
	    upDownChoice.setTitle(R.string.sync_conflict_dialog_title);
	    upDownChoice.setMessage(R.string.sync_conflict_dialog_msg);
	    upDownChoice.setPositiveButton(R.string.sync_dialog_upload,
					   new DialogInterface.OnClickListener() {
					       @Override
					       public void onClick(DialogInterface arg0, int arg1) {
						   Log.v(TAG, "User selected PUSH");
						   sendBroadcast(new Intent(
									    getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE)
								 .putExtra(Constants.EXTRA_OVERWRITE, true)
								 .putExtra(Constants.EXTRA_FORCE_SYNC, true));
						   // backgroundPushToRemote();
						   showToast(getString(R.string.sync_upload_message));
						   removeDialog(SYNC_CONFLICT_DIALOG);
					       }
					   });
	    upDownChoice.setNegativeButton(R.string.sync_dialog_download,
					   new DialogInterface.OnClickListener() {
					       @Override
					       public void onClick(DialogInterface arg0, int arg1) {
						   Log.v(TAG, "User selected PULL");
						   sendBroadcast(new Intent(
									    getPackageName()+Constants.BROADCAST_START_SYNC_FROM_REMOTE)
								 .putExtra(Constants.EXTRA_FORCE_SYNC, true));
						   // backgroundPullFromRemote();
						   showToast(getString(R.string.sync_download_message));
						   removeDialog(SYNC_CONFLICT_DIALOG);
					       }
					   });
	    return upDownChoice.show();
	} else {
	    return null;
	}
    }

    /**
     * Handle clear filter click *
     */
    public void onClearClick(View v) {
	// Collapse the actionview if we are searching
	Intent intent = getIntent();
	if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	    options_menu.findItem(R.id.search).collapseActionView();
	}
	clearFilter();
	m_adapter.setFilteredTasks(false);
    }

    public ArrayList<ActiveFilter> getSavedFilter() {
        ArrayList<ActiveFilter> saved_filters = new ArrayList<ActiveFilter>();
        SharedPreferences saved_filter_ids = getSharedPreferences("filters",MODE_PRIVATE);
        Set<String> filterIds = saved_filter_ids.getStringSet("ids", new HashSet<String>());
        for (String id : filterIds ) {
            SharedPreferences filter_pref = getSharedPreferences(id,MODE_PRIVATE);
            ActiveFilter filter = new ActiveFilter(getResources());
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
		    String value = input.getText().toString();
		    if (value.equals("")) {
			Util.showToastShort(getApplicationContext(), R.string.filter_name_empty);
		    } else {
			SharedPreferences saved_filters = getSharedPreferences("filters", MODE_PRIVATE);
			int newId  = saved_filters.getInt("max_id", 1)+1;
			Set<String> filters = saved_filters.getStringSet("ids", new HashSet<String>());
			filters.add("filter_" + newId);
			saved_filters.edit()
                            .putStringSet("ids", filters )
                            .putInt("max_id", newId)
                            .commit();
			SharedPreferences test_filter_prefs = getSharedPreferences("filter_" + newId,MODE_PRIVATE);
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
    protected void onNewIntent(Intent intent) {
	super.onNewIntent(intent);
	if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	    Intent currentIntent = getIntent();
	    currentIntent.putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY));
	    setIntent(currentIntent);
	    handleIntent(null);
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
	setIntent(intent);
        finishActionmode();
        updateDrawers();
    }

    private void updateDrawers () {
        updateLeftDrawer();
        updateRightDrawer();
    }

    private void updateRightDrawer() {
        ArrayList<String> names = new ArrayList<String>();
        final ArrayList<ActiveFilter> filters = getSavedFilter();
        Collections.sort(filters, new Comparator<ActiveFilter>(){
		public int compare(ActiveFilter f1, ActiveFilter f2) {
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
		    m_adapter.setFilteredTasks(false);
		    if (m_drawerLayout!=null) {
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
		    PopupMenu popupMenu = new PopupMenu(Simpletask.this,view);
		    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			    @Override
			    public boolean onMenuItemClick(MenuItem item) {
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

    public void createFilterShortcut(ActiveFilter filter) {
        final Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        Intent target = new Intent(Constants.INTENT_START_FILTER);
        filter.saveInIntent(target);

        target.putExtra("name", filter.getName());

        // Setup target intent for shortcut
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target);

        // Set shortcut icon
        Intent.ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(this, R.drawable.icon);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, filter.getName());
        sendBroadcast(shortcut);
    }

    private void deleteSavedFilter(String prefsName) {
        SharedPreferences saved_filters = getSharedPreferences("filters", MODE_PRIVATE);
        HashSet<String> ids = new HashSet<String>();
        ids.addAll(saved_filters.getStringSet("ids", new HashSet<String>()));
        ids.remove(prefsName);
        saved_filters.edit().putStringSet("ids", ids).commit();
        SharedPreferences filter_prefs = getSharedPreferences(prefsName,MODE_PRIVATE);
        filter_prefs.edit().clear().commit();
        File prefs_path = new File (this.getFilesDir(), "../shared_prefs");
        File prefs_xml = new File (prefs_path,  prefsName+".xml");
        prefs_xml.delete();
        updateRightDrawer();
    }

    private void updateSavedFilter(String prefsName) {
        SharedPreferences filter_pref = getSharedPreferences(prefsName,MODE_PRIVATE);
        ActiveFilter old_filter = new ActiveFilter(getResources());
        old_filter.initFromPrefs(filter_pref);
        String filterName = old_filter.getName();
        mFilter.setName(filterName);
        mFilter.saveInPrefs(filter_pref);
        updateRightDrawer();
    }

    private void renameSavedFilter(String prefsName) {
        final SharedPreferences filter_pref = getSharedPreferences(prefsName,MODE_PRIVATE);
        ActiveFilter old_filter = new ActiveFilter(getResources());
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
		    String value = input.getText().toString();
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
        TaskBag taskBag = getTaskBag();
        DrawerAdapter drawerAdapter = new DrawerAdapter(getLayoutInflater(),
							taskBag.getDecoratedContexts(true), taskBag.getDecoratedProjects(true));

	m_leftDrawerList.setAdapter(drawerAdapter);
        m_leftDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
	m_leftDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        for (String context : mFilter.getContexts()) {
            int position = drawerAdapter.getIndexOf("@"+ context);
            if (position!=-1) {
                m_leftDrawerList.setItemChecked(position, true);
            }
        }

        for (String project : mFilter.getProjects()) {
            int position = drawerAdapter.getIndexOf("+"+ project);
            if (position!=-1) {
                m_leftDrawerList.setItemChecked(position, true);
            }
        }
        m_leftDrawerList.setItemChecked(drawerAdapter.getContextHeaderPosition(), mFilter.getContextsNot());
        m_leftDrawerList.setItemChecked(drawerAdapter.getProjectsHeaderPosition(), mFilter.getProjectsNot());
    }

    public void storeKeys(String accessTokenKey, String accessTokenSecret) {
	Editor editor = m_app.getPrefs().edit();
	editor.putString(Constants.PREF_ACCESSTOKEN_KEY, accessTokenKey);
	editor.putString(Constants.PREF_ACCESSTOKEN_SECRET, accessTokenSecret);
	editor.commit();
    }

    public void showToast(String string) {
	Util.showToastLong(this, string);
    }

    public void showToast(int id) {
	Util.showToastLong(this, getString(id));
    }

    public void startFilterActivity() {
	Intent i = new Intent(this, FilterActivity.class);
	mFilter.saveInIntent(i);
	i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
	startActivityForResult(i, REQUEST_FILTER);
    }

    private static class ViewHolder {
	private TextView tasktext;
	private TextView taskage;
	private TextView taskdue;
	private TextView taskthreshold;
        private CheckBox cbCompleted;
    }

    public class TaskAdapter extends BaseAdapter implements ListAdapter,
							    Filterable {
	public class VisibleLine {
	    private Task task = null;
	    private String title = null;
	    private boolean header = false;

	    public VisibleLine(String title) {
		this.title = title;
		this.header = true;
	    }

	    public VisibleLine(Task task) {
		this.task = task;
		this.header = false;
	    }

	    @Override
	    public boolean equals(Object obj) {
		if (this == obj)
		    return true;
		if (obj == null)
		    return false;
		if (getClass() != obj.getClass())
		    return false;

		VisibleLine other = (VisibleLine) obj;
		if (other.header != this.header)
		    return false;
		if (other.header) {
		    return title.equals(other.title);
		} else {
		    return this.task.equals(other.task);
		}
	    }

	    @Override
	    public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((task == null) ? 0 : task.hashCode());
		return result;
	    }
	}
	ArrayList<VisibleLine> visibleLines = new ArrayList<VisibleLine>();
	Set<DataSetObserver> obs = new HashSet<DataSetObserver>();
	private LayoutInflater m_inflater;

	public TaskAdapter(Context context, int textViewResourceId,
			   LayoutInflater inflater, ListView view) {
	    this.m_inflater = inflater;
	}

	void setFilteredTasks(boolean reload) {
	    ArrayList<Task> visibleTasks = new ArrayList<Task>();
	    Log.v(TAG, "setFilteredTasks called, reload: " + reload);
	    if (reload) {
		getTaskBag().reload();
		// Update lists in side drawer
		// Set the adapter for the list view
		updateDrawers();
	    }

	    visibleTasks.clear();
	    visibleLines.clear();
	    visibleTasks.addAll(mFilter.apply(getTaskBag().getTasks()));
	    ArrayList<String> sorts = mFilter.getSort();
	    Collections.sort(visibleTasks, MultiComparator.create(sorts));
	    String header = "";
	    String newHeader = "";
	    int index = 0;
	    int position = 0;
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
		    int last = visibleLines.size()-1;
		    if (last!=-1 && visibleLines.get(last).header && !m_app.showEmptyLists()) {
			visibleLines.set(last, headerLine);
		    } else {
			visibleLines.add(headerLine);
		    }
		    header = newHeader;
		}
                       
		if (!t.isHidden() || m_app.showHidden()) {
		    // enduring tasks should not be displayed
		    VisibleLine taskLine = new VisibleLine(t);
		    visibleLines.add(taskLine);
		}
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

	/*
	** Get the adapter position for task
	*/
	public int getPosition (Task task) {
	    VisibleLine line = new VisibleLine(task);
	    int index = visibleLines.indexOf(line);
	    return index;
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
	    obs.remove(observer);
	}

	@Override
	public int getCount() {
	    return visibleLines.size();
	}

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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    VisibleLine line = visibleLines.get(position);
	    if (line.header) {
		convertView = m_inflater.inflate(R.layout.list_header, null);
		TextView t = (TextView) convertView
		    .findViewById(R.id.list_header_title);
		t.setText(line.title);

	    } else {
		final ViewHolder holder;
		if (convertView == null) {
		    convertView = m_inflater.inflate(R.layout.list_item, null);
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
		if (task != null) {
		    SpannableString ss = new SpannableString(
							     task.inScreenFormat(mFilter));

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




		    String relAge = task.getRelativeAge();
		    SpannableString relDue = task.getRelativeDueDate(res, m_app.hasColorDueDates());
		    String relThres = task.getRelativeThresholdDate();
		    boolean anyDateShown = false;
		    if (!Strings.isEmptyOrNull(relAge)) {
			holder.taskage.setText(relAge);
			anyDateShown = true;
		    } else {
			holder.taskage.setText("");
		    }
		    if (relDue!=null) {
			anyDateShown = true;
			holder.taskdue.setText(relDue);
		    } else {
			holder.taskdue.setText("");
		    }
		    if (!Strings.isEmptyOrNull(relThres)) {
			anyDateShown = true;
			holder.taskthreshold.setText(relThres);
		    } else {
			holder.taskthreshold.setText("");
		    }
		    LinearLayout datesBar = (LinearLayout)convertView
			.findViewById(R.id.datebar);
		    if (!anyDateShown || task.isCompleted()) {
			datesBar.setVisibility(View.GONE);
			holder.tasktext.setPadding(
						   holder.tasktext.getPaddingLeft(),
						   holder.tasktext.getPaddingTop(),
						   holder.tasktext.getPaddingRight(), 4);
		    } else {
			datesBar.setVisibility(View.VISIBLE);
			holder.tasktext.setPadding(
						   holder.tasktext.getPaddingLeft(),
						   holder.tasktext.getPaddingTop(),
						   holder.tasktext.getPaddingRight(), 0);
		    }
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

	@Override
	public Filter getFilter() {
	    return new Filter() {
		@Override
		protected FilterResults performFiltering(
							 CharSequence charSequence) {
		    mFilter.setSearch(charSequence.toString());
		    //Log.v(TAG, "performFiltering: " + charSequence.toString());
		    return null;
		}

		@Override
		protected void publishResults(CharSequence charSequence,
					      FilterResults filterResults) {
		    setFilteredTasks(false);
		}
	    };
	}
    }

    class ActionBarListener implements AbsListView.MultiChoiceModeListener {

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position,
					      long id, boolean checked) {
	    getListView().invalidateViews();
	    mode.invalidate();
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.task_context, menu);
	    actionMode = mode;
        if (!m_app.showCompleteCheckbox()) {
            menu.findItem(R.id.complete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.uncomplete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
	    return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
	    List<Task> checkedTasks = getCheckedTasks();
	    int numSelected = checkedTasks.size();
	    String title = "" + numSelected;
	    mode.setTitle(title);
	    menu.removeGroup(Menu.CATEGORY_SECONDARY);
	    for (Task t : getCheckedTasks()) {
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
	    }
	    return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
	    List<Task> checkedTasks = getCheckedTasks();
	    int menuid = item.getItemId();
	    Intent intent;
	    switch (menuid) {
	    case R.id.complete:
		completeTasks(checkedTasks);
		break;
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
		intent = new Intent(android.content.Intent.ACTION_SEND)
		    .setType("text/plain")
		    .putExtra(android.content.Intent.EXTRA_SUBJECT,
			      getString(R.string.share_title))
		    .putExtra(android.content.Intent.EXTRA_TEXT, shareText);
		startActivity(Intent.createChooser(intent, "Share"));
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
				calDate.getTimeInMillis()+60*60*1000);
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
				new String[] { item.getTitle().toString() });
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
	    if (m_drawerLayout!=null) {
		m_drawerLayout.closeDrawers();
	    }
	    updateDrawers();
        }
    }

    private void updateLists(final List<Task> checkedTasks) {
        final ArrayList<String> contexts = new ArrayList<String>();
        Set<String> selectedContexts = new HashSet<String>();

        TaskBag taskbag = getTaskBag();
        contexts.addAll(taskbag.getContexts(false));
        for (Task t: checkedTasks) {
            selectedContexts.addAll(t.getLists());
        }


        View view = getLayoutInflater().inflate(R.layout.tag_dialog, null);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_multiple_choice,
					       contexts.toArray(new String[contexts.size()] )));
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        for (String context : selectedContexts) {
            int position = contexts.indexOf(context);
            if (position!=-1) {
                lv.setItemChecked(position, true);
            }
        }
        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(this);

        final EditText ed = (EditText) view.findViewById(R.id.editText);
        m_app.setEditTextHint(ed,R.string.new_list_name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
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
		    m_adapter.setFilteredTasks(false);
		    getTaskBag().store();
		    m_app.updateWidgets();
		    m_app.setNeedToPush(true);
		    updateDrawers();
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

    private void updateTags(final List<Task> checkedTasks) {
        final ArrayList<String> projects = new ArrayList<String>();
        Set<String> selectedProjects= new HashSet<String>();

        TaskBag taskbag = getTaskBag();
        projects.addAll(taskbag.getProjects(false));
        for (Task t: checkedTasks) {
            selectedProjects.addAll(t.getTags());
        }


        View view = getLayoutInflater().inflate(R.layout.tag_dialog, null);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_multiple_choice,
					       projects.toArray(new String[projects.size()] )));
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        for (String context : selectedProjects) {
            int position = projects.indexOf(context);
            if (position!=-1) {
                lv.setItemChecked(position, true);
            }
        }

        final EditText ed = (EditText) view.findViewById(R.id.editText);
        m_app.setEditTextHint(ed,R.string.new_list_name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
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
			    t.addTag(item);
			}
		    }
		    for (String item : uncheckedItesm) {
			for (Task t : checkedTasks) {
			    t.removeTag("+"+item);
			}
		    }
		    finishActionmode();
		    m_adapter.setFilteredTasks(false);
		    getTaskBag().store();
		    m_app.updateWidgets();
		    m_app.setNeedToPush(true);
		    updateDrawers();
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
	    //view.setSelected(true);
	    ArrayList<String> tags;
	    ListView lv = (ListView) parent;
	    DrawerAdapter adapter = (DrawerAdapter) lv.getAdapter();
	    if (adapter.getProjectsHeaderPosition()==position) {
		mFilter.setProjectsNot(!mFilter.getProjectsNot());
	    }
	    if (adapter.getContextHeaderPosition()==position) {
		mFilter.setContextsNot(!mFilter.getContextsNot());
	    } else {
		tags = Util.getCheckedItems(lv,true);
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
	    setIntent(intent);
	    adapter.notifyDataSetChanged();
	    finishActionmode();
	    m_adapter.setFilteredTasks(false);
	    //m_drawerLayout.closeDrawer(Gravity.LEFT);
	}
    }

    private void finishActionmode() {
        if (actionMode!=null) {
            actionMode.finish();
        }
    }
}
