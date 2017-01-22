[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [CachedFileProvider](.)

# CachedFileProvider

`class CachedFileProvider : ContentProvider` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/CachedFileProvider.kt#L15)

### Constructors

| [&lt;init&gt;](-init-.md) | `CachedFileProvider()` |

### Functions

| [delete](delete.md) | `fun delete(uri: Uri, s: String, as: Array<String>): Int` |
| [getType](get-type.md) | `fun getType(uri: Uri): String` |
| [insert](insert.md) | `fun insert(uri: Uri, contentvalues: ContentValues): Uri?` |
| [onCreate](on-create.md) | `fun onCreate(): Boolean` |
| [openFile](open-file.md) | `fun openFile(uri: Uri, mode: String): ParcelFileDescriptor` |
| [query](query.md) | `fun query(uri: Uri, projection: Array<String>?, s: String?, as1: Array<String>?, s1: String?): Cursor?` |
| [update](update.md) | `fun update(uri: Uri, contentvalues: ContentValues, s: String, as: Array<String>): Int` |

### Companion Object Properties

| [AUTHORITY](-a-u-t-h-o-r-i-t-y.md) | `const val AUTHORITY: <ERROR CLASS>` |

