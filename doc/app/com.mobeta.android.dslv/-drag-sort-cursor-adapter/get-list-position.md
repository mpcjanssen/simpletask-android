[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortCursorAdapter](index.md) / [getListPosition](.)

# getListPosition

`open fun getListPosition(cursorPosition: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)

Get the list position mapped to by the provided Cursor position. If the provided Cursor position has been removed by a drag-sort, this returns ``[`#REMOVED`](-r-e-m-o-v-e-d.md).

### Parameters

`cursorPosition` - A Cursor position

**Return**
The mapped-to list position or REMOVED

