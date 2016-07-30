package nl.mpcjanssen.simpletask.dao


import android.database.Cursor
import de.greenrobot.dao.converter.PropertyConverter
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.dao.gen.*
import nl.mpcjanssen.simpletask.task.Task
import java.text.SimpleDateFormat
import java.util.*

object Daos {
    internal val daoSession: DaoSession
    val logDao: LogItemDao
    val backupDao: TodoFileDao
    val todoItemDao: TodoItemDao
    init {
        val helper = DaoMaster.DevOpenHelper(TodoApplication.app, "TodoFiles_v1.db", null)
        val todoDb = helper.writableDatabase
        val daoMaster = DaoMaster(todoDb)
        daoSession = daoMaster.newSession()
        logDao = daoSession.logItemDao
        backupDao = daoSession.todoFileDao
        todoItemDao = daoSession.todoItemDao
        Logger.setDao(logDao)
    }

    fun backup (file : TodoFile) {
        backupDao.insertOrReplace(file)
        // Clean up old files
        val removeBefore = Date(Date().time - 2 * 24 * 60 * 60 * 1000)
        backupDao.queryBuilder().where(TodoFileDao.Properties.Date.lt(removeBefore)).buildDelete().executeDeleteWithoutDetachingEntities()
    }

    fun logItemsDesc () : List<String> {
        return logDao.queryBuilder().orderDesc(LogItemDao.Properties.Id).list().map { it -> logItemToString(it) }
    }

    fun logAsText () : String {
        val logContents = StringBuilder()
        for (item in logDao.loadAll()) {
            logContents.append(logItemToString(item)).append("\n")
        }
        return logContents.toString()
    }

    private fun logItemToString(entry: LogItem): String {
        val format = SimpleDateFormat("HH:mm:ss.S", Locale.US)
        return format.format(entry.timestamp) + "\t" + entry.severity + "\t" + entry.tag + "\t" + entry.message + "\t" + entry.exception
    }

    fun cleanLogging() {
        val now = Date()
        val removeBefore = Date(now.time - 24 * 60 * 60 * 1000)
        val oldLogCount = logDao.count()
        logDao.queryBuilder().where(LogItemDao.Properties.Timestamp.lt(removeBefore)).buildDelete().executeDeleteWithoutDetachingEntities()
        val logCount = logDao.count()
        Logger.info(Daos.javaClass.simpleName, "Cleared " + (oldLogCount - logCount) + " old log items")
    }

    fun initHistoryCursor (): Cursor {
        val builder = daoSession.todoFileDao.queryBuilder()
        return builder.buildCursor().query()
    }
}

class TaskPropertyConverter  : PropertyConverter<Task, String> {
    override fun convertToEntityProperty(databaseValue: String) : Task {
        return Task(databaseValue)
    }
    override fun convertToDatabaseValue(entityProperty : Task) : String {
        return entityProperty.inFileFormat()
    }
}