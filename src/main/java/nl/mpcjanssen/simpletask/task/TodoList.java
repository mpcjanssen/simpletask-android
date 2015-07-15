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
package nl.mpcjanssen.simpletask.task;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import com.google.common.collect.Ordering;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.remote.BackupInterface;
import nl.mpcjanssen.simpletask.remote.FileStore;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RunnableFuture;


/**
 * Implementation of the in memory representation of the todo list
 *
 * @author Mark Janssen

 */
public class TodoList {
    final static String TAG = TodoList.class.getSimpleName();
    private final Context mCtx;
    private final Logger log;

    @org.jetbrains.annotations.Nullable
    private ArrayList<Task> mTasks = new ArrayList<Task>();
    private List<Task> mSelectedTask;
    @org.jetbrains.annotations.Nullable
    private ArrayList<String> mLists = null;
    @org.jetbrains.annotations.Nullable
    private ArrayList<String> mTags = null;
    private TodoListChanged mTodoListChanged;

    private Handler todolistQueue;
    private boolean loadQueued = false;
    private FileStoreInterface mFileStore;


    public TodoList(Context ctx,
                    TodoListChanged todoListChanged,
                    FileStoreInterface.FileChangeListener fileChanged,
                    String eol) {
        // Set up the message queue
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                todolistQueue = new Handler();
                Looper.loop();
            }
        });
        t.start();
        log = LoggerFactory.getLogger(this.getClass());
        this.mCtx = ctx;
        this.mTodoListChanged = todoListChanged;
        this.mFileStore = new FileStore(ctx, fileChanged, eol);

    }

    public void sync() {
        queueRunnable("Sync", new Runnable() {
            @Override
            public void run() {
                mFileStore.sync();
            }
        });
    }

    public void queueRunnable(final String description, Runnable r) {
        log.info("Handler: Queue " + description);
        while (todolistQueue==null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        todolistQueue.post(new LoggingRunnable(description, r));
    }

    public boolean loadQueued() {
        return loadQueued;
    }


    public void add(final Task t) {
        queueRunnable("Add task " + t.inFileFormat(), new Runnable() {
            @Override
            public void run() {
                mTasks.add(t);
            }
        });
    }


    public void remove(@NotNull final Task t) {
        queueRunnable("Remove", new Runnable() {
            @Override
            public void run() {
                mTasks.remove(t);
            }
        });
    }


    public int size() {
        if (mTasks == null) {
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
        if (mLists != null) {
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
        if (mTags != null) {
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


    public void undoComplete(@NotNull final List<Task> tasks) {
        queueRunnable("Uncomplete", new Runnable() {
            @Override
            public void run() {
                ArrayList<String> originalStrings = new ArrayList<String>();
                for (Task t : tasks) {
                    originalStrings.add(t.inFileFormat());
                    t.markIncomplete();
                }
            }
        });
    }

    public void complete(@NotNull final Task task,
                         final boolean originalDate,
                         final boolean keepPrio) {

        queueRunnable("Complete", new Runnable() {
            @Override
            public void run() {

                Task extra = task.markComplete(DateTime.now(TimeZone.getDefault()), originalDate);
                if (extra != null) {
                    mTasks.add(extra);
                }
                if (!keepPrio) {
                    task.setPriority(Priority.NONE);
                }
            }
        });
    }


    public void prioritize(final List<Task> tasks, final Priority prio) {
        queueRunnable("Complete", new Runnable() {
            @Override
            public void run() {
                for (Task t : tasks) {
                    t.setPriority(prio);
                }
            }
        });
    }

    public void defer(@NotNull final String deferString, @NotNull final Task tasksToDefer, final int dateType) {
        queueRunnable("Defer", new Runnable() {
            @Override
            public void run() {
                switch (dateType) {
                    case Task.DUE_DATE:
                        tasksToDefer.deferDueDate(deferString, Util.getTodayAsString());
                        break;
                    case Task.THRESHOLD_DATE:
                        tasksToDefer.deferThresholdDate(deferString, Util.getTodayAsString());
                        break;
                }
            }
        });
    }

    public List<Task> getSelectedTasks() {
        if (mSelectedTask != null) {
            return mSelectedTask;
        } else {
            return new ArrayList<>();
        }
    }

    public void setSelectedTasks(List<Task> selectedTasks) {
        this.mSelectedTask = selectedTasks;
    }


    public void notifyChanged(final boolean changed) {
        log.info("Handler: Queue notifychanged");
        todolistQueue.post(new Runnable() {
            @Override
            public void run() {
                log.info("Handler: Handle notifychanged");
                clearSelectedTasks();
                if (mTodoListChanged != null) {
                    log.info("TodoList changed, notifying listener and invalidating cached values");
                    mTags = null;
                    mLists = null;
                    mTodoListChanged.todoListChanged(changed);
                } else {
                    log.info("TodoList changed, but nobody is listening");
                }
            }
        });
    }

    public void deauthenticate() {
        if (mFileStore != null) {
            mFileStore.logout();
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
        if (mSelectedTask == null) {
            mSelectedTask = new ArrayList<>();
        }
        if (mSelectedTask.indexOf(t) == -1) {
            mSelectedTask.add(t);
        }
    }

    public void unSelectTask(Task t) {
        if (mSelectedTask == null) {
            mSelectedTask = new ArrayList<>();
        }
        mSelectedTask.remove(t);
    }

    public void clearSelectedTasks() {
        mSelectedTask = new ArrayList<>();
    }

    public void selectTask(int index) {
        if (index < 0 || index > mTasks.size() - 1) {
            return;
        }
        mSelectedTask.add(mTasks.get(index));
    }

    public void reload(final String filename, final BackupInterface backup, final LocalBroadcastManager lbm) {
        if (TodoList.this.loadQueued()) {
            log.info("Todolist reload is already queued waiting");
            return;
        }
        lbm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        loadQueued = true;
        queueRunnable("Reload", new Runnable() {
            @Override
            public void run() {
                clearSelectedTasks();
                try {
                    List<Task> tasks = mFileStore.loadTasksFromFile(filename, backup);
                    mTasks.clear();
                    mTasks.addAll(tasks);
                } catch (IOException e) {
                    log.error("Todolist load failed: {}", filename, e);
                    Util.showToastShort(mCtx, "Loading of todo file failed");
                    e.printStackTrace();
                }
                loadQueued = false;
                log.info("Todolist loaded, refresh UI");
                notifyChanged(false);
            }
        });
    }

    public void setEol(String eol) {
        this.mFileStore.setEol(eol);
    }

    public FileStoreInterface getFileStore() {
        return mFileStore;
    }

    public boolean fileStoreCanSync() {
        return mFileStore != null && mFileStore.supportsSync();
    }

    public void save(final String todoFileName, final BackupInterface backup) {
        queueRunnable("Save", new Runnable() {
            @Override
            public void run() {

                try {
                    mFileStore.saveTasksToFile(todoFileName, mTasks, backup);
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.showToastLong(mCtx, R.string.write_failed);
                }
            }
        });

    }

    public interface TodoListChanged {
        void todoListChanged(boolean save);
    }

    public class LoggingRunnable implements Runnable {
        private final String description;
        private final Runnable runnable;

        LoggingRunnable(String description, Runnable r) {
            log.info("Creating action " + description);
            this.description = description;
            this.runnable = r;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        public void run() {
            log.info("Execution action " + description);
            runnable.run();
        }

    }
}
