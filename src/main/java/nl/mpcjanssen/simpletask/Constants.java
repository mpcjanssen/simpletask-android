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

    public static final String PREF_ACCESSTOKEN_KEY = "accesstokenkey";
    public static final String PREF_ACCESSTOKEN_SECRET = "accesstokensecret";

    // Constants for creating shortcuts
    public final static String INTENT_SELECTED_TASK = "SELECTED_TASK";

    public final static String EXTRA_TASK = "TASK";

    public final static String BROADCAST_ACTION_ARCHIVE = "ACTION_ARCHIVE";
    public final static String BROADCAST_ACTION_LOGOUT = "ACTION_LOGOUT";
    public final static String BROADCAST_UPDATE_UI = "UPDATE_UI";
    public final static String BROADCAST_SYNC_START = "SYNC_START";
    public final static String BROADCAST_SYNC_DONE = "SYNC_DONE";


    // Public intents
    public final static String INTENT_START_FILTER = "nl.mpcjanssen.simpletask.START_WITH_FILTER";
    public final static String INTENT_BACKGROUND_TASK = "nl.mpcjanssen.simpletask.BACKGROUND_TASK";
    public final static String EXTRA_BACKGROUND_TASK = "task";

    // Android OS specific constants
    public static final String ANDROID_EVENT = "vnd.android.cursor.item/event";

    // Supported backends
    public static final int STORE_DROPBOX = 0x0;
}
