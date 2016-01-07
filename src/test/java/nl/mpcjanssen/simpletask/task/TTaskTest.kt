package nl.mpcjanssen.simpletask.task

import junit.framework.TestCase
import java.util.*

class TTaskTest : TestCase() {
    fun testParseIdempotence() {
        val t1 = "x 1231 2 123 312 +test"
        assertEquals(t1, TTask(t1).text)
        val s = "Test abcd "
        val t = " "
        val v = ""
        assertEquals(s, TTask(s).text)
        assertEquals(t, TTask(t).text)
        assertEquals(v, TTask(v).text)
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