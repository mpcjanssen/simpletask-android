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
import nl.mpcjanssen.simpletask.dao.gen.TodoListItem
import nl.mpcjanssen.simpletask.dao.gen.TodoListItemDao
import nl.mpcjanssen.simpletask.dao.gen.TodoListItemDao.Properties
import nl.mpcjanssen.simpletask.remote.BackupInterface
import nl.mpcjanssen.simpletask.remote.FileStoreInterface
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*

import java.io.IOException
import java.util.*


/**
 * Implementation of the in memory representation of the todo list

 * @author Mark Janssen
 */
class TodoList(private val app: TodoApplication, private val mTodoListChanged: TodoList.TodoListChanged?) {
    private val log: Logger

    private var mLists: ArrayList<String>? = null
    private var mTags: ArrayList<String>? = null

    private var todolistQueue: Handler? = null
    private var loadQueued = false

    private var dao: TodoListItemDao

    init {
        dao = app.todoListItemDao;
        // Set up the message queue
        val t = Thread(Runnable {
            Looper.prepare()
            todolistQueue = Handler()
            Looper.loop()
        })
        t.start()

        log = Logger


    }


    fun queueRunnable(description: String, r: Runnable) {
        log.info(TAG, "Handler: Queue " + description)
        while (todolistQueue == null) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
        if (todolistQueue != null) {
            todolistQueue!!.post(LoggingRunnable(description, r))
        } else {
            r.run()
        }
    }

    fun loadQueued(): Boolean {
        return loadQueued
    }

    fun firstLine(): Long {
        val items =  dao.queryBuilder().orderAsc(Properties.Line).listLazy()
        if (items.size > 0) {
            return items[0].line
        } else {
            return -1
        }
    }

    fun lastLine(): Long {
        val items = dao.queryBuilder().orderDesc(Properties.Line).listLazy()
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
            dao.insert(t)
        })
    }

    fun add(t: Task, atEnd: Boolean) {
        add(TodoListItem(0,t.text,false),atEnd)
    }


    fun remove(item: TodoListItem) {
        queueRunnable("Remove", Runnable {
            dao.delete(item)
        })
    }


    fun size(): Long {
        return dao.queryBuilder().count()
    }

    fun updateItems(items: List<TodoListItem>) {
        queueRunnable("Update items", Runnable {
            dao.updateInTx(items)
        })
    }

    fun updateItem(item: TodoListItem) {
        queueRunnable("Update item", Runnable {
            dao.updateInTx(item)
        })
    }

    operator fun get(position: Int): TodoListItem? {
        if (position < dao.count()) {
            return dao.queryBuilder().orderAsc(Properties.Line).offset(position).listLazy()[0]
        } else {
            return null
        }
    }

    val priorities: ArrayList<Priority>
        get() {
            val res = HashSet<Priority>()
            for (item in dao.loadAll()) {
                res.add(item.task.priority)
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
            for (item in dao.loadAll()) {
                res.addAll(item.task.lists)
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
            for (item in dao.loadAll()) {
                res.addAll(item.task.tags)
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
                val task = it.task
                task.markIncomplete()
                it.text = task.text
            }
        })
    }

    fun complete(item: TodoListItem,
                 keepPrio: Boolean,
                 extraAtEnd: Boolean) {

        queueRunnable("Complete", Runnable {
            val task  = item.task
            val extra = task.markComplete(todayAsString)
            item.text = task.text
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
                item.text = task.text

            }
            dao.updateInTx(items)
        })
    }

    fun defer(deferString: String, items: List<TodoListItem>, dateType: DateType) {
        items.forEach {
            val taskToDefer = it.task
            when (dateType) {
                DateType.DUE -> taskToDefer.deferDueDate(deferString, todayAsString)
                DateType.THRESHOLD -> taskToDefer.deferThresholdDate(deferString, todayAsString)
            }
            it.text = taskToDefer.text
        }
        updateItems(items)
    }

    var selectedTasks: List<TodoListItem>? = ArrayList()
        get() {
            return dao.queryBuilder().where(Properties.Selected.eq(true)).list()
        }


    fun notifyChanged(fileStore: FileStoreInterface, todoName: String, eol: String, backup: BackupInterface?, save: Boolean) {
        log.info(TAG, "Handler: Queue notifychanged")
        todolistQueue!!.post {
            if (save) {
                log.info(TAG, "Handler: Handle notifyChanged")
                log.info(TAG, "Saving todo list, size {}" + dao.count())
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

    val todoItems: List<TodoListItem>
        get() = dao.queryBuilder().orderAsc(Properties.Line).listLazy()

    fun getSortedTasksCopy(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): List<TodoListItem> {
        val filteredTasks = filter.apply(todoItems)
        val comp = MultiComparator(sorts, caseSensitive, app.useCreateBackup())
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
                app.todoListItemDao.session.callInTx {
                    app.todoListItemDao.deleteAll()
                    var line: Long = 0
                    for (t in fileStore.loadTasksFromFile(filename, backup, eol)) {
                        app.todoListItemDao.insert(
                                TodoListItem(line, t, false))
                        line++
                    }
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()

            } catch (e: IOException) {
                log.error(TAG, "TodoList load failed: {}" + filename, e)
                showToastShort(TodoApplication.getAppContext(), "Loading of todo file failed")
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
            try {
                val tasks = dao.queryBuilder().orderAsc(Properties.Line).listLazyUncached()
                val lines = tasks.map {
                    it.text
                }
                fileStore.saveTasksToFile(todoFileName, lines, backup, eol)
            } catch (e: IOException) {
                e.printStackTrace()
                showToastLong(TodoApplication.getAppContext(), R.string.write_failed)
            }
        })

    }

    fun archive(fileStore: FileStoreInterface, todoFilename: String, doneFileName: String, tasks: List<TodoListItem>?, eol: String) {
        queueRunnable("Archive", Runnable {
            val tasksToArchive: List<TodoListItem>
            if (tasks == null) {
                tasksToArchive = dao.queryBuilder().where(Properties.Text.like("x %")).list();
            } else {
                tasksToArchive = tasks.filter { it.task.isCompleted() };
            }
            try {
                fileStore.appendTaskToFile(doneFileName, tasksToArchive.map {it.text}, eol);
                dao.deleteInTx(tasksToArchive)

                notifyChanged(fileStore, todoFilename, eol, null, true);
            } catch (e : IOException) {
                e.printStackTrace();
                showToastShort(TodoApplication.getAppContext(), "Task archiving failed");
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

    fun selectTasks(items: List<Task>) {
        dao.session.callInTx {
            items.forEach {
                val entities = dao.queryBuilder().where(Properties.Text.eq(it.text)).listLazy()
                if (entities.size > 0) {
                    entities[0].selected = true
                    dao.update(entities[0])
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
            dao.updateInTx(items)
    }

    fun unSelectTodoItem(item: TodoListItem) {
        unSelectTodoItems(listOf(item))
    }

    fun unSelectTodoItems(items: List<TodoListItem>) {
        items.forEach {
            it.selected = false
        }
        dao.updateInTx(items)
    }

    fun clearSelection() {
        val selectedItems = dao.queryBuilder().where(Properties.Selected.eq(true)).list()
        selectedItems.map {it.selected = false}
        dao.updateInTx(selectedItems)
    }
}
