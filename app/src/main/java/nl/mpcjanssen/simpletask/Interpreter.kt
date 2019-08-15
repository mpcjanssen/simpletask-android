package nl.mpcjanssen.simpletask


import android.util.Log
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TextToken
import nl.mpcjanssen.simpletask.util.*
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.*

object Interpreter :  AbstractInterpreter() {
    private val globals = JsePlatform.standardGlobals()!!
    const val tag = "LuaInterp"

    private val TODOLIB = readAsset(TodoApplication.app.assets, "lua/todolib.lua")

    init {
        globals.set("toast", LuaToastShort())
        globals.set("log", LuaLog())
        evalScript(null, TODOLIB)
        try {
            evalScript(null, TodoApplication.config.luaConfig)
        } catch (e: LuaError) {
            Log.w(TodoApplication.config.TAG, "Lua execution failed " + e.message)
            showToastLong(TodoApplication.app, "${getString(R.string.lua_error)}:  ${e.message}")
        }

    }

    override fun tasklistTextSize(): Float? {
        return callZeroArgLuaFunction(globals, CONFIG_TASKLIST_TEXT_SIZE_SP) { it -> it.tofloat() }
    }

    // Callback to determine the theme. Return true for datk.
    override fun configTheme(): String? {
        return callZeroArgLuaFunction(globals, CONFIG_THEME) { it -> it.toString() }
    }

    override fun onFilterCallback(moduleName: String, t: Task): Pair<Boolean, String> {
        val module = globals.get(moduleName).checktable()
        if (module == LuaValue.NIL) {
            return Pair(true, "true")
        }
        val onFilter = module.get(ON_FILTER_NAME)
        if (!onFilter.isnil()) {
            val args = fillOnFilterVarargs(t)
            try {
                val result = onFilter.call(args.arg1(), args.arg(2), args.arg(3))
                return Pair(result.toboolean(), result.toString())
            } catch (e: LuaError) {
                Log.d(TAG, "Lua execution failed " + e.message)
            }
        }
        return Pair(true, "true")
    }

    override fun hasFilterCallback(moduleName: String): Boolean {
        return try {
            val module = globals.get(moduleName).checktable() ?: globals
            !module.get(ON_FILTER_NAME).isnil()
        } catch (e: LuaError) {
            Log.e(TAG, "Lua error: ${e.message} )")
            false
        }
    }

    override fun hasOnSortCallback(moduleName: String): Boolean {
        return try {
            val module = globals.get(moduleName).checktable() ?: globals
            !module.get(ON_SORT_NAME).isnil()
        } catch (e: LuaError) {
            Log.e(TAG, "Lua error: ${e.message} )")
            false
        }
    }

    override fun hasOnGroupCallback(moduleName: String): Boolean {
        return try {
            val module = globals.get(moduleName).checktable() ?: globals
            !module.get(ON_GROUP_NAME).isnil()
        } catch (e: LuaError) {
            Log.e(TAG, "Lua error: ${e.message} )")
            false
        }
    }

    override fun onSortCallback(moduleName: String, t: Task): String {
        val module = try {
            globals.get(moduleName).checktable()
        } catch (e: LuaError) {
            Log.e(TAG, "Lua error: ${e.message} )")
            LuaValue.NIL
        }
        if (module == LuaValue.NIL) {
            return ""
        }
        val callback = module.get(ON_SORT_NAME)
        if (!callback.isnil()) {
            val args = fillOnFilterVarargs(t)
            try {
                val result = callback.call(args.arg1(), args.arg(2), args.arg(3))
                return result.tojstring()
            } catch (e: LuaError) {
                Log.d(TAG, "Lua execution failed " + e.message)
            }
        }
        return ""
    }

    override fun onGroupCallback(moduleName: String, t: Task): String? {
        val module = try {
            globals.get(moduleName).checktable()
        } catch (e: LuaError) {
            Log.e(TAG, "Lua error: ${e.message} )")
            LuaValue.NIL
        }
        if (module == LuaValue.NIL) {
            return null
        }
        val callback = module.get(ON_GROUP_NAME)
        if (!callback.isnil()) {
            val args = fillOnFilterVarargs(t)
            try {
                val result = callback.call(args.arg1(), args.arg(2), args.arg(3))
                return result.tojstring()
            } catch (e: LuaError) {
                Log.d(TAG, "Lua execution failed " + e.message)
            }
        }
        return null
    }

    override fun onDisplayCallback(moduleName: String, t: Task): String? {
        val module = try {
            globals.get(moduleName).checktable()
        } catch (e: LuaError) {
            Log.e(TAG, "Lua error: ${e.message} )")
            LuaValue.NIL
        }
        if (module == LuaValue.NIL) {
            return null
        }
        val callback = module.get(ON_DISPLAY_NAME)
        if (!callback.isnil()) {
            val args = fillOnFilterVarargs(t)
            try {
                val result = callback.call(args.arg1(), args.arg(2), args.arg(3))
                return result.tojstring()
            } catch (e: LuaError) {
                Log.d(TAG, "Lua execution failed " + e.message)
            }
        }
        return null
    }

    override fun onAddCallback(t: Task): Task? {
        val callback = globals.get(ON_ADD_NAME)
        if (!callback.isnil()) {
            val args = fillOnFilterVarargs(t)
            try {
                val result = callback.call(args.arg1(), args.arg(2), args.arg(3))
                return Task(result.tojstring())
            } catch (e: LuaError) {
                Log.d(TAG, "Lua execution failed " + e.message)
            }
        }
        return null
    }

    override fun onTextSearchCallback(moduleName: String, input: String, search: String, caseSensitive: Boolean): Boolean? {
        val module = try {
            globals.get(moduleName).checktable()
        } catch (e: LuaError) {
            Log.e(TAG, "Lua error: ${e.message} )")
            LuaValue.NIL
        }
        if (module == LuaValue.NIL) {
            return null
        }
        val onFilter = module.get(ON_TEXTSEARCH_NAME)
        if (!onFilter.isnil()) {
            try {
                val result = onFilter.invoke(LuaString.valueOf(input), LuaString.valueOf(search), LuaBoolean.valueOf(caseSensitive)).arg1()
                return result.toboolean()
            } catch (e: LuaError) {
                Log.d(TAG, "Lua execution failed " + e.message)
            }
        }
        return null
    }

    override fun evalScript(moduleName: String?, script: String?): AbstractInterpreter {
        if (moduleName != null) {
            val module = LuaTable.tableOf()
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
    private fun fillOnFilterVarargs(t: Task): Varargs {
        val args = ArrayList<LuaValue>()
        args.add(LuaValue.valueOf(t.inFileFormat(TodoApplication.config.useUUIDs)))
        val fieldTable = LuaTable.tableOf()
        fieldTable.set("due", dateStringToLuaLong(t.dueDate))
        fieldTable.set("threshold", dateStringToLuaLong(t.thresholdDate))
        fieldTable.set("createdate", dateStringToLuaLong(t.createDate))
        fieldTable.set("completiondate", dateStringToLuaLong(t.completionDate))
        fieldTable.set("text", t.alphaParts)

        val recPat = t.recurrencePattern
        if (recPat != null) {
            fieldTable.set("recurrence", t.recurrencePattern)
        }
        fieldTable.set("completed", LuaBoolean.valueOf(t.isCompleted()))

        val prioCode = t.priority.code
        if (prioCode != "-") {
            fieldTable.set("priority", prioCode)
        }
        fieldTable.set("tags", javaListToLuaTable(t.tags))
        fieldTable.set("lists", javaListToLuaTable(t.lists))

        args.add(fieldTable)

        val extensionTable = LuaTable.tableOf()
        for ((key, value) in t.extensions) {
            extensionTable.set(key, value)
        }
        args.add(extensionTable)

        return LuaValue.varargsOf(args.toTypedArray())
    }

    private fun dateStringToLuaLong(dateString: String?): LuaValue {
        dateString?.toDateTime()?.let {
            return LuaValue.valueOf((it.getMilliseconds(TimeZone.getDefault()) / 1000).toDouble())
        }
        return LuaValue.NIL
    }

    private fun javaListToLuaTable(javaList: Iterable<String>?): LuaValue {
        val luaTable = LuaValue.tableOf()
        javaList?.forEach {
            luaTable.set(it, LuaValue.TRUE)
        }
        return luaTable
    }

    // Call a Lua function `name`
    // Use unpackResult to transform the resulting LuaValue to the expected return type `T`
    // Returns null if the function is not found or if a `LuaError` occurred
    private fun <T> callZeroArgLuaFunction(globals: LuaValue, name: String, unpackResult: (LuaValue) -> T?): T? {
        val function = globals.get(name)
        if (!function.isnil()) {
            try {
                return unpackResult(function.call())
            } catch (e: LuaError) {
                Log.d(TAG, "Lua execution failed " + e.message)
            }
        }
        return null

    }

    override fun clearOnFilter(moduleName: String) {
        val module = globals.get(moduleName)
        if (module != LuaValue.NIL) {
            module.set(ON_FILTER_NAME, LuaValue.NIL)
        }
    }


}

class LuaToastShort : OneArgFunction() {
    override fun call(text: LuaValue?): LuaValue? {
        val string = text?.tojstring() ?: ""
        Log.i(Interpreter.tag, "Toasted: \"$string\"")
        showToastShort(TodoApplication.app, string)
        return LuaValue.NIL
    }
}

class LuaLog : OneArgFunction() {
    override fun call(text: LuaValue?): LuaValue? {
        val string = text?.tojstring() ?: ""
        Log.i(Interpreter.tag, string)
        return LuaValue.NIL
    }
}