[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [DragSortListener](.)

# DragSortListener

`interface DragSortListener : `[`DropListener`](-drop-listener/index.md)`, `[`DragListener`](-drag-listener/index.md)`, `[`RemoveListener`](-remove-listener/index.md)

### Inherited Functions

| Name | Summary |
|---|---|
| [drag](-drag-listener/drag.md) | `abstract fun drag(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [drop](-drop-listener/drop.md) | `abstract fun drop(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [remove](-remove-listener/remove.md) | `abstract fun remove(which: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [DragSortCursorAdapter](../-drag-sort-cursor-adapter/index.md) | `abstract class DragSortCursorAdapter : CursorAdapter, DragSortListener`<br>A subclass of android.widget.CursorAdapter that provides reordering of the elements in the Cursor based on completed drag-sort operations. The reordering is a simple mapping of list positions into Cursor positions (the Cursor is unchanged). To persist changes made by drag-sorts, one can retrieve the mapping with the ``[`#getCursorPositions()`](../-drag-sort-cursor-adapter/get-cursor-positions.md) method, which returns the reordered list of Cursor positions. An instance of this class is passed to DragSortListView#setAdapter(ListAdapter) and, since this class implements the DragSortListView.DragSortListener interface, it is automatically set as the DragSortListener for the DragSortListView instance. |
