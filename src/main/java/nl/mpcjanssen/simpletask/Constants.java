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

import android.content.Intent;

public class Constants {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final String PREF_FIRSTRUN = "firstrun";
    public static final String PREF_ACCESSTOKEN_KEY = "accesstokenkey";
    public static final String PREF_ACCESSTOKEN_SECRET = "accesstokensecret";
    public static final String PREF_TODO_REV = "todo_rev";
    public static final String PREF_DONE_REV = "done_rev";
    public static final String PREF_NEED_TO_PUSH = "need_to_push";
    public static final String DROPBOX_MODUS = "dropbox";

    // Constants for creating shortcuts
    public final static String INTENT_SELECTED_TASK = "SELECTED_TASK";

    public final static String EXTRA_TASK = "TASK";
    public final static String EXTRA_FORCE_SYNC = "FORCE_SYNC";
    public final static String EXTRA_OVERWRITE = "OVERWRITE";

    public final static String BROADCAST_ACTION_ARCHIVE = "ACTION_ARCHIVE";
    public final static String BROADCAST_ACTION_LOGOUT = "ACTION_LOGOUT";
    public final static String BROADCAST_ASYNC_FAILED = "ASYNC_FAILED";
    public final static String BROADCAST_SYNC_CONFLICT = "SYNC_CONFLICT";
    public final static String BROADCAST_START_SYNC_WITH_REMOTE = "START_SYNC";
    public final static String BROADCAST_START_SYNC_TO_REMOTE = "START_SYNC_TO";
    public final static String BROADCAST_START_SYNC_FROM_REMOTE = "START_SYNC_FROM";
    public final static String BROADCAST_SET_MANUAL = "GO_OFFLINE";
    public final static String BROADCAST_UPDATE_UI = "UPDATE_UI";
    public final static String BROADCAST_SYNC_START = "SYNC_START";
    public final static String BROADCAST_SYNC_DONE = "SYNC_DONE";


    // Public intents
    public final static String INTENT_START_FILTER = "nl.mpcjanssen.simpletask.START_WITH_FILTER";
    public final static String INTENT_BACKGROUND_TASK = "nl.mpcjanssen.simpletask.BACKGROUND_TASK";
    public final static String EXTRA_BACKGROUND_TASK = "task";

    // Android OS specific constants
    public static final String ANDROID_EVENT = "vnd.android.cursor.item/event";

    // Constants for syncdirection
    public static final String INTENT_SYNC_DIRECTION = "SYNC_DIRECTION";
    public static final int PUSH = 0;
    public static final int PULL = 1;
}
