[simpletask](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [FloatViewManager](index.md) / [onDestroyFloatView](.)

# onDestroyFloatView

`abstract fun onDestroyFloatView(floatView: View): Unit` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L2465)

Called when the float View is dropped; lets you perform any necessary cleanup. The internal DSLV floating View reference is set to null immediately after this is called.

### Parameters

`floatView` - The floating View passed to ``[`#onCreateFloatView(int)`](on-create-float-view.md).