[app](../index.md) / [nl.mpcjanssen.simpletask.task](.)

## Package nl.mpcjanssen.simpletask.task

### Types

| Name | Summary |
|---|---|
| [ByContextFilter](-by-context-filter/index.md) | `class ByContextFilter : `[`TaskFilter`](-task-filter/index.md)<br>A filter that matches Tasks containing the specified contexts |
| [ByPriorityFilter](-by-priority-filter/index.md) | `class ByPriorityFilter : `[`TaskFilter`](-task-filter/index.md)<br>A filter that matches Tasks containing the specified priorities |
| [ByProjectFilter](-by-project-filter/index.md) | `class ByProjectFilter : `[`TaskFilter`](-task-filter/index.md)<br>A filter that matches Tasks containing the specified projects |
| [ByTextFilter](-by-text-filter/index.md) | `class ByTextFilter : `[`TaskFilter`](-task-filter/index.md)<br>A filter that matches Tasks containing the specified text |
| [CompletedDateToken](-completed-date-token/index.md) | `data class CompletedDateToken : `[`StringValueToken`](-string-value-token/index.md) |
| [CompletedToken](-completed-token/index.md) | `data class CompletedToken : `[`TToken`](-t-token/index.md) |
| [CreateDateToken](-create-date-token/index.md) | `data class CreateDateToken : `[`StringValueToken`](-string-value-token/index.md) |
| [DueDateToken](-due-date-token/index.md) | `data class DueDateToken : `[`KeyValueToken`](-key-value-token/index.md) |
| [ExtToken](-ext-token/index.md) | `data class ExtToken : `[`KeyValueToken`](-key-value-token/index.md) |
| [HiddenToken](-hidden-token/index.md) | `data class HiddenToken : `[`KeyValueToken`](-key-value-token/index.md) |
| [KeyValueToken](-key-value-token/index.md) | `interface KeyValueToken : `[`TToken`](-t-token/index.md) |
| [LinkToken](-link-token/index.md) | `data class LinkToken : `[`StringValueToken`](-string-value-token/index.md) |
| [ListToken](-list-token/index.md) | `data class ListToken : `[`TToken`](-t-token/index.md) |
| [MailToken](-mail-token/index.md) | `data class MailToken : `[`StringValueToken`](-string-value-token/index.md) |
| [PhoneToken](-phone-token/index.md) | `data class PhoneToken : `[`StringValueToken`](-string-value-token/index.md) |
| [Priority](-priority/index.md) | `enum class Priority` |
| [PriorityToken](-priority-token/index.md) | `data class PriorityToken : `[`TToken`](-t-token/index.md) |
| [RecurrenceToken](-recurrence-token/index.md) | `data class RecurrenceToken : `[`KeyValueToken`](-key-value-token/index.md) |
| [StringValueToken](-string-value-token/index.md) | `interface StringValueToken : `[`TToken`](-t-token/index.md) |
| [TToken](-t-token/index.md) | `interface TToken` |
| [TagToken](-tag-token/index.md) | `data class TagToken : `[`TToken`](-t-token/index.md) |
| [Task](-task/index.md) | `class Task` |
| [TaskFilter](-task-filter/index.md) | `interface TaskFilter` |
| [TextToken](-text-token/index.md) | `data class TextToken : `[`StringValueToken`](-string-value-token/index.md) |
| [ThresholdDateToken](-threshold-date-token/index.md) | `data class ThresholdDateToken : `[`KeyValueToken`](-key-value-token/index.md) |
| [TodoList](-todo-list/index.md) | `object TodoList`<br>Implementation of the in memory representation of the Todo list uses an ActionQueue to ensure modifications and access of the underlying todo list are sequential. If this is not done properly the result is a likely ConcurrentModificationException. |
| [WhiteSpaceToken](-white-space-token/index.md) | `data class WhiteSpaceToken : `[`StringValueToken`](-string-value-token/index.md) |

### Extensions for External Classes

| Name | Summary |
|---|---|
| [kotlin.String](kotlin.-string/index.md) |  |
