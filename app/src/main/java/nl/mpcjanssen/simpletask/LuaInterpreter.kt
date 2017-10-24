package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.*
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.*

object LuaInterpreter {
    private val globals = JsePlatform.standardGlobals()!!
    private val log = Logger
    private val TAG = "LuaInterpreter"

    val ON_DISPLAY_NAME = "onDisplay"
    val ON_FILTER_NAME = "onFilter"
    val ON_GROUP_NAME = "onGroup"
    val ON_TEXTSEARCH_NAME = "onTextSearch"
    val CONFIG_THEME = "theme"
    val CONFIG_TASKLIST_TEXT_SIZE_SP = "tasklistTextSize"
    val STDLIB = """
        function print_table(node)
    -- to make output beautiful
    local function tab(amt)
        local str = ""
        for i=1,amt do
            str = str .. "\t"
        end
        return str
    end

    local cache, stack, output = {},{},{}
    local depth = 1
    local output_str = "{\n"

    while true do
        local size = 0
        for k,v in pairs(node) do
            size = size + 1
        end

        local cur_index = 1
        for k,v in pairs(node) do
            if (cache[node] == nil) or (cur_index >= cache[node]) then

                if (string.find(output_str,"}",output_str:len())) then
                    output_str = output_str .. ",\n"
                elseif not (string.find(output_str,"\n",output_str:len())) then
                    output_str = output_str .. "\n"
                end

                -- This is necessary for working with HUGE tables otherwise we run out of memory using concat on huge strings
                table.insert(output,output_str)
                output_str = ""

                local key
                if (type(k) == "number" or type(k) == "boolean") then
                    key = "["..tostring(k).."]"
                else
                    key = "['"..tostring(k).."']"
                end

                if (type(v) == "number" or type(v) == "boolean") then
                    output_str = output_str .. tab(depth) .. key .. " = "..tostring(v)
                elseif (type(v) == "table") then
                    output_str = output_str .. tab(depth) .. key .. " = {\n"
                    table.insert(stack,node)
                    table.insert(stack,v)
                    cache[node] = cur_index+1
                    break
                else
                    output_str = output_str .. tab(depth) .. key .. " = '"..tostring(v).."'"
                end

                if (cur_index == size) then
                    output_str = output_str .. "\n" .. tab(depth-1) .. "}"
                else
                    output_str = output_str .. ","
                end
            else
                -- close the table
                if (cur_index == size) then
                    output_str = output_str .. "\n" .. tab(depth-1) .. "}"
                end
            end

            cur_index = cur_index + 1
        end

        if (size == 0) then
            output_str = output_str .. "\n" .. tab(depth-1) .. "}"
        end

        if (#stack > 0) then
            node = stack[#stack]
            stack[#stack] = nil
            depth = cache[node] == nil and depth + 1 or depth - 1
        else
            break
        end
    end

    -- This is necessary for working with HUGE tables otherwise we run out of memory using concat on huge strings
    table.insert(output,output_str)
    output_str = table.concat(output)

    return output_str
end
            """

    init {

        try {
            globals.set("toast", LuaToastShort())
            evalScript(null,STDLIB)
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

    fun hasFilterCallback(moduleName : String) : Boolean {
        return try {
            val module = globals.get(moduleName).checktable() ?: globals
            !module.get(LuaInterpreter.ON_FILTER_NAME).isnil()
        } catch (e: LuaError) {
            Logger.error(TAG, "Lua error: ${e.message} )")
            false
        }
    }

    fun onGroupCallback (moduleName : String, t: Task): String? {
        val module = globals.get(moduleName).checktable()
        if (module == LuaValue.NIL) {
            return null
        }
        val callback = module.get(LuaInterpreter.ON_GROUP_NAME)
        if (!callback.isnil()) {
            val args = fillOnFilterVarargs(t)
            try {
                val result = callback.call(args.arg1(), args.arg(2), args.arg(3))
                return result.tojstring()
            } catch (e: LuaError) {
                log.debug(TAG, "Lua execution failed " + e.message)
            }
        }
        return null
    }

    fun onDisplayCallback (moduleName : String, t: Task): String? {
        val module = globals.get(moduleName).checktable()
        if (module == LuaValue.NIL) {
            return null
        }
        val callback = module.get(LuaInterpreter.ON_DISPLAY_NAME)
        if (!callback.isnil()) {
            val args = fillOnFilterVarargs(t)
            try {
                val result = callback.call(args.arg1(), args.arg(2), args.arg(3))
                return result.tojstring()
            } catch (e: LuaError) {
                log.debug(TAG, "Lua execution failed " + e.message)
            }
        }
        return null
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
        args.add(LuaValue.valueOf(t.inFileFormat()))
        val fieldTable = LuaTable.tableOf()
        val tokensTable =  LuaTable.tableOf()
        fieldTable.set("task", t.inFileFormat())
        t.tokens.forEachIndexed { idx,  tok ->
            val luaIdx = idx +1
            val tokenTable = LuaTable.tableOf().apply {
                set("type", LuaValue.valueOf(tok.type))
                set("text", LuaValue.valueOf(tok.text))
            }
            tokensTable.set(luaIdx, tokenTable)
        }
        fieldTable.set("tokens", tokensTable)
        fieldTable.set("due", dateStringToLuaLong(t.dueDate))
        fieldTable.set("threshold", dateStringToLuaLong(t.thresholdDate))
        fieldTable.set("createdate", dateStringToLuaLong(t.createDate))
        fieldTable.set("completiondate", dateStringToLuaLong(t.completionDate))
        fieldTable.set("text", t.showParts(TToken.TEXT))

        val recPat = t.recurrencePattern
        if (recPat != null) {
            fieldTable.set("recurrence", t.recurrencePattern)
        }
        fieldTable.set("completed", LuaBoolean.valueOf(t.isCompleted()))
        fieldTable.set("priority", t.priority.code)

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

    private fun javaListToLuaTable(javaList: Iterable<String>): LuaValue {
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
        private fun <T> callZeroArgLuaFunction(globals: LuaValue, name: String, unpackResult: (LuaValue) -> T?): T? {
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

class LuaToastShort : OneArgFunction() {
    override fun call(text: LuaValue?): LuaValue? {
        val string = text?.tojstring() ?: ""
        log.info(TAG, "Toast called $string")
        showToastShort(TodoApplication.app, string)
        return LuaValue.NIL
    }
}
