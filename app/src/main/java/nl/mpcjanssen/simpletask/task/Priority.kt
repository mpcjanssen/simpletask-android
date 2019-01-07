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

import java.util.*

enum class Priority {

    A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    NONE {
        override val code: String = "-"
        override val fileFormat: String = ""
    };

    open val code: String = name
    open val fileFormat: String = "($name)"

    companion object {

        val codes: List<String>
            get() = values().map { it.code }.sorted()

        fun inCode(priorities: List<Priority>): ArrayList<String> {
            return ArrayList<String>(priorities.map { it.code })
        }

        fun toPriority(codes: List<String>): ArrayList<Priority> {
            return ArrayList<Priority>(codes.map { toPriority(it) })
        }

        fun toPriority(s: String?): Priority {
            val upper = s?.toUpperCase(Locale.US) ?: return NONE
            try {
                return valueOf(upper)
            }
            catch (e: IllegalArgumentException) {
                return NONE
            }
        }
    }

}
