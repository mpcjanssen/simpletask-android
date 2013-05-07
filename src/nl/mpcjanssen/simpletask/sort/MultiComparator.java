package nl.mpcjanssen.simpletask.sort;

import java.util.Comparator;
import java.util.List;

public class MultiComparator<Task> implements Comparator<Task> {
    private List<Comparator<Task>> m_comparators;

    public MultiComparator(List<Comparator<Task>> comparators) {
        m_comparators = comparators;
    }

    @Override
	public int compare(Task o1, Task o2) {
        for (Comparator<Task> comparator : m_comparators) {
            int comparison = comparator.compare(o1, o2);
            if (comparison != 0) return comparison;
        }
        return 0;
    }
}
