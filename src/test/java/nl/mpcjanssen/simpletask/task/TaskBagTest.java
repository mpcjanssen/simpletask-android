package nl.mpcjanssen.simpletask.task;

import android.app.Activity;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;

/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

public class TaskBagTest extends TestCase {

    private TaskCache tc;

    public void initTaskCache(TaskCache tc, ArrayList<String> lines) {
        tc.startLoading();
        int i = 0;
        for (String line : lines) {
            tc.load(new Task(i, line));
            i++;
        }
        tc.endLoading();
    }

    @Override
    protected void setUp() {
        tc = new TaskCache(null);
    }
    public void testInit() {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("Test");
        lines.add("Test2");
        initTaskCache(tc,lines);
        
        assertEquals(2, tc.size());
        assertEquals("Test", tc.getTaskAt(0).inFileFormat());
        assertEquals(0, tc.getContexts().size());
    }

    public void testDeleteIdentical() {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("Test");
        lines.add("Test");
        initTaskCache(tc,lines);
        assertEquals(2, tc.size());
        assertEquals("Test", tc.getTaskAt(0).inFileFormat());
        ArrayList<Task> tasksToDelete = new ArrayList<Task>();
        tasksToDelete.add(new Task(0,"Test"));
        tc.modify(null,null,null,tasksToDelete);
        assertEquals(1, tc.size());
    }

    public void testSimpleFilter() {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("Test");
        lines.add("Test2 @Match");
        initTaskCache(tc,lines);
        ActiveFilter filter = new ActiveFilter();
        ArrayList<String> contexts = new ArrayList<String>();
        contexts.add("NoMatch");
        filter.setContexts(contexts);
        ArrayList<Task> visibleTasks = filter.apply(tc.getTasks());
        assertEquals(0, visibleTasks.size());
        contexts.clear();
        contexts.add("Match");
        filter.setContexts(contexts);
        visibleTasks = filter.apply(tc.getTasks());
        assertEquals(1, visibleTasks.size());
    }
}
