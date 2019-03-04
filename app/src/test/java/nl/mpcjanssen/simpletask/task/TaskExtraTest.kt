package nl.mpcjanssen.simpletask.task


import junit.framework.TestCase

class TaskExtraTest : TestCase() {
    fun testMarkdownCheckList () {
        assertFalse(Task(" - [ ] abcd").isCompleted())
        assertTrue(Task(" - [x] abcd").isCompleted())
        assertTrue(Task(" - [X] abcd").isCompleted())
        val t = Task(" - [ ] abcd")
        t.markComplete("2000-00-00")
        assertEquals(t.inFileFormat()," - [x] abcd")
    }

}
