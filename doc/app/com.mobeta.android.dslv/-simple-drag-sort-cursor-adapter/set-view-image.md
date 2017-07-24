[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [SimpleDragSortCursorAdapter](index.md) / [setViewImage](.)

# setViewImage

`open fun setViewImage(@NonNull v: @NonNull ImageView, value: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Called by bindView() to set the image for an ImageView but only if there is no existing ViewBinder or if the existing ViewBinder cannot handle binding to an ImageView. By default, the value will be treated as an image resource. If the value cannot be used as an image resource, the value is used as an image Uri. Intended to be overridden by Adapters that need to filter strings retrieved from the database.

### Parameters

`v` - ImageView to receive an image

`value` - the value retrieved from the cursor