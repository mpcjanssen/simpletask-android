package nl.mpcjanssen.simpletask.sort

import java.util.*
import kotlin.text.toUpperCase


class AlphabeticalStringComparator(caseSensitive: Boolean) : Comparator<String> {
    internal var bCaseSensitive = true

    init {
        this.bCaseSensitive = caseSensitive
    }

    override fun compare(a: String?, b: String?): Int {
        var a = a
        var b = b
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
