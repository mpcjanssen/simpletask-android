package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.*
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.*

object LuaInterpreter {
    val globals = JsePlatform.standardGlobals()!!
    private val log = Logger
    private val TAG = "LuaInterpreter"

    val ON_FILTER_NAME = "onFilter"
    val ON_TEXTSEARCH_NAME = "onTextSearch"
    val CONFIG_THEME = "theme"
    val CONFIG_TASKLIST_TEXT_SIZE_SP = "tasklistTextSize"

    init {

        try {
            globals.set("toast", LuaToastShort())
            evalScript(null, Config.luaConfig)

        } catch (e: LuaError) {
            nl.mpcjanssen.simpletask.util.log.warn(Config.TAG, "Lua execution failed " + e.message)
            showToastLong(TodoApplication.app, "${getString(R.string.lua_error)}:  ${e.message}")
        }

    }

    fun tasklistTextSize(): Float? {
        return callZeroArgLuaFunction(globals, CONFIG_TASKLIST_TEXT_SIZE_SP) { it -> it.tofloat() }
    }

    // Callback to determine the theme. Return true for datk.
    fun configTheme(): String? {
        return callZeroArgLuaFunction(globals, CONFIG_THEME) { it -> it.toString() }
    }


    fun onFilterCallback(moduleName : String, t: Task): Boolean {
        val module = globals.get(moduleName).checktable()
        if (module == LuaValue.NIL) {
            return true
        }
        val onFilter = module.get(LuaInterpreter.ON_FILTER_NAME)
        if (!onFilter.isnil()) {
            val args = fillOnFilterVarargs(t)
            try {
                val result = onFilter.call(args.arg1(), args.arg(2), args.arg(3))
                return result.toboolean()
            } catch (e: LuaError) {
                log.debug(TAG, "Lua execution failed " + e.message)
            }
        }
        return true
    }

    fun onTextSearchCallback(moduleName: String, input: String, search: String, caseSensitive: Boolean): Boolean? {
        val module = globals.get(moduleName)
        if (module == LuaValue.NIL) {
            return true
        }
        val onFilter = module.get(ON_TEXTSEARCH_NAME)
        if (!onFilter.isnil()) {
            try {
                val result = onFilter.invoke(LuaString.valueOf(input), LuaString.valueOf(search), LuaBoolean.valueOf(caseSensitive)).arg1()
                return result.toboolean()
            } catch (e: LuaError) {
                log.debug(TAG, "Lua execution failed " + e.message)
            }
        }
        return null
    }

    fun evalScript(moduleName : String?, script: String?) : LuaInterpreter {
        var module = globals.checktable()
        if (moduleName != null) {
            module = LuaTable.tableOf()
            val metatable = LuaValue.tableOf()
            metatable.set("__index", globals)
            module.setmetatable(metatable)
            globals.set(moduleName, module)
            script?.let { globals.load(script, moduleName, module).call() }
        } else {
            script?.let { globals.load(script).call() }
        }

        return this
    }

    // Fill the arguments for the onFilter callback
    fun fillOnFilterVarargs(t: Task): Varargs {
        val args = ArrayList<LuaValue>()
        args.add(LuaValue.valueOf(t.inFileFormat()))
        val fieldTable = LuaTable.tableOf()
        fieldTable.set("task", t.inFileFormat())

        fieldTable.set("due", dateStringToLuaLong(t.dueDate))
        fieldTable.set("threshold", dateStringToLuaLong(t.thresholdDate))
        fieldTable.set("createdate", dateStringToLuaLong(t.createDate))
        fieldTable.set("completiondate", dateStringToLuaLong(t.completionDate))

        val recPat = t.recurrencePattern
        if (recPat != null) {
            fieldTable.set("recurrence", t.recurrencePattern)
        }
        fieldTable.set("completed", LuaBoolean.valueOf(t.isCompleted()))
        fieldTable.set("priority", t.priority.code)

        fieldTable.set("tags", javaListToLuaTable(t.tags))
        fieldTable.set("lists", javaListToLuaTable(t.lists))

        args.add(fieldTable)

        // TODO: implement
        val extensionTable = LuaTable.tableOf()
        args.add(extensionTable)

        return LuaValue.varargsOf(args.toTypedArray())
    }

    fun dateStringToLuaLong(dateString: String?): LuaValue {
        dateString?.toDateTime()?.let {
            return LuaValue.valueOf((it.getMilliseconds(TimeZone.getDefault()) / 1000).toDouble())
        }
        return LuaValue.NIL
    }

    fun javaListToLuaTable(javaList: Iterable<String>): LuaValue {
        val size = javaList.count()
        val luaTable = LuaValue.tableOf()
        if (size == 0) return luaTable
        for (item in javaList) {
            luaTable.set(item, LuaValue.TRUE)
        }
        return luaTable
    }




        // Call a Lua function `name`
        // Use unpackResult to transform the resulting LuaValue to the expected return type `T`
        // Returns null if the function is not found or if a `LuaError` occurred
        fun <T> callZeroArgLuaFunction(globals: LuaValue, name: String, unpackResult: (LuaValue) -> T?): T? {
            val function = globals.get(name)
            if (!function.isnil()) {
                try {
                    return unpackResult(function.call())
                } catch (e: LuaError) {
                    log.debug(TAG, "Lua execution failed " + e.message)
                }
            }
            return null

        }


    fun clearOnFilter(moduleName: String) {
        val module = globals.get(moduleName)
        if (module != LuaValue.NIL) {
            module.set(ON_FILTER_NAME, LuaValue.NIL)
        }
    }
}

class LuaToastShort() : OneArgFunction() {
    override fun call(text: LuaValue?): LuaValue? {
        val string = text?.tojstring() ?: ""
        log.info(TAG, "Toast called $string")
        showToastShort(TodoApplication.app, string)
        return LuaValue.NIL
    }
}
