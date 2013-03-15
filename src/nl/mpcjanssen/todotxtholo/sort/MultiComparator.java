package nl.mpcjanssen.todotxtholo.sort;

import java.util.Comparator;
import java.util.List;

public class MultiComparator<Task> implements Comparator<Task> {
    private List<Comparator<Task>> comparators;

    public MultiComparator(List<Comparator<Task>> comparators) {
        this.comparators = comparators;
    }

    public int compare(Task o1, Task o2) {
        for (Comparator<Task> comparator : comparators) {
            int comparison = comparator.compare(o1, o2);
            if (comparison != 0) return comparison;
        }
        return 0;
    }
}
