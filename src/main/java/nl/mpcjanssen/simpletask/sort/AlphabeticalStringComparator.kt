package nl.mpcjanssen.simpletask.sort

import java.util.*


class AlphabeticalStringComparator(caseSensitive: Boolean) : Comparator<String> {
    internal var bCaseSensitive = true

    init {
        this.bCaseSensitive = caseSensitive
    }

    override fun compare(t1: String?, t2: String?): Int {
        var a = t1
        var b = t2
        if (a == null) {
            a = ""
        }
        if (b == null) {
            b = ""
        }
        if (bCaseSensitive) {
            return a.compareTo(b)
        } else {

            return a.toUpperCase().compareTo(b.toUpperCase())
        }
    }
}
