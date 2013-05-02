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
import nl.mpcjanssen.simpletask.task.Task;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * A utility class for performing Task level I/O
 *
 * @author Tim Barlotta
 */
public class TaskIo {
    private final static String TAG = TaskIo.class.getSimpleName();

    public static ArrayList<Task> loadTasksFromStream(InputStream is)
            throws IOException {
        ArrayList<Task> items = new ArrayList<Task>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(is));
            String line;
            long counter = 0L;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    items.add(new Task(counter, line));
                }
                counter++;
            }
        } finally {
            Util.closeStream(in);
            Util.closeStream(is);
        }
        return items;
    }

    public static ArrayList<Task> loadTasksFromFile(File file)
            throws IOException {
        ArrayList<Task> items = new ArrayList<Task>();
        BufferedReader in = null;
        if (!file.exists()) {
            Log.w(TAG, file.getAbsolutePath() + " does not exist!");
        } else {
            InputStream is = new FileInputStream(file);
            try {
                in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                long counter = 0L;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0) {
                        items.add(new Task(counter, line));
                    }
                    counter++;
                }
            } finally {
                Util.closeStream(in);
                Util.closeStream(is);
            }
        }
        return items;
    }

    public static void writeToFile(List<Task> tasks, File file,
                                   boolean useWindowsBreaks) {
        writeToFile(tasks, file, false, useWindowsBreaks);
    }

    public static void writeToFile(List<Task> tasks, File file,
                                   boolean append, boolean useWindowsBreaks) {
        try {
            Util.createParentDirectory(file);
            Writer fw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, append), "UTF-8"));
                for (Task task : tasks) {
                    if (task!=null) {
                    String fileFormat = task.inFileFormat();
                    fw.write(fileFormat);
                    if (useWindowsBreaks) {
                        // Log.v(TAG, "Using Windows line breaks");
                        fw.write("\r\n");
                    } else {
                        // Log.v(TAG, "NOT using Windows line breaks");
                        fw.write("\n");
                    }
                }
            }
            fw.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
