package nl.mpcjanssen.simpletask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.mpcjanssen.simpletask.dao.gen.LogItemDao
import nl.mpcjanssen.simpletask.util.todayAsString


import java.util.Date

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.info(TAG, "Executing Alarm callback")
        val m_app = context.applicationContext as SimpletaskApplication

        // Clean up logging
        val now = Date()
        val removeBefore = Date(now.time - 24 * 60 * 60 * 1000)
        val oldLogCount = m_app.logDao.count()
        m_app.today = todayAsString
        m_app.logDao.queryBuilder().where(LogItemDao.Properties.Timestamp.lt(removeBefore)).buildDelete().executeDeleteWithoutDetachingEntities()
        val logCount = m_app.logDao.count()
        Logger.info(TAG, "Cleared " + (oldLogCount - logCount) + " old log items")

        // Update UI (widgets and main screen)
        m_app.localBroadCastManager.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_UI))
    }

    companion object {
        private val TAG = AlarmReceiver::class.java.simpleName
    }
}
