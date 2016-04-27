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

import android.content.Context
import android.content.SharedPreferences

import nl.mpcjanssen.simpletask.*

import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Implementation of the task list backed by an internal file

 * @author Mark Janssen
 */
class TaskList(private val app: SimpletaskApplication) {

    private val TAG = "TaskList"
    private val log = Logger

    private var queue: MessageQueue
    private var storage = InternalStorage(app, "todo.txt", app.eol)

    private val taskItems = ArrayList<Task>()
    val selection = HashSet<Task>()

    init {
        queue = MessageQueue("TaskListQueue")
        queue.add("initial load", Runnable {
            taskItems.clear()
            taskItems.addAll(storage.load())
            listChanged()
        })
    }

    fun size(): Int {
        return taskItems.size
    }


    operator fun get(position: Int): Task? {
        return taskItems[position]
    }

    val priorities: ArrayList<Priority>
        get() {
            val res = HashSet<Priority>()
            taskItems.forEach {
                res.add(it.priority)
            }
            val ret = ArrayList(res)
            Collections.sort(ret)
            return ret
        }

    private var mLists: HashSet<String>? = null
    val contexts: List<String>
        get() {
            val lists = mLists
            if (lists != null) {
                return lists.toList()
            }
            val res = HashSet<String>()
            taskItems.forEach {
                res.addAll(it.lists)
            }
            mLists = res
            return res.toList()
        }

    private var mTags: HashSet<String>? = null
    val projects: List<String>
        get() {
            val tags = mTags
            if (tags != null) {
                return tags.toList()
            }
            val res = HashSet<String>()
            taskItems.forEach {
                res.addAll(it.tags)
            }
            mTags = res
            return res.toList()
        }


    val decoratedContexts: List<String>
        get() = prefixItems("@", contexts)

    val decoratedProjects: List<String>
        get() = prefixItems("+", projects)



    fun getSortedTasksCopy(filter: ActiveFilter): List<Task> {
        val items = taskItems
        log.info("TodoList", "Getting sorted tasks..")
        val filteredTasks = filter.apply(items)
        val sorts = filter.getSort(app.defaultSorts)
        val comp = MultiComparator(sorts, app.today, app.sortCaseSensitive(), filter.createIsThreshold)
        Collections.sort(filteredTasks, comp)
        log.info("TodoList", "Getting sorted tasks..done")
        return filteredTasks
    }

    fun archive(items: List<Task>?) {
        // Not implemented
        listChanged()
    }

    fun clearSelection() {
        queue.add("clear selection", Runnable {
            selection.clear()
            listChanged()
        })
    }

    fun selectTask(task : Task) {
        queue.add("select task", Runnable {
            selection.add(task)
            listChanged()
        })
    }

    fun unSelectTask(task : Task) {
        queue.add("unselect task", Runnable {
            selection.remove(task)
            listChanged()
        })
    }

    private fun listChanged() {
        // Clear cached values
        mLists = null
        mTags = null
        app.broadCastTodoListChanged()
    }

    fun update(updatedTasks: List<UpdatedTask>?, addedTasks: List<Task>? = null, removedTasks: List<Task>? = null, atEnd: Boolean = false) {
        // updates are by reference
        queue.add("update", Runnable {
            updatedTasks?.forEach {
                val idx = taskItems.indexOf(it.original)
                if (idx < 0) {
                    log.info(TAG, "Couldn't update task, original ${it.original} not found")
                } else {
                    taskItems[idx] = it.updated
                }
            }
            addedTasks?.let {
                if (atEnd) {
                    taskItems.addAll(addedTasks)
                } else {
                    taskItems.addAll(0, addedTasks)
                }
            }
            removedTasks?.let {
                taskItems.removeAll(removedTasks)
            }
            storage.save(taskItems)
            listChanged()
        })
    }

    companion object {
        // Some functions to make updating tasks easier

    }

    val tasks: List<Task>
    get() {
        val copy = ArrayList<Task>()
        copy.addAll(taskItems)
        return copy
    }


}

data class UpdatedTask(val original: Task, val updated: Task)

// Some helper functions to get a list of UpdatedTasks after perfoming some bulk operatio

fun List<Task>.completeTasks () : List<UpdatedTask> {
    val updatedTasks = ArrayList<UpdatedTask> ()
    this.forEach {
        updatedTasks.add(UpdatedTask(it, Task(it.text)))
    }
    return updatedTasks
}

fun List<Task>.undoComplete () : List<UpdatedTask> {
    val updatedTasks = ArrayList<UpdatedTask> ()
    this.forEach {
        updatedTasks.add(UpdatedTask(it, Task(it.text)))
    }
    return updatedTasks
}