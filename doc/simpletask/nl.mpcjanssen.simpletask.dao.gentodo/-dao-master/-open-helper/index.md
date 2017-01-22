[simpletask](../../../index.md) / [nl.mpcjanssen.simpletask.dao.gentodo](../../index.md) / [DaoMaster](../index.md) / [OpenHelper](.)

# OpenHelper

`abstract class OpenHelper : SQLiteOpenHelper` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/dao/gentodo/DaoMaster.java#L30)

### Constructors

| [&lt;init&gt;](-init-.md) | `OpenHelper(context: Context, name: String, factory: CursorFactory)` |

### Functions

| [onCreate](on-create.md) | `open fun onCreate(db: SQLiteDatabase): Unit` |

### Inheritors

| [DevOpenHelper](../-dev-open-helper/index.md) | `open class DevOpenHelper : OpenHelper`<br>WARNING: Drops all table on Upgrade! Use only during development. |

