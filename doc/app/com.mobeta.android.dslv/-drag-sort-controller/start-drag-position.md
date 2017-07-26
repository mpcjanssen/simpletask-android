[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortController](index.md) / [startDragPosition](.)

# startDragPosition

`open fun startDragPosition(@NonNull ev: @NonNull MotionEvent): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)

Get the position to start dragging based on the ACTION_DOWN MotionEvent. This function simply calls ``[`#dragHandleHitPosition(MotionEvent)`](drag-handle-hit-position.md). Override to change drag handle behavior; this function is called internally when an ACTION_DOWN event is detected.

### Parameters

`ev` - The ACTION_DOWN MotionEvent.

**Return**
The list position to drag if a drag-init gesture is detected; MISS if unsuccessful.

