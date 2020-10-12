package nl.mpcjanssen.simpletask.util

import android.util.Log
import me.smichel.android.KPreferences.Preferences
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.task.Task
import org.json.JSONObject
import java.io.File
import java.util.*

class Config(app: TodoApplication) : Preferences(app) {

    val TAG = "Config"

    init {
        registerCallbacks(listOf<String>(
                getString(R.string.widget_theme_pref_key),
                getString(R.string.widget_extended_pref_key),
                getString(R.string.widget_background_transparency),
                getString(R.string.widget_header_transparency)
        )) {
            TodoApplication.app.redrawWidgets()
        }
        registerCallbacks(listOf<String>(
                getString(R.string.calendar_sync_dues),
                getString(R.string.calendar_sync_thresholds),
                getString(R.string.calendar_reminder_days),
                getString(R.string.calendar_reminder_time)
        )) {
            CalendarSync.updatedSyncTypes()
        }
    }

    val useTodoTxtTerms by BooleanPreference(R.string.ui_todotxt_terms, false)

    val showTxtOnly by BooleanPreference(R.string.show_txt_only, false)

    val _syncDues by BooleanPreference(R.string.calendar_sync_dues, false)
    val isSyncDues: Boolean
        get() = TodoApplication.atLeastAPI(16) && _syncDues

    val _syncThresholds by BooleanPreference(R.string.calendar_sync_thresholds, false)
    val isSyncThresholds: Boolean
        get() = TodoApplication.atLeastAPI(16) && _syncThresholds

    val reminderDays by IntPreference(R.string.calendar_reminder_days, 1)

    val reminderTime by IntPreference(R.string.calendar_reminder_time, 720)

    val listTerm: String
        get() {
            if (useTodoTxtTerms) {
                return getString(R.string.context_prompt_todotxt)
            } else {
                return getString(R.string.context_prompt)
            }
        }

    val tagTerm: String
        get() {
            if (useTodoTxtTerms) {
                return getString(R.string.project_prompt_todotxt)
            } else {
                return getString(R.string.project_prompt)
            }
        }

    var lastSeenRemoteId by StringOrNullPreference(R.string.file_current_version_id)

    var lastScrollPosition by IntPreference(R.string.ui_last_scroll_position, -1)

    var lastScrollOffset by IntPreference(R.string.ui_last_scroll_offset, -1)

    var luaConfig by StringPreference(R.string.lua_config, "")

    var isWordWrap by BooleanPreference(R.string.word_wrap_key, true)

    var isCapitalizeTasks by BooleanPreference(R.string.capitalize_tasks, true)

    val showTodoPath by BooleanPreference(R.string.show_todo_path, false)

    val backClearsFilter by BooleanPreference(R.string.back_clears_filter, false)

    val sortCaseSensitive by BooleanPreference(R.string.ui_sort_case_sensitive, true)

    private val _windowsEOL by BooleanPreference(R.string.line_breaks_pref_key, true)
    val eol: String
        get() = if (_windowsEOL) "\r\n" else "\n"

    fun hasDonated(): Boolean {
        try {
            TodoApplication.app.packageManager.getInstallerPackageName("nl.mpcjanssen.simpletask.donate")
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    val hasAppendAtEnd by BooleanPreference(R.string.append_tasks_at_end, true)

    // Takes an argument f, an expression that maps theme strings to IDs
    val activeTheme: Int
        get() {
            return when (activeThemeString) {
                "dark" -> R.style.AppTheme_NoActionBar
                "black" -> R.style.AppTheme_Black_NoActionBar
                else -> R.style.AppTheme_Light_NoActionBar
            }
        }

    val activeActionBarTheme: Int
        get() {
            return when (activeThemeString) {
                "dark" -> R.style.AppTheme_ActionBar
                "black" -> R.style.AppTheme_Black_ActionBar
                else -> R.style.AppTheme_Light_DarkActionBar
            }
        }

    val activePopupTheme: Int
        get() {
            return if (isDarkTheme || isBlackTheme) {
                R.style.AppTheme_ActionBar
            } else {
                R.style.AppTheme_Black_ActionBar
            }
        }

    val isDarkTheme: Boolean
        get() {
            return when (activeThemeString) {
                "dark" -> true
                else -> false
            }
        }

    val isBlackTheme: Boolean
        get() {
            return when (activeThemeString) {
                "black" -> true
                else -> false
            }
        }

    private val _widgetTheme by StringPreference(R.string.widget_theme_pref_key, "light_darkactionbar")
    val isDarkWidgetTheme: Boolean
        get() = _widgetTheme == "dark"

    private val _activeTheme by StringPreference(R.string.theme_pref_key, "light_darkactionbar")
    private val activeThemeString: String
        get() = Interpreter.configTheme() ?: _activeTheme

    // Only used in Dropbox build
    @Suppress("unused")
    var fullDropBoxAccess by BooleanPreference(R.string.dropbox_full_access, true)

    private val dateBarSize by IntPreference(R.string.datebar_relative_size, 80)
    val dateBarRelativeSize: Float
        get() = dateBarSize / 100.0f

    val showCalendar by BooleanPreference(R.string.ui_show_calendarview, false)

    val tasklistTextSize: Float?
        get() {
            val luaValue = Interpreter.tasklistTextSize()
            if (luaValue != null) {
                return luaValue
            }
            val customSize by BooleanPreference(R.string.custom_font_size, false)
            if (!customSize) {
                return 14.0f
            }
            val fontSize by IntPreference(R.string.font_size, 14)
            return fontSize.toFloat()
        }

    val hasShareTaskShowsEdit by BooleanPreference(R.string.share_task_show_edit, false)

    val useListAndTagIcons by BooleanPreference(R.string.use_list_and_tags_icons, true)

    val hasExtendedTaskView by BooleanPreference(R.string.taskview_extended_pref_key, true)

    val showCompleteCheckbox by BooleanPreference(R.string.ui_complete_checkbox, true)

    val showConfirmationDialogs by BooleanPreference(R.string.ui_show_confirmation_dialogs, true)

    val defaultSorts: Array<String>
        get() = TodoApplication.app.resources.getStringArray(R.array.sortKeys)

    private var _todoFileName by StringOrNullPreference(R.string.todo_file_key)
    val todoFile: File
        get()  = _todoFileName?.let { File(it )} ?: FileStore.getDefaultFile()



    fun setTodoFile(file: File?) {
        _todoFileName = file?.path
        clearCache()
    }

    val doneFile: File
        get() = File(todoFile.parentFile, "done.txt")

    fun clearCache() {
        cachedContents = null
        todoList = null
        lastSeenRemoteId = null
    }

    val isAutoArchive by BooleanPreference(R.string.auto_archive_pref_key, false)

    val hasPrependDate by BooleanPreference(R.string.prepend_date_pref_key, true)

    val hasKeepSelection by BooleanPreference(R.string.keep_selection, false)

    val hasKeepPrio by BooleanPreference(R.string.keep_prio, true)

    val shareAppendText by StringPreference(R.string.share_task_append_text, " +background")

    var latestChangelogShown by IntPreference(R.string.latest_changelog_shown, 0)

    var rightDrawerDemonstrated by BooleanPreference(R.string.right_drawer_demonstrated, false)

    val localFileRoot by StringPreference(R.string.local_file_root, "/sdcard/")

    val hasColorDueDates by BooleanPreference(R.string.color_due_date_key, true)

    private var cachedContents by StringOrNullPreference(R.string.cached_todo_file)

    var todoList: List<Task>?
        get() = cachedContents?.let {
            val lines = it.lines()
            Log.i(TAG, "Getting ${lines.size} items todoList from cache")
            ArrayList<Task>().apply {
                addAll(lines.map { line -> Task(line) })
            }
        }
        set(items) {
            Log.i(TAG, "Updating todoList cache with ${items?.size} tasks")
            if (items == null) {
                prefs.edit().remove(getString(R.string.cached_todo_file)).apply()
            } else {
                cachedContents = items.joinToString("\n") { it.inFileFormat(useUUIDs) }
            }
        }
    var changesPending by BooleanPreference(R.string.changes_pending, false)
    var forceEnglish by BooleanPreference(R.string.force_english, false)
    var useUUIDs by BooleanPreference(R.string.use_uuids, false)

    fun legacyQueryStoreJson() : String   {
        val queries = LegacyQueryStore.ids().map {
            LegacyQueryStore.get(it)
        }
        val jsonObject = queries.fold(JSONObject()) { acc, query ->
            acc.put(query.name, query.query.saveInJSON())
        }
        return jsonObject.toString(2)
    }

    var savedQueriesJSONString by StringPreference(R.string.query_store, legacyQueryStoreJson())

    var savedQueries : List<NamedQuery>
    get() {
        val queries = ArrayList<NamedQuery>()
        val jsonFilters = JSONObject(savedQueriesJSONString)
        jsonFilters.keys().forEach { name ->
            val json = jsonFilters.getJSONObject(name)
            val newQuery = NamedQuery(name, Query(json, luaModule = "mainui"))
            queries.add(newQuery)
        }
        return queries
    }
    set(queries) {
        val jsonFilters = queries.fold(JSONObject()) { acc, query ->
            acc.put(query.name, query.query.saveInJSON())
        }
        savedQueriesJSONString = jsonFilters.toString(2)
    }

    fun getSortString(key: String): String {
        if (useTodoTxtTerms) {
            if ("by_context" == key) {
                return getString(R.string.by_context_todotxt)
            }
            if ("by_project" == key) {
                return getString(R.string.by_project_todotxt)
            }
        }
        val keys = Arrays.asList(*TodoApplication.app.resources.getStringArray(R.array.sortKeys))
        val values = TodoApplication.app.resources.getStringArray(R.array.sort)
        val index = keys.indexOf(key)
        if (index == -1) {
            return getString(R.string.none)
        }
        return values[index]
    }

    var mainQuery: Query
        get() = Query(this.prefs, luaModule = "mainui")
        set(value) {
            // Update the intent so we wont get the old applyFilter after
            // switching back to app later. Fixes [1c5271ee2e
            value.saveInPrefs(prefs)
            TodoApplication.config.lastScrollPosition = -1
        }

    val idleBeforeSaveSeconds by IntPreference(R.string.idle_before_save, 5)

}
