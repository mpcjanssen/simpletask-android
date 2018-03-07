package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.Task

object Interpreter : AbstractInterpreter() {
    override fun tasklistTextSize(): Float? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun configTheme(): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onFilterCallback(moduleName: String, t: Task): Pair<Boolean, String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasFilterCallback(moduleName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasOnSortCallback(moduleName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasOnGroupCallback(moduleName: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSortCallback(moduleName: String, t: Task): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onGroupCallback(moduleName: String, t: Task): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDisplayCallback(moduleName: String, t: Task): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAddCallback(t: Task): Task? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTextSearchCallback(moduleName: String, input: String, search: String, caseSensitive: Boolean): Boolean? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun evalScript(moduleName: String?, script: String?): AbstractInterpreter {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearOnFilter(moduleName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
