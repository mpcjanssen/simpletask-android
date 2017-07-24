[app](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [LuaInterpreter](.)

# LuaInterpreter

`object LuaInterpreter`

### Properties

| Name | Summary |
|---|---|
| [CONFIG_TASKLIST_TEXT_SIZE_SP](-c-o-n-f-i-g_-t-a-s-k-l-i-s-t_-t-e-x-t_-s-i-z-e_-s-p.md) | `val CONFIG_TASKLIST_TEXT_SIZE_SP: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [CONFIG_THEME](-c-o-n-f-i-g_-t-h-e-m-e.md) | `val CONFIG_THEME: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ON_FILTER_NAME](-o-n_-f-i-l-t-e-r_-n-a-m-e.md) | `val ON_FILTER_NAME: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ON_GROUP_NAME](-o-n_-g-r-o-u-p_-n-a-m-e.md) | `val ON_GROUP_NAME: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ON_TEXTSEARCH_NAME](-o-n_-t-e-x-t-s-e-a-r-c-h_-n-a-m-e.md) | `val ON_TEXTSEARCH_NAME: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [globals](globals.md) | `val globals: Globals` |

### Functions

| Name | Summary |
|---|---|
| [callZeroArgLuaFunction](call-zero-arg-lua-function.md) | `fun <T> callZeroArgLuaFunction(globals: LuaValue, name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, unpackResult: (LuaValue) -> T?): T?` |
| [clearOnFilter](clear-on-filter.md) | `fun clearOnFilter(moduleName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [configTheme](config-theme.md) | `fun configTheme(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
| [dateStringToLuaLong](date-string-to-lua-long.md) | `fun dateStringToLuaLong(dateString: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?): LuaValue` |
| [evalScript](eval-script.md) | `fun evalScript(moduleName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?, script: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?): LuaInterpreter` |
| [fillOnFilterVarargs](fill-on-filter-varargs.md) | `fun fillOnFilterVarargs(t: `[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`): Varargs` |
| [hasFilterCallback](has-filter-callback.md) | `fun hasFilterCallback(moduleName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [javaListToLuaTable](java-list-to-lua-table.md) | `fun javaListToLuaTable(javaList: `[`Iterable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): LuaValue` |
| [onFilterCallback](on-filter-callback.md) | `fun onFilterCallback(moduleName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, t: `[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onGroupCallback](on-group-callback.md) | `fun onGroupCallback(moduleName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, t: `[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
| [onTextSearchCallback](on-text-search-callback.md) | `fun onTextSearchCallback(moduleName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, input: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, search: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, caseSensitive: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`?` |
| [tasklistTextSize](tasklist-text-size.md) | `fun tasklistTextSize(): `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`?` |
