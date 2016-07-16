package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.createAlertDialog
import org.luaj.vm2.LuaError

class FilterScriptFragment : Fragment() {
    private var txtScript: EditText? = null
    private var cbUseScript: CheckBox? = null
    private var txtTestTask: EditText? = null
    private var log: Logger = Logger


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

        val btnTest = layout.findViewById(R.id.btnTest) as Button
        btnTest.setOnClickListener {
            val t = Task(testTask)
            try {
                log.info(TAG, "Running onFilter test Lua callback in module $environment")
                val script = script
                val snackBar = Snackbar.make(activity.findViewById(android.R.id.content), "", Snackbar.LENGTH_LONG)
                val barView = snackBar.view
                if (script.trim { it <= ' ' }.isEmpty() || LuaScripting.onFilterCallback(LuaScripting.setOnFilter(environment, script), t)) {
                    snackBar.setText("True, task will be shown")
                    barView.setBackgroundColor(0xff43a047.toInt())
                } else {
                    snackBar.setText("False: task will not be shown")
                    barView.setBackgroundColor(0xffe53935.toInt())
                }
                snackBar.show()
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

    val useScript: Boolean
        get() {
            val arguments = arguments
            if (cbUseScript == null) {
                return arguments.getBoolean(ActiveFilter.INTENT_USE_SCRIPT_FILTER, false)
            } else {
                return cbUseScript?.isChecked ?: false
            }
        }

    val environment: String?
        get() {
            val arguments = arguments
            return arguments.getString(ActiveFilter.INTENT_WIDGET_ID, null)
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

    companion object {
        internal val TAG = FilterScriptFragment::class.java.simpleName
    }
}
