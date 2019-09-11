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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.saf.login.*
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.Simpletask
import nl.mpcjanssen.simpletask.ThemedNoActionBarActivity
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.showToastLong

class OpenFileScreen : ThemedNoActionBarActivity() {


    fun performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            addCategory(Intent.CATEGORY_OPENABLE)

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            type = "*/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performFileSearch()
    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
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
                Log.i(TAG, "Uri: $uri")
                TodoApplication.config.todoUri = uri
                val takeFlags: Int = intent.flags and
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                // Check for the freshest data.
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }
        switchToTodolist()
    }

    companion object {
        private val READ_REQUEST_CODE: Int = 42
        internal val TAG = OpenFileScreen::class.java.simpleName
    }
}
