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
 * Implementation of the database backed representation of the todo list

 * @author Mark Janssen
 */
class TodoList(private val app: SimpletaskApplication) {

    private val TAG = "TodoList"
    private val log = Logger

    private var todolistQueue: MessageQueue
    private var todolistStorage = TodoStorage(app, "todo.txt", app.eol)

    val todoItems = CopyOnWriteArrayList<TodoItem>()

    init {
        todolistQueue = MessageQueue("TodoListQueue")
        todolistQueue.queueRunnable("initial load", Runnable {
            todoItems.clear()
            todoItems.addAll(todolistStorage.load())
            listChanged()
        })
    }

    fun add(t: List<TodoItem>, atEnd: Boolean) {
        todolistQueue.queueRunnable("add", Runnable {
            if (atEnd) {
                todoItems.addAll(t)
            } else {
                todoItems.addAll(0, t)
            }
            listChanged()
        })
    }

    fun remove(t: List<TodoItem>) {
        if (t.size > 0) {
            todoItems.removeAll(t)
            listChanged()
        }
    }


    fun size(): Int {
        return todoItems.size
    }


    operator fun get(position: Int): TodoItem? {
        return todoItems[position]
    }

    private val mPriorities = ArrayList<Priority>()
    val priorities: ArrayList<Priority>
        get() {
            val res = HashSet<Priority>()
            todoItems.forEach {
                res.add(it.task.priority)
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
            todoItems.forEach {
                res.addAll(it.task.lists)
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
            todoItems.forEach {
                res.addAll(it.task.tags)
            }
            mTags = res
            return res.toList()
        }


    val decoratedContexts: List<String>
        get() = prefixItems("@", contexts)

    val decoratedProjects: List<String>
        get() = prefixItems("+", projects)


    fun undoComplete(items: List<TodoItem>) {
        items.forEach {
            val t = it.task
            t.markIncomplete()
        }
        listChanged()
    }

    fun complete(items: List<TodoItem>,
                 keepPriority: Boolean,
                 extraAtEnd: Boolean) {
        val extraItems = ArrayList<TodoItem>()
        items.forEach {
            val t = it.task
            val extra = t.markComplete(todayAsString)
            if (extra != null) {
                extraItems.add(extra.asTodoItem())
            }
        }
        add(extraItems, extraAtEnd)
    }


    fun prioritize(items: List<TodoItem>, priority: Priority) {
        items.forEach {
            val t = it.task
            t.priority = priority
        }
        listChanged()
    }

    fun defer(deferString: String, items: List<TodoItem>, dateType: DateType) {
        items.forEach {
            val t = it.task
            when (dateType) {
                DateType.DUE -> t.deferDueDate(deferString, todayAsString)
                DateType.THRESHOLD -> t.deferThresholdDate(deferString, todayAsString)
            }
        }
        listChanged()
    }


    var selectedTasks: List<TodoItem> = ArrayList()
        get() {
            return todoItems.filter { it.selected }
        }


    fun getSortedTasksCopy(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): List<TodoItem> {
        val items = todoItems
        log.info("TodoList", "Getting sorted tasks..")
        val filteredTasks = filter.apply(items)
        val comp = MultiComparator(sorts, app.today, caseSensitive, filter.createIsThreshold)
        Collections.sort(filteredTasks, comp)
        log.info("TodoList", "Getting sorted tasks..done")
        return filteredTasks
    }

    fun archive(items: List<TodoItem>?) {
        // Not implemented
        listChanged()
    }

    fun clearSelection() {
        todoItems.forEach {
            it.selected = false
        }
        listChanged()
    }

    fun save() {
        todolistQueue.queueRunnable("save", Runnable {
            todolistStorage.save(todoItems)
        })
    }

    private fun listChanged() {
        // Clear cached values
        mLists = null
        mTags = null
        app.broadCastTodoListChanged()
    }

    fun update(updatedTasks: List<TodoItem>) {
        // updates are by reference
        listChanged()
    }

    fun appendRaw(lines: List<String>) {
        todolistStorage.addLines(lines, true)
        listChanged()
    }
}

data class TodoItem(var task: Task, var selected: Boolean = false)

class TodoStorage(val ctx: Context, val filename: String, val eol: String) {

    val prefs : SharedPreferences
    init {
        prefs = ctx.getSharedPreferences("storage",Context.MODE_PRIVATE)
    }
    fun save(todoItems: List<TodoItem>, append: Boolean = false) {
        addLines(todoItems.map {it.task.text}, append)
    }


    fun load(): List<TodoItem> {
        log.info("TodoStorage", "Loading tasks from storage..")
        val inputStream = ctx.openFileInput(filename)
        val reader = inputStream.bufferedReader()
        val result = ArrayList<TodoItem>()
        reader.forEachLine {
            result.add(TodoItem(Task(it)))
        }
        reader.close()
        log.info("TodoStorage", "Loading tasks from storage..done")
        return result
    }

    fun addLines(lines: List<String>, append : Boolean) {
        log.info("TodoStorage", "Saving to internal file ${filename}, append: $append...")
        val mode = if (append) Context.MODE_APPEND else Context.MODE_PRIVATE
        val outputStream = ctx.openFileOutput(filename,mode)
        val writer = outputStream.bufferedWriter()
        val contentToSave = lines.joinToString(eol)
        if (append) {
            writer.write(eol)
        }
        writer.write(contentToSave)
        outputStream.close()
        log.info("TodoStorage", "Saving to internal file ${filename}, append: $append..done.")

    }
}