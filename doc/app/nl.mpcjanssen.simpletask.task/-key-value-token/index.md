[app](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [KeyValueToken](.)

# KeyValueToken

`interface KeyValueToken : `[`TToken`](../-t-token/index.md)

### Properties

| Name | Summary |
|---|---|
| [key](key.md) | `abstract val key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [text](text.md) | `open val text: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [value](value.md) | `open val value: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?` |
| [valueStr](value-str.md) | `abstract val valueStr: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Inherited Properties

| Name | Summary |
|---|---|
| [type](../-t-token/type.md) | `abstract val type: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [DueDateToken](../-due-date-token/index.md) | `data class DueDateToken : KeyValueToken` |
| [ExtToken](../-ext-token/index.md) | `data class ExtToken : KeyValueToken` |
| [HiddenToken](../-hidden-token/index.md) | `data class HiddenToken : KeyValueToken` |
| [RecurrenceToken](../-recurrence-token/index.md) | `data class RecurrenceToken : KeyValueToken` |
| [ThresholdDateToken](../-threshold-date-token/index.md) | `data class ThresholdDateToken : KeyValueToken` |
