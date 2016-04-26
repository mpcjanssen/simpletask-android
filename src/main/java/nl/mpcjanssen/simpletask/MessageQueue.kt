package nl.mpcjanssen.simpletask

import android.os.Handler
import android.os.Looper
import nl.mpcjanssen.simpletask.task.TodoList
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



    fun queueRunnable(description: String, r: Runnable) {
        log.info(tag, "Handler: Queue " + description)
        while (mHandler == null) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }

        mHandler?.post(LoggingRunnable(tag, description, r))
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


