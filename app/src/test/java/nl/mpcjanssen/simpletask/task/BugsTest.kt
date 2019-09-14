package nl.mpcjanssen.simpletask.task

import junit.framework.TestCase
import nl.mpcjanssen.simpletask.Query
import nl.mpcjanssen.simpletask.MultiComparator
import nl.mpcjanssen.simpletask.util.todayAsString
import org.junit.Assert

/**
 * Tests to guard against bug regressions.
 */
class BugsTest : TestCase() {

    fun testBuga7248d85e() {
        // Even though tasks below is not technically formatted
        // correctly it should not be changed in the file
        val taskContents = "x test"
        val t = Task(taskContents)
        Assert.assertEquals(taskContents, t.inFileFormat(false))
    }

    fun testActiveSortNullCrash() {
        val q = Query(luaModule = "test")
        val mc = MultiComparator(q.getSort(null), todayAsString, true, false)
        Assert.assertNotNull(mc)
    }

    fun testBug50() {
        val t = Task("2012-01-01 @list test")
        Assert.assertEquals("test", t.showParts({ (it !is ListToken) && (it !is CreateDateToken) }).trim { it <= ' ' })

    }

    // https://github.com/mpcjanssen/simpletask-android/issues/367
    fun testBug367() {
        val t = Task("Test due:2010-10-10 t:2010-11-11")
        t.dueDate = ""
        t.thresholdDate = ""
        Assert.assertEquals(null, t.dueDate)
        Assert.assertEquals(null, t.thresholdDate)
    }
}
