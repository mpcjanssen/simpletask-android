package nl.mpcjanssen.simpletask.util

import android.content.SharedPreferences
import me.smichel.android.KPreferences.Preferences
import nl.mpcjanssen.simpletask.CalendarSync
import nl.mpcjanssen.simpletask.LuaInterpreter
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.task.Task
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object Config : Preferences(TodoApplication.app), SharedPreferences.OnSharedPreferenceChangeListener {

    val TAG = "LuaConfig"

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
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

    var currentVersionId by StringOrNullPreference(R.string.file_current_version_id)

    var lastScrollPosition by IntPreference(R.string.ui_last_scroll_position, -1)

    var lastScrollOffset by IntPreference(R.string.ui_last_scroll_offset, -1)

    var luaConfig by StringPreference(R.string.lua_config, "")

    var isWordWrap by BooleanPreference(R.string.word_wrap_key, true)

    var isShowEditTextHint by BooleanPreference(R.string.show_edittext_hint, true)

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

    var isAddTagsCloneTags by BooleanPreference(R.string.clone_tags_key, false)

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
            return if (isDarkTheme) {
                R.style.AppTheme_ActionBar
            } else {
                R.style.AppTheme_Black_ActionBar
            }
        }

    val isDarkTheme: Boolean
        get() {
            return when (activeThemeString) {
                "dark", "black" -> true
                else -> false
            }
        }

    private val _widgetTheme by StringPreference(R.string.widget_theme_pref_key, "light_darkactionbar")
    val isDarkWidgetTheme: Boolean
        get() = _widgetTheme == "dark"

    private val _activeTheme by StringPreference(R.string.theme_pref_key, "light_darkactionbar")
    private val activeThemeString: String
        get() = LuaInterpreter.configTheme() ?: _activeTheme

    // Only used in Dropbox build
    @Suppress("unused")
    var fullDropBoxAccess by BooleanPreference(R.string.dropbox_full_access, true)

    val dateBarRelativeSize: Float
        get() {
            val dateBarSize by IntPreference(R.string.datebar_relative_size, 80)
            return dateBarSize / 100.0f
        }

    val showCalendar by BooleanPreference(R.string.ui_show_calendarview, false)

    val tasklistTextSize: Float?
        get() {
            val luaValue = LuaInterpreter.tasklistTextSize()
            if (luaValue != null) {
                return luaValue
            }
            val customSize by BooleanPreference(R.string.custom_font_size, false)
            if (!customSize) {
                return 14.0f
            }
            val font_size by IntPreference(R.string.font_size, 14)
            return font_size.toFloat()
        }

    val hasShareTaskShowsEdit by BooleanPreference(R.string.share_task_show_edit, false)

    val hasExtendedTaskView by BooleanPreference(R.string.taskview_extended_pref_key, true)

    val showCompleteCheckbox by BooleanPreference(R.string.ui_complete_checkbox, true)

    val showConfirmationDialogs by BooleanPreference(R.string.ui_show_confirmation_dialogs, true)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        when (s) {
            getString(R.string.widget_theme_pref_key),
            getString(R.string.widget_extended_pref_key),
            getString(R.string.widget_background_transparency),
            getString(R.string.widget_header_transparency) -> {
                TodoApplication.app.redrawWidgets()
            }
            getString(R.string.calendar_sync_dues),
            getString(R.string.calendar_sync_thresholds),
            getString(R.string.calendar_reminder_days),
            getString(R.string.calendar_reminder_time) -> {
                CalendarSync.updatedSyncTypes()
            }
        }
    }

    val defaultSorts: Array<String>
        get() = TodoApplication.app.resources.getStringArray(R.array.sortKeys)

    private var _todoFileName by StringOrNullPreference(R.string.todo_file_key)
    val todoFileName: String
        get() {
            var name = _todoFileName
            if (name == null) {
                name = FileStore.getDefaultPath()
                setTodoFile(name)
            }
            val todoFile = File(name)
            try {
                return todoFile.canonicalPath
            } catch (e: IOException) {
                return FileStore.getDefaultPath()
            }

        }

    val todoFile: File
        get() = File(todoFileName)

    fun setTodoFile(todo: String) {
        _todoFileName = todo
        prefs.edit().remove(getString(R.string.file_current_version_id)).apply()
    }

    fun clearCache() {
        todoList = null
        currentVersionId = null
    }

    val isAutoArchive by BooleanPreference(R.string.auto_archive_pref_key, false)

    val hasPrependDate by BooleanPreference(R.string.prepend_date_pref_key, true)

    val hasKeepSelection by BooleanPreference(R.string.keep_selection, false)

    val hasKeepPrio by BooleanPreference(R.string.keep_prio, true)

    val shareAppendText by StringPreference(R.string.share_task_append_text, " +background")

    var latestChangelogShown by IntPreference(R.string.latest_changelog_shown, 0)

    val localFileRoot by StringPreference(R.string.local_file_root, "/sdcard/")

    val hasColorDueDates by BooleanPreference(R.string.color_due_date_key, true)

    private var cachedContents by StringOrNullPreference(R.string.cached_todo_file)

    var todoList: CopyOnWriteArrayList<Task>?
        get() = cachedContents?.let {
            CopyOnWriteArrayList<Task>().apply {
                addAll(it.lines().map { line -> Task(line) })
            }
        }
        set(items) {
            if (items == null) {
                prefs.edit().remove(getString(R.string.cached_todo_file)).apply()
            } else {
                cachedContents = items.map { it.inFileFormat() }.joinToString("\n")
            }
        }
    val  useUUIDs by BooleanPreference(R.string.use_uuids, false)
}