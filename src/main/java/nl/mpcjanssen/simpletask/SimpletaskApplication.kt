/**

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * Copyright (c) 2013- Mark Janssen
 * Copyright (c) 2015 Vojtech Kral

 * LICENSE:

 * Simpletas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Simpletask is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Sinpletask.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * *
 * @copyright 2013- Mark Janssen
 * *
 * @copyright 2015 Vojtech Kral
 */
package nl.mpcjanssen.simpletask

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.widget.EditText

import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.todayAsString
import java.io.File
import java.io.IOException
import java.util.*


class SimpletaskApplication : Application(),

        SharedPreferences.OnSharedPreferenceChangeListener, TodoList.TodoListChanged, FileStoreInterface.FileChangeListener, BackupInterface {

    lateinit private var androidUncaughtExceptionHandler: Thread.UncaughtExceptionHandler
    lateinit var localBroadCastManager: LocalBroadcastManager
    lateinit var  todoList: TodoList
    private lateinit var m_calSync: CalendarSync
    private lateinit var m_broadcastReceiver: BroadcastReceiver

    private val log = Logger

    internal lateinit var daoSession: DaoSession
    lateinit var  logDao: LogItemDao
    internal lateinit var backupDao: TodoFileDao
    lateinit var prefs: SharedPreferences

    override fun onCreate() {
        log.debug(TAG, "onCreate()")
        super.onCreate()
        localBroadCastManager = LocalBroadcastManager.getInstance(this)
        prefs  = PreferenceManager.getDefaultSharedPreferences(this);
        val helper = DaoMaster.DevOpenHelper(this, "TodoFiles_v1.db", null)
        val todoDb = helper.writableDatabase
        val daoMaster = DaoMaster(todoDb)
        daoSession = daoMaster.newSession()
        logDao = daoSession.logItemDao
        backupDao = daoSession.todoFileDao
        log.setDao(logDao)

        setupUncaughtExceptionHandler()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_UPDATE_UI)

        m_broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.BROADCAST_UPDATE_UI) {
                    m_calSync.syncLater()
                    redrawWidgets()
                    updateWidgets()
                }
            }
        }

        localBroadCastManager.registerReceiver(m_broadcastReceiver, intentFilter)
        prefsChangeListener(this)
        todoList = TodoList(this, this)
        this.mFileStore = FileStore(this, this)
        log.info(TAG, "Created todolist {}" + todoList)
        loadTodoList(true)
        m_calSync = CalendarSync(this, isSyncDues, isSyncThresholds)
        scheduleOnNewDay()
    }

    private fun setupUncaughtExceptionHandler() {
        // save original Uncaught exception handler
        androidUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        // Handle all uncaught exceptions for logging.
        // After that call the default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log.error(TAG, "Uncaught exception", throwable)
            androidUncaughtExceptionHandler.uncaughtException(thread, throwable)
        }
    }

    private fun scheduleOnNewDay() {
        // Schedules activities to run on a new day
        // - Refresh widgets and UI
        // - Cleans up logging

        val calendar = Calendar.getInstance()

        // Prevent alarm from triggering for today when setting it
        calendar.add(Calendar.DATE, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 2)
        calendar.set(Calendar.SECOND, 0)

        Logger.info(TAG, "Scheduling daily UI update alarm, first at ${calendar.time}")
        val pi = PendingIntent.getBroadcast(this, 0,
                Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        val am = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, pi)
    }

    fun prefsChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onTerminate() {
        log.info(TAG, "De-registered receiver")
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        localBroadCastManager.unregisterReceiver(m_broadcastReceiver)
        super.onTerminate()
    }


    val defaultSorts: Array<String>
        get() = resources.getStringArray(R.array.sortKeys)

    fun showCompleteCheckbox(): Boolean {
        return prefs.getBoolean(getString(R.string.ui_complete_checkbox), true)
    }

    fun showCalendar(): Boolean {
        return prefs.getBoolean(getString(R.string.ui_show_calendarview), false)
    }

    val listTerm: String
        get() {
            if (useTodoTxtTerms()) {
                return getString(R.string.context_prompt_todotxt)
            } else {
                return getString(R.string.context_prompt)
            }
        }

    val tagTerm: String
        get() {
            if (useTodoTxtTerms()) {
                return getString(R.string.project_prompt_todotxt)
            } else {
                return getString(R.string.project_prompt)
            }
        }

    private fun useTodoTxtTerms(): Boolean {
        return prefs.getBoolean(getString(R.string.ui_todotxt_terms), false)
    }

    fun useFastScroll(): Boolean {
        return prefs.getBoolean(getString(R.string.ui_fast_scroll), true)
    }

    fun showTxtOnly(): Boolean {
        return prefs.getBoolean(getString(R.string.show_txt_only), false)
    }

    val isSyncDues: Boolean
        get() = atLeastAPI(16) && prefs.getBoolean(getString(R.string.calendar_sync_dues), false)

    val isSyncThresholds: Boolean
        get() = atLeastAPI(16) && prefs.getBoolean(getString(R.string.calendar_sync_thresholds), false)

    val reminderDays: Int
        get() = prefs.getInt(getString(R.string.calendar_reminder_days), 1)

    val reminderTime: Int
        get() = prefs.getInt(getString(R.string.calendar_reminder_time), 720)


    val todoFileName: String
        get() {
            var name = prefs.getString(getString(R.string.todo_file_key), null)
            if (name == null) {
                name = FileStore.getDefaultPath(this)
                setTodoFile(name)
            }
            val todoFile = File(name)
            try {
                return todoFile.canonicalPath
            } catch (e: IOException) {
                return FileStore.getDefaultPath(this)
            }

        }

    val todoFile: File
        get() = File(todoFileName)

    @SuppressLint("CommitPrefEdits")
    fun setTodoFile(todo: String) {
        prefs.edit().putString(getString(R.string.todo_file_key), todo).commit()
    }

    val isAutoArchive: Boolean
        get() = prefs.getBoolean(getString(R.string.auto_archive_pref_key), false)

    fun hasPrependDate(): Boolean {
        return prefs.getBoolean(getString(R.string.prepend_date_pref_key), true)
    }

    fun hasKeepPrio(): Boolean {
        return prefs.getBoolean(getString(R.string.keep_prio), true)
    }

    val shareAppendText: String
        get() = prefs.getString(getString(R.string.share_task_append_text), "")

    val localFileRoot: String
        get() = prefs.getString(getString(R.string.local_file_root), "/sdcard/")

    fun hasCapitalizeTasks(): Boolean {
        return prefs.getBoolean(getString(R.string.capitalize_tasks), false)
    }

    fun hasColorDueDates(): Boolean {
        return prefs.getBoolean(getString(R.string.color_due_date_key), true)
    }

    fun hasLandscapeDrawers(): Boolean {
        return prefs.getBoolean(getString(R.string.ui_drawer_fixed_landscape), false) && resources.getBoolean(R.bool.is_landscape)
    }

    fun setEditTextHint(editText: EditText, resId: Int) {
        if (prefs.getBoolean(getString(R.string.ui_show_edittext_hints), true)) {
            editText.setHint(resId)
        }
    }

    var isAddTagsCloneTags: Boolean
        get() = prefs.getBoolean(getString(R.string.clone_tags_key), false)
        set(bool) = prefs.edit().putBoolean(getString(R.string.clone_tags_key), bool).apply()

    fun hasAppendAtEnd(): Boolean {
        return prefs.getBoolean(getString(R.string.append_tasks_at_end), true)
    }

    var isWordWrap: Boolean
        get() = prefs.getBoolean(getString(R.string.word_wrap_key), true)
        set(bool) = prefs.edit().putBoolean(getString(R.string.word_wrap_key), bool).apply()

    fun showTodoPath(): Boolean {
        return prefs.getBoolean(getString(R.string.show_todo_path), false)
    }


    fun backClearsFilter(): Boolean {
        return prefs.getBoolean(getString(R.string.back_clears_filter), false)
    }

    fun sortCaseSensitive(): Boolean {
        return prefs.getBoolean(getString(R.string.ui_sort_case_sensitive), true)
    }

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
            packageManager.getInstallerPackageName("nl.mpcjanssen.simpletask.donate")
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    val isLoading: Boolean
        get() = fileStore.isLoading

    fun loadTodoList(background: Boolean) {
        log.info(TAG, "Load todolist")
        todoList.reload(mFileStore, todoFileName, this, localBroadCastManager, background, eol)

    }

    fun updateWidgets() {
        val mgr = AppWidgetManager.getInstance(applicationContext)
        for (appWidgetId in mgr.getAppWidgetIds(ComponentName(applicationContext, MyAppWidgetProvider::class.java))) {
            mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetlv)
            log.info(TAG, "Updating widget: " + appWidgetId)
        }
    }

    private fun redrawWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, MyAppWidgetProvider::class.java))
        log.info(TAG, "Redrawing widgets ")
        if (appWidgetIds.size > 0) {
            MyAppWidgetProvider().onUpdate(this, appWidgetManager, appWidgetIds)
        }
    }

    private fun themeStringToId(theme: String): Int {
        when (theme) {
            "dark" -> return R.style.AppTheme
            "light_darkactionbar" -> return R.style.AppTheme_Light_DarkActionBar
        }
        return R.style.AppTheme_Light_DarkActionBar

    }

    val activeTheme: Int
        get() {
            return themeStringToId(prefs.getString(getString(R.string.theme_pref_key), ""))
        }

    val isDarkTheme: Boolean
        get() {
            when (activeThemeString) {
                "dark" -> return true
                else -> return false
            }
        }

    val isDarkWidgetTheme: Boolean
        get() = "dark" == prefs.getString(getString(R.string.widget_theme_pref_key), "light_darkactionbar")

    private val activeThemeString: String
        get() = prefs.getString(getString(R.string.theme_pref_key), "light_darkactionbar")

    var fullDropBoxAccess: Boolean
        @SuppressWarnings("unused")
        get() = prefs.getBoolean(getString(R.string.dropbox_full_access), true)
        set(full) {
            prefs.edit().putBoolean(getString(R.string.dropbox_full_access), full).commit()
        }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        if (s == getString(R.string.widget_theme_pref_key) ||
                s == getString(R.string.widget_extended_pref_key) ||
                s == getString(R.string.widget_background_transparency) ||
                s == getString(R.string.widget_header_transparency)) {
            redrawWidgets()
        } else if (s == getString(R.string.calendar_sync_dues)) {
            m_calSync.setSyncDues(isSyncDues)
        } else if (s == getString(R.string.calendar_sync_thresholds)) {
            m_calSync.setSyncThresholds(isSyncThresholds)
        } else if (s == getString(R.string.calendar_reminder_days) || s == getString(R.string.calendar_reminder_time)) {
            m_calSync.syncLater()
        }
    }

    fun switchTodoFile(newTodo: String, background: Boolean) {
        setTodoFile(newTodo)
        loadTodoList(background)

    }


    override fun todoListChanged() {
        log.info(TAG, "Tasks have changed, update UI")
        localBroadCastManager.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_UI))
    }

    val dateBarRelativeSize: Float
        get() {
            val def = 80
            return prefs.getInt(getString(R.string.datebar_relative_size), def)/100.0f
        }

    val activeFont: Int
        get() {
            if (!prefs.getBoolean(getString(R.string.custom_font_size), false)) {
                return R.style.FontSizeDefault
            }
            val font_size = prefs.getInt(getString(R.string.font_size), -1 )
            when (font_size) {
                12 -> return R.style.FontSize12sp
                14 -> return R.style.FontSize14sp
                16 -> return R.style.FontSize16sp
                18 -> return R.style.FontSize18sp
                20 -> return R.style.FontSize20sp
                22 -> return R.style.FontSize22sp
                24 -> return R.style.FontSize24sp
                26 -> return R.style.FontSize26sp
                28 -> return R.style.FontSize28sp
                30 -> return R.style.FontSize30sp
                32 -> return R.style.FontSize32sp
                34 -> return R.style.FontSize34sp
                36 -> return R.style.FontSize36sp
                else -> return R.style.FontSizeDefault
            }
        }

    val activeFontSize: Float
        get() {
            if (!prefs.getBoolean(getString(R.string.custom_font_size), false)) {
                return 14.0f
            }
            val font_size = prefs.getInt(getString(R.string.font_size), 14 )
            return font_size.toFloat()
        }

    fun showConfirmationDialog(cxt: Context, msgid: Int,
                               okListener: DialogInterface.OnClickListener, titleid: Int) {
        val show = prefs.getBoolean(getString(R.string.ui_show_confirmation_dialogs), true)

        val builder = AlertDialog.Builder(cxt)
        builder.setTitle(titleid)
        builder.setMessage(msgid)
        builder.setPositiveButton(android.R.string.ok, okListener)
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.setCancelable(true)
        val dialog = builder.create()
        if (show) {
            dialog.show()
        } else {
            okListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
        }
    }

    val isAuthenticated: Boolean
        get() {
            val fs = fileStore
            return fs.isAuthenticated
        }

    fun startLogin(caller: Activity) {
        fileStore.startLogin(caller)
    }

    fun storeType(): Int {
        return fileStore.type
    }


    val doneFileName: String
        get() = File(todoFile.parentFile, "done.txt").absolutePath

    override fun backup(name: String, contents: String) {

        val now = Date()
        val fileToBackup = TodoFile(contents, name, now)
        backupDao.insertOrReplace(fileToBackup)
        // Clean up old files
        val removeBefore = Date(now.time - 2 * 24 * 60 * 60 * 1000)

        backupDao.queryBuilder().where(TodoFileDao.Properties.Date.lt(removeBefore)).buildDelete().executeDeleteWithoutDetachingEntities()

    }

    fun getSortString(key: String): String {
        if (useTodoTxtTerms()) {
            if ("by_context" == key) {
                return getString(R.string.by_context_todotxt)
            }
            if ("by_project" == key) {
                return getString(R.string.by_project_todotxt)
            }
        }
        val keys = Arrays.asList(*resources.getStringArray(R.array.sortKeys))
        val values = resources.getStringArray(R.array.sort)
        val index = keys.indexOf(key)
        if (index == -1) {
            return getString(R.string.none)
        }
        return values[index]
    }

    fun hasShareTaskShowsEdit(): Boolean {
        return prefs.getBoolean(getString(R.string.share_task_show_edit), false)
    }

    fun hasExtendedTaskView(): Boolean {
        return prefs.getBoolean(getString(R.string.taskview_extended_pref_key), true)
    }

    companion object {

        private val TAG = SimpletaskApplication::class.java.simpleName
        fun atLeastAPI(api: Int) : Boolean =  android.os.Build.VERSION.SDK_INT >= api
    }

    var today: String = todayAsString
}
