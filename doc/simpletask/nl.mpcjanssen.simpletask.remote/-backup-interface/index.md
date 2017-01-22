[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.remote](../index.md) / [BackupInterface](.)

# BackupInterface

`interface BackupInterface` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/remote/BackupInterface.kt#L8)

Interface definition of the storage backend used.

Uses events to communicate with the application. Currently supported are SYNC_START, SYNC_DONE and FILE_CHANGED.

### Functions

| [backup](backup.md) | `abstract fun backup(name: String, contents: String): Unit` |

### Inheritors

| [TodoApplication](../../nl.mpcjanssen.simpletask/-todo-application/index.md) | `class TodoApplication : Application, `[`FileChangeListener`](../-file-store-interface/-file-change-listener/index.md)`, BackupInterface` |

