package nl.mpcjanssen.simpletask

import android.util.Log
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

    val interps = HashMap<String,Interp>()

    fun getInterp(moduleName: String) : Interp {
            return interps[moduleName] ?: Interp().also {
                try {
                    it.init(moduleName == "")
                } catch (e: TclException) {
                    Log.e(TAG, "Interper $moduleName failed to initialize ${it.result.toString()}")
                }
                interps[moduleName] = it }
    }
    val mainInterp : Interp by lazy { getInterp("")}

    override fun tasklistTextSize(): Float? {
        return mainInterp.tasklistTextSize()?.toFloat()
    }

    override fun configTheme(): String? {
        return mainInterp.configTheme()
    }

    override fun onFilterCallback(moduleName: String, t: Task): Pair<Boolean, String> {
        return getInterp(moduleName).onFilterCallback(t)
    }

    override fun hasFilterCallback(moduleName: String): Boolean {
        return getInterp(moduleName).hasOnFilterCallback()
    }

    override fun hasOnSortCallback(moduleName: String): Boolean {
        return getInterp(moduleName).hasOnSortCallback()
    }

    override fun hasOnGroupCallback(moduleName: String): Boolean {
        return getInterp(moduleName).hasOnGroupCallback()
    }

    override fun onSortCallback(moduleName: String, t: Task): String {
        return getInterp(moduleName).onSortCallback(t)
    }

    override fun onGroupCallback(moduleName: String, t: Task): String? {
        return getInterp(moduleName).onGroupCallback(t)
    }

    override fun onDisplayCallback(moduleName: String, t: Task): String? {
        return getInterp(moduleName).onDisplayCallback(t)
    }

    override fun onAddCallback(t: Task): Task? {
        return mainInterp.onAddCallback(t)
    }

    override fun onTextSearchCallback(moduleName: String, input: String, search: String, caseSensitive: Boolean): Boolean? {
        return true
    }

    override fun evalScript(moduleName: String, script: String?): AbstractInterpreter {
        getInterp(moduleName).eval(script)
        return this
    }

    override fun clearOnFilter(moduleName: String) {
        getInterp(moduleName).eval("rename $ON_FILTER_NAME {}")
    }

    override fun getStackTrace(moduleName: String): String? {
        return getInterp(moduleName).result.toString()
    }

}


fun Interp.init(main : Boolean = false) : Interp  {
    val TODOLIB = readAsset(TodoApplication.app.assets, "tcl/todolib.tcl")
    this.createCommand("toast", ToastShortCmd())
    this.eval(TODOLIB)
    if (main)  this.eval(Config.luaConfig)
    return this
}


fun Interp.tasklistTextSize(): Double? {
    try {
        return this.getVar(Vars.CONFIG_TASKLIST_TEXT_SIZE_SP, TCL.GLOBAL_ONLY)?.let {
            TclDouble.get(this, it)
        }
    } catch (e: TclVarException) {
        return null
    }
}


// Callback to determine the theme. Return true for dark.


fun Interp.configTheme(): String? {
    try {
        return this.getVar(Vars.CONFIG_THEME, TCL.GLOBAL_ONLY)?.toString()
    } catch (e: TclVarException) {
        return null
    }
}


fun Interp.hasOnFilterCallback(): Boolean {
    val cmd = this.getCommand(Callbacks.ON_FILTER_NAME)
    return cmd != null
}

fun Interp.hasOnSortCallback(): Boolean {
    val cmd = this.getCommand(Callbacks.ON_SORT_NAME)
    return cmd != null
}

fun Interp.hasOnDisplayCallback(): Boolean {
    val cmd = this.getCommand(Callbacks.ON_DISPLAY_NAME)
    return cmd != null
}

fun Interp.hasOnGroupCallback(): Boolean {
    val cmd = this.getCommand(Callbacks.ON_GROUP_NAME)
    return cmd != null
}

fun Interp.onFilterCallback(t: Task): Pair<Boolean, String> {
    if (!hasOnFilterCallback()) {
        return Pair(true, "true")
    }

    executeCallbackCommand(Callbacks.ON_FILTER_NAME, t)
    val strVal = this.result.toString()
    val boolVal = TclBoolean.get(this, this.result)
    return Pair(boolVal,strVal)

}

fun Interp.onSortCallback(t1: Task): String {

    val cmdList = TclList.newInstance()
    TclList.append(this, cmdList, TclString.newInstance(Callbacks.ON_SORT_NAME))
    appendCallbackArgs(t1, cmdList)
    this.eval(cmdList, TCL.GLOBAL_ONLY)
    return this.result.toString()

}

fun Interp.onGroupCallback(t: Task): String? {
    if (!hasOnGroupCallback()) {
        return null
    }

    executeCallbackCommand(Callbacks.ON_GROUP_NAME, t)
    return this.result.toString()

}

fun Interp.onDisplayCallback(t: Task): String? {
    if (!hasOnDisplayCallback()) {
        return null
    }
    executeCallbackCommand(Callbacks.ON_DISPLAY_NAME, t)
    return this.result.toString()

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


private fun Interp.javaListToTclList(javaList: Iterable<String>?): TclObject {
    val tclList = TclList.newInstance()
    javaList?.forEach() {
        TclList.append(this, tclList, TclString.newInstance(it))
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
