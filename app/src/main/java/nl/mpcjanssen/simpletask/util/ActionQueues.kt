package nl.mpcjanssen.simpletask.util

import android.os.Handler
import android.os.Looper


open class ActionQueue(val qName: String) : Thread() {
    private var mHandler: Handler? = null
    private val TAG = "ActionQueue"
    private val queueContents = ArrayList<String>()

    init {
        start()
    }

    override fun run(): Unit {
        Looper.prepare()
        mHandler = Handler() // the Handler hooks up to the current Thread
        Looper.loop()
    }

    fun hasPending(): Boolean {
        return mHandler?.hasMessages(0) ?: false
    }

    fun add(description: String, r: Runnable) {
        while (mHandler == null) {

            log.debug(TAG, "Queue handler is null, waiting")

            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        mHandler?.post(LoggingRunnable("$qName: $description", r, queueContents))
        queueContents.add(description)
        log.debug(TAG, "$qName == ${queueContents}")

    }
}

object TodoActionQueue : ActionQueue("TLQ")

object FileStoreActionQueue : ActionQueue("FSQ")

class LoggingRunnable(val description: String, val runnable: Runnable, val queueContents: ArrayList<String>) : Runnable {
    private val TAG = TodoActionQueue::class.java.simpleName

    override fun toString(): String {
        return description
    }

    override fun run() {
        if (queueContents.isNotEmpty()) queueContents.removeAt(0)
        log.info(TAG, description)
        runnable.run()
    }
}


