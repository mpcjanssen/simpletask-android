[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [DropListener](.)

# DropListener

`interface DropListener`

Your implementation of this has to reorder your ListAdapter! Make sure to call ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it in your implementation.

**Author**
heycosmo

### Functions

| Name | Summary |
|---|---|
| [drop](drop.md) | `abstract fun drop(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [DragSortListener](../-drag-sort-listener.md) | `interface DragSortListener : DropListener, `[`DragListener`](../-drag-listener/index.md)`, `[`RemoveListener`](../-remove-listener/index.md) |
