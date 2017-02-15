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
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.remote.FileStoreInterface
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*
import java.io.IOException
import java.util.*


/**
 * Implementation of the in memory representation of the Todo list

 * @author Mark Janssen
 */
object TodoList {
    private val log: Logger = Logger

    private var mLists: ArrayList<String>? = null
    private var mTags: ArrayList<String>? = null
    val todoItems: ArrayList<Task>
        get() {
            val current = Config.todoList
            if (current != null) {
                return current
            } else {
                TodoApplication.app.loadTodoList()
                return ArrayList<Task>()
            }
        }
    val selectedItems = HashSet<Task>()
    val pendingEdits = LinkedHashSet<Task>()

    fun hasPendingAction(): Boolean {
        return ActionQueue.hasPending()
    }

    // Wait until there are no more pending actions
    @Suppress("unused") // Used in test suite
    fun settle() {
        while (hasPendingAction()) {
            Thread.sleep(10)
        }
    }

    fun add(items: List<Task>, atEnd: Boolean) {
        ActionQueue.add("Add task ${items.size} atEnd: $atEnd", Runnable {
            if (atEnd) {
                todoItems.addAll(items)
            } else {
                todoItems.addAll(0, items)
            }
            updateCache()
        })
    }

    fun add(t: Task, atEnd: Boolean) {
        add(listOf(t), atEnd)
    }


    fun remove(t: Task) {
        ActionQueue.add("Remove", Runnable {
            todoItems.remove(t)
            selectedItems.remove(t)
            pendingEdits.remove(t)
            updateCache()
        })
    }


    fun size(): Int {
        return todoItems.size
    }


    val priorities: ArrayList<Priority>
        get() {
            val res = HashSet<Priority>()
            todoItems.forEach {
                res.add(it.priority)
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
            todoItems.forEach {
                res.addAll(it.lists)
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
            todoItems.forEach {
                res.addAll(it.tags)
            }
            val newTags = ArrayList<String>()
            newTags.addAll(res)
            mTags = newTags
            return newTags
        }


    val decoratedContexts: List<String>
        get() = contexts.map { "@" + it }

    val decoratedProjects: List<String>
        get() = projects.map { "+" + it }


    fun uncomplete(items: List<Task>) {
        ActionQueue.add("Uncomplete", Runnable {
            items.forEach {
                it.markIncomplete()
            }
            updateCache()
        })
    }

    fun complete(tasks: List<Task>, keepPrio: Boolean, extraAtEnd: Boolean) {
        ActionQueue.add("Complete", Runnable {
            for (task in tasks) {
                val extra = task.markComplete(todayAsString)
                if (extra != null) {
                    if (extraAtEnd) {
                        todoItems.add(extra)
                    } else {
                        todoItems.add(0, extra)
                    }
                }
                if (!keepPrio) {
                    task.priority = Priority.NONE
                }
            }
            updateCache()
        })
    }


    fun prioritize(tasks: List<Task>, prio: Priority) {
        ActionQueue.add("Complete", Runnable {
            tasks.map { it.priority = prio }
            updateCache()
        })

    }

    fun defer(deferString: String, tasks: List<Task>, dateType: DateType) {
        ActionQueue.add("Defer", Runnable {
            tasks.forEach {
                when (dateType) {
                    DateType.DUE -> it.deferDueDate(deferString, todayAsString)
                    DateType.THRESHOLD -> it.deferThresholdDate(deferString, todayAsString)
                }
            }
            updateCache()
        })
    }

    var selectedTasks: List<Task> = ArrayList()
        get() {
            return selectedItems.toList()
        }

    var completedTasks: List<Task> = ArrayList()
        get() {
            return todoItems.filter { it.isCompleted() }
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
        ActionQueue.add("Start add/edit task activity", Runnable {
            log.info(TAG, "Starting addTask activity")
            val intent = Intent(act, AddTask::class.java)
            act.startActivity(intent)
        })
    }

    fun getSortedTasks(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): List<Task> {
        val filteredTasks = filter.apply(todoItems)
        val comp = MultiComparator(sorts, TodoApplication.app.today, caseSensitive, filter.createIsThreshold)
        if (!comp.fileOrder) {
            filteredTasks.reverse()
        }
        comp.comparator?.let {
            Collections.sort(filteredTasks, it)
        }
        return filteredTasks
    }

    fun reload(backup: BackupInterface, lbm: LocalBroadcastManager, eol: String) {
        lbm.sendBroadcast(Intent(Constants.BROADCAST_SYNC_START))
        val filename = Config.todoFileName
        if (FileStore.needsRefresh(Config.currentVersionId)) {
            try {
                todoItems.clear()
                val items = ArrayList<Task>(
                        FileStore.loadTasksFromFile(filename, backup, eol).map { text ->
                            Task(text)
                        })
                todoItems.addAll(items)
                clearSelection()
                Config.currentVersionId = FileStore.getVersion(filename)

            } catch (e: Exception) {
                e.printStackTrace()

            } catch (e: IOException) {
                log.error(TAG, "TodoList load failed: {}" + filename, e)
                showToastShort(TodoApplication.app, "Loading of todo file failed")
            }
            log.info(TAG, "TodoList loaded, refresh UI")
        } else {
            log.info(TAG, "Todolist reload not needed, refresh UI")
        }
        updateCache()
        notifyChanged(filename, eol, backup, false)
    }


    private fun save(fileStore: FileStoreInterface, todoFileName: String, backup: BackupInterface?, eol: String) {
        try {
            val lines = todoItems.map {
                it.inFileFormat()
            }

            log.info(TAG, "Saving todo list, size ${lines.size}")
            fileStore.saveTasksToFile(todoFileName, lines, backup, eol = eol, updateVersion = true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun archive(todoFilename: String, doneFileName: String, tasks: List<Task>, eol: String) {
        ActionQueue.add("Archive", Runnable {
            try {
                FileStore.appendTaskToFile(doneFileName, tasks.map { it.text }, eol)
                tasks.forEach {
                    todoItems.remove(it)
                }
                updateCache()
                notifyChanged(todoFilename, eol, null, true)
            } catch (e: IOException) {
                e.printStackTrace()
                showToastShort(TodoApplication.app, "Task archiving failed")
            }
        })
    }

    fun isSelected(item: Task): Boolean {
        return selectedItems.indexOf(item) > -1
    }

    fun numSelected(): Int {
        return selectedItems.size
    }


    internal val TAG = TodoList::class.java.simpleName


    fun selectTasks(items: List<Task>) {
        ActionQueue.add("Select", Runnable {
            selectedItems.addAll(items)
            broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
        })
    }

    fun selectTask(item: Task?) {
        item?.let {
            selectTasks(listOf(item))
        }
    }


    fun unSelectTask(item: Task) {
        unSelectTasks(listOf(item))
    }

    fun unSelectTasks(items: List<Task>) {
        ActionQueue.add("Unselect", Runnable {
            selectedItems.removeAll(items)
            broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
        })
    }

    fun clearSelection() {
        ActionQueue.add("Clear selection", r = Runnable {
            selectedItems.clear()
            broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
        })
    }

    fun getTaskCount(): Long {
        val items = todoItems
        return items.filter { it.inFileFormat().isNotBlank() }.size.toLong()
    }

    fun updateCache() {
        Config.todoList = todoItems
    }

    fun editTasks(from: Activity, tasks: List<Task>) {
        ActionQueue.add("Clear selection", Runnable {
            pendingEdits.addAll(tasks)
            startAddTaskActivity(from)
        })
    }

    fun clearPendingEdits() {
        ActionQueue.add("Clear selection", Runnable {
            pendingEdits.clear()
        })
    }
}

