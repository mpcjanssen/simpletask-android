package nl.mpcjanssen.simpletask.task

import junit.framework.TestCase
import java.util.*

class TTaskTest : TestCase() {
    fun testParseIdempotence() {
        var t = "x 1231 2 123 312 +test"
        assertEquals(t, TTask(t).text)
        t = "Test abcd "
        assertEquals(t, TTask(t).text)
        t = "x 2012-14-11 rec:12w mail@example.com"
        assertEquals(t, TTask(t).text)
    }

    fun testLexing() {
        val res = ArrayList<String>()
        var lexeme = ""
        "ab b  d s".forEach { char ->
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

        assertEquals(listOf("ab", " ", "b", " ", " ", "d", " ", "s"),res)
        assertEquals(listOf("ab", " ", "b", " ", " ", "d", " ", "s"), "ab b  d s".lex())

    }


}