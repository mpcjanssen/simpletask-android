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

import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.addInterval
import java.util.*

class Task(text: String, defaultPrependedDate: String? = null) {

    var tokens: List<TToken>

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

    private inline fun <reified T : TToken> upsertToken(newToken: T) {

            if (getFirstToken<T>() == null) {
                tokens += newToken
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

    val text: String
        get() {
            return tokens.map { it.text }.joinToString(" ")
        }

    val extensions: List<Pair<String, String>>
        get() {
            return tokens.filter { it is KeyValueToken }.map {
                val token = it as KeyValueToken
                Pair(token.key, token.valueStr)
            }
        }

    var completionDate: String? = null
        get() = getFirstToken<CompletedDateToken>()?.value

    var uuid: String? = getFirstToken<UUIDToken>()?.valueStr

    var createDate: String?
        get() = getFirstToken<CreateDateToken>()?.value
        set(newDate) {
            val temp = ArrayList<TToken>()
            if (tokens.isNotEmpty() && (tokens.first() is CompletedToken)) {
                temp.add(tokens[0])
                tokens = tokens.drop(1)
                if (tokens.isNotEmpty() && tokens.first() is CompletedDateToken) {
                    temp.add(tokens.first())
                    tokens = tokens.drop(1)
                }
            }
            if (tokens.isNotEmpty() && tokens[0] is PriorityToken) {
                temp.add(tokens.first())
                tokens = tokens.drop(1)
            }
            if (tokens.isNotEmpty() && tokens[0] is CreateDateToken) {
                tokens = tokens.drop(1)
            }
            newDate?.let {
                temp.add(CreateDateToken(newDate))
            }
            temp.addAll(tokens)
            tokens = temp
        }

    var dueDate: String?
        get() = getFirstToken<DueDateToken>()?.valueStr
        set(dateStr) {
            if (dateStr.isNullOrEmpty()) {
                tokens = tokens.filter { it !is DueDateToken }
            } else {
                upsertToken(DueDateToken(dateStr!!))
            }
        }

    var thresholdDate: String?
        get() = getFirstToken<ThresholdDateToken>()?.valueStr
        set(dateStr) {
            if (dateStr.isNullOrEmpty()) {
                tokens = tokens.filter { it !is ThresholdDateToken }
            } else {
                upsertToken(ThresholdDateToken(dateStr!!))
            }
        }

    var priority: Priority
        get() = getFirstToken<PriorityToken>()?.value ?: Priority.NONE
        set(prio) {
            if (prio == Priority.NONE) {
                tokens = tokens.filter { it !is PriorityToken }
            } else if (tokens.any { it is PriorityToken }) {
                upsertToken(PriorityToken(prio.inFileFormat()))
            } else {
                tokens = listOf(PriorityToken(prio.inFileFormat())) + tokens
            }
        }

    var recurrencePattern: String? = null
        get() = getFirstToken<RecurrenceToken>()?.valueStr

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
            !((it is TagToken) && it.value == tag)
        }
    }

    fun removeList(list: String) {
        tokens = tokens.filter {
            !((it is ListToken) && (it.value == list))
        }
    }

    fun markComplete(dateStr: String) : Task? {
        if (!this.isCompleted()) {
            val textWithoutCompletedInfo = text
            tokens = listOf(CompletedToken(true), CompletedDateToken(dateStr)) + tokens
            val pattern = recurrencePattern
            if (pattern != null) {
                var deferFromDate : String = ""
                if (!(recurrencePattern?.contains("+") ?: true)) {
                    deferFromDate = completionDate ?: ""
                }
                val newTask = Task(textWithoutCompletedInfo)
                if (newTask.dueDate != null) {
                    newTask.deferDueDate(pattern, deferFromDate)

                }
                if (newTask.thresholdDate != null) {
                    newTask.deferThresholdDate(pattern, deferFromDate)
                }
                if (!createDate.isNullOrEmpty()) {
                    newTask.createDate = dateStr
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

    fun deferThresholdDate(deferString: String, deferFromDate: String) {
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

    fun deferDueDate(deferString: String, deferFromDate: String) {
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
            return date > today
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
        return tokens.filter { (it.type and parts) != 0 }
                      .map { it.text }
                       .joinToString(" ")
    }

    fun getHeader(sort: String, empty: String, createIsThreshold: Boolean): String {
        if (sort.contains("by_context")) {
            if (lists.size > 0) {
                return lists.first()
            } else {
                return empty
            }
        } else if (sort.contains("by_project")) {
            if (tags.size > 0) {
                return tags.first()
            } else {
                return empty
            }
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
            if (!lists.contains(it)) {
                tokens += ListToken("@" + it)
            }
        }
    }

    /* Tags the task with tag
    ** If the task already has te tag, it does nothing
    */
    fun addTag(tagName : String) {
        tagName.split(Regex("\\s+")).forEach {
            if (!tags.contains(it)) {
                tokens += TagToken("+" + it)
            }
        }
    }

    companion object {
        var TAG = "Task"
        const val DATE_FORMAT = "YYYY-MM-DD"
        private val MATCH_LIST = Regex("@(\\S+)")
        private val MATCH_TAG = Regex("\\+(\\S+)")
        private val MATCH_HIDDEN = Regex("[Hh]:([01])")
        private val MATCH_UUID = Regex("[Uu][Uu][Ii][Dd]:([A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12})")
        private val MATCH_DUE = Regex("[Dd][Uu][Ee](\\d{4}-\\d{2}-\\d{2})")
        private val MATCH_THRESHOLD = Regex("[Tt]:(\\d{4}-\\d{2}-\\d{2})")
        private val MATCH_RECURRURENE = Regex("[Rr][Ee][Cc]:((\\+?)\\d+[dDwWmMyYbB])")
        private val MATCH_EXT = Regex("(.+):(.+)")
        private val MATCH_PRIORITY = Regex("\\(([A-Z])\\)")
        private val MATCH_SINGLE_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val MATCH_PHONE_NUMBER = Regex("[0+]?[0-9,#]{4,}")
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
                    tokens.add(DueDateToken(it.groupValues[1]))
                    return@forEach
                }
                MATCH_THRESHOLD.matchEntire(lexeme)?.let {
                    tokens.add(ThresholdDateToken(it.groupValues[1]))
                    return@forEach
                }
                MATCH_HIDDEN.matchEntire(lexeme)?.let {
                    tokens.add(HiddenToken(it.groupValues[1]))
                    return@forEach
                }
                MATCH_UUID.matchEntire(lexeme)?.let {
                    tokens.add(UUIDToken(it.groupValues[1]))
                    return@forEach
                }
                MATCH_RECURRURENE.matchEntire(lexeme)?.let {
                    tokens.add(RecurrenceToken(it.groupValues[1]))
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
                MATCH_EXT.matchEntire(lexeme)?.let {
                    tokens.add(ExtToken(it.groupValues[1], it.groupValues[2]))
                    return@forEach
                }
                if (lexeme.isBlank()) {
                    tokens.add(WhiteSpaceToken(lexeme))
                } else {
                    tokens.add(TextToken(lexeme))
                }

            }
            if (Config.useUUIDs && tokens.filterIsInstance<UUIDToken>().isEmpty()) {
                tokens.add(UUIDToken(UUID.randomUUID().toString()))
            }
            return tokens
        }
    }
}

interface TToken {
    val text: String
    val value: Any?
    val type: Int
    companion object {
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
        const val EXTENSION = 1 shl 15
        const val TUUID = 1 shl 16
        const val ALL = 0b1111111111111111
    }
}

data class CompletedToken(override val value: Boolean) : TToken {
    override val text: String
    get() = if (value) "x" else ""
    override val type = TToken.COMPLETED
}

data class PriorityToken(override val text: String) : TToken {
    override val value: Priority
    get() = Priority.toPriority(text.removeSurrounding("(", ")"))
    override val type = TToken.PRIO
}

data class ListToken(override val text: String) : TToken {
    override val value: String
        get() = text.substring(1)
    override val type = TToken.LIST
}

data class TagToken(override val text: String) : TToken {
    override val value: String
        get() = text.substring(1)
    override val type = TToken.TTAG
}

// Tokens with the same value as text representation
interface StringValueToken : TToken {
    override val value: String
        get () = text
}

data class CreateDateToken(override val text: String) : StringValueToken {
    override val type = TToken.CREATION_DATE
}
data class CompletedDateToken(override val text: String) : StringValueToken {
    override val type = TToken.COMPLETED_DATE
}

data class TextToken(override val text: String) : StringValueToken {
    override val type = TToken.TEXT
}
data class WhiteSpaceToken(override val text: String) : StringValueToken {
    override val type = TToken.WHITE_SPACE
}
data class MailToken(override val text: String) : StringValueToken {
    override val type = TToken.MAIL
}
data class LinkToken(override val text: String) : StringValueToken {
    override val type = TToken.LINK
}
data class PhoneToken(override val text: String) : StringValueToken {
    override val type = TToken.PHONE
}


// Key Value tokens
interface KeyValueToken : TToken {
    val key : String
    val valueStr : String
    override val value: Any?
    get() = valueStr
    override val text: String
        get() = "$key:$valueStr"
}

data class DueDateToken(override val valueStr : String) : KeyValueToken {
    override val key = "due"
    override val type = TToken.DUE_DATE
}
data class ThresholdDateToken(override val valueStr : String) : KeyValueToken {
    override val key = "t"
    override val type = TToken.THRESHOLD_DATE
}

data class RecurrenceToken(override val valueStr : String) : KeyValueToken {
    override val key = "rec"
    override val type = TToken.RECURRENCE
}

data class HiddenToken(override val valueStr : String) : KeyValueToken {
    override val key = "h"
    override val value: Boolean
        get() = valueStr == "1"
    override val type = TToken.HIDDEN
}

data class ExtToken(override val key : String, override val valueStr: String) : KeyValueToken {
    override val type = TToken.EXTENSION
}

data class UUIDToken(override val valueStr: String) : KeyValueToken {
    override val key = "uuid"
    override val type = TToken.TUUID
}

// Extension functions
fun String.lex(): List<String> = this.split(" ")

