package nl.mpcjanssen.simpletask

import android.content.Context
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.showToastShort
import nl.mpcjanssen.simpletask.util.toDateTime
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.*

object LuaScripting {
    private val log = Logger

    private val TAG = "LuaScripting"
    val globals = JsePlatform.standardGlobals()
    val ON_FILTER_NAME = "onFilter"
    val CONFIG_THEME = "theme"
    val CONFIG_TASKLIST_TEXT_SIZE_SP = "tasklistTextSize"

    fun init(context: Context) {
        globals.set("toast", LuaToastShort(context))
    }

    // Call a Lua function `name`
    // Use unpackResult to transform the resulting LuaValue to the expected return type `T`
    // Returns null if the function is not found or if a `LuaError` occurred
    fun <T> callZeroArgLuaFunction(name: String, unpackResult: (LuaValue) -> T?): T? {
        synchronized(this) {
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
    }

    fun tasklistTextSize(): Float? {
        return callZeroArgLuaFunction(CONFIG_TASKLIST_TEXT_SIZE_SP) { it -> it.tofloat() }
    }

    // Callback to determine the theme. Return true for datk.
    fun configTheme(): String? {
        return callZeroArgLuaFunction(CONFIG_THEME) { it -> it.toString() }
    }

    fun onFilterCallback (t : Task) : Boolean {
        synchronized(this) {
            val onFilter = globals.get(ON_FILTER_NAME)
            if (!onFilter.isnil()) {
                val args = fillOnFilterVarargs(t)
                try {
                    val result = onFilter.invoke(args).arg1()
                    return result.toboolean()
                } catch (e: LuaError) {
                    log.debug(TAG, "Lua execution failed " + e.message)
                }
            }
            return true
        }
    }

    fun evalScript(script: String) {
        synchronized(this) {
            globals.load(script).call()
        }
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
}

class LuaToastShort(val context: Context) : OneArgFunction() {
    override fun call(text: LuaValue?): LuaValue? {
        val string = text?.tojstring() ?: ""
        showToastShort(context, string)
        return LuaValue.NIL
    }
}