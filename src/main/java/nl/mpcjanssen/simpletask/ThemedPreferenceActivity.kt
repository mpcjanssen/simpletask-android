package nl.mpcjanssen.simpletask

import android.os.Bundle

abstract class ThemedPreferenceActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as SimpletaskApplication
        setTheme(app.activeTheme)
        setTheme(app.activeFont)
        super.onCreate(savedInstanceState)
    }
}
