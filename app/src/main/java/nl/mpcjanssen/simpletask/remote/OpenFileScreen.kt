/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)

 * LICENSE:

 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.login.*
import nl.mpcjanssen.simpletask.Constants.BROWSE_FOR_DONE_FILE
import nl.mpcjanssen.simpletask.Constants.BROWSE_FOR_TODO_FILE
import nl.mpcjanssen.simpletask.Constants.BROWSE_TYPE

import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.Simpletask
import nl.mpcjanssen.simpletask.ThemedNoActionBarActivity
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.broadcastTasklistChanged

class OpenFileScreen : ThemedNoActionBarActivity() {


    fun performFileSearch(new: Boolean) {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.

        val fileType = intent.getIntExtra(BROWSE_TYPE,BROWSE_FOR_TODO_FILE)
        val name = when(fileType) {
            BROWSE_FOR_TODO_FILE -> "todo.txt"
            BROWSE_FOR_DONE_FILE -> "done.txt"
            else -> ""
        }

        val browseIntent = if (new) {
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                // Filter to only show results that can be "opened", such as a
                // file (as opposed to a list of contacts or timezones)
                addCategory(Intent.CATEGORY_OPENABLE)

                // Filter to show only images, using the image MIME data type.
                // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
                // To search for all documents available via installed storage providers,
                // it would be "*/*".
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, name)
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                // Filter to only show results that can be "opened", such as a
                // file (as opposed to a list of contacts or timezones)
                addCategory(Intent.CATEGORY_OPENABLE)

                // Filter to show only images, using the image MIME data type.
                // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
                // To search for all documents available via installed storage providers,
                // it would be "*/*".
                type = if (TodoApplication.config.showTxtOnly) "text/plain" else "*/*"
            }
        }
        browseIntent.flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or FLAG_GRANT_PREFIX_URI_PERMISSION
        startActivityForResult(browseIntent, if (new) CREATE_REQUEST_CODE else READ_REQUEST_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(TodoApplication.config.activeTheme)
        setContentView(R.layout.login)


        browse_new.setOnClickListener {
            performFileSearch(true)
        }

        browse.setOnClickListener {
            performFileSearch(false)
        }
    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
    }

    fun updateUri(uri: Uri, new: Boolean) {
        val fileType = intent.getIntExtra(BROWSE_TYPE,BROWSE_FOR_TODO_FILE)
        when(fileType) {
            BROWSE_FOR_TODO_FILE -> {
                TodoApplication.config.clearCache()
                if (new) {
                    TodoApplication.config.todoList = emptyList()
                }
                TodoApplication.config.todoUri = uri
                broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)

            }
            BROWSE_FOR_DONE_FILE -> {
                TodoApplication.config.doneUri = uri
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            resultData?.data?.also { uri ->
                Log.i(TAG, "Opened uri: $uri")
                updateUri(uri, false)
            }
        } else if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            resultData?.data?.also { uri ->
                Log.i(TAG, "Created uri: $uri")
                updateUri(uri, true)
            }
        }
        switchToTodolist()
    }

    companion object {
        private val READ_REQUEST_CODE: Int = 42
        private val CREATE_REQUEST_CODE: Int = 43

        internal val TAG = OpenFileScreen::class.java.simpleName
    }
}
