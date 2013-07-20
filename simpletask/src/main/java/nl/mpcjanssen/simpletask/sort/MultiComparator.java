package nl.mpcjanssen.simpletask.sort;

import android.util.Log;
import nl.mpcjanssen.simpletask.Constants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MultiComparator<Task> implements Comparator<Task> {
    private List<Comparator<Task>> comparators;

    static public MultiComparator create(ArrayList<String> sorts) {
        List<Comparator<?>> comparators = new ArrayList<Comparator<?>>();


        for (String sort : sorts) {
            String parts[] = sort.split(Constants.SORT_SEPARATOR);
            boolean reverse = false;
            String sortType;
            if(parts.length==1) {
                // support older shortcuts and widgets
                reverse = false;
                sortType = parts[0];
            } else {
                sortType = parts[1];
                if (parts[0].equals(Constants.REVERSED_SORT)) {
                    reverse = true;
                }
            }
            if (sortType.equals("file_order")) {
                comparators.add(new FileOrderComparator(reverse));
            } else if (sortType.equals("by_context")) {
                comparators.add(new ContextComparator(reverse));
            } else if (sortType.equals("by_project")) {
                comparators.add(new ProjectComparator(reverse));
            } else if (sortType.equals("alphabetical")) {
                comparators.add(new AlphabeticalComparator(reverse));
            } else if (sortType.equals("by_prio")) {
                comparators.add(new PriorityComparator(reverse));
            } else if (sortType.equals("completed")) {
                comparators.add(new CompletedComparator(reverse));
            } else if (sortType.equals("by_creation_date")){
                comparators.add(new CreationDateComparator(reverse));
            } else if (sortType.equals("in_future")){
                comparators.add(new FutureComparator(reverse));
            } else if (sortType.equals("by_due_date")){
                comparators.add(new DueDateComparator(reverse));
            }else {
                Log.w("Simpletask", "Unknown sort: " + sort);
            }
        }
        return (new MultiComparator(comparators));
    }

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
