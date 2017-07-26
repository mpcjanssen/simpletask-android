[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [setRemoveListener](.)

# setRemoveListener

`open fun setRemoveListener(l: `[`RemoveListener`](-remove-listener/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Probably a no-brainer, but make sure that your remove listener calls ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it. When an item removal occurs, DragSortListView relies on a redraw of all the items to recover invisible views and such. Strictly speaking, if you remove something, your dataset has changed...

### Parameters

`l` - 