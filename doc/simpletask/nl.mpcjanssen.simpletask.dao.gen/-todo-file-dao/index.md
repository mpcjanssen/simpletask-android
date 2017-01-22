[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.dao.gen](../index.md) / [TodoFileDao](.)

# TodoFileDao

`open class TodoFileDao : AbstractDao<`[`TodoFile`](../-todo-file/index.md)`, String>` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/dao/gen/TodoFileDao.java#L17)

DAO for table "TODO_FILE".

### Types

| [Properties](-properties/index.md) | `open class Properties`<br>Properties of entity TodoFile. Can be used for QueryBuilder and for referencing column names. |

### Constructors

| [&lt;init&gt;](-init-.md) | `TodoFileDao(config: DaoConfig)`<br>`TodoFileDao(config: DaoConfig, daoSession: `[`DaoSession`](../-dao-session/index.md)`)` |

### Properties

| [TABLENAME](-t-a-b-l-e-n-a-m-e.md) | `static val TABLENAME: String` |

### Functions

| [createTable](create-table.md) | `open static fun createTable(db: SQLiteDatabase, ifNotExists: Boolean): Unit`<br>Creates the underlying database table. |
| [dropTable](drop-table.md) | `open static fun dropTable(db: SQLiteDatabase, ifExists: Boolean): Unit`<br>Drops the underlying database table. |
| [getKey](get-key.md) | `open fun getKey(entity: `[`TodoFile`](../-todo-file/index.md)`): String` |
| [readEntity](read-entity.md) | `open fun readEntity(cursor: Cursor, offset: Int): `[`TodoFile`](../-todo-file/index.md)<br>`open fun readEntity(cursor: Cursor, entity: `[`TodoFile`](../-todo-file/index.md)`, offset: Int): Unit` |
| [readKey](read-key.md) | `open fun readKey(cursor: Cursor, offset: Int): String` |

