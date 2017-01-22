[simpletask](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](.)

# DragSortListView

`open class DragSortListView : ListView` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L58)

ListView subclass that mediates drag and drop resorting of items.

**Author**
heycosmo

### Types

| [DragListener](-drag-listener/index.md) | `interface DragListener` |
| [DragScrollProfile](-drag-scroll-profile/index.md) | `interface DragScrollProfile`<br>Interface for controlling scroll speed as a function of touch position and time. Use ``[`DragSortListView#setDragScrollProfile(DragScrollProfile)`](set-drag-scroll-profile.md) to set custom profile. |
| [DragSortListener](-drag-sort-listener.md) | `interface DragSortListener : `[`DropListener`](-drop-listener/index.md)`, `[`DragListener`](-drag-listener/index.md)`, `[`RemoveListener`](-remove-listener/index.md) |
| [DropListener](-drop-listener/index.md) | `interface DropListener`<br>Your implementation of this has to reorder your ListAdapter! Make sure to call ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it in your implementation. |
| [FloatViewManager](-float-view-manager/index.md) | `interface FloatViewManager`<br>Interface for customization of the floating View appearance and dragging behavior. Implement your own and pass it to ``[`#setFloatViewManager`](set-float-view-manager.md). If your own is not passed, the default SimpleFloatViewManager implementation is used. |
| [RemoveListener](-remove-listener/index.md) | `interface RemoveListener`<br>Make sure to call ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it in your implementation. |

### Constructors

| [&lt;init&gt;](-init-.md) | `DragSortListView(context: Context, attrs: AttributeSet?)` |

### Properties

| [DRAG_NEG_X](-d-r-a-g_-n-e-g_-x.md) | `static val DRAG_NEG_X: Int`<br>Drag flag bit. Floating View can move in the negative x direction. |
| [DRAG_NEG_Y](-d-r-a-g_-n-e-g_-y.md) | `static val DRAG_NEG_Y: Int`<br>Drag flag bit. Floating View can move in the negative y direction. This is subtle. What this actually means is that the floating View can be dragged above its starting position. Remove in favor of lower-bounding item position? |
| [DRAG_POS_X](-d-r-a-g_-p-o-s_-x.md) | `static val DRAG_POS_X: Int`<br>Drag flag bit. Floating View can move in the positive x direction. |
| [DRAG_POS_Y](-d-r-a-g_-p-o-s_-y.md) | `static val DRAG_POS_Y: Int`<br>Drag flag bit. Floating View can move in the positive y direction. This is subtle. What this actually means is that, if enabled, the floating View can be dragged below its starting position. Remove in favor of upper-bounding item position? |

### Functions

| [cancelDrag](cancel-drag.md) | `open fun cancelDrag(): Unit`<br>Cancel a drag. Calls #stopDrag(boolean, boolean) with `true` as the first argument. |
| [getFloatAlpha](get-float-alpha.md) | `open fun getFloatAlpha(): Float` |
| [getInputAdapter](get-input-adapter.md) | `open fun getInputAdapter(): ListAdapter?`<br>As opposed to ``[`ListView#getAdapter()`](#), which returns a heavily wrapped ListAdapter (DragSortListView wraps the input ListAdapter {\emph and} ListView wraps the wrapped one). |
| [isDragEnabled](is-drag-enabled.md) | `open fun isDragEnabled(): Boolean` |
| [listViewIntercepted](list-view-intercepted.md) | `open fun listViewIntercepted(): Boolean` |
| [moveCheckState](move-check-state.md) | `open fun moveCheckState(from: Int, to: Int): Unit`<br>Use this to move the check state of an item from one position to another in a drop operation. If you have a choiceMode which is not none, this method must be called when the order of items changes in an underlying adapter which does not have stable IDs (see ``[`ListAdapter#hasStableIds()`](#)). This is because without IDs, the ListView has no way of knowing which items have moved where, and cannot update the check state accordingly. <br> A word of warning about a "feature" in Android that you may run into when dealing with movable list items: for an adapter that *does* have stable IDs, ListView will attempt to locate each item based on its ID and move the check state from the item's old position to the new position — which is all fine and good (and removes the need for calling this function), except for the half-baked approach. Apparently to save time in the naive algorithm used, ListView will only search for an ID in the close neighborhood of the old position. If the user moves an item too far (specifically, more than 20 rows away), ListView will give up and just force the item to be unchecked. So if there is a reasonable chance that the user will move items more than 20 rows away from the original position, you may wish to use an adapter with unstable IDs and call this method manually instead. |
| [moveItem](move-item.md) | `open fun moveItem(from: Int, to: Int): Unit`<br>Move an item, bypassing the drag-sort process. Simply calls through to ``[`DropListener#drop(int, int)`](#). |
| [onInterceptTouchEvent](on-intercept-touch-event.md) | `open fun onInterceptTouchEvent(ev: MotionEvent): Boolean` |
| [onTouchEvent](on-touch-event.md) | `open fun onTouchEvent(ev: MotionEvent): Boolean` |
| [removeCheckState](remove-check-state.md) | `open fun removeCheckState(position: Int): Unit`<br>Use this when an item has been deleted, to move the check state of all following items up one step. If you have a choiceMode which is not none, this method must be called when the order of items changes in an underlying adapter which does not have stable IDs (see ``[`ListAdapter#hasStableIds()`](#)). This is because without IDs, the ListView has no way of knowing which items have moved where, and cannot update the check state accordingly. See also further comments on ``[`#moveCheckState(int, int)`](#). |
| [removeItem](remove-item.md) | `open fun removeItem(which: Int): Unit``open fun removeItem(which: Int, velocityX: Float): Unit`<br>Removes an item from the list and animates the removal. |
| [requestLayout](request-layout.md) | `open fun requestLayout(): Unit` |
| [setAdapter](set-adapter.md) | `open fun setAdapter(adapter: ListAdapter?): Unit`<br>For each DragSortListView Listener interface implemented by `adapter`, this method calls the appropriate set*Listener method with `adapter` as the argument. |
| [setDragEnabled](set-drag-enabled.md) | `open fun setDragEnabled(enabled: Boolean): Unit`<br>Allows for easy toggling between a DragSortListView and a regular old ListView. If enabled, items are draggable, where the drag init mode determines how items are lifted (see (). If disabled, items cannot be dragged. |
| [setDragListener](set-drag-listener.md) | `open fun setDragListener(l: `[`DragListener`](-drag-listener/index.md)`): Unit` |
| [setDragScrollProfile](set-drag-scroll-profile.md) | `open fun setDragScrollProfile(ssp: `[`DragScrollProfile`](-drag-scroll-profile/index.md)`?): Unit`<br>Completely custom scroll speed profile. Default increases linearly with position and is constant in time. Create your own by implementing DragSortListView.DragScrollProfile. |
| [setDragScrollStart](set-drag-scroll-start.md) | `open fun setDragScrollStart(heightFraction: Float): Unit`<br>Set the width of each drag scroll region by specifying a fraction of the ListView height. |
| [setDragScrollStarts](set-drag-scroll-starts.md) | `open fun setDragScrollStarts(upperFrac: Float, lowerFrac: Float): Unit`<br>Set the width of each drag scroll region by specifying a fraction of the ListView height. |
| [setDragSortListener](set-drag-sort-listener.md) | `open fun setDragSortListener(l: `[`DragSortListener`](-drag-sort-listener.md)`): Unit` |
| [setDropListener](set-drop-listener.md) | `open fun setDropListener(l: `[`DropListener`](-drop-listener/index.md)`): Unit`<br>This better reorder your ListAdapter! DragSortListView does not do this for you; doesn't make sense to. Make sure ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it is called in your implementation. Furthermore, if you have a choiceMode other than none and the ListAdapter does not return true for ``[`ListAdapter#hasStableIds()`](#), you will need to call ``[`#moveCheckState(int, int)`](#) to move the check boxes along with the list items. |
| [setFloatAlpha](set-float-alpha.md) | `open fun setFloatAlpha(alpha: Float): Unit`<br>Usually called from a FloatViewManager. The float alpha will be reset to the xml-defined value every time a drag is stopped. |
| [setFloatViewManager](set-float-view-manager.md) | `open fun setFloatViewManager(manager: `[`FloatViewManager`](-float-view-manager/index.md)`): Unit` |
| [setMaxScrollSpeed](set-max-scroll-speed.md) | `open fun setMaxScrollSpeed(max: Float): Unit`<br>Set maximum drag scroll speed in positions/second. Only applies if using default ScrollSpeedProfile. |
| [setRemoveListener](set-remove-listener.md) | `open fun setRemoveListener(l: `[`RemoveListener`](-remove-listener/index.md)`): Unit`<br>Probably a no-brainer, but make sure that your remove listener calls ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it. When an item removal occurs, DragSortListView relies on a redraw of all the items to recover invisible views and such. Strictly speaking, if you remove something, your dataset has changed... |
| [startDrag](start-drag.md) | `open fun startDrag(position: Int, dragFlags: Int, deltaX: Int, deltaY: Int): Boolean`<br>Start a drag of item at `position` using the registered FloatViewManager. Calls through to ``[`#startDrag(int,View,int,int,int)`](#) after obtaining the floating View from the FloatViewManager.`open fun startDrag(position: Int, floatView: View?, dragFlags: Int, deltaX: Int, deltaY: Int): Boolean`<br>Start a drag of item at `position` without using a FloatViewManager. |
| [stopDrag](stop-drag.md) | `open fun stopDrag(remove: Boolean): Boolean`<br>Stop a drag in progress. Pass `true` if you would like to remove the dragged item from the list.`open fun stopDrag(remove: Boolean, velocityX: Float): Boolean` |
| [stopDragWithVelocity](stop-drag-with-velocity.md) | `open fun stopDragWithVelocity(remove: Boolean, velocityX: Float): Boolean` |

