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
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.common.collect.Ordering;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * Implementation of the in memory representation of the todo list
 *
 * @author Tim Barlotta, Mark Janssen
 *         <p/>
 *         The taskbag is the backing store for the task list used by the application
 *         It is loaded from and stored to the local copy of the todo.txt file and
 *         it is global to the application so all activities operate on the same copy
 */
public class TodoList {
    final static String TAG = TodoList.class.getSimpleName();
    private final Context mCtx;
    @org.jetbrains.annotations.Nullable
    private ArrayList<Task> mTasks = new ArrayList<Task>();
    private List<Task> mSelectedTask;
    @org.jetbrains.annotations.Nullable
    private ArrayList<String> mLists = null;
    @org.jetbrains.annotations.Nullable
    private ArrayList<String> mTags = null;
    private TodoListChanged mTodoListChanged;


    public TodoList(Context context, TodoListChanged todoListChanged) {
        this.mCtx = context;
        this.mTodoListChanged = todoListChanged;
    }

    public void add (Task t) {
        mTasks.add(t);
    }



    public void remove (@NotNull Task task) {
        mTasks.remove(task);
    }


    public int size() {
        if (mTasks==null) {
            return 0;
        } else {
            return mTasks.size();
        }
    }

    public Task get(int position) {
        return mTasks.get(position);
    }

    @NotNull
    public ArrayList<Priority> getPriorities() {
        Set<Priority> res = new HashSet<Priority>();
        for (Task item : mTasks) {
            res.add(item.getPriority());
        }
        ArrayList<Priority> ret = new ArrayList<Priority>(res);
        Collections.sort(ret);
        return ret;
    }

    @Nullable
    public ArrayList<String> getContexts() {
        if(mLists!=null) {
            return mLists;
        }
        Set<String> res = new HashSet<String>();
        for (Task item : mTasks) {
            res.addAll(item.getLists());
        }
        mLists = new ArrayList<String>();
        mLists.addAll(res);
        return mLists;
    }

    @org.jetbrains.annotations.Nullable
    public ArrayList<String> getProjects() {
        if(mTags!=null) {
            return mTags;
        }
        Set<String> res = new HashSet<String>();
        for (Task item : mTasks) {
            res.addAll(item.getTags());
        }
        mTags = new ArrayList<String>();
        mTags.addAll(res);
        return mTags;
    }


    public ArrayList<String> getDecoratedContexts() {
        return Util.prefixItems("@", getContexts());
    }

    public ArrayList<String> getDecoratedProjects() {
        return Util.prefixItems("+", getProjects());
    }


    public void undoComplete(@NotNull List<Task> tasks) {
        ArrayList<String> originalStrings = new ArrayList<String>();

        for (Task t : tasks) {
            originalStrings.add(t.inFileFormat());
            t.markIncomplete();
        }
    }

    public void complete(@NotNull Task task,
            boolean originalDate,
            boolean keepPrio) {
        ArrayList<String> originalStrings = new ArrayList<String>();
        ArrayList<Task> recurredTasks = new ArrayList<Task>();

            originalStrings.add(task.inFileFormat());
            Task extra = task.markComplete(DateTime.now(TimeZone.getDefault()), originalDate);
            if (extra!=null) {
                recurredTasks.add(extra);
            }
            if (!keepPrio) {
                task.setPriority(Priority.NONE);
            }

    }


    public void prioritize(List<Task> tasks, Priority prio) {
        for (Task t: tasks) {
            t.setPriority(prio);
        }
    }

    public void update(ArrayList<Integer> lines, ArrayList<String> newContents) {
        for (Integer line: lines) {
            mTasks.set(line, new Task(newContents.get(line)));
        }
    }

    public void defer(@NotNull String deferString, @NotNull List<Task> tasksToDefer, int dateType) {
        for (Task t: tasksToDefer) {
            switch (dateType) {
                case Task.DUE_DATE:
                    t.deferDueDate(deferString, Util.getTodayAsString());
                    break;
                case Task.THRESHOLD_DATE:
                    t.deferThresholdDate(deferString, Util.getTodayAsString());
                    break;
            }
        }
    }

    public List<Task> getSelectedTasks() {
        if (mSelectedTask !=null) {
            return mSelectedTask;
        } else {
            return new ArrayList<>();
        }
    }

    public void setSelectedTasks(List<Task> selectedTasks) {
        this.mSelectedTask = selectedTasks;
    }

    public void notifyChanged() {
        clearSelectedTasks();
        if (mTodoListChanged!=null) {
            Log.v(TAG, "TodoList changed, notifying listener");
            mTodoListChanged.todoListChanged();
        } else {
            Log.v(TAG, "TodoList changed, but nobody is listening");
        }

    }

    public List<Task> getTasks() {
        return mTasks;
    }

    public List<Task> getSortedTasksCopy(@NotNull ActiveFilter filter, @NotNull ArrayList<String> sorts, boolean caseSensitive) {
        List<Task> filteredTasks = filter.apply(mTasks);
        return Ordering.from(new MultiComparator(sorts, caseSensitive, filteredTasks)).sortedCopy(filteredTasks);
    }

    public void selectTask(Task t) {
        if (mSelectedTask==null) {
            mSelectedTask = new ArrayList<>();
        }
        if (mSelectedTask.indexOf(t)==-1) {
            mSelectedTask.add(t);
        }
    }

    public void unSelectTask(Task t) {
        if (mSelectedTask==null) {
            mSelectedTask = new ArrayList<>();
        }
        mSelectedTask.remove(t);
    }

    public void clearSelectedTasks() {
        mSelectedTask = new ArrayList<>();
    }

    public void selectTask(int index) {
        if (index < 0 || index > mTasks.size()-1) {
            return;
        }
        mSelectedTask.add(mTasks.get(index));
    }


    public interface TodoListChanged {
        void todoListChanged();
    }
}
