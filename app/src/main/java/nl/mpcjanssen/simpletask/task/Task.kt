package nl.mpcjanssen.simpletask.task

import nl.mpcjanssen.simpletask.util.addInterval
import java.util.*
import java.util.regex.Pattern

class Task(var text: String, defaultPrependedDate: String? = null) {
    var txtVersion: Long
    var selected: Boolean = false

    init {
        defaultPrependedDate?.let {
            if (createDate == null) {
                createDate = defaultPrependedDate
            }
        }
        txtVersion = 0
    }

    constructor (text: String) : this(text, null)

    fun update(rawText: String) {
        text = rawText
        txtVersion++
    }

    // TODO
    val extensions: List<Pair<String, String>>
        get() {
            return emptyList()
        }


    // TODO
    var completionDate: String? = null

    // TODO
    var uuid: String? = null

    // TODO
    var createDate: String?
        get() = null
        set(newDate) {
        }

    // TODO
    var dueDate: String?
        get() = null
        set(dateStr) {

        }

    // TODO
    var thresholdDate: String?
        get() = null
        set(dateStr) {

        }

    // TODO
    var priority: Priority
        get() = Priority.NONE
        set(prio) {

        }

    var recurrencePattern: String? = null
        get() = null

    // TODO
    val tags : SortedSet<String>?
    get() {
        return null
    }

    // TODO
    val lists : SortedSet<String>?
    get() {
        return null
    }
        // TODO
    val links : SortedSet<String>?
    get() {
        return null
    }

        // TODO
    val mailAddresses : SortedSet<String>?
    get() {
        return null
    }


    // TODO
    val phoneNumbers: Set<String>?
        get() {
            return null
        }



    // TODO
    fun removeTag(tag: String) {

    }

    // TODO
    fun removeList(list: String) {

    }

    // TODO
    fun markComplete(dateStr: String): Task? {

        return null
    }

    // TODO
    fun markIncomplete() {

    }

    // TODO
    fun deferThresholdDate(deferString: String, deferFromDate: String) {

    }

    // TODO
    fun deferDueDate(deferString: String, deferFromDate: String) {

    }

    fun inFileFormat() = text

    fun inFuture(today: String): Boolean {
        val date = thresholdDate
        date?.let {
            return date > today
        }
        return false
    }

    // TODO
    fun isHidden(): Boolean {
        return  false
    }


    fun isCompleted(): Boolean {
        return text.startsWith("x ")
    }

    // TODO
    fun showParts(showText: Boolean = true): String {
        return text
    }

    fun getHeader(sort: String, empty: String, createIsThreshold: Boolean): String {
        if (sort.contains("by_context")) {
            lists?.apply {
                if (size > 0) {
                    return first()
                }
            }
            return empty

        } else if (sort.contains("by_project")) {
            tags?.apply {
                if (size > 0) {
                    return first()
                }
            }
            return empty

        } else if (sort.contains("by_threshold_date")) {
            if (createIsThreshold) {
                return thresholdDate ?: createDate ?: empty
            } else {
                return thresholdDate ?: empty
            }
        } else if (sort.contains("by_prio")) {
            return priority.code
        } else if (sort.contains("by_due_date")) {
            return dueDate ?: empty
        }
        return ""
    }

    /* Adds the task to list Listname
    ** If the task is already on that list, it does nothing
    */
    fun addList(listName: String) {
        listName.split(Regex("\\s+")).forEach {
            if (lists?.contains(it) != true) {
                text += " @$it"
            }
        }
    }

    /* Tags the task with tag
    ** If the task already has te tag, it does nothing
    */
    fun addTag(tagName: String) {
        tagName.split(Regex("\\s+")).forEach {
            if (tags?.contains(it) != true) {
                text += " +$it"
            }
        }
    }

    companion object {
        var TAG = "Task"
        const val DATE_FORMAT = "YYYY-MM-DD"
        private val MATCH_TEXT = Pattern.compile("(\\S+)").matcher("")
        private val MATCH_LIST = Pattern.compile("@(\\S+)").matcher("")
        private val MATCH_TAG = Pattern.compile("\\+(\\S+)").matcher("")
        private val MATCH_HIDDEN = Pattern.compile("[Hh]:([01])").matcher("")
        private val MATCH_UUID = Pattern
                .compile("[Uu][Uu][Ii][Dd]:([A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12})")
                .matcher("")
        private val MATCH_DUE = Pattern.compile("[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})").matcher("")
        private val MATCH_THRESHOLD = Pattern.compile("[Tt]:(\\d{4}-\\d{2}-\\d{2})").matcher("")
        private val MATCH_RECURRENCE = Pattern.compile("[Rr][Ee][Cc]:((\\+?)\\d+[dDwWmMyYbB])").matcher("")
        private val MATCH_EXT = Pattern.compile("(.+):(.+)").matcher("")
        private val MATCH_PRIORITY = Regex("\\(([A-Z])\\)")
        private val MATCH_SINGLE_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}").matcher("")
        private val MATCH_PHONE_NUMBER = Pattern.compile("[+]?[0-9,#()-]{4,}").matcher("")
        private val MATCH_URI = Pattern.compile("[a-z]+://(\\S+)").matcher("")
        private val MATCH_MAIL = Pattern.compile("[a-zA-Z0-9\\+\\._%\\-]{1,256}" + "@"
                + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\."
                + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+").matcher("")

    }
}

