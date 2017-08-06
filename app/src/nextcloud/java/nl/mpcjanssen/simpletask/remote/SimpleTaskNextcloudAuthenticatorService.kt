package nl.mpcjanssen.simpletask.remote

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SimpleTaskNextcloudAuthenticatorService : Service() {
    // Instance field that stores the authenticator object
    var mAuthenticator: SimpleTaskNextcloudAuthenticator? = null

    override fun onCreate() {
        // Create a new authenticator object
        mAuthenticator = SimpleTaskNextcloudAuthenticator(this);
    }

    override fun onBind(intent: Intent): IBinder {
        return mAuthenticator!!.iBinder;
    }

}