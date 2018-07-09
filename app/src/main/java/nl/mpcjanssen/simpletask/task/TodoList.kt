package nl.mpcjanssen.simpletask.task

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.remote.IFileStore
import nl.mpcjanssen.simpletask.util.*
import java.util.*
import kotlin.collections.ArrayList

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
    val todoItems = ArrayList<Task>()
    val pendingEdits = ArrayList<Int>()
    internal val TAG = TodoList::class.java.simpleName

    init {
        Config.todoList?.let { todoItems.addAll(it) }
    }

    fun add(items: List<Task>, atEnd: Boolean) {
        log.debug(TAG, "Add task ${items.size} atEnd: $atEnd")
        val updatedItems = items.map { item ->
            Interpreter.onAddCallback(item) ?: item
        }
        if (atEnd) {
            todoItems.addAll(updatedItems)
        } else {
            todoItems.addAll(0, updatedItems)
        }

    }

    fun add(t: Task, atEnd: Boolean) {
        add(listOf(t), atEnd)
    }

    fun removeAll(tasks: List<Task>) {
        log.debug(TAG, "Remove")
        tasks.forEach {
            val idx = todoItems.indexOf(it)
            pendingEdits.remove(idx)
        }
        todoItems.removeAll(tasks)

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
        log.debug(TAG, "Uncomplete")
        items.forEach {
            it.markIncomplete()
        }
    }

    fun complete(tasks: List<Task>, keepPrio: Boolean, extraAtEnd: Boolean) {
        log.debug(TAG, "Complete")
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

    fun prioritize(tasks: List<Task>, prio: Priority) {
        log.debug(TAG, "Complete")
        tasks.map { it.priority = prio }
    }

    fun defer(deferString: String, tasks: List<Task>, dateType: DateType) {
        log.debug(TAG, "Defer")
        tasks.forEach {
            when (dateType) {
                DateType.DUE -> it.deferDueDate(deferString, todayAsString)
                DateType.THRESHOLD -> it.deferThresholdDate(deferString, todayAsString)
            }
        }
    }

    var selectedTasks: List<Task> = ArrayList()
        get() {
            return todoItems.filter { it.selected }
        }


    var completedTasks: List<Task> = ArrayList()
        get() {
            return todoItems.filter { it.isCompleted() }
        }

    fun notifyTasklistChanged(todoName: String, save: Boolean) {
        log.debug(TAG, "Notified changed")
        if (save) {
            save(FileStore, todoName, true, Config.eol)
        }
        if (!Config.hasKeepSelection) {
            clearSelection()
        }
        mLists = null
        mTags = null
        broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)
    }

    fun startAddTaskActivity(act: Activity, prefill: String) {
        log.debug(TAG, "Start add/edit task activity")
        val intent = Intent(act, AddTask::class.java)
        intent.putExtra(Constants.EXTRA_PREFILL_TEXT, prefill)
        act.startActivity(intent)
    }

    fun getSortedTasks(filter: Query, sorts: ArrayList<String>, caseSensitive: Boolean): Pair<List<Task>, Int> {
        log.debug(TAG, "Getting sorted and filtered tasks")
        val start = SystemClock.elapsedRealtime()
        val comp = MultiComparator(sorts, TodoApplication.app.today, caseSensitive, filter.createIsThreshold, filter.luaModule)
        val taskCount = todoItems.size
        val itemsToSort = if (comp.fileOrder) {
            todoItems
        } else {
            todoItems.reversed()
        }
        val sortedItems = comp.comparator?.let { itemsToSort.sortedWith(it) } ?: itemsToSort
        val result = filter.applyFilter(sortedItems, showSelected = true)
        val end = SystemClock.elapsedRealtime()
        log.debug(TAG, "Sorting and filtering tasks took ${end - start} ms")
        return Pair(result, taskCount)

    }

    fun reload(reason: String = "") {
        log.debug(TAG, "Reload: $reason")
        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
        if (!FileStore.isAuthenticated) return
        val filename = Config.todoFileName
        if (Config.changesPending && FileStore.isOnline) {
            log.info(TAG, "Not loading, changes pending")
            log.info(TAG, "Saving instead of loading")
            save(FileStore, filename, true, Config.eol)
        } else {
            FileStoreActionQueue.add("Reload") {
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
                val cachedList = Config.todoList
                if (cachedList == null || needSync) {
                    try {
                        val remoteContents = FileStore.loadTasksFromFile(filename)
                        val items = ArrayList<Task>(
                                remoteContents.contents.map { text ->
                                    Task(text)
                                })

                        log.debug(TAG, "Fill todolist")
                        todoItems.clear()
                        todoItems.addAll(items)
                        // Update cache
                        Config.cachedContents = remoteContents.contents.joinToString("\n")
                        Config.lastSeenRemoteId = remoteContents.remoteId
                        // Backup
                        Backupper.backup(filename, items.map { it.inFileFormat() })
                        notifyTasklistChanged(filename, false)


                    } catch (e: Exception) {
                        log.error(TAG, "TodoList load failed: {}" + filename, e)
                        showToastShort(TodoApplication.app, "Loading of todo file failed")
                    }

                    log.info(TAG, "TodoList loaded from dropbox")
                }
            }
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
        }
    }

    private fun save(fileStore: IFileStore, todoFileName: String, backup: Boolean, eol: String) {
        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
        val lines = todoItems.map {
            it.inFileFormat()
        }
        // Update cache
        Config.cachedContents = lines.joinToString("\n")
        if (backup) {
            Backupper.backup(todoFileName, lines)
        }
        FileStoreActionQueue.add("Save") {
            try {
                log.info(TAG, "Saving todo list, size ${lines.size}")
                val rev = fileStore.saveTasksToFile(todoFileName, lines, eol = eol)
                Config.lastSeenRemoteId = rev
                val changesWerePending = Config.changesPending
                Config.changesPending = false
                if (changesWerePending) {
                    // Remove the red bar
                    broadcastUpdateStateIndicator(TodoApplication.app.localBroadCastManager)
                }

            } catch (e: Exception) {
                log.error(TAG, "TodoList save to $todoFileName failed", e)
                Config.changesPending = true
                if (FileStore.isOnline) {
                    showToastShort(TodoApplication.app, "Saving of todo file failed")
                }
            }
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
        }
    }

    fun archive(todoFilename: String, doneFileName: String, tasks: List<Task>?, eol: String) {
        log.debug(TAG, "Archive ${tasks?.size ?: 0} tasks")

        val tasksToDelete = tasks ?: completedTasks

        FileStoreActionQueue.add("Append to file") {
            broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
            try {
                FileStore.appendTaskToFile(doneFileName, tasksToDelete.map { it.text }, eol)
                removeAll(tasksToDelete)
                notifyTasklistChanged(todoFilename, true)
            } catch (e: Exception) {
                log.error(TAG, "Task archiving failed", e)
                showToastShort(TodoApplication.app, "Task archiving failed")
            }
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
        }
    }

    fun isSelected(item: Task): Boolean = item.selected

    fun numSelected(): Int {
        return todoItems.count { it.selected }
    }

    fun selectTasks(items: List<Task>) {
        log.debug(TAG, "Select")
        items.forEach { selectTask(it) }
        broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
    }

    private fun selectTask(item: Task?) {
        item?.selected = true
    }

    private fun unSelectTask(item: Task) {
        item.selected = false
    }

    fun unSelectTasks(items: List<Task>) {
        log.debug(TAG, "Unselect")
        items.forEach { unSelectTask(it) }
        broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)

    }

    fun clearSelection() {
        log.debug(TAG, "Clear selection")
        todoItems.forEach { it.selected = false }
        broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)

    }

    fun getTaskIndex(t: Task): Int {
        return todoItems.indexOf(t)
    }

    fun getTaskAt(idx: Int): Task? {
        return todoItems.getOrNull(idx)
    }


    fun editTasks(from: Activity, tasks: List<Task>, prefill: String) {
        log.debug(TAG, "Edit tasks")
        for (task in tasks) {
            val i = TodoList.todoItems.indexOf(task)
            if (i >= 0) {
                pendingEdits.add(Integer.valueOf(i))
            }
        }
        startAddTaskActivity(from, prefill)
    }

    fun clearPendingEdits() {
        log.debug(TAG, "Clear selection")
        pendingEdits.clear()
    }
}

