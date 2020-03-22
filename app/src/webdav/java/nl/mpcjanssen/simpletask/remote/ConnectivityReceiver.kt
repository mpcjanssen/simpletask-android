package nl.mpcjanssen.simpletask.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import nl.mpcjanssen.simpletask.util.broadcastUpdateStateIndicator

class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Connectivity changed {}" + intent)
        broadcastUpdateStateIndicator(TodoApplication.app.localBroadCastManager)
        if (FileStore.isOnline) {
            Log.i(TAG, "Device went online")
            broadcastFileSync(TodoApplication.app.localBroadCastManager)
        } else {
            Log.i(TAG, "Device no longer online")
        }
    }

    companion object {
        private val TAG = "ConnectivityReceiver"
    }
}
