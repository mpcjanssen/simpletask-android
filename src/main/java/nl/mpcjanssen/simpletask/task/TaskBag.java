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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.Simpletask;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.util.Util;


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
    private final FileStoreInterface mFileStore;
    private String mTodoName;
    private Preferences preferences;
    private ArrayList<Task> mTasks = null;


    public TaskBag(Preferences taskBagPreferences,
                   FileStoreInterface fileStore, String todoName) {
        this.preferences = taskBagPreferences;
        this.mFileStore = fileStore;
        this.mTodoName = todoName;
        reload();
    }

    private String contents () {
        ArrayList<String> contents = new ArrayList<String>();
        for (Task t : getTasks()) {
            contents.add(t.inFileFormat());
        }
        return Util.join(contents,"\n");
    }

    private void store(ArrayList<Task> tasks) {
        mFileStore.store(mTodoName, Util.tasksToString(tasks, preferences.isUseWindowsLineBreaksEnabled()));
        mTasks = null;
    }

    public void store() {
        store(getTasks());
    }

    public boolean archive(List<Task> tasksToArchive) {
        boolean windowsLineBreaks = preferences.isUseWindowsLineBreaksEnabled();

        ArrayList<Task> archivedTasks = new ArrayList<Task>(getTasks().size());
        ArrayList<Task> remainingTasks = new ArrayList<Task>(getTasks().size());

        for (Task task : getTasks()) {
            if (tasksToArchive != null) {
                // Archive selected tasks
                if (tasksToArchive.indexOf(task) != -1) {
                    archivedTasks.add(task);
                } else {
                    remainingTasks.add(task);
                }
            } else {
                // Archive completed tasks
                if (task.isCompleted()) {
                    archivedTasks.add(task);
                } else {
                    remainingTasks.add(task);
                }
            }
        }

        // append completed tasks to done.txt
        if (!mFileStore.append(new File(mTodoName).getParent() + "/done.txt",
                Util.tasksToString(archivedTasks, windowsLineBreaks))) {
            return false;
        }
        this.store(remainingTasks);
        return true;
    }

    private void reload() {
       this.reload(mFileStore.get(mTodoName, preferences));
    }

    private void reload (ArrayList<String> loadedLines) {
        this.mTasks = new ArrayList<Task>();
        int index = 0;
        for (String s : loadedLines) {
            this.mTasks.add(new Task(index, s));
            index ++;
        }
    }

    public int size() {
        return getTasks().size();
    }

    public ArrayList<Task> getTasks() {
        if (mTasks == null) {
            reload();
        }
        return mTasks;
    }

    public Task getTaskAt(int position) {
        return getTasks().get(position);
    }

    public Task addAsTask(String input) {
        ArrayList<Task> tasks = getTasks();
        try {
            Task task = new Task(tasks.size(), input,
                    (preferences.isPrependDateEnabled() ? DateTime.today(TimeZone.getDefault()) : null));
            if (preferences.addAtEnd()) {
                tasks.add(task);
            } else {
                tasks.add(0, task);
            }
            store();
            return task;
        } catch (Exception e) {
            throw new TaskPersistException("An error occurred while adding {"
                    + input + "}", e);
        }
    }

    public void updateTask(Task task, String input) {
        int index = getTasks().indexOf(task);
        if (index!=-1) {
            getTasks().get(index).init(input, null);
            store();
        }
    }

    public void delete(List<Task> tasksToDelete) {
        ArrayList<Task> tasks = getTasks();
        for (Task t : tasksToDelete) {
            if (t!=null) {
                tasks.remove(t);
            }
        }
        store();
    }

    public ArrayList<Priority> getPriorities() {
        // TODO cache this after reloads?
        Set<Priority> res = new HashSet<Priority>();
        for (Task item : getTasks()) {
            res.add(item.getPriority());
        }
        ArrayList<Priority> ret = new ArrayList<Priority>(res);
        Collections.sort(ret);
        return ret;
    }

    public ArrayList<String> getContexts(boolean includeNone) {
        // TODO cache this after reloads?
        Set<String> res = new HashSet<String>();
        for (Task item : getTasks()) {
            res.addAll(item.getLists());
        }
        ArrayList<String> ret = new ArrayList<String>(res);
        Collections.sort(ret);
        if (includeNone) {
            ret.add(0, "-");
        }
        return ret;
    }

    public ArrayList<String> getProjects(boolean includeNone) {
        // TODO cache this after reloads?
        Set<String> res = new HashSet<String>();
        for (Task item : getTasks()) {
            res.addAll(item.getTags());
        }
        ArrayList<String> ret = new ArrayList<String>(res);
        Collections.sort(ret);
        if (includeNone) {
            ret.add(0, "-");
        }
        return ret;
    }

    public ArrayList<String> getDecoratedContexts(boolean includeNone) {
        return Util.prefixItems("@", getContexts(includeNone));
    }

    public ArrayList<String> getDecoratedProjects(boolean includeNone) {
        return Util.prefixItems("+", getProjects(includeNone));
    }

    public static class Preferences {
        private final SharedPreferences sharedPreferences;

        public Preferences(SharedPreferences sharedPreferences) {
            this.sharedPreferences = sharedPreferences;
        }

        public void setUseWindowsLineBreaksEnabled(boolean enabled) {
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putBoolean("linebreakspref", enabled);
            edit.commit();

        }
        public boolean isUseWindowsLineBreaksEnabled() {
            return sharedPreferences.getBoolean("linebreakspref", false);
        }

        public boolean isPrependDateEnabled() {
            return sharedPreferences.getBoolean("todotxtprependdate", true);
        }

        public boolean isOnline() {
            return !sharedPreferences.getBoolean("workofflinepref", false);
        }

	public boolean addAtEnd() {
	    return sharedPreferences.getBoolean("addtaskatendpref", true);
	}
    }

}
