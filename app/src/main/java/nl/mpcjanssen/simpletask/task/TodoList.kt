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
import nl.mpcjanssen.simpletask.*

import nl.mpcjanssen.simpletask.remote.BackupInterface
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.remote.IFileStore
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Implementation of the in memory representation of the Todo list
 * uses an ActionQueue to ensure modifications and access of the underlying todo list are
 * sequential. If this is not done properly the result is a likely ConcurrentModificationException.

 * @author Mark Janssen
 */
object TodoList {
    private val log: Logger = Logger

    private var mLists: ArrayList<String>? = null
    private var mTags: ArrayList<String>? = null
    val todoItems = CopyOnWriteArrayList<Task>()
    val selectedItems = CopyOnWriteArraySet<Task>()
    val pendingEdits = ArrayList<Int>()
    internal val TAG = TodoList::class.java.simpleName

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

    fun queue(description: String, body: () -> Unit) {
        val r = Runnable(body)
        ActionQueue.add(description, r)
    }

    fun add(items: List<Task>, atEnd: Boolean) {
        queue("Add task ${items.size} atEnd: $atEnd") {
            if (atEnd) {
                todoItems.addAll(items)
            } else {
                todoItems.addAll(0, items)
            }
        }
    }

    fun add(t: Task, atEnd: Boolean) {
        add(listOf(t), atEnd)
    }

    fun removeAll(tasks: List<Task>) {
        queue("Remove") {
            todoItems.removeAll(tasks)
            selectedItems.removeAll(tasks)
            tasks.forEach {
                pendingEdits.remove(todoItems.indexOf(it))
            }
        }
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
            todoItems.toMutableList().forEach {
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
            todoItems.toMutableList().forEach {
                res.addAll(it.tags)
            }
            val newTags = ArrayList<String>()
            newTags.addAll(res)
            mTags = newTags
            return newTags
        }

    fun uncomplete(items: List<Task>) {
        queue("Uncomplete") {
            items.forEach {
                it.markIncomplete()
            }
        }
    }

    fun complete(tasks: List<Task>, keepPrio: Boolean, extraAtEnd: Boolean) {
        queue("Complete") {
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
        }
    }

    fun prioritize(tasks: List<Task>, prio: Priority) {
        queue("Complete") {
            tasks.map { it.priority = prio }
        }

    }

    fun defer(deferString: String, tasks: List<Task>, dateType: DateType) {
        queue("Defer") {
            tasks.forEach {
                when (dateType) {
                    DateType.DUE -> it.deferDueDate(deferString, todayAsString)
                    DateType.THRESHOLD -> it.deferThresholdDate(deferString, todayAsString)
                }
            }
        }
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
        queue("Notified changed") {
            if (save) {
                save(FileStore, todoName, backup, eol)
            }
            if (!Config.hasKeepSelection) {
                TodoList.clearSelection()
            }
            mLists = null
            mTags = null
            broadcastRefreshUI(TodoApplication.app.localBroadCastManager)
        }
    }

    fun startAddTaskActivity(act: Activity, prefill: String) {
        queue("Start add/edit task activity") {
            val intent = Intent(act, AddTask::class.java)
            intent.putExtra(Constants.EXTRA_PREFILL_TEXT, prefill)
            act.startActivity(intent)
        }
    }

    fun getSortedTasks(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): Sequence<Task> {
        val comp = MultiComparator(sorts, TodoApplication.app.today, caseSensitive, filter.createIsThreshold)
        val itemsToSort = if (comp.fileOrder) {
            todoItems
        } else {
            todoItems.reversed()
        }
        val filteredTasks = filter.apply(itemsToSort.asSequence())
        comp.comparator?.let {
            return filteredTasks.sortedWith(it)
        }
        return filteredTasks
    }

    fun reload(backup: BackupInterface, eol: String, reason: String = "") {
        val logText = "Reload: " + reason
        queue(logText) {

            if (!FileStore.isAuthenticated) return@queue
            broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
            val filename = Config.todoFileName
            val cachedList = Config.todoList
            if (Config.changesPending && FileStore.isOnline) {
                log.info(TAG, "Not loading, changes pending")
                if (cachedList != null) {
                    log.info(TAG,"Saving instead of loading")
                    save(FileStore,filename,backup,eol)
                }
            } else {
                val needSync = try {
                    val newerVersion = FileStore.getRemoteVersion(Config.todoFileName)
                    newerVersion != Config.lastSeenRemoteId
                } catch (e: Throwable) {
                    log.error(TAG, "Can't determine remote file version", e)
                    false
                }

                if (needSync) {
                    log.info(TAG, "Remote version is different, sync")
                } else {
                    log.info(TAG, "Remote version is same, load from cache")
                }
                if (cachedList == null || needSync) {
                    try {
                        val remoteContents = FileStore.loadTasksFromFile(filename, eol)
                        val items = ArrayList<Task>(
                                remoteContents.contents.map { text ->
                                    Task(text)
                                })

                        todoItems.clear()
                        todoItems.addAll(items)
                        // Update cache
                        Config.cachedContents = remoteContents.contents.joinToString("\n")
                        Config.lastSeenRemoteId = remoteContents.remoteId

                        // Backup
                        backup.backup(filename, items)

                        clearSelection()

                    } catch (e: Exception) {
                        log.error(TAG, "TodoList load failed: {}" + filename, e)
                        showToastShort(TodoApplication.app, "Loading of todo file failed")
                    }
                    log.info(TAG, "TodoList loaded from dropbox")
                } else {
                    log.info(TAG, "Todolist loaded from cache")
                    todoItems.clear()
                    todoItems.addAll(cachedList)
                }
                broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
                notifyChanged(filename, eol, backup, false)
            }
        }
    }

    private fun save(fileStore: IFileStore, todoFileName: String, backup: BackupInterface?, eol: String) {
        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
        val lines = todoItems.map {
            it.inFileFormat()
        }
        // Update cache
        Config.cachedContents = lines.joinToString("\n")
        backup?.backup(todoFileName, todoItems)
        try {
            log.info(TAG, "Saving todo list, size ${lines.size}")
            val rev = fileStore.saveTasksToFile(todoFileName, lines, eol = eol)
            Config.lastSeenRemoteId = rev
            val changesWerePending = Config.changesPending
            Config.changesPending = false
            if (changesWerePending) {
                // Remove the red bar
                broadcastRefreshUI(TodoApplication.app.localBroadCastManager)
            }

        }  catch (e: Exception) {
            log.error(TAG, "TodoList save to $todoFileName failed", e)
            Config.changesPending = true
            if (FileStore.isOnline) {
                showToastShort(TodoApplication.app, "Saving of todo file failed")
            }
        }
        broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
    }

    fun archive(todoFilename: String, doneFileName: String, tasks: List<Task>?, eol: String) {
        queue("Archive") {

            val tasksToDelete = tasks ?: completedTasks
            queue("Append to file") {
                try {
                    FileStore.appendTaskToFile(doneFileName, tasksToDelete.map { it.text }, eol)
                    removeAll(tasksToDelete)
                    notifyChanged(todoFilename, eol, null, true)
                } catch (e: Exception) {
                    log.error(TAG, "Task archiving failed", e)
                    showToastShort(TodoApplication.app, "Task archiving failed")
                }


            }
        }
    }

    fun isSelected(item: Task): Boolean {
        return selectedItems.indexOf(item) > -1
    }

    fun numSelected(): Int {
        return selectedItems.size
    }

    fun selectTasks(items: List<Task>) {
        queue("Select") {
            selectedItems.addAll(items)
            broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
        }
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
        queue("Unselect") {
            selectedItems.removeAll(items)
            broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
        }
    }

    fun clearSelection() {
        queue("Clear selection") {
            selectedItems.clear()
            broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
        }
    }

    fun getTaskCount(): Long {
        val items = todoItems
        return items.filter { it.inFileFormat().isNotBlank() }.size.toLong()
    }


    fun editTasks(from: Activity, tasks: List<Task>, prefill: String) {
        queue("Edit tasks") {
            for (task in tasks) {
                val i = TodoList.todoItems.indexOf(task)
                if (i >= 0) {
                    pendingEdits.add(Integer.valueOf(i))
                }
            }
            startAddTaskActivity(from, prefill)
        }
    }

    fun clearPendingEdits() {
        queue("Clear selection") {
            pendingEdits.clear()
        }
    }
}

