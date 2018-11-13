package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import nl.mpcjanssen.simpletask.util.Config
import java.util.*

abstract class ThemedNoActionBarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeTheme)
        if (Config.forceEnglish) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
        super.onCreate(savedInstanceState)
    }
}

abstract class ThemedActionBarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeActionBarTheme)
        if (Config.forceEnglish) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
        super.onCreate(savedInstanceState)
    }
}

abstract class ThemedPreferenceActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeActionBarTheme)
        if (Config.forceEnglish) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
        super.onCreate(savedInstanceState)
    }
}

