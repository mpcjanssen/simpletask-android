[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [FloatViewManager](.)

# FloatViewManager

`interface FloatViewManager`

Interface for customization of the floating View appearance and dragging behavior. Implement your own and pass it to ``[`#setFloatViewManager`](../set-float-view-manager.md). If your own is not passed, the default SimpleFloatViewManager implementation is used.

### Functions

| Name | Summary |
|---|---|
| [onCreateFloatView](on-create-float-view.md) | `abstract fun onCreateFloatView(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): View`<br>Return the floating View for item at `position`. DragSortListView will measure and layout this View for you, so feel free to just inflate it. You can help DSLV by setting some ViewGroup.LayoutParams on this View; otherwise it will set some for you (with a width of MATCH_PARENT and a height of WRAP_CONTENT). |
| [onDestroyFloatView](on-destroy-float-view.md) | `abstract fun onDestroyFloatView(floatView: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Called when the float View is dropped; lets you perform any necessary cleanup. The internal DSLV floating View reference is set to null immediately after this is called. |
| [onDragFloatView](on-drag-float-view.md) | `abstract fun onDragFloatView(floatView: View, location: Point, touch: Point): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Called whenever the floating View is dragged. Float View properties can be changed here. Also, the upcoming location of the float View can be altered by setting `location.x` and `location.y`. |

### Inheritors

| Name | Summary |
|---|---|
| [SimpleFloatViewManager](../../-simple-float-view-manager/index.md) | `open class SimpleFloatViewManager : FloatViewManager`<br>Simple implementation of the FloatViewManager class. Uses list items as they appear in the ListView to create the floating View. |
