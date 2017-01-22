[simpletask](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [DragScrollProfile](.)

# DragScrollProfile

`interface DragScrollProfile` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L2786)

Interface for controlling scroll speed as a function of touch position and time. Use ``[`DragSortListView#setDragScrollProfile(DragScrollProfile)`](../set-drag-scroll-profile.md) to set custom profile.

**Author**
heycosmo

### Functions

| [getSpeed](get-speed.md) | `abstract fun getSpeed(w: Float, t: Long): Float`<br>Return a scroll speed in pixels/millisecond. Always return a positive number. |

