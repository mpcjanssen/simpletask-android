package nl.mpcjanssen.simpletask.task

import junit.framework.Assert
import junit.framework.TestCase
import nl.mpcjanssen.simpletask.ActiveFilter
import nl.mpcjanssen.simpletask.FilterOptions
import nl.mpcjanssen.simpletask.sort.MultiComparator
import nl.mpcjanssen.simpletask.util.*

/**
 * Tests to guard against bug regressions.
 */
class BugsTest : TestCase() {

    fun testBuga7248d85e() {
        // Even though tasks below is not technically formatted
        // correctly it should not be changed in the file
        val taskContents = "x test"
        val t = Task(taskContents)
        Assert.assertEquals(taskContents, t.inFileFormat())
    }

    fun testActiveSortNullCrash() {
        val opt = FilterOptions(luaModule = "test")
        val f = ActiveFilter(opt)
        val mc = MultiComparator(f.getSort(null), todayAsString, true, false)
        Assert.assertNotNull(mc)
    }

    fun testBug50() {
        val t = Task("2012-01-01 @list test")
        Assert.assertEquals("test", t.showParts(TToken.LIST.inv() and TToken.CREATION_DATE.inv()).trim { it <= ' ' })

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
