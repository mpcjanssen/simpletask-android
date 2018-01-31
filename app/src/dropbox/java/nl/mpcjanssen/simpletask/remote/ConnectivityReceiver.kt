package nl.mpcjanssen.simpletask.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import nl.mpcjanssen.simpletask.util.broadcastRefreshUI

class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val lbm = LocalBroadcastManager.getInstance(context)
        val log = Logger
        log.debug(TAG, "Connectivity changed {}" + intent)
        broadcastRefreshUI(lbm)
        if (FileStore.isOnline) {
            log.info(TAG, "Device went online")
            broadcastFileSync(lbm)
        } else {
            log.info(TAG, "Device no longer online")
        }
    }

    companion object {

        private val TAG = "ConnectivityReceiver"
    }
}
