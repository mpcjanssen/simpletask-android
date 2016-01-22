package nl.mpcjanssen.simpletask;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.task.TToken;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppWidgetService extends RemoteViewsService {

    
    @Override
    public RemoteViewsFactory onGetViewFactory( Intent intent) {
	    return new AppWidgetRemoteViewsFactory((TodoApplication)getApplication(), intent);
    }
}

class AppWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    final static String TAG = AppWidgetRemoteViewsFactory.class.getSimpleName();
    private final Logger log;

    private ActiveFilter mFilter;

    private Context mContext;
    private TodoApplication application;
    
    ArrayList<Task> visibleTasks = new ArrayList<>();

    public AppWidgetRemoteViewsFactory(TodoApplication application,  Intent intent) {
        log = Logger.INSTANCE;
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        log.debug(TAG, "Creating view for widget: " + widgetId);
        mContext = TodoApplication.getAppContext();
        SharedPreferences preferences = mContext.getSharedPreferences("" + widgetId, 0);
        mFilter = new ActiveFilter();
        mFilter.initFromPrefs(preferences);
        this.application = application;
        setFilteredTasks();
    }

    
    private Intent createSelectedIntent(Task t) {
        Intent target = new Intent();
        TodoList tl = application.getTodoList();
        target.putExtra(Constants.INTENT_SELECTED_TASK_POSITION, tl.find(t));
        mFilter.saveInIntent(target);
        return target;
    }


    void setFilteredTasks() {
        // log.debug(TAG, "Widget: setFilteredTasks called");
        visibleTasks = new ArrayList<>();
        if (application==null)  {
            log.debug(TAG, "application object was null");
            return;
        }
        if (!application.isAuthenticated()) {
            return;
        }

        TodoList tl = application.getTodoList();
        List<Task> tasks = tl.getTasks();
        if (tasks==null) {
            return;
        }
        for (Task t : mFilter.apply(tasks)) {
            if (t.isVisible()) {
                visibleTasks.add(t);
            }
        }
        MultiComparator comp = new MultiComparator(mFilter.getSort(
                application.getDefaultSorts()),
                application.sortCaseSensitive(),
                visibleTasks,
                application.useCreateBackup());
        Collections.sort(visibleTasks, comp);
    }

    @Override
    public int getCount() {
        return visibleTasks.size();
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Nullable
    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    private RemoteViews getExtendedView(int taskIndex, Task task) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);
        boolean extended_widget = TodoApplication.getPrefs().getBoolean("widget_extended", true);

        if (task != null) {
            int tokensToShow = TToken.ALL;
            tokensToShow = tokensToShow & ~TToken.CREATION_DATE;
            tokensToShow = tokensToShow & ~TToken.COMPLETED;
            tokensToShow = tokensToShow & ~TToken.COMPLETED_DATE;
            tokensToShow = tokensToShow & ~TToken.THRESHOLD_DATE;
            tokensToShow = tokensToShow & ~TToken.DUE_DATE;
            if (mFilter.getHideLists()) {
                tokensToShow = tokensToShow & ~ TToken.LIST;
            }
            if (mFilter.getHideTags()) {
                tokensToShow = tokensToShow & ~ TToken.TTAG;
            }
            SpannableString ss = new SpannableString(
                    task.showParts(tokensToShow).trim());

            if (application.isDarkWidgetTheme()) {
                itemForDarkTheme(rv);
            } else {
                itemForLightTheme(rv);
            }
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
            if (prioColor != 0) {
                Util.setColor(ss, prioColor, task.getPriority()
                        .inFileFormat());
            }
            if (task.isCompleted()) {
                ss.setSpan(new StrikethroughSpan(), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            rv.setTextViewText(R.id.tasktext, ss);

            String relAge = task.getRelativeAge(mContext);
            SpannableString relDue = task.getRelativeDueDate(mContext, res.getColor(android.R.color.holo_green_light),
                    res.getColor(android.R.color.holo_red_light),
                    true);
            String relThres = task.getRelativeThresholdDate(mContext);
            boolean anyDateShown = false;
            if (!Strings.isEmptyOrNull(relAge) && !mFilter.getHideCreateDate()) {
                rv.setTextViewText(R.id.taskage, relAge);
                anyDateShown = true;
            } else {
                rv.setTextViewText(R.id.taskage, "");
            }
            if (relDue != null) {
                anyDateShown = true;
                rv.setTextViewText(R.id.taskdue, relDue);
            } else {
                rv.setTextViewText(R.id.taskdue, "");
            }
            if (!Strings.isEmptyOrNull(relThres)) {
                anyDateShown = true;
                rv.setTextViewText(R.id.taskthreshold, relThres);
            } else {
                rv.setTextViewText(R.id.taskthreshold, "");
            }
            if (!anyDateShown || task.isCompleted() || !extended_widget) {
                rv.setViewVisibility(R.id.datebar, View.GONE);
                //rv.setViewPadding(R.id.tasktext,
                //       4, 4, 4, 4);
            } else {
                rv.setViewVisibility(R.id.datebar, View.VISIBLE);
                //rv.setViewPadding(R.id.tasktext,
                //        4, 4, 4, 0);
            }
        }
        rv.setOnClickFillInIntent(R.id.taskline, createSelectedIntent(task));
        return rv;
    }

    private void itemForLightTheme(RemoteViews rv) {
        rv.setTextColor(R.id.tasktext, application.getResources().getColor(android.R.color.black));
        rv.setTextColor(R.id.taskage, application.getResources().getColor(android.R.color.darker_gray));
        rv.setTextColor(R.id.taskdue, application.getResources().getColor(android.R.color.darker_gray));
        rv.setTextColor(R.id.taskthreshold, application.getResources().getColor(android.R.color.darker_gray));
    }

    private void itemForDarkTheme(RemoteViews rv) {
        rv.setTextColor(R.id.tasktext, application.getResources().getColor(android.R.color.white));
        rv.setTextColor(R.id.taskage, application.getResources().getColor(android.R.color.darker_gray));
        rv.setTextColor(R.id.taskdue, application.getResources().getColor(android.R.color.darker_gray));
        rv.setTextColor(R.id.taskthreshold, application.getResources().getColor(android.R.color.darker_gray));
    }

    @Override
    @Nullable
    public RemoteViews getViewAt(int position) {
        
        if (visibleTasks.size()<=position) {
            // This should never happen but there was a crash reported
            // so in that case at least don't crash
            return null;
        }

        // find index in the todolist of the clicked task
        Task task = visibleTasks.get(position);
        TodoList tl = application.getTodoList();
        int taskIndex = tl.getTasks().indexOf(task);


        return getExtendedView(taskIndex,task);
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
        // log.debug(TAG, "Widget: OnCreate called in ViewFactory");
        setFilteredTasks();
    }

    @Override
    public void onDataSetChanged() {
        // log.debug(TAG, "Widget: Data set changed, refresh");
        setFilteredTasks();
    }

    @Override
    public void onDestroy() {
    }
}

