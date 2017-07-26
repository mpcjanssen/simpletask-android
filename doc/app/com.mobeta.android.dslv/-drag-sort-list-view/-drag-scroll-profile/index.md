[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [DragScrollProfile](.)

# DragScrollProfile

`interface DragScrollProfile`

Interface for controlling scroll speed as a function of touch position and time. Use ``[`DragSortListView#setDragScrollProfile(DragScrollProfile)`](../set-drag-scroll-profile.md) to set custom profile.

**Author**
heycosmo

### Functions

| Name | Summary |
|---|---|
| [getSpeed](get-speed.md) | `abstract fun getSpeed(w: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`, t: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`): `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)<br>Return a scroll speed in pixels/millisecond. Always return a positive number. |
