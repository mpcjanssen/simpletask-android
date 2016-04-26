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

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.dao.todo.ArchiveItemDao
import nl.mpcjanssen.simpletask.dao.todo.DaoMaster
import nl.mpcjanssen.simpletask.dao.todo.DaoSession
import nl.mpcjanssen.simpletask.dao.todo.TodoItemDao
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*



/**
 * Implementation of the database backed representation of the todo list

 * @author Mark Janssen
 */
class TodoList(private val app: SimpletaskApplication) {

    private val TAG = "TodoList"
    private val log = Logger

    private var todolistQueue: Handler? = null
    private var loadQueued = false

    private val daoSession: DaoSession?

    private val todoDao: TodoItemDao?
    private val archiveDao: ArchiveItemDao?

    init {
        val helper = DaoMaster.DevOpenHelper(app, "Todo_v1.db", null)
        val todoDb = helper.writableDatabase
        val daoMaster = DaoMaster(todoDb)
        daoSession = daoMaster.newSession()
        todoDao = daoSession.todoItemDao
        archiveDao = daoSession.archiveItemDao
    }


    fun loadQueued(): Boolean {
        return loadQueued
    }

    fun firstLine(): Int {
        val items = todoItems ?: CopyOnWriteArrayList<TodoListItem>()
        if (items.size > 0) {
            return items[0].line
        } else {
            return -1
        }
    }

    fun lastLine(): Int {
        val items = todoItems ?: CopyOnWriteArrayList<TodoListItem>()
        if (items.size > 0) {
            return items[0].line
        } else {
            return 1
        }
    }

    fun add(t: TodoListItem, atEnd: Boolean) {
        queueRunnable("Add task", Runnable {
            log.debug(TAG, "Adding task of length {} into {} atEnd " + t.task.inFileFormat().length + " " + atEnd)
            if (atEnd) {
                t.line = lastLine() + 1
            } else {
                t.line = firstLine() - 1
            }
            todoItems?.add(t) ?: log.warn(TAG, "Adding while todolist was not loaded")
        })
    }

    fun add(t: Task, atEnd: Boolean) {
        add(TodoListItem(0,t,false),atEnd)
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



    fun getSortedTasksCopy(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): List<TodoListItem> {
        val filteredTasks = filter.apply(todoItems)
        val comp = MultiComparator(sorts, app.today, caseSensitive,filter.createIsThreshold)
        Collections.sort(filteredTasks, comp)
        return filteredTasks
    }

    fun reload(fileStore: FileStoreInterface, filename: String, backup: BackupInterface, lbm: LocalBroadcastManager, background: Boolean, eol: String) {
        if (this@TodoList.loadQueued()) {
            log.info(TAG, "TodoList reload is already queued waiting")
            return
        }
        lbm.sendBroadcast(Intent(Constants.BROADCAST_SYNC_START))
        loadQueued = true
        val r = Runnable {
            try {
                todoItems = CopyOnWriteArrayList<TodoListItem>(
                fileStore.loadTasksFromFile(filename, backup, eol).mapIndexed { line, text ->
                    TodoListItem(line,Task(text))
                })
            } catch (e: Exception) {
                e.printStackTrace()

            } catch (e: IOException) {
                log.error(TAG, "TodoList load failed: {}" + filename, e)
                showToastShort(app, "Loading of todo file failed")
            }

            loadQueued = false
            log.info(TAG, "TodoList loaded, refresh UI")
            notifyChanged(fileStore, filename, eol, backup, false)
        }
        if (background) {
            log.info(TAG, "Loading TodoList asynchronously")
            queueRunnable("Reload", r)

        } else {
            log.info(TAG, "Loading TodoList synchronously")
            r.run()
        }
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

    fun archive(todoFilename: String, doneFileName: String, tasks: List<TodoListItem>?, eol: String) {
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
