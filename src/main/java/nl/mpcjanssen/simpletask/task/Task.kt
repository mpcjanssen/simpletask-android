/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)

 * LICENSE:

 * Simpletask is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Mark Janssen, Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2012- Mark Janssen
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task

import nl.mpcjanssen.simpletask.util.addInterval

import java.util.*


class Task(var text: String, defaultPrependedDate: String? = null) {
    init {
        defaultPrependedDate?.let {
            if (createDate == null) {
                createDate = defaultPrependedDate
            }
        }
    }

    constructor (text: String) : this(text, null)

    fun update(rawText: String) {
        text = rawText
    }

    var completionDate: String? = null
        get() {
            MATCH_COMPLETED.find(text)?.groupValues?.let {
                if (it.size>1) {
                    return it.get(1)
                }
            }
            return null
        }

    var createDate: String?
        get() {
            val withoutPrefix = text.replace(MATCH_COMPLETED,"")
            return MATCH_SINGLE_DATE.find(withoutPrefix)?.value ?: null
        }
        set(dateStr: String?) {

        }


    var dueDate: String?
        get() {
            return MATCH_DUE.find(text)?.value ?: null
        }
        set(dateStr: String?) {

        }

    var thresholdDate: String?
        get() {
            return MATCH_THRESHOLD.find(text)?.value ?: null
        }
        set(dateStr: String?) {

        }

    var priority: Priority
        get() = Priority.NONE
        set(prio: Priority) {

        }

    var recurrencePattern: String? = null
        get() {
            return MATCH_RECURRURENE.find(text)?.value ?: null
        }


    var tags: SortedSet<String> = emptySet<String>().toSortedSet()
        get() {
            return MATCH_TAG.findAll(text).map {it.value.substring(1)}.toSortedSet()
        }

    var lists: SortedSet<String> = emptySet<String>().toSortedSet()
        get() {
            return MATCH_LIST.findAll(text).map {it.value.substring(1)}.toSortedSet()
        }

    var links: Set<String> = emptySet()
        get() {
            return MATCH_LINK.findAll(text).map {it.value}.toSortedSet()
        }

    var phoneNumbers: Set<String> = emptySet()
        get() {
            return MATCH_PHONE_NUMBER.findAll(text).map {it.value}.toSortedSet()
        }
    var mailAddresses: Set<String> = emptySet()
        get() {
            return MATCH_MAIL.findAll(text).map {it.value}.toSortedSet()
        }


    fun removeTag(tag: String) {

    }

    fun markComplete(dateStr: String) : Task? {
        if (!this.isCompleted()) {
            val textWithoutCompletedInfo = text
            text += "x $dateStr $text"
            val pattern = recurrencePattern
            if (pattern != null) {
                var deferFromDate : String = "";
                if (!(recurrencePattern?.contains("+") ?: true)) {
                    deferFromDate = completionDate?:"";
                }
                val newTask = Task(textWithoutCompletedInfo);
                if (newTask.dueDate != null) {
                    newTask.deferDueDate(pattern, deferFromDate);

                }
                if (newTask.thresholdDate != null) {
                    newTask.deferThresholdDate(pattern, deferFromDate);
                }
                if (!createDate.isNullOrEmpty()) {
                    newTask.createDate = dateStr;
                }
                return newTask
            }
        }
        return null
    }

    fun markIncomplete() {
        text = text.replace(MATCH_COMPLETED, "")
    }

    fun deferThresholdDate(deferString:String, deferFromDate:String) {
        if (deferString.matches(MATCH_SINGLE_DATE))
        {
            thresholdDate = deferString
            return
        }
        if (deferString == "")
        {
            thresholdDate = null
            return
        }
        val olddate: String?
        if (deferFromDate.isNullOrEmpty())
        {
            olddate = thresholdDate
        }
        else
        {
            olddate = deferFromDate
        }
        val newDate = addInterval(olddate, deferString)
        thresholdDate = newDate?.format(DATE_FORMAT)
    }

    fun deferDueDate(deferString:String, deferFromDate:String) {
        if (deferString.matches(MATCH_SINGLE_DATE))
        {
            dueDate = deferString
            return
        }
        if (deferString == "")
        {
            dueDate = null
            return
        }
        val olddate: String?
        if (deferFromDate.isNullOrEmpty())
        {
            olddate = dueDate
        }
        else
        {
            olddate = deferFromDate
        }
        val newDate = addInterval(olddate, deferString)
        dueDate = newDate?.format(DATE_FORMAT)
    }


    fun inFileFormat() = text

    fun inFuture(today: String): Boolean {
        val date = thresholdDate
        date?.let {
            return date.compareTo(today) > 0
        }
        return false
    }

    fun isHidden(): Boolean {
        return text.contains(MATCH_HIDDEN)
    }

    fun isCompleted(): Boolean {
        return text.startsWith("x ")
    }

    fun showParts(parts: Int): String {
        return text
    }

    fun getHeader(sort: String, empty: String): String {
        if (sort.contains("by_context")) {
            if (lists.size > 0) {
                return lists.first();
            } else {
                return empty;
            }
        } else if (sort.contains("by_project")) {
            if (tags.size > 0) {
                return tags.first();
            } else {
                return empty;
            }
        } else if (sort.contains("by_threshold_date")) {
            return thresholdDate?:empty;
        } else if (sort.contains("by_prio")) {
            return priority.code;
        } else if (sort.contains("by_due_date")) {
            return dueDate?:empty;
        }
        return "";
    }

    /* Adds the task to list Listname
** If the task is already on that list, it does nothing
 */
    fun addList(listName : String) {
        if (!lists.contains(listName)) {
            text += " +$listName"
        }
    }

    /* Tags the task with tag
    ** If the task already has te tag, it does nothing
    */
    fun addTag(tagName : String) {
        if (!tags.contains(tagName)) {
            text += " +$tagName"
        }
    }



    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Task

        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int{
        return text.hashCode()
    }


    companion object {
        var TAG = Task::class.java.simpleName
        const val DATE_FORMAT = "YYYY-MM-DD"

        private val MATCH_COMPLETED = Regex("^x (\\d{4}-\\d{2}-\\d{2})\\s{0,1}")

        private val MATCH_LIST = Regex("@(\\S*)")
        private val MATCH_TAG = Regex("\\+(\\S*)")
        private val MATCH_HIDDEN = Regex("\\b[Hh]:([01])\\b")
        private val MATCH_DUE = Regex("\\b[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})\\b")
        private val MATCH_THRESHOLD = Regex("\\b[Tt]:(\\d{4}-\\d{2}-\\d{2})\\b")
        private val MATCH_RECURRURENE = Regex("\\b[Rr][Ee][Cc]:((\\+?)\\d+[dDwWmMyYbB])\\b")
        private val MATCH_PRIORITY = Regex("\\(([A-Z])\\)")
        private val MATCH_SINGLE_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val MATCH_PHONE_NUMBER = Regex("\\b[0\\+]?[0-9,#]{4,}")
        private val MATCH_LINK = Regex("\\b(file|http|https|todo)://[\\w\\-_./]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-@?^=%&amp;/~\\+#])?")
        private val MATCH_MAIL = Regex("\\b[a-zA-Z0-9\\+\\._%\\-]{1,256}" + "@"
                + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\."
                + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+\\b")
    }
}

