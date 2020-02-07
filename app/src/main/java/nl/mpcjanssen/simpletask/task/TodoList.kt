package nl.mpcjanssen.simpletask.task

import android.app.Activity
import android.content.Intent
import android.net.Uri

import android.os.CountDownTimer
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
class TodoList(val config: Config) {
    private var timer: CountDownTimer? = null
    private var mLists: MutableList<String>? = null
    private var mTags: MutableList<String>? = null
    private val todoItems = ArrayList<Task>()
    val pendingEdits = HashSet<Task>()
    internal val tag = TodoList::class.java.simpleName

    init {
        config.todoList?.let { todoItems.addAll(it) }
    }

    @Synchronized
    fun add(items: List<Task>, atEnd: Boolean) {
        Log.d(tag, "Add task ${items.size} atEnd: $atEnd")
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
        Log.d(tag, "Remove")
        pendingEdits.removeAll(tasks)
        todoItems.removeAll(tasks)

    }

    @Synchronized
    fun size(): Int {
        return todoItems.size
    }

    val priorities: ArrayList<Priority>
        @Synchronized
        get() {
            val res = HashSet<Priority>()
            todoItems.forEach {
                res.add(it.priority)
            }
            val ret = ArrayList(res)
            ret.sort()
            return ret
        }

    val contexts: List<String>
        @Synchronized
        get() {
            val lists = mLists
            if (lists != null) {
                return lists
            }
            val res = HashSet<String>()
            todoItems.forEach { t ->
                t.lists?.let {res.addAll(it)}

            }
            val newLists = res.toMutableList()
            mLists = newLists
            return newLists
        }

    val projects: List<String>
        @Synchronized
        get() {
            val tags = mTags
            if (tags != null) {
                return tags
            }
            val res = HashSet<String>()
            todoItems.forEach { t ->
                t.tags?.let {res.addAll(it)}

            }
            val newTags = res.toMutableList()
            mTags = newTags
            return newTags
        }

    @Synchronized
    fun uncomplete(items: List<Task>) {
        Log.d(tag, "Uncomplete")
        items.forEach {
            it.markIncomplete()
        }
    }

    @Synchronized
    fun complete(tasks: List<Task>, keepPrio: Boolean, extraAtEnd: Boolean) {
        Log.d(tag, "Complete")
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
        Log.d(tag, "Complete")
        tasks.map { it.priority = prio }
    }

    @Synchronized
    fun defer(deferString: String, tasks: List<Task>, dateType: DateType) {
        Log.d(tag, "Defer")
        tasks.forEach {
            when (dateType) {
                DateType.DUE -> it.deferDueDate(deferString, todayAsString)
                DateType.THRESHOLD -> it.deferThresholdDate(deferString, todayAsString)
            }
        }
    }

    @Synchronized
    fun update(org: Collection<Task>, updated: List<Task>, addAtEnd: Boolean) {
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
        @Synchronized
        get() {
            return todoItems.toList().filter { it.selected }
        }

    val fileFormat : String =  todoItems.toList().joinToString(separator = "\n", transform = {
        it.inFileFormat(config.useUUIDs)
    })

    @Synchronized
    fun notifyTasklistChanged(uri: Uri?, save: Boolean, refreshMainUI: Boolean = true) {
        Log.d(tag, "Notified changed")
        if (uri == null) {
            return
        }
        if (uri == null) {
            return
        }
        if (save) {
            save(FileStore, uri, config.eol)
        }
        if (!config.hasKeepSelection) {
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

    @Synchronized
    private fun startAddTaskActivity(act: Activity, prefill: String) {
        Log.d(tag, "Start add/edit task activity")
        val intent = Intent(act, AddTask::class.java)
        intent.putExtra(Constants.EXTRA_PREFILL_TEXT, prefill)
        act.startActivity(intent)
    }

    @Synchronized
    fun getSortedTasks(filter: Query, caseSensitive: Boolean): Pair<List<Task>, Int> {
        val sorts = filter.getSort(config.defaultSorts)
        Log.d(tag, "Getting sorted and filtered tasks")
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
        Log.d(tag, "Sorting and filtering tasks took ${end - start} ms")
        return Pair(result, taskCount)

    }

    @Synchronized
    fun reload(reason: String = "") {
        Log.d(TAG, "Reload: $reason")
        val uri = config.todoUri ?: return
        if (config.changesPending) {
            Log.i(TAG, "Not loading, changes pending")
            Log.i(TAG, "Saving instead of loading")
            save(FileStore, uri,  config.eol)
        } else {
            FileStoreActionQueue.add("Reload") {
                reloadaction(uri)
            }
        }
    }

    @Synchronized
    private fun reloadaction(filename: Uri) {
        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)


            try {
                val remoteContents = FileStore.loadTasksFromFile(filename)
                val items = remoteContents.contents

                Log.d(TAG, "Fill todolist with ${items.size} items")
                todoItems.clear()
                todoItems.addAll(items.map { Task(it) })
                // Update cache
                Log.i(TAG, "Updating cache with remote version ${remoteContents}")
                config.todoList = todoItems
                config.lastSeenRemoteId = remoteContents.lastModified
                // Backup
                FileStoreActionQueue.add("Backup") {
                    Backupper.backup(filename.toString(), items)
                }
                notifyTasklistChanged(filename, false, true)
            } catch (e: Exception) {
                Log.e(TAG, "TodoList load failed: $filename", e)
                showToastShort(TodoApplication.app, "Loading of todo file failed")
            }

            Log.i(TAG, "TodoList loaded from filestore")

        broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
    }

    @Synchronized
    private fun save(fileStore: IFileStore, uri: Uri, eol: String) {
        config.changesPending = true
        broadcastUpdateStateIndicator(TodoApplication.app.localBroadCastManager)

        // Update cache
        config.todoList = todoItems

        val lines = todoItems.map { it.inFileFormat() }
        FileStoreActionQueue.add("Backup") {
            Backupper.backup(uri.toString(), lines)
        }
        runOnMainThread(Runnable {
            timer?.apply { cancel() }

            timer = object: CountDownTimer(config.idleBeforeSaveSeconds*1000L, 1000) {
                override fun onFinish() {
                    Log.d(tag, "Executing pending Save")
                    FileStoreActionQueue.add("Save") {
                        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
                        try {
                            Log.i(tag, "Saving todo list, size ${lines.size}")
                            fileStore.saveTasksToFile(uri, lines, eol = eol)
                            config.changesPending = false
                            broadcastUpdateStateIndicator(TodoApplication.app.localBroadCastManager)
                        } catch (e: Exception) {
                            Log.e(tag, "TodoList save to $uri failed", e)
                            config.changesPending = true
                        }
                        broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)
                    }
                }

                override fun onTick(p0: Long) {
                    Log.d(tag, "Scheduled save in $p0")
                }
            }.start()})
    }

    @Synchronized
    fun archive(todoFilename: Uri?, doneFileName: Uri, tasks: List<Task>, eol: String) {
        Log.d(tag, "Archive ${tasks.size} tasks")
        if (todoFilename == null) return
        removeAll(tasks)
        FileStoreActionQueue.add("archive to file") {
            try {
                FileStore.appendTaskToFile(doneFileName, tasks.map {it.inFileFormat(useUUIDs = TodoApplication.config.useUUIDs)}, eol)
                removeAll(tasks)
                notifyTasklistChanged(todoFilename, true, true)
            } catch (e: Exception) {
                Log.e(tag, "Task archiving failed", e)
                showToastShort(TodoApplication.app, "Task archiving failed")
            }
        }
    }

    fun isSelected(item: Task): Boolean = item.selected

    @Synchronized
    fun numSelected(): Int {
        return todoItems.toList().count { it.selected }
    }

    @Synchronized
    fun selectTasks(items: List<Task>) {
        Log.d(tag, "Select")
        items.forEach { selectTask(it) }
        broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
    }

    @Synchronized
    private fun selectTask(item: Task?) {
        item?.selected = true
    }

    @Synchronized
    private fun unSelectTask(item: Task) {
        item.selected = false
    }

    @Synchronized
    fun unSelectTasks(items: List<Task>) {
        Log.d(tag, "Unselect")
        items.forEach { unSelectTask(it) }
        broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)

    }

    @Synchronized
    fun clearSelection() {
        Log.d(tag, "Clear selection")
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

    @Synchronized
    fun editTasks(from: Activity, tasks: List<Task>, prefill: String) {
        Log.d(tag, "Edit tasks")
        pendingEdits.addAll(tasks)
        startAddTaskActivity(from, prefill)
    }

    @Synchronized
    fun clearPendingEdits() {
        Log.d(tag, "Clear selection")
        pendingEdits.clear()
    }
}
