package nl.mpcjanssen.simpletask;

import android.util.Log;

import org.l6n.sendlog.library.SendLogActivityBase;

public class SendLogActivity extends SendLogActivityBase {

    /**
     * This is the only method you have to override.
     * There others that you may optionally want to override.
     */
    @Override
    protected String getDestinationAddress() {
        return "simpletask-logs@mpcjanssen.nl";
    }
}
