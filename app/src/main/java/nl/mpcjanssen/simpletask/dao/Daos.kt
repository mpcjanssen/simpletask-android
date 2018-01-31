package nl.mpcjanssen.simpletask.dao

import android.database.Cursor
import de.greenrobot.dao.converter.PropertyConverter
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.dao.gen.*
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.shortAppVersion
import java.text.SimpleDateFormat

import java.util.*

object Daos {
    internal val daoSession: DaoSession
    val backupDao: TodoFileDao
    init {
        val helper = DaoMaster.DevOpenHelper(TodoApplication.app, "TodoFiles_v1.db", null)
        val logDb = helper.writableDatabase
        val daoMaster = DaoMaster(logDb)
        daoSession = daoMaster.newSession()
        backupDao = daoSession.todoFileDao

    }

    fun backup (file : TodoFile) {
        backupDao.insertOrReplace(file)
        // Clean up old files
        val removeBefore = Date(Date().time - 2 * 24 * 60 * 60 * 1000)
        backupDao.queryBuilder().where(TodoFileDao.Properties.Date.lt(removeBefore)).buildDelete().executeDeleteWithoutDetachingEntities()
    }


    private fun logItemToString(entry: LogItem): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", Locale.US)
        return format.format(entry.timestamp) + "\t" + shortAppVersion() + "\t" + entry.severity + "\t" + entry.tag + "\t" + entry.message + "\t" + entry.exception
    }


    fun initHistoryCursor (): Cursor {
        val builder = daoSession.todoFileDao.queryBuilder()
        return builder.buildCursor().query()
    }
}

class TaskPropertyConverter : PropertyConverter<Task, String> {
    override fun convertToEntityProperty(databaseValue: String) : Task {
        return Task(databaseValue)
    }
    override fun convertToDatabaseValue(entityProperty : Task) : String {
        return entityProperty.inFileFormat()
    }
}