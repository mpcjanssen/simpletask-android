package nl.mpcjanssen.simpletask


import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import nl.mpcjanssen.simpletask.dao.gentodo.TodoItem
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.task.Priority
import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.*
import org.json.JSONObject
import java.util.*

class AppWidgetService : RemoteViewsService() {


    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return AppWidgetRemoteViewsFactory(intent)
    }
}

data class AppWidgetRemoteViewsFactory(val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val log: Logger
    val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
    var visibleTasks = ArrayList<TodoItem>()

    init {
        log = Logger
        log.debug(TAG, "Creating view for widget: " + widgetId)
    }

    fun moduleName () : String {
        return "widget$widgetId"
    }
   
    fun getFilter () : ActiveFilter {
	    log.debug (TAG, "Getting filter from preferences for widget $widgetId")
	    val preferences = TodoApplication.app.getSharedPreferences("" + widgetId, 0)
        val filter = ActiveFilter(FilterOptions(luaModule = moduleName()))
        filter.initFromPrefs(preferences)
        val obj = JSONObject()
        filter.saveInJSON(obj)
        log.debug (TAG, "Widget $widgetId filter $obj")

        return filter
    }


    private fun createSelectedIntent(t: TodoItem): Intent {
        val target = Intent()
        getFilter().saveInIntent(target)
        target.putExtra(Constants.INTENT_SELECTED_TASK_LINE, t.line)
        return target
    }


    fun setFilteredTasks() {
        log.debug(TAG, "Widget $widgetId: setFilteredTasks called")

        if (!TodoApplication.app.isAuthenticated) {
            log.debug(TAG, "TodoApplication.app is not authenticated")
            return
        }
        
        val items = TodoList.todoItems
        visibleTasks.clear()
        val filter = getFilter() 

        for (t in filter.apply(items)) {
            visibleTasks.add(t)
        }
        val comp = MultiComparator(filter.getSort(
                Config.defaultSorts),
                TodoApplication.app.today,
                Config.sortCaseSensitive(),
                filter.createIsThreshold)
        Collections.sort(visibleTasks, comp)
        log.debug(TAG, "Widget $widgetId: setFilteredTasks returned ${visibleTasks.size} tasks")
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

    private fun getExtendedView(item: TodoItem): RemoteViews {
        val filter = getFilter()
        val rv = RemoteViews(TodoApplication.app.packageName, R.layout.widget_list_item)
        val extended_widget = Config.prefs.getBoolean("widget_extended", true)
        val task = item.task

        var tokensToShow = TToken.ALL
        tokensToShow = tokensToShow and TToken.CREATION_DATE.inv()
        tokensToShow = tokensToShow and TToken.COMPLETED.inv()
        tokensToShow = tokensToShow and TToken.COMPLETED_DATE.inv()
        tokensToShow = tokensToShow and TToken.THRESHOLD_DATE.inv()
        tokensToShow = tokensToShow and TToken.DUE_DATE.inv()
        if (filter.hideLists) {
            tokensToShow = tokensToShow and TToken.LIST.inv()
        }
        if (filter.hideTags) {
            tokensToShow = tokensToShow and TToken.TTAG.inv()
        }
        val ss = SpannableString(
                task.showParts(tokensToShow).trim { it <= ' ' })

        if (Config.isDarkWidgetTheme) {
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
            Priority.A -> prioColor = ContextCompat.getColor(TodoApplication.app,android.R.color.holo_red_dark)
            Priority.B -> prioColor = ContextCompat.getColor(TodoApplication.app,android.R.color.holo_orange_dark)
            Priority.C -> prioColor = ContextCompat.getColor(TodoApplication.app,android.R.color.holo_green_dark)
            Priority.D -> prioColor = ContextCompat.getColor(TodoApplication.app,android.R.color.holo_blue_dark)
            else -> prioColor = ContextCompat.getColor(TodoApplication.app,android.R.color.darker_gray)
        }
        if (prioColor != 0) {
            setColor(ss, prioColor, task.priority.inFileFormat())
        }
        if (task.isCompleted()) {
            ss.setSpan(StrikethroughSpan(), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.tasktext, ss)

        val relAge = getRelativeAge(task, TodoApplication.app)
        val relDue = getRelativeDueDate(task , TodoApplication.app, ContextCompat.getColor(TodoApplication.app,android.R.color.holo_green_light),
                ContextCompat.getColor(TodoApplication.app,android.R.color.holo_red_light),
                true)
        val relThres = getRelativeThresholdDate(task, TodoApplication.app)
        var anyDateShown = false
        if (!isEmptyOrNull(relAge) && !filter.hideCreateDate) {
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
        rv.setTextColor(R.id.tasktext, ContextCompat.getColor(TodoApplication.app,android.R.color.black))
        rv.setTextColor(R.id.taskage, ContextCompat.getColor(TodoApplication.app,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskdue, ContextCompat.getColor(TodoApplication.app,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskthreshold, ContextCompat.getColor(TodoApplication.app,android.R.color.darker_gray))
    }

    private fun itemForDarkTheme(rv: RemoteViews) {
        rv.setTextColor(R.id.tasktext, ContextCompat.getColor(TodoApplication.app,android.R.color.white))
        rv.setTextColor(R.id.taskage, ContextCompat.getColor(TodoApplication.app,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskdue, ContextCompat.getColor(TodoApplication.app,android.R.color.darker_gray))
        rv.setTextColor(R.id.taskthreshold, ContextCompat.getColor(TodoApplication.app,android.R.color.darker_gray))
    }

    override fun getViewAt(position: Int): RemoteViews? {

        if (visibleTasks.size <= position) {
            // This should never happen but there was a crash reported
            // so in that case at least don't crash
            return null
        }

        // find index in the to-do list of the clicked task
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
        log.debug(TAG, "Widget: OnCreate called in ViewFactory")
    }

    override fun onDataSetChanged() {
        setFilteredTasks()
    }

    override fun onDestroy() {
    }

    companion object {
        val TAG = "AppWidgetRemoteViewsFactory"
    }
}

