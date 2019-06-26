package nl.mpcjanssen.simpletask.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import nl.mpcjanssen.simpletask.util.broadcastUpdateStateIndicator

class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val lbm = LocalBroadcastManager.getInstance(context)
        Log.d(TAG, "Connectivity changed {}" + intent)
        broadcastUpdateStateIndicator(lbm)
        if (FileStore.isOnline) {
            Log.i(TAG, "Device went online")
            broadcastFileSync(lbm)
        } else {
            Log.i(TAG, "Device no longer online")
        }
    }

    companion object {

        private val TAG = "ConnectivityReceiver"
    }
}
