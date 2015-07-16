package nl.mpcjanssen.simpletask.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import nl.mpcjanssen.simpletask.TodoApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger log = LoggerFactory.getLogger(this.getClass());
        TodoApplication mApp = ((TodoApplication) context.getApplicationContext());
        FileStore store =  (FileStore) mApp.getFileStore();
        log.debug("Connectivity changed {}", intent);
        store.changedConnectionState(intent);
    }
}
