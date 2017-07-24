[app](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [StringValueToken](.)

# StringValueToken

`interface StringValueToken : `[`TToken`](../-t-token/index.md)

### Properties

| Name | Summary |
|---|---|
| [value](value.md) | `open val value: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Inherited Properties

| Name | Summary |
|---|---|
| [text](../-t-token/text.md) | `abstract val text: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [type](../-t-token/type.md) | `abstract val type: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [CompletedDateToken](../-completed-date-token/index.md) | `data class CompletedDateToken : StringValueToken` |
| [CreateDateToken](../-create-date-token/index.md) | `data class CreateDateToken : StringValueToken` |
| [LinkToken](../-link-token/index.md) | `data class LinkToken : StringValueToken` |
| [MailToken](../-mail-token/index.md) | `data class MailToken : StringValueToken` |
| [PhoneToken](../-phone-token/index.md) | `data class PhoneToken : StringValueToken` |
| [TextToken](../-text-token/index.md) | `data class TextToken : StringValueToken` |
| [WhiteSpaceToken](../-white-space-token/index.md) | `data class WhiteSpaceToken : StringValueToken` |
