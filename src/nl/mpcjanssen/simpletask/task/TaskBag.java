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
package nl.mpcjanssen.simpletask.task;

import android.content.SharedPreferences;
import nl.mpcjanssen.simpletask.Simpletask;

import java.util.*;


/**
 * Implementation of the TaskBag
 *
 * @author Tim Barlotta, Mark Janssen
 *         <p/>
 *         The taskbag is the backing store for the task list used by the application
 *         It is loaded from and stored to the local copy of the todo.txt file and
 *         it is global to the application so all activities operate on the same copy
 */
public class TaskBag {
    final static String TAG = Simpletask.class.getSimpleName();
    private Preferences preferences;
    private final LocalFileTaskRepository localRepository;
    private ArrayList<Task> tasks = new ArrayList<Task>();

    public TaskBag(Preferences taskBagPreferences,
                   LocalFileTaskRepository localTaskRepository) {
        this.preferences = taskBagPreferences;
        this.localRepository = localTaskRepository;
    }

    private void store(ArrayList<Task> tasks) {
        localRepository.store(tasks);
    }

    public void store() {
        store(this.tasks);
    }

    public void archive() {
        localRepository.archive(tasks);
        reload();
    }

    public void reload() {
        this.tasks = localRepository.load();
    }

    public int size() {
        return tasks.size();
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void addAsTask(String input) {
        Task task = new Task(tasks.size(), input,
                (preferences.isPrependDateEnabled() ? new Date() : null));
        tasks.add(task);
    }

    public void delete(Task task) {
        tasks.remove(task);
    }


    public ArrayList<Priority> getPriorities() {
        Set<Priority> res = new HashSet<Priority>();
        for (Task item : tasks) {
            res.add(item.getPriority());
        }
        ArrayList<Priority> ret = new ArrayList<Priority>(res);
        Collections.sort(ret);
        return ret;
    }

    public ArrayList<String> getContexts(boolean includeEmpty) {
        Set<String> res = new HashSet<String>();
        for (Task item : tasks) {
            res.addAll(item.getContexts());
        }
        ArrayList<String> ret = new ArrayList<String>(res);
        Collections.sort(ret);
        if (includeEmpty) {
            ret.add(0, "-");
        }
        return ret;
    }

    public ArrayList<String> getProjects(boolean includeEmpty) {
        Set<String> res = new HashSet<String>();
        for (Task item : tasks) {
            res.addAll(item.getProjects());
        }
        ArrayList<String> ret = new ArrayList<String>(res);
        Collections.sort(ret);
        if (includeEmpty) {
            ret.add(0, "-");
        }
        return ret;
    }

    public static class Preferences {
        private final SharedPreferences sharedPreferences;

        public Preferences(SharedPreferences sharedPreferences) {
            this.sharedPreferences = sharedPreferences;
        }

        public boolean isUseWindowsLineBreaksEnabled() {
            return sharedPreferences.getBoolean("linebreakspref", false);
        }

        public boolean isPrependDateEnabled() {
            return sharedPreferences.getBoolean("todotxtprependdate", true);
        }
    }
}
