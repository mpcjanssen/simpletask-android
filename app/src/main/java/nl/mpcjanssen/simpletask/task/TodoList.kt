package nl.mpcjanssen.simpletask.task

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.remote.BackupInterface
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
    val selectedIndexes = ArrayList<Int>()
    val pendingEdits = ArrayList<Int>()
    internal val TAG = TodoList::class.java.simpleName

    init {
        Config.todoList?.let { todoItems.addAll(it) }
    }

    fun hasPendingAction(): Boolean {
        return TodoActionQueue.hasPending()
    }

    // Wait until there are no more pending actions
    @Suppress("unused") // Used in test suite
    fun settle() {
        while (hasPendingAction()) {
            Thread.sleep(10)
        }
    }

    fun fileStoreQueue(description: String, body: () -> Unit) {
        val r = Runnable(body)
        FileStoreActionQueue.add(description, r)
    }

    fun todoQueue(description: String, body: () -> Unit) {
        val r = Runnable(body)
        TodoActionQueue.add(description, r)
    }

    fun add(items: List<Task>, atEnd: Boolean) {
        todoQueue("Add task ${items.size} atEnd: $atEnd") {
            val updatedItems = items.map {item ->
                LuaInterpreter.onAddCallback(item) ?: item
            }
            if (atEnd) {
                todoItems.addAll(updatedItems)
            } else {
                todoItems.addAll(0, updatedItems)
            }
        }
    }

    fun add(t: Task, atEnd: Boolean) {
        add(listOf(t), atEnd)
    }

    fun removeAll(tasks: List<Task>) {
        todoQueue("Remove") {
            tasks.forEach {
                val idx = todoItems.indexOf(it)
                selectedIndexes.remove(idx)
                pendingEdits.remove(idx)
            }
            todoItems.removeAll(tasks)
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
        todoQueue("Uncomplete") {
            items.forEach {
                it.markIncomplete()
            }
        }
    }

    fun complete(tasks: List<Task>, keepPrio: Boolean, extraAtEnd: Boolean) {
        todoQueue("Complete") {
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
        todoQueue("Complete") {
            tasks.map { it.priority = prio }
        }

    }

    fun defer(deferString: String, tasks: List<Task>, dateType: DateType) {
        todoQueue("Defer") {
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
            return selectedIndexes.map { idx ->
                todoItems[idx]
            }
        }

    var completedTasks: List<Task> = ArrayList()
        get() {
            return todoItems.filter { it.isCompleted() }
        }

    fun notifyTasklistChanged(todoName: String, eol: String, backup: BackupInterface?, save: Boolean) {
        todoQueue("Notified changed") {
            if (save) {
                save(FileStore, todoName, backup, eol)
            }
            if (!Config.hasKeepSelection) {
                TodoList.clearSelection()
            }
            mLists = null
            mTags = null
            broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)
            broadcastRefreshUI(TodoApplication.app.localBroadCastManager)
        }
    }

    fun startAddTaskActivity(act: Activity, prefill: String) {
        todoQueue("Start add/edit task activity") {
            val intent = Intent(act, AddTask::class.java)
            intent.putExtra(Constants.EXTRA_PREFILL_TEXT, prefill)
            act.startActivity(intent)
        }
    }

    fun getSortedTasks(filter: ActiveFilter, sorts: ArrayList<String>, caseSensitive: Boolean): Sequence<Task> {
        log.debug(TAG, "Getting sorted and filtered tasks")
        val start = SystemClock.elapsedRealtime()
        filter.initInterpreter()
        val comp = MultiComparator(sorts, TodoApplication.app.today, caseSensitive, filter.createIsThreshold, filter.options.luaModule)
        val itemsToSort = if (comp.fileOrder) {
            todoItems
        } else {
            todoItems.reversed()
        }
        val sortedItems = comp.comparator?.let { itemsToSort.sortedWith(it) } ?: itemsToSort
        val result = filter.apply(sortedItems.asSequence())
        val end = SystemClock.elapsedRealtime()
        log.debug(TAG, "Sorting and filtering tasks took ${end-start} ms")
        return result

    }

    fun reload(backup: BackupInterface, eol: String, reason: String = "") {
        val logText = "Reload: " + reason
        todoQueue(logText) {
            broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
            if (!FileStore.isAuthenticated) return@todoQueue
            val filename = Config.todoFileName
            if (Config.changesPending && FileStore.isOnline) {
                log.info(TAG, "Not loading, changes pending")
                log.info(TAG, "Saving instead of loading")
                save(FileStore, filename, backup, eol)
            } else {
                fileStoreQueue("Reload") {
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
                            val remoteContents = FileStore.loadTasksFromFile(filename, eol)
                            val items = ArrayList<Task>(
                                    remoteContents.contents.map { text ->
                                        Task(text)
                                    })

                            todoQueue("Fill todolist") {
                                todoItems.clear()
                                todoItems.addAll(items)
                                // Update cache
                                Config.cachedContents = remoteContents.contents.joinToString("\n")
                                Config.lastSeenRemoteId = remoteContents.remoteId
                                // Backup
                                backup.backup(filename, items)
                                notifyTasklistChanged(filename, eol, backup, false)
                            }


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
    }

    private fun save(fileStore: IFileStore, todoFileName: String, backup: BackupInterface?, eol: String) {
        broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
        val lines = todoItems.map {
            it.inFileFormat()
        }
        // Update cache
        Config.cachedContents = lines.joinToString("\n")
        backup?.backup(todoFileName, todoItems)
        fileStoreQueue("Save") {
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
        todoQueue("Archive") {

            val tasksToDelete = tasks ?: completedTasks

            fileStoreQueue("Append to file") {
                broadcastFileSyncStart(TodoApplication.app.localBroadCastManager)
                try {
                    FileStore.appendTaskToFile(doneFileName, tasksToDelete.map { it.text }, eol)
                    removeAll(tasksToDelete)
                    notifyTasklistChanged(todoFilename, eol, null, true)
                } catch (e: Exception) {
                    log.error(TAG, "Task archiving failed", e)
                    showToastShort(TodoApplication.app, "Task archiving failed")
                }
                broadcastFileSyncDone(TodoApplication.app.localBroadCastManager)


            }
        }
    }

    fun isSelected(item: Task): Boolean {
        val idx = todoItems.indexOf(item)
        return idx in selectedIndexes
    }

    fun numSelected(): Int {
        return selectedIndexes.size
    }

    fun selectTasks(items: List<Task>) {
        todoQueue("Select") {
            val idxs = items.map { todoItems.indexOf(it) }.filterNot { it == -1 }
            selectedIndexes.addAll(idxs)
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
        todoQueue("Unselect") {
            val idxs = items.map { todoItems.indexOf(it) }
            selectedIndexes.removeAll(idxs)
            broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
        }
    }

    fun clearSelection() {
        todoQueue("Clear selection") {
            selectedIndexes.clear()
            broadcastRefreshSelection(TodoApplication.app.localBroadCastManager)
        }
    }

    fun getTaskCount(): Long {
        val items = todoItems
        return items.filter { it.inFileFormat().isNotBlank() }.size.toLong()
    }

    fun getTaskIndex(t: Task): Int {
        return todoItems.indexOf(t)
    }

    fun getTaskAt(idx: Int): Task? {
        return todoItems.getOrNull(idx)
    }


    fun editTasks(from: Activity, tasks: List<Task>, prefill: String) {
        todoQueue("Edit tasks") {
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
        todoQueue("Clear selection") {
            pendingEdits.clear()
        }
    }
}

