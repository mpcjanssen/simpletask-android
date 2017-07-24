[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [SimpleDragSortCursorAdapter](index.md) / [convertToString](.)

# convertToString

`open fun convertToString(@NonNull cursor: @NonNull Cursor): `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html)

Returns a CharSequence representation of the specified Cursor as defined by the current CursorToStringConverter. If no CursorToStringConverter has been set, the String conversion column is used instead. If the conversion column is -1, the returned String is empty if the cursor is null or Cursor.toString().

### Parameters

`cursor` - the Cursor to convert to a CharSequence

**Return**
a non-null CharSequence representing the cursor

