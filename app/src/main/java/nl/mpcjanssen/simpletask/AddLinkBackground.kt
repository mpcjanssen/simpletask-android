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
import android.util.Log
import androidx.core.app.ShareCompat
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.showToastLong
import nl.mpcjanssen.simpletask.util.showToastShort
import nl.mpcjanssen.simpletask.util.todayAsString
import java.io.IOException

class AddLinkBackground : Activity() {
    val TAG = "AddLinkBackground"

    public override fun onCreate(instance: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(instance)

        val append_text = TodoApplication.config.shareAppendText
        val intentReader = ShareCompat.IntentReader.from(this)
        val uri = intentReader.stream
        val subject = intentReader.subject ?: ""
        val mimeType = intentReader.type
        Log.i(TAG, "Added link to content ($mimeType)")
        if (uri == null) {
            showToastLong(TodoApplication.app, R.string.share_link_failed)
        } else {
            addBackgroundTask("$subject $uri", append_text)
        }
    }

    private fun addBackgroundTask(sharedText: String, appendText: String) {
        val todoList = TodoApplication.todoList
        Log.d(TAG, "Adding background tasks to todolist $todoList")

        val rawLines = sharedText.split("\r\n|\r|\n".toRegex()).filterNot(String::isBlank)
        val lines = if (appendText.isBlank()) { rawLines } else {
            rawLines.map { "$it $appendText" }
        }
        val tasks = lines.map { text ->
            if (TodoApplication.config.hasPrependDate) { Task(text, todayAsString) } else { Task(text) }
        }

        todoList.add(tasks, TodoApplication.config.hasAppendAtEnd)
        todoList.notifyTasklistChanged(TodoApplication.config.todoFileName,  true, true)
        showToastShort(TodoApplication.app, R.string.link_added)
        if (TodoApplication.config.hasShareTaskShowsEdit) {
            todoList.editTasks(this, tasks, "")
        }
        finish()
    }
}
