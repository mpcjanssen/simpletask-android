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
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
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
    @org.jetbrains.annotations.Nullable
    private ArrayList<Task> mTasks = null;
    private List<Task> mTasksToUpdate;
    @org.jetbrains.annotations.Nullable
    private ArrayList<String> mLists = null;
    @org.jetbrains.annotations.Nullable
    private ArrayList<String> mTags = null;
    private long mMaxId = 0;

    public TaskCache(Context context, @NotNull FileStoreInterface fileStore, String todoName) {
        this.mCtx = context;
        this.mTodoName = todoName;
        this.mFileStore = fileStore;
        this.mTasks = loadTasksFromStore(todoName);
        if (mTasks!=null && mTasks.size()>0) {
            this.mMaxId = mTasks.get(mTasks.size()-1).getId();
        }
    }

    public void archive(String targetPath, @Nullable List<Task> selectedTasks) {
        ArrayList<Task> tasksToArchive = new ArrayList<Task>();
        if (selectedTasks==null) {
            selectedTasks = mTasks;
        }
        if (mTasks==null) {
            return;
        }
        for (Task t: selectedTasks) {
            if (t.isCompleted()) {
                tasksToArchive.add(t);
            }
        }
        if (tasksToArchive.size()==0) {
            return;
        }
        for (Task t : tasksToArchive) {
            mTasks.remove(t);
        }
        notifyChanged();
        mFileStore.modify(mTodoName,null,null,null,Util.tasksToString(tasksToArchive));
        mFileStore.archive(targetPath, Util.tasksToString(tasksToArchive));
    }

    @NotNull
    private ArrayList<Task> loadTasksFromStore (@NotNull String path) {
        ArrayList<Task> result = new ArrayList<Task>();
        int index = 0;
        for (String s : mFileStore.get(path)) {
            if (!"".equals(s.trim())) {
                result.add(new Task(index, s));
            }
            index ++;
        }
        return result;
    }

    public int size() {
        if (mTasks==null) {
            return 0;
        } else {
            return mTasks.size();
        }
    }

    @Nullable
    public ArrayList<Task> getTasks() {
        return mTasks;
    }

    public Task getTaskAt(int position) {
        return mTasks.get(position);
    }

    @NotNull
    public ArrayList<Priority> getPriorities() {
        Set<Priority> res = new HashSet<Priority>();
        for (Task item : getTasks()) {
            res.add(item.getPriority());
        }
        ArrayList<Priority> ret = new ArrayList<Priority>(res);
        Collections.sort(ret);
        return ret;
    }

    @Nullable
    public ArrayList<String> getContexts(boolean includeNone) {
        if(mLists!=null) {
            return mLists;
        }
        Set<String> res = new HashSet<String>();
        for (Task item : mTasks) {
            res.addAll(item.getLists());
        }
        mLists = new ArrayList<String>();
        mLists.addAll(res);
        Collections.sort(mLists);
        if (includeNone) {
            mLists.add(0, "-");
        }
        return mLists;
    }

    @org.jetbrains.annotations.Nullable
    public ArrayList<String> getProjects(boolean includeNone) {
        if(mTags!=null) {
            return mTags;
        }
        Set<String> res = new HashSet<String>();
        for (Task item : mTasks) {
            res.addAll(item.getTags());
        }
        mTags = new ArrayList<String>();
        mTags.addAll(res);
        Collections.sort(mTags);
        if (includeNone) {
            mTags.add(0, "-");
        }
        return mTags;
    }


    public ArrayList<String> getDecoratedContexts(boolean includeNone) {
        return Util.prefixItems("@", getContexts(includeNone));
    }

    public ArrayList<String> getDecoratedProjects(boolean includeNone) {
        return Util.prefixItems("+", getProjects(includeNone));
    }

    private void notifyChanged() {
        // We have changes in cache
        // Invalidate cached lists and tags
        mLists = null;
        mTags = null;
        // Update any visible activity
        if (mCtx!=null) {
            Log.v(TAG, "Tasks have changed, reload cache");
            LocalBroadcastManager.getInstance(mCtx).sendBroadcast(new Intent(Constants.BROADCAST_UPDATE_UI));
        }
    }

    public void undoComplete(@NotNull List<Task> tasks) {
        ArrayList<String> originalStrings = new ArrayList<String>();

        for (Task t : tasks) {
            originalStrings.add(t.inFileFormat());
            t.markIncomplete();
        }
        modify(originalStrings, tasks,null,null);
    }

    public void complete(@NotNull List<Task> tasks, boolean originalDate) {
        ArrayList<String> originalStrings = new ArrayList<String>();
        ArrayList<Task> recurredTasks = new ArrayList<Task>();
        for (Task t : tasks) {
            originalStrings.add(t.inFileFormat());
            Task extra = t.markComplete(DateTime.now(TimeZone.getDefault()), originalDate);
            if (extra!=null) {
                recurredTasks.add(extra);
            }
        }
        modify(originalStrings, tasks, recurredTasks,null);
    }

    public void modify(List<String> originalTasks, List<Task> updatedTasks, List<Task> addedTasks, List<Task> deletedTasks) {
        // Updated tasks are already reflected in cache as they are passed by reference
        if (addedTasks!=null) {
            for (Task t : addedTasks) {
                // Update ID of task so file order sorting works properly (#119)
                mMaxId++;
                t.setId(mMaxId);
                mTasks.add(t);
            }
        }
        if (deletedTasks!=null) {
            for (Task t : deletedTasks) {
                mTasks.remove(t);
            }
        }
        mFileStore.modify(mTodoName,originalTasks,
                Util.tasksToString(updatedTasks),
                Util.tasksToString(addedTasks),
                Util.tasksToString(deletedTasks));
        notifyChanged();
    }

    public void defer(@NotNull String selected, @NotNull List<Task> tasksToDefer, int dateType) {
        ArrayList<String> originalTasks = Util.tasksToString(tasksToDefer);
        for (Task t: tasksToDefer) {
            switch (dateType) {
                case Task.DUE_DATE:
                    t.deferDueDate(selected, Util.getTodayAsString());
                    break;
                case Task.THRESHOLD_DATE:
                    t.deferThresholdDate(selected, Util.getTodayAsString());
                    break;
            }
        }
        modify(originalTasks,tasksToDefer,null,null);
    }

    public ArrayList<Task> getTasks(@NotNull ActiveFilter filter, @NotNull ArrayList<String> sorts) {
        Collections.sort(mTasks,new MultiComparator(sorts));
        return filter.apply(mTasks);
    }

    public List<Task> getTasksToUpdate() {
        return mTasksToUpdate;
    }

    public void setTasksToUpdate(List<Task> mTasksToUpdate) {
        this.mTasksToUpdate = mTasksToUpdate;
    }
}
