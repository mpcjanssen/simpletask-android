package nl.mpcjanssen.simpletask;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Collections;

import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;

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
    private TodoApplication application;
    ArrayList<Task> visibleTasks = new ArrayList<Task>();
    private TaskCache m_taskBag;

    public AppWidgetRemoteViewsFactory(TodoApplication application, Intent intent) {
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
	Log.v(TAG, "Creating view for widget: " + widgetId);
	mContext = TodoApplication.getAppContext();
        SharedPreferences preferences = mContext.getSharedPreferences("" + widgetId, 0);
        mFilter = new ActiveFilter();
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
        for (Integer i : mFilter.apply(application.getTaskCache().getTasks())) {
            Task t = application.getTaskCache().getTaskAt(i);
            if (t.isVisible()) {
                visibleTasks.add(t);
            }
        }
        Collections.sort(visibleTasks,MultiComparator.create(mFilter.getSort(application.getDefaultSorts())));
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

    private RemoteViews getExtendedView(int position) {
	RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);
	Task task;
	task = visibleTasks.get(position);

	if (task != null) {
	    SpannableString ss = new SpannableString(
						     task.inScreenFormat(mFilter));

	    if (TodoApplication.getPrefs().getString("widget_theme","").equals("android.R.style.Theme_Holo")) {
		rv.setTextColor(R.id.tasktext, application.getResources().getColor(android.R.color.white));
	    } else {
		rv.setTextColor(R.id.tasktext, application.getResources().getColor(android.R.color.black));
	    }
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

	    Resources res = mContext.getResources();
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
	    if (prioColor!=0) {
		Util.setColor(ss, prioColor, task.getPriority()
			      .inFileFormat());
	    }
	    if (task.isCompleted()) {
		ss.setSpan(new StrikethroughSpan(), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	    }
	    rv.setTextViewText(R.id.tasktext,ss);

	    String relAge = task.getRelativeAge();
	    SpannableString relDue = task.getRelativeDueDate(res.getColor(android.R.color.holo_green_light),
                res.getColor(android.R.color.holo_red_light),
                true);
	    String relThres = task.getRelativeThresholdDate();
	    boolean anyDateShown = false;
	    if (!Strings.isEmptyOrNull(relAge)) {
		rv.setTextViewText(R.id.taskage,relAge);
		anyDateShown = true;
	    } else {
		rv.setTextViewText(R.id.taskage, "");
	    }
	    if (relDue!=null) {
		anyDateShown = true;
		rv.setTextViewText(R.id.taskdue,relDue);
	    } else {
		rv.setTextViewText(R.id.taskdue, "");
	    }
	    if (!Strings.isEmptyOrNull(relThres)) {
		anyDateShown = true;
		rv.setTextViewText(R.id.taskthreshold, relThres);
	    } else {
		rv.setTextViewText(R.id.taskthreshold, "");
	    }
	    if (!anyDateShown || task.isCompleted()) {
		rv.setViewVisibility(R.id.datebar, View.GONE);
		//rv.setViewPadding(R.id.tasktext,
		//       4, 4, 4, 4);
	    } else {
		rv.setViewVisibility(R.id.datebar,View.VISIBLE);
		//rv.setViewPadding(R.id.tasktext,
		//        4, 4, 4, 0);
	    }
	}
        rv.setOnClickFillInIntent(R.id.taskline, createFilterIntent(visibleTasks.get(position)));
        return rv;
    }

    private RemoteViews getSimpleView(int position) {
        RemoteViews rv;
        rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_simple_list_item);
        Task task = visibleTasks.get(position);
        SpannableString ss = new SpannableString(
						 task.inScreenFormat(mFilter));
        if (task.isCompleted()) {
            ss.setSpan(new StrikethroughSpan(), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        rv.setTextViewText(R.id.widget_item_text, ss);
        if (TodoApplication.getPrefs().getString("widget_theme","").equals("android.R.style.Theme_Holo")) {
            rv.setTextColor( R.id.widget_item_text, application.getResources().getColor(android.R.color.white));
        } else {
            rv.setTextColor(R.id.widget_item_text, application.getResources().getColor(android.R.color.black));
        }
        rv.setOnClickFillInIntent(R.id.widget_item_text, createFilterIntent(visibleTasks.get(position)));
        return rv;
    }



    @Override
    public RemoteViews getViewAt(int position) {
        //Log.v(TAG,"GetViewAt:" + position);
        RemoteViews rv;
        boolean extended_widget =  TodoApplication.getPrefs().getBoolean("widget_extended",true);
        if (extended_widget) {
            rv =  getExtendedView(position);
        } else {
            rv = getSimpleView(position);
        }
        return rv;
    }


    @Override
    public int getViewTypeCount() {
	return 2;
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
	    Log.v(TAG, "Data set changed, refresh");
	    resetTaskBag();
        setFilteredTasks();
    }

    @Override
    public void onDestroy() {
	// TODO Auto-generated method stub
    }

    private void resetTaskBag() {
        m_taskBag = null;
    }
}

