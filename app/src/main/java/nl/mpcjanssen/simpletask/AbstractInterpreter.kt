package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.Task

abstract class AbstractInterpreter {
    val ON_DISPLAY_NAME = "onDisplay"
    val ON_ADD_NAME = "onAdd"
    val ON_FILTER_NAME = "onFilter"
    val ON_GROUP_NAME = "onGroup"
    val ON_TEXTSEARCH_NAME = "onTextSearch"
    val ON_SORT_NAME = "onSort"
    val CONFIG_THEME = "theme"
    val CONFIG_TASKLIST_TEXT_SIZE_SP = "tasklistTextSize"
    abstract fun tasklistTextSize(): Float?
    // Callback to determine the theme. Return true for datk.
    abstract fun configTheme(): String?

    abstract fun onFilterCallback(moduleName: String, t: Task): Pair<Boolean, String>
    abstract fun hasFilterCallback(moduleName: String): Boolean
    abstract fun hasOnSortCallback(moduleName: String): Boolean
    abstract fun hasOnGroupCallback(moduleName: String): Boolean
    abstract fun onSortCallback(moduleName: String, t: Task): String
    abstract fun onGroupCallback(moduleName: String, t: Task): String?
    abstract fun onDisplayCallback(moduleName: String, t: Task): String?
    abstract fun onAddCallback(t: Task): Task?
    abstract fun onTextSearchCallback(moduleName: String, input: String, search: String, caseSensitive: Boolean): Boolean?
    abstract fun evalScript(moduleName: String?, script: String?): AbstractInterpreter
    abstract fun clearOnFilter(moduleName: String)
}