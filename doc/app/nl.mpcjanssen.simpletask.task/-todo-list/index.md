[app](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [TodoList](.)

# TodoList

`object TodoList`

Implementation of the in memory representation of the Todo list
uses an ActionQueue to ensure modifications and access of the underlying todo list are
sequential. If this is not done properly the result is a likely ConcurrentModificationException.

**Author**
Mark Janssen

### Properties

| Name | Summary |
|---|---|
| [completedTasks](completed-tasks.md) | `var completedTasks: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>` |
| [contexts](contexts.md) | `val contexts: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [decoratedContexts](decorated-contexts.md) | `val decoratedContexts: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [decoratedProjects](decorated-projects.md) | `val decoratedProjects: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [pendingEdits](pending-edits.md) | `val pendingEdits: `[`LinkedHashSet`](http://docs.oracle.com/javase/6/docs/api/java/util/LinkedHashSet.html)`<`[`Task`](../-task/index.md)`>` |
| [priorities](priorities.md) | `val priorities: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`Priority`](../-priority/index.md)`>` |
| [projects](projects.md) | `val projects: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [selectedItems](selected-items.md) | `val selectedItems: `[`CopyOnWriteArraySet`](http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/CopyOnWriteArraySet.html)`<`[`Task`](../-task/index.md)`>` |
| [selectedTasks](selected-tasks.md) | `var selectedTasks: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>` |
| [todoItems](todo-items.md) | `val todoItems: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`Task`](../-task/index.md)`>` |

### Functions

| Name | Summary |
|---|---|
| [add](add.md) | `fun add(items: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>, atEnd: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>`fun add(t: `[`Task`](../-task/index.md)`, atEnd: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [archive](archive.md) | `fun archive(todoFilename: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, doneFileName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, tasks: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>?, eol: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [clearPendingEdits](clear-pending-edits.md) | `fun clearPendingEdits(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [clearSelection](clear-selection.md) | `fun clearSelection(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [complete](complete.md) | `fun complete(tasks: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>, keepPrio: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`, extraAtEnd: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [defer](defer.md) | `fun defer(deferString: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, tasks: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>, dateType: `[`DateType`](../../nl.mpcjanssen.simpletask/-date-type/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [editTasks](edit-tasks.md) | `fun editTasks(from: Activity, tasks: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>, prefill: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [getSortedTasks](get-sorted-tasks.md) | `fun getSortedTasks(filter: `[`ActiveFilter`](../../nl.mpcjanssen.simpletask/-active-filter/index.md)`, sorts: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>, caseSensitive: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Sequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence/index.html)`<`[`Task`](../-task/index.md)`>` |
| [getTaskCount](get-task-count.md) | `fun getTaskCount(): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [hasPendingAction](has-pending-action.md) | `fun hasPendingAction(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isSelected](is-selected.md) | `fun isSelected(item: `[`Task`](../-task/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [notifyChanged](notify-changed.md) | `fun notifyChanged(todoName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, eol: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, backup: `[`BackupInterface`](../../nl.mpcjanssen.simpletask.remote/-backup-interface/index.md)`?, save: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [numSelected](num-selected.md) | `fun numSelected(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [prioritize](prioritize.md) | `fun prioritize(tasks: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>, prio: `[`Priority`](../-priority/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [queue](queue.md) | `fun queue(description: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, body: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [reload](reload.md) | `fun reload(backup: `[`BackupInterface`](../../nl.mpcjanssen.simpletask.remote/-backup-interface/index.md)`, lbm: LocalBroadcastManager, eol: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, reason: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = ""): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [removeAll](remove-all.md) | `fun removeAll(tasks: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [selectTask](select-task.md) | `fun selectTask(item: `[`Task`](../-task/index.md)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [selectTasks](select-tasks.md) | `fun selectTasks(items: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [settle](settle.md) | `fun settle(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [size](size.md) | `fun size(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [startAddTaskActivity](start-add-task-activity.md) | `fun startAddTaskActivity(act: Activity, prefill: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [unSelectTask](un-select-task.md) | `fun unSelectTask(item: `[`Task`](../-task/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [unSelectTasks](un-select-tasks.md) | `fun unSelectTasks(items: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [uncomplete](uncomplete.md) | `fun uncomplete(items: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Task`](../-task/index.md)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [updateCache](update-cache.md) | `fun updateCache(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
