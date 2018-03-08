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
        val db = helper.writableDatabase
        val daoMaster = DaoMaster(db)
        daoSession = daoMaster.newSession()
        backupDao = daoSession.todoFileDao

    }

    fun backup (file : TodoFile) {
        backupDao.insertOrReplace(file)
        // Clean up old files
        val removeBefore = Date(Date().time - 2 * 24 * 60 * 60 * 1000)
        backupDao.queryBuilder().where(TodoFileDao.Properties.Date.lt(removeBefore)).buildDelete().executeDeleteWithoutDetachingEntities()
    }



    fun initHistoryCursor (): Cursor {
        val builder = daoSession.todoFileDao.queryBuilder()
        return builder.buildCursor().query()
    }
}
