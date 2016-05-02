package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Util;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.jse.JsePlatform;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FilterScriptFragment extends Fragment {

    final static String TAG = FilterScriptFragment.class.getSimpleName();
    private EditText txtScript;
    private CheckBox cbUseScript;
    @Nullable
    ActionBar actionbar;
    private EditText txtTestTask;
    private TextView tvResult;
    private TextView tvBooleanResult;
    private Logger log;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = Logger.INSTANCE;
        log.debug(TAG, "onCreate() this:" + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.debug(TAG, "onDestroy() this:" + this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        log.debug(TAG, "onSaveInstanceState() this:" + this);
        outState.putString(ActiveFilter.INTENT_SCRIPT_FILTER,getScript());
        outState.putString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER,getTestTask());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        log.debug(TAG, "onCreateView() this:" + this);

        Bundle arguments = getArguments();
        actionbar = getActivity().getActionBar();
        log.debug(TAG, "Fragment bundle:" + this);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.script_filter,
                container, false);

        cbUseScript = (CheckBox) layout.findViewById(R.id.cb_use_script);
        txtScript = (EditText) layout.findViewById(R.id.txt_script);
        txtTestTask = (EditText) layout.findViewById(R.id.txt_testtask);
        tvResult = (TextView) layout.findViewById(R.id.result);
        tvBooleanResult = (TextView) layout.findViewById(R.id.booleanResult);
        Button btnTest = (Button) layout.findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Task t = new Task(getTestTask());
                try {
                    String script = getScript();
                    InputStream input = new ByteArrayInputStream(script.getBytes());
                    Prototype prototype = LuaC.instance.compile(input, "script");
                    Globals globals = JsePlatform.standardGlobals();

                    Util.initGlobals(globals, t);
                    LuaClosure closure = new LuaClosure(prototype, globals);
                    LuaValue result = closure.call();

                    tvResult.setText(result.toString());

                    if (result.toboolean() || script.trim().isEmpty()) {
                        tvBooleanResult.setText("true");
                    } else {
                        tvBooleanResult.setText("false");
                    }
                } catch (LuaError e) {
                    log.debug(TAG, "Lua execution failed " + e.getMessage());
                    tvBooleanResult.setText("error");
                    tvResult.setText(e.getMessage());
                } catch (IOException e) {
                    log.debug(TAG, "Execution failed " + e.getMessage());
                }
            }

        });
        if (savedInstanceState != null) {
            cbUseScript.setChecked(savedInstanceState.getBoolean(ActiveFilter.INTENT_USE_SCRIPT_FILTER,false));
            txtScript.setText(savedInstanceState.getString(ActiveFilter.INTENT_SCRIPT_FILTER,""));
            txtTestTask.setText(savedInstanceState.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER,""));
        } else {
            cbUseScript.setChecked(arguments.getBoolean(ActiveFilter.INTENT_USE_SCRIPT_FILTER,false));
            txtScript.setText(arguments.getString(ActiveFilter.INTENT_SCRIPT_FILTER,""));
            txtTestTask.setText(arguments.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER,""));
        }
        return layout;
    }

    public Boolean getUseScript() {
        Bundle arguments = getArguments();
        if (cbUseScript == null) {
            return arguments.getBoolean(ActiveFilter.INTENT_USE_SCRIPT_FILTER, false);
        } else {
            return cbUseScript.isChecked();
        }
    }

    public String getScript() {
        Bundle arguments = getArguments();
        if (txtScript == null) {
            return arguments.getString(ActiveFilter.INTENT_SCRIPT_FILTER, "");
        } else {
            return txtScript.getText().toString();
        }
    }

    public String getTestTask() {
        Bundle arguments = getArguments();
        if (txtTestTask == null) {
            return arguments.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER, "");
        } else {
            return txtTestTask.getText().toString();
        }
    }

    public void setScript(String script) {
        if (txtScript!=null) {
            txtScript.setText(script);
        }
    }
}
