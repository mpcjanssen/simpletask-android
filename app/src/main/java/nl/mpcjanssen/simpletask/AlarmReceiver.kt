package nl.mpcjanssen.simpletask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.dao.Daos
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import nl.mpcjanssen.simpletask.util.broadcastRefreshUI

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val lbm = LocalBroadcastManager.getInstance(context)
        Logger.info(TAG, "Executing Alarm callback")
        if (Constants.ALARM_RELOAD == intent.getStringExtra(Constants.ALARM_REASON_EXTRA)) {
            broadcastFileSync( lbm )
        } else {
            broadcastRefreshUI(lbm)
        }

    }

    companion object {
        private val TAG = AlarmReceiver::class.java.simpleName
    }
}
