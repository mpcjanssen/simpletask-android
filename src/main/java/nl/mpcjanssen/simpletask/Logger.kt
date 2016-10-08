package nl.mpcjanssen.simpletask

import android.os.Handler
import android.os.Looper
import android.util.Log

import nl.mpcjanssen.simpletask.dao.genlog.DaoMaster
import nl.mpcjanssen.simpletask.dao.genlog.LogItem
import nl.mpcjanssen.simpletask.dao.genlog.LogItemDao
import nl.mpcjanssen.simpletask.util.shortAppVersion
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*

/*
 * Small logging wrapper to be able to swap loggers without
 * changing any code.
 */

object Logger {

    init {
        // Initialize the event bus. Wrap in catch to prevent test suite errors.
        // (Event Bus doesn't work in non instrumentation tests)
        try {
            EventBus.getDefault().register(this);
        } catch (ex : RuntimeException) {
            // Do nothing
        }
    }

    val dao: LogItemDao by lazy {
        val logHelper = DaoMaster.DevOpenHelper(TodoApplication.app, "log.db", null)
        val logDb = logHelper.writableDatabase
        val logMaster = DaoMaster(logDb)
        val logSession = logMaster.newSession()
        logSession.logItemDao
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND) fun onMessageEvent(event: LogEvent) {
        var throwableMessage: String = ""
        event.throwable?.let {
            throwableMessage = Log.getStackTraceString(event.throwable)
        }
        val item = LogItem(null, Date(), event.severity, event.tag, event.s, throwableMessage)
        dao.insert(item)
    }


    fun logInDB(severity: String, tag: String, s: String, throwable: Throwable? = null) {
        EventBus.getDefault().post(LogEvent(severity,tag,s,throwable));
    }

    fun error(tag: String, s: String) {
        Log.e(tag,s)
        logInDB("e", tag,s)
    }
    fun warn(tag: String, s: String) {
        Log.w(tag,s)
        logInDB("w", tag,s)
    }

    fun info(tag: String, s: String) {
        Log.i(tag,s)
        logInDB("i", tag,s)
    }

    fun debug(tag: String, s: String) {
        Log.d(tag,s)
        logInDB("d", tag,s)
    }


    fun error(tag: String, s: String, throwable: Throwable) {
        Log.e(tag,s,throwable)
        logInDB("e", tag,s,throwable)
    }

    fun warn(tag: String, s: String, throwable: Throwable) {
        Log.w(tag,s, throwable)
        logInDB("w", tag,s, throwable)
    }

    fun info(tag: String, s: String, ex: Throwable) {
        Log.i(tag,s,ex)
        logInDB("i", tag,s,ex)
    }

    fun debug(tag: String, s: String, ex: Throwable) {
        Log.d(tag,s,ex)
        logInDB("d", tag,s,ex)
    }

    fun logItemsDesc () : List<String> {
        return dao.queryBuilder().orderDesc(LogItemDao.Properties.Id).list().map { it -> logItemToString(it) }
    }

    fun logAsText () : String {
        val logContents = StringBuilder()
        for (item in dao.loadAll()) {
            logContents.append(logItemToString(item)).append("\n")
        }
        return logContents.toString()
    }

    private fun logItemToString(entry: LogItem): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", Locale.US)
        return format.format(entry.timestamp) + "\t" + shortAppVersion() + "\t" + entry.severity + "\t" + entry.tag + "\t" + entry.message + "\t" + entry.exception
    }

    fun cleanLogging() {
        val now = Date()
        val removeBefore = Date(now.time - 24 * 60 * 60 * 1000)
        val oldLogCount = dao.count()
        dao.queryBuilder().where(LogItemDao.Properties.Timestamp.lt(removeBefore)).buildDelete().executeDeleteWithoutDetachingEntities()
        val logCount = dao.count()
        Logger.info(Logger.javaClass.simpleName, "Cleared " + (oldLogCount - logCount) + " old log items")
    }
}

data class LogEvent (val severity: String, val tag: String, val s: String, val throwable: Throwable? = null)


