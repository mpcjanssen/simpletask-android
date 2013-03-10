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
package nl.mpcjanssen.todotxtholo.util;

import android.util.Log;
import nl.mpcjanssen.todotxtholo.task.Task;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.dropbox.sync.android.DbxFile;


/**
 * A utility class for performing Task level I/O
 *
 * @author Tim Barlotta
 */
public class TaskIo {
    private final static String TAG = TaskIo.class.getSimpleName();


    public static ArrayList<Task> loadTasksFromFile(DbxFile file)
            throws IOException {
        ArrayList<Task> items = new ArrayList<Task>();
        BufferedReader in = null;

            InputStream is = file.getReadStream();
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
        return items;
    }

    public static void writeTasksToFile(List<Task> tasks, DbxFile file) {
    	List<String> items = new ArrayList<String>();
        try {
        		for (Task task : tasks) {
                    if (task!=null) {
                    	items.add(task.inFileFormat());
                    }
        		}
            file.writeString(Util.join(items, "\n"));    
        	} catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
