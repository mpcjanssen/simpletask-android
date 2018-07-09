package nl.mpcjanssen.simpletask.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import nl.mpcjanssen.simpletask.util.broadcastUpdateStateIndicator

class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val log = Logger
        log.debug(TAG, "Connectivity changed {}" + intent)

        broadcastUpdateStateIndicator(TodoApplication.app.localBroadCastManager)
        if (FileStore.isOnline) {
            log.info(TAG, "Device went online")
            broadcastFileSync(TodoApplication.app.localBroadCastManager)
        } else {
            log.info(TAG, "Device no longer online")
        }

    }

    companion object {

        private val TAG = "ConnectivityReceiver"
    }
}
