package nl.mpcjanssen.simpletask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import nl.mpcjanssen.simpletask.sort.AlphabeticalComparator;
import nl.mpcjanssen.simpletask.sort.ContextComparator;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.sort.PriorityComparator;
import nl.mpcjanssen.simpletask.sort.ProjectComparator;
import nl.mpcjanssen.simpletask.task.ByContextFilter;
import nl.mpcjanssen.simpletask.task.ByPriorityFilter;
import nl.mpcjanssen.simpletask.task.ByProjectFilter;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.task.TaskFilter;
import nl.mpcjanssen.simpletask.util.Util;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class AppWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new AppWidgetRemoteViewsFactory((MainApplication)getApplication(), intent);
	}

}

class AppWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	final static String TAG = AppWidgetRemoteViewsFactory.class.getSimpleName();
	private ArrayList<String> m_contexts;
	private ArrayList<String> m_projects;
	private ArrayList<Priority> m_prios;
	private boolean m_priosNot;
	private boolean m_projectsNot;
	private boolean m_contextsNot;
	private int m_sort;
	
	private Context mContext;
	private int widgetId;
	private SharedPreferences preferences;
	private TaskBag taskBag;
	ArrayList<Task> visibleTasks = new ArrayList<Task>();

	public AppWidgetRemoteViewsFactory(MainApplication application, Intent intent) {
		widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		Log.v(TAG, "Creating view for widget: " + widgetId);
		mContext = MainApplication.getAppContext();
		preferences = mContext.getSharedPreferences(""+widgetId, 0);
		m_contexts = new ArrayList<String>();
		m_contexts.addAll(preferences.getStringSet(Constants.INTENT_CONTEXTS_FILTER_v1, new HashSet<String>()));
		Log.v(TAG, "contexts: " + m_contexts);
		ArrayList<String> prio_strings = new ArrayList<String>();
		prio_strings.addAll(preferences.getStringSet(Constants.INTENT_PRIORITIES_FILTER_v1, new HashSet<String>()));
		m_prios = Priority.toPriority(prio_strings);		
		Log.v(TAG, "prios: " + m_prios);
		m_projects = new ArrayList<String>();
		m_projects.addAll(preferences.getStringSet(Constants.INTENT_PROJECTS_FILTER_v1, new HashSet<String>()));
		Log.v(TAG, "tags: " + m_projects);
		m_contextsNot = preferences.getBoolean(Constants.INTENT_CONTEXTS_FILTER_NOT_v1,false);
		m_projectsNot = preferences.getBoolean(Constants.INTENT_PROJECTS_FILTER_NOT_v1,false);
		m_priosNot = preferences.getBoolean(Constants.INTENT_PRIORITIES_FILTER_NOT_v1,false);
		m_sort = preferences.getInt(Constants.INTENT_ACTIVE_SORT_v1,Constants.SORT_UNSORTED);
		taskBag = application.getTaskBag();
		setFilteredTasks(true);
		
	}
	
    private Intent createFilterIntent() {
    	Intent target = new Intent();
        target.putExtra(Constants.INTENT_VERSION, Constants.INTENT_CURRENT_VERSION);
        target.putExtra(Constants.INTENT_CONTEXTS_FILTER_v1, Util.join(m_contexts, "\n"));
        target.putExtra(Constants.INTENT_CONTEXTS_FILTER_NOT_v1, m_contextsNot);
        target.putExtra(Constants.INTENT_PROJECTS_FILTER_v1, Util.join(m_projects, "\n"));
        target.putExtra(Constants.INTENT_PROJECTS_FILTER_NOT_v1, m_projectsNot);
        target.putExtra(Constants.INTENT_PRIORITIES_FILTER_v1, Util.join(m_prios, "\n"));
        target.putExtra(Constants.INTENT_PRIORITIES_FILTER_NOT_v1, m_priosNot);
        target.putExtra(Constants.INTENT_ACTIVE_SORT_v1, m_sort);
        return target;
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
			if (filter.apply(t) && !t.isCompleted()) {
				visibleTasks.add(t);
			}
		}
		Collections.sort(visibleTasks,getActiveSort());
		}

	@Override
	public int getCount() {
		return visibleTasks.size();
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public RemoteViews getLoadingView() {
		return null;
	}

	@Override
	public RemoteViews getViewAt(int position) {

        final int itemId = R.layout.widget_list_item;
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), itemId);
        rv.setTextViewText(R.id.widget_item_text, visibleTasks.get(position).getText());
        rv.setOnClickFillInIntent(R.id.widget_item_text, createFilterIntent());
        return rv;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void onCreate() {
        Log.v(TAG, "OnCreate called in ViewFactory");
	}

	@Override
	public void onDataSetChanged() {
		Log.v(TAG, "Data set changed, refresh");
		setFilteredTasks(false);

	}

	@Override
	public void onDestroy() {
	}
	
	private MultiComparator<Task> getActiveSort() {
		List<Comparator<Task>> comparators = new ArrayList<Comparator<Task>>();
		switch (m_sort) {
		case Constants.SORT_UNSORTED:
			break;
		case Constants.SORT_REVERSE:
			Comparator<Task> comp = Collections.reverseOrder();
            comparators.add(comp);
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
		return (new MultiComparator<Task>(comparators));
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

			for (TaskFilter f : filters) {
				if (!f.apply(input)) {
					return false;
				}
			}
			return true;
		}
	}

}

