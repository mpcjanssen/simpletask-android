package nl.mpcjanssen.simpletask.util

import java.util.*

class ListenerList<L> {
    private val listenerList = ArrayList<L>()

    interface FireHandler<in L> {
        fun fireEvent(listener: L)
    }

    fun add(listener: L) {
        listenerList.add(listener)
    }

    fun fireEvent(fireHandler: FireHandler<L>) {
        val copy = ArrayList(listenerList)
        for (l in copy) {
            fireHandler.fireEvent(l)
        }
    }

    fun remove(listener: L) {
        listenerList.remove(listener)
    }

}
