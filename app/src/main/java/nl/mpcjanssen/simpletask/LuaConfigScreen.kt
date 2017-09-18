/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.EditText
import android.view.Menu
import android.view.MenuItem

import nl.mpcjanssen.simpletask.util.*
import org.luaj.vm2.LuaError

class LuaConfigScreen : ThemedActionBarActivity() {

    private val log = Logger
    private lateinit var m_app : TodoApplication
    private lateinit var scriptEdit : EditText
    private var m_menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m_app = application as TodoApplication
        setContentView(R.layout.lua_config)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)

        scriptEdit = findViewById(R.id.lua_config) as EditText
        scriptEdit.setText(Config.luaConfig)

        val fab = findViewById(R.id.lua_config_fab) as FloatingActionButton?
        fab?.setOnClickListener {
            Config.luaConfig = script()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        val inflater = menuInflater
        inflater.inflate(R.menu.lua_config, menu)
        m_menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.lua_config_run -> {
                try {
                    LuaInterpreter.evalScript(null, script())
                } catch (e: LuaError) {
                    log.debug(FilterScriptFragment.TAG, "Lua execution failed " + e.message)
                    createAlertDialog(this, R.string.lua_error, e.message ?: "").show()
                }
            }
            R.id.lua_config_help -> {
                val intent = Intent(this, HelpScreen::class.java)
                intent.putExtra(Constants.EXTRA_HELP_PAGE, "script")
                startActivityForResult(intent, 0)
            }
            R.id.lua_config_share -> {
                shareText(this, getString(R.string.lua_config_screen), script())
            }

        }
        return true
    }

    fun script () : String {
        return scriptEdit.text.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        internal val TAG = LuaConfigScreen::class.java.simpleName
    }
}
