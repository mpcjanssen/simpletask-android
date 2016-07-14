/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import nl.mpcjanssen.simpletask.util.createAlertDialog
import org.luaj.vm2.LuaError

class LuaConfigScreen : ThemedActivity() {

    private val log = Logger
    private lateinit var m_app : TodoApplication
    private lateinit var scriptEdit : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m_app = application as TodoApplication
        setContentView(R.layout.lua_config)
        scriptEdit = findViewById(R.id.lua_config) as EditText
        val btnRun = findViewById(R.id.btn_run) as Button
        btnRun.setOnClickListener {
            try {
                LuaScripting.evalScript(null, script())
            } catch (e: LuaError) {
                log.debug(FilterScriptFragment.TAG, "Lua execution failed " + e.message)
                createAlertDialog(this, R.string.lua_error, e.message ?: "").show()
            }
        }
        scriptEdit.setText(m_app.luaConfig)
    }

    fun script () : String {
        return scriptEdit.text.toString()
    }

    override fun onDestroy() {
        m_app.luaConfig = script()
        m_app.reloadLuaConfig()
        super.onDestroy()
    }

    companion object {
        internal val TAG = LuaConfigScreen::class.java.simpleName
    }
}
