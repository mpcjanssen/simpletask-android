package nl.mpcjanssen.simpletask

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import androidx.core.content.ContextCompat
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

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AppWidgetRemoteViewsFactory(intent)
    }
}

data class AppWidgetRemoteViewsFactory(val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
    private var visibleTasks = ArrayList<Task>()
    private var _filter: Query? = null
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

    private fun moduleName () : String {
        return "widget$widgetId"
    }

    private fun updateFilter(): Query {
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

    private fun setFilteredTasks() {
        Log.d(TAG, "Widget $widgetId: setFilteredTasks called")

        if (!TodoApplication.app.isAuthenticated) {
            Log.d(TAG, "TodoApplication.app is not authenticated")
            return
        }

        val currentFilter = updateFilter()
        filter = currentFilter

        val newVisibleTasks = ArrayList<Task>()
        val (tasks, _) = TodoApplication.todoList.getSortedTasks(currentFilter, TodoApplication.config.sortCaseSensitive)
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
        val extendedWidget = TodoApplication.config.prefs.getBoolean("widget_extended", true)

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
        val txt = Interpreter.onDisplayCallback(currentFilter.luaModule, item) ?: item.showParts(tokensToShowFilter).trim { it <= ' ' }
        val ss = SpannableString(txt)

        if (TodoApplication.config.isDarkWidgetTheme) {
            itemForDarkTheme(rv)
        } else {
            itemForLightTheme(rv)
        }
        val colorizeStrings = ArrayList<String>()
        item.lists?.forEach {
            colorizeStrings.add("@$it")
        }
        setColor(ss, Color.GRAY, colorizeStrings)
        colorizeStrings.clear()
        item.tags?.forEach {
            colorizeStrings.add("+$it")
        }
        setColor(ss, Color.GRAY, colorizeStrings)

        val prioColor: Int = when (item.priority) {
            Priority.A -> ContextCompat.getColor(TodoApplication.app, R.color.simple_red_dark)
            Priority.B -> ContextCompat.getColor(TodoApplication.app, R.color.simple_orange_dark)
            Priority.C -> ContextCompat.getColor(TodoApplication.app, R.color.simple_green_dark)
            Priority.D -> ContextCompat.getColor(TodoApplication.app, R.color.simple_blue_dark)
            else -> ContextCompat.getColor(TodoApplication.app, R.color.gray67)
        }
        if (prioColor != 0) {
            setColor(ss, prioColor, item.priority.fileFormat)
        }
        if (item.isCompleted()) {
            ss.setSpan(StrikethroughSpan(), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.tasktext, ss)

        val relAge = getRelativeAge(item, TodoApplication.app)
        val relDue = getRelativeDueDate(item, TodoApplication.app)
        val relThres = getRelativeThresholdDate(item, TodoApplication.app)
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
        if (!anyDateShown || item.isCompleted() || !extendedWidget) {
            rv.setViewVisibility(R.id.datebar, View.GONE)
            //rv.setViewPadding(R.prefName.tasktext,
            //       4, 4, 4, 4);
        } else {
            rv.setViewVisibility(R.id.datebar, View.VISIBLE)
            //rv.setViewPadding(R.prefName.tasktext,
            //        4, 4, 4, 0);
        }
        rv.setOnClickFillInIntent(R.id.taskline, createSelectedIntent(TodoApplication.todoList.getTaskIndex(item)))
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
        const val TAG = "WidgetService"
    }
}

