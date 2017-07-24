[app](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [CachedFileProvider](.)

# CachedFileProvider

`class CachedFileProvider : ContentProvider`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `CachedFileProvider()` |

### Functions

| Name | Summary |
|---|---|
| [delete](delete.md) | `fun delete(uri: Uri, s: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, as: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getType](get-type.md) | `fun getType(uri: Uri): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [insert](insert.md) | `fun insert(uri: Uri, contentvalues: ContentValues): Uri?` |
| [onCreate](on-create.md) | `fun onCreate(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [openFile](open-file.md) | `fun openFile(uri: Uri, mode: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): ParcelFileDescriptor` |
| [query](query.md) | `fun query(uri: Uri, projection: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>?, s: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?, as1: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>?, s1: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?): Cursor?` |
| [update](update.md) | `fun update(uri: Uri, contentvalues: ContentValues, s: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, as: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Companion Object Properties

| Name | Summary |
|---|---|
| [AUTHORITY](-a-u-t-h-o-r-i-t-y.md) | `const val AUTHORITY: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
