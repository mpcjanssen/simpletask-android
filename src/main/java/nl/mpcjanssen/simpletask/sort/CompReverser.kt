package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TodoListItem
import java.util.*


class CompReverser(val comp: Comparator<TodoListItem>) : Comparator<TodoListItem> {
    override fun compare(t1: TodoListItem?, t2: TodoListItem?): Int {
        return -comp.compare(t1,t2)
    }

}