package nl.mpcjanssen.simpletask.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import nl.mpcjanssen.simpletask.TodoApplication;

/**
 * Created by Mark on 6/26/2015.
 */
public class ConnectivityReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        TodoApplication mApp = ((TodoApplication) context.getApplicationContext());
        FileStore store =  (FileStore) mApp.getFileStore();
        Log.v(TAG, "Connectivity changed");
        store.changedConnectionState(intent);
    }
}
