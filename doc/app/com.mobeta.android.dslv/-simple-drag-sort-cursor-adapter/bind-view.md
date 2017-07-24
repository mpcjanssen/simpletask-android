[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [SimpleDragSortCursorAdapter](index.md) / [bindView](.)

# bindView

`open fun bindView(@NonNull view: @NonNull View, context: Context, @NonNull cursor: @NonNull Cursor): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Binds all of the field names passed into the "to" parameter of the constructor with their corresponding cursor columns as specified in the "from" parameter. Binding occurs in two phases. First, if a android.widget.SimpleCursorAdapter.ViewBinder is available, ``[`ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)`](#) is invoked. If the returned value is true, binding has occured. If the returned value is false and the view to bind is a TextView, ``[`#setViewText(TextView, String)`](#) is invoked. If the returned value is false and the view to bind is an ImageView, ``[`#setViewImage(ImageView, String)`](#) is invoked. If no appropriate binding can be found, an IllegalStateException is thrown.

### Exceptions

`IllegalStateException` - if binding cannot occur

**See Also**
[android.widget.CursorAdapter#bindView(android.view.View,
     *      android.content.Context, android.database.Cursor)](#)[#getViewBinder()](get-view-binder.md)#setViewBinder(android.widget.SimpleCursorAdapter.ViewBinder)[#setViewImage(ImageView, String)](#)[#setViewText(TextView, String)](#)

