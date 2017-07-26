[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [DragScrollProfile](index.md) / [getSpeed](.)

# getSpeed

`abstract fun getSpeed(w: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`, t: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`): `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)

Return a scroll speed in pixels/millisecond. Always return a positive number.

### Parameters

`w` - Normalized position in scroll region (i.e. w \in [0,1]). Small w typically means slow scrolling.

`t` - Time (in milliseconds) since start of scroll (handy if you want scroll acceleration).

**Return**
Scroll speed at position w and time t in pixels/ms.

