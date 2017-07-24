[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortController](index.md) / [startDrag](.)

# startDrag

`open fun startDrag(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, deltaX: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, deltaY: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Sets flags to restrict certain motions of the floating View based on DragSortController settings (such as remove mode). Starts the drag on the DragSortListView.

### Parameters

`position` - The list item position (includes headers).

`deltaX` - Touch x-coord minus left edge of floating View.

`deltaY` - Touch y-coord minus top edge of floating View.

**Return**
True if drag started, false otherwise.

