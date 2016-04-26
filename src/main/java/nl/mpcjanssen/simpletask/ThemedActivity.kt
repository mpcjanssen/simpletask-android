package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

abstract class ThemedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as SimpletaskApplication
        setTheme(app.activeTheme)
        setTheme(app.activeFont)
        super.onCreate(savedInstanceState)
    }
}
