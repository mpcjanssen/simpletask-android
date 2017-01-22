[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [TodoList](.)

# TodoList

`object TodoList` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/task/TodoList.kt#L54)

Implementation of the in memory representation of the todo list

**Author**
Mark Janssen

### Properties

| [completedTasks](completed-tasks.md) | `var completedTasks: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>` |
| [contexts](contexts.md) | `val contexts: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [decoratedContexts](decorated-contexts.md) | `val decoratedContexts: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [decoratedProjects](decorated-projects.md) | `val decoratedProjects: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [priorities](priorities.md) | `val priorities: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`Priority`](../-priority/index.md)`>` |
| [projects](projects.md) | `val projects: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [selectedTasks](selected-tasks.md) | `var selectedTasks: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>` |
| [selectionQuery](selection-query.md) | `val selectionQuery: Query<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>` |
| [todoItems](todo-items.md) | `val todoItems: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>` |
| [todoItemsDao](todo-items-dao.md) | `var todoItemsDao: `[`TodoItemDao`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item-dao/index.md) |

### Functions

| [add](add.md) | `fun add(t: `[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`, atEnd: Boolean): Unit`<br>`fun add(t: `[`Task`](../-task/index.md)`, atEnd: Boolean, select: Boolean = false): Unit` |
| [archive](archive.md) | `fun archive(todoFilename: String, doneFileName: String, tasks: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>, eol: String): Unit` |
| [clearSelection](clear-selection.md) | `fun clearSelection(): Unit` |
| [complete](complete.md) | `fun complete(items: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>, keepPrio: Boolean, extraAtEnd: Boolean): Unit` |
| [defer](defer.md) | `fun defer(deferString: String, items: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>, dateType: `[`DateType`](../../nl.mpcjanssen.simpletask/-date-type/index.md)`): Unit` |
| [firstLine](first-line.md) | `fun firstLine(): Long` |
| [getSortedTasks](get-sorted-tasks.md) | `fun getSortedTasks(filter: `[`ActiveFilter`](../../nl.mpcjanssen.simpletask/-active-filter/index.md)`, sorts: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>, caseSensitive: Boolean): List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>` |
| [getTaskCount](get-task-count.md) | `fun getTaskCount(): Long` |
| [hasPendingAction](has-pending-action.md) | `fun hasPendingAction(): Boolean` |
| [isSelected](is-selected.md) | `fun isSelected(item: `[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`): Boolean` |
| [lastLine](last-line.md) | `fun lastLine(): Long` |
| [notifyChanged](notify-changed.md) | `fun notifyChanged(todoName: String, eol: String, backup: `[`BackupInterface`](../../nl.mpcjanssen.simpletask.remote/-backup-interface/index.md)`?, save: Boolean): Unit` |
| [numSelected](num-selected.md) | `fun numSelected(): Long` |
| [prioritize](prioritize.md) | `fun prioritize(items: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>, prio: `[`Priority`](../-priority/index.md)`): Unit` |
| [reload](reload.md) | `fun reload(backup: `[`BackupInterface`](../../nl.mpcjanssen.simpletask.remote/-backup-interface/index.md)`, lbm: LocalBroadcastManager, eol: String): Unit` |
| [remove](remove.md) | `fun remove(item: `[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`): Unit` |
| [selectLine](select-line.md) | `fun selectLine(line: Long): Unit` |
| [selectTodoItem](select-todo-item.md) | `fun selectTodoItem(item: `[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`): Unit` |
| [selectTodoItems](select-todo-items.md) | `fun selectTodoItems(items: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>): Unit` |
| [settle](settle.md) | `fun settle(): Unit` |
| [size](size.md) | `fun size(): Int` |
| [startAddTaskActivity](start-add-task-activity.md) | `fun startAddTaskActivity(act: Activity): Unit` |
| [unSelectTodoItem](un-select-todo-item.md) | `fun unSelectTodoItem(item: `[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`): Unit` |
| [unSelectTodoItems](un-select-todo-items.md) | `fun unSelectTodoItems(items: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>): Unit` |
| [uncomplete](uncomplete.md) | `fun uncomplete(items: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>): Unit` |
| [update](update.md) | `fun update(items: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>): Unit` |

