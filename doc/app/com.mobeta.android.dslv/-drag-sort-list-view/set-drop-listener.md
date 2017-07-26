[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [setDropListener](.)

# setDropListener

`open fun setDropListener(l: `[`DropListener`](-drop-listener/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

This better reorder your ListAdapter! DragSortListView does not do this for you; doesn't make sense to. Make sure ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it is called in your implementation. Furthermore, if you have a choiceMode other than none and the ListAdapter does not return true for ``[`ListAdapter#hasStableIds()`](#), you will need to call ``[`#moveCheckState(int, int)`](#) to move the check boxes along with the list items.

### Parameters

`l` - 