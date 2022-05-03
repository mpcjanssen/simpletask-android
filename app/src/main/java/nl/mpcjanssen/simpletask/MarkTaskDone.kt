package nl.mpcjanssen.simpletask

import android.app.Activity
import android.content.Intent
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
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat

class MarkTaskDone : Service() {
    val TAG = "MarkTaskDone"

    public override fun onStartCommand (intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        val taskId = intent.getStringExtra(Constants.EXTRA_TASK_ID)
        if (taskId == null) {
            Log.e(TAG, "'${Constants.EXTRA_TASK_ID}' not found in intent: $intent")
            return START_STICKY_COMPATIBILITY
        }
        Log.d(TAG, "task ID: $taskId")

        val todoList = TodoApplication.todoList
        val task = todoList.getTaskWithId(taskId)
        if (task == null ) {
            Log.e(TAG, "task with id '$taskId' not found in todo list")
            return START_STICKY_COMPATIBILITY
        }
        Log.d(TAG, task.text)
        task.markComplete(todayAsString)
        broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)
        with(NotificationManagerCompat.from(this)) {
            cancel(task.id.hashCode())
        }
        stopSelf()
        return START_STICKY_COMPATIBILITY
    }

    public override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
