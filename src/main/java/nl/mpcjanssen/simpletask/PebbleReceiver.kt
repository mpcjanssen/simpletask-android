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


        val line_val=data?.getInteger(AppKeyRequestData);
        if (line_val == null || line_val > Int.MAX_VALUE) {
            log.warn(TAG, "Line number null or too large, cannot send to Pebble: " + line_val)

            // A new AppMessage was received, tell Pebble rejected
            log.debug(TAG, "Sending ack...")
            PebbleKit.sendNackToPebble(context, transactionId);
            log.debug(TAG, "Ack sent")
        } else {
            val line: Int = line_val.toInt()
            log.info(TAG, "Expected type MESSAGE_KEY_RequestData=0, sending data for row " + line);
            val items = TodoList.todoItems
            // TODO(bk) apply filter/sort

            if (line >= items.size) {
                log.debug(TAG, "No more items to send...")

                // A new AppMessage was received, tell Pebble
                log.debug(TAG, "Sending ack...")
                PebbleKit.sendNackToPebble(context, transactionId);
                log.debug(TAG, "Nack sent")


                return;
            }

            // A new AppMessage was received, tell Pebble
            log.debug(TAG, "Sending ack...")
            PebbleKit.sendAckToPebble(context, transactionId);
            log.debug(TAG, "Ack sent")


            val item = items.get(line);
            val dict = PebbleDictionary()

            val text = item.task.text

            dict.addInt32(AppKeyReceiveLine, line);
            dict.addString(AppKeyReceiveName, text)

            val pebbleIsConnected = PebbleKit.isWatchConnected(context)
            val pebbleAppMessageSupported = PebbleKit.areAppMessagesSupported(context)
            log.debug(TAG, "Is watch connected? " + pebbleIsConnected)
            log.debug(TAG, "Is message supported? " + pebbleAppMessageSupported)

            log.debug(TAG, "Sending task: " + item.line + "=" + item.task.text+" with UUID: "+ appUuid);
            PebbleKit.sendDataToPebble(context, appUuid, dict)
        }
    }

    companion object {
        val appUuid = UUID.fromString("a6c5b8ef-0b0e-4db2-8ebe-32a363699065")

        val AppKeyRequestData=0;

        val AppKeyReceiveLine=0;
        val AppKeyReceiveName=1;

        private val TAG = "PebbleReceiver"
    }

}
