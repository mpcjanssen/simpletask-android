/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 *
 * LICENSE:
 *
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
@file:JvmName("TaskIo")
package nl.mpcjanssen.simpletask.util

import java.io.*

@Throws(IOException::class)
fun loadFromFile(file: File): List<String> {
    return file.readLines()
}

@Throws(IOException::class)
fun writeToFile(contents: String, file: File, append: Boolean) {
    try {
        createParentDirectory(file)
    } catch (e: IOException) {
        nl.mpcjanssen.simpletask.Logger.warn("TaskIO", "Couldn't create directory of ${file.absolutePath}", e)
        throw e
    }
    val str = FileOutputStream(file, append)

    val fw = BufferedWriter(OutputStreamWriter(
            str, "UTF-8"))
    fw.write(contents)
    fw.close()
    str.close()
}


