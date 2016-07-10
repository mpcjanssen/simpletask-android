package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.Task

import nl.mpcjanssen.simpletask.util.toDateTime
import org.luaj.vm2.*
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.*

object LuaScripting {
    private val log = Logger
    private val TAG = "LuaCallback"
    val interp = JsePlatform.standardGlobals()
    val ON_FILTER_NAME = "onFilter"

    fun onFilterCallback (t : Task) : Boolean {
        synchronized(this) {
            val onFilter = interp.get(ON_FILTER_NAME)
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
            interp.load(script).call();
        }
    }

    // Fill the arguments for the onFilter callback
    fun fillOnFilterVarargs(t: Task): Varargs {
        val args = ArrayList<LuaValue>();
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
        if (size == 0) return LuaValue.NIL
        val luaTable = LuaValue.tableOf()
        var i = 0
        for (item in javaList) {
            luaTable.set(item, LuaValue.TRUE)
        }
        return luaTable
    }
}