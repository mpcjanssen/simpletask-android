[simpletask](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortCursorAdapter](.)

# DragSortCursorAdapter

`abstract class DragSortCursorAdapter : CursorAdapter, `[`DragSortListener`](../-drag-sort-list-view/-drag-sort-listener.md) [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortCursorAdapter.java#L29)

A subclass of android.widget.CursorAdapter that provides reordering of the elements in the Cursor based on completed drag-sort operations. The reordering is a simple mapping of list positions into Cursor positions (the Cursor is unchanged). To persist changes made by drag-sorts, one can retrieve the mapping with the ``[`#getCursorPositions()`](get-cursor-positions.md) method, which returns the reordered list of Cursor positions. An instance of this class is passed to DragSortListView#setAdapter(ListAdapter) and, since this class implements the DragSortListView.DragSortListener interface, it is automatically set as the DragSortListener for the DragSortListView instance.

### Constructors

| [&lt;init&gt;](-init-.md) | `DragSortCursorAdapter(context: Context, c: Cursor)`<br>`DragSortCursorAdapter(context: Context, c: Cursor, autoRequery: Boolean)`<br>`DragSortCursorAdapter(context: Context, c: Cursor, flags: Int)` |

### Properties

| [REMOVED](-r-e-m-o-v-e-d.md) | `static val REMOVED: Int` |

### Functions

| [changeCursor](change-cursor.md) | `open fun changeCursor(cursor: Cursor): Unit`<br>Changes Cursor and clears list-Cursor mapping. |
| [drag](drag.md) | `open fun drag(from: Int, to: Int): Unit`<br>Does nothing. Just completes DragSortListener interface. |
| [drop](drop.md) | `open fun drop(from: Int, to: Int): Unit`<br>On drop, this updates the mapping between Cursor positions and ListView positions. The Cursor is unchanged. Retrieve the current mapping with (. |
| [getCount](get-count.md) | `open fun getCount(): Int` |
| [getCursorPosition](get-cursor-position.md) | `open fun getCursorPosition(position: Int): Int`<br>Get the Cursor position mapped to by the provided list position (given all previously handled drag-sort operations). |
| [getCursorPositions](get-cursor-positions.md) | `open fun getCursorPositions(): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<Int>`<br>Get the current order of Cursor positions presented by the list. |
| [getDropDownView](get-drop-down-view.md) | `open fun getDropDownView(position: Int, convertView: View, parent: ViewGroup): View` |
| [getItem](get-item.md) | `open fun getItem(position: Int): Any` |
| [getItemId](get-item-id.md) | `open fun getItemId(position: Int): Long` |
| [getListPosition](get-list-position.md) | `open fun getListPosition(cursorPosition: Int): Int`<br>Get the list position mapped to by the provided Cursor position. If the provided Cursor position has been removed by a drag-sort, this returns ``[`#REMOVED`](-r-e-m-o-v-e-d.md). |
| [getView](get-view.md) | `open fun getView(position: Int, convertView: View, parent: ViewGroup): View` |
| [remove](remove.md) | `open fun remove(which: Int): Unit`<br>On remove, this updates the mapping between Cursor positions and ListView positions. The Cursor is unchanged. Retrieve the current mapping with (. |
| [reset](reset.md) | `open fun reset(): Unit`<br>Resets list-cursor mapping. |
| [swapCursor](swap-cursor.md) | `open fun swapCursor(newCursor: Cursor): Cursor`<br>Swaps Cursor and clears list-Cursor mapping. |

### Inheritors

| [ResourceDragSortCursorAdapter](../-resource-drag-sort-cursor-adapter/index.md) | `abstract class ResourceDragSortCursorAdapter : DragSortCursorAdapter`<br>Static library support version of the framework's android.widget.ResourceCursorAdapter. Used to write apps that run on platforms prior to Android 3.0. When running on Android 3.0 or above, this implementation is still used; it does not try to switch to the framework's implementation. See the framework SDK documentation for a class overview. |

