package nl.mpcjanssen.simpletask;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import nl.mpcjanssen.simpletask.task.token.Token;


public class TaskDisplayActivity extends ThemedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_task_display);
        // Directly select all for now
        setResult(Activity.RESULT_FIRST_USER + Token.SHOW_ALL);
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_task_display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.menu_filter_action) {
            setResult(Activity.RESULT_FIRST_USER + Token.SHOW_ALL);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
