package nl.mpcjanssen.simpletask

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver
import com.getpebble.android.kit.util.PebbleDictionary
import nl.mpcjanssen.simpletask.task.TodoList
import java.util.*

/**
 * Created by brandonknitter on 10/5/16.
 */


class PebbleReceiver(subscribedUuid: UUID?) : PebbleDataReceiver(subscribedUuid) {

    private var log = Logger

    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        log.debug(TAG, "receiveData...")

        // A new AppMessage was received, tell Pebble
        log.debug(TAG, "Sending ack...")
        PebbleKit.sendAckToPebble(context, transactionId);
        log.debug(TAG, "Ack sent")

        if (data?.getInteger(0) == 0L) {
            log.info(TAG, "Expected type MESSAGE_KEY_RequestData=0, sending data")
            val items = TodoList.todoItems
            // TODO(bk) apply filter/sort
            for (item in items) {
                val dict = PebbleDictionary()
                log.debug(TAG, "Sending task: "+item.task.text)
                dict.addString(AppKeyTaskName, item.task.text)
                PebbleKit.sendDataToPebble(context, appUuid, dict)
            }
        } else {
            log.info(TAG, "Unsupported request type, ignoring...")
        }
    }

    companion object {
        val appUuid = UUID.fromString("a6c5b8ef-0b0e-4db2-8ebe-32a363699065")

        val AppKeyTaskName=0;

        private val TAG = "PebbleReceiver"
    }

}
