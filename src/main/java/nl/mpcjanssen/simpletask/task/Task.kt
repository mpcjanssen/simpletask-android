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

import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.util.addInterval
import nl.mpcjanssen.simpletask.util.toJSON
import org.json.JSONArray
import org.json.JSONObject

import java.util.*


class Task(val tokens: ArrayList<TToken>) {

    init {
        // Next line is only meant for debugging. When uncommented it will show
        // full task contents in the log.
        //Logger.debug("Task", "Task initialized JSON: ${toJSON()}")
    }

    constructor (text: String, defaultPrependedDate: String?) : this(parse(text)) {
        defaultPrependedDate?.let {
            if (createDate == null) {
                Logger.debug("Task", "Updated create date of task to $createDate")
                createDate = defaultPrependedDate

            }
        }
    }
    constructor (text: String) : this(text, null)

    fun update(rawText: String) {
        tokens.clear()
        tokens.addAll(parse(rawText))
    }

    private  fun getFirstToken(type: Int): TToken? {
        return tokens.first { it.type == type }
    }

    private fun upsertToken(newToken: TToken) {
        val idx  = tokens.indexOfFirst {  it.type == newToken.type }
        if (idx==-1) {
            tokens.add(newToken)
        } else {
            tokens[idx] = newToken
        }
    }



    val text: String
        get() {
            return tokens.map { it.text }.joinToString(" ")
        }

    var completionDate: String? = null
        get() = getFirstToken(COMPLETED_DATE)?.value ?: null

    var createDate: String?
        get() = getFirstToken(CREATION_DATE)?.value ?: null
        set(newDate: String?) {
            val temp = ArrayList<TToken>()
            if (tokens.size > 0 && (tokens.first().type == COMPLETED)) {
                temp.add(tokens[0])
                tokens.drop(1)
                if (tokens.size > 0 && tokens.first().type == COMPLETED_DATE) {
                    temp.add(tokens.first())
                    tokens.drop(1)
                }
            }
            if (tokens.size > 0 && tokens[0].type == PRIO) {
                temp.add(tokens.first())
                tokens.drop(1)
            }
            if (tokens.size > 0 && tokens[0].type == CREATION_DATE) {
                tokens.drop(1)
            }
            newDate?.let {
                temp.add(TToken(CREATION_DATE,newDate,newDate))
            }
        }

    var dueDate: String?
        get() = getFirstToken<DueDateToken>()?.value ?: null
        set(dateStr: String?) {
            if (dateStr.isNullOrEmpty()) {
                tokens = tokens.filter { it !is DueDateToken }
            } else {
                upsertToken(DueDateToken("due:$dateStr"))
            }
        }

    var thresholdDate: String?
        get() = getFirstToken<ThresholdDateToken>()?.value ?: null
        set(dateStr: String?) {
            if (dateStr.isNullOrEmpty()) {
                tokens = tokens.filter { it !is ThresholdDateToken }
            } else {
                upsertToken(ThresholdDateToken("t:$dateStr"))
            }
        }

    var priority: Priority
        get() = getFirstToken<PriorityToken>()?.value ?: Priority.NONE
        set(prio: Priority) {
            if (prio == Priority.NONE) {
                tokens = tokens.filter { if (it is PriorityToken) false else true }
            } else if (tokens.any { it is PriorityToken }) {
                upsertToken(PriorityToken(prio.inFileFormat()))
            } else {
                tokens = listOf(PriorityToken(prio.inFileFormat())) + tokens
            }
        }

    var recurrencePattern: String? = null
        get() = getFirstToken<RecurrenceToken>()?.value


    var tags: SortedSet<String> = emptySet<String>().toSortedSet()
        get() {
            return tokens.filter { it is TagToken }.map { it -> (it as TagToken).value }.toSortedSet()
        }

    var lists: SortedSet<String> = emptySet<String>().toSortedSet()
        get() {
            return tokens.filter { it is ListToken }.map { it -> (it as ListToken).value }.toSortedSet()
        }

    var links: Set<String> = emptySet()
        get() {
            return tokens.filter { it is LinkToken }.map { it -> (it as LinkToken).text }.toSortedSet()
        }

    var phoneNumbers: Set<String> = emptySet()
        get() {
            return tokens.filter { it is PhoneToken }.map { it -> (it as PhoneToken).text }.toSortedSet()
        }
    var mailAddresses: Set<String> = emptySet()
        get() {
            return tokens.filter { it is MailToken }.map { it -> (it as MailToken).text }.toSortedSet()
        }


    fun removeTag(tag: String) {
        tokens = tokens.filter {
            if ((it is TagToken) && it.value == tag) false else true
        }
    }

    fun removeList(list: String) {
        tokens = tokens.filter {
            if ((it is ListToken) && (it.value == list)) false else true
        }
    }

    fun markComplete(dateStr: String) : Task? {
        if (!this.isCompleted()) {
            val textWithoutCompletedInfo = text
            tokens = listOf(CompletedToken(true), CompletedDateToken(dateStr)) + tokens
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
        tokens = tokens.filter {
             when (it) {
                is CompletedDateToken -> false
                is CompletedToken -> false
                else -> true
            }
        }
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
        return getFirstToken<HiddenToken>()?.value ?: false
    }

    fun isCompleted(): Boolean {
        return getFirstToken<CompletedToken>() != null
    }

    fun showParts(parts: Int): String {
        return tokens.filter {
            (it.type and parts) != 0
        }.map {it.text}.joinToString(" ")
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
            tokens += ListToken("@"+listName);
        }
    }

    /* Tags the task with tag
    ** If the task already has te tag, it does nothing
    */
    fun addTag(tagName : String) {
        if (!tags.contains(tagName)) {
            tokens += TagToken("+"+tagName);
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
        private val MATCH_LIST = Regex("@(\\S+)")
        private val MATCH_TAG = Regex("\\+(\\S+)")
        private val MATCH_HIDDEN = Regex("[Hh]:([01])")
        private val MATCH_DUE = Regex("[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})")
        private val MATCH_THRESHOLD = Regex("[Tt]:(\\d{4}-\\d{2}-\\d{2})")
        private val MATCH_RECURRURENE = Regex("[Rr][Ee][Cc]:((\\+?)\\d+[dDwWmMyYbB])")
        private val MATCH_PRIORITY = Regex("\\(([A-Z])\\)")
        private val MATCH_SINGLE_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val MATCH_PHONE_NUMBER = Regex("[0\\+]?[0-9,#]{4,}")
        private val MATCH_URI = Regex("[a-z]+://(\\S+)")
        private val MATCH_MAIL = Regex("[a-zA-Z0-9\\+\\._%\\-]{1,256}" + "@"
                + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\."
                + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+")

        fun parse(text: String): ArrayList<TToken> {
            var lexemes = text.lex()
            val tokens = ArrayList<TToken>()

            if (lexemes.take(1) == listOf("x")) {
                tokens.add(CompletedToken(true))
                lexemes = lexemes.drop(1)
                var nextToken = lexemes.getOrElse(0, { "" })
                MATCH_SINGLE_DATE.matchEntire(nextToken)?.let {
                    tokens.add(CompletedDateToken(lexemes.first()))
                    lexemes = lexemes.drop(1)
                    nextToken = lexemes.getOrElse(0, { "" })
                    MATCH_SINGLE_DATE.matchEntire(nextToken)?.let {
                        tokens.add(CreateDateToken(lexemes.first()))
                        lexemes = lexemes.drop(1)
                    }
                }
            }

            var nextToken = lexemes.getOrElse(0, { "" })
            MATCH_PRIORITY.matchEntire(nextToken)?.let {
                tokens.add(PriorityToken(nextToken))
                lexemes = lexemes.drop(1)
            }

            nextToken = lexemes.getOrElse(0, { "" })
            MATCH_SINGLE_DATE.matchEntire(nextToken)?.let {
                tokens.add(CreateDateToken(lexemes.first()))
                lexemes = lexemes.drop(1)
            }


            lexemes.forEach { lexeme ->
                MATCH_LIST.matchEntire(lexeme)?.let {
                    tokens.add(ListToken(lexeme))
                    return@forEach
                }
                MATCH_TAG.matchEntire(lexeme)?.let {
                    tokens.add(TagToken(lexeme))
                    return@forEach
                }
                MATCH_DUE.matchEntire(lexeme)?.let {
                    tokens.add(DueDateToken(lexeme))
                    return@forEach
                }
                MATCH_THRESHOLD.matchEntire(lexeme)?.let {
                    tokens.add(ThresholdDateToken(lexeme))
                    return@forEach
                }
                MATCH_HIDDEN.matchEntire(lexeme)?.let {
                    tokens.add(HiddenToken(lexeme))
                    return@forEach
                }
                MATCH_RECURRURENE.matchEntire(lexeme)?.let {
                    tokens.add(RecurrenceToken(lexeme))
                    return@forEach
                }
                MATCH_PHONE_NUMBER.matchEntire(lexeme)?.let {
                    tokens.add(PhoneToken(lexeme))
                    return@forEach
                }
                MATCH_URI.matchEntire(lexeme)?.let {
                    tokens.add(LinkToken(lexeme))
                    return@forEach
                }
                MATCH_MAIL.matchEntire(lexeme)?.let {
                    tokens.add(MailToken(lexeme))
                    return@forEach
                }
                if (lexeme.isBlank()) {
                    tokens.add(WhiteSpaceToken(lexeme))
                } else {
                    tokens.add(TextToken(lexeme))
                }

            }
            return tokens
        }
    }
}

class TToken : JSONObject {
    constructor(type: Int, text : String, value: String ) : super() {
        put("type", type)
        put("text", text)
        put("value", value)
    }
    val type : Int
        get() {
            return this.optInt("type")
        }
    val text : String
        get() {
            return this.optString("text")
        }
    val value : String
        get() {
            return this.optString("value")
        }

    fun typeAsString () : String {
        return when(type) {
            WHITE_SPACE -> "white_space"
            LIST -> "list"
            TTAG -> "tag"
            COMPLETED -> "completed"
            COMPLETED_DATE -> "completed_date"
            CREATION_DATE -> "creation_date"
            TEXT -> "text"
            PRIO -> "prio"
            THRESHOLD_DATE -> "threshold_date"
            DUE_DATE -> "due_date"
            HIDDEN -> "hidden"
            RECURRENCE -> "recurrence"
            PHONE -> "phone"
            LINK -> "link"
            MAIL -> "mail"
            else -> "unknown"
        }
    }

}

// Extension functions

fun String.lex(): List<String> = this.split(" ")

const val WHITE_SPACE = 1
const val LIST = 1 shl 1
const val TTAG = 1 shl 2
const val COMPLETED = 1 shl 3
const val COMPLETED_DATE = 1 shl 4
const val CREATION_DATE = 1 shl 5
const val TEXT = 1 shl 6
const val PRIO = 1 shl 7
const val THRESHOLD_DATE = 1 shl 8
const val DUE_DATE = 1 shl 9
const val HIDDEN = 1 shl 10
const val RECURRENCE = 1 shl 11
const val PHONE = 1 shl 12
const val LINK = 1 shl 13
const val MAIL = 1 shl 14
const val ALL = 0b111111111111111