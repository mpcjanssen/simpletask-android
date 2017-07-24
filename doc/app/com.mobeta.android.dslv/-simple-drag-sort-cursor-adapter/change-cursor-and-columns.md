[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [SimpleDragSortCursorAdapter](index.md) / [changeCursorAndColumns](.)

# changeCursorAndColumns

`open fun changeCursorAndColumns(c: Cursor, from: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>, to: `[`IntArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int-array/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Change the cursor and change the column-to-view mappings at the same time.

### Parameters

`c` - The database cursor. Can be null if the cursor is not available yet.

`from` - A list of column names representing the data to bind to the UI. Can be null if the cursor is not available yet.

`to` - The views that should display column in the "from" parameter. These should all be TextViews. The first N views in this list are given the values of the first N columns in the from parameter. Can be null if the cursor is not available yet.