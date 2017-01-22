[simpletask](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortListView](index.md) / [getInputAdapter](.)

# getInputAdapter

`@Nullable open fun getInputAdapter(): @Nullable ListAdapter?` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L622)

As opposed to ``[`ListView#getAdapter()`](#), which returns a heavily wrapped ListAdapter (DragSortListView wraps the input ListAdapter {\emph and} ListView wraps the wrapped one).

**Return**
The ListAdapter set as the argument of (

