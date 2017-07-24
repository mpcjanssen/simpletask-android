[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [SimpleDragSortCursorAdapter](index.md) / [&lt;init&gt;](.)

# &lt;init&gt;

`SimpleDragSortCursorAdapter(context: Context, layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, c: Cursor, @NonNull from: @NonNull `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>, to: `[`IntArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int-array/index.html)`)`
**Deprecated:** This option is discouraged, as it results in Cursor queries being performed on the application's UI thread and thus can cause poor responsiveness or even Application Not Responding errors. As an alternative, use android.app.LoaderManager with a android.content.CursorLoader.

Constructor the enables auto-requery.

`SimpleDragSortCursorAdapter(context: Context, layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, c: Cursor, @NonNull from: @NonNull `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>, to: `[`IntArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int-array/index.html)`, flags: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`

Standard constructor.

### Parameters

`context` - The context where the ListView associated with this SimpleListItemFactory is running

`layout` - resource identifier of a layout file that defines the views for this list item. The layout file should include at least those named views defined in "to"

`c` - The database cursor. Can be null if the cursor is not available yet.

`from` - A list of column names representing the data to bind to the UI. Can be null if the cursor is not available yet.

`to` - The views that should display column in the "from" parameter. These should all be TextViews. The first N views in this list are given the values of the first N columns in the from parameter. Can be null if the cursor is not available yet.

`flags` - Flags used to determine the behavior of the adapter, as per CursorAdapter#CursorAdapter(Context, Cursor, int).