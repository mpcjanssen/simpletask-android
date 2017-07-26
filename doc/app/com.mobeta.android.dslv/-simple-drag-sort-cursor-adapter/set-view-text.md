[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [SimpleDragSortCursorAdapter](index.md) / [setViewText](.)

# setViewText

`open fun setViewText(@NonNull v: @NonNull TextView, text: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Called by bindView() to set the text for a TextView but only if there is no existing ViewBinder or if the existing ViewBinder cannot handle binding to a TextView. Intended to be overridden by Adapters that need to filter strings retrieved from the database.

### Parameters

`v` - TextView to receive text

`text` - the text to be set for the TextView