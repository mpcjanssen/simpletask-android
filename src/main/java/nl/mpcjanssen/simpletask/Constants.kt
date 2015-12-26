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
package nl.mpcjanssen.simpletask

object Constants {

    val DATE_FORMAT = "YYYY-MM-DD"

    // Constants for creating shortcuts
    val INTENT_SELECTED_TASK_POSITION = "SELECTED_TASK_POSITION"

    val BROADCAST_ACTION_ARCHIVE = "ACTION_ARCHIVE"
    val BROADCAST_ACTION_LOGOUT = "ACTION_LOGOUT"
    val BROADCAST_UPDATE_UI = "UPDATE_UI"
    val BROADCAST_FILE_WRITE_FAILED = "FILE_WRITE_FAILED"
    val BROADCAST_SYNC_START = "SYNC_START"
    val BROADCAST_SYNC_DONE = "SYNC_DONE"

    // Sharing constants
    val SHARE_FILE_NAME = "simpletask.txt"

    // Public intents
    val INTENT_START_FILTER = "nl.mpcjanssen.simpletask.START_WITH_FILTER"
    val INTENT_BACKGROUND_TASK = "nl.mpcjanssen.simpletask.BACKGROUND_TASK"


    // Intent extras
    val EXTRA_BACKGROUND_TASK = "task"
    val EXTRA_HELP_PAGE = "page"

    // Android OS specific constants
    val ANDROID_EVENT = "vnd.android.cursor.item/event"

    // Supported backends
    val STORE_DROPBOX = 0
    val STORE_SDCARD = 1

    // Help pages
    val HELP_INDEX = "index.en.md"
    val HELP_ADD_TASK = "addtask.en.md"

}
