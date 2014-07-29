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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Util;

public class FilterScriptFragment extends Fragment {

    final static String TAG = FilterScriptFragment.class.getSimpleName();
    private EditText txtJavaScript;
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
        outState.putString(ActiveFilter.INTENT_JAVASCRIPT_FILTER,getJavascript());
        outState.putString(ActiveFilter.INTENT_JAVASCRIPT_TEST_TASK_FILTER,getTestTask());
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

        txtJavaScript = (EditText) layout.findViewById(R.id.txt_javascript);
        txtTestTask = (EditText) layout.findViewById(R.id.txt_testtask);
        tvResult = (TextView) layout.findViewById(R.id.result);
        tvBooleanResult = (TextView) layout.findViewById(R.id.booleanResult);
        btnTest = (Button) layout.findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = Context.enter();
                // Disable JVM on the fly, we are on Dalvik/Art
                context.setOptimizationLevel(-1);
                ScriptableObject scope = context.initStandardObjects();
                try {
                    Script script = context.compileString(getJavascript(), "javascript", 1, null);
                    Task t = new Task(0, txtTestTask.getText().toString());
                    Util.fillScope(scope, t);
                    Object result = script.exec(context, scope);
                    tvResult.setText(Context.toString(result));
                    if (Context.toBoolean(result)) {
                        tvBooleanResult.setText("true");
                    } else {
                        tvBooleanResult.setText("false");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    tvResult.setText(e.getMessage());
                    tvBooleanResult.setText("error");
                }

            }
        });
        if (savedInstanceState != null) {
            txtJavaScript.setText(savedInstanceState.getString(ActiveFilter.INTENT_JAVASCRIPT_FILTER,""));
            txtTestTask.setText(savedInstanceState.getString(ActiveFilter.INTENT_JAVASCRIPT_TEST_TASK_FILTER,""));
        } else {
            txtJavaScript.setText(arguments.getString(ActiveFilter.INTENT_JAVASCRIPT_FILTER,""));
            txtTestTask.setText(arguments.getString(ActiveFilter.INTENT_JAVASCRIPT_TEST_TASK_FILTER,""));
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

    public String getJavascript() {
        Bundle arguments = getArguments();
        if (txtJavaScript == null) {
            return arguments.getString(ActiveFilter.INTENT_JAVASCRIPT_FILTER, "");
        } else {
            return txtJavaScript.getText().toString();
        }
    }

    public String getTestTask() {
        Bundle arguments = getArguments();
        if (txtTestTask == null) {
            return arguments.getString(ActiveFilter.INTENT_JAVASCRIPT_TEST_TASK_FILTER, "");
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
