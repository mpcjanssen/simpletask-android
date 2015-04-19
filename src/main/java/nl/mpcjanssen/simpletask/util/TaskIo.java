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
package nl.mpcjanssen.simpletask.util;

import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import com.google.common.io.LineProcessor;
import org.jetbrains.annotations.NotNull;
import android.support.v4.util.AtomicFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

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

    public static void writeToFile(@NotNull String contents, @NotNull File file, boolean append) {
        try {
            Util.createParentDirectory(file);
	    FileOutputStream str;
	    AtomicFile atom = new AtomicFile(file);
	    if (append) {
		     str = new FileOutputStream(file, append) ;
	    } else {

		    str = atom.startWrite();
	    }
            Writer fw = new BufferedWriter(new OutputStreamWriter(
                    str, "UTF-8"));
            fw.write(contents);
            fw.close();
	    str.close();
            atom.finishWrite(str);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
