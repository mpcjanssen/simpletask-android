[simpletask](../../../index.md) / [com.mobeta.android.dslv](../../index.md) / [DragSortListView](../index.md) / [DropListener](.)

# DropListener

`interface DropListener` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortListView.java#L2537)

Your implementation of this has to reorder your ListAdapter! Make sure to call ``[`BaseAdapter#notifyDataSetChanged()`](#) or something like it in your implementation.

**Author**
heycosmo

### Functions

| [drop](drop.md) | `abstract fun drop(from: Int, to: Int): Unit` |

### Inheritors

| [DragSortListener](../-drag-sort-listener.md) | `interface DragSortListener : DropListener, `[`DragListener`](../-drag-listener/index.md)`, `[`RemoveListener`](../-remove-listener/index.md) |

