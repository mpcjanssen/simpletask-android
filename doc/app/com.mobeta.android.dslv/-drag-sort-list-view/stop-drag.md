[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [stopDrag](.)

# stopDrag

`open fun stopDrag(remove: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Stop a drag in progress. Pass `true` if you would like to remove the dragged item from the list.

### Parameters

`remove` - Remove the dragged item from the list. Calls a registered RemoveListener, if one exists. Otherwise, calls the DropListener, if one exists.

**Return**
True if the stop was successful. False if there is no floating View.

`open fun stopDrag(remove: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`, velocityX: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)