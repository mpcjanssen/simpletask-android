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


import hirondelle.date4j.DateTime

import nl.mpcjanssen.simpletask.task.ttoken.*

import java.util.*



class TTask (text: String, defaultPrependedDate: DateTime? = null) {

    public var tokens: ArrayList<TToken>

    init {
        tokens = parse(text)
    }

    constructor (text: String) : this(text, null)

    fun update(rawText: String) {
        tokens = parse(rawText)
    }

    private inline fun <reified T>getFirstToken() : T? {
        tokens.filterIsInstance<T>().forEach {
            return it
        }
        return null
    }

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as TTask

        if (tokens != other.tokens) return false

        return true
    }

    override fun hashCode(): Int{
        return tokens.hashCode()
    }


    var completionDate: String? =  null
            get() =  getFirstToken<CompletedDateToken>()?.text ?: null


    var dueDate: DateTime? =  null
        get() =  getFirstToken<DueDateToken>()?.value ?: null




    val text: String
        get() {
            return tokens.map {it.text}.joinToString("")
        }

    companion object {
        var TAG = Task::class.java.name
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

        fun parse(text: String) : ArrayList<TToken> {
            var lexemes = text.lex()
            val tokens = ArrayList<TToken>()

            if (lexemes.take(2) == listOf("x", " ")) {
                tokens.add(CompletedToken(true))
                lexemes = lexemes.drop(2)
                var nextToken = lexemes.getOrElse(0, { "" })
                MATCH_SINGLE_DATE.matchEntire(nextToken)?.let {
                    tokens.add(CompletedDateToken(lexemes.first()))
                    lexemes = lexemes.drop(1)
                    nextToken = lexemes.getOrElse(0, { "" })
                    MATCH_SINGLE_DATE.matchEntire(nextToken)?.let {
                        tokens.add(CreatedDateToken(lexemes.first()))
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
                tokens.add(CompletedDateToken(lexemes.first()))
                lexemes = lexemes.drop(1)
                nextToken = lexemes.getOrElse(0, { "" })
                MATCH_SINGLE_DATE.matchEntire(nextToken)?.let {
                    tokens.add(CreatedDateToken(lexemes.first()))
                    lexemes = lexemes.drop(1)
                }
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


// Extension functions

fun String.lex() : List<String> {
    val res = ArrayList<String>()
    var lexeme = ""
    this.forEach { char ->
        when (char) {
            ' ' -> {
                if (lexeme.isNotEmpty()) res.add(lexeme)
                res.add(char.toString())
                lexeme = ""
            }
            else -> lexeme += char
        }
    }
    if (lexeme.isNotEmpty()) res.add(lexeme)
    return res
}

fun String.toDateTime(): DateTime? {
    var date : DateTime? = null;
    if (this != null && DateTime.isParseable(this)) {
        date = DateTime(this)
    } else if (this == null) {
        date = DateTime.today(TimeZone.getDefault())
    } else {
        date = null
    }
    return date
}