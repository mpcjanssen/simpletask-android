package nl.mpcjanssen.simpletask

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import java.util.*

/**
 * Created by brandonknitter on 10/9/16.
 */

class PebbleNackReceiver(subscribedUuid: UUID?) : PebbleKit.PebbleNackReceiver(subscribedUuid) {
    private var log = Logger

    override fun receiveNack(context: Context?, transactionId: Int) {
        log.debug(TAG, "nack from pebble: "+transactionId);
    }

    companion object {
        private val TAG = "PebbleNackReceiver"
    }

}
