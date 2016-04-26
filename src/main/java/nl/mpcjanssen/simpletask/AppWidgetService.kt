package nl.mpcjanssen.simpletask

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.task.*
import nl.mpcjanssen.simpletask.util.*


import java.util.ArrayList
import java.util.Collections

class AppWidgetService : RemoteViewsService() {


    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return AppWidgetRemoteViewsFactory(applicationContext, intent)
    }
}

internal class AppWidgetRemoteViewsFactory(private val ctxt: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val log: Logger

    private val mFilter: ActiveFilter
    private val application: SimpletaskApplication

    var visibleTasks = ArrayList<TodoListItem>()

    init {
        log = Logger
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        log.debug(TAG, "Creating view for widget: " + widgetId)
        application = ctxt as SimpletaskApplication
        val preferences = ctxt.getSharedPreferences("" + widgetId, 0)
        mFilter = ActiveFilter()
        mFilter.initFromPrefs(preferences)
        setFilteredTasks()
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }


    private fun createSelectedIntent(t: TodoListItem): Intent {
        val target = Intent()
        mFilter.saveInIntent(target)
        target.putExtra(Constants.INTENT_SELECTED_TASK, t.task.inFileFormat())
        return target
    }


    fun setFilteredTasks() {
        // log.debug(TAG, "Widget: setFilteredTasks called");
        visibleTasks = ArrayList<TodoListItem>()
        if (application == null) {
            log.debug(TAG, "application object was null")
            return
        }
        if (!application.isAuthenticated) {
            return
        }

        val tl = application.todoList
        val items = tl.todoItems
        for (t in mFilter.apply(items)) {
            visibleTasks.add(t)
        }
        val comp = MultiComparator(mFilter.getSort(
                application.defaultSorts),
                application.today,
                application.sortCaseSensitive(),
                mFilter.createIsThreshold)
        Collections.sort(visibleTasks, comp)
    }

    override fun getCount(): Int {
        return visibleTasks.size
    }

    override fun getItemId(arg0: Int): Long {
        return arg0.toLong()
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    private fun getExtendedView(item: TodoListItem): RemoteViews {
        val rv = RemoteViews(application.packageName, R.layout.widget_list_item)
        val extended_widget = application.prefs.getBoolean("widget_extended", true)
        val task = item.task

        var tokensToShow = ALL
        tokensToShow = tokensToShow and CREATION_DATE.inv()
        tokensToShow = tokensToShow and COMPLETED.inv()
        tokensToShow = tokensToShow and COMPLETED_DATE.inv()
        tokensToShow = tokensToShow and THRESHOLD_DATE.inv()
        tokensToShow = tokensToShow and DUE_DATE.inv()
        if (mFilter.hideLists) {
            tokensToShow = tokensToShow and LIST.inv()
        }
        if (mFilter.hideTags) {
            tokensToShow = tokensToShow and TTAG.inv()
        }
        val ss = SpannableString(
                task.showParts(tokensToShow).trim { it <= ' ' })

        if (application.isDarkWidgetTheme) {
            itemForDarkTheme(rv)
        } else {
            itemForLightTheme(rv)
        }
        val colorizeStrings = ArrayList<String>()
        for (context in task.lists) {
            colorizeStrings.add("@" + context)
        }
        setColor(ss, Color.GRAY, colorizeStrings)
        colorizeStrings.clear()
        for (project in task.tags) {
            colorizeStrings.add("+" + project)
        }
        setColor(ss, Color.GRAY, colorizeStrings)

        val prioColor: Int
        when (task.priority) {
            Priority.A -> prioColor = ContextCompat.getColor(application,android.R.color.holo_red_dark)
            Priority.B -> prioColor = ContextCompat.getColor(application,android.R.color.holo_orange_dark)
            Priority.C -> prioColor = ContextCompat.getColor(application,android.R.color.holo_green_dark)
            Priority.D -> prioColor = ContextCompat.getColor(application,android.R.color.holo_blue_dark)
            else -> prioColor = ContextCompat.getColor(application,android.R.color.darker_gray)
        }
        if (prioColor != 0) {
            setColor(ss, prioColor, task.priority.inFileFormat())
        }
        if (task.isCompleted()) {
            ss.setSpan(StrikethroughSpan(), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.tasktext, ss)

        val relAge = getRelativeAge(task, application)
        val relDue = getRelativeDueDate(task , application, ContextCompat.getColor(application,android.R.color.holo_green_light),
                ContextCompat.getColor(application,android.R.color.holo_red_light),
                true)
        val relThres = getRelativeThresholdDate(task, application)
        var anyDateShown = false
        if (!isEmptyOrNull(relAge) && !mFilter.hideCreateDate) {
            rv.setTextViewText(R.id.taskage, relAge)
            anyDateShown = true
        } else {
            rv.setTextViewText(R.id.taskage, "")
        }
        if (relDue != null) {
            anyDateShown = true
            rv.setTextViewText(R.id.taskdue, relDue)
        } else {
            rv.setTextViewText(R.id.taskdue, "")
        }
        if (!isEmptyOrNull(relThres)) {
            anyDateShown = true
            rv.setTextViewText(R.id.taskthreshold, relThres)
        } else {
            rv.setTextViewText(R.id.taskthreshold, "")
        }
        if (!anyDateShown || task.isCompleted() || !extended_widget) {
            rv.setViewVisibility(R.id.datebar, View.GONE)
            //rv.setViewPadding(R.id.tasktext,
            //       4, 4, 4, 4);
        } else {
            rv.setViewVisibility(R.id.datebar, View.VISIBLE)
            //rv.setViewPadding(R.id.tasktext,
            //        4, 4, 4, 0);
        }
        rv.setOnClickFillInIntent(R.id.taskline, createSelectedIntent(item))
        return rv
    }

    private fun itemForLightTheme(rv: RemoteViews) {
        rv.setTextColor(R.id.tasktext, ContextCompat.getColor(application,android.R.color.black))
        rv.setTextColor(R.id.taskage, ContextCompat.getColor(application,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskdue, ContextCompat.getColor(application,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskthreshold, ContextCompat.getColor(application,android.R.color.darker_gray))
    }

    private fun itemForDarkTheme(rv: RemoteViews) {
        rv.setTextColor(R.id.tasktext, ContextCompat.getColor(application,android.R.color.white))
        rv.setTextColor(R.id.taskage, ContextCompat.getColor(application,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskdue, ContextCompat.getColor(application,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskthreshold, ContextCompat.getColor(application,android.R.color.darker_gray))
    }

    override fun getViewAt(position: Int): RemoteViews? {

        if (visibleTasks.size <= position) {
            // This should never happen but there was a crash reported
            // so in that case at least don't crash
            return null
        }

        // find index in the todolist of the clicked task
        val task = visibleTasks[position]

        return getExtendedView(task)
    }


    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onCreate() {
        // log.debug(TAG, "Widget: OnCreate called in ViewFactory");
        setFilteredTasks()
    }

    override fun onDataSetChanged() {
        // log.debug(TAG, "Widget: Data set changed, refresh");
        setFilteredTasks()
    }

    override fun onDestroy() {
    }

    companion object {
        val TAG = AppWidgetRemoteViewsFactory::class.java.simpleName
    }
}

