package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.v7.app.AppCompatActivity

abstract class ThemedPreferenceActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as TodoApplication
        setTheme(app.activeTheme)
        setTheme(app.activeFont)
        super.onCreate(savedInstanceState)
    }
}
