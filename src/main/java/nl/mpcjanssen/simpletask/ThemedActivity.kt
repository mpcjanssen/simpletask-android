package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import nl.mpcjanssen.simpletask.util.Config

abstract class ThemedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Config.activeTheme)

        super.onCreate(savedInstanceState)
    }
}
