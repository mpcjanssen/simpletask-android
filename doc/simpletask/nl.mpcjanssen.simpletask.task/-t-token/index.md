[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [TToken](.)

# TToken

`interface TToken` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/task/Task.kt#L465)

### Properties

| [text](text.md) | `abstract val text: String` |
| [type](type.md) | `abstract val type: Int` |
| [value](value.md) | `abstract val value: Any?` |

### Companion Object Properties

| [ALL](-a-l-l.md) | `const val ALL: Int` |
| [COMPLETED](-c-o-m-p-l-e-t-e-d.md) | `const val COMPLETED: Int` |
| [COMPLETED_DATE](-c-o-m-p-l-e-t-e-d_-d-a-t-e.md) | `const val COMPLETED_DATE: Int` |
| [CREATION_DATE](-c-r-e-a-t-i-o-n_-d-a-t-e.md) | `const val CREATION_DATE: Int` |
| [DUE_DATE](-d-u-e_-d-a-t-e.md) | `const val DUE_DATE: Int` |
| [EXTENSION](-e-x-t-e-n-s-i-o-n.md) | `const val EXTENSION: Int` |
| [HIDDEN](-h-i-d-d-e-n.md) | `const val HIDDEN: Int` |
| [LINK](-l-i-n-k.md) | `const val LINK: Int` |
| [LIST](-l-i-s-t.md) | `const val LIST: Int` |
| [MAIL](-m-a-i-l.md) | `const val MAIL: Int` |
| [PHONE](-p-h-o-n-e.md) | `const val PHONE: Int` |
| [PRIO](-p-r-i-o.md) | `const val PRIO: Int` |
| [RECURRENCE](-r-e-c-u-r-r-e-n-c-e.md) | `const val RECURRENCE: Int` |
| [TEXT](-t-e-x-t.md) | `const val TEXT: Int` |
| [THRESHOLD_DATE](-t-h-r-e-s-h-o-l-d_-d-a-t-e.md) | `const val THRESHOLD_DATE: Int` |
| [TTAG](-t-t-a-g.md) | `const val TTAG: Int` |
| [WHITE_SPACE](-w-h-i-t-e_-s-p-a-c-e.md) | `const val WHITE_SPACE: Int` |

### Inheritors

| [CompletedToken](../-completed-token/index.md) | `data class CompletedToken : TToken` |
| [KeyValueToken](../-key-value-token/index.md) | `interface KeyValueToken : TToken` |
| [ListToken](../-list-token/index.md) | `data class ListToken : TToken` |
| [PriorityToken](../-priority-token/index.md) | `data class PriorityToken : TToken` |
| [StringValueToken](../-string-value-token/index.md) | `interface StringValueToken : TToken` |
| [TagToken](../-tag-token/index.md) | `data class TagToken : TToken` |

