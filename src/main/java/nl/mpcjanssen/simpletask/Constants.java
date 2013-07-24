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

    public static final int SORT_UNSORTED = 0;
    public static final int SORT_REVERSE = 1;
    public static final int SORT_ALPHABETICAL = 2;
    public static final int SORT_PRIORITY = 3;
    public static final int SORT_PROJECT = 4;
    public static final int SORT_CONTEXT = 5;
    public static final int SORT_CREATION_DATE = 6;

    public static final String PREF_FIRSTRUN = "firstrun";
    public static final String PREF_ACCESSTOKEN_KEY = "accesstokenkey";
    public static final String PREF_ACCESSTOKEN_SECRET = "accesstokensecret";
    public static final String PREF_TODO_REV = "todo_rev";
    public static final String PREF_DONE_REV = "done_rev";
    public static final String PREF_NEED_TO_PUSH = "need_to_push";
    public static final String DROPBOX_MODUS = "dropbox";

    public final static long INVALID_ID = -1;
    public final static int INVALID_POSITION = -1;


    // Constants for creating shortcuts
    public final static String INTENT_SORT_ORDER = "SORTS";
    public final static String INTENT_CONTEXTS_FILTER = "CONTEXTS";
    public final static String INTENT_PROJECTS_FILTER = "PROJECTS";
    public final static String INTENT_PRIORITIES_FILTER = "PRIORITIES";
    public final static String INTENT_CONTEXTS_FILTER_NOT = "CONTEXTSnot";
    public final static String INTENT_PROJECTS_FILTER_NOT = "PROJECTSnot";
    public final static String INTENT_PRIORITIES_FILTER_NOT = "PRIORITIESnot";
    public final static String INTENT_TITLE = "TITLE";
    public final static String INTENT_SELECTED_TASK = "SELECTED_TASK";


    public final static String ACTIVE_SORTS = "SORTS";
    public static final String NORMAL_SORT = "+";
    public static final String REVERSED_SORT = "-";
    public static final String SORT_SEPARATOR = "!";

    public final static String EXTRA_PRIORITIES = "PRIORITIES";
    public final static String EXTRA_PRIORITIES_SELECTED = "PRIORITIES_SELECTED";
    public final static String EXTRA_PROJECTS = "PROJECTS";
    public final static String EXTRA_PROJECTS_SELECTED = "PROJECTS_SELECTED";
    public final static String EXTRA_CONTEXTS = "CONTEXTS";
    public final static String EXTRA_CONTEXTS_SELECTED = "CONTEXTS_SELECTED";
    public final static String EXTRA_SORTS_SELECTED = "SORTS_SELECTED";
    public final static String EXTRA_SEARCH = "SEARCH";
    public final static String EXTRA_TASK = "TASK";
    public final static String EXTRA_APPLIED_FILTERS = "APPLIED_FITERS";
    public final static String EXTRA_FORCE_SYNC = "FORCE_SYNC";
    public final static String EXTRA_OVERWRITE = "OVERWRITE";

    public final static String INTENT_ACTION_ARCHIVE = "nl.mpcjanssen.simpletask.ACTION_ARCHIVE";
    public final static String INTENT_ACTION_LOGOUT = "nl.mpcjanssen.simpletask.ACTION_LOGOUT";
    public final static String INTENT_ASYNC_SUCCESS = "nl.mpcjanssen.simpletask.ASYNC_SUCCESS";
    public final static String INTENT_ASYNC_FAILED = "nl.mpcjanssen.simpletask.ASYNC_FAILED";
    public final static String INTENT_SYNC_CONFLICT = "nl.mpcjanssen.simpletask.SYNC_CONFLICT";
    public final static String INTENT_START_SYNC_WITH_REMOTE = "nl.mpcjanssen.simpletask.START_SYNC";
    public final static String INTENT_START_SYNC_TO_REMOTE = "nl.mpcjanssen.simpletask.START_SYNC_TO";
    public final static String INTENT_START_SYNC_FROM_REMOTE = "nl.mpcjanssen.simpletask.START_SYNC_FROM";
    public final static String INTENT_SET_MANUAL = "nl.mpcjanssen.simpletask.GO_OFFLINE";
    public final static String INTENT_UPDATE_UI = "nl.mpcjanssen.simpletask.UPDATE_UI";
    public final static String INTENT_SYNC_START = "nl.mpcjanssen.simpletask.SYNC_START";
    public final static String INTENT_SYNC_DONE = "nl.mpcjanssen.simpletask.SYNC_DONE";
    public final static String INTENT_START_FILTER = "nl.mpcjanssen.simpletask.START_WITH_FILTER";
    public final static String INTENT_START_SELECTION = "nl.mpcjanssen.simpletask.START_WITH_SELECTION";

    // Constants for saving state
    public static final String LAYOUT_ID = "layoutId";
    public static final String LISTVIEW_ID = "lvId";
    public static final String CHECKBOX_ID = "cbId";
    public static final String FILTER_ITEMS = "items";
    public static final String INITIAL_SELECTED_ITEMS = "initialSelectedItems";
    public static final String INITIAL_NOT = "initialNot";
    public static final String SELECTION_MODE = "selection_mode";

    // Android OS specific constants
    public static final String ANDROID_EVENT = "vnd.android.cursor.item/event";

    // Constants for syncdirection
    public static final String INTENT_SYNC_DIRECTION = "SYNC_DIRECTION";
    public static final int PUSH = 0;
    public static final int PULL = 1;
}
