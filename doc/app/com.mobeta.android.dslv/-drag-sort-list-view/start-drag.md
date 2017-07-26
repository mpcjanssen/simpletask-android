[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [startDrag](.)

# startDrag

`open fun startDrag(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, dragFlags: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, deltaX: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, deltaY: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Start a drag of item at `position` using the registered FloatViewManager. Calls through to ``[`#startDrag(int,View,int,int,int)`](#) after obtaining the floating View from the FloatViewManager.

### Parameters

`position` - Item to drag.

`dragFlags` - Flags that restrict some movements of the floating View. For example, set `dragFlags |= ~``[`#DRAG_NEG_X`](-d-r-a-g_-n-e-g_-x.md)` to allow dragging the floating View in all directions except off the screen to the left.

`deltaX` - Offset in x of the touch coordinate from the left edge of the floating View (i.e. touch-x minus float View left).

`deltaY` - Offset in y of the touch coordinate from the top edge of the floating View (i.e. touch-y minus float View top).

**Return**
True if the drag was started, false otherwise. This `startDrag` will fail if we are not currently in a touch event, there is no registered FloatViewManager, or the FloatViewManager returns a null View.

`open fun startDrag(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, @Nullable floatView: @Nullable View?, dragFlags: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, deltaX: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, deltaY: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Start a drag of item at `position` without using a FloatViewManager.

### Parameters

`position` - Item to drag.

`floatView` - Floating View.

`dragFlags` - Flags that restrict some movements of the floating View. For example, set `dragFlags |= ~``[`#DRAG_NEG_X`](-d-r-a-g_-n-e-g_-x.md)` to allow dragging the floating View in all directions except off the screen to the left.

`deltaX` - Offset in x of the touch coordinate from the left edge of the floating View (i.e. touch-x minus float View left).

`deltaY` - Offset in y of the touch coordinate from the top edge of the floating View (i.e. touch-y minus float View top).

**Return**
True if the drag was started, false otherwise. This `startDrag` will fail if we are not currently in a touch event, `floatView` is null, or there is a drag in progress.

