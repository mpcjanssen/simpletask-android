/**
 * This file is part of Simpletask.

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * Copyright (c) 2013- Mark Janssen

 * LICENSE:

 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Mark Janssen
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * *
 * @copyright 2013- Mark Janssen
 */
package nl.mpcjanssen.simpletask


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.showToastShort
import java.io.IOException

const val REQUEST_READ_PERMISSION = 1

class AddTaskBackground : ThemedActivity() {
    private var log = Logger
    val TAG = "AddTaskBackground"

    private lateinit var m_app: SimpletaskApplication

    public override fun onCreate(instance: Bundle?) {
        log = Logger;
        log.debug(TAG, "onCreate()")
        super.onCreate(instance)
        m_app = this.application as SimpletaskApplication
        extractData()
    }

    private fun extractData() {
        val action = intent.action
        val append_text = m_app.shareAppendText
        if (intent.type.startsWith("text/")) {
            if (Intent.ACTION_SEND == action) {
                log.debug(TAG, "Share")
                var share_text = ""
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    val uri = intent.extras.get(Intent.EXTRA_STREAM) as Uri?
                    if (uri == null) {
                        return
                    }
                    if (uri.scheme.equals("file")) {
                        // With file:// schemes we need external storage access
                        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                            setContentView(R.layout.add_task)
                            ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_PERMISSION)
                            return
                        }
                    }

                    try {
                        val `is` = contentResolver.openInputStream(uri)
                        share_text = `is`.reader().readText()
                        `is`.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
                    if (text != null) {
                        share_text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT).toString()
                    }
                }
                addBackgroundTask(share_text, append_text)
            } else if ("com.google.android.gm.action.AUTO_SEND" == action) {
                // Called as note to self from google search/now
                noteToSelf(intent, append_text)

            } else if (Constants.INTENT_BACKGROUND_TASK == action) {
                log.debug(TAG, "Adding background task")
                if (intent.hasExtra(Constants.EXTRA_BACKGROUND_TASK)) {
                    addBackgroundTask(intent.getStringExtra(Constants.EXTRA_BACKGROUND_TASK), append_text)
                } else {
                    log.warn(TAG, "Task was not in extras")
                }

            }
        } else {
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            log.info(TAG, "Added link to content: ${imageUri.toString()}")
            addBackgroundTask(imageUri.toString(), append_text)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_READ_PERMISSION -> if (grantResults.size > 0 &&  grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(intent)
            } else {
                showToastShort(m_app, R.string.shared_data_access_denied)
            }
        }
        finish()
    }

    private fun startAddTaskActivity() {
        log.info(TAG, "Starting addTask activity")
        val intent = Intent(this, AddTask::class.java)
        startActivity(intent)
    }

    private fun noteToSelf(intent: Intent, append_text: String) {
        val task = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            log.debug(TAG, "Voice note added.")
        }
        addBackgroundTask(task, append_text)
    }

    private fun addBackgroundTask(sharedText: String, appendText: String) {
        val m_app = this.application as SimpletaskApplication
        val todoList = m_app.taskList
        val lines = sharedText.split("\r\n|\r|\n".toRegex()).dropLastWhile({ it.isEmpty() }).map { Task("it$appendText") }
        todoList.update(null,lines,null,m_app.hasAppendAtEnd())
        showToastShort(m_app, R.string.task_added)
        finish()
    }

}
