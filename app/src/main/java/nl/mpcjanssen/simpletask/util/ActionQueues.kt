package nl.mpcjanssen.simpletask.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.jetbrains.anko.doAsync


open class ActionQueue(val qName: String) : Thread() {


    fun add(description: String, r: () -> Unit) {
        Log.i(qName, "-> $description")
        doAsync {
            Log.i(qName, "<- $description")
            r.invoke()
        }
    }
}

object FileStoreActionQueue : ActionQueue("FSQ")
object BackupQueue : ActionQueue("BQ")



