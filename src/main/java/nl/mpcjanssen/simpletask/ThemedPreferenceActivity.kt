package nl.mpcjanssen.simpletask

import android.os.Bundle
import nl.mpcjanssen.simpletask.util.Config

abstract class ThemedPreferenceActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeTheme({ it ->
            when (it) {
                "dark" -> R.style.AppTheme_Settings
                "black" -> R.style.AppTheme_Black_Settings
                else -> R.style.AppTheme_Light_DarkActionBar_Settings
            }
        }))
        super.onCreate(savedInstanceState)
    }
}
