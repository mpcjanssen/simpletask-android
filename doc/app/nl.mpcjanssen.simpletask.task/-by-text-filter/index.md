[app](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [ByTextFilter](.)

# ByTextFilter

`class ByTextFilter : `[`TaskFilter`](../-task-filter/index.md)

A filter that matches Tasks containing the specified text

**Author**
Tim Barlotta

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ByTextFilter(moduleName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, searchText: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?, isCaseSensitive: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)`<br>A filter that matches Tasks containing the specified text |

### Properties

| Name | Summary |
|---|---|
| [moduleName](module-name.md) | `val moduleName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [text](text.md) | `val text: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Functions

| Name | Summary |
|---|---|
| [apply](apply.md) | `fun apply(task: `[`Task`](../-task/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
