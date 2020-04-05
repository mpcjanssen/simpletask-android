/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import nl.mpcjanssen.simpletask.dao.DB_FILE
import nl.mpcjanssen.simpletask.dao.TodoFile
import nl.mpcjanssen.simpletask.util.createCachedDatabase
import nl.mpcjanssen.simpletask.util.shareText
import nl.mpcjanssen.simpletask.util.showToastShort
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HistoryScreen : ThemedActionBarActivity() {


    private var toolbar_menu: Menu? = null
    private var mScroll = 0
    val db = TodoApplication.db
    lateinit var history: List<TodoFile>
    private lateinit var dbFile : File
    private var cursorIdx = 0
    private var m_app: TodoApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m_app = application as TodoApplication
        dbFile = getDatabasePath(DB_FILE)
        if (dbFile.exists()) {
            var title = title.toString()
            title = title + " (" + dbFile.length() / 1024 + "KB)"
            setTitle(title)
        }
        setContentView(R.layout.history)
        doAsync {
            history = db.todoFileDao().getAll()
            uiThread {
                initToolbar()
                displayCurrent()
            }
        }
    }


    private fun shareHistory() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "application/x-sqlite3"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                "Simpletask History Database")
        try {
            createCachedDatabase(this, dbFile)
            val fileUri = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/" + dbFile.name)
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create file for sharing", e)
        }

        startActivity(Intent.createChooser(shareIntent, "Share History Database"))

    }


    private fun initToolbar(): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val inflater = menuInflater
        toolbar_menu = toolbar.menu
        toolbar_menu!!.clear()
        inflater.inflate(R.menu.history_menu, toolbar_menu)
        updateMenu()
        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                // Respond to the action bar's Up/Home button
                R.id.menu_prev -> {
                    showPrev()
                    return@OnMenuItemClickListener true
                }
                R.id.menu_next -> {
                    showNext()
                    return@OnMenuItemClickListener true
                }
                R.id.menu_clear_database -> {
                    clearDatabase()
                    return@OnMenuItemClickListener true
                }
                R.id.menu_share -> {
                    if (history.isEmpty()) {
                        showToastShort(this@HistoryScreen, "Nothing to share")
                    } else {
                        shareText(this@HistoryScreen, "Old todo version", history.getOrNull(cursorIdx)?.contents
                                ?: "Nothing to share")
                    }
                    return@OnMenuItemClickListener true
                }
                R.id.menu_share_database -> {
                    shareHistory()
                    return@OnMenuItemClickListener true
                }
            }
            true
        })
        return true
    }

    private fun clearDatabase() {
        Log.i(TAG, "Clearing history database")
        doAsync {
            db.todoFileDao().deleteAll()
            history = db.todoFileDao().getAll()
            uiThread {
                updateMenu()
                displayCurrent()
            }
        }
    }

    private fun showNext() {
        saveScroll()
        cursorIdx = maxOf(cursorIdx + 1, history.size - 1)
        displayCurrent()
    }

    private fun saveScroll() {
        val sv = findViewById<ScrollView>(R.id.scrollbar)
        mScroll = sv.scrollY
    }

    private fun showPrev() {
        saveScroll()
        cursorIdx = minOf(0, cursorIdx - 1)
        displayCurrent()
    }

    private fun displayCurrent() {

        val current = history.getOrNull(cursorIdx)


        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val fileView = findViewById<TextView>(R.id.history_view)

        val nameView = findViewById<TextView>(R.id.name)
        val dateView = findViewById<TextView>(R.id.date)
        val sv = findViewById<ScrollView>(R.id.scrollbar)
        fileView.text = current?.contents ?: "No history"
        nameView.text = current?.name ?: ""
        dateView.text = format.format(current?.date ?: Date())
        sv.scrollY = mScroll
        updateMenu()


    }


    private fun updateMenu() {
        if (toolbar_menu == null) {
            return
        }
        val prev = toolbar_menu!!.findItem(R.id.menu_prev)
        val next = toolbar_menu!!.findItem(R.id.menu_next)

        val enablePrev = cursorIdx > 0
        val enableNext = cursorIdx < history.size - 1

        prev.isEnabled = enablePrev
        next.isEnabled = enableNext


    }

    companion object {
        private const val TAG = "HistoryScreen"
    }
}
