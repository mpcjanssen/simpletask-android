[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [ResourceDragSortCursorAdapter](.)

# ResourceDragSortCursorAdapter

`abstract class ResourceDragSortCursorAdapter : `[`DragSortCursorAdapter`](../-drag-sort-cursor-adapter/index.md)

Static library support version of the framework's android.widget.ResourceCursorAdapter. Used to write apps that run on platforms prior to Android 3.0. When running on Android 3.0 or above, this implementation is still used; it does not try to switch to the framework's implementation. See the framework SDK documentation for a class overview.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ResourceDragSortCursorAdapter(context: Context, layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, c: Cursor)`<br>Constructor the enables auto-requery.`ResourceDragSortCursorAdapter(context: Context, layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, c: Cursor, autoRequery: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)`<br>Constructor with default behavior as per CursorAdapter#CursorAdapter(Context, Cursor, boolean); it is recommended you not use this, but instead #ResourceCursorAdapter(Context, int, Cursor, int). When using this constructor, ``[`#FLAG_REGISTER_CONTENT_OBSERVER`](#) will always be set.`ResourceDragSortCursorAdapter(context: Context, layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, c: Cursor, flags: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`<br>Standard constructor. |

### Inherited Properties

| Name | Summary |
|---|---|
| [REMOVED](../-drag-sort-cursor-adapter/-r-e-m-o-v-e-d.md) | `static val REMOVED: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Functions

| Name | Summary |
|---|---|
| [newDropDownView](new-drop-down-view.md) | `open fun newDropDownView(context: Context, cursor: Cursor, parent: ViewGroup): View` |
| [newView](new-view.md) | `open fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View`<br>Inflates view(s) from the specified XML file. |
| [setDropDownViewResource](set-drop-down-view-resource.md) | `open fun setDropDownViewResource(dropDownLayout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Sets the layout resource of the drop down views. |
| [setViewResource](set-view-resource.md) | `open fun setViewResource(layout: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Sets the layout resource of the item views. |

### Inherited Functions

| Name | Summary |
|---|---|
| [changeCursor](../-drag-sort-cursor-adapter/change-cursor.md) | `open fun changeCursor(cursor: Cursor): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Changes Cursor and clears list-Cursor mapping. |
| [drag](../-drag-sort-cursor-adapter/drag.md) | `open fun drag(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Does nothing. Just completes DragSortListener interface. |
| [drop](../-drag-sort-cursor-adapter/drop.md) | `open fun drop(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>On drop, this updates the mapping between Cursor positions and ListView positions. The Cursor is unchanged. Retrieve the current mapping with (. |
| [getCount](../-drag-sort-cursor-adapter/get-count.md) | `open fun getCount(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getCursorPosition](../-drag-sort-cursor-adapter/get-cursor-position.md) | `open fun getCursorPosition(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Get the Cursor position mapped to by the provided list position (given all previously handled drag-sort operations). |
| [getCursorPositions](../-drag-sort-cursor-adapter/get-cursor-positions.md) | `open fun getCursorPositions(): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>Get the current order of Cursor positions presented by the list. |
| [getDropDownView](../-drag-sort-cursor-adapter/get-drop-down-view.md) | `open fun getDropDownView(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, convertView: View, parent: ViewGroup): View` |
| [getItem](../-drag-sort-cursor-adapter/get-item.md) | `open fun getItem(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getItemId](../-drag-sort-cursor-adapter/get-item-id.md) | `open fun getItemId(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [getListPosition](../-drag-sort-cursor-adapter/get-list-position.md) | `open fun getListPosition(cursorPosition: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Get the list position mapped to by the provided Cursor position. If the provided Cursor position has been removed by a drag-sort, this returns ``[`#REMOVED`](../-drag-sort-cursor-adapter/-r-e-m-o-v-e-d.md). |
| [getView](../-drag-sort-cursor-adapter/get-view.md) | `open fun getView(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, convertView: View, parent: ViewGroup): View` |
| [remove](../-drag-sort-cursor-adapter/remove.md) | `open fun remove(which: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>On remove, this updates the mapping between Cursor positions and ListView positions. The Cursor is unchanged. Retrieve the current mapping with (. |
| [reset](../-drag-sort-cursor-adapter/reset.md) | `open fun reset(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Resets list-cursor mapping. |
| [swapCursor](../-drag-sort-cursor-adapter/swap-cursor.md) | `open fun swapCursor(newCursor: Cursor): Cursor`<br>Swaps Cursor and clears list-Cursor mapping. |

### Inheritors

| Name | Summary |
|---|---|
| [SimpleDragSortCursorAdapter](../-simple-drag-sort-cursor-adapter/index.md) | `open class SimpleDragSortCursorAdapter : ResourceDragSortCursorAdapter`<br>An easy adapter to map columns from a cursor to TextViews or ImageViews defined in an XML file. You can specify which columns you want, which views you want to display the columns, and the XML file that defines the appearance of these views. Binding occurs in two phases. First, if a android.widget.SimpleCursorAdapter.ViewBinder is available, ``[`ViewBinder#setViewValue(android.view.View, android.database.Cursor, int)`](#) is invoked. If the returned value is true, binding has occured. If the returned value is false and the view to bind is a TextView, ``[`#setViewText(TextView, String)`](#) is invoked. If the returned value is false and the view to bind is an ImageView, ``[`#setViewImage(ImageView, String)`](#) is invoked. If no appropriate binding can be found, an IllegalStateException is thrown. If this adapter is used with filtering, for instance in an android.widget.AutoCompleteTextView, you can use the android.widget.SimpleCursorAdapter.CursorToStringConverter and the android.widget.FilterQueryProvider interfaces to get control over the filtering process. You can refer to ``[`#convertToString(android.database.Cursor)`](../-simple-drag-sort-cursor-adapter/convert-to-string.md) and ``[`#runQueryOnBackgroundThread(CharSequence)`](#) for more information. |
