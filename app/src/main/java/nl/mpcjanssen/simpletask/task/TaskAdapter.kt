package nl.mpcjanssen.simpletask.task

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.TextView
import kotlinx.android.synthetic.main.list_header.view.*
import kotlinx.android.synthetic.main.list_item.view.*
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.*
import java.io.File
import java.util.ArrayList

class TaskViewHolder(itemView: View, val viewType : Int) : RecyclerView.ViewHolder(itemView)

class TaskAdapter(var query: Query?, private val m_inflater: LayoutInflater) : RecyclerView.Adapter <TaskViewHolder>() {
    val TAG = "TaskAdapter"
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
                is ListToken -> !(query?.hideLists?:false)
                is TagToken -> !(query?.hideTags?:false)
                else -> true
            }
        }
        val txt = LuaInterpreter.onDisplayCallback(activeQuery.luaModule, task) ?: task.showParts(tokensToShowFilter)
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
            Priority.A -> priorityColor = ContextCompat.getColor(m_app, R.color.simple_red_dark)
            Priority.B -> priorityColor = ContextCompat.getColor(m_app, R.color.simple_orange_dark)
            Priority.C -> priorityColor = ContextCompat.getColor(m_app, R.color.simple_green_dark)
            Priority.D -> priorityColor = ContextCompat.getColor(m_app, R.color.simple_blue_dark)
            else -> priorityColor = ContextCompat.getColor(m_app, R.color.gray67)
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
            cb.setOnClickListener({
                uncompleteTasks(item)
                // Update the tri state checkbox
                if (activeMode() == Simpletask.Mode.SELECTION) invalidateOptionsMenu()
                TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
            })
        } else {
            taskText.paintFlags = taskText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            taskAge.paintFlags = taskAge.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            cb.setOnClickListener {
                completeTasks(item)
                // Update the tri state checkbox
                if (activeMode() == Simpletask.Mode.SELECTION) invalidateOptionsMenu()
                TodoList.notifyTasklistChanged(Config.todoFileName, m_app, false)
            }

        }
        cb.isChecked = completed

        val relAge = getRelativeAge(task, m_app)
        val relDue = getRelativeDueDate(task, m_app)
        val relativeThresholdDate = getRelativeThresholdDate(task, m_app)
        if (!isEmptyOrNull(relAge) && !activeQuery.hideCreateDate) {
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
        view.setOnClickListener { it ->

            val newSelectedState = !TodoList.isSelected(item)
            if (newSelectedState) {
                TodoList.selectTask(item)
            } else {
                TodoList.unSelectTask(item)
            }
            it.isActivated = newSelectedState
            invalidateOptionsMenu()
        }

        view.setOnLongClickListener {
            val links = ArrayList<String>()
            val actions = ArrayList<String>()
            val t = item
            for (link in t.links) {
                actions.add(Simpletask.ACTION_LINK)
                links.add(link)
            }
            for (number in t.phoneNumbers) {
                actions.add(Simpletask.ACTION_PHONE)
                links.add(number)
                actions.add(Simpletask.ACTION_SMS)
                links.add(number)
            }
            for (mail in t.mailAddresses) {
                actions.add(Simpletask.ACTION_MAIL)
                links.add(mail)
            }
            if (actions.size != 0) {

                val titles = ArrayList<String>()
                for (i in links.indices) {
                    when (actions[i]) {
                        Simpletask.ACTION_SMS -> titles.add(i, getString(R.string.action_pop_up_sms) + links[i])
                        Simpletask.ACTION_PHONE -> titles.add(i, getString(R.string.action_pop_up_call) + links[i])
                        else -> titles.add(i, links[i])
                    }
                }
                val build = AlertDialog.Builder(this@Simpletask)
                build.setTitle(R.string.task_action)
                val titleArray = titles.toArray<String>(arrayOfNulls<String>(titles.size))
                build.setItems(titleArray) { _, which ->
                    val actionIntent: Intent
                    val url = links[which]
                    log.info(Simpletask.TAG, "" + actions[which] + ": " + url)
                    when (actions[which]) {
                        Simpletask.ACTION_LINK -> if (url.startsWith("todo://")) {
                            val todoFolder = Config.todoFile.parentFile
                            val newName = File(todoFolder, url.substring(7))
                            m_app.switchTodoFile(newName.absolutePath)
                        } else if (url.startsWith("root://")) {
                            val rootFolder = Config.localFileRoot
                            val file = File(rootFolder, url.substring(7))
                            actionIntent = Intent(Intent.ACTION_VIEW)
                            val contentUri = Uri.fromFile(file)
                            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                            actionIntent.setDataAndType(contentUri, mime)
                            actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(actionIntent)
                        } else {
                            try {
                                actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(actionIntent)
                            } catch (e: ActivityNotFoundException) {
                                log.info(Simpletask.TAG, "No handler for task action $url")
                                showToastLong(TodoApplication.app, "No handler for $url" )
                            }
                        }
                        Simpletask.ACTION_PHONE -> {
                            actionIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(url)))
                            startActivity(actionIntent)
                        }
                        Simpletask.ACTION_SMS -> {
                            actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + Uri.encode(url)))
                            startActivity(actionIntent)
                        }
                        Simpletask.ACTION_MAIL -> {
                            actionIntent = Intent(Intent.ACTION_SEND, Uri.parse(url))
                            actionIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                                    arrayOf(url))
                            actionIntent.type = "text/plain"
                            startActivity(actionIntent)
                        }
                    }
                }
                build.create().show()
            }
            true
        }
    }
    internal var visibleLines = ArrayList<VisibleLine>()

    internal fun setFilteredTasks(caller: Simpletask?, newQuery: Query) {
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
                caller?.showListViewProgress(false)
                if (Config.lastScrollPosition != -1) {
                    val manager = listView?.layoutManager as LinearLayoutManager?
                    val position = Config.lastScrollPosition
                    val offset = Config.lastScrollOffset
                    Logger.info(Simpletask.TAG, "Restoring scroll offset $position, $offset")
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

