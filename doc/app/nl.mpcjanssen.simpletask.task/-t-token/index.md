[app](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [TToken](.)

# TToken

`interface TToken`

### Properties

| Name | Summary |
|---|---|
| [text](text.md) | `abstract val text: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [type](type.md) | `abstract val type: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [value](value.md) | `abstract val value: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?` |

### Companion Object Properties

| Name | Summary |
|---|---|
| [ALL](-a-l-l.md) | `const val ALL: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [COMPLETED](-c-o-m-p-l-e-t-e-d.md) | `const val COMPLETED: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [COMPLETED_DATE](-c-o-m-p-l-e-t-e-d_-d-a-t-e.md) | `const val COMPLETED_DATE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [CREATION_DATE](-c-r-e-a-t-i-o-n_-d-a-t-e.md) | `const val CREATION_DATE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [DUE_DATE](-d-u-e_-d-a-t-e.md) | `const val DUE_DATE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [EXTENSION](-e-x-t-e-n-s-i-o-n.md) | `const val EXTENSION: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [HIDDEN](-h-i-d-d-e-n.md) | `const val HIDDEN: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [LINK](-l-i-n-k.md) | `const val LINK: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [LIST](-l-i-s-t.md) | `const val LIST: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [MAIL](-m-a-i-l.md) | `const val MAIL: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [PHONE](-p-h-o-n-e.md) | `const val PHONE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [PRIO](-p-r-i-o.md) | `const val PRIO: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [RECURRENCE](-r-e-c-u-r-r-e-n-c-e.md) | `const val RECURRENCE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [TEXT](-t-e-x-t.md) | `const val TEXT: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [THRESHOLD_DATE](-t-h-r-e-s-h-o-l-d_-d-a-t-e.md) | `const val THRESHOLD_DATE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [TTAG](-t-t-a-g.md) | `const val TTAG: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [WHITE_SPACE](-w-h-i-t-e_-s-p-a-c-e.md) | `const val WHITE_SPACE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [CompletedToken](../-completed-token/index.md) | `data class CompletedToken : TToken` |
| [KeyValueToken](../-key-value-token/index.md) | `interface KeyValueToken : TToken` |
| [ListToken](../-list-token/index.md) | `data class ListToken : TToken` |
| [PriorityToken](../-priority-token/index.md) | `data class PriorityToken : TToken` |
| [StringValueToken](../-string-value-token/index.md) | `interface StringValueToken : TToken` |
| [TagToken](../-tag-token/index.md) | `data class TagToken : TToken` |
