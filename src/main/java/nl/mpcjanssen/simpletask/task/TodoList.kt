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
import nl.mpcjanssen.simpletask.remote.BackupInterface
import nl.mpcjanssen.simpletask.remote.FileStoreInterface
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Implementation of the in memory representation of the todo list

 * @author Mark Janssen
 */
object TodoList {
    private val log: Logger

    private var mLists: ArrayList<String>? = null
    private var mTags: ArrayList<String>? = null
    private val selection = HashSet<TodoListItem>()
    var todoItems: CopyOnWriteArrayList<TodoListItem>? = null

    init {

        log = Logger
    }


    fun firstLine(): Int {
        return todoItems?.map { it -> it.line }?.min() ?: 0
    }

    fun lastLine(): Int {
        return todoItems?.map { it -> it.line }?.max() ?: 1
    }

    fun add(t: TodoListItem, atEnd: Boolean) {
        ActionQueue.add("Add task", Runnable {
            log.debug(TAG, "Adding task of length {} into {} atEnd " + t.task.inFileFormat().length + " " + atEnd)
            if (atEnd) {
                t.line = lastLine() + 1
            } else {
                t.line = firstLine() - 1
            }
            todoItems?.add(t) ?: log.warn(TAG, "Adding while todolist was not loaded")
        })
    }

    fun add(t: Task, atEnd: Boolean, select: Boolean = false) {
        val newItem = TodoListItem(0, t)
        add(TodoListItem(0, t), atEnd)
        if (select) {
            selectTodoItem(newItem)
        }
    }


    fun remove(item: TodoListItem) {
        ActionQueue.add("Remove", Runnable {
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
        ActionQueue.add("Uncomplete", Runnable {
            items.forEach {
                it.task.markIncomplete()
            }
        })
    }

    fun complete(item: TodoListItem,
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


    fun prioritize(items: List<TodoListItem>, prio: Priority) {
        ActionQueue.add("Complete", Runnable {
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
            return selection.toList()
        }


    fun notifyChanged(fileStore: FileStoreInterface, todoName: String, eol: String, backup: BackupInterface?, save: Boolean) {
        log.info(TAG, "Handler: Queue notifychanged")
        ActionQueue.add("Notified changed", Runnable {
            if (save) {
                save(fileStore, todoName, backup, eol)
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

    fun getSortedTasks(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): List<TodoListItem> {
        val filteredTasks = filter.apply(todoItems)
        val comp = MultiComparator(sorts, TodoApplication.app.today, caseSensitive, filter.createIsThreshold)
        Collections.sort(filteredTasks, comp)
        return filteredTasks
    }

    fun reload(fileStore: FileStoreInterface, filename: String, backup: BackupInterface, lbm: LocalBroadcastManager, eol: String) {
        lbm.sendBroadcast(Intent(Constants.BROADCAST_SYNC_START))
        try {
            todoItems = CopyOnWriteArrayList<TodoListItem>(
                    fileStore.loadTasksFromFile(filename, backup, eol).mapIndexed { line, text ->
                        TodoListItem(line, Task(text))
                    })
        } catch (e: Exception) {
            e.printStackTrace()

        } catch (e: IOException) {
            log.error(TAG, "TodoList load failed: {}" + filename, e)
            showToastShort(TodoApplication.app, "Loading of todo file failed")
        }
        log.info(TAG, "TodoList loaded, refresh UI")
        notifyChanged(fileStore, filename, eol, backup, false)
    }


    private fun save(fileStore: FileStoreInterface, todoFileName: String, backup: BackupInterface?, eol: String) {
        val items = todoItems
        if (items == null) {
            log.error(TAG, "Trying to save null todo list")
            return
        }
        try {
            val lines = items.map {
                it.task.text
            }
            log.info(TAG, "Saving todo list, size ${lines.size}")
            fileStore.saveTasksToFile(todoFileName, lines, backup, eol)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun archive(fileStore: FileStoreInterface, todoFilename: String, doneFileName: String, tasks: List<TodoListItem>?, eol: String) {
        ActionQueue.add("Archive", Runnable {
            val items = todoItems
            if (items == null) {
                log.error(TAG, "Trying to archive null todo list")
                return@Runnable
            }
            val tasksToArchive = tasks ?: items

            val completedTasks = tasksToArchive.filter { it.task.isCompleted() }
            try {
                fileStore.appendTaskToFile(doneFileName, completedTasks.map { it.task.text }, eol)
                completedTasks.forEach {
                    todoItems?.remove(it)
                }

                notifyChanged(fileStore, todoFilename, eol, null, true)
            } catch (e: IOException) {
                e.printStackTrace()
                showToastShort(TodoApplication.app, "Task archiving failed")
            }
        })
    }

    fun isSelected(item: TodoListItem): Boolean {
        return selection.indexOf(item) > -1
    }

    fun numSelected(): Int {
        return selection.size
    }


    internal val TAG = TodoList::class.java.simpleName


    fun selectTasks(items: List<Task>, lbm: LocalBroadcastManager?) {
        if (todoItems == null) {
            var selection = items
            log.info(TAG, "todoItems is null, queuing selection of ${items.size} items")
            ActionQueue.add("Selection", Runnable {
                selectTasks(selection, lbm)
                log.info(TAG, "Queued selection update, lbm = $lbm")
                lbm?.sendBroadcast(Intent(Constants.BROADCAST_HIGHLIGHT_SELECTION))
            })
            return
        } else {
            todoItems?.forEach {
                if (it.task in items) {
                    selection.add(it)
                }
            }

        }
    }

    fun selectTodoItem(item: TodoListItem) {
        selectTodoItems(listOf(item))
    }

    fun selectTodoItems(items: List<TodoListItem>) {
        selection.addAll(items)
    }

    fun unSelectTodoItem(item: TodoListItem) {
        unSelectTodoItems(listOf(item))
    }

    fun unSelectTodoItems(items: List<TodoListItem>) {
        selection.removeAll(items)

    }

    fun clearSelection() {
        selection.clear()
    }

    fun getTaskCount(): Long {
        val items = todoItems ?: return 0
        return items.filter { it.task.inFileFormat().isNotBlank() }.size.toLong()
    }


}

data class TodoListItem(var line: Int, var task: Task)
