[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [moveItem](.)

# moveItem

`open fun moveItem(from: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, to: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Move an item, bypassing the drag-sort process. Simply calls through to ``[`DropListener#drop(int, int)`](#).

### Parameters

`from` - Position to move (NOTE: headers/footers ignored! this is a position in your input ListAdapter).

`to` - Target position (NOTE: headers/footers ignored! this is a position in your input ListAdapter).