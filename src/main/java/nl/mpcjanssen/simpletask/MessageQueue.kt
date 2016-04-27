package nl.mpcjanssen.simpletask

import android.os.Handler
import android.os.Looper
import nl.mpcjanssen.simpletask.task.TaskList
import nl.mpcjanssen.simpletask.util.log

class MessageQueue(val tag: String) : Thread() {
    private var mHandler: Handler? = null

    init {
        start()
    }
    override fun run(): Unit {
        Looper.prepare();
        mHandler = Handler() // the Handler hooks up to the current Thread
        Looper.loop();
    }



    fun add(description: String, r: Runnable, silent: Boolean = false) {
        while (mHandler == null) {
            if (!silent) {
                log.debug(tag, "Queue handler is null, waiting")
            }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
        if (!silent) {
            mHandler?.post(LoggingRunnable(tag, description, r))
        } else {
            mHandler?.post (r)
        }
    }


}

class LoggingRunnable (val tag : String, val description: String, val runnable: Runnable) : Runnable {

    init {
        log.info(tag, "Creating action " + description)
    }

    override fun toString(): String {
        return description
    }

    override fun run() {
        log.info(tag, "Execution action " + description)
        runnable.run()
    }

}


