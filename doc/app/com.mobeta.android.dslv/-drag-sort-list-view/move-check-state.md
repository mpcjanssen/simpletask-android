[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [moveCheckState](.)

# moveCheckState

`open fun moveCheckState(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Use this to move the check state of an item from one position to another in a drop operation. If you have a choiceMode which is not none, this method must be called when the order of items changes in an underlying adapter which does not have stable IDs (see ``[`ListAdapter#hasStableIds()`](#)). This is because without IDs, the ListView has no way of knowing which items have moved where, and cannot updateCache the check state accordingly.

 A word of warning about a "feature" in Android that you may run into when dealing with movable list items: for an adapter that *does* have stable IDs, ListView will attempt to locate each item based on its ID and move the check state from the item's old position to the new position — which is all fine and good (and removes the need for calling this function), except for the half-baked approach. Apparently to save time in the naive algorithm used, ListView will only search for an ID in the close neighborhood of the old position. If the user moves an item too far (specifically, more than 20 rows away), ListView will give up and just force the item to be unchecked. So if there is a reasonable chance that the user will move items more than 20 rows away from the original position, you may wish to use an adapter with unstable IDs and call this method manually instead.

### Parameters

`from` -

`to` - 