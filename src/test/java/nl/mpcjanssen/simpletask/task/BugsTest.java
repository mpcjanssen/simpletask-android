package nl.mpcjanssen.simpletask.task;

import junit.framework.TestCase;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.sort.MultiComparator;
import nl.mpcjanssen.simpletask.task.token.Token;

import java.util.ArrayList;

/**
 * Tests to guard against bug regressions.
 */
public class BugsTest extends TestCase {

    public void testBuga7248d85e () {
        // Even though tasks below is not technically formatted
        // correctly it should not be changed in the file
        String taskContents = "x test";
        Task t = new Task(taskContents);
        assertEquals(taskContents, t.inFileFormat());
    }

    public void testActiveSortNullCrash() {
        ActiveFilter f = new ActiveFilter();
        MultiComparator mc =  new MultiComparator(f.getSort(null),true, new ArrayList<Task>());
        assertNotNull(mc);
    }

    public void testBug50() {
        Task t = new Task("2012-01-01 @list test");
        assertEquals("test",t.showParts(~Token.LIST & ~Token.CREATION_DATE).trim());
        
    }
}
