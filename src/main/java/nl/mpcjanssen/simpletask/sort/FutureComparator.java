package nl.mpcjanssen.simpletask.sort;

import android.support.annotation.NonNull;
import com.google.common.collect.Ordering;
import nl.mpcjanssen.simpletask.task.Task;

public class FutureComparator extends Ordering<Task> {

    @Override
    public int compare(Task a, Task b) {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }
        int futureA = a.inFuture() ? 1 : 0;
        int futureB = b.inFuture() ? 1 : 0;
        return (futureA - futureB);
    }
}
