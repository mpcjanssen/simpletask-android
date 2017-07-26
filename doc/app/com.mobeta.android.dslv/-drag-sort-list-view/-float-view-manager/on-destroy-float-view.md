[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [FloatViewManager](index.md) / [onDestroyFloatView](.)

# onDestroyFloatView

`abstract fun onDestroyFloatView(floatView: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Called when the float View is dropped; lets you perform any necessary cleanup. The internal DSLV floating View reference is set to null immediately after this is called.

### Parameters

`floatView` - The floating View passed to ``[`#onCreateFloatView(int)`](on-create-float-view.md).