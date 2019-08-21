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
import kotlinx.android.synthetic.main.script_filter.*
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.createAlertDialog

class FilterScriptFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() this:$this")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() this:$this")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState() this:$this")
        outState.putString(Query.INTENT_SCRIPT_FILTER, script)
        outState.putString(Query.INTENT_SCRIPT_TEST_TASK_FILTER, testTask)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView() this:$this")

        val arguments = arguments
        Log.d(TAG, "Fragment bundle:$this")
        val layout = inflater.inflate(R.layout.script_filter,
                container, false) as LinearLayout

//        cbUseScript = layout.findViewById(R.id.cb_use_script) as CheckBox
//        txtScript = layout.findViewById(R.id.txt_script) as EditText
//        txtTestTask = layout.findViewById(R.id.txt_testtask) as EditText
//        spnCallback = layout.findViewById(R.id.spnCallback) as Spinner
        activity?.let { act ->
            val callbacks = arrayOf<String>(Interpreter.ON_DISPLAY_NAME, Interpreter.ON_FILTER_NAME, Interpreter.ON_GROUP_NAME, Interpreter.ON_SORT_NAME)
            val spnAdapter = ArrayAdapter(act, R.layout.spinner_item, callbacks)
            spnCallback.adapter = spnAdapter

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
            if (savedInstanceState != null) {
                cb_use_script.isChecked = savedInstanceState.getBoolean(Query.INTENT_USE_SCRIPT_FILTER, false)
                txt_script.setText(savedInstanceState.getString(Query.INTENT_SCRIPT_FILTER, ""))
                txt_testtask.setText(savedInstanceState.getString(Query.INTENT_SCRIPT_TEST_TASK_FILTER, ""))
            } else {
                cb_use_script.isChecked = arguments?.getBoolean(Query.INTENT_USE_SCRIPT_FILTER, false) ?: false
                txt_script.setText(arguments?.getString(Query.INTENT_SCRIPT_FILTER, "") ?: "")
                txt_testtask.setText(arguments?.getString(Query.INTENT_SCRIPT_TEST_TASK_FILTER, "") ?: "")
            }
        }
        return layout
    }

    private fun testOnFilterCallback(barView: View, script: String, snackBar: Snackbar, t: Task) {
        val (toShow, result) = Interpreter.evalScript(environment, script).onFilterCallback(environment, t)
        if (toShow) {
            snackBar.setText(result + ": " + getString(R.string.script_tab_true_task_shown))
            barView.setBackgroundColor(0xff43a047.toInt())
        } else {
            snackBar.setText(result + ": " + getString(R.string.script_tab_false_task_not_shown))
            barView.setBackgroundColor(0xffe53935.toInt())
        }
        snackBar.show()
    }

    private fun testOnGroupCallback(barView: View, script: String, snackBar: Snackbar, t: Task) {
        activity?.let { act ->
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
            return if (cb_use_script == null) {
                arguments?.getBoolean(Query.INTENT_USE_SCRIPT_FILTER, false) ?: false
            } else {
                cb_use_script.isChecked
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
            return if (txt_script == null) {
                arguments?.getString(Query.INTENT_SCRIPT_FILTER, "") ?: ""
            } else {
                txt_script.text.toString()
            }
        }
        set(script) {
            txt_script.setText(script)
        }

    val testTask: String
        get() {
            val arguments = arguments
            return if (txt_testtask == null) {
                arguments?.getString(Query.INTENT_SCRIPT_TEST_TASK_FILTER, "") ?: ""
            } else {
                txt_testtask.text.toString()
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
