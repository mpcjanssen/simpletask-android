[app](../../index.md) / [nl.mpcjanssen.simpletask.dao](../index.md) / [Daos](.)

# Daos

`object Daos`

### Properties

| Name | Summary |
|---|---|
| [backupDao](backup-dao.md) | `val backupDao: `[`TodoFileDao`](../../nl.mpcjanssen.simpletask.dao.gen/-todo-file-dao/index.md) |
| [logDao](log-dao.md) | `val logDao: `[`LogItemDao`](../../nl.mpcjanssen.simpletask.dao.gen/-log-item-dao/index.md) |

### Functions

| Name | Summary |
|---|---|
| [backup](backup.md) | `fun backup(file: `[`TodoFile`](../../nl.mpcjanssen.simpletask.dao.gen/-todo-file/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [cleanLogging](clean-logging.md) | `fun cleanLogging(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [initHistoryCursor](init-history-cursor.md) | `fun initHistoryCursor(): Cursor` |
| [logAsText](log-as-text.md) | `fun logAsText(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [logItemsDesc](log-items-desc.md) | `fun logItemsDesc(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
