/**
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @author Mark Janssen
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
 */

package nl.mpcjanssen.simpletask;

import nl.mpcjanssen.simpletask.remote.RemoteClient;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.task.ByContextFilter;
import nl.mpcjanssen.simpletask.task.ByPriorityFilter;
import nl.mpcjanssen.simpletask.task.ByProjectFilter;
import nl.mpcjanssen.simpletask.task.ByTextFilter;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.task.TaskFilter;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletask.util.Util.OnMultiChoiceDialogListener;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Thread.sleep;


public class Simpletask extends ListActivity  {

	final static String TAG = Simpletask.class.getSimpleName();
	private final static int REQUEST_FILTER = 1;
	private final static int REQUEST_PREFERENCES = 2;

	private final static int DRAWER_CONTEXT = 1;
	private final static int DRAWER_PROJECT = 2;


	Menu options_menu;
	TodoApplication m_app;

	ActiveFilter mFilter;

	TaskAdapter m_adapter;

	private BroadcastReceiver m_broadcastReceiver;

	private static final int SYNC_CHOICE_DIALOG = 100;
	private static final int SYNC_CONFLICT_DIALOG = 101;

	private ActionMode actionMode;

	// Drawer vars
	private ListView m_contextDrawerList;
	private ListView m_projectDrawerList;
	private DrawerLayout m_drawerLayout;
	private ViewGroup m_container;
	private ActionBarDrawerToggle m_drawerToggle;

	private ArrayList<String> m_contextsList;
	private ArrayList<String> m_projectsList;
	private SearchManager searchManager;


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
					archiveTasks();
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

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// Set the proper theme
		setTheme(m_app.getActiveTheme());

		setContentView(R.layout.main);

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

		m_contextDrawerList = (ListView) findViewById(R.id.left_tags_list);
		m_projectDrawerList = (ListView) findViewById(R.id.right_tags_list);

		m_drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		if (m_drawerLayout == null) {
			// In tablet landscape mode
			m_container = (ViewGroup) findViewById(R.id.tablet_drawer_layout);
		} else {
			// Not in tablet landscape mode
			m_container = m_drawerLayout;
		}
		// Set the list's click listener
		m_contextDrawerList.setOnItemClickListener(new DrawerItemClickListener(DRAWER_CONTEXT));
		m_projectDrawerList.setOnItemClickListener(new DrawerItemClickListener(DRAWER_PROJECT));

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
            showDrawerHeaders(false);

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
			updateDrawerList();
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
		Log.v(TAG, "onRestart: " + getIntent().getExtras());
        if(m_app.hasSyncOnResume()) {
            syncClient(false);
        }
		handleIntent(null);

	}

	@Override
	protected void onStop() {
		super.onStop();
		if (actionMode != null) {
			actionMode.finish();
		}
		mFilter.saveInPrefs(TodoApplication.getPrefs());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
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
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// toggle selected state
		l.setItemChecked(position, !l.isItemChecked(position));
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
			for (String s : task.getContexts()) {
				contexts.add("@" + s);
			}
			for (String s : task.getProjects()) {
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
                    Task newTask = new Task(0,t.getOriginalText());
                    newTask.deferDueDate(t.getRecurrencePattern());
                    taskBag.addAsTask(newTask.inFileFormat());
                }
                t.markComplete(new Date());
			}
		}
		if (m_app.isAutoArchive()) {
			taskBag.archive();
		}
		taskBag.store();
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
							DatePickerDialog dialog = new DatePickerDialog(Simpletask.this, new DatePickerDialog.OnDateSetListener() {
								@Override
								public void onDateSet(DatePicker datePicker, int year, int month, int day) {
									Calendar cal = Calendar.getInstance();
									cal.set(year, month, day);
									deferTasks(cal.getTime(), tasksToDefer, dateType);

								}
							},
							Calendar.getInstance().get(Calendar.YEAR),
							Calendar.getInstance().get(Calendar.MONTH),
							Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

							dialog.show();
						} else {
							deferTasks(selected, tasksToDefer, dateType);
						}
					}
				});
		d.show();
	}

	private void deferTasks(Date selected, List<Task> tasksToDefer, int type) {
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
				    t.deferDueDate(selected);
                } else {
                    t.deferThresholdDate(selected);
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
		Util.showDeleteConfirmationDialog(this, new DialogInterface.OnClickListener() {
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
				// We have change the data, views should refresh
				sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
			}
		});
	}

	private void archiveTasks() {
		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					getTaskBag().archive();
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
							"Archived completed tasks");
					sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
					sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_UPDATE_UI));
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
				startFilterActivity(false);
				break;
			case R.id.sort:
				startFilterActivity(true);
				break;
			case R.id.share:
				shareTodoList();
				break;
			case R.id.sync:
				syncClient(false);
				break;
			case R.id.archive:
				Util.showConfirmationDialog(this, R.string.delete_task_message, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						archiveTasks();
					}
				}, R.string.archive_task_title);
                break;
            case R.id.open_file:
               if (m_app.isCloudLess()) {
                   m_app.openCloudlessFile(this);
               } else {
                   Util.showConfirmationDialog(this, R.string.dropbox_open, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           m_app.openDropboxFile(Simpletask.this);
                       }
                   });
               }
                break;
            default:
				return super.onMenuItemSelected(featureId, item);
		}
		return true;
	}


	private void startAddTaskActivity(Task task) {
		Log.v(TAG, "Starting addTask activity");
		Intent intent = new Intent(this, AddTask.class);
		intent.putExtra(Constants.EXTRA_TASK, task);
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

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
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
		if (actionMode!=null) {
			actionMode.finish();
		}
		updateDrawerList();
	}

	public class TaskAdapter extends BaseAdapter implements ListAdapter,
	       Filterable {

		       private LayoutInflater m_inflater;
		       ArrayList<Task> visibleTasks = new ArrayList<Task>();
		       Set<DataSetObserver> obs = new HashSet<DataSetObserver>();
		       SparseArray<String> headerTitles = new SparseArray<String>();
		       SparseArray<Integer> positionToIndex = new SparseArray<Integer>();
		       SparseArray<Integer> indexToPosition = new SparseArray<Integer>();
		       int size = 0;

		       public TaskAdapter(Context context, int textViewResourceId,
				       LayoutInflater inflater, ListView view) {
			       this.m_inflater = inflater;
		       }

		       void setFilteredTasks(boolean reload) {
			       Log.v(TAG, "setFilteredTasks called, reload: " + reload);
			       if (reload) {
				       getTaskBag().reload();
				       // Update lists in side drawer
				       // Set the adapter for the list view
				       updateDrawerList();
			       }

			       visibleTasks.clear();
			       visibleTasks.addAll(mFilter.apply(getTaskBag().getTasks()));
			       ArrayList<String> sorts = mFilter.getSort();
			       Collections.sort(visibleTasks, MultiComparator.create(sorts));
			       positionToIndex.clear();
			       indexToPosition.clear();
			       headerTitles.clear();
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
					       header = newHeader;
					       // Log.v(TAG, "Start of header: " + header +
					       // " at position: " + position);
					       headerTitles.put(position, header);
					       positionToIndex.put(position, null);
					       position++;
				       }

				       positionToIndex.put(position, index);
				       indexToPosition.put(index, position);
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

		       /*
			** Get the adapter position for task
			*/
		       public int getPosition (Task task) {
			       int index = visibleTasks.indexOf(task);
			       if  (index==-1 || indexToPosition.indexOfKey(index)==-1) {
				       return index;
			       }
			       return indexToPosition.valueAt(indexToPosition.indexOfKey(index));
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
			       return true; // To change body of implemented methods use File |
			       // Settings | File Templates.
		       }

		       @Override
		       public View getView(int position, View convertView, ViewGroup parent) {
			       if (headerTitles.get(position,null) != null) {
				       convertView = m_inflater.inflate(R.layout.list_header, null);
				       TextView t = (TextView) convertView
					       .findViewById(R.id.list_header_title);
				       t.setText(headerTitles.get(position));

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
					       convertView.setTag(holder);
				       } else {
					       holder = (ViewHolder) convertView.getTag();
				       }
				       Task task;
				       task = getItem(position);

				       if (task != null) {
					       SpannableString ss = new SpannableString(
							       task.datelessScreenFormat());

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
							       prioColor = holder.tasktext.getCurrentTextColor();
					       }
					       Util.setColor(ss, prioColor, task.getPriority()
							       .inFileFormat());
					       holder.tasktext.setText(ss);

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
			       if (headerTitles.get(position,null) != null) {
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

	private void updateDrawerList() {
        TaskBag taskBag = getTaskBag();
		m_contextsList = taskBag.getContexts(true);
		m_contextDrawerList.setAdapter(new ArrayAdapter<String>(this,
					R.layout.selection_drawer_list_item, m_contextsList));
		m_contextDrawerList.setOnItemClickListener(new DrawerItemClickListener(DRAWER_CONTEXT));
		m_projectsList = taskBag.getProjects(true);
		m_projectDrawerList.setAdapter(new ArrayAdapter<String>(this,
					R.layout.selection_drawer_list_item, m_projectsList));
		m_projectDrawerList.setOnItemClickListener(new DrawerItemClickListener(DRAWER_PROJECT));
		showDrawerHeaders(false);
		for (String context : mFilter.getContexts()) {
			int position = m_contextsList.indexOf(context);
			if (position!=-1) {
				m_contextDrawerList.setItemChecked(position,true);
			}
		}

		CheckedTextView not = (CheckedTextView)m_container.findViewById(R.id.left_drawer_not);
		not.setChecked(mFilter.getContextsNot());
		not.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				CheckedTextView cb = (CheckedTextView) view;
				cb.setChecked(!cb.isChecked());
				boolean state = cb.isChecked();
				mFilter.setContextsNot(state);
				Intent intent = getIntent();
				mFilter.saveInIntent(intent);
				setIntent(intent);
				m_adapter.setFilteredTasks(false);
			}
		});

        CheckedTextView future = (CheckedTextView)m_container.findViewById(R.id.show_future);
        future.setChecked(!mFilter.getHideFuture());
        future.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckedTextView cb = (CheckedTextView) view;
                cb.setChecked(!cb.isChecked());
                boolean state = cb.isChecked();
                mFilter.setHideFuture(!state);
                Intent intent = getIntent();
                mFilter.saveInIntent(intent);
                setIntent(intent);
                m_adapter.setFilteredTasks(false);
            }
        });

        CheckedTextView completed = (CheckedTextView)m_container.findViewById(R.id.show_completed);
        completed.setChecked(!mFilter.getHideCompleted());
        completed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckedTextView cb = (CheckedTextView) view;
                cb.setChecked(!cb.isChecked());
                boolean state = cb.isChecked();
                mFilter.setHideCompleted(!state);
                Intent intent = getIntent();
                mFilter.saveInIntent(intent);
                setIntent(intent);
                m_adapter.setFilteredTasks(false);
            }
        });

		for (String project : mFilter.getProjects()) {
			int position = m_projectsList.indexOf(project);
			if (position!=-1) {
				m_projectDrawerList.setItemChecked(position,true);
			}
		}
		not = (CheckedTextView)m_container.findViewById(R.id.right_drawer_not);
		not.setChecked(mFilter.getProjectsNot());
		not.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				CheckedTextView cb = (CheckedTextView) view;
				cb.setChecked(!cb.isChecked());
				boolean state = cb.isChecked();
				mFilter.setProjectsNot(state);
				Intent intent = getIntent();
				mFilter.saveInIntent(intent);
				setIntent(intent);
				m_adapter.setFilteredTasks(false);
			}
		});
		if (!m_app.drawersExplained() && m_contextsList.size() > 1 ) {
			m_app.setDrawersExplained();
			AsyncTask<Void,Integer,Void> drawerDemo = new AsyncTask<Void,Integer,Void>() {

				@Override
				protected Void doInBackground(Void... objects) {
					try {
						publishProgress(0);
						sleep(1500);
						publishProgress(1);
						sleep(500);
						publishProgress(2);
						sleep(1500);
						publishProgress(3);
					} catch (Exception e) {
						Log.e(TAG,""+e);
					}
					return null;
				}

				protected void onProgressUpdate(Integer... progressArray) {
					int progress = progressArray[0];
					if (m_drawerLayout == null) {
						return;
					}
					switch (progress) {
						case 0:
							m_drawerLayout.openDrawer(Gravity.LEFT);
							break;
						case 1:
							m_drawerLayout.closeDrawer(Gravity.LEFT);
							break;
						case 2:
							m_drawerLayout.openDrawer(Gravity.RIGHT);
							break;
						case 3:
							m_drawerLayout.closeDrawer(Gravity.RIGHT);
							break;
					}
				}
			};
			drawerDemo.execute(null,null,null);
		}
	}

	private void updateDrawerListForSelection(final List<Task> checkedTasks) {
        TaskBag taskBag = getTaskBag();
		LinkedHashSet<String> selectedContexts = new LinkedHashSet<String>();
		LinkedHashSet<String> selectedProjects = new LinkedHashSet<String>();
		for (Task t: checkedTasks) {
			selectedContexts.addAll(t.getContexts());
		}
		for (Task t: checkedTasks) {
			selectedProjects.addAll(t.getProjects());
		}
		m_contextsList = taskBag.getContexts(false);
		m_projectsList = taskBag.getProjects(false);
		m_contextDrawerList.setAdapter(new ArrayAdapter<String>(this,
					R.layout.selection_drawer_list_item, m_contextsList));
		m_contextDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		m_contextDrawerList.setOnItemClickListener(null);
		for (String context : selectedContexts) {
			int position = m_contextsList.indexOf(context);
			if (position!=-1) {
				m_contextDrawerList.setItemChecked(position, true);
			}
		}


		m_projectDrawerList.setAdapter(new ArrayAdapter<String>(this,
					R.layout.selection_drawer_list_item, m_projectsList));
		m_projectDrawerList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		m_projectDrawerList.setOnItemClickListener(null);
		for (String context : selectedProjects) {
			int position = m_projectsList.indexOf(context);
			if (position!=-1) {
				m_projectDrawerList.setItemChecked(position, true);
			}
		}
		showDrawerHeaders(true);
		Button applyButton = (Button)m_container.findViewById(R.id.left_apply_button);
		applyButton.setVisibility(View.VISIBLE);
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				EditText ed = (EditText) m_container.findViewById(R.id.new_list_name);
				if (!Strings.isEmptyOrNull(ed.getText().toString())) {
					String newTag = ed.getText().toString();
					if (!Task.validTag(newTag)) {
						Util.showToastShort(view.getContext(),getString(R.string.invalid_context) + ": " + newTag );
						return;
					}
					for (Task t: checkedTasks) {
						t.addList(ed.getText().toString());
					}
				}
				ListView lv = (ListView) m_container.findViewById(R.id.left_tags_list);
				SparseBooleanArray checks = lv.getCheckedItemPositions();
				for (int i = 0 ; i < checks.size() ; i++) {
					String listName = (String)lv.getAdapter().getItem(checks.keyAt(i));
					if (checks.valueAt(i)) {
						for (Task t: checkedTasks) {
							t.addList(listName);
						}
					} else {
						for (Task t: checkedTasks) {
							t.removeTag("@" + listName);
						}
					}
				}
				ed.clearFocus();
				ed.setText("");
				getTaskBag().store();
				m_adapter.setFilteredTasks(false);
				m_app.updateWidgets();
				m_app.setNeedToPush(true);
				sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
				actionMode.finish();
				if (m_drawerLayout!=null) {
					m_drawerLayout.closeDrawers();
				}
			}
		});
		Button rightApplyButton = (Button)m_container.findViewById(R.id.right_apply_button);
		rightApplyButton.setVisibility(View.VISIBLE);
		rightApplyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				EditText ed = (EditText) m_container.findViewById(R.id.new_tag_name);
				if (!Strings.isEmptyOrNull(ed.getText().toString())) {
					String newTag = ed.getText().toString();
					if (!Task.validTag(newTag)) {
						Util.showToastShort(view.getContext(),getString(R.string.invalid_project) + ": " + newTag );
						return;
					}
					for (Task t: checkedTasks) {
						t.addTag(ed.getText().toString());
					}
				}
				ListView lv = (ListView) m_container.findViewById(R.id.right_tags_list);
				SparseBooleanArray checks = lv.getCheckedItemPositions();
				for (int i = 0 ; i < checks.size() ; i++) {
					String listName = (String)lv.getAdapter().getItem(checks.keyAt(i));
					if (checks.valueAt(i)) {
						for (Task t: checkedTasks) {
							t.addTag(listName);
						}
					} else {
						for (Task t: checkedTasks) {
							t.removeTag("+" + listName);
						}
					}
				}
				getTaskBag().store();
				m_adapter.setFilteredTasks(false);
				m_app.updateWidgets();
				m_app.setNeedToPush(true);
				sendBroadcast(new Intent(getPackageName()+Constants.BROADCAST_START_SYNC_TO_REMOTE));
				actionMode.finish();
				if (m_drawerLayout!=null) {
					m_drawerLayout.closeDrawers();
				}
				ed.setText("");
				ed.clearFocus();
			}
		});
	}

	private void showDrawerHeaders(boolean show) {
		if (m_container==null) {
			return;
		}
		if (show) {
			m_container.findViewById(R.id.left_drawer_header).setVisibility(View.VISIBLE);
			m_container.findViewById(R.id.right_drawer_header).setVisibility(View.VISIBLE);
			m_container.findViewById(R.id.right_drawer_inverted).setVisibility(View.GONE);
            m_container.findViewById(R.id.left_drawer_showing).setVisibility(View.GONE);
			m_container.findViewById(R.id.left_drawer_inverted).setVisibility(View.GONE);
		} else {
			m_container.findViewById(R.id.left_drawer_header).setVisibility(View.GONE);
			m_container.findViewById(R.id.right_drawer_header).setVisibility(View.GONE);
			m_container.findViewById(R.id.right_drawer_inverted).setVisibility(View.VISIBLE);
            if (m_app.hasExtendedDrawer()) {
                m_container.findViewById(R.id.left_drawer_showing).setVisibility(View.VISIBLE);
            } else {
                m_container.findViewById(R.id.left_drawer_showing).setVisibility(View.GONE);
            }
			m_container.findViewById(R.id.left_drawer_inverted).setVisibility(View.VISIBLE);
		}
	}

	private static class ViewHolder {
		private TextView tasktext;
		private TextView taskage;
		private TextView taskdue;
		private TextView taskthreshold;
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


	public void startFilterActivity(boolean openSortTab) {
		Intent i = new Intent(this, FilterActivity.class);
		mFilter.saveInIntent(i);
		i.putExtra(Constants.INTENT_OPEN_SORT_TAB, openSortTab);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivityForResult(i, REQUEST_FILTER);
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
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<Task> checkedTasks = getCheckedTasks();
			updateDrawerListForSelection(checkedTasks);
			int numSelected = checkedTasks.size();
			String title = "";
			title = title + numSelected;
			title = title + " " + getString(R.string.selected);
			mode.setTitle(title);
			if (numSelected == 1) {
				// show the edit menu item and hide the appropriate
				// complete/uncomplete item
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
				case R.id.update:
					startAddTaskActivity(checkedTasks.get(0));
					break;
				case R.id.done:
					completeTasks(checkedTasks);
					break;
				case R.id.delete:
					deleteTasks(checkedTasks);
					break;
				case R.id.defer_due:
                    deferTasks(checkedTasks, Task.DUE_DATE);
                    break;
                case R.id.defer_threshold:
                    deferTasks(checkedTasks, Task.THRESHOLD_DATE);
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
			if (m_drawerLayout!=null) {
				m_drawerLayout.closeDrawers();
			}
			updateDrawerList();
			return;
		}
	}



	private class DrawerItemClickListener implements
		AdapterView.OnItemClickListener {

			private final int type;

			public DrawerItemClickListener(int type) {
				this.type = type;
			}

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				ArrayList<String> tags;
				ListView lv = (ListView) parent;
				Intent intent = getIntent();
				tags = Util.getCheckedItems(lv,true);
				switch(type) {
					case DRAWER_CONTEXT:
						mFilter.setContexts(tags);
						break;
					case DRAWER_PROJECT:
						mFilter.setProjects(tags);
						break;
				}
				mFilter.saveInIntent(intent);
				setIntent(intent);
				m_adapter.setFilteredTasks(false);
			}
		}
}
