package nl.mpcjanssen.simpletask

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import nl.mpcjanssen.simpletask.dao.LogItem
import nl.mpcjanssen.simpletask.dao.LogItemDao
import java.util.*

/*
 * Small logging wrapper to be able to swap loggers without
 * changing any code.
 */

object Logger {
    private var dao: LogItemDao? = null

    fun setDao (dao: LogItemDao) {

        this.dao = dao
    }

    fun logInDB(severity: String, tag: String, s: String, ex: Exception? = null) {
        val item = LogItem(Date(),severity, tag,s,ex?.message?:"")
        dao?.insert(item)
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
    }

    fun error(tag: String, s: String, ex: Exception) {
        Log.e(tag,s,ex)
        logInDB("e", tag,s,ex)
    }
    fun warn(tag: String, s: String, ex: Exception) {
        Log.w(tag,s,ex)
        logInDB("w", tag,s,ex)
    }

    fun info(tag: String, s: String, ex: Exception) {
        Log.i(tag,s,ex)
        logInDB("i", tag,s,ex)
    }

    fun debug(tag: String, s: String, ex: Exception) {
        Log.d(tag,s,ex)
    }
}