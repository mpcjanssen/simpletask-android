[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.dao.gen](../index.md) / [LogItem](.)

# LogItem

`open class LogItem` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/dao/gen/LogItem.java#L7)

Entity mapped to table "LOG_ITEM".

### Constructors

| [&lt;init&gt;](-init-.md) | `LogItem()`<br>`LogItem(id: Long)`<br>`LogItem(id: Long, timestamp: `[`Date`](http://docs.oracle.com/javase/6/docs/api/java/util/Date.html)`, severity: String, tag: String, message: String, exception: String)` |

### Functions

| [getException](get-exception.md) | `open fun getException(): String`<br>Not-null value. |
| [getId](get-id.md) | `open fun getId(): Long` |
| [getMessage](get-message.md) | `open fun getMessage(): String`<br>Not-null value. |
| [getSeverity](get-severity.md) | `open fun getSeverity(): String`<br>Not-null value. |
| [getTag](get-tag.md) | `open fun getTag(): String`<br>Not-null value. |
| [getTimestamp](get-timestamp.md) | `open fun getTimestamp(): `[`Date`](http://docs.oracle.com/javase/6/docs/api/java/util/Date.html)<br>Not-null value. |
| [setException](set-exception.md) | `open fun setException(exception: String): Unit`<br>Not-null value; ensure this value is available before it is saved to the database. |
| [setId](set-id.md) | `open fun setId(id: Long): Unit` |
| [setMessage](set-message.md) | `open fun setMessage(message: String): Unit`<br>Not-null value; ensure this value is available before it is saved to the database. |
| [setSeverity](set-severity.md) | `open fun setSeverity(severity: String): Unit`<br>Not-null value; ensure this value is available before it is saved to the database. |
| [setTag](set-tag.md) | `open fun setTag(tag: String): Unit`<br>Not-null value; ensure this value is available before it is saved to the database. |
| [setTimestamp](set-timestamp.md) | `open fun setTimestamp(timestamp: `[`Date`](http://docs.oracle.com/javase/6/docs/api/java/util/Date.html)`): Unit`<br>Not-null value; ensure this value is available before it is saved to the database. |

