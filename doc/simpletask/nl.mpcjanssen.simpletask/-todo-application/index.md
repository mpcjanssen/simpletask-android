[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [TodoApplication](.)

# TodoApplication

`class TodoApplication : Application, `[`FileChangeListener`](../../nl.mpcjanssen.simpletask.remote/-file-store-interface/-file-change-listener/index.md)`, `[`BackupInterface`](../../nl.mpcjanssen.simpletask.remote/-backup-interface/index.md) [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/TodoApplication.kt#L53)

### Constructors

| [&lt;init&gt;](-init-.md) | `TodoApplication()` |

### Properties

| [doneFileName](done-file-name.md) | `val doneFileName: String` |
| [isAuthenticated](is-authenticated.md) | `val isAuthenticated: Boolean` |
| [localBroadCastManager](local-broad-cast-manager.md) | `lateinit var localBroadCastManager: LocalBroadcastManager` |
| [today](today.md) | `var today: String` |

### Functions

| [backup](backup.md) | `fun backup(name: String, contents: String): Unit` |
| [browseForNewFile](browse-for-new-file.md) | `fun browseForNewFile(act: Activity): Unit` |
| [fileChanged](file-changed.md) | `fun fileChanged(newName: String?): Unit` |
| [getSortString](get-sort-string.md) | `fun getSortString(key: String): String` |
| [loadTodoList](load-todo-list.md) | `fun loadTodoList(): Unit` |
| [onCreate](on-create.md) | `fun onCreate(): Unit` |
| [onTerminate](on-terminate.md) | `fun onTerminate(): Unit` |
| [redrawWidgets](redraw-widgets.md) | `fun redrawWidgets(): Unit` |
| [startLogin](start-login.md) | `fun startLogin(caller: Activity): Unit` |
| [switchTodoFile](switch-todo-file.md) | `fun switchTodoFile(newTodo: String): Unit` |
| [updateWidgets](update-widgets.md) | `fun updateWidgets(): Unit` |

### Companion Object Properties

| [app](app.md) | `lateinit var app: TodoApplication` |

### Companion Object Functions

| [atLeastAPI](at-least-a-p-i.md) | `fun atLeastAPI(api: Int): Boolean` |

