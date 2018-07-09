package nl.mpcjanssen.simpletask.util

import android.os.Handler
import android.os.Looper


open class ActionQueue(val qName: String) : Thread() {
    private var mHandler: Handler? = null
    private val TAG = "ActionQueue"

    override fun run()  {
        Looper.prepare()
        mHandler = Handler() // the Handler hooks up to the current Thread
        Looper.loop()
    }

    fun hasPending(): Boolean {
        return mHandler?.hasMessages(0) ?: false
    }

    fun add(description: String, r: () -> Unit) {
        while (mHandler == null) {

            log.debug(TAG, "Queue handler is null, waiting")

            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        log.info(qName, "-> $description")
        mHandler?.post(LoggingRunnable(qName,description, Runnable(r)))
    }
}

object FileStoreActionQueue : ActionQueue("FSQ")

class LoggingRunnable(val queuName : String, val description: String, val runnable: Runnable) : Runnable {

    override fun toString(): String {
        return description
    }

    override fun run() {
        log.info(queuName, "<- $description")
        runnable.run()
    }
}


