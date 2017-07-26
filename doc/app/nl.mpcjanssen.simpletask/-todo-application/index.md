[app](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [TodoApplication](.)

# TodoApplication

`class TodoApplication : Application, `[`FileChangeListener`](../../nl.mpcjanssen.simpletask.remote/-file-store-interface/-file-change-listener/index.md)`, `[`BackupInterface`](../../nl.mpcjanssen.simpletask.remote/-backup-interface/index.md)

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `TodoApplication()` |

### Properties

| Name | Summary |
|---|---|
| [doneFileName](done-file-name.md) | `val doneFileName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [isAuthenticated](is-authenticated.md) | `val isAuthenticated: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [localBroadCastManager](local-broad-cast-manager.md) | `lateinit var localBroadCastManager: LocalBroadcastManager` |
| [today](today.md) | `var today: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Functions

| Name | Summary |
|---|---|
| [backup](backup.md) | `fun backup(name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, contents: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [browseForNewFile](browse-for-new-file.md) | `fun browseForNewFile(act: Activity): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [fileChanged](file-changed.md) | `fun fileChanged(newName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [getSortString](get-sort-string.md) | `fun getSortString(key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [loadTodoList](load-todo-list.md) | `fun loadTodoList(reason: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onCreate](on-create.md) | `fun onCreate(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onTerminate](on-terminate.md) | `fun onTerminate(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [redrawWidgets](redraw-widgets.md) | `fun redrawWidgets(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [startLogin](start-login.md) | `fun startLogin(caller: Activity): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [switchTodoFile](switch-todo-file.md) | `fun switchTodoFile(newTodo: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [updateWidgets](update-widgets.md) | `fun updateWidgets(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Companion Object Properties

| Name | Summary |
|---|---|
| [app](app.md) | `lateinit var app: TodoApplication` |

### Companion Object Functions

| Name | Summary |
|---|---|
| [atLeastAPI](at-least-a-p-i.md) | `fun atLeastAPI(api: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
