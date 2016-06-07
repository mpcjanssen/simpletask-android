/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A filter that matches Tasks containing the specified priorities
 *
 * @author Tim Barlotta
 */
public class ByPriorityFilter implements TaskFilter {
    @NonNull
    ArrayList<Priority> priorities = new ArrayList<>();
    private boolean not;

    public ByPriorityFilter(@Nullable List<Priority> priorities, boolean not) {
        if (priorities != null) {
            this.priorities.addAll(priorities);
        }
        this.not = not;
    }

    @Override
    public boolean apply(@NonNull Task t) {
        if (not) {
            return !filter(t);
        } else {
            return filter(t);
        }
    }

    public boolean filter(@NonNull Task input) {
        if (priorities.size() == 0) {
            return true;
        }

        return priorities.contains(input.getPriority());
    }

    /* FOR TESTING ONLY, DO NOT USE IN APPLICATION */
    @NonNull
    ArrayList<Priority> getPriorities() {
        return priorities;
    }
}
