package nl.mpcjanssen.simpletask.task;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import de.greenrobot.dao.AbstractDaoSession;
import junit.framework.Assert;
import junit.framework.TestCase;
import nl.mpcjanssen.simpletask.dao.Daos;
import nl.mpcjanssen.simpletask.dao.gentodo.TodoItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TodoListTest {
    @Before
    public void ensureTodoListEmpty() {
        assertThat(0 , is(TodoList.INSTANCE.size()));
    }

    @Test
    public void testCompleteCommitted() {
        TodoList tl = TodoList.INSTANCE;
        tl.add(new Task("12344"),true,false);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertThat(1, is(tl.getTodoItems().size()));
        List<TodoItem> items = tl.getTodoItems();
        TodoItem first = items.get(0);
        assertThat("12344", is(first.getTask().getText()));
        tl.complete(first, true, true);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Clear cached value
        Daos.INSTANCE.getTodoItemDao().detach(first);
        items = tl.getTodoItems();
        first = items.get(0);
        assertThat(first.getTask().isCompleted(), is(true));
    }
}

