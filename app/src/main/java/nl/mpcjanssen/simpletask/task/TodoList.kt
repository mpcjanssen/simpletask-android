package nl.mpcjanssen.simpletask.task

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.util.Log
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

    private var mLists: MutableList<String>? = null
    private var mTags: MutableList<String>? = null
    private val todoItems = ArrayList<Task>()
    val pendingEdits = ArrayList<Task>()
    internal val TAG = TodoList::class.java.simpleName

    init {
        Config.todoList?.let { todoItems.addAll(it) }
    }

    @Synchronized
    fun add(items: List<Task>, atEnd: Boolean) {
        Log.d(TAG, "Add task ${items.size} atEnd: $atEnd")
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

    @Synchronized
    fun removeAll(tasks: List<Task>) {
        Log.d(TAG, "Remove")
        pendingEdits.removeAll(tasks)
        todoItems.removeAll(tasks)

    }

    @Synchronized
    fun size(): Int {
        return todoItems.size
    }


    val priorities: ArrayList<Priority>
        get() {
            val res = HashSet<Priority>()
            todoItems.iterator().forEach {
                res.add(it.priority)
            }
            val ret = ArrayList(res)
            ret.sort()
            return ret
        }

    val contexts: List<String>
        get() {
            val lists = mLists
            if (lists != null) {
                return lists
            }
            val res = HashSet<String>()
            todoItems.iterator().forEach { t ->
                t.lists?.let {res.addAll(it)}

            }
            val newLists = res.toMutableList()
            mLists = newLists
            return newLists
        }

    val projects: List<String>
        get() {
            val tags = mTags
            if (tags != null) {
                return tags
            }
            val res = HashSet<String>()
            todoItems.iterator().forEach { t ->
                t.tags?.let {res.addAll(it)}

            }
            val newTags = res.toMutableList()
            mTags = newTags
            return newTags
        }

    @Synchronized
    fun uncomplete(items: List<Task>) {
        Log.d(TAG, "Uncomplete")
        items.forEach {
            it.markIncomplete()
        }
    }

    @Synchronized
    fun complete(tasks: List<Task>, keepPrio: Boolean, extraAtEnd: Boolean) {
        Log.d(TAG, "Complete")
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

    @Synchronized
    fun prioritize(tasks: List<Task>, prio: Priority) {
        Log.d(TAG, "Complete")
        tasks.map { it.priority = prio }
    }

    fun defer(deferString: String, tasks: List<Task>, dateType: DateType) {
        Log.d(TAG, "Defer")
        tasks.forEach {
            when (dateType) {
                DateType.DUE -> it.deferDueDate(deferString, todayAsString)
                DateType.THRESHOLD -> it.deferThresholdDate(deferString, todayAsString)
            }
        }
    }

    @Synchronized
    fun update(org: List<Task>, updated: List<Task>, addAtEnd: Boolean) {
        val smallestSize = org.zip(updated) { orgTask, updatedTask ->
            val idx = todoItems.indexOf(orgTask)
            if (idx != -1) {
                todoItems[idx] = updatedTask
            } else {
                todoItems.add(updatedTask)
            }
            1
        }.size
        removeAll(org.toMutableList().drop(smallestSize))
        add(updated.toMutableList().drop(smallestSize), addAtEnd)
    }

    val selectedTasks: List<Task>
        get() {
            return todoItems.toList().filter { it.selected }
        }

    val fileFormat : String =  todoItems.toList().joinToString(separator = "\n", transform = Task::inFileFormat)



    @Synchronized
    fun notifyTasklistChanged(todoName: String, save: Boolean, refreshMainUI: Boolean = true) {
        Log.d(TAG, "Notified changed")
        if (save) {
            save(FileStore, todoName, true, Config.eol)
        }
        if (!Config.hasKeepSelection) {
            clearSelection()
        }
        mLists = null
        mTags = null
        if (refreshMainUI) {
            broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)
        } else {
            broadcastRefreshWidgets(TodoApplication.app.localBroadCastManager)
        }
    }

    private fun startAddTaskActivity(act: Activity, prefill: String) {
        Log.d(TAG, "Start add/edit task activity")
        val intent = Intent(act, AddTask::class.java)
        intent.putExtra(Constants.EXTRA_PREFILL_TEXT, prefill)
        act.startActivity(intent)
    }

    @Synchronized
    fun getSortedTasks(filter: Query, sorts: ArrayList<String>, caseSensitive: Boolean): Pair<List<Task>, Int> {
        Log.d(TAG, "Getting sorted and filtered tasks")
        val start = SystemClock.elapsedRealtime()
        val comp = MultiComparator(sorts, TodoApplication.app.today, caseSensitive, filter.createIsThreshold, filter.luaModule)
        val listCopy = todoItems.toList()
        val taskCount = listCopy.size
        val itemsToSort = if (comp.fileOrder) {
            listCopy
        } else {
            listCopy.reversed()
        }
        val sortedItems = comp.comparator?.let { itemsToSort.sortedWith(it) } ?: itemsToSort
        val result = filter.applyFilter(sortedItems, showSelected = true)
        val end = SystemClock.elapsedRealtime()
        Log.d(TAG, "Sorting and filtering tasks took ${end - start} ms")
        return Pair(result, taskCount)

    }

    @Synchronized
    fun reload(reason: String = "") {
        Log.d(TAG, "Reload: $reason")
        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
        if (!FileStore.isAuthenticated) {
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
            return
        }
        val filename = Config.todoFileName
        if (Config.changesPending && FileStore.isOnline) {
            Log.i(TAG, "Not loading, changes pending")
            Log.i(TAG, "Saving instead of loading")
            save(FileStore, filename, true, Config.eol)
        } else {
            FileStoreActionQueue.add("Reload") {
                val needSync = try {
                    val newerVersion = FileStore.getRemoteVersion(Config.todoFileName)
                    newerVersion != Config.lastSeenRemoteId
                } catch (e: Throwable) {
                    Log.e(TAG, "Can't determine remote file version", e)
                    false
                }
                if (needSync) {
                    Log.i(TAG, "Remote version is different, sync")
                } else {
                    Log.i(TAG, "Remote version is same, load from cache")
                }
                val cachedList = Config.todoList
                if (cachedList == null || needSync) {
                    try {
                        val remoteContents = FileStore.loadTasksFromFile(filename)
                        val items = ArrayList<Task>(
                                remoteContents.contents.map { text ->
                                    Task(text)
                                })

                        Log.d(TAG, "Fill todolist")
                        todoItems.clear()
                        todoItems.addAll(items)
                        // Update cache
                        Config.cachedContents = remoteContents.contents.joinToString("\n")
                        Config.lastSeenRemoteId = remoteContents.remoteId
                        // Backup
                        Backupper.backup(filename, items.map { it.inFileFormat() })
                        notifyTasklistChanged(filename, false, true)
                    } catch (e: Exception) {
                        Log.e(TAG, "TodoList load failed: {}" + filename, e)
                        showToastShort(TodoApplication.app, "Loading of todo file failed")
                    }

                    Log.i(TAG, "TodoList loaded from dropbox")
                }
            }
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
        }
    }

    @Synchronized
    private fun save(fileStore: IFileStore, todoFileName: String, backup: Boolean, eol: String) {
        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
        val lines = todoItems.map {
            it.inFileFormat()
        }
        // Update cache
        Config.cachedContents = lines.joinToString("\n")

        FileStoreActionQueue.add("Save") {
            if (backup) {
                Backupper.backup(todoFileName, lines)
            }
            try {
                Log.i(TAG, "Saving todo list, size ${lines.size}")
                val rev = fileStore.saveTasksToFile(todoFileName, lines, eol = eol)
                Config.lastSeenRemoteId = rev
                val changesWerePending = Config.changesPending
                Config.changesPending = false
                if (changesWerePending) {
                    // Remove the red bar
                    broadcastUpdateStateIndicator(TodoApplication.app.localBroadCastManager)
                }

            } catch (e: Exception) {
                Log.e(TAG, "TodoList save to $todoFileName failed", e)
                Config.changesPending = true
                if (FileStore.isOnline) {
                    showToastShort(TodoApplication.app, "Saving of todo file failed")
                }
            }
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
        }
    }

    fun archive(todoFilename: String, doneFileName: String, tasks: List<Task>, eol: String) {
        Log.d(TAG, "Archive ${tasks.size} tasks")

        FileStoreActionQueue.add("Append to file") {
            broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
            try {
                FileStore.appendTaskToFile(doneFileName, tasks.map { it.text }, eol)
                removeAll(tasks)
                notifyTasklistChanged(todoFilename, true, true)
            } catch (e: Exception) {
                Log.e(TAG, "Task archiving failed", e)
                showToastShort(TodoApplication.app, "Task archiving failed")
            }
            broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
        }
    }

    fun isSelected(item: Task): Boolean = item.selected

    @Synchronized
    fun numSelected(): Int {
        return todoItems.toList().count { it.selected }
    }


    fun selectTasks(items: List<Task>) {
        Log.d(TAG, "Select")
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
        Log.d(TAG, "Unselect")
        items.forEach { unSelectTask(it) }
        broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)

    }

    @Synchronized
    fun clearSelection() {
        Log.d(TAG, "Clear selection")
        todoItems.iterator().forEach { it.selected = false }
        broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)

    }

    @Synchronized
    fun getTaskIndex(t: Task): Int {
        return todoItems.indexOf(t)
    }

    @Synchronized
    fun getTaskAt(idx: Int): Task? {
        return todoItems.getOrNull(idx)
    }

    @Synchronized
    fun each (callback : (Task) -> Unit) {
        todoItems.forEach { callback.invoke(it) }
    }

    fun editTasks(from: Activity, tasks: List<Task>, prefill: String) {
        Log.d(TAG, "Edit tasks")
        pendingEdits.addAll(tasks)
        startAddTaskActivity(from, prefill)
    }

    fun clearPendingEdits() {
        Log.d(TAG, "Clear selection")
        pendingEdits.clear()
    }
}

