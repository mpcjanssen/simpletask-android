package nl.mpcjanssen.simpletask.lua

import junit.framework.Assert
import junit.framework.TestCase
import nl.mpcjanssen.simpletask.sort.AlphabeticalComparator
import nl.mpcjanssen.simpletask.task.Task
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.*


class TableTest : TestCase() {
    fun testGlobalFunctionCall() {
        val globals = JsePlatform.standardGlobals()!!
        globals.load("function test() ; return true ; end").call()
        val res = globals.get("test").call()
        Assert.assertTrue(res.toboolean())
        val moduleTable = LuaTable.tableOf()
        var error = false
        try {
            globals.load("test()", "module", moduleTable).call()
        } catch (e: LuaError) {
            error = true
        }
        assertTrue(error)
        val metatable = LuaValue.tableOf()
        metatable.set("__index", globals)
        moduleTable.setmetatable(metatable)
        globals.load("function onCallBack () ; test () ; return true ; end", "module", moduleTable).call()
        val callBack = moduleTable.get("onCallBack")
        Assert.assertNotSame(callBack, LuaValue.NIL)
        assertTrue(moduleTable.get("onCallBack").call().toboolean())
        globals.load("function onCallBack () ; test () ; return os.time() ; end", "module", moduleTable).call()
        Assert.assertNotSame(moduleTable.get("onCallBack").call().tolong(), 0L)
    }
}