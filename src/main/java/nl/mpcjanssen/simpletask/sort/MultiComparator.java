package nl.mpcjanssen.simpletask.sort;

import android.support.annotation.NonNull;
import com.google.common.collect.Ordering;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MultiComparator implements Comparator<Task> {
    private Ordering<? super Task> ordering;

    public MultiComparator (@NonNull ArrayList<String> sorts, boolean caseSensitve, List<Task> taskList) {
        Logger log = LoggerFactory.getLogger(this.getClass());
        List<Comparator<? super Task>> comparators = new ArrayList<>();
        label:
        for (String sort : sorts) {
            String parts[] = sort.split(ActiveFilter.SORT_SEPARATOR);
            boolean reverse = false;
            String sortType;
            if (parts.length == 1) {
                // support older shortcuts and widgets
                reverse = false;
                sortType = parts[0];
            } else {
                sortType = parts[1];
                if (parts[0].equals(ActiveFilter.REVERSED_SORT)) {
                    reverse = true;
                }
            }
            Ordering<? super Task> comp;
            switch (sortType) {
                case "file_order":
                    // In case of revers file order sort, we can just reverse
                    // based on the object order
                    if (reverse) {
                        comparators.add(Ordering.explicit(taskList).reverse());
                    }
                    // No need to continue sorting after unsorted
                    break label;
                case "by_context":
                    comp = new ContextComparator(caseSensitve);
                    break;
                case "by_project":
                    comp = new ProjectComparator(caseSensitve);
                    break;
                case "alphabetical":
                    comp = new AlphabeticalComparator(caseSensitve);
                    break;
                case "by_prio":
                    comp = new PriorityComparator();
                    break;
                case "completed":
                    comp = new CompletedComparator();
                    break;
                case "by_creation_date":
                    comp = new CreationDateComparator();
                    break;
                case "in_future":
                    comp = new FutureComparator();
                    break;
                case "by_due_date":
                    comp = new DueDateComparator();
                    break;
                case "by_threshold_date":
                    comp = new ThresholdDateComparator();
                    break;
                default:
                    log.warn("Unknown sort: " + sort);
                    comp = Ordering.allEqual();
                    break;
            }
            if (reverse) {
                comp = comp.reverse();
            }
            comparators.add(comp);
        }
        this.ordering = Ordering.compound(comparators);
    }

    public int compare(Task o1, Task o2) {
        return ordering.compare(o1,o2);
    }
}
