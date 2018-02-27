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

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.SystemClock
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.dao.Daos
import nl.mpcjanssen.simpletask.dao.gen.TodoFile
import nl.mpcjanssen.simpletask.remote.BackupInterface
import nl.mpcjanssen.simpletask.remote.FileDialog
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.*
import java.io.File
import java.util.*

class TodoApplication : Application(),

        BackupInterface {

    private lateinit var androidUncaughtExceptionHandler: Thread.UncaughtExceptionHandler
    lateinit var localBroadCastManager: LocalBroadcastManager
    private lateinit var m_broadcastReceiver: BroadcastReceiver

    override fun onCreate() {
        app = this
        super.onCreate()

        localBroadCastManager = LocalBroadcastManager.getInstance(this)

        setupUncaughtExceptionHandler()

        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_UPDATE_UI)
        intentFilter.addAction(Constants.BROADCAST_UPDATE_WIDGETS)
        intentFilter.addAction(Constants.BROADCAST_FILE_SYNC)

        m_broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Logger.info(TAG, "Received broadcast ${intent.action}")
                if (intent.action == Constants.BROADCAST_UPDATE_UI) {
                    TodoList.todoQueue("Refresh external UI") {
                        CalendarSync.syncLater()
                        redrawWidgets()
                        updateWidgets()
                    }
                } else if (intent.action == Constants.BROADCAST_UPDATE_WIDGETS) {
                    Logger.info(TAG, "Refresh widgets from broadcast")
                    redrawWidgets()
                    updateWidgets()
                } else if (intent.action == Constants.BROADCAST_FILE_SYNC) {
                    loadTodoList("From BROADCAST_FILE_SYNC")
                }
            }
        }

        TodoActionQueue.start()
        FileStoreActionQueue.start()

        localBroadCastManager.registerReceiver(m_broadcastReceiver, intentFilter)
        Logger.info(TAG, "onCreate()")
        Logger.info(TAG, "Created todolist " + TodoList)
        Logger.info(TAG, "Started ${appVersion(this)}")
        scheduleOnNewDay()
        scheduleRepeating()
    }

    private fun setupUncaughtExceptionHandler() {
        // save original Uncaught exception handler
        androidUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        // Handle all uncaught exceptions for logging.
        // After that call the default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.error(TAG, "Uncaught exception", throwable)
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

        Logger.info(TAG, "Scheduling daily UI updateCache alarm, first at ${calendar.time}")
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra(Constants.ALARM_REASON_EXTRA, Constants.ALARM_NEW_DAY)
        val pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val am = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, pi)
    }

    private fun scheduleRepeating() {
        // Schedules background reload

        Logger.info(TAG, "Scheduling task list reload")
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra(Constants.ALARM_REASON_EXTRA, Constants.ALARM_RELOAD)
        val pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val am = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 15 * 60 * 1000,
                15 * 60 * 1000, pi)
    }

    override fun onTerminate() {
        Logger.info(TAG, "De-registered receiver")
        localBroadCastManager.unregisterReceiver(m_broadcastReceiver)
        super.onTerminate()
    }

    fun switchTodoFile(newTodo: String) {
        Config.setTodoFile(newTodo)
        loadTodoList("from file switch")
    }

    fun loadTodoList(reason: String) {
        Logger.info(TAG, "Loading todolist")
        TodoList.reload(this, reason = reason)
    }

    fun updateWidgets() {
        val mgr = AppWidgetManager.getInstance(applicationContext)
        for (appWidgetId in mgr.getAppWidgetIds(ComponentName(applicationContext, MyAppWidgetProvider::class.java))) {
            mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetlv)
            Logger.info(TAG, "Updating widget: " + appWidgetId)
        }
    }

    fun redrawWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, MyAppWidgetProvider::class.java))
        Logger.info(TAG, "Redrawing widgets ")
        if (appWidgetIds.isNotEmpty()) {
            MyAppWidgetProvider().onUpdate(this, appWidgetManager, appWidgetIds)
        }
    }

    val isAuthenticated: Boolean
        get() {
            return FileStore.isAuthenticated
        }

    fun startLogin(caller: Activity) {
        val loginActivity = FileStore.loginActivity()?.java
        loginActivity?.let {
            val intent = Intent(caller, it)
            caller.startActivity(intent)
        }
    }


    fun browseForNewFile(act: Activity) {
        val fileStore = FileStore
        FileDialog.browseForNewFile(
                act,
                fileStore,
                Config.todoFile.parent,
                object : FileDialog.FileSelectedListener {
                    override fun fileSelected(file: String) {
                        switchTodoFile(file)
                    }
                },
                Config.showTxtOnly)
    }

    val doneFileName: String
        get() = File(Config.todoFile.parentFile, "done.txt").absolutePath

    override fun backup(name: String, lines: List<Task>) {
        val now = Date()
        val fileToBackup = TodoFile(lines.map { it.inFileFormat() }.joinToString ("\n"), name, now)
        Daos.backup(fileToBackup)

    }

    fun getSortString(key: String): String {
        if (Config.useTodoTxtTerms) {
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

    companion object {
        private val TAG = TodoApplication::class.java.simpleName
        fun atLeastAPI(api: Int): Boolean = android.os.Build.VERSION.SDK_INT >= api
        lateinit var app : TodoApplication
    }

    var today: String = todayAsString
}

