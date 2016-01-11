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


import android.support.annotation.NonNull
import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.ActiveFilter
import nl.mpcjanssen.simpletask.Constants
import nl.mpcjanssen.simpletask.task.token.Token
import nl.mpcjanssen.simpletask.util.addInterval

import java.util.*


class Task(text: String, defaultPrependedDate: String? = null) {

    public var tokens: List<TToken>

    init {
        tokens = parse(text)
        defaultPrependedDate?.let {
            if (createDate == null) {
                createDate = defaultPrependedDate
            }
        }
    }

    constructor (text: String) : this(text, null)

    fun update(rawText: String) {
        tokens = parse(rawText)
    }

    private inline fun <reified T> getFirstToken(): T? {
        tokens.filterIsInstance<T>().forEach {
            return it
        }
        return null
    }

    private inline fun <reified T : TToken> upsertToken(newToken: T?) {
        if (newToken == null) {
            tokens = tokens.filter {
                if (it is T) {
                    false
                } else {
                    true
                }
            }
        } else {
            if (getFirstToken<T>() == null) {
                tokens = tokens + newToken
            } else {
                tokens = tokens.map {
                    if (it is T) {
                        newToken
                    } else {
                        it
                    }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Task

        if (tokens != other.tokens) return false

        return true
    }

    override fun hashCode(): Int {
        return tokens.hashCode()
    }

    val text: String
        get() {
            return tokens.map { it.text }.joinToString(" ")
        }

    var completionDate: String? = null
        get() = getFirstToken<CompletedDateToken>()?.value ?: null

    var createDate: String?
        get() = getFirstToken<CreateDateToken>()?.value ?: null
        set(newDate: String?) {
            val temp = ArrayList<TToken>()
            if (tokens.size > 0 && (tokens.first() is CompletedToken)) {
                temp.add(tokens.get(0))
                tokens.drop(1)
                if (tokens.size > 0 && tokens.first() is CompletedDateToken) {
                    temp.add(tokens.first())
                    tokens.drop(1)
                }
            }
            if (tokens.size > 0 && tokens.get(0) is PriorityToken) {
                temp.add(tokens.first())
                tokens.drop(1)
            }
            if (tokens.size > 0 && tokens.get(0) is CreateDateToken) {
                tokens.drop(1)
            }
            newDate?.let {
                temp.add(CreateDateToken(newDate))
            }
            temp.addAll(tokens)
            tokens = temp
        }

    var dueDate: String? = null
        get() = getFirstToken<DueDateToken>()?.value ?: null

    var thresholdDate: String?
        get() = getFirstToken<ThresholdDateToken>()?.value ?: null
        set(dateStr: String?) {
            if (dateStr == null) {
                upsertToken<ThresholdDateToken>(null)
            } else {
                upsertToken(ThresholdDateToken("t:${dateStr}"))
            }
        }

    var priority: Priority = Priority.NONE
        get() = getFirstToken<PriorityToken>()?.value ?: Priority.NONE

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
            return tokens.filter { it is LinkToken }.map { it -> (it as LinkToken).value }.toSortedSet()
        }

    var phoneNumbers: Set<String> = emptySet()
        get() {
            return tokens.filter { it is PhoneToken }.map { it -> (it as PhoneToken).value }.toSortedSet()
        }
    var mailAddresses: Set<String> = emptySet()
        get() {
            return tokens.filter { it is MailToken }.map { it -> (it as MailToken).value }.toSortedSet()
        }


    public fun removeTag(tag: String) {
        tokens = tokens.filter {
            if (it is TagToken && it.value == tag) false else true
        }
    }

    public fun markComplete(dateStr: String) : Task? {
        if (!this.isCompleted()) {
            val textWithoutCompletedInfo = text
            tokens = listOf(CompletedToken(true), CompletedDateToken(dateStr)) + tokens
            val pattern = recurrencePattern
            if (pattern != null) {
                var deferFromDate : String = "";
                if (recurrencePattern?.contains("+") ?: false) {
                    deferFromDate = completionDate?:"";
                }
                val newTask = Task(textWithoutCompletedInfo);
                if (newTask.dueDate == null && newTask.dueDate == null) {
                    newTask.deferDueDate(pattern, deferFromDate);
                } else {
                    if (newTask.dueDate != null) {
                        newTask.deferDueDate(pattern, deferFromDate);
                    }
                    if (newTask.thresholdDate != null) {
                        newTask.deferThresholdDate(pattern, deferFromDate);
                    }
                }
                if (!createDate.isNullOrEmpty()) {
                    newTask.createDate = dateStr;
                }
                return newTask
            }
        }
        return null
    }

    public fun markIncomplete() {
        tokens = tokens.filter {
             when (it) {
                is CompletedDateToken -> false
                is CompletedToken -> false
                else -> true
            }
        }
    }

    fun deferThresholdDate(deferString:String, deferFromDate:String) {
        if (DateTime.isParseable(deferString))
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
        val newDate = addInterval(olddate?.toDateTime(), deferString)
        thresholdDate = newDate?.format(Constants.DATE_FORMAT)
    }

    fun deferDueDate(deferString:String, deferFromDate:String) {
        if (DateTime.isParseable(deferString))
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
        val newDate = addInterval(olddate?.toDateTime(), deferString)
        dueDate = newDate?.format(Constants.DATE_FORMAT)
    }


    public fun inFileFormat() = text

    public fun inFuture(): Boolean {
        val date = thresholdDate
        date?.let {
            return date.compareTo(DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT)) > 0
        }
        return false
    }

    public fun isHidden(): Boolean {
        return getFirstToken<HiddenToken>()?.value ?: false
    }

    public fun isVisible(): Boolean {
        return !isHidden()
    }

    public fun isCompleted(): Boolean {
        return getFirstToken<CompletedToken>() != null
    }

    fun showParts(parts: Int): String? {
        return tokens.filter {
            it.type and parts == 1
        }.joinToString("")
    }

    public fun getHeader(sort: String, empty: String): String {
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
            return priority.getCode();
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
            tokens += ListToken(listName);
        }
    }

    /* Tags the task with tag
    ** If the task already has te tag, it does nothing
    */
    fun addTag(tagName : String) {
        if (!tags.contains(tagName)) {
            tokens += ListToken(tagName);
        }
    }



    fun initWithFilter(mFilter : ActiveFilter) {
        if (!mFilter.getContextsNot() && mFilter.getContexts().size()==1) {
            addList(mFilter.getContexts().get(0));
        }

        if (!mFilter.getProjectsNot() && mFilter.getProjects().size()==1) {
            addTag(mFilter.getProjects().get(0));
        }
    }


    companion object {
        val DUE_DATE = 1
        val THRESHOLD_DATE = 2
        var TAG = this.javaClass.simpleName
        private val MATCH_LIST = Regex("@(\\S*)")
        private val MATCH_TAG = Regex("\\+(\\S*)")
        private val MATCH_HIDDEN = Regex("[Hh]:([01])")
        private val MATCH_DUE = Regex("[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})")
        private val MATCH_THRESHOLD = Regex("[Tt]:(\\d{4}-\\d{2}-\\d{2})")
        private val MATCH_RECURRURENE = Regex("[Rr][Ee][Cc]:((\\+?)\\d+[dDwWmMyY])")
        private val MATCH_PRIORITY = Regex("\\(([A-Z])\\)")
        private val MATCH_SINGLE_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val MATCH_PHONE_NUMBER = Regex("[0\\+]?[0-9,#]{4,}")
        private val MATCH_LINK = Regex("(http|https|todo)://[\\w\\-_./]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-@?^=%&amp;/~\\+#])?")
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
                lexemes.drop(1)
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
                MATCH_LINK.matchEntire(lexeme)?.let {
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


abstract interface TToken {
    val text: String
    val value: Any?
    val type: Int
    companion object {
        val WHITE_SPACE = 1
        val LIST = 1 shl 1
        val TTAG = 1 shl 2
        val COMPLETED = 1 shl 3
        val COMPLETED_DATE = 1 shl 4
        val CREATION_DATE = 1 shl 5
        val TEXT = 1 shl 6
        val PRIO = 1 shl 7
        val THRESHOLD_DATE = 1 shl 8
        val DUE_DATE = 1 shl 9
        val HIDDEN = 1 shl 10
        val RECURRENCE = 1 shl 11
        val PHONE = 1 shl 12
        val LINK = 1 shl 13
        val MAIL = 1 shl 14
    }
}

data class CompletedToken(override val value: Boolean) :TToken {
    override val text: String
    get() = if (value) "x" else ""
    override val type = TToken.COMPLETED;
}

data class HiddenToken(override val text: String) :TToken {
    override val value: Boolean
    get() = if (text == "h:1") true else false
    override val type = TToken.HIDDEN;
}
data class PriorityToken(override val text: String) :TToken {
    override val value: Priority
    get() = Priority.valueOf(text)
    override val type = TToken.PRIO;
}

data class ListToken(override val text: String) : TToken {
    override val value: String
        get() = text.substring(1)
    override val type = TToken.LIST;
}

data class TagToken(override val text: String) : TToken {
    override val value: String
        get() = text.substring(1)
    override val type = TToken.TTAG;
}

// The value of this token is the val part in key:val
// If there is no key: then val is returned as is.
interface KeyValueToken : TToken {
    override val value: String
    get() = text.split(":").last()
}
data class CreateDateToken(override val text: String) : KeyValueToken {
    override val type = TToken.CREATION_DATE;
}
data class CompletedDateToken(override val text: String) : KeyValueToken {
    override val type = TToken.COMPLETED_DATE;
}
data class DueDateToken(override val text: String) : KeyValueToken {
    override val type = TToken.DUE_DATE;
}
data class ThresholdDateToken(override val text: String) : KeyValueToken {
    override val type = TToken.THRESHOLD_DATE;
}
data class TextToken(override val text: String) : KeyValueToken {
    override val type = TToken.TEXT;
}
data class WhiteSpaceToken(override val text: String) : KeyValueToken {
    override val type = TToken.WHITE_SPACE;
}
data class MailToken(override val text: String) : KeyValueToken {
    override val type = TToken.MAIL;
}
data class LinkToken(override val text: String) : KeyValueToken {
    override val type = TToken.LINK;
}
data class PhoneToken(override val text: String) : KeyValueToken {
    override val type = TToken.PHONE;
}
data class RecurrenceToken(override val text: String) : KeyValueToken {
    override val type = TToken.RECURRENCE;
}

// Extension functions

fun String.lex(): List<String> = this.split(" ")

fun String.toDateTime(): DateTime? {
    var date: DateTime?;
    if ( DateTime.isParseable(this)) {
        date = DateTime(this)
    } else {
        date = null
    }
    return date
}