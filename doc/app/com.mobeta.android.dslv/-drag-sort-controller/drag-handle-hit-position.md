[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortController](index.md) / [dragHandleHitPosition](.)

# dragHandleHitPosition

`open fun dragHandleHitPosition(@NonNull ev: @NonNull MotionEvent): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)

Checks for the touch of an item's drag handle (specified by ``[`#setDragHandleId(int)`](set-drag-handle-id.md)), and returns that item's position if a drag handle touch was detected.

### Parameters

`ev` - The ACTION_DOWN MotionEvent.

**Return**
The list position of the item whose drag handle was touched; MISS if unsuccessful.

