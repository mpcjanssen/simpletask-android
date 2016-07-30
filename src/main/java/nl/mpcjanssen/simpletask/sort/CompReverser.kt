package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.dao.gen.TodoItem
import java.util.*


class CompReverser(val comp: Comparator<TodoItem>) : Comparator<TodoItem> {
    override fun compare(t1: TodoItem?, t2: TodoItem?): Int {
        return -comp.compare(t1,t2)
    }

}