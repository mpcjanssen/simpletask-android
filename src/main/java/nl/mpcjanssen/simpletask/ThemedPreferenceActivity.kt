package nl.mpcjanssen.simpletask

import android.os.Bundle
import nl.mpcjanssen.simpletask.util.Config

abstract class ThemedPreferenceActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeActionBarTheme)
        super.onCreate(savedInstanceState)
    }
}
