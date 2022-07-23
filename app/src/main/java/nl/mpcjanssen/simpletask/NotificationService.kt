package nl.mpcjanssen.simpletask

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.showToastShort
import nl.mpcjanssen.simpletask.util.todayAsString
import nl.mpcjanssen.simpletask.util.broadcastTasklistChanged
import java.io.IOException
import android.app.Service
import android.app.NotificationManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationService : Service() {
    public override fun onCreate() {
        val builder = NotificationCompat.Builder(this, "pin-notifications")
            .setSmallIcon(R.drawable.ic_done_white_24dp)
            .setGroup("group")
            .setGroupSummary(true)
            .setOngoing(true)
        startForeground(1, builder.build())
    }

    public override fun onStartCommand (intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand()")
        val taskIds = intent.getStringArrayExtra(Constants.EXTRA_TASK_ID)
        Log.d(TAG, "got taskIds from extras ${taskIds?.joinToString(",")}")
        if (taskIds == null) {
            Log.e(TAG, "Extra ${Constants.EXTRA_TASK_ID} not found in intent")
            return START_STICKY_COMPATIBILITY
        }
        pinNotifications(taskIds)
        return START_REDELIVER_INTENT
    }

    private fun pinNotifications(taskIds: Array<String>) {
        for (id in taskIds) {
            val task = TodoApplication.todoList.getTaskWithId(id)
            if (task == null) {
                Log.e(TAG, "Task with id '$id' not found in todo list")
                continue
            }
            val taskIdHash = task.id.hashCode()
            val editTaskIntent = Intent(this, AddTask::class.java).let {
                it.putExtra(Constants.EXTRA_TASK_ID, task.id)
                PendingIntent.getActivity(this, taskIdHash, it, PendingIntent.FLAG_IMMUTABLE)
            }
            val markDoneIntent = Intent(this, MarkTaskDone::class.java).let {
                it.putExtra(Constants.EXTRA_TASK_ID, task.id)
                PendingIntent.getService(this, taskIdHash, it, PendingIntent.FLAG_IMMUTABLE)
            }
            var builder = NotificationCompat.Builder(this, "pin-notifications")
                .setSmallIcon(R.drawable.ic_done_white_24dp)
                .setContentTitle(task.text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(editTaskIntent)
                .addAction(R.drawable.ic_done_white_24dp, getString(R.string.done), markDoneIntent)
                .addExtras(Bundle().apply { putString(Constants.EXTRA_TASK_ID, task.id) })
                .setGroup("group")

            with(NotificationManagerCompat.from(this)) {
                notify(taskIdHash, builder.build())
            }
            if (!TodoApplication.config.hasKeepSelection) {
                TodoApplication.todoList.clearSelection()
            }
        }
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        val TAG = "NotificationService"
    
        fun removeNotifications(taskIds: List<String>) {
            taskIds.forEach{
                TodoApplication.notificationManager.cancel(it.hashCode())
            }
        }
    }
}
