package nl.mpcjanssen.simpletask

import android.os.Bundle

abstract class ThemedPreferenceActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as TodoApplication
        setTheme(app.activeTheme)
        super.onCreate(savedInstanceState)
    }
}
