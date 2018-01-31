package nl.mpcjanssen.simpletask

import android.os.Handler
import android.os.Looper
import android.util.Log
import nl.mpcjanssen.simpletask.dao.gen.LogItem
import nl.mpcjanssen.simpletask.dao.gen.LogItemDao
import java.util.*

/*
 * Small logging wrapper to be able to swap loggers without
 * changing any code.
 */

object Logger  {


    fun error(tag: String, s: String) {
        Log.e(tag, s)
    }
    fun warn(tag: String, s: String) {
        Log.w(tag, s)
    }

    fun info(tag: String, s: String) {
        Log.i(tag, s)
    }

    fun debug(tag: String, s: String) {
        Log.d(tag, s)
    }

    fun error(tag: String, s: String, throwable: Throwable) {
        Log.e(tag, s, throwable)
    }

    fun warn(tag: String, s: String, throwable: Throwable) {
        Log.w(tag, s, throwable)
    }

    fun info(tag: String, s: String, ex: Throwable) {
        Log.i(tag, s, ex)
    }

    fun debug(tag: String, s: String, ex: Throwable) {
        Log.d(tag, s, ex)
    }
}
