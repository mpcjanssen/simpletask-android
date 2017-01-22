[simpletask](../../index.md) / [com.mobeta.android.dslv](../index.md) / [SimpleDragSortCursorAdapter](index.md) / [setStringConversionColumn](.)

# setStringConversionColumn

`open fun setStringConversionColumn(stringConversionColumn: Int): Unit` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/SimpleDragSortCursorAdapter.java#L269)

Defines the index of the column in the Cursor used to get a String representation of that Cursor. The column is used to convert the Cursor to a String only when the current CursorToStringConverter is null.

### Parameters

`stringConversionColumn` - a valid index in the current Cursor or -1 to use the default conversion mechanism

**See Also**
[android.widget.CursorAdapter#convertToString(android.database.Cursor)](#)[#getStringConversionColumn()](get-string-conversion-column.md)#setCursorToStringConverter(android.widget.SimpleCursorAdapter.CursorToStringConverter)[#getCursorToStringConverter()](get-cursor-to-string-converter.md)

