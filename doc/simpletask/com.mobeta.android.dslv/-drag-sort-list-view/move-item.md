[simpletask](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [moveItem](.)

# moveItem

`open fun moveItem(from: Int, to: Int): Unit` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L1449)

Move an item, bypassing the drag-sort process. Simply calls through to ``[`DropListener#drop(int, int)`](#).

### Parameters

`from` - Position to move (NOTE: headers/footers ignored! this is a position in your input ListAdapter).

`to` - Target position (NOTE: headers/footers ignored! this is a position in your input ListAdapter).