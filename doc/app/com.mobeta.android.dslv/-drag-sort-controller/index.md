[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortController](.)

# DragSortController

`open class DragSortController : `[`SimpleFloatViewManager`](../-simple-float-view-manager/index.md)`, OnTouchListener, OnGestureListener`

Class that starts and stops item drags on a DragSortListView based on touch gestures. This class also inherits from SimpleFloatViewManager, which provides basic float View creation. An instance of this class is meant to be passed to the methods DragSortListView#setTouchListener() and DragSortListView#setFloatViewManager() of your DragSortListView instance.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `DragSortController(dslv: `[`DragSortListView`](../-drag-sort-list-view/index.md)`)`<br>Calls #DragSortController(DragSortListView, int) with a 0 drag handle id, FLING_RIGHT_REMOVE remove mode, and ON_DOWN drag init. By default, sorting is enabled, and removal is disabled.`DragSortController(dslv: `[`DragSortListView`](../-drag-sort-list-view/index.md)`, dragHandleId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, dragInitMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, removeMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`<br>`DragSortController(dslv: `[`DragSortListView`](../-drag-sort-list-view/index.md)`, dragHandleId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, dragInitMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, removeMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, clickRemoveId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)``DragSortController(dslv: `[`DragSortListView`](../-drag-sort-list-view/index.md)`, dragHandleId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, dragInitMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, removeMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, clickRemoveId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, flingHandleId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`<br>By default, sorting is enabled, and removal is disabled. |

### Properties

| Name | Summary |
|---|---|
| [CLICK_REMOVE](-c-l-i-c-k_-r-e-m-o-v-e.md) | `static val CLICK_REMOVE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Remove mode enum. |
| [FLING_REMOVE](-f-l-i-n-g_-r-e-m-o-v-e.md) | `static val FLING_REMOVE: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [MISS](-m-i-s-s.md) | `static val MISS: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [ON_DOWN](-o-n_-d-o-w-n.md) | `static val ON_DOWN: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Drag init mode enum. |
| [ON_DRAG](-o-n_-d-r-a-g.md) | `static val ON_DRAG: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [ON_LONG_PRESS](-o-n_-l-o-n-g_-p-r-e-s-s.md) | `static val ON_LONG_PRESS: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Functions

| Name | Summary |
|---|---|
| [dragHandleHitPosition](drag-handle-hit-position.md) | `open fun dragHandleHitPosition(ev: MotionEvent): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Checks for the touch of an item's drag handle (specified by ``[`#setDragHandleId(int)`](set-drag-handle-id.md)), and returns that item's position if a drag handle touch was detected. |
| [flingHandleHitPosition](fling-handle-hit-position.md) | `open fun flingHandleHitPosition(ev: MotionEvent): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getDragInitMode](get-drag-init-mode.md) | `open fun getDragInitMode(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getRemoveMode](get-remove-mode.md) | `open fun getRemoveMode(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [isRemoveEnabled](is-remove-enabled.md) | `open fun isRemoveEnabled(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isSortEnabled](is-sort-enabled.md) | `open fun isSortEnabled(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onDown](on-down.md) | `open fun onDown(ev: MotionEvent): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onDragFloatView](on-drag-float-view.md) | `open fun onDragFloatView(floatView: View, position: Point, touch: Point): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Overrides to provide fading when slide removal is enabled. |
| [onFling](on-fling.md) | `fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`, velocityY: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onLongPress](on-long-press.md) | `open fun onLongPress(e: MotionEvent): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onScroll](on-scroll.md) | `open fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`, distanceY: `[`Float`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-float/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onShowPress](on-show-press.md) | `open fun onShowPress(ev: MotionEvent): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onSingleTapUp](on-single-tap-up.md) | `open fun onSingleTapUp(ev: MotionEvent): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onTouch](on-touch.md) | `open fun onTouch(v: View, ev: MotionEvent): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [setClickRemoveId](set-click-remove-id.md) | `open fun setClickRemoveId(id: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Set the resource id for the View that represents click removal button. |
| [setDragHandleId](set-drag-handle-id.md) | `open fun setDragHandleId(id: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Set the resource id for the View that represents the drag handle in a list item. |
| [setDragInitMode](set-drag-init-mode.md) | `open fun setDragInitMode(mode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Set how a drag is initiated. Needs to be one of ON_DOWN, ON_DRAG, or ON_LONG_PRESS. |
| [setFlingHandleId](set-fling-handle-id.md) | `open fun setFlingHandleId(id: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Set the resource id for the View that represents the fling handle in a list item. |
| [setRemoveEnabled](set-remove-enabled.md) | `open fun setRemoveEnabled(enabled: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Enable/Disable item removal without affecting remove mode. |
| [setRemoveMode](set-remove-mode.md) | `open fun setRemoveMode(mode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>One of CLICK_REMOVE, FLING_RIGHT_REMOVE, FLING_LEFT_REMOVE, SLIDE_RIGHT_REMOVE, or SLIDE_LEFT_REMOVE. |
| [setSortEnabled](set-sort-enabled.md) | `open fun setSortEnabled(enabled: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Enable/Disable list item sorting. Disabling is useful if only item removal is desired. Prevents drags in the vertical direction. |
| [startDrag](start-drag.md) | `open fun startDrag(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, deltaX: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, deltaY: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Sets flags to restrict certain motions of the floating View based on DragSortController settings (such as remove mode). Starts the drag on the DragSortListView. |
| [startDragPosition](start-drag-position.md) | `open fun startDragPosition(ev: MotionEvent): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Get the position to start dragging based on the ACTION_DOWN MotionEvent. This function simply calls ``[`#dragHandleHitPosition(MotionEvent)`](drag-handle-hit-position.md). Override to change drag handle behavior; this function is called internally when an ACTION_DOWN event is detected. |
| [startFlingPosition](start-fling-position.md) | `open fun startFlingPosition(ev: MotionEvent): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [viewIdHitPosition](view-id-hit-position.md) | `open fun viewIdHitPosition(ev: MotionEvent, id: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Inherited Functions

| Name | Summary |
|---|---|
| [onCreateFloatView](../-simple-float-view-manager/on-create-float-view.md) | `open fun onCreateFloatView(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): View?`<br>This simple implementation creates a Bitmap copy of the list item currently shown at ListView `position`. |
| [onDestroyFloatView](../-simple-float-view-manager/on-destroy-float-view.md) | `open fun onDestroyFloatView(floatView: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Removes the Bitmap from the ImageView created in onCreateFloatView() and tells the system to recycle it. |
| [setBackgroundColor](../-simple-float-view-manager/set-background-color.md) | `open fun setBackgroundColor(color: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
