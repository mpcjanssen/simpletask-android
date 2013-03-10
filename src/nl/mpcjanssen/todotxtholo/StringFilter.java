package nl.mpcjanssen.todotxtholo;

import nl.mpcjanssen.todotxtholo.util.Util;

import java.util.ArrayList;

public class StringFilter {
    ArrayList<String> items = new ArrayList<String>();
    boolean not;

    public StringFilter(ArrayList<String> items, boolean not) {
        if (items == null) {
            items = new ArrayList<String>();
        }
        this.items.addAll(items);
        this.not = not;
    }

    public ArrayList<String> getItems() {
        return items;
    }

    public boolean getNot() {
        return not;
    }

    @Override
    public String toString() {
        String result = "";
        if (not) {
            result = result + "Not ";
        }
        result = result + Util.join(items, ", ");
        return result;
    }
}
