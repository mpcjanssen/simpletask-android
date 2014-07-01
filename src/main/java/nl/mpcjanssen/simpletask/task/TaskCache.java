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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.Simpletask;
import nl.mpcjanssen.simpletask.TodoApplication;
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
public class TaskCache {
    final static String TAG = TaskCache.class.getSimpleName();
    private final Context mCtx;
    private final String mTodoName;
    private final FileStoreInterface mFileStore;
    private ArrayList<Task> mTasks = null;


    public TaskCache(Context context, FileStoreInterface fileStore, String todoName) {
        this.mCtx = context;
        this.mTodoName = todoName;
        this.mFileStore = fileStore;
        reload();
    }

    public boolean archive(List<Task> tasksToArchive) {
        // fixme
        return true;
    }

    private void reload() {
       this.reload(mFileStore.get(mTodoName));
    }

    private void reload (ArrayList<String> loadedLines) {
        this.mTasks = new ArrayList<Task>();
        int index = 0;
        for (String s : loadedLines) {
            if (!"".equals(s.trim())) {
                this.mTasks.add(new Task(index, s));
            }
            index ++;
        }
        // File changed update widgets and UI
        notifyChanged();
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

    public Task addAsTask(String toAdd) {
        mFileStore.append(mTodoName,toAdd);
        Task t = new Task(0,toAdd);
        mTasks.add(t);
        notifyChanged();
        return t;
    }

    private void notifyChanged() {
        LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_UPDATE_UI));
    }

    public void delete(List<Task> tasks) {
        mTasks.removeAll(tasks);
        mFileStore.delete(mTodoName,Util.tasksToString(tasks));
        notifyChanged();
    }

    public void undoComplete(List<Task> tasks) {
        ArrayList<String> originalStrings = new ArrayList<String>();
        ArrayList<String> replacementStrings = new ArrayList<String>();

        for (Task t : tasks) {
            originalStrings.add(t.inFileFormat());
            t.markIncomplete();
            replacementStrings.add(t.inFileFormat());
        }
        mFileStore.update(mTodoName,originalStrings,replacementStrings);
        notifyChanged();
    }

    public void complete(List<Task> tasks) {
        ArrayList<String> originalStrings = new ArrayList<String>();
        ArrayList<String> replacementStrings = new ArrayList<String>();

        for (Task t : tasks) {
            originalStrings.add(t.inFileFormat());
            t.markComplete(DateTime.now(TimeZone.getDefault()));
            replacementStrings.add(t.inFileFormat());
        }
        mFileStore.update(mTodoName,originalStrings,replacementStrings);
        // fixme run autoarchive
        notifyChanged();
    }

    public void append(String taskText) {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add(taskText);
        append(lines);
    }

    public void append(ArrayList<String> lines) {
        mFileStore.append(mTodoName,lines);
        for (String line: lines ) {
            mTasks.add(new Task(0,line));
        }
        notifyChanged();
    }

    public void update(List<String> originalTasks, List<Task> updatedTasks) {
        mFileStore.update(mTodoName,originalTasks,Util.tasksToString(updatedTasks));
        notifyChanged();
    }

    public void defer(DateTime date, List<Task> tasksToDefer, int dateType) {
        // fixme
    }

    public void defer(String selected, List<Task> tasksToDefer, int dateType) {
        // fixme
    }
}