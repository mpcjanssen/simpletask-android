package nl.mpcjanssen.simpletask

import android.util.Log

/*
 * Small logging wrapper to be able to swap loggers without
 * changing any code.
 */

object Logger {

    fun error(tag: String, s: String) {
        Log.e(tag,s)
    }
    fun warn(tag: String, s: String) {
        Log.w(tag,s)
    }

    fun info(tag: String, s: String) {
        Log.i(tag,s)
    }

    fun debug(tag: String, s: String) {
        Log.d(tag,s)
    }

    fun error(tag: String, s: String, ex: Exception) {
        Log.e(tag,s,ex)
    }
    fun warn(tag: String, s: String, ex: Exception) {
        Log.w(tag,s,ex)
    }

    fun info(tag: String, s: String, ex: Exception) {
        Log.i(tag,s,ex)
    }

    fun debug(tag: String, s: String, ex: Exception) {
        Log.d(tag,s,ex)
    }
}