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

import java.util.Locale

/**
 * A filter that matches Tasks containing the specified text

 * @author Mark Janssen
 */
class ByTextFilter(var text: String?, private val caseSensitive: Boolean) : TaskFilter {


    init {
        val origText = text ?: ""
        text = if (caseSensitive) origText else origText.toUpperCase(Locale.getDefault())
    }

    override fun apply(t: Task): Boolean {
        val taskText = if (caseSensitive)
            t.inFileFormat()
        else
            t.inFileFormat().toUpperCase(Locale.getDefault())
        var fuzzy: String = text ?: return true
        taskText.forEach {
            if (fuzzy.length == 0 ) return true
            if (it == fuzzy.get(0)) {
                fuzzy = fuzzy.substring(1)
            }
        }
        return fuzzy.length == 0
    }
}
