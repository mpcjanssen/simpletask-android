package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Util;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.*;
import org.luaj.vm2.lib.jse.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

public class FilterScriptFragment extends Fragment {

    final static String TAG = FilterScriptFragment.class.getSimpleName();
    private EditText txtScript;
    private GestureDetector gestureDetector;
    @Nullable
    ActionBar actionbar;
    private EditText txtTestTask;
    private TextView tvResult;
    private TextView tvBooleanResult;
    private Button btnTest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate() this:" + this);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.v(TAG, "onDestroy() this:" + this);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(TAG, "onSaveInstanceState() this:" + this);
        outState.putString(ActiveFilter.INTENT_SCRIPT_FILTER,getScript());
        outState.putString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER,getTestTask());
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView() this:" + this);

        Bundle arguments = getArguments();
        actionbar = getActivity().getActionBar();
        Log.v(TAG, "Fragment bundle:" + this);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.script_filter,
                container, false);

        txtScript = (EditText) layout.findViewById(R.id.txt_script);
        txtTestTask = (EditText) layout.findViewById(R.id.txt_testtask);
        tvResult = (TextView) layout.findViewById(R.id.result);
        tvBooleanResult = (TextView) layout.findViewById(R.id.booleanResult);
        btnTest = (Button) layout.findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Task t = new Task(0, getTestTask());
                try {
                    String script = getScript();
                    InputStream input = new ByteArrayInputStream(script.getBytes());
                    Prototype prototype = LuaC.instance.compile(input, "script");
                    Globals globals = JsePlatform.standardGlobals();
                    
                    Util.initGlobals(globals,t);
                    LuaClosure closure = new LuaClosure(prototype, globals);
                    LuaValue result = closure.call(); 

                    tvResult.setText(result.toString());
                   
                    if (result.toboolean() ) {
                        tvBooleanResult.setText("true");
                    } else {
                        tvBooleanResult.setText("false");
                    }
                } catch (LuaError e) {
                    Log.v(TAG, "Lua execution failed " + e.getMessage());
                    tvBooleanResult.setText("error");
                    tvResult.setText(e.getMessage());
                } catch (IOException e) {
                    Log.v(TAG, "Execution failed " + e.getMessage());
                }
            }

        });
        if (savedInstanceState != null) {
            txtScript.setText(savedInstanceState.getString(ActiveFilter.INTENT_SCRIPT_FILTER,""));
            txtTestTask.setText(savedInstanceState.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER,""));
        } else {
            txtScript.setText(arguments.getString(ActiveFilter.INTENT_SCRIPT_FILTER,""));
            txtTestTask.setText(arguments.getString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER,""));
        }

        gestureDetector = new GestureDetector(TodoApplication.getAppContext(),
                new FilterGestureDetector());
        OnTouchListener gestureListener = new OnTouchListener() {
            @Override
            public boolean onTouch(@NotNull View v, @NotNull MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    v.onTouchEvent(cancelEvent);
                    return true;
                }
                return false;
            }
        };

        layout.setOnTouchListener(gestureListener);
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

    class FilterGestureDetector extends SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 120;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(@NotNull MotionEvent e1, @NotNull MotionEvent e2, float velocityX,
                               float velocityY) {

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            if (actionbar==null) {
                return false;
            }
            int index = actionbar.getSelectedNavigationIndex();
            // right to left swipe
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                Log.v(TAG, "Fling left");
                if (index < actionbar.getTabCount() - 1)
                    index++;
                actionbar.setSelectedNavigationItem(index);
                return true;
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // left to right swipe
                Log.v(TAG, "Fling right");
                if (index > 0)
                    index--;
                actionbar.setSelectedNavigationItem(index);
                return true;
            }
            return false;
        }
    }
}
