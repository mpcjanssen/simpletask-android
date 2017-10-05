package nl.mpcjanssen.simpletask.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.task.TodoList.queue

class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val log = Logger
        log.debug(TAG, "Connectivity changed {}" + intent)
        queue("Changed connection state") { FileStore.changedConnectionState()}
    }

    companion object {

        private val TAG = "ConnectivityReceiver"
    }
}
