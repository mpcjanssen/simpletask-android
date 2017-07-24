[app](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortItemView](.)

# DragSortItemView

`open class DragSortItemView : ViewGroup`

Lightweight ViewGroup that wraps list items obtained from user's ListAdapter. ItemView expects a single child that has a definite height (i.e. the child's layout height is not MATCH_PARENT). The width of ItemView will always match the width of its child (that is, the width MeasureSpec given to ItemView is passed directly to the child, and the ItemView measured width is set to the child's measured width). The height of ItemView can be anything; the The purpose of this class is to optimize slide shuffle animations.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `DragSortItemView(context: Context)` |

### Functions

| Name | Summary |
|---|---|
| [getGravity](get-gravity.md) | `open fun getGravity(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [setGravity](set-gravity.md) | `open fun setGravity(gravity: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [DragSortItemViewCheckable](../-drag-sort-item-view-checkable/index.md) | `open class DragSortItemViewCheckable : DragSortItemView, Checkable`<br>Lightweight ViewGroup that wraps list items obtained from user's ListAdapter. ItemView expects a single child that has a definite height (i.e. the child's layout height is not MATCH_PARENT). The width of ItemView will always match the width of its child (that is, the width MeasureSpec given to ItemView is passed directly to the child, and the ItemView measured width is set to the child's measured width). The height of ItemView can be anything; the The purpose of this class is to optimize slide shuffle animations. |
