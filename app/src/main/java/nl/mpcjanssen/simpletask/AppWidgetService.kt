package nl.mpcjanssen.simpletask

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import nl.mpcjanssen.simpletask.task.*
import nl.mpcjanssen.simpletask.util.*

class AppWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return AppWidgetRemoteViewsFactory(intent)
    }
}

data class AppWidgetRemoteViewsFactory(val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
    var visibleTasks = ArrayList<Task>()
    var _filter: Query? = null
    var filter: Query
        get() {
            val currentFilter = _filter ?: updateFilter()
            if (_filter == null) {
                _filter = currentFilter
            }
            return currentFilter
        }
        set (newFilter) {
            _filter = newFilter
        }

    init {
        Log.d(TAG, "Creating view for widget: $widgetId")
    }

    fun moduleName () : String {
        return "widget$widgetId"
    }

    fun updateFilter(): Query {
	    Log.d (TAG, "Getting applyFilter from preferences for widget $widgetId")
	    val preferences = TodoApplication.app.getSharedPreferences("" + widgetId, 0)
        val filter = Query(preferences, luaModule = moduleName())
        Log.d(TAG, "Retrieved widget $widgetId query")

        return filter
    }

    private fun createSelectedIntent(position: Int): Intent {
        val target = Intent()
        val currentFilter = filter
        currentFilter.saveInIntent(target)
        target.putExtra(Constants.INTENT_SELECTED_TASK_LINE, position)
        return target
    }

    fun setFilteredTasks() {
        Log.d(TAG, "Widget $widgetId: setFilteredTasks called")

        if (!TodoApplication.app.isAuthenticated) {
            Log.d(TAG, "TodoApplication.app is not authenticated")
            return
        }

        val currentFilter = updateFilter()
        filter = currentFilter
        val sorts = currentFilter.getSort(Config.defaultSorts)

        val newVisibleTasks = ArrayList<Task>()
        val (tasks, _) = TodoList.getSortedTasks(currentFilter, sorts, Config.sortCaseSensitive)
        newVisibleTasks.addAll(tasks)
        Log.d(TAG, "Widget $widgetId: setFilteredTasks returned ${newVisibleTasks.size} tasks")
        visibleTasks = newVisibleTasks
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

    private fun getExtendedView(item: Task, position: Int): RemoteViews {
        val currentFilter = filter
        val rv = RemoteViews(TodoApplication.app.packageName, R.layout.widget_list_item)
        val extended_widget = Config.prefs.getBoolean("widget_extended", true)
        val task = item

        val tokensToShowFilter: (it: TToken) -> Boolean = {
            when (it) {
                is UUIDToken -> false
                is CreateDateToken -> false
                is CompletedToken -> false
                is CompletedDateToken -> false
                is DueDateToken -> false
                is ThresholdDateToken -> false
                is ListToken -> !currentFilter.hideLists
                is TagToken -> !currentFilter.hideTags
                else -> true
            }
        }
        val txt = Interpreter.onDisplayCallback(currentFilter.luaModule, task) ?: task.showParts(tokensToShowFilter).trim { it <= ' ' }
        val ss = SpannableString(txt)

        if (Config.isDarkWidgetTheme) {
            itemForDarkTheme(rv)
        } else {
            itemForLightTheme(rv)
        }
        val colorizeStrings = ArrayList<String>()
        task.lists?.forEach {
            colorizeStrings.add("@$it")
        }
        setColor(ss, Color.GRAY, colorizeStrings)
        colorizeStrings.clear()
        task.tags?.forEach {
            colorizeStrings.add("+$it")
        }
        setColor(ss, Color.GRAY, colorizeStrings)

        val prioColor: Int
        prioColor = when (task.priority) {
            Priority.A -> ContextCompat.getColor(TodoApplication.app, R.color.simple_red_dark)
            Priority.B -> ContextCompat.getColor(TodoApplication.app, R.color.simple_orange_dark)
            Priority.C -> ContextCompat.getColor(TodoApplication.app, R.color.simple_green_dark)
            Priority.D -> ContextCompat.getColor(TodoApplication.app, R.color.simple_blue_dark)
            else -> ContextCompat.getColor(TodoApplication.app, R.color.gray67)
        }
        if (prioColor != 0) {
            setColor(ss, prioColor, task.priority.fileFormat)
        }
        if (task.isCompleted()) {
            ss.setSpan(StrikethroughSpan(), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.tasktext, ss)

        val relAge = getRelativeAge(task, TodoApplication.app)
        val relDue = getRelativeDueDate(task, TodoApplication.app)
        val relThres = getRelativeThresholdDate(task, TodoApplication.app)
        var anyDateShown = false
        if (!relAge.isNullOrEmpty() && !filter.hideCreateDate) {
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
        if (!relThres.isNullOrEmpty()) {
            anyDateShown = true
            rv.setTextViewText(R.id.taskthreshold, relThres)
        } else {
            rv.setTextViewText(R.id.taskthreshold, "")
        }
        if (!anyDateShown || task.isCompleted() || !extended_widget) {
            rv.setViewVisibility(R.id.datebar, View.GONE)
            //rv.setViewPadding(R.prefName.tasktext,
            //       4, 4, 4, 4);
        } else {
            rv.setViewVisibility(R.id.datebar, View.VISIBLE)
            //rv.setViewPadding(R.prefName.tasktext,
            //        4, 4, 4, 0);
        }
        rv.setOnClickFillInIntent(R.id.taskline, createSelectedIntent(TodoList.getTaskIndex(task)))
        return rv
    }

    private fun itemForLightTheme(rv: RemoteViews) {
        rv.setTextColor(R.id.tasktext, ContextCompat.getColor(TodoApplication.app, R.color.black))
        rv.setTextColor(R.id.taskage, ContextCompat.getColor(TodoApplication.app, R.color.gray67))
        rv.setTextColor(R.id.taskdue, ContextCompat.getColor(TodoApplication.app, R.color.gray67))
        rv.setTextColor(R.id.taskthreshold, ContextCompat.getColor(TodoApplication.app, R.color.gray67))
    }

    private fun itemForDarkTheme(rv: RemoteViews) {
        rv.setTextColor(R.id.tasktext, ContextCompat.getColor(TodoApplication.app, R.color.white))
        rv.setTextColor(R.id.taskage, ContextCompat.getColor(TodoApplication.app, R.color.gray67))
        rv.setTextColor(R.id.taskdue, ContextCompat.getColor(TodoApplication.app, R.color.gray67))
        rv.setTextColor(R.id.taskthreshold, ContextCompat.getColor(TodoApplication.app, R.color.gray67))
    }

    override fun getViewAt(position: Int): RemoteViews? {

        if (visibleTasks.size <= position) {
            // This should never happen but there was a crash reported
            // so in that case at least don't crash
            return null
        }

        // find index in the to-do list of the clicked task
        val task = visibleTasks[position]

        return getExtendedView(task, position)
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onCreate() {
        Log.d(TAG, "Widget: OnCreate called in ViewFactory")
    }

    override fun onDataSetChanged() {
        setFilteredTasks()
    }

    override fun onDestroy() {
    }

    companion object {
        val TAG = "WidgetService"
    }
}

