/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 * <p/>
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * <p/>
 * LICENSE:
 * <p/>
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * <p/>
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.util;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import com.google.common.io.LineProcessor;
import org.jetbrains.annotations.NotNull;


import java.io.*;


/**
 * A utility class for performing Task level I/O
 *
 * @author Tim Barlotta
 */
public class TaskIo {
    private final static String TAG = TaskIo.class.getSimpleName();

    @NotNull
    public static void loadFromFile(@NotNull File file, LineProcessor<String> lineProc) throws IOException {
        Files.readLines(file, Charsets.UTF_8, lineProc);
    }

    public static void writeToFile(@NotNull String contents, @NotNull File file, boolean append) throws IOException {
        Util.createParentDirectory(file);
        FileOutputStream str = new FileOutputStream(file, append);

        Writer fw = new BufferedWriter(new OutputStreamWriter(
                str, "UTF-8"));
        fw.write(contents);
        fw.close();
        str.close();
    }
}

