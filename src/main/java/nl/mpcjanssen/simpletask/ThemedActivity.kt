package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

abstract class ThemedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as TodoApplication
        setTheme(app.activeTheme)
        super.onCreate(savedInstanceState)
    }
}
