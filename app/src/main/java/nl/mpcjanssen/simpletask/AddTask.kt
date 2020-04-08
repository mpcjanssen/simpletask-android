/**
 * This file is part of Simpletask.
 *
 * @copyright 2013- Mark Janssen
 */
package nl.mpcjanssen.simpletask

import android.app.DatePickerDialog
import android.content.*
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.appcompat.app.AlertDialog
import android.text.InputType
import android.text.Selection
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import hirondelle.date4j.DateTime
import kotlinx.android.synthetic.main.add_task.*
import nl.mpcjanssen.simpletask.task.Priority
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.*
import java.util.*

class AddTask : ThemedActionBarActivity() {
    private var startText: String = ""

    private val shareText: String? = null

    // private val m_backup = ArrayList<Task>()

    private var mBroadcastReceiver: BroadcastReceiver? = null
    private var localBroadcastManager: LocalBroadcastManager? = null

    /*
        Deprecated functions still work fine.
        For now keep using the old version, will updated if it breaks.
     */
    @Suppress("DEPRECATION")
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)

        TodoApplication.app.loadTodoList("before adding tasks")

        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_SYNC_START)
        intentFilter.addAction(Constants.BROADCAST_SYNC_DONE)

        localBroadcastManager = TodoApplication.app.localBroadCastManager

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.BROADCAST_SYNC_START) {
                    setProgressBarIndeterminateVisibility(true)
                } else if (intent.action == Constants.BROADCAST_SYNC_DONE) {
                    setProgressBarIndeterminateVisibility(false)
                }
            }
        }
        localBroadcastManager!!.registerReceiver(broadcastReceiver, intentFilter)
        mBroadcastReceiver = broadcastReceiver
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        setContentView(R.layout.add_task)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        if (!TodoApplication.config.useListAndTagIcons) {
            btnContext.setImageResource(R.drawable.ic_action_todotxt_lists)
            btnProject.setImageResource(R.drawable.ic_action_todotxt_tags)

        }


        if (shareText != null) {
            taskText.setText(shareText)
        }

        setTitle(R.string.addtask)

        Log.d(TAG, "Fill addtask")

        val pendingTasks = TodoApplication.todoList.pendingEdits.map { it.inFileFormat(TodoApplication.config.useUUIDs) }
            val preFillString = when {
                pendingTasks.isNotEmpty() -> {
                    setTitle(R.string.updatetask)
                    join(pendingTasks, "\n")
                }
                intent.hasExtra(Constants.EXTRA_PREFILL_TEXT) -> intent.getStringExtra(Constants.EXTRA_PREFILL_TEXT)
                intent.hasExtra(Query.INTENT_JSON) -> Query(intent, luaModule = "from_intent").prefill
                else -> ""
            }
            startText = preFillString
            // Avoid discarding changes on rotate
            if (taskText.text.isEmpty()) {
                taskText.setText(preFillString)
            }

            setInputType()




            // Set button callbacks
            btnContext.setOnClickListener { showListMenu() }
            btnProject.setOnClickListener { showTagMenu() }
            btnPrio.setOnClickListener { showPriorityMenu() }
            btnDue.setOnClickListener { insertDate(DateType.DUE) }
            btnThreshold.setOnClickListener { insertDate(DateType.THRESHOLD) }
            btnNext.setOnClickListener { addPrefilledTask() }
            btnSave.setOnClickListener { saveTasksAndClose() }
            taskText.requestFocus()
            Selection.setSelection(taskText.text,0)

    }

    private fun addPrefilledTask() {
        val position = taskText.selectionStart
        val remainingText = taskText.text.toString().substring(position)
        val endOfLineDistance = remainingText.indexOf('\n')
        var endOfLine: Int
        endOfLine = if (endOfLineDistance == -1) {
            taskText.length()
        } else {
            position + endOfLineDistance
        }
        taskText.setSelection(endOfLine)
        replaceTextAtSelection("\n", false)

        val precedingText = taskText.text.toString().substring(0, endOfLine)
        val lineStart = precedingText.lastIndexOf('\n')
        val line: String
        line = if (lineStart != -1) {
            precedingText.substring(lineStart, endOfLine)
        } else {
            precedingText
        }
        val t = Task(line)
        val prefillItems = mutableListOf<String>()
        t.lists?.let {lists ->
            prefillItems.addAll(lists.map { "@$it" })
        }
        t.tags?.let {tags ->
            prefillItems.addAll(tags.map { "+$it" })
        }

        replaceTextAtSelection(join(prefillItems, " "), true)

        endOfLine++
        taskText.setSelection(endOfLine)
    }

    private fun setWordWrap(bool: Boolean) {
        taskText.setHorizontallyScrolling(!bool)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        val inflater = menuInflater
        inflater.inflate(R.menu.add_task, menu)

        // Set checkboxes
        val menuWordWrap = menu.findItem(R.id.menu_word_wrap)
        menuWordWrap.isChecked = TodoApplication.config.isWordWrap

        val menuCapitalizeTasks = menu.findItem(R.id.menu_capitalize_tasks)
        menuCapitalizeTasks.isChecked = TodoApplication.config.isCapitalizeTasks

        return true
    }

    private fun setInputType() {
        val basicType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        if (TodoApplication.config.isCapitalizeTasks) {
            taskText.inputType = basicType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        } else {
            taskText.inputType = basicType
        }
        setWordWrap(TodoApplication.config.isWordWrap)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                finishEdit(confirmation = true)
            }
            R.id.menu_word_wrap -> {
                val newVal = !TodoApplication.config.isWordWrap
                TodoApplication.config.isWordWrap = newVal
                setWordWrap(newVal)
                item.isChecked = !item.isChecked
            }
            R.id.menu_capitalize_tasks -> {
                TodoApplication.config.isCapitalizeTasks = !TodoApplication.config.isCapitalizeTasks
                setInputType()
                item.isChecked = !item.isChecked
            }
            R.id.menu_help -> {
                showHelp()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showHelp() {
        val i = Intent(this, HelpScreen::class.java)
        i.putExtra(Constants.EXTRA_HELP_PAGE, getText(R.string.help_add_task))
        startActivity(i)
    }

    private fun saveTasksAndClose() {
        val todoList = TodoApplication.todoList
        // strip line breaks
        val input: String = taskText.text.toString()

        // Don't add empty tasks
        if (input.trim { it <= ' ' }.isEmpty()) {
            Log.i(TAG, "Not adding empty line")
            finish()
            return
        }

        // Update the TodoList with changes
        val enteredTasks = getTasks().dropLastWhile { it.text.isEmpty() }.map { task ->
            if (TodoApplication.config.hasPrependDate) {
                Task(task.text, todayAsString)
            } else {
                task
            }
        }
        val origTasks = todoList.pendingEdits
        Log.i(TAG, "Saving ${enteredTasks.size} tasks, updating $origTasks tasks")
        todoList.update(origTasks, enteredTasks, TodoApplication.config.hasAppendAtEnd)

        // Save
        todoList.notifyTasklistChanged(TodoApplication.config.todoFile, save = true, refreshMainUI = false)
        finishEdit(confirmation = false)
    }

    private fun finishEdit(confirmation: Boolean) {
        val close = DialogInterface.OnClickListener { _, _ ->
            TodoApplication.todoList.clearPendingEdits()
            finish()
        }
        if (confirmation && (taskText.text.toString() != startText)) {
            showConfirmationDialog(this, R.string.cancel_changes, close, null)
        } else {
            close.onClick(null, 0)
        }

    }

    override fun onBackPressed() {
        saveTasksAndClose()
        super.onBackPressed()
    }

    private fun insertDate(dateType: DateType) {
        var titleId = R.string.defer_due
        if (dateType === DateType.THRESHOLD) {
            titleId = R.string.defer_threshold
        }
        val d = createDeferDialog(this, titleId, object : InputDialogListener {
            /*
                Deprecated functions still work fine.
                For now keep using the old version, will updated if it breaks.
            */
            @Suppress("DEPRECATION")
            override fun onClick(input: String) {
                if (input == "pick") {
                    /* Note on some Android versions the OnDateSetListener can fire twice
                     * https://code.google.com/p/android/issues/detail?id=34860
                     * With the current implementation which replaces the dates this is not an
                     * issue. The date is just replaced twice
                     */
                    val today = DateTime.today(TimeZone.getDefault())
                    val dialog = DatePickerDialog(this@AddTask, DatePickerDialog.OnDateSetListener { _, year, month, day ->
                        val date = DateTime.forDateOnly(year, month + 1, day)
                        insertDateAtSelection(dateType, date)
                    },
                            today.year!!,
                            today.month!! - 1,
                            today.day!!)

                    val showCalendar = TodoApplication.config.showCalendar
                    dialog.datePicker.calendarViewShown = showCalendar
                    dialog.datePicker.spinnersShown = !showCalendar
                    dialog.show()
                } else {
                    if (!input.isEmpty()) {
                        insertDateAtSelection(dateType, addInterval(DateTime.today(TimeZone.getDefault()), input))
                    } else {
                        replaceDate(dateType, input)
                    }
                }
            }
        })
        d.show()
    }

    private fun replaceDate(dateType: DateType, date: String) {
        if (dateType === DateType.DUE) {
            replaceDueDate(date)
        } else {
            replaceThresholdDate(date)
        }
    }

    private fun insertDateAtSelection(dateType: DateType, date: DateTime?) {
        date?.let {
            replaceDate(dateType, date.format("YYYY-MM-DD"))
        }
    }

    private fun showTagMenu() {
        val items = TreeSet<String>()

        items.addAll(TodoApplication.todoList.projects)
        // Also display projects in tasks being added
        val tasks = getTasks()
        if (tasks.size == 0) {
            tasks.add(Task(""))
        }
        tasks.forEach {task ->
            task.tags?.let {items.addAll(it)}
        }
        val idx = getCurrentCursorLine()
        val task = getTasks().getOrElse(idx) { Task("") }

        updateItemsDialog(
                TodoApplication.config.tagTerm,
                listOf(task),
                ArrayList(items),
                Task::tags,
                Task::addTag,
                Task::removeTag
        ) {
            if (idx != -1) {
                tasks[idx] = task
            } else {
                tasks.add(task)
            }
            taskText.setText(tasks.joinToString("\n") { it.text })
        }
    }

    private fun showPriorityMenu() {
        val builder = AlertDialog.Builder(this)
        val priorities = Priority.values()
        val priorityCodes = priorities.mapTo(ArrayList()) { it.code }

        builder.setItems(priorityCodes.toArray<String>(arrayOfNulls<String>(priorityCodes.size))
        ) { _, which -> replacePriority(priorities[which].code) }

        // Create the AlertDialog
        val dialog = builder.create()
        dialog.setTitle(R.string.priority_prompt)
        dialog.show()
    }

    private fun getTasks(): MutableList<Task> {
        val input = taskText.text.toString()
        return input.split("\r\n|\r|\n".toRegex()).asSequence().map(::Task).toMutableList()
    }

    private fun showListMenu() {
        val items = TreeSet<String>()

        items.addAll(TodoApplication.todoList.contexts)
        // Also display contexts in tasks being added
        val tasks = getTasks()
        if (tasks.size == 0) {
            tasks.add(Task(""))
        }
        tasks.forEach {task ->
            task.lists?.let {items.addAll(it)}
        }

        val idx = getCurrentCursorLine()
        val task = getTasks().getOrElse(idx) { Task("") }

        updateItemsDialog(
                TodoApplication.config.listTerm,
                listOf(task),
                ArrayList(items),
                Task::lists,
                Task::addList,
                Task::removeList
        ) {
            if (idx != -1) {
                tasks[idx] = task
            } else {
                tasks.add(task)
            }
            taskText.setText(tasks.joinToString("\n") { it.text })
        }
    }

    private fun getCurrentCursorLine(): Int {
        val selectionStart = taskText.selectionStart
        if (selectionStart == -1) {
            return -1
        }

        val chars = taskText.text.subSequence(0, selectionStart)
        return (0 until chars.length).count { chars[it] == '\n' }
    }

    private fun replaceDueDate(newDueDate: CharSequence) {
        // save current selection and length
        val start = taskText.selectionStart
        val length = taskText.text.length
        val lines = ArrayList<String>()
        Collections.addAll(lines, *taskText.text.toString().split("\\n".toRegex()).toTypedArray())

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        var currentLine = getCurrentCursorLine()
        if (currentLine > lines.size - 1) {
            currentLine = lines.size - 1
        }
        if (currentLine != -1) {
            val t = Task(lines[currentLine])
            t.dueDate = newDueDate.toString()
            lines[currentLine] = t.inFileFormat(TodoApplication.config.useUUIDs)
            taskText.setText(join(lines, "\n"))
        }
        restoreSelection(start, length, false)
    }

    private fun replaceThresholdDate(newThresholdDate: CharSequence) {
        // save current selection and length
        val start = taskText.selectionStart
        val length = taskText.text.length
        val lines = ArrayList<String>()
        Collections.addAll(lines, *taskText.text.toString().split("\\n".toRegex()).toTypedArray())

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        var currentLine = getCurrentCursorLine()
        if (currentLine > lines.size - 1) {
            currentLine = lines.size - 1
        }
        if (currentLine != -1) {
            val t = Task(lines[currentLine])
            t.thresholdDate = newThresholdDate.toString()
            lines[currentLine] = t.inFileFormat(TodoApplication.config.useUUIDs)
            taskText.setText(join(lines, "\n"))
        }
        restoreSelection(start, length, false)
    }

    private fun restoreSelection(location: Int, oldLength: Int, moveCursor: Boolean) {
        var newLocation = location
        val newLength = taskText.text.length
        val deltaLength = newLength - oldLength
        // Check if we want the cursor to move by delta (for priority changes)
        // or not (for due and threshold changes
        if (moveCursor) {
            newLocation += deltaLength
        }

        // Don't go out of bounds
        newLocation = Math.min(newLocation, newLength)
        newLocation = Math.max(0, newLocation)
        taskText.setSelection(newLocation, newLocation)
    }

    private fun replacePriority(newPriority: CharSequence) {
        // save current selection and length
        val start = taskText.selectionStart
        val end = taskText.selectionEnd
        Log.d(TAG, "Current selection: $start-$end")
        val length = taskText.text.length
        val lines = ArrayList<String>()
        Collections.addAll(lines, *taskText.text.toString().split("\\n".toRegex()).toTypedArray())

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        var currentLine = getCurrentCursorLine()
        if (currentLine > lines.size - 1) {
            currentLine = lines.size - 1
        }
        if (currentLine != -1) {
            val t = Task(lines[currentLine])
            Log.d(TAG, "Changing priority from " + t.priority.toString() + " to " + newPriority.toString())
            t.priority = Priority.toPriority(newPriority.toString())
            lines[currentLine] = t.inFileFormat(TodoApplication.config.useUUIDs)
            taskText.setText(join(lines, "\n"))
        }
        restoreSelection(start, length, true)
    }

    private fun replaceTextAtSelection(newText: CharSequence, spaces: Boolean) {
        var text = newText
        val start = taskText.selectionStart
        val end = taskText.selectionEnd
        if (start == end && start != 0 && spaces) {
            // no selection prefix with space if needed
            if (taskText.text[start - 1] != ' ') {
                text = " $text"
            }
        }
        taskText.text.replace(Math.min(start, end), Math.max(start, end),
                text, 0, text.length)
    }

    public override fun onDestroy() {
        super.onDestroy()
        mBroadcastReceiver?.let {
            localBroadcastManager?.unregisterReceiver(it)
        }
    }

    companion object {
        private const val TAG = "AddTask"
    }
}
