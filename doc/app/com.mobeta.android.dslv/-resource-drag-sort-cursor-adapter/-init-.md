[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [ResourceDragSortCursorAdapter](index.md) / [&lt;init&gt;](.)

# &lt;init&gt;

`ResourceDragSortCursorAdapter(@NonNull context: @NonNull Context, layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, c: Cursor)`
**Deprecated:** This option is discouraged, as it results in Cursor queries being performed on the application's UI thread and thus can cause poor responsiveness or even Application Not Responding errors. As an alternative, use android.app.LoaderManager with a android.content.CursorLoader.

Constructor the enables auto-requery.

### Parameters

`context` - The context where the ListView associated with this adapter is running

`layout` - resource identifier of a layout file that defines the views for this list item. Unless you override them later, this will define both the item views and the drop down views.`ResourceDragSortCursorAdapter(@NonNull context: @NonNull Context, layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, c: Cursor, autoRequery: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)`

Constructor with default behavior as per CursorAdapter#CursorAdapter(Context, Cursor, boolean); it is recommended you not use this, but instead #ResourceCursorAdapter(Context, int, Cursor, int). When using this constructor, ``[`#FLAG_REGISTER_CONTENT_OBSERVER`](#) will always be set.

### Parameters

`context` - The context where the ListView associated with this adapter is running

`layout` - resource identifier of a layout file that defines the views for this list item. Unless you override them later, this will define both the item views and the drop down views.

`c` - The cursor from which to get the data.

`autoRequery` - If true the adapter will call requery() on the cursor whenever it changes so the most recent data is always displayed. Using true here is discouraged.`ResourceDragSortCursorAdapter(@NonNull context: @NonNull Context, layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, c: Cursor, flags: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`

Standard constructor.

### Parameters

`context` - The context where the ListView associated with this adapter is running

`layout` - Resource identifier of a layout file that defines the views for this list item. Unless you override them later, this will define both the item views and the drop down views.

`c` - The cursor from which to get the data.

`flags` - Flags used to determine the behavior of the adapter, as per CursorAdapter#CursorAdapter(Context, Cursor, int).