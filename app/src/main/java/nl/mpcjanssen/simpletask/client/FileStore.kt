package nl.mpcjanssen.simpletask.client

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.remote.IFileStorePlugin
import java.io.File

var FileStore: IFileStorePlugin? = null
val TAG = "FileStoreClient"
val mConnection = object : ServiceConnection {

    // Called when the connection with the service is established
    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        // Following the example above for an AIDL interface,
        // this gets an instance of the IRemoteInterface, which we can use to call on the service
        Log.v(TAG, "$className connected")
        FileStore = IFileStorePlugin.Stub.asInterface(service).also {
            if (!it.isAuthenticated) {
                it.login()
            }
        }
    }

    // Called when the connection with the service disconnects unexpectedly
    override fun onServiceDisconnected(className: ComponentName) {
        Log.e(TAG, "Service $className has unexpectedly disconnected")
        FileStore = null
    }
}