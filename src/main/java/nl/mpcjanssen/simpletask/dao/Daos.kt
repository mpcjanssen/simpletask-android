package nl.mpcjanssen.simpletask.dao


import android.database.Cursor
import de.greenrobot.dao.converter.PropertyConverter

import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.dao.genbackup.DaoMaster as BackupDaoMaster
import nl.mpcjanssen.simpletask.dao.gentodo.DaoMaster as TodoDaoMaster
import nl.mpcjanssen.simpletask.dao.genlog.DaoMaster as LogDaoMaster

import nl.mpcjanssen.simpletask.dao.genbackup.TodoFile
import nl.mpcjanssen.simpletask.dao.genbackup.TodoFileDao


import nl.mpcjanssen.simpletask.dao.gentodo.TodoItemDao
import nl.mpcjanssen.simpletask.task.Task


import java.util.*

object Daos {
    val backupDao: TodoFileDao by lazy {
        val helper = BackupDaoMaster.DevOpenHelper(TodoApplication.app, "TodoFiles_v1.db", null)
        val db = helper.writableDatabase
        val master = BackupDaoMaster(db)
        val session = master.newSession()
        session.todoFileDao
    }
    val todoItemDao: TodoItemDao by lazy {
        val helper = TodoDaoMaster.DevOpenHelper(TodoApplication.app, "todolist.db", null)
        val db = helper.writableDatabase
        val master = TodoDaoMaster(db)
        val session = master.newSession()
        session.todoItemDao
    }



    fun backup (file : TodoFile) {
        backupDao.insertOrReplace(file)
        // Clean up old files
        val removeBefore = Date(Date().time - 2 * 24 * 60 * 60 * 1000)
        backupDao.queryBuilder().where(TodoFileDao.Properties.Date.lt(removeBefore)).buildDelete().executeDeleteWithoutDetachingEntities()
    }



    fun initHistoryCursor (): Cursor {
        val builder = backupDao.queryBuilder()
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