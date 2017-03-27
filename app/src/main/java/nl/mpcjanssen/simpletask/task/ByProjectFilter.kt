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
package nl.mpcjanssen.simpletask.task

import java.util.ArrayList

/**
 * A filter that matches Tasks containing the specified projects

 * @author Tim Barlotta
 */
class ByProjectFilter(projects: List<String>?, internal var not: Boolean) : TaskFilter {
    /* FOR TESTING ONLY, DO NOT USE IN APPLICATION */
    internal val projects = ArrayList<String>()

    init {
        if (projects != null) {
            this.projects.addAll(projects)
        }
    }


    override fun apply(task: Task): Boolean {
        if (not) {
            return !filter(task)
        } else {
            return filter(task)
        }
    }

    fun filter(input: Task): Boolean {
        if (projects.size == 0) {
            return true
        }
        for (p in input.tags) {
            if (projects.contains(p)) {
                return true
            }
        }        /*
         * Match tasks without project if filter contains "-"
		 */
        return input.tags.size == 0 && projects.contains("-")
    }
}
