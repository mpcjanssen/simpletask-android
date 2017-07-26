[app](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [FloatViewManager](index.md) / [onCreateFloatView](.)

# onCreateFloatView

`abstract fun onCreateFloatView(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): View`

Return the floating View for item at `position`. DragSortListView will measure and layout this View for you, so feel free to just inflate it. You can help DSLV by setting some ViewGroup.LayoutParams on this View; otherwise it will set some for you (with a width of MATCH_PARENT and a height of WRAP_CONTENT).

### Parameters

`position` - Position of item to drag (NOTE: `position` excludes header Views; thus, if you want to call ``[`ListView#getChildAt(int)`](#), you will need to add ``[`ListView#getHeaderViewsCount()`](#) to the index).

**Return**
The View you wish to display as the floating View.

