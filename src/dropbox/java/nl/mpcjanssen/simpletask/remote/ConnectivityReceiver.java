package nl.mpcjanssen.simpletask.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import nl.mpcjanssen.simpletask.TodoApplication;



public class ConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger log = Logger.INSTANCE;
        TodoApplication mApp = ((TodoApplication) context.getApplicationContext());
        FileStore store =  (FileStore) mApp.getFileStore();
        log.debug(TAG, "Connectivity changed {}", intent);
        store.changedConnectionState(intent);
    }
}
