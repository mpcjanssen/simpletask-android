[simpletask](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [FloatViewManager](index.md) / [onDragFloatView](.)

# onDragFloatView

`abstract fun onDragFloatView(floatView: View, location: Point, touch: Point): Unit` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L2455)

Called whenever the floating View is dragged. Float View properties can be changed here. Also, the upcoming location of the float View can be altered by setting `location.x` and `location.y`.

### Parameters

`floatView` - The floating View.

`location` - The location (top-left; relative to DSLV top-left) at which the float View would like to appear, given the current touch location and the offset provided in ``[`DragSortListView#startDrag`](#).

`touch` - The current touch location (relative to DSLV top-left).

`pendingScroll` - 