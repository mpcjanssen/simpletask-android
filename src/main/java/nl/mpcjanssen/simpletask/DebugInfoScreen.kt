/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import nl.mpcjanssen.simpletask.dao.app.LogItem
import nl.mpcjanssen.simpletask.dao.app.LogItemDao
import nl.mpcjanssen.simpletask.util.appVersion
import nl.mpcjanssen.simpletask.util.shareText
import java.text.SimpleDateFormat
import java.util.*

class DebugInfoScreen : ThemedActivity() {

    private var m_app: SimpletaskApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.log)
        val mListView = findViewById(R.id.listView) as ListView
        val mVersionInfo = findViewById(R.id.versionInfo) as TextView
        mListView.isFastScrollEnabled = true
        mVersionInfo.text = "Version: ${appVersion(this)}"

        val myDataset = ArrayList<String>()
        m_app = application as SimpletaskApplication
        for (entry in m_app!!.logDao.queryBuilder().orderDesc(LogItemDao.Properties.Timestamp).list()) {
            val line = logItemToString(entry)
            myDataset.add(line)
        }

        // specify an adapter (see also next example)
        val mAdapter = ArrayAdapter(this, R.layout.log_item, myDataset)
        mListView.adapter = mAdapter

    }

    private fun logItemToString(entry: LogItem): String {
        val format = SimpleDateFormat("HH:mm:ss.S", Locale.US)
        return format.format(entry.timestamp) + "\t" + entry.severity + "\t" + entry.tag + "\t" + entry.message + "\t" + entry.exception
    }

    private fun sendLog() {
        val logContents = StringBuilder()
        for (item in m_app!!.logDao.loadAll()) {
            logContents.append(logItemToString(item)).append("\n")
        }
        shareText(this@DebugInfoScreen, "${appVersion(this)} log",  logContents.toString())
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        val inflater = menuInflater
        val toolbar_menu = toolbar.menu
        toolbar_menu.clear()
        inflater.inflate(R.menu.log_menu, toolbar_menu)
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
