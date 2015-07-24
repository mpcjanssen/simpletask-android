package nl.mpcjanssen.simpletask.task;

import junit.framework.TestCase;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;

/**
 * Created by Mark on 2015-07-24.
 */
public class TodoListTest extends TestCase implements TodoList.TodoListChanged {

    public void testInit() {
        TodoList t = new TodoList(null, false);
    }

    @Override
    public void todoListChanged() {

    }
}