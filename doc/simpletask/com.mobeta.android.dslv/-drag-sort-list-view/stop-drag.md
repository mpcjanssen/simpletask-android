[simpletask](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [stopDrag](.)

# stopDrag

`open fun stopDrag(remove: Boolean): Boolean` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L1566)

Stop a drag in progress. Pass `true` if you would like to remove the dragged item from the list.

### Parameters

`remove` - Remove the dragged item from the list. Calls a registered RemoveListener, if one exists. Otherwise, calls the DropListener, if one exists.

**Return**
True if the stop was successful. False if there is no floating View.

`open fun stopDrag(remove: Boolean, velocityX: Float): Boolean` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L1577)