package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.preference.PreferenceFragment

class FlavourPrefFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.flavourpreferences)
    }
}
