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

    public static final String DATE_FORMAT = "yyyy-MM-dd";


    // Constants for creating shortcuts
    public final static String INTENT_SELECTED_TASK_ID = "SELECTED_TASK_ID";
    public final static String INTENT_SELECTED_TASK_TEXT = "SELECTED_TASK_TEXT";

    public final static String INTENT_SORT_ORDER = "SORTS";
    public final static String INTENT_CONTEXTS_FILTER = "CONTEXTS";
    public final static String INTENT_PROJECTS_FILTER = "PROJECTS";
    public final static String INTENT_PRIORITIES_FILTER = "PRIORITIES";
    public final static String INTENT_CONTEXTS_FILTER_NOT = "CONTEXTSnot";
    public final static String INTENT_PROJECTS_FILTER_NOT = "PROJECTSnot";
    public final static String INTENT_PRIORITIES_FILTER_NOT = "PRIORITIESnot";
    public final static String INTENT_TITLE = "TITLE";

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
    public final static String EXTRA_TASK = "TASK";


    public final static String INTENT_UPDATE_UI = "nl.mpcjanssen.simpletask.UPDATE_UI";

    public final static String INTENT_START_FILTER = "nl.mpcjanssen.simpletask.START_WITH_FILTER";


    public static final String FILTER_ITEMS = "items";
    public static final String INITIAL_SELECTED_ITEMS = "initialSelectedItems";
    public static final String INITIAL_NOT = "initialNot";

    // Android OS specific constants
    public static final String ANDROID_EVENT = "vnd.android.cursor.item/event";
}
