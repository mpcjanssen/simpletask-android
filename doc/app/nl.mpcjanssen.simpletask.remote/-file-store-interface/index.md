[app](../../index.md) / [nl.mpcjanssen.simpletask.remote](../index.md) / [FileStoreInterface](.)

# FileStoreInterface

`interface FileStoreInterface`

Interface definition of the storage backend used.

### Types

| Name | Summary |
|---|---|
| [FileChangeListener](-file-change-listener/index.md) | `interface FileChangeListener` |
| [FileReadListener](-file-read-listener/index.md) | `interface FileReadListener` |
| [FileSelectedListener](-file-selected-listener/index.md) | `interface FileSelectedListener` |

### Properties

| Name | Summary |
|---|---|
| [isAuthenticated](is-authenticated.md) | `abstract val isAuthenticated: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isLoading](is-loading.md) | `abstract val isLoading: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isOnline](is-online.md) | `abstract val isOnline: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

### Functions

| Name | Summary |
|---|---|
| [appendTaskToFile](append-task-to-file.md) | `abstract fun appendTaskToFile(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, lines: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>, eol: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [browseForNewFile](browse-for-new-file.md) | `abstract fun browseForNewFile(act: Activity, path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, listener: `[`FileSelectedListener`](-file-selected-listener/index.md)`, txtOnly: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [changesPending](changes-pending.md) | `abstract fun changesPending(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [getVersion](get-version.md) | `abstract fun getVersion(filename: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
| [getWritePermission](get-write-permission.md) | `abstract fun getWritePermission(act: Activity, activityResult: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [loadTasksFromFile](load-tasks-from-file.md) | `abstract fun loadTasksFromFile(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, backup: `[`BackupInterface`](../-backup-interface/index.md)`?, eol: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [logout](logout.md) | `abstract fun logout(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [needsRefresh](needs-refresh.md) | `abstract fun needsRefresh(currentVersion: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [pause](pause.md) | `open fun pause(pause: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [readFile](read-file.md) | `abstract fun readFile(file: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fileRead: `[`FileReadListener`](-file-read-listener/index.md)`?): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [saveTasksToFile](save-tasks-to-file.md) | `abstract fun saveTasksToFile(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, lines: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>, backup: `[`BackupInterface`](../-backup-interface/index.md)`?, eol: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, updateVersion: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [startLogin](start-login.md) | `abstract fun startLogin(caller: Activity): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [supportsSync](supports-sync.md) | `abstract fun supportsSync(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [sync](sync.md) | `abstract fun sync(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [writeFile](write-file.md) | `abstract fun writeFile(file: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`, contents: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [FileStore](../-file-store/index.md) | `object FileStore : FileStoreInterface` |
