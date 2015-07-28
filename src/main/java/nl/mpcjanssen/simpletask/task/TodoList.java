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

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import com.google.common.collect.Ordering;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.remote.BackupInterface;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;



/**
 * Implementation of the in memory representation of the todo list
 *
 * @author Mark Janssen

 */
public class TodoList {
    final static String TAG = TodoList.class.getSimpleName();
    private final Logger log;
    private final boolean startLooper;

    @NonNull
    private List<Task> mTasks = new ArrayList<>();
    @NonNull
    private List<Task> mSelectedTask = new ArrayList<>();
    @Nullable
    private ArrayList<String> mLists = null;
    @Nullable
    private ArrayList<String> mTags = null;
    private TodoListChanged mTodoListChanged;

    private Handler todolistQueue;
    private boolean loadQueued = false;


    public TodoList(TodoListChanged todoListChanged) {
        this(todoListChanged, true);
    }


    public TodoList(TodoListChanged todoListChanged,
                    boolean startLooper) {
        this.startLooper = startLooper;
        // Set up the message queue
        if (startLooper) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    todolistQueue = new Handler();
                    Looper.loop();
                }
            });
            t.start();
        }
        log = LoggerFactory.getLogger(this.getClass());
        this.mTodoListChanged = todoListChanged;


    }


    public void queueRunnable(final String description, Runnable r) {
        log.info("Handler: Queue " + description);
        while (todolistQueue==null && startLooper ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (todolistQueue!=null) {
            todolistQueue.post(new LoggingRunnable(description, r));
        } else {
            r.run();
        }
    }

    public boolean loadQueued() {
        return loadQueued;
    }


    public void add(final Task t) {
        queueRunnable("Add task", new Runnable() {
            @Override
            public void run() {
                log.debug("Adding task '{}' into {}", t.inFileFormat(), TodoList.this);
                mTasks.add(t);
            }
        });
    }


    public void remove(@NonNull final Task t) {
        queueRunnable("Remove", new Runnable() {
            @Override
            public void run() {
                mTasks.remove(t);
            }
        });
    }


    public int size() {
        return mTasks.size();
    }

    public Task get(int position) {
        return mTasks.get(position);
    }

    @NonNull
    public ArrayList<Priority> getPriorities() {
        Set<Priority> res = new HashSet<>();
        for (Task item : mTasks) {
            res.add(item.getPriority());
        }
        ArrayList<Priority> ret = new ArrayList<>(res);
        Collections.sort(ret);
        return ret;
    }

    @NonNull
    public ArrayList<String> getContexts() {
        if (mLists != null) {
            return mLists;
        }
        Set<String> res = new HashSet<>();
        for (Task item : mTasks) {
            res.addAll(item.getLists());
        }
        mLists = new ArrayList<>();
        mLists.addAll(res);
        return mLists;
    }

    @NonNull
    public ArrayList<String> getProjects() {
        if (mTags != null) {
            return mTags;
        }
        Set<String> res = new HashSet<>();
        for (Task item : mTasks) {
            res.addAll(item.getTags());
        }
        mTags = new ArrayList<>();
        mTags.addAll(res);
        return mTags;
    }


    public ArrayList<String> getDecoratedContexts() {
        return Util.prefixItems("@", getContexts());
    }

    public ArrayList<String> getDecoratedProjects() {
        return Util.prefixItems("+", getProjects());
    }


    public void undoComplete(@NonNull final List<Task> tasks) {
        queueRunnable("Uncomplete", new Runnable() {
            @Override
            public void run() {
                for (Task t : tasks) {
                    t.markIncomplete();
                }
            }
        });
    }

    public void complete(@NonNull final Task task,
                         final boolean keepPrio) {

        queueRunnable("Complete", new Runnable() {
            @Override
            public void run() {

                Task extra = task.markComplete(DateTime.now(TimeZone.getDefault()));
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

    public void defer(@NonNull final String deferString, @NonNull final Task tasksToDefer, final int dateType) {
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

    @NonNull
    public List<Task> getSelectedTasks() {
        return mSelectedTask;
    }

    public void setSelectedTasks(List<Task> selectedTasks) {
        this.mSelectedTask = selectedTasks;
    }


    public void notifyChanged(final FileStoreInterface filestore, final String todoname, final String eol, final BackupInterface backup) {
        log.info("Handler: Queue notifychanged");
        todolistQueue.post(new Runnable() {
            @Override
            public void run() {
                log.info("Handler: Handle notifychanged");
                log.info("Saving todo list, size {}", mTasks.size());
                save(filestore, todoname, backup, eol);
                clearSelectedTasks();
                if (mTodoListChanged != null) {
                    log.info("TodoList changed, notifying listener and invalidating cached values");
                    mTags = null;
                    mLists = null;
                    mTodoListChanged.todoListChanged();
                } else {
                    log.info("TodoList changed, but nobody is listening");
                }
            }
        });
    }

    public List<Task> getTasks() {
        return mTasks;
    }

    public List<Task> getSortedTasksCopy(@NonNull ActiveFilter filter, @NonNull ArrayList<String> sorts, boolean caseSensitive) {
        List<Task> filteredTasks = filter.apply(mTasks);
        return Ordering.from(new MultiComparator(sorts, caseSensitive, filteredTasks)).sortedCopy(filteredTasks);
    }

    public void selectTask(Task t) {
        if (mSelectedTask.indexOf(t) == -1) {
            mSelectedTask.add(t);
        }
    }

    public void unSelectTask(Task t) {
        mSelectedTask.remove(t);
    }

    public void clearSelectedTasks() {
        mSelectedTask = new ArrayList<>();
    }

    public void selectTask(int index) {
        if (index < 0 || index > mTasks.size() - 1) {
            return;
        }
        selectTask(mTasks.get(index));
    }

    public void reload(final FileStoreInterface fileStore, final String filename, final BackupInterface backup, final LocalBroadcastManager lbm, final boolean background, final String eol) {
        if (TodoList.this.loadQueued()) {
            log.info("Todolist reload is already queued waiting");
            return;
        }
        lbm.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_START));
        loadQueued = true;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                clearSelectedTasks();
                try {
                    mTasks = fileStore.loadTasksFromFile(filename, backup, eol);
                } catch (IOException e) {
                    log.error("Todolist load failed: {}", filename, e);
                    Util.showToastShort(TodoApplication.getAppContext(), "Loading of todo file failed");
                }
                loadQueued = false;
                log.info("Todolist loaded, refresh UI");
                notifyChanged(fileStore,filename,eol,backup);
            }};
        if (background ) {
            log.info("Loading todolist asynchronously into {}", this);
            queueRunnable("Reload", r);

        } else {
            log.info("Loading todolist synchronously into {}", this);
            r.run();
        }
    }

    public void save(final FileStoreInterface filestore, final String todoFileName, final BackupInterface backup, final String eol) {
        queueRunnable("Save", new Runnable() {
            @Override
            public void run() {
                try {
                    filestore.saveTasksToFile(todoFileName, mTasks, backup, eol);
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.showToastLong(TodoApplication.getAppContext(), R.string.write_failed);
                }
            }
        });

    }

    public void archive(final FileStoreInterface filestore, final String todoFilename, final String doneFileName,  final List<Task> tasks, final String eol) {
        queueRunnable("Archive", new Runnable() {
            @Override
            public void run() {
                List<Task> tasksToArchive;
                if (tasks == null) {
                    tasksToArchive = mTasks;
                } else {
                    tasksToArchive = tasks;
                }
                List<Task> tasksToDelete = new ArrayList<>();
                for (Task t : tasksToArchive) {
                    if (t.isCompleted()) {
                        tasksToDelete.add(t);
                    }
                }
                try {
                    filestore.appendTaskToFile(doneFileName, tasksToDelete, eol);
                    for (Task t : tasksToDelete) {
                        mTasks.remove(t);
                    }
                    notifyChanged(filestore,todoFilename,eol,null);
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.showToastShort(TodoApplication.getAppContext(), "Task archiving failed");
                }
            }
        });
    }

    public interface TodoListChanged {
        void todoListChanged();
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
