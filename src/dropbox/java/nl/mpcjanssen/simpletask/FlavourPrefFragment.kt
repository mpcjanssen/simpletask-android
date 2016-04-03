package nl.mpcjanssen.simpletask

import android.content.DialogInterface
import android.content.Intent
import nl.mpcjanssen.simpletask.R
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import nl.mpcjanssen.simpletask.util.restartToIntent

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
                        restartToIntent(activity, Intent(activity, Simpletask::class.java));
                    }, R.string.dropbox_logout_pref_title)
            true
        }
    }

}
