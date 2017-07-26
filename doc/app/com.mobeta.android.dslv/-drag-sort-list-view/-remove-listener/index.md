[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [RemoveListener](.)

# RemoveListener

`interface RemoveListener`

Make sure to call ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it in your implementation.

**Author**
heycosmo

### Functions

| Name | Summary |
|---|---|
| [remove](remove.md) | `abstract fun remove(which: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [DragSortListener](../-drag-sort-listener.md) | `interface DragSortListener : `[`DropListener`](../-drop-listener/index.md)`, `[`DragListener`](../-drag-listener/index.md)`, RemoveListener` |
