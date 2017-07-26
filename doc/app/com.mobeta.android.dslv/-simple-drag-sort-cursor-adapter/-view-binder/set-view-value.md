[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [SimpleDragSortCursorAdapter](../index.md) / [ViewBinder](index.md) / [setViewValue](.)

# setViewValue

`abstract fun setViewValue(view: View, cursor: Cursor, columnIndex: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Binds the Cursor column defined by the specified index to the specified view. When binding is handled by this ViewBinder, this method must return true. If this method returns false, SimpleCursorAdapter will attempts to handle the binding on its own.

### Parameters

`view` - the view to bind the data to

`cursor` - the cursor to get the data from

`columnIndex` - the column at which the data can be found in the cursor

**Return**
true if the data was bound to the view, false otherwise

