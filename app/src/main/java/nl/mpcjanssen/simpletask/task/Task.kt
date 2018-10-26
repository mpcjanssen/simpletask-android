package nl.mpcjanssen.simpletask.task

import nl.mpcjanssen.simpletask.util.addInterval
import java.util.*
import java.util.regex.Pattern

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
                upsertToken(PriorityToken(prio.fileFormat))
            } else {
                tokens = listOf(PriorityToken(prio.fileFormat)) + tokens
            }
        }

    var recurrencePattern: String? = null
        get() = getFirstToken<RecurrenceToken>()?.valueStr

    var tags: Set<String> = emptySet<String>()
        get() {
            return tokens.filter { it is TagToken }.map { it -> (it as TagToken).value }.toSet()
        }

    var lists: Set<String> = emptySet<String>().toSortedSet()
        get() {
            return tokens.filter { it is ListToken }.map { it -> (it as ListToken).value }.toSet()
        }

    var links: Set<String> = emptySet()
        get() {
            return tokens.filter { it is LinkToken }.map { it -> (it as LinkToken).text }.toSet()
        }

    var phoneNumbers: Set<String> = emptySet()
        get() {
            return tokens.filter { it is PhoneToken }.map { it -> (it as PhoneToken).text }.toSet()
        }
    var mailAddresses: Set<String> = emptySet()
        get() {
            return tokens.filter { it is MailToken }.map { it -> (it as MailToken).text }.toSet()
        }
    var selected: Boolean = false

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

    fun markComplete(dateStr: String): Task? {
        if (!this.isCompleted()) {
            val textWithoutCompletedInfo = text
            tokens = listOf(CompletedToken(true), CompletedDateToken(dateStr)) + tokens
            val pattern = recurrencePattern
            if (pattern != null) {
                var deferFromDate = ""
                if (recurrencePattern?.contains("+") == false) {
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
        if (MATCH_SINGLE_DATE.reset(deferString).matches()) {
            thresholdDate = deferString
            return
        }
        if (deferString == "") {
            thresholdDate = null
            return
        }
        val olddate: String?
        if (deferFromDate.isEmpty()) {
            olddate = thresholdDate
        } else {
            olddate = deferFromDate
        }
        val newDate = addInterval(olddate, deferString)
        thresholdDate = newDate?.format(DATE_FORMAT)
    }

    fun deferDueDate(deferString: String, deferFromDate: String) {
        if (MATCH_SINGLE_DATE.reset(deferString).matches()) {
            dueDate = deferString
            return
        }
        if (deferString == "") {
            dueDate = null
            return
        }
        val olddate: String?
        if (deferFromDate.isEmpty()) {
            olddate = dueDate
        } else {
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

    fun showParts(filter: (TToken) -> Boolean): String {
        return tokens.filter(filter)
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
    fun addTag(tagName: String) {
        tagName.split(Regex("\\s+")).forEach {
            if (!tags.contains(it)) {
                tokens += TagToken("+" + it)
            }
        }
    }

    companion object {
        var TAG = "Task"
        const val DATE_FORMAT = "YYYY-MM-DD"
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

//        fun parse(text: String): ArrayList<TToken> {
//            val tokens = ArrayList<TToken>()
//            tokens.add(TextToken(text))
//            return tokens
//        }

        fun parse(text: String): ArrayList<TToken> {
            var lexemes = text.lex()
            val tokens = ArrayList<TToken>()

            if (lexemes.take(1) == listOf("x")) {
                tokens.add(CompletedToken(true))
                lexemes = lexemes.drop(1)
                var nextToken = lexemes.getOrElse(0, { "" })
                MATCH_SINGLE_DATE.reset(nextToken).apply {
                    if (matches()) {
                        tokens.add(CompletedDateToken(lexemes.first()))
                        lexemes = lexemes.drop(1)
                        nextToken = lexemes.getOrElse(0, { "" })
                        MATCH_SINGLE_DATE.reset(nextToken).apply {
                            if (matches()) {
                                tokens.add(CreateDateToken(lexemes.first()))
                                lexemes = lexemes.drop(1)
                            }
                        }
                    }
                }
            }

            var nextToken = lexemes.getOrElse(0, { "" })
            MATCH_PRIORITY.matchEntire(nextToken)?.let {
                tokens.add(PriorityToken(nextToken))
                lexemes = lexemes.drop(1)
            }

            nextToken = lexemes.getOrElse(0, { "" })
            MATCH_SINGLE_DATE.reset(nextToken)?.apply {
                if (matches()) {
                    tokens.add(CreateDateToken(lexemes.first()))
                    lexemes = lexemes.drop(1)
                }
            }

            lexemes.forEach { lexeme ->
                MATCH_LIST.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(ListToken(lexeme))
                        return@forEach
                    }
                }
                MATCH_TAG.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(TagToken(lexeme))
                        return@forEach
                    }
                }
                MATCH_HIDDEN.reset(lexeme)?.apply {
                    if (matches()) {
                        tokens.add(HiddenToken(group(1)))
                        return@forEach
                    }
                }
                // Match phone numbers before tags to support +31.....
                // This will make tags which can also be interpreted as phone numbers not possible
                MATCH_PHONE_NUMBER.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(PhoneToken(lexeme))
                        return@forEach
                    }
                }

                MATCH_DUE.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(DueDateToken(group(1)))
                        return@forEach
                    }
                }
                MATCH_THRESHOLD.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(ThresholdDateToken(group(1)))
                        return@forEach
                    }
                }
                MATCH_RECURRENCE.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(RecurrenceToken(group(1)))
                        return@forEach
                    }
                }
                MATCH_UUID.reset(lexeme).apply {
                    if(matches()) {
                        tokens.add(UUIDToken(group(1)))
                        return@forEach
                    }
                }
                MATCH_URI.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(LinkToken(lexeme))
                        return@forEach
                    }
                }
                MATCH_MAIL.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(MailToken(lexeme))
                        return@forEach
                    }
                }
                MATCH_EXT.reset(lexeme).apply {
                    if (matches()) {
                        tokens.add(ExtToken(group(1), group(2)))
                        return@forEach
                    }
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

interface TToken {
    val text: String
    val value: Any?
    fun isAlpha() = false
}

data class CompletedToken(override val value: Boolean) : TToken {
    override val text: String
        get() = if (value) "x" else ""

}

data class PriorityToken(override val text: String) : TToken {
    override val value: Priority
        get() = Priority.toPriority(text.removeSurrounding("(", ")"))

}

data class ListToken(override val text: String) : TToken {
    override val value: String
        get() = text.substring(1)

}

data class TagToken(override val text: String) : TToken {
    override val value: String
        get() = text.substring(1)

}

// Tokens with the same value as text representation
interface StringValueToken : TToken {
    override val value: String
        get () = text
}

data class CreateDateToken(override val text: String) : StringValueToken
data class CompletedDateToken(override val text: String) : StringValueToken
data class TextToken(override val text: String) : StringValueToken {
    override fun isAlpha(): Boolean {
        return true
    }
}

data class WhiteSpaceToken(override val text: String) : StringValueToken {
    override fun isAlpha(): Boolean {
        return true
    }
}

data class MailToken(override val text: String) : StringValueToken {
    override fun isAlpha(): Boolean {
        return true
    }
}

data class LinkToken(override val text: String) : StringValueToken {
    override fun isAlpha(): Boolean {
        return true
    }
}

data class PhoneToken(override val text: String) : StringValueToken {
    override fun isAlpha(): Boolean {
        return true
    }
}


// Key Value tokens
interface KeyValueToken : TToken {
    val key: String
    val valueStr: String
    override val value: Any?
        get() = valueStr
    override val text: String
        get() = "$key:$valueStr"
}

data class DueDateToken(override val valueStr: String) : KeyValueToken {
    override val key = "due"
}

data class ThresholdDateToken(override val valueStr: String) : KeyValueToken {
    override val key = "t"
}

data class RecurrenceToken(override val valueStr: String) : KeyValueToken {
    override val key = "rec"
}

data class HiddenToken(override val valueStr: String) : KeyValueToken {
    override val key = "h"
    override val value: Boolean
        get() = valueStr == "1"
}

data class ExtToken(override val key: String, override val valueStr: String) : KeyValueToken

data class UUIDToken(override val valueStr: String) : KeyValueToken {
    override val key = "uuid"
}

// Extension functions
fun String.lex(): List<String> = this.split(" ")

