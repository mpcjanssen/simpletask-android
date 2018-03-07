/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.task.TodoList.todoQueue
import nl.mpcjanssen.simpletask.util.*
import java.io.File
import java.io.IOException

class ScriptConfigScreen : ThemedActionBarActivity() {

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

        scriptEdit = findViewById<EditText>(R.id.lua_config)
        script = Config.luaConfig

        val fab = findViewById<FloatingActionButton>(R.id.lua_config_fab)
        fab?.setOnClickListener {
            Config.luaConfig = script
            // Run the script and refilter
            runScript()
            broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)
            broadcastRefreshWidgets(TodoApplication.app.localBroadCastManager)
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
                runScript()
            }
            R.id.lua_config_help -> {
                val intent = Intent(this, HelpScreen::class.java)
                intent.putExtra(Constants.EXTRA_HELP_PAGE, "script")
                startActivityForResult(intent, 0)
            }
            R.id.lua_config_share -> {
                shareText(this, getString(R.string.lua_config_screen), script)
            }
            R.id.lua_config_import -> {
                importLuaConfig(File(Config.todoFile.parent, "config.lua"))
            }
            R.id.lua_config_export -> {
                exportLuaConfig(File(Config.todoFile.parent, "config.lua"))
            }
        }
        return true
    }

    private fun runScript() {
        try {
            Interpreter.evalScript(null, script)
        } catch (e: ScriptError) {
            log.debug(FilterScriptFragment.TAG, "Lua execution failed " + e.message)
            createAlertDialog(this, R.string.lua_error, e.message ?: "").show()
        }
    }

    private fun exportLuaConfig (exportFile: File) {
        todoQueue("Export Lua config") {
            Config.luaConfig = script
            try {
                FileStore.writeFile(exportFile, Config.luaConfig)
                showToastShort(this, "Lua config exported")
            } catch (e: Exception) {
                log.error(TAG, "Export lua config failed", e)
                showToastLong(this, "Error exporting lua config")
            }
        }
    }

    private fun importLuaConfig (importFile: File) {
        todoQueue("Import Lua config") {
            try {
                FileStore.readFile(importFile.canonicalPath) { contents ->
                    showToastShort(this, getString(R.string.toast_lua_config_imported))
                    runOnUiThread {
                        script = contents
                    }
                }

            } catch (e: IOException) {
                log.error(TAG, "Import lua config, cant read file ${importFile.canonicalPath}", e)
                showToastLong(this, "Error reading file ${importFile.canonicalPath}")
            }
        }
    }

    var script: String
        get() = scriptEdit.text.toString()
        set(value) = scriptEdit.setText(value)

    companion object {
        internal val TAG = ScriptConfigScreen::class.java.simpleName
    }
}
