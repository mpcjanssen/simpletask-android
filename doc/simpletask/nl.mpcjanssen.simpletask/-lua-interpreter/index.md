[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [LuaInterpreter](.)

# LuaInterpreter

`object LuaInterpreter` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/LuaInterpreter.kt#L10)

### Properties

| [CONFIG_TASKLIST_TEXT_SIZE_SP](-c-o-n-f-i-g_-t-a-s-k-l-i-s-t_-t-e-x-t_-s-i-z-e_-s-p.md) | `val CONFIG_TASKLIST_TEXT_SIZE_SP: String` |
| [CONFIG_THEME](-c-o-n-f-i-g_-t-h-e-m-e.md) | `val CONFIG_THEME: String` |
| [ON_FILTER_NAME](-o-n_-f-i-l-t-e-r_-n-a-m-e.md) | `val ON_FILTER_NAME: String` |
| [ON_GROUP_NAME](-o-n_-g-r-o-u-p_-n-a-m-e.md) | `val ON_GROUP_NAME: String` |
| [ON_TEXTSEARCH_NAME](-o-n_-t-e-x-t-s-e-a-r-c-h_-n-a-m-e.md) | `val ON_TEXTSEARCH_NAME: String` |
| [globals](globals.md) | `val globals: Globals` |

### Functions

| [callZeroArgLuaFunction](call-zero-arg-lua-function.md) | `fun <T> callZeroArgLuaFunction(globals: LuaValue, name: String, unpackResult: (LuaValue) -> T?): T?` |
| [clearOnFilter](clear-on-filter.md) | `fun clearOnFilter(moduleName: String): Unit` |
| [configTheme](config-theme.md) | `fun configTheme(): String?` |
| [dateStringToLuaLong](date-string-to-lua-long.md) | `fun dateStringToLuaLong(dateString: String?): LuaValue` |
| [evalScript](eval-script.md) | `fun evalScript(moduleName: String?, script: String?): LuaInterpreter` |
| [fillOnFilterVarargs](fill-on-filter-varargs.md) | `fun fillOnFilterVarargs(t: `[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`): Varargs` |
| [javaListToLuaTable](java-list-to-lua-table.md) | `fun javaListToLuaTable(javaList: Iterable<String>): LuaValue` |
| [onFilterCallback](on-filter-callback.md) | `fun onFilterCallback(moduleName: String, t: `[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`): Boolean` |
| [onGroupCallback](on-group-callback.md) | `fun onGroupCallback(moduleName: String, t: `[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`): String?` |
| [onTextSearchCallback](on-text-search-callback.md) | `fun onTextSearchCallback(moduleName: String, input: String, search: String, caseSensitive: Boolean): Boolean?` |
| [tasklistTextSize](tasklist-text-size.md) | `fun tasklistTextSize(): Float?` |

