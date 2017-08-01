package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.createAlertDialog
import org.luaj.vm2.LuaError

class FilterScriptFragment : Fragment() {
    private var txtScript: EditText? = null
    private var cbUseScript: CheckBox? = null
    private var txtTestTask: EditText? = null
    private var spnCallback: Spinner? = null
    private val log: Logger = Logger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.debug(TAG, "onCreate() this:" + this)
    }

    override fun onDestroy() {
        super.onDestroy()
        log.debug(TAG, "onDestroy() this:" + this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        log.debug(TAG, "onSaveInstanceState() this:" + this)
        outState.putString(ActiveFilter.INTENT_SCRIPT_FILTER, script)
        outState.putString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER, testTask)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        log.debug(TAG, "onCreateView() this:" + this)

        val arguments = arguments
        log.debug(TAG, "Fragment bundle:" + this)
        val layout = inflater.inflate(R.layout.script_filter,
                container, false) as LinearLayout

        cbUseScript = layout.findViewById(R.id.cb_use_script) as CheckBox
        txtScript = layout.findViewById(R.id.txt_script) as EditText
        txtTestTask = layout.findViewById(R.id.txt_testtask) as EditText
        spnCallback = layout.findViewById(R.id.spnCallback) as Spinner

        val callbacks = arrayOf<String>(LuaInterpreter.ON_FILTER_NAME, LuaInterpreter.ON_GROUP_NAME)
        val spnAdapter = ArrayAdapter(activity, R.layout.spinner_item, callbacks)
        spnCallback?.adapter = spnAdapter

        val btnTest = layout.findViewById(R.id.btnTest) as Button
        btnTest.setOnClickListener {
            val callbackToTest = selectedCallback
            val t = Task(testTask)
            try {
                log.info(TAG, "Running $callbackToTest test Lua callback in module $environment")
                val script = script
                val snackBar = Snackbar.make(activity.findViewById(android.R.id.content), "", Snackbar.LENGTH_LONG)
                val barView = snackBar.view
                when (callbackToTest) {
                    LuaInterpreter.ON_FILTER_NAME -> testOnFilterCallback(barView, script, snackBar, t)
                    LuaInterpreter.ON_GROUP_NAME -> testOnGroupCallback(barView, script, snackBar, t)
                }

            } catch (e: LuaError) {
                log.debug(TAG, "Lua execution failed " + e.message)
                createAlertDialog(activity, R.string.lua_error, e.message ?: "").show()
            }
        }
        if (savedInstanceState != null) {
            cbUseScript!!.isChecked = savedInstanceState.getBoolean(ActiveFilter.INTENT_USE_SCRIPT_FILTER, false)
            txtScript!!.setText(savedInstanceState.getString(ActiveFilter.INTENT_SCRIPT_FILTER, ""))
            txtTestTask!!.setText(savedInstanceState.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER, ""))
        } else {
            cbUseScript!!.isChecked = arguments.getBoolean(ActiveFilter.INTENT_USE_SCRIPT_FILTER, false)
            txtScript!!.setText(arguments.getString(ActiveFilter.INTENT_SCRIPT_FILTER, ""))
            txtTestTask!!.setText(arguments.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER, ""))
        }
        return layout
    }

    private fun testOnFilterCallback(barView: View, script: String, snackBar: Snackbar, t: Task) {
        if (script.trim { it <= ' ' }.isEmpty() || LuaInterpreter.evalScript(environment, script).onFilterCallback(environment, t)) {
            snackBar.setText(getString(R.string.script_tab_true_task_shown))
            barView.setBackgroundColor(0xff43a047.toInt())
        } else {
            snackBar.setText(getString(R.string.script_tab_false_task_not_shown))
            barView.setBackgroundColor(0xffe53935.toInt())
        }
        snackBar.show()
    }
    private fun testOnGroupCallback(barView: View, script: String, snackBar: Snackbar, t: Task) {
        if (!script.trim { it <= ' ' }.isEmpty()) {
            snackBar.setText("Group: " + LuaInterpreter.evalScript(environment, script).onGroupCallback(environment, t))
            barView.setBackgroundColor(ContextCompat.getColor(activity, R.color.gray74))
        } else {
            snackBar.setText("Callback not defined")
            barView.setBackgroundColor(0xffe53935.toInt())
        }
        snackBar.show()
    }

    val useScript: Boolean
        get() {
            val arguments = arguments
            if (cbUseScript == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_USE_SCRIPT_FILTER, false)
            } else {
                return cbUseScript?.isChecked ?: false
            }
        }

    val environment: String
        get() {
            val arguments = arguments
            return arguments.getString(ActiveFilter.INTENT_LUA_MODULE, "main")
        }

    var script: String
        get() {
            val arguments = arguments
            if (txtScript == null) {
                return arguments.getString(ActiveFilter.INTENT_SCRIPT_FILTER, "")
            } else {
                return txtScript!!.text.toString()
            }
        }
        set(script) {
            txtScript?.setText(script)
        }

    val testTask: String
        get() {
            val arguments = arguments
            if (txtTestTask == null) {
                return arguments.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER, "")
            } else {
                return txtTestTask!!.text.toString()
            }
        }

    val selectedCallback: String
        get() {
            if (spnCallback == null) {
                return LuaInterpreter.ON_FILTER_NAME
            }
            return spnCallback?.selectedItem.toString()
        }

    companion object {
        internal val TAG = FilterScriptFragment::class.java.simpleName
    }
}
