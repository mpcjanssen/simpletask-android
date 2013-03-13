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
package nl.mpcjanssen.todotxtholo;

public class Constants {

    public static final int SORT_UNSORTED = 0;
    public static final int SORT_REVERSE = 1;
    public static final int SORT_ALPHABETICAL = 2;
    public static final int SORT_PRIORITY = 3;
    public static final int SORT_PROJECT = 4;
    public static final int SORT_CONTEXT = 5;

    // Constants for creating shortcuts
    public final static String INTENT_ACTIVE_SORT = "SORT";
    public final static String INTENT_STRING_FILTER = "STRING";
    public final static String INTENT_CONTEXTS_FILTER = "CONTEXTS";
    public final static String INTENT_PROJECTS_FILTER = "PROJECTS";
    public final static String INTENT_PRIORITIES_FILTER = "PRIORITIES";
    public final static String INTENT_CONTEXTS_FILTER_NOT = "CONTEXTSnot";
    public final static String INTENT_PROJECTS_FILTER_NOT = "PROJECTSnot";
    public final static String INTENT_PRIORITIES_FILTER_NOT = "PRIORITIESnot";

    // Constants for passing data
    public final static String EXTRA_SORT_SELECTED = "SORT";
    public final static String EXTRA_PRIORITIES = "PRIORITIES";
    public final static String EXTRA_PRIORITIES_SELECTED = "PRIORITIES_SELECTED";
    public final static String EXTRA_PROJECTS = "PROJECTS";
    public final static String EXTRA_PROJECTS_SELECTED = "PROJECTS_SELECTED";
    public final static String EXTRA_CONTEXTS = "CONTEXTS";
    public final static String EXTRA_CONTEXTS_SELECTED = "CONTEXTS_SELECTED";
    public final static String EXTRA_TASK = "TASK";

    // Constants for fragment arguments
    public final static String ITEMS = "ITEMS";
    public final static String INITIAL_SELECTED_ITEMS = "INITIAL_SELECTED_ITEMS";
    public final static String INITIAL_NOT = "INITIAL_NOT";

    // Intents for main app
    public static final String INTENT_RELOAD_TASKBAG = "nl.mpcjanssen.todotxtholo.RELOAD_TASK_BAG";
    public static final String INTENT_START_FROM_SHORTCUT = "nl.mpcjanssen.todotxtholo.START_FROM_SHORTCUT";
}
