[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [Logger](.)

# Logger

`object Logger : `[`Thread`](http://docs.oracle.com/javase/6/docs/api/java/lang/Thread.html) [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/Logger.kt#L15)

### Functions

| [debug](debug.md) | `fun debug(tag: String, s: String): Unit`<br>`fun debug(tag: String, s: String, ex: Throwable): Unit` |
| [error](error.md) | `fun error(tag: String, s: String): Unit`<br>`fun error(tag: String, s: String, throwable: Throwable): Unit` |
| [info](info.md) | `fun info(tag: String, s: String): Unit`<br>`fun info(tag: String, s: String, ex: Throwable): Unit` |
| [logInDB](log-in-d-b.md) | `fun logInDB(severity: String, tag: String, s: String, throwable: Throwable? = null): Unit` |
| [run](run.md) | `fun run(): Unit` |
| [setDao](set-dao.md) | `fun setDao(dao: `[`LogItemDao`](../../nl.mpcjanssen.simpletask.dao.gen/-log-item-dao/index.md)`): Unit` |
| [warn](warn.md) | `fun warn(tag: String, s: String): Unit`<br>`fun warn(tag: String, s: String, throwable: Throwable): Unit` |

