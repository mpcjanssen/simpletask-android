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
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.SystemClock
import androidx.multidex.MultiDexApplication
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log
import androidx.room.Room
import nl.mpcjanssen.simpletask.dao.AppDatabase
import nl.mpcjanssen.simpletask.dao.DB_FILE
import nl.mpcjanssen.simpletask.dao.TodoFile

import nl.mpcjanssen.simpletask.remote.BackupInterface
import nl.mpcjanssen.simpletask.remote.FileDialog
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoItem
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.task.inFileFormat
import nl.mpcjanssen.simpletask.util.*
import java.io.File
import java.util.*

class TodoApplication : MultiDexApplication() {

    private lateinit var androidUncaughtExceptionHandler: Thread.UncaughtExceptionHandler
    lateinit var localBroadCastManager: LocalBroadcastManager
    private lateinit var m_broadcastReceiver: BroadcastReceiver


    override fun onCreate() {
        super.onCreate()
        app = this
        config = Config(app)
        todoList = TodoList(config.todoList?.map { TodoItem(it) } ?: emptyList())
        db = Room.databaseBuilder(this,
                AppDatabase::class.java, DB_FILE).fallbackToDestructiveMigration()
                .build()
        if (config.forceEnglish) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }

        localBroadCastManager = LocalBroadcastManager.getInstance(this)

        setupUncaughtExceptionHandler()

        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_UPDATE_WIDGETS)
        intentFilter.addAction(Constants.BROADCAST_FILE_SYNC)
        intentFilter.addAction(Constants.BROADCAST_TASKLIST_CHANGED)

        m_broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "Received broadcast ${intent.action}")
                when {
                    intent.action == Constants.BROADCAST_TASKLIST_CHANGED -> {
                        CalendarSync.syncLater()
                        redrawWidgets()
                        updateWidgets()
                    }
                    intent.action == Constants.BROADCAST_UPDATE_WIDGETS -> {
                        Log.i(TAG, "Refresh widgets from broadcast")
                        redrawWidgets()
                        updateWidgets()
                    }
                    intent.action == Constants.BROADCAST_FILE_SYNC -> loadTodoList("From BROADCAST_FILE_SYNC")
                }
            }
        }
        FileStoreActionQueue.start()

        localBroadCastManager.registerReceiver(m_broadcastReceiver, intentFilter)
        Log.i(TAG, "onCreate()")
        Log.i(TAG, "Created todolist $todoList")
        Log.i(TAG, "Started ${appVersion(this)}")
        scheduleOnNewDay()
        scheduleRepeating()
    }

    private fun setupUncaughtExceptionHandler() {
        // save original Uncaught exception handler
        androidUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        // Handle all uncaught exceptions for logging.
        // After that call the default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception", throwable)
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

        Log.i(TAG, "Scheduling daily UI updateCache alarm, first at ${calendar.time}")
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra(Constants.ALARM_REASON_EXTRA, Constants.ALARM_NEW_DAY)
        val pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val am = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, pi)
    }

    private fun scheduleRepeating() {
        // Schedules background reload

        Log.i(TAG, "Scheduling task list reload")
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra(Constants.ALARM_REASON_EXTRA, Constants.ALARM_RELOAD)
        val pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val am = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 15 * 60 * 1000,
                15 * 60 * 1000, pi)
    }

    override fun onTerminate() {
        Log.i(TAG, "De-registered receiver")
        localBroadCastManager.unregisterReceiver(m_broadcastReceiver)
        super.onTerminate()
    }

    fun switchTodoFile(newTodo: String) {
        config.setTodoFile(newTodo)
        todoList = loadTodoList("from file switch")
    }


    fun loadTodolist(reason: String): TodoList {
        Log.i(TAG, "Loading todolist")
        Log.d(TAG, "Reload: $reason")
        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
        if (!FileStore.isAuthenticated) {
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
            return TodoList(emptyList())
        }
        val filename = config.todoFileName
        if (config.changesPending && FileStore.isOnline) {
            Log.i(TAG, "Not loading, changes pending")
            Log.i(TAG, "Saving instead of loading")
            save(FileStore, filename, true, config.eol)
            return todoList
        } else {
            FileStoreActionQueue.add("Reload") {
                val needSync = try {
                    val newerVersion = FileStore.getRemoteVersion(config.todoFileName)
                    newerVersion != config.lastSeenRemoteId
                } catch (e: Throwable) {
                    Log.e(TAG, "Can't determine remote file version", e)
                    false
                }
                if (needSync) {
                    Log.i(TAG, "Remote version is different, sync")
                } else {
                    Log.i(TAG, "Remote version is same, load from cache")
                }
                val cachedList = config.todoList
                if (cachedList == null || needSync) {
                    try {
                        val remoteContents = FileStore.loadTasksFromFile(filename)
                        val items = ArrayList<Task>(
                                remoteContents.contents.map { text ->
                                    Task(text)
                                })

                        Log.d(TAG, "Fill todolist")
                        todoItems.clear()
                        todoItems.addAll(items)
                        // Update cache
                        config.cachedContents = remoteContents.contents.joinToString("\n")
                        config.lastSeenRemoteId = remoteContents.remoteId
                        // Backup
                        Backupper.backup(filename, items.map { it.inFileFormat(config.useUUIDs) })
                        notifyTasklistChanged(filename, false, true)
                    } catch (e: Exception) {
                        Log.e(TAG, "TodoList load failed: {}" + filename, e)
                        showToastShort(TodoApplication.app, "Loading of todo file failed")
                    }

                    Log.i(TAG, "TodoList loaded from dropbox")
                }
            }
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
        }
    }


    fun updateWidgets() {
        val mgr = AppWidgetManager.getInstance(applicationContext)
        for (appWidgetId in mgr.getAppWidgetIds(ComponentName(applicationContext, MyAppWidgetProvider::class.java))) {
            mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetlv)
            Log.i(TAG, "Updating widget: $appWidgetId")
        }
    }

    fun redrawWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, MyAppWidgetProvider::class.java))
        Log.i(TAG, "Redrawing widgets ")
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
                config.todoFile.parent,
                object : FileDialog.FileSelectedListener {
                    override fun fileSelected(file: String) {
                        switchTodoFile(file)
                    }
                },
                config.showTxtOnly)
    }



    companion object {
        private val TAG = TodoApplication::class.java.simpleName
        fun atLeastAPI(api: Int): Boolean = android.os.Build.VERSION.SDK_INT >= api
        lateinit var app : TodoApplication
        lateinit var config : Config
        lateinit var todoList: TodoList
        lateinit var db : AppDatabase
    }

    var today: String = todayAsString
}


object Backupper : BackupInterface {
    override fun backup(name: String, lines: List<String>) {
        val now = Date().time
        val fileToBackup = TodoFile(lines.joinToString ("\n"), name, now)
        val dao =  TodoApplication.db.todoFileDao()
        if(dao.insert(fileToBackup) == -1L) {
            dao.update(fileToBackup)
        }
        dao.removeBefore( now - 2 * 24 * 60 * 60 * 1000)
    }
}
