[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.dao.gentodo](../index.md) / [TodoItemDao](.)

# TodoItemDao

`open class TodoItemDao : AbstractDao<`[`TodoItem`](../-todo-item/index.md)`, Long>` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/dao/gentodo/TodoItemDao.java#L20)

DAO for table "TODO_ITEM".

### Types

| [Properties](-properties/index.md) | `open class Properties`<br>Properties of entity TodoItem. Can be used for QueryBuilder and for referencing column names. |

### Constructors

| [&lt;init&gt;](-init-.md) | `TodoItemDao(config: DaoConfig)`<br>`TodoItemDao(config: DaoConfig, daoSession: `[`DaoSession`](../-dao-session/index.md)`)` |

### Properties

| [TABLENAME](-t-a-b-l-e-n-a-m-e.md) | `static val TABLENAME: String` |

### Functions

| [createTable](create-table.md) | `open static fun createTable(db: SQLiteDatabase, ifNotExists: Boolean): Unit`<br>Creates the underlying database table. |
| [dropTable](drop-table.md) | `open static fun dropTable(db: SQLiteDatabase, ifExists: Boolean): Unit`<br>Drops the underlying database table. |
| [getKey](get-key.md) | `open fun getKey(entity: `[`TodoItem`](../-todo-item/index.md)`): Long` |
| [readEntity](read-entity.md) | `open fun readEntity(cursor: Cursor, offset: Int): `[`TodoItem`](../-todo-item/index.md)<br>`open fun readEntity(cursor: Cursor, entity: `[`TodoItem`](../-todo-item/index.md)`, offset: Int): Unit` |
| [readKey](read-key.md) | `open fun readKey(cursor: Cursor, offset: Int): Long` |

