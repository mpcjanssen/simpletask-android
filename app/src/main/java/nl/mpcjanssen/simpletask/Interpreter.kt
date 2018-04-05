package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.*

import tcl.lang.*

class ToastShortCmd : Command {

    override fun cmdProc(interp: Interp?, objv: Array<out TclObject>?) {
        interp?.let {
            if (objv == null || objv.size != 2) {
                throw TclNumArgsException(interp, 1, objv,
                        "toastText")
            }
            showToastShort(TodoApplication.app, objv[1].toString())
        }
    }

}

object Interpreter : AbstractInterpreter() {
    val interp = Interp()
    override fun tasklistTextSize(): Float? {
        return interp.tasklistTextSize()?.toFloat()
    }

    override fun configTheme(): String? {
        return interp.configTheme()
    }

    override fun onFilterCallback(moduleName: String, t: Task): Pair<Boolean, String> {
        return interp.onFilterCallback(moduleName,t)
    }

    override fun hasFilterCallback(moduleName: String): Boolean {
        return interp.hasOnFilterCallback(moduleName)
    }

    override fun hasOnSortCallback(moduleName: String): Boolean {
        return interp.hasOnSortCallback(moduleName)
    }

    override fun hasOnGroupCallback(moduleName: String): Boolean {
        return interp.hasOnGroupCallback(moduleName)
    }

    override fun onSortCallback(moduleName: String, t: Task): String {
        return interp.onSortCallback(moduleName,t)
    }

    override fun onGroupCallback(moduleName: String, t: Task): String? {
        return interp.onGroupCallback(moduleName, t)
    }

    override fun onDisplayCallback(moduleName: String, t: Task): String? {
        return interp.onDisplayCallback(moduleName,t)
    }

    override fun onAddCallback(t: Task): Task? {
        return interp.onAddCallback(t)
    }

    override fun onTextSearchCallback(moduleName: String, input: String, search: String, caseSensitive: Boolean): Boolean? {
        return true
    }

    override fun evalScript(moduleName: String?, script: String?): AbstractInterpreter {
        script?.let { interp.evalNSScript(moduleName?:"::", it) }
        return this
    }

    override fun clearOnFilter(moduleName: String) {
        interp.evalNSScript(moduleName, "rename $ON_FILTER_NAME {}")
    }


}

fun Interp.init() : Interp  {
    val TODOLIB = readAsset(TodoApplication.app.assets, "tcl/todolib.tcl")
    try {
        this.createCommand("toast", ToastShortCmd())
        this.eval(TODOLIB)
        this.eval(Config.luaConfig)
    } catch (e: TclException) {
        Logger.warn(Config.TAG, "Script execution failed " + this.result)
        showToastLong(TodoApplication.app, "${getString(R.string.lua_error)}:  ${this.result.toString()}")
    }
    return this
}


fun Interp.tasklistTextSize(): Double? {
    try {
        return TclDouble.get(this, this.getVar(Vars.CONFIG_TASKLIST_TEXT_SIZE_SP, TCL.GLOBAL_ONLY))
    } catch (e: TclException) {
        return null
    }
}

// Callback to determine the theme. Return true for dark.


fun Interp.configTheme(): String? {
    try {
        return this.getVar(Vars.CONFIG_THEME, TCL.GLOBAL_ONLY).toString()
    } catch (e: TclException) {
        return null
    }
}

fun cmdName(namespace : String, cmd: String) : String {
    return "::$namespace::$cmd"
}

fun Interp.hasOnFilterCallback(namespace : String): Boolean {
    val cmd = this.getCommand(cmdName(namespace, Callbacks.ON_FILTER_NAME))
    return cmd != null
}

fun Interp.hasOnSortCallback(namespace : String): Boolean {
    val cmd = this.getCommand(cmdName(namespace, Callbacks.ON_SORT_NAME))
    return cmd != null
}

fun Interp.hasOnDisplayCallback(namespace: String): Boolean {
    val cmd = this.getCommand(cmdName(namespace, Callbacks.ON_DISPLAY_NAME))
    return cmd != null
}

fun Interp.hasOnGroupCallback(namespace: String): Boolean {
    val cmd = this.getCommand(cmdName(namespace, Callbacks.ON_GROUP_NAME))
    return cmd != null
}

fun Interp.onFilterCallback(ns: String, t: Task): Pair<Boolean, String> {
    if (!hasOnFilterCallback(ns)) {
        return Pair(true, "true")
    }
    try {
        executeCallbackCommand(cmdName(ns, Callbacks.ON_FILTER_NAME), t)
        val strVal = this.result.toString()
        val boolVal = TclBoolean.get(this, this.result)
        return Pair(boolVal,strVal)
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed: ${this.result}")
        return Pair(true, "true")
    }
}

fun Interp.onSortCallback(ns: String, t1: Task): String {
    try {
        val cmdList = TclList.newInstance()
        TclList.append(this, cmdList, TclString.newInstance(cmdName(ns, Callbacks.ON_SORT_NAME)))
        appendCallbackArgs(t1, cmdList)
        this.eval(cmdList, TCL.GLOBAL_ONLY)
        return this.result.toString()
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed " + e.message)
        return ""
    }
}

fun Interp.evalNSScript(ns: String, script: String) {
    try {
        val cmdList = TclString.newInstance("namespace eval")
        TclList.append(this, cmdList, TclString.newInstance(ns))
        TclList.append(this, cmdList, TclString.newInstance(script))
        this.eval(cmdList, TCL.GLOBAL_ONLY)
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed " + this.result)
    }
}

fun Interp.onGroupCallback(ns: String, t: Task): String? {
    if (!hasOnGroupCallback(ns)) {
        return null
    }
    try {
        executeCallbackCommand(cmdName(ns, Callbacks.ON_GROUP_NAME), t)
        return this.result.toString()
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed " + e.message)
        return null
    }
}

fun Interp.onDisplayCallback(ns: String, t: Task): String? {
    if (!hasOnDisplayCallback(ns)) {
        return null
    }
    try {
        executeCallbackCommand(cmdName(ns, Callbacks.ON_DISPLAY_NAME), t)
        return this.result.toString()
    } catch (e: TclException) {
        log.debug(TAG, "Tcl execution failed " + e.message)
        return null
    }
}

fun Interp.onAddCallback(t: Task): Task {
    return t
}


private fun Interp.executeCallbackCommand(command: String, t: Task) {
    val cmdList = TclList.newInstance()
    TclList.append(this, cmdList, TclString.newInstance(command))
    appendCallbackArgs(t, cmdList)
    this.eval(cmdList, TCL.GLOBAL_ONLY)
}

private fun Interp.appendCallbackArgs(t: Task?, cmdList: TclObject) {
    val fieldDict = TclDict.newInstance()
    val extensionDict = TclDict.newInstance()
    t?.let {
        fieldDict.put(this, "task", t.inFileFormat())
        fieldDict.put(this, "due", t.dueDate ?: "")
        fieldDict.put(this, "threshold", t.thresholdDate ?: "")
        fieldDict.put(this, "createdate", t.createDate ?: "")
        fieldDict.put(this, "completiondate", t.completionDate ?: "")
        fieldDict.put(this, "text", t.text)


        val recPat = t.recurrencePattern
        if (recPat != null) {
            fieldDict.put(this, "recurrence", recPat)
        }
        fieldDict.put(this, "completed", if (t.isCompleted()) "1" else "0")
        fieldDict.put(this, "priority", t.priority.code)

        fieldDict.put(this, "tags", javaListToTclList(t.tags))
        fieldDict.put(this, "lists", javaListToTclList(t.lists))



        for ((key, value) in t.extensions) {
            extensionDict.put(this, key, value)
        }
    }
    TclList.append(this, cmdList, TclString.newInstance(t?.inFileFormat() ?: ""))
    TclList.append(this, cmdList, fieldDict)
    TclList.append(this, cmdList, extensionDict)

}


private fun Interp.javaListToTclList(javaList: Iterable<String>): TclObject {
    val tclList = TclList.newInstance()
    for (item in javaList) {
        TclList.append(this, tclList, TclString.newInstance(item))
    }
    return tclList
}


object Vars {
    val CONFIG_TASKLIST_TEXT_SIZE_SP = "tasklistTextSize"
    val CONFIG_THEME = "theme"
}

object Callbacks {
    val ON_DISPLAY_NAME = "onDisplay"
    val ON_FILTER_NAME = "onFilter"
    val ON_GROUP_NAME = "onGroup"
    val ON_SORT_NAME = "onSort"
}

fun TclObject.put(interp: Interp, key: String, value: TclObject) {
    TclDict.put(interp, this, TclString.newInstance(key), value)
}

fun TclObject.put(interp: Interp, key: String, value: String) {
    this.put(interp, key, TclString.newInstance(value))
}
