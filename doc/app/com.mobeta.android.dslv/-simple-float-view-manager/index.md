[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [SimpleFloatViewManager](.)

# SimpleFloatViewManager

`open class SimpleFloatViewManager : `[`FloatViewManager`](../-drag-sort-list-view/-float-view-manager/index.md)

Simple implementation of the FloatViewManager class. Uses list items as they appear in the ListView to create the floating View.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SimpleFloatViewManager(lv: ListView)` |

### Functions

| Name | Summary |
|---|---|
| [onCreateFloatView](on-create-float-view.md) | `open fun onCreateFloatView(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): View?`<br>This simple implementation creates a Bitmap copy of the list item currently shown at ListView `position`. |
| [onDestroyFloatView](on-destroy-float-view.md) | `open fun onDestroyFloatView(floatView: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Removes the Bitmap from the ImageView created in onCreateFloatView() and tells the system to recycle it. |
| [onDragFloatView](on-drag-float-view.md) | `open fun onDragFloatView(floatView: View, position: Point, touch: Point): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>This does nothing |
| [setBackgroundColor](set-background-color.md) | `open fun setBackgroundColor(color: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [DragSortController](../-drag-sort-controller/index.md) | `open class DragSortController : SimpleFloatViewManager, OnTouchListener, OnGestureListener`<br>Class that starts and stops item drags on a DragSortListView based on touch gestures. This class also inherits from SimpleFloatViewManager, which provides basic float View creation. An instance of this class is meant to be passed to the methods DragSortListView#setTouchListener() and DragSortListView#setFloatViewManager() of your DragSortListView instance. |
