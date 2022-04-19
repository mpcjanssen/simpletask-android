package nl.mpcjanssen.simpletask

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceFragment

import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.util.showConfirmationDialog

class FlavourPrefFragment : PreferenceFragment() {
    private val TAG = "FlavourPrefFragment"
    private lateinit var app: TodoApplication
    override fun onCreate(savedInstanceState: Bundle?) {
        app = activity.application as TodoApplication
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.flavourpreferences)

        val dropboxPref = findPreference("logout_dropbox")
        dropboxPref.setOnPreferenceClickListener {
            Log.i(TAG, "Logging out from Dropbox")
            showConfirmationDialog(activity, R.string.logout_message,
                    { _, _ ->
                        activity.finish()
                        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(Constants.BROADCAST_ACTION_LOGOUT))
                    }, R.string.dropbox_logout_pref_title)
            true
        }
    }

}
