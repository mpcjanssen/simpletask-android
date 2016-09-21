package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import nl.mpcjanssen.simpletask.util.Config

abstract class ThemedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeTheme({ it ->
            when (it) {
                "dark" -> R.style.AppTheme_NoActionBar
                "black" -> R.style.AppTheme_Black_NoActionBar
                else -> R.style.AppTheme_Light_NoActionBar
            }
        }))

        super.onCreate(savedInstanceState)
    }
}
