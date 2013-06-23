package nl.mpcjanssen.todotxtholo.sort;

import nl.mpcjanssen.todotxtholo.task.Task;

public class FutureComparator extends ReversableComparator {

    public FutureComparator(boolean reverse) {
        super(reverse);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        int futureA = a.inFuture() ? 1 : 0;
        int futureB = b.inFuture() ? 1 : 0;
        return (futureA - futureB);
    }
}
