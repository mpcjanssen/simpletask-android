[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortController](index.md) / [&lt;init&gt;](.)

# &lt;init&gt;

`DragSortController(@NonNull dslv: @NonNull `[`DragSortListView`](../-drag-sort-list-view/index.md)`)`

Calls #DragSortController(DragSortListView, int) with a 0 drag handle id, FLING_RIGHT_REMOVE remove mode, and ON_DOWN drag init. By default, sorting is enabled, and removal is disabled.

### Parameters

`dslv` - The DSLV instance`DragSortController(@NonNull dslv: @NonNull `[`DragSortListView`](../-drag-sort-list-view/index.md)`, dragHandleId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, dragInitMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, removeMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`
`DragSortController(@NonNull dslv: @NonNull `[`DragSortListView`](../-drag-sort-list-view/index.md)`, dragHandleId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, dragInitMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, removeMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, clickRemoveId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)``DragSortController(@NonNull dslv: @NonNull `[`DragSortListView`](../-drag-sort-list-view/index.md)`, dragHandleId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, dragInitMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, removeMode: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, clickRemoveId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, flingHandleId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`

By default, sorting is enabled, and removal is disabled.

### Parameters

`dslv` - The DSLV instance

`dragHandleId` - The resource id of the View that represents the drag handle in a list item.