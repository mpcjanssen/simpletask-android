package nl.mpcjanssen.simpletask;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import nl.mpcjanssen.simpletask.sort.*;
import nl.mpcjanssen.simpletask.task.*;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletask.R;

import java.util.*;

public class AppWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		// TODO Auto-generated method stub
		return new AppWidgetRemoteViewsFactory((TodoApplication)getApplication(), intent);
	}

}

class AppWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	final static String TAG = AppWidgetRemoteViewsFactory.class.getSimpleName();

    private ActiveFilter mFilter;

	private Context mContext;
	private int widgetId;
	private SharedPreferences preferences;
	private TodoApplication application;
	ArrayList<Task> visibleTasks = new ArrayList<Task>();

	public AppWidgetRemoteViewsFactory(TodoApplication application, Intent intent) {
		widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		Log.v(TAG, "Creating view for widget: " + widgetId);
		mContext = TodoApplication.getAppContext();
		preferences = mContext.getSharedPreferences(""+widgetId, 0);
        mFilter = new ActiveFilter(mContext.getResources());
        mFilter.initFromPrefs(preferences);
        this.application = application;
		setFilteredTasks();
	}
	
    private Intent createFilterIntent(Task selectedTask) {
    	Intent target = new Intent();
        mFilter.saveInIntent(target);
        target.putExtra(Constants.INTENT_SELECTED_TASK, selectedTask.getId() + ":" + selectedTask.inFileFormat());
        return target;
    }



	void setFilteredTasks() {
		Log.v(TAG, "setFilteredTasks called");
		visibleTasks.clear();
        visibleTasks.addAll(mFilter.apply(application.getTaskBag().getTasks()));
		Collections.sort(visibleTasks,MultiComparator.create(mFilter.getSort()));
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return visibleTasks.size();
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public RemoteViews getLoadingView() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteViews getViewAt(int position) {
        //Log.v(TAG,"GetViewAt:" + position);
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);
        rv.setTextViewText(R.id.widget_item_text, visibleTasks.get(position).getText());
        if (TodoApplication.getPrefs().getString("widget_theme","").equals("android.R.style.Theme_Holo")) {
            rv.setTextColor( R.id.widget_item_text, application.getResources().getColor(android.R.color.white));
        } else {
            rv.setTextColor(R.id.widget_item_text, application.getResources().getColor(android.R.color.black));
        }
        rv.setOnClickFillInIntent(R.id.widget_item_text, createFilterIntent(visibleTasks.get(position)));
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
		// TODO Auto-generated method stub

	}

	@Override
	public void onDataSetChanged() {
		// TODO Auto-generated method stub
		Log.v(TAG, "Data set changed, refresh");
		setFilteredTasks();

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub

	}
}

