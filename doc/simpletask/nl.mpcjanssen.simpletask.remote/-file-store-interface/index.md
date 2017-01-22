[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.remote](../index.md) / [FileStoreInterface](.)

# FileStoreInterface

`interface FileStoreInterface` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/remote/FileStoreInterface.kt#L11)

Interface definition of the storage backend used.

### Types

| [FileChangeListener](-file-change-listener/index.md) | `interface FileChangeListener` |
| [FileReadListener](-file-read-listener/index.md) | `interface FileReadListener` |
| [FileSelectedListener](-file-selected-listener/index.md) | `interface FileSelectedListener` |

### Properties

| [isAuthenticated](is-authenticated.md) | `abstract val isAuthenticated: Boolean` |
| [isLoading](is-loading.md) | `abstract val isLoading: Boolean` |
| [isOnline](is-online.md) | `abstract val isOnline: Boolean` |
| [type](type.md) | `abstract val type: Int` |

### Functions

| [appendTaskToFile](append-task-to-file.md) | `abstract fun appendTaskToFile(path: String, lines: List<String>, eol: String): Unit` |
| [browseForNewFile](browse-for-new-file.md) | `abstract fun browseForNewFile(act: Activity, path: String, listener: `[`FileSelectedListener`](-file-selected-listener/index.md)`, txtOnly: Boolean): Unit` |
| [changesPending](changes-pending.md) | `abstract fun changesPending(): Boolean` |
| [getVersion](get-version.md) | `abstract fun getVersion(filename: String): String?` |
| [getWritePermission](get-write-permission.md) | `abstract fun getWritePermission(act: Activity, activityResult: Int): Boolean` |
| [loadTasksFromFile](load-tasks-from-file.md) | `abstract fun loadTasksFromFile(path: String, backup: `[`BackupInterface`](../-backup-interface/index.md)`?, eol: String): List<String>` |
| [logout](logout.md) | `abstract fun logout(): Unit` |
| [needsRefresh](needs-refresh.md) | `abstract fun needsRefresh(currentVersion: String?): Boolean` |
| [pause](pause.md) | `open fun pause(pause: Boolean): Unit` |
| [readFile](read-file.md) | `abstract fun readFile(file: String, fileRead: `[`FileReadListener`](-file-read-listener/index.md)`?): String` |
| [saveTasksToFile](save-tasks-to-file.md) | `abstract fun saveTasksToFile(path: String, lines: List<String>, backup: `[`BackupInterface`](../-backup-interface/index.md)`?, eol: String, updateVersion: Boolean = false): Unit` |
| [startLogin](start-login.md) | `abstract fun startLogin(caller: Activity): Unit` |
| [supportsSync](supports-sync.md) | `abstract fun supportsSync(): Boolean` |
| [sync](sync.md) | `abstract fun sync(): Unit` |
| [writeFile](write-file.md) | `abstract fun writeFile(file: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`, contents: String): Unit` |

