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


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.showToastShort
import java.io.IOException
import java.util.*


class AddTaskBackground : Activity() {
    private var log = Logger
    val TAG = "AddTaskBackground"

    public override fun onCreate(instance: Bundle?) {
        log = Logger;
        log.debug(TAG, "onCreate()")
        super.onCreate(instance)
        val m_app = this.application as SimpletaskApplication
        m_app.loadTodoList(true)

        val intent = intent
        val action = intent.action

        val append_text = m_app.shareAppendText
        if (intent.type.startsWith("text/")) {
            if (Intent.ACTION_SEND == action) {
                log.debug(TAG, "Share")
                var share_text = ""
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    val uri = intent.extras.get(Intent.EXTRA_STREAM) as Uri?
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

    private fun startAddTaskActivity(addedTasks: ArrayList<Task>) {
        log.info(TAG, "Starting addTask activity")
        val m_app = this.application as SimpletaskApplication
        val intent = Intent(this, AddTask::class.java)
        m_app.todoList.clearSelection()
        m_app.todoList.selectTasks(addedTasks,null)
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
        val todoList = m_app.todoList
        val addedTasks = ArrayList<Task>()
        log.debug(TAG, "Adding background tasks to todolist {} " + todoList)

        for (taskText in sharedText.split("\r\n|\r|\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
            if (taskText.trim({ it <= ' ' }).isEmpty()) {
                continue
            }
            var text : String
            if (!appendText.isEmpty()) {
                text = taskText + " " + appendText
            } else {
                text = taskText
            }
            val t: Task
            if (m_app.hasPrependDate()) {
                t = Task(text, DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT))
            } else {
                t = Task(text)
            }
            todoList.add(t, m_app.hasAppendAtEnd())
            addedTasks.add(t)
        }
        todoList.notifyChanged(m_app.fileStore, m_app.todoFileName, m_app.eol, m_app, true)
        finish()
        showToastShort(m_app, R.string.task_added)
        if (m_app.hasShareTaskShowsEdit()) {
            startAddTaskActivity(addedTasks)
        }
    }

}
