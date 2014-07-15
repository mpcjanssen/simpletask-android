package nl.mpcjanssen.simpletask.sort;

import android.util.Log;

import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.task.Task;

public class MultiComparator implements Comparator<Task> {
    private Ordering<Task> ordering;

    public MultiComparator (ArrayList<String> sorts) {
        List<Comparator<Task>> comparators = new ArrayList<Comparator<Task>>();


        for (String sort : sorts) {
            String parts[] = sort.split(ActiveFilter.SORT_SEPARATOR);
            boolean reverse = false;
            String sortType;
            if(parts.length==1) {
                // support older shortcuts and widgets
                reverse = false;
                sortType = parts[0];
            } else {
                sortType = parts[1];
                if (parts[0].equals(ActiveFilter.REVERSED_SORT)) {
                    reverse = true;
                }
            }
            Ordering<Task> comp;
            if (sortType.equals("file_order")) {
                comp = new FileOrderComparator();
            } else if (sortType.equals("by_context")) {
                comp = new ContextComparator();
            } else if (sortType.equals("by_project")) {
                comp = new ProjectComparator();
            } else if (sortType.equals("alphabetical")) {
                comp = new AlphabeticalComparator();
            } else if (sortType.equals("by_prio")) {
                comp = new PriorityComparator();
            } else if (sortType.equals("completed")) {
                comp = new CompletedComparator();
            } else if (sortType.equals("by_creation_date")){
                comp = new CreationDateComparator();
            } else if (sortType.equals("in_future")){
                comp = new FutureComparator();
            } else if (sortType.equals("by_due_date")){
                comp = new DueDateComparator();
            }else if (sortType.equals("by_threshold_date")){
                comp = new ThresholdDateComparator();
            } else {
                Log.w("Simpletask", "Unknown sort: " + sort);
                comp = new FileOrderComparator();
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
