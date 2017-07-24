package nl.mpcjanssen.simpletask.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.mpcjanssen.simpletask.Logger

class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val log = Logger
        log.debug(TAG, "Connectivity changed {}" + intent)
        FileStore.changedConnectionState()
    }

    companion object {

        private val TAG = "ConnectivityReceiver"
    }
}
