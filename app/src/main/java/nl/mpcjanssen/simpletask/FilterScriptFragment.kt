package nl.mpcjanssen.simpletask

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.createAlertDialog

class FilterScriptFragment : Fragment() {
    private var txtScript: EditText? = null
    private var cbUseScript: CheckBox? = null
    private var txtTestTask: EditText? = null
    private var spnCallback: Spinner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() this:" + this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() this:" + this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState() this:" + this)
        outState.putString(Query.INTENT_SCRIPT_FILTER, script)
        outState.putString(Query.INTENT_SCRIPT_TEST_TASK_FILTER, testTask)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView() this:" + this)

        val arguments = arguments
        Log.d(TAG, "Fragment bundle:" + this)
        val layout = inflater.inflate(R.layout.script_filter,
                container, false) as LinearLayout

        cbUseScript = layout.findViewById(R.id.cb_use_script) as CheckBox
        txtScript = layout.findViewById(R.id.txt_script) as EditText
        txtTestTask = layout.findViewById(R.id.txt_testtask) as EditText
        spnCallback = layout.findViewById(R.id.spnCallback) as Spinner

        val callbacks = arrayOf<String>(Interpreter.ON_DISPLAY_NAME, Interpreter.ON_FILTER_NAME, Interpreter.ON_GROUP_NAME, Interpreter.ON_SORT_NAME)
        val spnAdapter = activity?.let { ArrayAdapter(it, R.layout.spinner_item, callbacks) }
        spnCallback?.adapter = spnAdapter
        activity?.let { act ->
            val btnTest = layout.findViewById(R.id.btnTest) as Button
            btnTest.setOnClickListener {
                val callbackToTest = selectedCallback
                val t = Task(testTask)
                try {
                    Log.i(TAG, "Running $callbackToTest test Lua callback in module $environment")
                    val script = script

                    val snackBar = Snackbar.make(act.findViewById(android.R.id.content), "", Snackbar.LENGTH_LONG)
                    val barView = snackBar.view
                    when (callbackToTest) {
                        Interpreter.ON_DISPLAY_NAME -> testOnDisplayCallback(barView, script, snackBar, t)
                        Interpreter.ON_FILTER_NAME -> testOnFilterCallback(barView, script, snackBar, t)
                        Interpreter.ON_GROUP_NAME -> testOnGroupCallback(barView, script, snackBar, t)
                        Interpreter.ON_SORT_NAME -> testOnSortCallback(barView, script, snackBar, t)
                    }


                } catch (e: Exception) {
                    Log.d(TAG, "Lua execution failed " + e.message)
                    createAlertDialog(act, R.string.lua_error, e.message ?: "").show()
                }
            }
        }
        if (savedInstanceState != null) {
            cbUseScript!!.isChecked = savedInstanceState.getBoolean(Query.INTENT_USE_SCRIPT_FILTER, false)
            txtScript!!.setText(savedInstanceState.getString(Query.INTENT_SCRIPT_FILTER, ""))
            txtTestTask!!.setText(savedInstanceState.getString(Query.INTENT_SCRIPT_TEST_TASK_FILTER, ""))
        } else {
            cbUseScript!!.isChecked = arguments?.getBoolean(Query.INTENT_USE_SCRIPT_FILTER, false) ?: false
            txtScript!!.setText(arguments?.getString(Query.INTENT_SCRIPT_FILTER, "") ?:"")
            txtTestTask!!.setText(arguments?.getString(Query.INTENT_SCRIPT_TEST_TASK_FILTER, "")?:"")
        }
        return layout
    }

    private fun testOnFilterCallback(barView: View, script: String, snackBar: Snackbar, t: Task) {
        val (toShow, result) = Interpreter.evalScript(environment, script).onFilterCallback(environment, t)
        if (toShow) {
            snackBar.setText(result +": " + getString(R.string.script_tab_true_task_shown))
            barView.setBackgroundColor(0xff43a047.toInt())
        } else {
            snackBar.setText(result +": " + getString(R.string.script_tab_false_task_not_shown))
            barView.setBackgroundColor(0xffe53935.toInt())
        }
        snackBar.show()
    }

    private fun testOnGroupCallback(barView: View, script: String, snackBar: Snackbar, t: Task) {
        activity?.let {act ->
            if (!script.trim { it <= ' ' }.isEmpty()) {
                snackBar.setText("Group: " + Interpreter.evalScript(environment, script).onGroupCallback(environment, t))
                barView.setBackgroundColor(ContextCompat.getColor(act, R.color.gray74))
            } else {
                snackBar.setText("Callback not defined")
                barView.setBackgroundColor(0xffe53935.toInt())
            }
            snackBar.show()
        }
    }

    private fun testOnDisplayCallback(barView: View, script: String, snackBar: Snackbar, t: Task) {
        activity?.let { act ->
            if (!script.trim { it <= ' ' }.isEmpty()) {
                snackBar.setText("Display: " + Interpreter.evalScript(environment, script).onDisplayCallback(environment, t))
                barView.setBackgroundColor(ContextCompat.getColor(act, R.color.gray74))
            } else {
                snackBar.setText("Callback not defined")
                barView.setBackgroundColor(0xffe53935.toInt())
            }
            snackBar.show()
        }
    }

    private fun testOnSortCallback(barView: View, script: String, snackBar: Snackbar, t: Task) {
        activity?.let { act ->
            if (!script.trim { it <= ' ' }.isEmpty()) {
                snackBar.setText("Display: " + Interpreter.evalScript(environment, script).onSortCallback(environment, t))
                barView.setBackgroundColor(ContextCompat.getColor(act, R.color.gray74))
            } else {
                snackBar.setText("Callback not defined")
                barView.setBackgroundColor(0xffe53935.toInt())
            }
            snackBar.show()
        }
    }

    val useScript: Boolean
        get() {
            val arguments = arguments
            if (cbUseScript == null) {
                return arguments?.getBoolean(Query.INTENT_USE_SCRIPT_FILTER, false) ?: false
            } else {
                return cbUseScript?.isChecked ?: false
            }
        }

    val environment: String
        get() {
            val arguments = arguments
            return arguments?.getString(Query.INTENT_LUA_MODULE, "main") ?: "main"
        }

    var script: String
        get() {
            val arguments = arguments
            if (txtScript == null) {
                return arguments?.getString(Query.INTENT_SCRIPT_FILTER, "") ?: ""
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
                return arguments?.getString(Query.INTENT_SCRIPT_TEST_TASK_FILTER, "") ?: ""
            } else {
                return txtTestTask!!.text.toString()
            }
        }

    val selectedCallback: String
        get() {
            if (spnCallback == null) {
                return Interpreter.ON_FILTER_NAME
            }
            return spnCallback?.selectedItem.toString()
        }

    companion object {
        internal val TAG = FilterScriptFragment::class.java.simpleName
    }
}
