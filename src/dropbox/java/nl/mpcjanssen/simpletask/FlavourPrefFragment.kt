package nl.mpcjanssen.simpletask

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v4.content.LocalBroadcastManager

class FlavourPrefFragment : PreferenceFragment() {
    private val log = Logger
    private val TAG = "FlavourPrefFragment"
    private lateinit var app: TodoApplication
    override fun onCreate(savedInstanceState: Bundle?) {
        app = activity.application as TodoApplication
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.flavourpreferences)

        val dropboxPref = findPreference("logout_dropbox")
        dropboxPref.setOnPreferenceClickListener {
            log.info(TAG, "Logging out from Dropbox")
            app.showConfirmationDialog(activity, R.string.logout_message,
                    DialogInterface.OnClickListener() { dialogInterface, i ->
                        app.fileStore.logout()
                        activity.finish();
                        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(Constants.BROADCAST_ACTION_LOGOUT))
                    }, R.string.dropbox_logout_pref_title)
            true
        }
        val sync_sec_pref = findPreference(getString(R.string.dropbox_refresh_period))
        sync_sec_pref.valueInSummary()
        sync_sec_pref.setOnPreferenceChangeListener { preference, any ->
            preference.summary = getString(R.string.dropbox_sync_interval_title_summary)
            preference.valueInSummary(any)
            true
        }
    }

}
