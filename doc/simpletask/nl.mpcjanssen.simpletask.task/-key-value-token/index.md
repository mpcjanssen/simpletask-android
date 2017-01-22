[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [KeyValueToken](.)

# KeyValueToken

`interface KeyValueToken : `[`TToken`](../-t-token/index.md) [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/task/Task.kt#L544)

### Properties

| [key](key.md) | `abstract val key: String` |
| [text](text.md) | `open val text: String` |
| [value](value.md) | `open val value: Any?` |
| [valueStr](value-str.md) | `abstract val valueStr: String` |

### Inherited Properties

| [type](../-t-token/type.md) | `abstract val type: Int` |

### Inheritors

| [DueDateToken](../-due-date-token/index.md) | `data class DueDateToken : KeyValueToken` |
| [ExtToken](../-ext-token/index.md) | `data class ExtToken : KeyValueToken` |
| [HiddenToken](../-hidden-token/index.md) | `data class HiddenToken : KeyValueToken` |
| [RecurrenceToken](../-recurrence-token/index.md) | `data class RecurrenceToken : KeyValueToken` |
| [ThresholdDateToken](../-threshold-date-token/index.md) | `data class ThresholdDateToken : KeyValueToken` |

