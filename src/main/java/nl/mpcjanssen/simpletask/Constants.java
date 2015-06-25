/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask;

public class Constants {

    public static final String DATE_FORMAT = "YYYY-MM-DD";

    // Constants for creating shortcuts
    public final static String INTENT_SELECTED_TASK_POSITION = "SELECTED_TASK_POSITION";

    public final static String BROADCAST_ACTION_ARCHIVE = "ACTION_ARCHIVE";
    public final static String BROADCAST_ACTION_LOGOUT = "ACTION_LOGOUT";
    public final static String BROADCAST_UPDATE_UI = "UPDATE_UI";
    public final static String BROADCAST_FILE_WRITE_FAILED = "FILE_WRITE_FAILED";
    public final static String BROADCAST_SYNC_START = "SYNC_START";
    public final static String BROADCAST_SYNC_DONE = "SYNC_DONE";
    public final static String BROADCAST_LOADING_START = "LOADING_START";
    public final static String BROADCAST_LOADING_DONE = "LOADING_DONE";

    // Sharing constants
    public final static String SHARE_FILE_NAME = "simpletask.txt";

    // Public intents
    public final static String INTENT_START_FILTER = "nl.mpcjanssen.simpletask.START_WITH_FILTER";
    public final static String INTENT_BACKGROUND_TASK = "nl.mpcjanssen.simpletask.BACKGROUND_TASK";


    // Intent extras
    public final static String EXTRA_BACKGROUND_TASK = "task";
    public final static String EXTRA_HELP_PAGE = "page";

    // Android OS specific constants
    public static final String ANDROID_EVENT = "vnd.android.cursor.item/event";

    // Supported backends
    public static final int STORE_DROPBOX = 0x0;
    public static final int STORE_SDCARD = 0x1;

    // Help pages
    public static final String HELP_INDEX = "index.en.md";
    public static final String HELP_ADD_TASK = "addtask.en.md";

}
