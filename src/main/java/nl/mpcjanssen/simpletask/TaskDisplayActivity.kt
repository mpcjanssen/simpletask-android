package nl.mpcjanssen.simpletask

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import nl.mpcjanssen.simpletask.task.ALL
import nl.mpcjanssen.simpletask.task.TToken


class TaskDisplayActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_task_display);
        // Directly select all for now
        setResult(Activity.RESULT_FIRST_USER + ALL)
        finish()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_task_display, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.menu_filter_action) {
            setResult(Activity.RESULT_FIRST_USER + ALL)
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
