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

        val pwPref = findPreference(getString(R.string.pref_key__set_encryption_password)) as EditTextPreference
        if (TodoApplication.config.isDefaultPasswordSet()) pwPref.valueInSummary(getString(R.string.password_already_set_summary))
        else pwPref.valueInSummary()
        pwPref.setOnPreferenceChangeListener { preference, any ->
            val password = any.toString()
            TodoApplication.config.setDefaultPassword(password)
            if (TodoApplication.config.isDefaultPasswordSet()) preference.valueInSummary(getString(R.string.password_already_set_summary))
            else preference.valueInSummary()
            true
        }
    }

}
