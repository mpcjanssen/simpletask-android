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

import android.app.Activity
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.dao.Daos
import nl.mpcjanssen.simpletask.dao.gentodo.TodoItem
import nl.mpcjanssen.simpletask.dao.gentodo.TodoItemDao
import nl.mpcjanssen.simpletask.remote.BackupInterface
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.remote.FileStoreInterface
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*
import java.io.File
import java.io.IOException
import java.util.*


/**
 * Implementation of the in memory representation of the todo list

 * @author Mark Janssen
 */
object TodoList {
    private val log: Logger

    private var mLists: ArrayList<String>? = null
    private var mTags: ArrayList<String>? = null
    var todoItemsDao = Daos.todoItemDao

    init {

        log = Logger
    }


    val todoItems: List<TodoItem>
    get() = todoItemsDao.loadAll()

    fun firstLine(): Long {
        return todoItems?.map { it -> it.line }?.min() ?: 0
    }

    fun lastLine(): Long {
        return todoItems?.map { it -> it.line }?.max() ?: 1
    }

    fun add(t: TodoItem, atEnd: Boolean) {
        ActionQueue.add("Add task", Runnable {
            log.debug(TAG, "Adding task of length {} into {} atEnd " + t.task.inFileFormat().length + " " + atEnd)
            if (atEnd) {
                t.line = lastLine() + 1
            } else {
                t.line = firstLine() - 1
            }
            todoItemsDao.insert(t)
        })
    }

    fun add(t: Task, atEnd: Boolean, select: Boolean = false) {
        val newItem = TodoItem(0, t, select)
        add(newItem, atEnd)
    }


    fun remove(item: TodoItem) {
        ActionQueue.add("Remove", Runnable {
            todoItemsDao.delete(item)
        })
    }


    fun size(): Int {
        return todoItems?.size ?: 0
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


    fun undoComplete(items: List<TodoItem>) {
        ActionQueue.add("Uncomplete", Runnable {
            items.forEach {
                it.task.markIncomplete()
            }
            todoItemsDao.updateInTx(items)
        })
    }

    fun complete(item: TodoItem,
                 keepPrio: Boolean,
                 extraAtEnd: Boolean) {

        ActionQueue.add("Complete", Runnable {
            val task = item.task
            val extra = task.markComplete(todayAsString)
            if (extra != null) {
                add(extra, extraAtEnd)
            }
            if (!keepPrio) {
                task.priority = Priority.NONE
            }
        })
    }


    fun prioritize(items: List<TodoItem>, prio: Priority) {
        ActionQueue.add("Complete", Runnable {
            for (item in items) {
                item.task.priority = prio
            }
            todoItemsDao.updateInTx(items)
        })

    }

    fun defer(deferString: String, items: List<TodoItem>, dateType: DateType) {
        ActionQueue.add("Defer", Runnable {
            items.forEach {
                val taskToDefer = it.task
                when (dateType) {
                    DateType.DUE -> taskToDefer.deferDueDate(deferString, todayAsString)
                    DateType.THRESHOLD -> taskToDefer.deferThresholdDate(deferString, todayAsString)
                }
            }
            todoItemsDao.updateInTx(items)
        })
    }

    val selectionQuery = todoItemsDao.queryBuilder().where(TodoItemDao.Properties.Selected.eq(true)).build()
    var selectedTasks: List<TodoItem> = ArrayList()
        get() {
            return selectionQuery.list()
        }


    fun notifyChanged(todoName: String, eol: String, backup: BackupInterface?, save: Boolean) {
        log.info(TAG, "Handler: Queue notifychanged")
        ActionQueue.add("Notified changed", Runnable {
            if (save) {
                save(FileStore, todoName, backup, eol)
            }
            mLists = null
            mTags = null
            broadcastRefreshUI(TodoApplication.app.localBroadCastManager)
        })
    }

    fun startAddTaskActivity(act: Activity) {
        ActionQueue.add("Add/Edit tasks", Runnable {
            log.info(TAG, "Starting addTask activity")
            val intent = Intent(act, AddTask::class.java)
            act.startActivity(intent)
        })
    }

    fun getSortedTasks(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): List<TodoItem> {
        val filteredTasks = filter.apply(todoItems)
        val comp = MultiComparator(sorts, TodoApplication.app.today, caseSensitive, filter.createIsThreshold)
        Collections.sort(filteredTasks, comp)
        return filteredTasks
    }

    fun reload(backup: BackupInterface, lbm: LocalBroadcastManager, eol: String) {
        lbm.sendBroadcast(Intent(Constants.BROADCAST_SYNC_START))
        val filename = Config.todoFileName
        if (FileStore.needsRefesh(Config.currentVersionId)) {
            try {
                todoItemsDao.database.beginTransaction()
                todoItemsDao.deleteAll()
                val items = ArrayList<TodoItem>(
                        FileStore.loadTasksFromFile(filename, backup, eol).mapIndexed { line, text ->
                            TodoItem(line.toLong(), Task(text), false)
                        })
                todoItemsDao.insertInTx(items)
                todoItemsDao.database.setTransactionSuccessful();
                Config.currentVersionId = FileStore.getVersion(filename)

            } catch (e: Exception) {
                e.printStackTrace()

            } catch (e: IOException) {
                log.error(TAG, "TodoList load failed: {}" + filename, e)
                showToastShort(TodoApplication.app, "Loading of todo file failed")
            } finally {
                todoItemsDao.database.endTransaction()
            }
            log.info(TAG, "TodoList loaded, refresh UI")
        } else {
            log.info(TAG, "Todolist reload not needed, refresh UI")
        }
        notifyChanged(filename, eol, backup, false)
    }


    private fun save(fileStore: FileStoreInterface, todoFileName: String, backup: BackupInterface?, eol: String) {
        val items = todoItems
        try {
            val lines = items.sortedBy {it.line}.map {
                it.task.inFileFormat()
            }

            log.info(TAG, "Saving todo list, size ${lines.size}")
            fileStore.saveTasksToFile(todoFileName, lines, backup, eol = eol, updateVersion = true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun archive(fileStore: FileStoreInterface, todoFilename: String, doneFileName: String, tasks: List<TodoItem>?, eol: String) {
        ActionQueue.add("Archive", Runnable {
            val items = todoItems
            if (items == null) {
                log.error(TAG, "Trying to archive null todo list")
                return@Runnable
            }
            val tasksToArchive = tasks ?: items

            val completedTasks = tasksToArchive.filter { it.task.isCompleted() }
            try {
                FileStore.appendTaskToFile(doneFileName, completedTasks.map { it.task.text }, eol)
                todoItemsDao.deleteInTx(items)
                notifyChanged(todoFilename, eol, null, true)
            } catch (e: IOException) {
                e.printStackTrace()
                showToastShort(TodoApplication.app, "Task archiving failed")
            }
        })
    }

    fun isSelected(item: TodoItem): Boolean {
        return item.selected
    }

    fun numSelected(): Long {
        return todoItemsDao.queryBuilder().where(TodoItemDao.Properties.Selected.eq(true)).count()
    }


    internal val TAG = TodoList::class.java.simpleName


    fun selectTodoItems(items: List<TodoItem>) {
        items.forEach {
            it.selected = true
        }
        todoItemsDao.updateInTx(items)
    }

    fun selectTodoItem(item: TodoItem) {
        selectTodoItems(listOf(item))
    }


    fun unSelectTodoItem(item: TodoItem) {
        unSelectTodoItems(listOf(item))
    }

    fun unSelectTodoItems(items: List<TodoItem>) {
        items.forEach {
            it.selected = false
        }
        log.info(TAG, "Unselecting done")
    }

    fun clearSelection() {
        unSelectTodoItems(selectedTasks)
    }

    fun getTaskCount(): Long {
        val items = todoItems ?: return 0
        return items.filter { it.task.inFileFormat().isNotBlank() }.size.toLong()
    }

    fun selectLine(line : Long ) {
        val item = todoItemsDao.load(line)
        item.selected = true
        todoItemsDao.update(item)
    }


}

