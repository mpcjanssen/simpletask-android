package nl.mpcjanssen.simpletask

import java.util.*

/**
 * Created by alain on 27/12/17.
 */

class MyTitle(mytext :String) {
    var text :String = mytext
        get() = field + " ($count)"
    var ori=mytext
    var myFolding: HashMap<String, Boolean>? = null
    var count =0
}

