/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 *
 * LICENSE:
 *
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task

import android.app.IntentService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.dao.gen.TodoItem
import nl.mpcjanssen.simpletask.dao.gen.TodoItemDao
import nl.mpcjanssen.simpletask.remote.BackupInterface
import nl.mpcjanssen.simpletask.remote.FileStoreInterface
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*

import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Implementation of the todolist. It's backed by a database. Modification and reading is handled in a queue

 * @author Mark Janssen
 */







class TodoList(private val dao: TodoItemDao, private val mTodoListChanged: TodoList.TodoListChanged?) {
    private val log = Logger

    private var mLists: ArrayList<String>? = null
    private var mTags: ArrayList<String>? = null
    private val todolistQueue: Handler

    init {
        // Set up the message queue
        val t = LooperThread()
        t.start()
        todolistQueue = t.mHandler
    }

    // queue Runnables for sequential processsing
    fun queueRunnable(description: String, r: Runnable) {
        log.info(TAG, "Handler: Queue " + description)
        todolistQueue.post(LoggingRunnable(description, r))
    }
    

    fun firstLine(): Int {
        val num = dao.count()
        if (num > 0) {
            return dao.queryBuilder().orderAsc(TodoItemDao.Properties.Line).limit(1).build().list().first().line-1
        } else {
            return -1
        }
    }

    fun lastLine(): Int {
        val num = dao.count()
        if (num > 0) {
            return dao.queryBuilder().orderDesc(TodoItemDao.Properties.Line).limit(1).build().list().first().line+1
        } else {
            return 1
        }
    }

    fun add(t: TodoItem, atEnd: Boolean) {
        queueRunnable("Add task", Runnable {
            t = Task()
            log.debug(TAG, "Adding task atEnd #tokens: " + ${Task(). + " " + atEnd)
            if (atEnd) {
                t.line = lastLine() + 1
            } else {
                t.line = firstLine() - 1
            }
            dao.insert(t)
        })
    }

    fun add(t: Task, atEnd: Boolean) {
        add(TodoItem(0,t.toJSON().toString(),false),atEnd)
    }


    fun remove(item: TodoListItem) {
        queueRunnable("Remove", Runnable {
            todoItems?.remove(item) ?: log.warn(TAG, "Tried to remove an item from a null todo list")
         })
    }


    fun size(): Int {
        return todoItems?.size ?: 0
    }


    operator fun get(position: Int): TodoListItem? {
        return todoItems?.getOrNull(position)
    }

    val priorities: ArrayList<Priority>
        get() {
            val res = HashSet<Priority>()
            todoItems?.forEach {
                res.add(it.task.priority)
            }
            val ret = ArrayList(res)
            Collections.sort(ret)
            return ret
        }

    val contexts: ArrayList<String>
        get() {
            val lists = mLists
            if (lists != null) {
                return lists
            }
            val res = HashSet<String>()
            todoItems?.forEach {
                res.addAll(it.task.lists)
            }
            val newLists = ArrayList<String>()
            newLists.addAll(res)
            mLists = newLists
            return newLists
        }

    val projects: ArrayList<String>
        get() {
            val tags = mTags
            if (tags != null) {
                return tags
            }
            val res = HashSet<String>()
            todoItems?.forEach {
            res.addAll(it.task.tags)
            }
            val newTags = ArrayList<String>()
            newTags.addAll(res)
            mTags = newTags
            return newTags
        }


    val decoratedContexts: ArrayList<String>
        get() = prefixItems("@", contexts)

    val decoratedProjects: ArrayList<String>
        get() = prefixItems("+", projects)


    fun undoComplete(items: List<TodoListItem>) {
        queueRunnable("Uncomplete", Runnable {
            items.forEach {
                it.task.markIncomplete()
            }
        })
    }

    fun complete(item: TodoListItem,
                 keepPrio: Boolean,
                 extraAtEnd: Boolean) {

        queueRunnable("Complete", Runnable {
            val task  = item.task
            val extra = task.markComplete(todayAsString)
            if (extra != null) {
                add(extra, extraAtEnd)
            }
            if (!keepPrio) {
                task.priority = Priority.NONE
            }
        })
    }


    fun prioritize(items: List<TodoListItem>, prio: Priority) {
        queueRunnable("Complete", Runnable {
            for (item in items) {
                val task = item.task
                task.priority = prio
            }
        })
    }

    fun defer(deferString: String, items: List<TodoListItem>, dateType: DateType) {
        items.forEach {
            val taskToDefer = it.task
            when (dateType) {
                DateType.DUE -> taskToDefer.deferDueDate(deferString, todayAsString)
                DateType.THRESHOLD -> taskToDefer.deferThresholdDate(deferString, todayAsString)
            }
        }
    }

    var selectedTasks: List<TodoListItem> = ArrayList()
        get() {
            return todoItems?.filter { it.selected } ?: emptyList()
        }


    fun notifyChanged(fileStore: FileStoreInterface, todoName: String, eol: String, backup: BackupInterface?, save: Boolean) {
        log.info(TAG, "Handler: Queue notifychanged")
        todolistQueue!!.post {
            if (save) {
                log.info(TAG, "Handler: Handle notifyChanged")
                log.info(TAG, "Saving todo list, size ${size()}")
                save(fileStore, todoName, backup, eol)
            }
            if (mTodoListChanged != null) {
                log.info(TAG, "TodoList changed, notifying listener and invalidating cached values")
                mTags = null
                mLists = null
                mTodoListChanged.todoListChanged()
            } else {
                log.info(TAG, "TodoList changed, but nobody is listening")
            }
        }
    }



    fun getSortedTasksCopy(filter: ActiveFilter, today: String, sorts: ArrayList<String>, caseSensitive: Boolean): List<TodoListItem> {
        val filteredTasks = filter.apply(todoItems)
        val comp = MultiComparator(sorts, today, caseSensitive,filter.createIsThreshold)
        Collections.sort(filteredTasks, comp)
        return filteredTasks
    }




    fun save(fileStore: FileStoreInterface, todoFileName: String, backup: BackupInterface?, eol: String) {
        queueRunnable("Save", Runnable {
            val items = todoItems
            if (items==null) {
                log.error(TAG, "Trying to save null todo list")
                return@Runnable
            }
            try {
                val lines = items.map {
                    it.task.text
                }
                fileStore.saveTasksToFile(todoFileName, lines, backup, eol)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        })

    }

    fun archive(fileStore: FileStoreInterface, todoFilename: String, doneFileName: String, tasks: List<TodoListItem>?, eol: String) {
        queueRunnable("Archive", Runnable {
            val items = todoItems
            if (items==null) {
                log.error(TAG, "Trying to archive null todo list")
                return@Runnable
            }
            val tasksToArchive = tasks ?: items

            val completedTasks = tasksToArchive.filter { it.task.isCompleted() };
            try {
                fileStore.appendTaskToFile(doneFileName, completedTasks.map {it.task.text}, eol);
                completedTasks.forEach {
                    todoItems?.remove(it)
                }

                notifyChanged(fileStore, todoFilename, eol, null, true);
            } catch (e : IOException) {
                e.printStackTrace();
                showToastShort(app, "Task archiving failed");
            }
        })
    }

    interface TodoListChanged {
        fun todoListChanged()
    }

    inner class LoggingRunnable internal constructor(private val description: String, private val runnable: Runnable) : Runnable {

        init {
            log.info(TAG, "Creating action " + description)
        }

        override fun toString(): String {
            return description
        }

        override fun run() {
            log.info(TAG, "Execution action " + description)
            runnable.run()
        }

    }

    companion object {
        internal val TAG = TodoList::class.java.simpleName
    }

    fun selectTasks(items: List<Task>, lbm: LocalBroadcastManager?) {
        if (todoItems == null) {
            var selection = items
            log.info(TAG, "todoItems is null, queuing selection of ${items.size} items")
            queueRunnable("Selection", Runnable {
                selectTasks(selection,lbm)
                log.info(TAG, "Queued selection update, lbm = $lbm")
                lbm?.sendBroadcast(Intent(Constants.BROADCAST_HIGHLIGHT_SELECTION))
            })
            return
        } else {
            todoItems?.forEach {
                if (it.task in items) {
                    it.selected = true
                }
            }

        }
    }

    fun selectTodoItem(item: TodoListItem) {
        selectTodoItems(listOf(item))
    }

    fun selectTodoItems(items: List<TodoListItem>) {
            items.forEach {
                it.selected = true
            }
    }

    fun unSelectTodoItem(item: TodoListItem) {
        unSelectTodoItems(listOf(item))
    }

    fun unSelectTodoItems(items: List<TodoListItem>) {
        items.forEach {
            it.selected = false
        }
    }

    fun clearSelection() {
       todoItems?.forEach {
           it.selected = false
       }
    }
}

class LooperThread() : Thread() {
    lateinit var  mHandler : Handler
    override fun run() {
        Looper.prepare();
        mHandler = Handler()
        Looper.loop();
    }
}