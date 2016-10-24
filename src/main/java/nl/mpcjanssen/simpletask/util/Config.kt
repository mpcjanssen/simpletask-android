package nl.mpcjanssen.simpletask.util

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.widget.EditText
import nl.mpcjanssen.simpletask.CalendarSync
import nl.mpcjanssen.simpletask.LuaInterpreter
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.remote.FileStore
import java.io.File
import java.io.IOException

object Config : SharedPreferences.OnSharedPreferenceChangeListener {
    val prefs = PreferenceManager.getDefaultSharedPreferences(TodoApplication.app)!!
    val interpreter = LuaInterpreter

    val TAG = "LuaConfig"

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    val useTodoTxtTerms: Boolean
        get() =  prefs.getBoolean(getString(R.string.ui_todotxt_terms), false)

    val showTxtOnly: Boolean
        get() =  prefs.getBoolean(getString(R.string.show_txt_only), false)

    val isSyncDues: Boolean
        get() = TodoApplication.atLeastAPI(16) && prefs.getBoolean(getString(R.string.calendar_sync_dues), false)

    val isSyncThresholds: Boolean
        get() = TodoApplication.atLeastAPI(16) && prefs.getBoolean(getString(R.string.calendar_sync_thresholds), false)

    val reminderDays: Int
        get() = prefs.getInt(getString(R.string.calendar_reminder_days), 1)

    val reminderTime: Int
        get() = prefs.getInt(getString(R.string.calendar_reminder_time), 720)

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

    var currentVersionId : String?
        get() = prefs.getString(getString(R.string.file_current_version_id), null)
        set(version) {
            prefs.edit().putString(getString(R.string.file_current_version_id), version).commit()
        }

    var lastScrollPosition : Int
        get() = prefs.getInt(getString(R.string.ui_last_scroll_position), -1)
        set(position) {
            prefs.edit().putInt(getString(R.string.ui_last_scroll_position), position).commit()
        }

    var lastScrollOffset : Int
        get() = prefs.getInt(getString(R.string.ui_last_scroll_offset), -1)
        set(position) {
            prefs.edit().putInt(getString(R.string.ui_last_scroll_offset), position).commit()
        }


    var luaConfig: String
        get() = prefs.getString(getString(R.string.lua_config), "")
        set(config) {
            prefs.edit().putString(getString(R.string.lua_config), config).commit()
        }

    var isWordWrap: Boolean
        get() = prefs.getBoolean(getString(R.string.word_wrap_key), true)
        set(bool) = prefs.edit().putBoolean(getString(R.string.word_wrap_key), bool).apply()

    var isShowEditTextHint: Boolean
        get() = prefs.getBoolean(getString(R.string.show_edittext_hint), true)
        set(bool) = prefs.edit().putBoolean(getString(R.string.show_edittext_hint), bool).apply()

    var isCapitalizeTasks: Boolean
        get() = prefs.getBoolean(getString(R.string.capitalize_tasks), true)
        set(bool) = prefs.edit().putBoolean(getString(R.string.capitalize_tasks), bool).apply()

    val showTodoPath: Boolean
        get() =  prefs.getBoolean(getString(R.string.show_todo_path), false)


    val backClearsFilter: Boolean
        get() =  prefs.getBoolean(getString(R.string.back_clears_filter), false)

    val sortCaseSensitive: Boolean
        get() =  prefs.getBoolean(getString(R.string.ui_sort_case_sensitive), true)

    val eol: String
        get() {
            if (prefs.getBoolean(getString(R.string.line_breaks_pref_key), true)) {
                return "\r\n"
            } else {
                return "\n"
            }
        }

    fun hasDonated(): Boolean {
        try {
            TodoApplication.app.packageManager.getInstallerPackageName("nl.mpcjanssen.simpletask.donate")
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    var isAddTagsCloneTags: Boolean
        get() = prefs.getBoolean(getString(R.string.clone_tags_key), false)
        set(bool) = prefs.edit().putBoolean(getString(R.string.clone_tags_key), bool).apply()

    val hasAppendAtEnd: Boolean
        get() =  prefs.getBoolean(getString(R.string.append_tasks_at_end), true)

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

    val isDarkWidgetTheme: Boolean
        get() = "dark" == prefs.getString(getString(R.string.widget_theme_pref_key), "light_darkactionbar")

    private val activeThemeString: String
        get() = interpreter.configTheme() ?: prefs.getString(getString(R.string.theme_pref_key), "light_darkactionbar")

    // Only used in Dropbox build
    @Suppress("unused")
    var fullDropBoxAccess: Boolean
        @SuppressWarnings("unused")
        get() = prefs.getBoolean(getString(R.string.dropbox_full_access), true)
        set(full) {
            prefs.edit().putBoolean(getString(R.string.dropbox_full_access), full).commit()
        }

    val dateBarRelativeSize: Float
        get() {
            val def = 80
            return prefs.getInt(getString(R.string.datebar_relative_size), def) / 100.0f
        }

    val showCalendar: Boolean
        get() =  prefs.getBoolean(getString(R.string.ui_show_calendarview), false)

    val tasklistTextSize: Float?
        get() {
            val luaValue = interpreter.tasklistTextSize()
            if (luaValue != null) {
                return luaValue
            }

            if (!prefs.getBoolean(getString(R.string.custom_font_size), false)) {
                return 14.0f
            }
            val font_size = prefs.getInt(getString(R.string.font_size), 14)
            return font_size.toFloat()
        }

    val hasShareTaskShowsEdit: Boolean
        get() =  prefs.getBoolean(getString(R.string.share_task_show_edit), false)

    val hasExtendedTaskView: Boolean
        get() =  prefs.getBoolean(getString(R.string.taskview_extended_pref_key), true)


    val showConfirmationDialogs : Boolean
        get() =  prefs.getBoolean(getString(R.string.ui_show_confirmation_dialogs), true)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        if (s == getString(R.string.widget_theme_pref_key) ||
                s == getString(R.string.widget_extended_pref_key) ||
                s == getString(R.string.widget_background_transparency) ||
                s == getString(R.string.widget_header_transparency)) {
            TodoApplication.app.redrawWidgets()
        } else if (s == getString(R.string.calendar_sync_dues)) {
            CalendarSync.setSyncDues(isSyncDues)
        } else if (s == getString(R.string.calendar_sync_thresholds)) {
            CalendarSync.setSyncThresholds(isSyncThresholds)
        } else if (s == getString(R.string.calendar_reminder_days) || s == getString(R.string.calendar_reminder_time)) {
            CalendarSync.syncLater()
        }
    }



    val defaultSorts: Array<String>
        get() = TodoApplication.app.resources.getStringArray(R.array.sortKeys)

    val todoFileName: String
        get() {
            var name = prefs.getString(getString(R.string.todo_file_key), null)
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

    @SuppressLint("CommitPrefEdits")
    fun setTodoFile(todo: String) {
        val edit = prefs.edit()
        edit.putString(getString(R.string.todo_file_key), todo)
        edit.remove(getString(R.string.file_current_version_id))
        edit.commit()
    }

    val isAutoArchive: Boolean
        get() = prefs.getBoolean(getString(R.string.auto_archive_pref_key), false)

    val hasPrependDate: Boolean
        get() =  prefs.getBoolean(getString(R.string.prepend_date_pref_key), true)

    val hasKeepPrio: Boolean
        get() =  prefs.getBoolean(getString(R.string.keep_prio), true)

    val shareAppendText: String
        get() = prefs.getString(getString(R.string.share_task_append_text), "")

    var latestChangelogShown: Int
        get() = prefs.getInt(getString(R.string.latest_changelog_shown), 0)
        set(versionCode: Int) {
            prefs.edit().putInt(getString(R.string.latest_changelog_shown), versionCode).commit()
        }

    val localFileRoot: String
        get() = prefs.getString(getString(R.string.local_file_root), "/sdcard/")

    val hasCapitalizeTasks: Boolean
        get() = isCapitalizeTasks

    val hasColorDueDates: Boolean
        get() =  prefs.getBoolean(getString(R.string.color_due_date_key), true)
}
