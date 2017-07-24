[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [removeCheckState](.)

# removeCheckState

`open fun removeCheckState(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Use this when an item has been deleted, to move the check state of all following items up one step. If you have a choiceMode which is not none, this method must be called when the order of items changes in an underlying adapter which does not have stable IDs (see ``[`ListAdapter#hasStableIds()`](#)). This is because without IDs, the ListView has no way of knowing which items have moved where, and cannot updateCache the check state accordingly. See also further comments on ``[`#moveCheckState(int, int)`](#).

### Parameters

`position` - 