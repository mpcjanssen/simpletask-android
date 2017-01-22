[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.dao.gentodo](../index.md) / [DaoMaster](.)

# DaoMaster

`open class DaoMaster : AbstractDaoMaster` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/dao/gentodo/DaoMaster.java#L17)

Master of DAO (schema version 1): knows all DAOs.

### Types

| [DevOpenHelper](-dev-open-helper/index.md) | `open class DevOpenHelper : `[`OpenHelper`](-open-helper/index.md)<br>WARNING: Drops all table on Upgrade! Use only during development. |
| [OpenHelper](-open-helper/index.md) | `abstract class OpenHelper : SQLiteOpenHelper` |

### Constructors

| [&lt;init&gt;](-init-.md) | `DaoMaster(db: SQLiteDatabase)` |

### Properties

| [SCHEMA_VERSION](-s-c-h-e-m-a_-v-e-r-s-i-o-n.md) | `static val SCHEMA_VERSION: Int` |

### Functions

| [createAllTables](create-all-tables.md) | `open static fun createAllTables(db: SQLiteDatabase, ifNotExists: Boolean): Unit`<br>Creates underlying database table using DAOs. |
| [dropAllTables](drop-all-tables.md) | `open static fun dropAllTables(db: SQLiteDatabase, ifExists: Boolean): Unit`<br>Drops underlying database table using DAOs. |
| [newSession](new-session.md) | `open fun newSession(): `[`DaoSession`](../-dao-session/index.md)<br>`open fun newSession(type: IdentityScopeType): `[`DaoSession`](../-dao-session/index.md) |

