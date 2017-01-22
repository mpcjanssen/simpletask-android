[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [Task](.)

# Task

`class Task` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/task/Task.kt#L35)

### Constructors

| [&lt;init&gt;](-init-.md) | `Task(text: String)`<br>`Task(text: String, defaultPrependedDate: String? = null)` |

### Properties

| [completionDate](completion-date.md) | `var completionDate: String?` |
| [createDate](create-date.md) | `var createDate: String?` |
| [dueDate](due-date.md) | `var dueDate: String?` |
| [extensions](extensions.md) | `val extensions: List<Pair<String, String>>` |
| [links](links.md) | `var links: Set<String>` |
| [lists](lists.md) | `var lists: `[`SortedSet`](http://docs.oracle.com/javase/6/docs/api/java/util/SortedSet.html)`<String>` |
| [mailAddresses](mail-addresses.md) | `var mailAddresses: Set<String>` |
| [phoneNumbers](phone-numbers.md) | `var phoneNumbers: Set<String>` |
| [priority](priority.md) | `var priority: `[`Priority`](../-priority/index.md) |
| [recurrencePattern](recurrence-pattern.md) | `var recurrencePattern: String?` |
| [tags](tags.md) | `var tags: `[`SortedSet`](http://docs.oracle.com/javase/6/docs/api/java/util/SortedSet.html)`<String>` |
| [text](text.md) | `val text: String` |
| [thresholdDate](threshold-date.md) | `var thresholdDate: String?` |
| [tokens](tokens.md) | `var tokens: List<`[`TToken`](../-t-token/index.md)`>` |

### Functions

| [addList](add-list.md) | `fun addList(listName: String): Unit` |
| [addTag](add-tag.md) | `fun addTag(tagName: String): Unit` |
| [deferDueDate](defer-due-date.md) | `fun deferDueDate(deferString: String, deferFromDate: String): Unit` |
| [deferThresholdDate](defer-threshold-date.md) | `fun deferThresholdDate(deferString: String, deferFromDate: String): Unit` |
| [equals](equals.md) | `fun equals(other: Any?): Boolean` |
| [getHeader](get-header.md) | `fun getHeader(sort: String, empty: String, createIsThreshold: Boolean): String` |
| [hashCode](hash-code.md) | `fun hashCode(): Int` |
| [inFileFormat](in-file-format.md) | `fun inFileFormat(): String` |
| [inFuture](in-future.md) | `fun inFuture(today: String): Boolean` |
| [isCompleted](is-completed.md) | `fun isCompleted(): Boolean` |
| [isHidden](is-hidden.md) | `fun isHidden(): Boolean` |
| [markComplete](mark-complete.md) | `fun markComplete(dateStr: String): Task?` |
| [markIncomplete](mark-incomplete.md) | `fun markIncomplete(): Unit` |
| [removeList](remove-list.md) | `fun removeList(list: String): Unit` |
| [removeTag](remove-tag.md) | `fun removeTag(tag: String): Unit` |
| [showParts](show-parts.md) | `fun showParts(parts: Int): String` |
| [update](update.md) | `fun update(rawText: String): Unit` |

### Companion Object Properties

| [DATE_FORMAT](-d-a-t-e_-f-o-r-m-a-t.md) | `const val DATE_FORMAT: String` |
| [TAG](-t-a-g.md) | `var TAG: String` |

### Companion Object Functions

| [parse](parse.md) | `fun parse(text: String): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`TToken`](../-t-token/index.md)`>` |

