package nl.mpcjanssen.simpletask

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import java.util.*

/**
 * Created by brandonknitter on 10/9/16.
 */

class PebbleAckReceiver(subscribedUuid: UUID?) : PebbleKit.PebbleAckReceiver(subscribedUuid) {
    private var log = Logger

    override fun receiveAck(context: Context?, transactionId: Int) {
        log.debug(TAG, "ack from pebble: "+transactionId);
    }

    companion object {
        private val TAG = "PebbleAckReceiver"
    }

}
