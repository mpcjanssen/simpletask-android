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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Util;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclBoolean;
import tcl.lang.TclObject;
import tcl.lang.TclString;


public class FilterScriptFragment extends Fragment {

    final static String TAG = FilterScriptFragment.class.getSimpleName();
    private EditText txtScript;
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

        txtScript = (EditText) layout.findViewById(R.id.txt_script);
        txtTestTask = (EditText) layout.findViewById(R.id.txt_testtask);
        tvResult = (TextView) layout.findViewById(R.id.result);
        tvBooleanResult = (TextView) layout.findViewById(R.id.booleanResult);
        Button btnTest = (Button) layout.findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Task t = new Task(getTestTask());
                Interp i = new Interp();
                try {
                    TclObject script = TclString.newInstance(getScript());
                    // Load the script
                    i.eval(script, TCL.EVAL_GLOBAL);

                    // Call the filter proc
                    i.eval(Util.buildFilterTclCommand(i,t), TCL.EVAL_GLOBAL);
                    String result;
                    boolean resultAsBoolean;
                    if (i.returnCode == TCL.ERROR) {
                        result = i.errorInfo;
                        resultAsBoolean = false;
                    } else {
                        TclObject obj = i.getResult();
                        result = i.getResult().toString();
                        resultAsBoolean = TclBoolean.get(i,obj);
                    }
                    tvResult.setText(result);

                    if (resultAsBoolean || getScript().trim().isEmpty()) {
                        tvBooleanResult.setText("true");
                    } else {
                        tvBooleanResult.setText("false");
                    }

                } catch (Exception e) {
                    log.debug(TAG, "Tcl execution failed " + e.getCause() + i.getResult());
                    tvBooleanResult.setText("error");
                    tvResult.setText(i.getResult().toString());
                }
                i.dispose();
            }

        });
        if (savedInstanceState != null) {
            txtScript.setText(savedInstanceState.getString(ActiveFilter.INTENT_SCRIPT_FILTER,""));
            txtTestTask.setText(savedInstanceState.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER,""));
        } else {
            txtScript.setText(arguments.getString(ActiveFilter.INTENT_SCRIPT_FILTER,""));
            txtTestTask.setText(arguments.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER,""));
        }
        return layout;
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
