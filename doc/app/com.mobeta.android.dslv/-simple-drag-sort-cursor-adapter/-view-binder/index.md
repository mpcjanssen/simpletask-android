[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [SimpleDragSortCursorAdapter](../index.md) / [ViewBinder](.)

# ViewBinder

`interface ViewBinder`

This class can be used by external clients of SimpleCursorAdapter to bind values fom the Cursor to views. You should use this class to bind values from the Cursor to views that are not directly supported by SimpleCursorAdapter or to change the way binding occurs for views supported by SimpleCursorAdapter.

**See Also**
SimpleCursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)SimpleCursorAdapter#setViewImage(ImageView, String)SimpleCursorAdapter#setViewText(TextView, String)

### Functions

| Name | Summary |
|---|---|
| [setViewValue](set-view-value.md) | `abstract fun setViewValue(view: View, cursor: Cursor, columnIndex: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Binds the Cursor column defined by the specified index to the specified view. When binding is handled by this ViewBinder, this method must return true. If this method returns false, SimpleCursorAdapter will attempts to handle the binding on its own. |
