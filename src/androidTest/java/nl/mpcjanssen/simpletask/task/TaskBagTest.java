package nl.mpcjanssen.simpletask.task;

import android.content.SharedPreferences;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.TestLocalTaskRepository;
import nl.mpcjanssen.simpletask.util.Strings;

/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

public class TaskBagTest extends TestCase {
    TaskBag.Preferences pref;

    public void setUp () {
        pref = new TaskBag.Preferences(new SharedPreferences() {
            @Override
            public Map<String, ?> getAll() {
                return null;
            }

            @Override
            public String getString(String s, String s2) {
                return null;
            }

            @Override
            public Set<String> getStringSet(String s, Set<String> strings) {
                return null;
            }

            @Override
            public int getInt(String s, int i) {
                return 0;
            }

            @Override
            public long getLong(String s, long l) {
                return 0;
            }

            @Override
            public float getFloat(String s, float v) {
                return 0;
            }

            @Override
            public boolean getBoolean(String s, boolean b) {
                return false;
            }

            @Override
            public boolean contains(String s) {
                return false;
            }

            @Override
            public Editor edit() {
                return null;
            }

            @Override
            public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {

            }

            @Override
            public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {

            }
        });
    }


    public void testInit() {
        TaskBag tb = new TaskBag(pref, new TestLocalTaskRepository());
        tb.reload("Test\nTest2");
        assertEquals(2, tb.size());
        assertEquals("Test", tb.getTaskAt(0).inFileFormat());
        assertEquals(0, tb.getContexts(false).size());
    }

    public void testSimpleFilter () {
        TaskBag tb = new TaskBag(pref, new TestLocalTaskRepository());
        tb.reload("Test\nTest2 @Match");
        ActiveFilter filter = new ActiveFilter();
        ArrayList<String> contexts = new ArrayList<String>();
        contexts.add("NoMatch");
        filter.setContexts(contexts);
        ArrayList<Task> visibleTasks = filter.apply(tb.getTasks());
        assertEquals(0, visibleTasks.size());
        contexts.clear();
        contexts.add("Match");
        filter.setContexts(contexts);
        visibleTasks = filter.apply(tb.getTasks());
        assertEquals(1, visibleTasks.size());
    }
}