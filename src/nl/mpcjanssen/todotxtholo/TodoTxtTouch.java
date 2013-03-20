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

import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.DataSetObserver;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract.Events;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import nl.mpcjanssen.todotxtholo.remote.RemoteClient;
import nl.mpcjanssen.todotxtholo.sort.*;
import nl.mpcjanssen.todotxtholo.task.*;
import nl.mpcjanssen.todotxtholo.util.Strings;
import nl.mpcjanssen.todotxtholo.util.Util;
import nl.mpcjanssen.todotxtholo.util.Util.OnMultiChoiceDialogListener;

import java.net.URL;
import java.util.*;

public class TodoTxtTouch extends ListActivity implements
		OnSharedPreferenceChangeListener {

	final static String TAG = TodoTxtTouch.class.getSimpleName();

	private final static int REQUEST_PREFERENCES = 2;
	private TaskBag taskBag;
	ProgressDialog m_ProgressDialog = null;
	String m_DialogText = "";
	Boolean m_DialogActive = false;
	Menu options_menu;
	TodoApplication m_app;

	// filter variables
	private ArrayList<Priority> m_prios = new ArrayList<Priority>();
	private ArrayList<String> m_contexts = new ArrayList<String>();
	private ArrayList<String> m_projects = new ArrayList<String>();
	private boolean m_projectsNot = false;
	private String m_search;

	TaskAdapter m_adapter;

	private BroadcastReceiver m_broadcastReceiver;

	private static final int SYNC_CHOICE_DIALOG = 100;
	private static final int SYNC_CONFLICT_DIALOG = 101;

	private View mRefreshIndeterminateProgressView; // save inflated layout for
													// reference
	private MenuItem refreshItem; // reference to actionbar menu item we want to
									// swap

	private int sort = 0;

	private boolean m_priosNot;

	private boolean m_contextsNot;

	@Override
	public View onCreateView(View parent, String name, Context context,
			AttributeSet attrs) {
		// Log.v(TAG,"onCreateView");
		return super.onCreateView(parent, name, context, attrs);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		Log.v(TAG, "onContentChanged");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate");
		m_app = (TodoApplication) getApplication();

		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Constants.INTENT_ACTION_ARCHIVE);
		intentFilter.addAction(Constants.INTENT_SYNC_CONFLICT);
		intentFilter.addAction(Constants.INTENT_ACTION_LOGOUT);
		intentFilter.addAction(Constants.INTENT_UPDATE_UI);
		intentFilter.addAction(Constants.INTENT_SYNC_START);
		intentFilter.addAction(Constants.INTENT_SYNC_DONE);

		m_broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equalsIgnoreCase(
						Constants.INTENT_ACTION_ARCHIVE)) {
					// archive
					// refresh screen to remove completed tasks
					// push to remote
					archiveTasks();
				} else if (intent.getAction().equalsIgnoreCase(
						Constants.INTENT_ACTION_LOGOUT)) {
					Log.v(TAG, "Logging out from Dropbox");
					m_app.getRemoteClientManager().getRemoteClient()
							.deauthenticate();
					Intent i = new Intent(context, LoginScreen.class);
					startActivity(i);
					finish();
				} else if (intent.getAction().equalsIgnoreCase(
						Constants.INTENT_UPDATE_UI)) {
					m_adapter.setFilteredTasks(false);
				} else if (intent.getAction().equalsIgnoreCase(
						Constants.INTENT_SYNC_CONFLICT)) {
					handleSyncConflict();
				} else if (intent.getAction().equalsIgnoreCase(
						Constants.INTENT_SYNC_START)) {
					Log.v(TAG, "Start sync");
					if (mRefreshIndeterminateProgressView == null) {
						mRefreshIndeterminateProgressView = getLayoutInflater()
								.inflate(R.layout.main_progress, null);
					}
					if (refreshItem != null) {
						refreshItem
								.setActionView(mRefreshIndeterminateProgressView);
					}
				} else if (intent.getAction().equalsIgnoreCase(
						Constants.INTENT_SYNC_DONE)) {
					Log.v(TAG, "Sync done");
					if (refreshItem != null) {
						refreshItem.setActionView(null);
					}
					m_adapter.setFilteredTasks(true);
					Intent i = new Intent();
					i.setAction(Constants.INTENT_UPDATE_UI);
					sendBroadcast(i);
				}
			}
		};
		registerReceiver(m_broadcastReceiver, intentFilter);

		RemoteClient remoteClient = m_app.getRemoteClientManager()
				.getRemoteClient();
		if (!remoteClient.isAuthenticated() && !m_app.isManualMode()) {
			startLogin();
			return;
		}
		setContentView(R.layout.main);
		m_app.m_prefs.registerOnSharedPreferenceChangeListener(this);

		taskBag = m_app.getTaskBag();

		sort = m_app.m_prefs.getInt("sort", Constants.SORT_UNSORTED);

		// Show search or filter results
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			m_search = intent.getStringExtra(SearchManager.QUERY);
			Log.v(TAG, "Searched for " + m_search);
		} else if (Constants.INTENT_START_FILTER.equals(intent.getAction())) {
			Log.v(TAG, "Launched with filter:");

			// handle different versions of shortcuts
			String prios;
			String projects;
			String contexts;
			int version = intent.getIntExtra(Constants.INTENT_VERSION, 1);
			switch (version) {
			case 1:
			default:
				prios = intent
						.getStringExtra(Constants.INTENT_PRIORITIES_FILTER_v1);
				projects = intent
						.getStringExtra(Constants.INTENT_PROJECTS_FILTER_v1);
				contexts = intent
						.getStringExtra(Constants.INTENT_CONTEXTS_FILTER_v1);
				sort = intent.getIntExtra(Constants.INTENT_ACTIVE_SORT_v1,
						Constants.SORT_UNSORTED);
				m_priosNot = intent.getBooleanExtra(
						Constants.INTENT_PRIORITIES_FILTER_NOT_v1, false);
				m_projectsNot = intent.getBooleanExtra(
						Constants.INTENT_PROJECTS_FILTER_NOT_v1, false);
				m_contextsNot = intent.getBooleanExtra(
						Constants.INTENT_CONTEXTS_FILTER_NOT_v1, false);
				break;
			}
			Log.v(TAG, "\t sort:" + sort);
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
		} else if (savedInstanceState != null) {
			// Called without explicit filter try to reload last active one
			m_prios = Priority.toPriority(savedInstanceState
					.getStringArrayList("m_prios"));
			m_contexts = savedInstanceState.getStringArrayList("m_contexts");
			m_projects = savedInstanceState.getStringArrayList("m_projects");
			m_search = savedInstanceState.getString("m_search");

			sort = savedInstanceState.getInt("sort", Constants.SORT_UNSORTED);

		}
		// Initialize Adapter

		m_adapter = new TaskAdapter(this, R.layout.list_item,
				getLayoutInflater(), getListView());
		m_adapter.setFilteredTasks(true);

		// listen to the ACTION_LOGOUT intent, if heard display LoginScreen
		// and finish() current activity

		setListAdapter(this.m_adapter);

		ListView lv = getListView();
		lv.setTextFilterEnabled(false);
		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		lv.setMultiChoiceModeListener(new ActionBarListener());
		updateFilterBar();

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
				|| m_search != null) {
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
			if (m_search != null) {
				filterTitle += " " + getString(R.string.search);
			}

			actionbar_clear.setVisibility(View.VISIBLE);
			filterText.setText(filterTitle);

		} else {
			actionbar_clear.setVisibility(View.GONE);
			filterText.setText("No filter");
		}
	}

	private void startLogin() {
		Intent intent = new Intent(this, LoginScreen.class);
		startActivity(intent);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		m_app.m_prefs.unregisterOnSharedPreferenceChangeListener(this);
		unregisterReceiver(m_broadcastReceiver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		m_adapter.setFilteredTasks(false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.v(TAG, "onSharedPreferenceChanged key=" + key);
		if (Constants.PREF_ACCESSTOKEN_SECRET.equals(key)) {
			Log.i(TAG, "New access token secret. Syncing!");
			syncClient(false);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList("m_prios", Priority.inCode(m_prios));
		outState.putStringArrayList("m_contexts", m_contexts);
		outState.putStringArrayList("m_projects", m_projects);
		outState.putString("m_search", m_search);
		outState.putInt("sort", sort);
	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences.Editor editor = m_app.m_prefs.edit();
		editor.putInt("sort", sort);
		editor.commit();
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		Log.v(TAG, "Called restore instance state");
		// m_prios = Priority.toPriority(state.getStringArrayList("m_prios"));
		// m_contexts = state.getStringArrayList("m_contexts");
		// m_projects = state.getStringArrayList("m_projects");
		// m_search = state.getString("m_search");
		//
		// sort = state.getInt("sort", Constants.SORT_UNSORTED);
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

		refreshItem = menu.findItem(R.id.sync);

		this.options_menu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// start the action bar instead
		l.setItemChecked(position, true);
	}

	private Task getTaskAt(final int pos) {
		Task task = m_adapter.getItem(pos);
		return task;
	}

	private void shareTodoList() {
		String text = "";
		for (int i = 0; i < m_adapter.getCount(); i++) {
            Task task = m_adapter.getItem(i);
            if (task!=null) {
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
				.rangeInCode(Priority.NONE, Priority.E).toArray(new String[0]);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select priority");
		builder.setSingleChoiceItems(prioArr, 0, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, final int which) {

				dialog.dismiss();
				for (Task task : tasks) {
                    if (task!=null) {
					    task.setPriority(Priority.toPriority(prioArr[which]));
                    }
				}
				taskBag.store();
				m_app.setNeedToPush(true);
				// We have change the data, views should refresh
				m_adapter.setFilteredTasks(false);
				sendBroadcast(new Intent(Constants.INTENT_START_SYNC_TO_REMOTE));
			}
		});
		builder.show();

	}

	private void completeTasks(List<Task> tasks) {
		for (Task t : tasks) {
			if (t!=null && !t.isCompleted()) {
				t.markComplete(new Date());
			}
		}
		taskBag.store();
		m_app.setNeedToPush(true);
		// We have change the data, views should refresh
		m_adapter.setFilteredTasks(true);
		sendBroadcast(new Intent(Constants.INTENT_START_SYNC_TO_REMOTE));
	}

	private void undoCompleteTasks(List<Task> tasks) {
		for (Task t : tasks) {
			if (t!=null && t.isCompleted()) {
				t.markIncomplete();
			}
		}
		taskBag.store();
		m_app.setNeedToPush(true);
		// We have change the data, views should refresh
		m_adapter.setFilteredTasks(true);
		sendBroadcast(new Intent(Constants.INTENT_START_SYNC_TO_REMOTE));
	}

	private void editTask(Task task) {
		Intent intent = new Intent(this, AddTask.class);
		intent.putExtra(Constants.EXTRA_TASK, task);
		startActivity(intent);
	}

	private void deleteTasks(List<Task> tasks) {
		for (Task t : tasks) {
            if(t!=null) {
			    taskBag.delete(t);
            }
		}
		m_adapter.setFilteredTasks(false);
		taskBag.store();
		m_app.setNeedToPush(true);
		// We have change the data, views should refresh
		sendBroadcast(new Intent(Constants.INTENT_START_SYNC_TO_REMOTE));
	}

	private void archiveTasks() {
		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					taskBag.archive();
					return true;
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {
					Util.showToastLong(TodoTxtTouch.this,
							"Archived completed tasks");
					sendBroadcast(new Intent(
							Constants.INTENT_START_SYNC_TO_REMOTE));
				} else {
					Util.showToastLong(TodoTxtTouch.this,
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
			startAddTaskActivity();
			break;
		case R.id.sync:
			Log.v(TAG, "onMenuItemSelected: sync");
			syncClient(false);
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
		Log.v(TAG, "Starting addTask activity");
		Intent intent = new Intent(this, AddTask.class);
		intent.putExtra(Constants.EXTRA_PRIORITIES_SELECTED, m_prios);
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
			Intent i = new Intent(Constants.INTENT_START_SYNC_WITH_REMOTE);
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

		if (R.id.priority == id) {
			final List<Priority> pStrs = taskBag.getPriorities();
			int size = pStrs.size();
			boolean[] values = new boolean[size];
			for (Priority prio : m_prios) {
				int index = pStrs.indexOf(prio);
				if (index != -1) {
					values[index] = true;
				}
			}
			d = Util.createMultiChoiceDialog(this,
					pStrs.toArray(new String[size]), values, null, null,
					new OnMultiChoiceDialogListener() {

						@Override
						public void onClick(boolean[] selected) {
							m_prios.clear();
							for (int i = 0; i < selected.length; i++) {
								if (selected[i]) {
									m_prios.add(pStrs.get(i));
								}
							}
							removeDialog(R.id.priority);
						}
					});
			d.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					removeDialog(R.id.priority);
				}
			});
			return d;
		} else if (id == SYNC_CHOICE_DIALOG) {
			Log.v(TAG, "Time to show the sync choice dialog");
			AlertDialog.Builder upDownChoice = new AlertDialog.Builder(this);
			upDownChoice.setTitle(R.string.sync_dialog_title);
			upDownChoice.setMessage(R.string.sync_dialog_msg);
			upDownChoice.setPositiveButton(R.string.sync_dialog_upload,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							sendBroadcast(new Intent(
									Constants.INTENT_START_SYNC_TO_REMOTE)
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
									Constants.INTENT_START_SYNC_FROM_REMOTE)
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
									Constants.INTENT_START_SYNC_TO_REMOTE)
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
									Constants.INTENT_START_SYNC_FROM_REMOTE)
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
		// End current activity if it's search results
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			finish();
		} else { // otherwise just clear the filter in the current activity
			clearFilter();
		}
	}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.v(TAG, "onNewIntent: " + intent);
    }

    void clearFilter() {
		Intent intent = new Intent(Constants.INTENT_START_FILTER);
		intent.putExtra(Constants.INTENT_ACTIVE_SORT_v1, sort);
		startActivity(intent);
	}

	private MultiComparator getActiveSort() {
		List<Comparator<?>> comparators = new ArrayList<Comparator<?>>();
		if (m_app.completedLast()) {
			comparators.add(new CompletedComparator());
		}
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

	public class TaskAdapter extends BaseAdapter implements ListAdapter {

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

		void setFilteredTasks(boolean reload) {
			Log.v(TAG, "setFilteredTasks called, reload: " + reload);
			if (reload) {
				taskBag.reload();
			}
			Log.v(TAG, "setFilteredTasks called");

			AndFilter filter = new AndFilter();
			visibleTasks.clear();
			for (Task t : taskBag.getTasks()) {
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
						// Log.v(TAG, "Start of header: " + header +
						// " at position: " + position);
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
						Log.v(TAG, "Start of header: " + header
								+ " at position: " + position);
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

			}
			return convertView;
		}

		@Override
		public int getItemViewType(int position) {
			if (headerAtPostion.get(position) != null) {
				return 0;
			} else {
				return 1; // To change body of implemented methods use File |
							// Settings | File Templates.

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
			if (headerAtPostion.get(position) != null) {
				return false;
			} else {
				return true;
			}
		}

	}

	private static class ViewHolder {
		private TextView taskprio;
		private TextView tasktext;
		private TextView taskage;
	}

	public void storeKeys(String accessTokenKey, String accessTokenSecret) {
		Editor editor = m_app.m_prefs.edit();
		editor.putString(Constants.PREF_ACCESSTOKEN_KEY, accessTokenKey);
		editor.putString(Constants.PREF_ACCESSTOKEN_SECRET, accessTokenSecret);
		editor.commit();
	}

	public void showToast(String string) {
		Util.showToastLong(this, string);
	}

	public void startFilterActivity() {
		Intent i = new Intent(this, FilterActivity.class);

		i.putStringArrayListExtra(Constants.EXTRA_PRIORITIES,
				Priority.inCode(taskBag.getPriorities()));
		i.putStringArrayListExtra(Constants.EXTRA_PROJECTS,
				taskBag.getProjects());
		i.putStringArrayListExtra(Constants.EXTRA_CONTEXTS,
				taskBag.getContexts());

		i.putStringArrayListExtra(Constants.EXTRA_PRIORITIES_SELECTED,
				Priority.inCode(m_prios));
		i.putStringArrayListExtra(Constants.EXTRA_PROJECTS_SELECTED, m_projects);
		i.putStringArrayListExtra(Constants.EXTRA_CONTEXTS_SELECTED, m_contexts);
		i.putExtra(Constants.ACTIVE_SORT, sort);
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
							"More than one task was selected while hanling update menu");
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
				intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"
						+ item.getTitle().toString()));
				startActivity(intent);
				break;
			}
			// Not sure why this is explicitly needed
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
			m_adapter.setFilteredTasks(false);
			return;
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
