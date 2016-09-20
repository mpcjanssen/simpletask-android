package nl.mpcjanssen.simpletask

import android.os.Bundle
import nl.mpcjanssen.simpletask.util.Config

abstract class ThemedPreferenceActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeTheme({ it ->
            when (it) {
                "dark" -> R.style.AppTheme_ActionBar
                "black" -> R.style.AppTheme_Black_ActionBar
                else -> R.style.AppTheme_Light_DarkActionBar_ActionBar
            }
        }))
        super.onCreate(savedInstanceState)
    }
}
