[simpletask](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortCursorAdapter](index.md) / [getListPosition](.)

# getListPosition

`open fun getListPosition(cursorPosition: Int): Int` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortCursorAdapter.java#L231)

Get the list position mapped to by the provided Cursor position. If the provided Cursor position has been removed by a drag-sort, this returns ``[`#REMOVED`](-r-e-m-o-v-e-d.md).

### Parameters

`cursorPosition` - A Cursor position

**Return**
The mapped-to list position or REMOVED

