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

        val nextcloudPref = findPreference("logout_nextcloud")
        nextcloudPref.setOnPreferenceClickListener {
            Log.i(TAG, "Logging out from Nextcloud")
            showConfirmationDialog(activity, R.string.webdav_logout_message,
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        FileStore.logout()
                        TodoApplication.app.clearTodoFile()
                        activity.finish()
                        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(Constants.BROADCAST_ACTION_LOGOUT))
                    }, R.string.webdav_logout_pref_title)
            true
        }
    }

}
