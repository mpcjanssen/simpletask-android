package nl.mpcjanssen.simpletask.sort;

import junit.framework.TestCase;
import nl.mpcjanssen.simpletask.task.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mark on 2015-07-16.
 */
public class ContextComparatorTest extends TestCase {

    public void testCompare() throws Exception {
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("Test @b"));
        tasks.add(new Task("Test @a"));
        tasks.add(new Task("Test"));
        tasks.add(new Task("Loop @a"));
        Collections.sort(tasks, new ContextComparator(true));
        assertEquals("Test", tasks.get(0).inFileFormat());
        assertEquals("Test @a", tasks.get(1).inFileFormat());
        assertEquals("Loop @a", tasks.get(2).inFileFormat());
        assertEquals("Test @b", tasks.get(3).inFileFormat());
    }
}