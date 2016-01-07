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

import android.content.Context
import android.text.SpannableString
import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.ActiveFilter
import nl.mpcjanssen.simpletask.Constants
import nl.mpcjanssen.simpletask.task.token.*
import nl.mpcjanssen.simpletask.util.RelativeDate
import nl.mpcjanssen.simpletask.util.*
import nl.mpcjanssen.simpletask.util.*

import java.io.Serializable
import java.util.*

import java.util.regex.Matcher
import java.util.regex.Pattern


class TTask (text: String, defaultPrependedDate: DateTime? = null) {

    private var tokens: ArrayList<Token>

    init {
        tokens = parse(text)
    }

    fun update(rawText: String) {
        tokens = parse(rawText)
    }

    private inline fun <reified T>getFirstToken() : T? {
        tokens.filterIsInstance<T>().forEach {
            return it
        }
        return null
    }

    var completionDate: String? =  null
            get() =  getFirstToken<COMPLETED_DATE>()?.value ?: null



    val text: String
        get() {
            return tokens.map {it.value}.joinToString("")
        }

    companion object {
        var TAG = Task::class.java.name
        val DUE_DATE = 0
        val THRESHOLD_DATE = 1
        private val serialVersionUID = 1L
        private val LIST_MATCHER = Pattern.compile("^@(\\S*\\w)(.*)").matcher("")
        private val TAG_MATCHER = Pattern.compile("^\\+(\\S*\\w)(.*)").matcher("")
        private val HIDDEN_MATCHER = Pattern.compile("^[Hh]:([01])(.*)").matcher("")
        private val DUE_MATCHER = Pattern.compile("^[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})(.*)").matcher("")
        private val THRESHOLD_MATCHER = Pattern.compile("^[Tt]:(\\d{4}-\\d{2}-\\d{2})(.*)").matcher("")
        private val RECURRENCE_MATCHER = Pattern.compile("(^||\\s)[Rr][Ee][Cc]:((\\+?)\\d+[dDwWmMyY])").matcher("")
        private val PRIORITY_MATCHER = Pattern.compile("^(\\(([A-Z])\\) )(.*)").matcher("")
        private val SINGLE_DATE_MATCHER = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} )(.*)").matcher("")
        private val COMPLETED_PREFIX = "x "
        // Synchronized access as matcher.reset is not thread safe
        @Synchronized internal fun parse(text: String) : ArrayList<Token> {
            var lexemes = text.lex()
            val tokens = ArrayList<Token>()
            var m: Matcher

            var remaining = text
            if (lexemes.subList(0,2) == listOf("x", " ")) {
                tokens.add(COMPLETED())
                lexemes = lexemes.drop(2)
                remaining = text.substring(2)
                m = SINGLE_DATE_MATCHER.reset(remaining)
                // read optional completion date (this 'violates' the format spec)
                // be liberal with date format errors
                if (m.matches()) {

                    remaining = m.group(2)
                    tokens.add(COMPLETED_DATE(m.group(1)))
                    m = SINGLE_DATE_MATCHER.reset(remaining)
                    // read possible create date
                    if (m.matches()) {

                        remaining = m.group(2)
                        tokens.add(CREATION_DATE(m.group(1)))

                    }
                }
            }

            // Check for optional priority
            m = PRIORITY_MATCHER.reset(remaining)
            if (m.matches()) {

                remaining = m.group(3)
                tokens.add(PRIO(m.group(1)))
            }
            // Check for optional creation date
            m = SINGLE_DATE_MATCHER.reset(remaining)
            if (m.matches()) {
                remaining = m.group(2)
                tokens.add(CREATION_DATE(m.group(1)))
            }

            while (remaining.length > 0) {
                if (remaining.startsWith(" ")) {
                    var leading = ""
                    while (remaining.length > 0 && remaining.startsWith(" ")) {
                        leading = leading + " "
                        remaining = remaining.substring(1)
                    }
                    val ws = WHITE_SPACE(leading)
                    tokens.add(ws)
                    continue
                }
                m = LIST_MATCHER.reset(remaining)
                if (m.matches()) {
                    val list = m.group(1)
                    remaining = m.group(2)
                    val listToken = LIST("@" + list)
                    tokens.add(listToken)

                    continue
                }
                m = TAG_MATCHER.reset(remaining)
                if (m.matches()) {
                    val match = m.group(1)
                    remaining = m.group(2)
                    val listToken = TTAG("+" + match)
                    tokens.add(listToken)

                    continue
                }
                m = THRESHOLD_MATCHER.reset(remaining)
                if (m.matches()) {
                    val match = m.group(1)
                    remaining = m.group(2)
                    val tok = THRESHOLD_DATE(match)
                    tokens.add(tok)

                    continue
                }
                m = DUE_MATCHER.reset(remaining)
                if (m.matches()) {
                    val match = m.group(1)
                    remaining = m.group(2)
                    val tok = DUE_DATE(match)
                    tokens.add(tok)

                    continue
                }
                m = HIDDEN_MATCHER.reset(remaining)
                if (m.matches()) {
                    val match = m.group(1)
                    remaining = m.group(2)
                    val tok = HIDDEN(match)
                    tokens.add(tok)

                    continue
                }
                var leading = ""
                while (remaining.length > 0 && !remaining.startsWith(" ")) {
                    leading = leading + remaining.substring(0, 1)
                    remaining = remaining.substring(1)
                }
                val txt = TEXT(leading)
                tokens.add(txt)
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