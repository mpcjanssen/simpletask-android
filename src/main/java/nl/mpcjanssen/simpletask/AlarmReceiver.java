package nl.mpcjanssen.simpletask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import nl.mpcjanssen.simpletask.dao.gen.LogItemDao;


import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.INSTANCE.info(TAG, "Executing Alarm callback");
        TodoApplication m_app = (TodoApplication) context.getApplicationContext();

        // Clean up logging
        Date now = new Date();
        Date removeBefore = new Date(now.getTime()-24*60*60*1000);
        long oldLogCount = m_app.logDao.count();
        m_app.logDao.queryBuilder()
                .where(LogItemDao.Properties.Timestamp.lt(removeBefore))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
        long logCount = m_app.logDao.count();
        Logger.INSTANCE.info(TAG, "Cleared " + (oldLogCount-logCount) + " old log items");

        // Update UI (widgets and main screen)
        m_app.getLocalBroadCastManager().sendBroadcast(new Intent(Constants.BROADCAST_UPDATE_UI));
    }
}
