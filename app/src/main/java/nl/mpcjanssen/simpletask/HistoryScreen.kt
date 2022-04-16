/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.widget.ScrollView
import android.widget.TextView

import nl.mpcjanssen.simpletask.util.createCachedDatabase
import nl.mpcjanssen.simpletask.util.shareText
import nl.mpcjanssen.simpletask.util.showToastShort
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.lang.Integer.max
import java.text.SimpleDateFormat
import java.util.*

class HistoryScreen : ThemedActionBarActivity() {


    private var toolbar_menu: Menu? = null
    private var mScroll = 0

    lateinit var dbFile : File
    var cursorIdx = 0
    private var m_app: TodoApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m_app = application as TodoApplication

        setContentView(R.layout.history)

    }


    private fun shareHistory() {
        val shareIntent = Intent(android.content.Intent.ACTION_SEND)
        shareIntent.type = "application/x-sqlite3"
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
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


    fun initToolbar(): Boolean {
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

    }

    private fun showNext() {

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




    }


    private fun updateMenu() {



    }

    companion object {
        private val TAG = "HistoryScreen"
    }
}
