[app](../../index.md) / [nl.mpcjanssen.simpletask.remote](../index.md) / [BackupInterface](.)

# BackupInterface

`interface BackupInterface`

Interface definition of the storage backend used.

Uses events to communicate with the application. Currently supported are SYNC_START, SYNC_DONE and FILE_CHANGED.

### Functions

| Name | Summary |
|---|---|
| [backup](backup.md) | `abstract fun backup(name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, lines: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [TodoApplication](../../nl.mpcjanssen.simpletask/-todo-application/index.md) | `class TodoApplication : Application, `[`FileChangeListener`](../-file-store-interface/-file-change-listener/index.md)`, BackupInterface` |
