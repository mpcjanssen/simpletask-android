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
        debugIntent(intent,TAG);
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        boolean connected = netInfo != null && netInfo.isConnected();
        store.changedConnectionState(connected);
    }

    private void debugIntent(Intent intent, String tag) {
        Log.v(tag, "action: " + intent.getAction());
        Log.v(tag, "component: " + intent.getComponent());
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key: extras.keySet()) {
                Log.v(tag, "key [" + key + "]: " +
                        extras.get(key));
            }
        }
        else {
            Log.v(tag, "no extras");
        }
    }
}
