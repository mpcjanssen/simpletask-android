[simpletask](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [RemoveListener](.)

# RemoveListener

`interface RemoveListener` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L2549)

Make sure to call ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it in your implementation.

**Author**
heycosmo

### Functions

| [remove](remove.md) | `abstract fun remove(which: Int): Unit` |

### Inheritors

| [DragSortListener](../-drag-sort-listener.md) | `interface DragSortListener : `[`DropListener`](../-drop-listener/index.md)`, `[`DragListener`](../-drag-listener/index.md)`, RemoveListener` |

