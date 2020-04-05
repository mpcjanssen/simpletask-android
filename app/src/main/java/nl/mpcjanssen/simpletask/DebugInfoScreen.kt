/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import nl.mpcjanssen.simpletask.util.appVersion
import nl.mpcjanssen.simpletask.util.shareText
import java.util.*

class DebugInfoScreen : ThemedActionBarActivity() {

    private var m_app: TodoApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.log)
        val mListView = findViewById<ListView>(R.id.listView)
        val mVersionInfo = findViewById<TextView>(R.id.versionInfo)
        mListView.isFastScrollEnabled = true
        mVersionInfo.text = "Version: ${appVersion(this)}"

        val myDataset = ArrayList<String>()
        m_app = application as TodoApplication
        for (line in LogCat.getLog()) {
            myDataset.add(line)
        }

        // specify an adapter (see also next example)
        val mAdapter = ArrayAdapter(this, R.layout.log_item, myDataset)
        mListView.adapter = mAdapter
        initToolbar()

    }

    private fun sendLog() {
        shareText(this@DebugInfoScreen, "${appVersion(this)} log", LogCat.getLog().joinToString("\n"))
    }

    private fun initToolbar(): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val inflater = menuInflater
        val toolbarMenu = toolbar.menu
        toolbarMenu.clear()
        inflater.inflate(R.menu.log_menu, toolbarMenu)
        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
            // Respond to the action bar's Up/Home button

                R.id.menu_share -> {
                    sendLog()
                    return@OnMenuItemClickListener true
                }
            }
            true
        })
        return true
    }
}
