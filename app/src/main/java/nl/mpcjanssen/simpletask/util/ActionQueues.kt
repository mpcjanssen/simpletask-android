package nl.mpcjanssen.simpletask.util

import android.util.Log
import org.jetbrains.anko.doAsync


open class ActionQueue(private val qName: String) : Thread() {


    fun add(description: String, r: () -> Unit) {
        Log.i(qName, "-> $description")
        doAsync {
            Log.i(qName, "<- $description")
            r.invoke()
        }
    }
}

object FileStoreActionQueue : ActionQueue("FSQ")



