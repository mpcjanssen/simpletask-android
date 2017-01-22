[simpletask](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [DragScrollProfile](index.md) / [getSpeed](.)

# getSpeed

`abstract fun getSpeed(w: Float, t: Long): Float` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L2797)

Return a scroll speed in pixels/millisecond. Always return a positive number.

### Parameters

`w` - Normalized position in scroll region (i.e. w \in [0,1]). Small w typically means slow scrolling.

`t` - Time (in milliseconds) since start of scroll (handy if you want scroll acceleration).

**Return**
Scroll speed at position w and time t in pixels/ms.

