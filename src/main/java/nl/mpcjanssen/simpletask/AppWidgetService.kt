package nl.mpcjanssen.simpletask

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
import nl.mpcjanssen.simpletask.task.Priority
import nl.mpcjanssen.simpletask.task.TodoListItem
import nl.mpcjanssen.simpletask.util.*


import java.util.ArrayList
import java.util.Collections

class AppWidgetService : RemoteViewsService() {


    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return AppWidgetRemoteViewsFactory(application as TodoApplication, intent)
    }
}

internal class AppWidgetRemoteViewsFactory(private val application: TodoApplication?, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val log: Logger

    private val mFilter: ActiveFilter

    private val m_app: Context

    var visibleTasks = ArrayList<TodoListItem>()

    init {
        log = Logger
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        log.debug(TAG, "Creating view for widget: " + widgetId)
        m_app = TodoApplication.appContext
        val preferences = m_app.getSharedPreferences("" + widgetId, 0)
        mFilter = ActiveFilter()
        mFilter.initFromPrefs(preferences)
        setFilteredTasks()
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
                application.sortCaseSensitive(),
                application.useCreateBackup())
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
        val rv = RemoteViews(m_app.packageName, R.layout.widget_list_item)
        val extended_widget = TodoApplication.prefs.getBoolean("widget_extended", true)
        val task = item.task

//        var tokensToShow = TToken.ALL
//        tokensToShow = tokensToShow and TToken.CREATION_DATE.inv()
//        tokensToShow = tokensToShow and TToken.COMPLETED.inv()
//        tokensToShow = tokensToShow and TToken.COMPLETED_DATE.inv()
//        tokensToShow = tokensToShow and TToken.THRESHOLD_DATE.inv()
//        tokensToShow = tokensToShow and TToken.DUE_DATE.inv()
        if (mFilter.hideLists) {
//            tokensToShow = tokensToShow and TToken.LIST.inv()
        }
        if (mFilter.hideTags) {
//            tokensToShow = tokensToShow and TToken.TTAG.inv()
        }
        val ss = SpannableString(
                task.showParts(0).trim { it <= ' ' })

        if (application!!.isDarkWidgetTheme) {
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
            Priority.A -> prioColor = ContextCompat.getColor(m_app,android.R.color.holo_red_dark)
            Priority.B -> prioColor = ContextCompat.getColor(m_app,android.R.color.holo_orange_dark)
            Priority.C -> prioColor = ContextCompat.getColor(m_app,android.R.color.holo_green_dark)
            Priority.D -> prioColor = ContextCompat.getColor(m_app,android.R.color.holo_blue_dark)
            else -> prioColor = ContextCompat.getColor(m_app,android.R.color.darker_gray)
        }
        if (prioColor != 0) {
            setColor(ss, prioColor, task.priority.inFileFormat())
        }
        if (task.isCompleted()) {
            ss.setSpan(StrikethroughSpan(), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.tasktext, ss)

        val relAge = getRelativeAge(task, m_app)
        val relDue = getRelativeDueDate(task ,m_app, ContextCompat.getColor(m_app,android.R.color.holo_green_light),
                ContextCompat.getColor(m_app,android.R.color.holo_red_light),
                true)
        val relThres = getRelativeThresholdDate(task, m_app)
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
        rv.setTextColor(R.id.tasktext, ContextCompat.getColor(m_app,android.R.color.black))
        rv.setTextColor(R.id.taskage, ContextCompat.getColor(m_app,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskdue, ContextCompat.getColor(m_app,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskthreshold, ContextCompat.getColor(m_app,android.R.color.darker_gray))
    }

    private fun itemForDarkTheme(rv: RemoteViews) {
        rv.setTextColor(R.id.tasktext, ContextCompat.getColor(m_app,android.R.color.white))
        rv.setTextColor(R.id.taskage, ContextCompat.getColor(m_app,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskdue, ContextCompat.getColor(m_app,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskthreshold, ContextCompat.getColor(m_app,android.R.color.darker_gray))
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

