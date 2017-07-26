[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortCursorAdapter](.)

# DragSortCursorAdapter

`abstract class DragSortCursorAdapter : CursorAdapter, `[`DragSortListener`](../-drag-sort-list-view/-drag-sort-listener.md)

A subclass of android.widget.CursorAdapter that provides reordering of the elements in the Cursor based on completed drag-sort operations. The reordering is a simple mapping of list positions into Cursor positions (the Cursor is unchanged). To persist changes made by drag-sorts, one can retrieve the mapping with the ``[`#getCursorPositions()`](get-cursor-positions.md) method, which returns the reordered list of Cursor positions. An instance of this class is passed to DragSortListView#setAdapter(ListAdapter) and, since this class implements the DragSortListView.DragSortListener interface, it is automatically set as the DragSortListener for the DragSortListView instance.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `DragSortCursorAdapter(context: Context, c: Cursor)`<br>`DragSortCursorAdapter(context: Context, c: Cursor, autoRequery: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)`<br>`DragSortCursorAdapter(context: Context, c: Cursor, flags: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)` |

### Properties

| Name | Summary |
|---|---|
| [REMOVED](-r-e-m-o-v-e-d.md) | `static val REMOVED: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Functions

| Name | Summary |
|---|---|
| [changeCursor](change-cursor.md) | `open fun changeCursor(cursor: Cursor): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Changes Cursor and clears list-Cursor mapping. |
| [drag](drag.md) | `open fun drag(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Does nothing. Just completes DragSortListener interface. |
| [drop](drop.md) | `open fun drop(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>On drop, this updates the mapping between Cursor positions and ListView positions. The Cursor is unchanged. Retrieve the current mapping with (. |
| [getCount](get-count.md) | `open fun getCount(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getCursorPosition](get-cursor-position.md) | `open fun getCursorPosition(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Get the Cursor position mapped to by the provided list position (given all previously handled drag-sort operations). |
| [getCursorPositions](get-cursor-positions.md) | `open fun getCursorPositions(): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`<br>Get the current order of Cursor positions presented by the list. |
| [getDropDownView](get-drop-down-view.md) | `open fun getDropDownView(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, convertView: View, parent: ViewGroup): View` |
| [getItem](get-item.md) | `open fun getItem(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getItemId](get-item-id.md) | `open fun getItemId(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [getListPosition](get-list-position.md) | `open fun getListPosition(cursorPosition: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Get the list position mapped to by the provided Cursor position. If the provided Cursor position has been removed by a drag-sort, this returns ``[`#REMOVED`](-r-e-m-o-v-e-d.md). |
| [getView](get-view.md) | `open fun getView(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, convertView: View, parent: ViewGroup): View` |
| [remove](remove.md) | `open fun remove(which: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>On remove, this updates the mapping between Cursor positions and ListView positions. The Cursor is unchanged. Retrieve the current mapping with (. |
| [reset](reset.md) | `open fun reset(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Resets list-cursor mapping. |
| [swapCursor](swap-cursor.md) | `open fun swapCursor(newCursor: Cursor): Cursor`<br>Swaps Cursor and clears list-Cursor mapping. |

### Inheritors

| Name | Summary |
|---|---|
| [ResourceDragSortCursorAdapter](../-resource-drag-sort-cursor-adapter/index.md) | `abstract class ResourceDragSortCursorAdapter : DragSortCursorAdapter`<br>Static library support version of the framework's android.widget.ResourceCursorAdapter. Used to write apps that run on platforms prior to Android 3.0. When running on Android 3.0 or above, this implementation is still used; it does not try to switch to the framework's implementation. See the framework SDK documentation for a class overview. |
