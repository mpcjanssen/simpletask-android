package nl.mpcjanssen.simpletask

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.PreferenceFragment

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.util.showConfirmationDialog

class FlavourPrefFragment : PreferenceFragment() {
    private val TAG = "FlavourPrefFragment"
    private lateinit var app: TodoApplication
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        app = activity.application as TodoApplication
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.flavourpreferences)

        val pwPref = findPreference(getString(R.string.pref_key__set_encryption_password)) as EditTextPreference
        if (FileStore.isDefaultPasswordSet()) pwPref.valueInSummary(getString(R.string.password_already_set_summary))
        else pwPref.valueInSummary()
        pwPref.setOnPreferenceChangeListener { preference, any ->
            val password = any.toString()
            FileStore.setDefaultPassword(password)
            if (FileStore.isDefaultPasswordSet()) preference.valueInSummary(getString(R.string.password_already_set_summary))
            else preference.valueInSummary()
            true
        }
    }

}
