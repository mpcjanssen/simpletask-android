package nl.mpcjanssen.simpletask.task

import android.graphics.Color
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.list_header.view.*
import kotlinx.android.synthetic.main.list_item.view.*
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.*
import java.util.ArrayList

class TaskViewHolder(itemView: View, val viewType : Int) : RecyclerView.ViewHolder(itemView)

class TaskAdapter(var query: Query, private val m_inflater: LayoutInflater, val completeAction: (Task) -> Unit, val uncompleteAction: (Task) -> Unit, val onClickAction: (Task) -> Unit, val onLongClickAction: (Task) -> Boolean) : RecyclerView.Adapter <TaskViewHolder>() {
    val TAG = "TaskAdapter"
    var textSize: Float = 14.0F
    override fun getItemCount(): Int {
        return visibleLines.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): TaskViewHolder {
        val view = when (viewType) {
            0 -> {
                // Header
                m_inflater.inflate(R.layout.list_header, parent, false)
            }
            1 -> {
                // Task
                m_inflater.inflate(R.layout.list_item, parent, false)
            }
            else -> {
                // Empty at end
                m_inflater.inflate(R.layout.empty_list_item, parent, false)
            }

        }
        return TaskViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: TaskViewHolder?, position: Int) {
        when (holder?.viewType) {
            0 -> bindHeader(holder, position)
            1 -> bindTask(holder, position)
            else -> return
        }
    }

    fun bindHeader(holder : TaskViewHolder, position: Int) {
        val t = holder.itemView.list_header_title
        val line = visibleLines[position]
        t.text = line.title
        t.textSize = textSize
    }

    fun bindTask (holder : TaskViewHolder, position: Int) {
        val line = visibleLines[position]
        val item = line.task ?: return
        val view = holder.itemView
        val taskText = view.tasktext
        val taskAge = view.taskage
        val taskDue = view.taskdue
        val taskThreshold = view.taskthreshold

        val task = item

        if (Config.showCompleteCheckbox) {
            view.checkBox.visibility = View.VISIBLE
        } else {
            view.checkBox.visibility = View.GONE
        }

        if (!Config.hasExtendedTaskView) {
            view.datebar.visibility = View.GONE
        }
        val tokensToShowFilter: (it: TToken) -> Boolean = {
            when (it) {
                is UUIDToken -> false
                is CreateDateToken -> false
                is CompletedToken -> false
                is CompletedDateToken -> !Config.hasExtendedTaskView
                is DueDateToken -> !Config.hasExtendedTaskView
                is ThresholdDateToken -> !Config.hasExtendedTaskView
                is ListToken -> !query.hideLists
                is TagToken -> !query.hideTags
                else -> true
            }
        }
        val txt = LuaInterpreter.onDisplayCallback(query.luaModule, task) ?: task.showParts(tokensToShowFilter)
        val ss = SpannableString(txt)

        val contexts = task.lists
        val colorizeStrings = contexts.mapTo(ArrayList<String>()) { "@" + it }
        setColor(ss, Color.GRAY, colorizeStrings)
        colorizeStrings.clear()
        val projects = task.tags
        projects.mapTo(colorizeStrings) { "+" + it }
        setColor(ss, Color.GRAY, colorizeStrings)

        val priorityColor: Int
        val priority = task.priority
        when (priority) {
            Priority.A -> priorityColor = ContextCompat.getColor(TodoApplication.app, R.color.simple_red_dark)
            Priority.B -> priorityColor = ContextCompat.getColor(TodoApplication.app, R.color.simple_orange_dark)
            Priority.C -> priorityColor = ContextCompat.getColor(TodoApplication.app, R.color.simple_green_dark)
            Priority.D -> priorityColor = ContextCompat.getColor(TodoApplication.app, R.color.simple_blue_dark)
            else -> priorityColor = ContextCompat.getColor(TodoApplication.app, R.color.gray67)
        }
        setColor(ss, priorityColor, priority.fileFormat)
        val completed = task.isCompleted()

        taskAge.textSize = textSize * Config.dateBarRelativeSize
        taskDue.textSize = textSize * Config.dateBarRelativeSize
        taskThreshold.textSize = textSize * Config.dateBarRelativeSize

        val cb = view.checkBox
        taskText.text = ss
        taskText.textSize = textSize
        handleEllipsis(taskText)

        if (completed) {
            // log.info( "Striking through " + task.getText());
            taskText.paintFlags = taskText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            taskAge.paintFlags = taskAge.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            cb.setOnClickListener { uncompleteAction(item) }
        } else {
            taskText.paintFlags = taskText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            taskAge.paintFlags = taskAge.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            cb.setOnClickListener { completeAction(item) }

        }
        cb.isChecked = completed

        val relAge = getRelativeAge(task, TodoApplication.app)
        val relDue = getRelativeDueDate(task, TodoApplication.app)
        val relativeThresholdDate = getRelativeThresholdDate(task, TodoApplication.app)
        if (!isEmptyOrNull(relAge) && !query.hideCreateDate) {
            taskAge.text = relAge
            taskAge.visibility = View.VISIBLE
        } else {
            taskAge.text = ""
            taskAge.visibility = View.GONE
        }

        if (relDue != null) {
            taskDue.text = relDue
            taskDue.visibility = View.VISIBLE
        } else {
            taskDue.text = ""
            taskDue.visibility = View.GONE
        }
        if (!isEmptyOrNull(relativeThresholdDate)) {
            taskThreshold.text = relativeThresholdDate
            taskThreshold.visibility = View.VISIBLE
        } else {
            taskThreshold.text = ""
            taskThreshold.visibility = View.GONE
        }
        // Set selected state
        // log.debug(TAG, "Setting selected state ${TodoList.isSelected(item)}")
        view.isActivated = TodoList.isSelected(item)

        // Set click listeners
        view.setOnClickListener { onClickAction (item) ; it.isActivated = !it.isActivated }

        view.setOnLongClickListener { onLongClickAction (item) }
    }
    internal var visibleLines = ArrayList<VisibleLine>()

    internal fun setFilteredTasks(caller: Simpletask?, newQuery: Query) {
        textSize = Config.tasklistTextSize ?: textSize
        log.info(TAG, "Text size = $textSize")
        query = newQuery
        TodoList.todoQueue("setFilteredTasks") {
            caller?.runOnUiThread {
                caller.showListViewProgress(true)
            }
            val visibleTasks: List<Task>
            log.info(TAG, "setFilteredTasks called: " + TodoList)
            val sorts = newQuery.getSort(Config.defaultSorts)
            visibleTasks = TodoList.getSortedTasks(newQuery, sorts, Config.sortCaseSensitive)
            val newVisibleLines = ArrayList<VisibleLine>()

            newVisibleLines.addAll(addHeaderLines(visibleTasks, newQuery, getString(R.string.no_header)))

            caller?.runOnUiThread {
                // Replace the array in the main thread to prevent OutOfIndex exceptions
                visibleLines = newVisibleLines
                notifyDataSetChanged()
                caller.showListViewProgress(false)
                if (Config.lastScrollPosition != -1) {
                    val manager = caller.listView?.layoutManager as LinearLayoutManager?
                    val position = Config.lastScrollPosition
                    val offset = Config.lastScrollOffset
                    Logger.info(TAG, "Restoring scroll offset $position, $offset")
                    manager?.scrollToPositionWithOffset(position, offset )
                    Config.lastScrollPosition = -1
                }
            }
        }
    }

    val countVisibleTasks: Int
        get() {
            return visibleLines.count { !it.header }
        }

    /*
    ** Get the adapter position for task
    */
    fun getPosition(task: Task): Int {
        val line = TaskLine(task = task)
        return visibleLines.indexOf(line)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        if (position == visibleLines.size) {
            return 2
        }
        val line = visibleLines[position]
        if (line.header) {
            return 0
        } else {
            return 1
        }
    }

    private fun handleEllipsis(taskText: TextView) {
        val noEllipsizeValue = "no_ellipsize"
        val ellipsizeKey = TodoApplication.app.getString(R.string.task_text_ellipsizing_pref_key)
        val ellipsizePref = Config.prefs.getString(ellipsizeKey, noEllipsizeValue)

        if (noEllipsizeValue != ellipsizePref) elipsis@ {
            val truncateAt: TextUtils.TruncateAt?
            taskText.ellipsize = when (ellipsizePref) {
                "start" -> TextUtils.TruncateAt.START
                "end" -> TextUtils.TruncateAt.END
                "middle" -> TextUtils.TruncateAt.MIDDLE
                "marquee" -> TextUtils.TruncateAt.MARQUEE
                else -> {
                    log.warn(TAG, "Unrecognized preference value for task text ellipsis: {} ! $ellipsizePref")
                    return@elipsis
                }
            }

            taskText.maxLines = 1
            taskText.setHorizontallyScrolling(true)
        }
    }
}

