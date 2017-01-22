[simpletask](../../index.md) / [com.mobeta.android.dslv](../index.md) / [DragSortItemViewCheckable](.)

# DragSortItemViewCheckable

`open class DragSortItemViewCheckable : `[`DragSortItemView`](../-drag-sort-item-view/index.md)`, Checkable` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/mobeta/android/dslv/DragSortItemViewCheckable.java#L22)

Lightweight ViewGroup that wraps list items obtained from user's ListAdapter. ItemView expects a single child that has a definite height (i.e. the child's layout height is not MATCH_PARENT). The width of ItemView will always match the width of its child (that is, the width MeasureSpec given to ItemView is passed directly to the child, and the ItemView measured width is set to the child's measured width). The height of ItemView can be anything; the The purpose of this class is to optimize slide shuffle animations.

### Constructors

| [&lt;init&gt;](-init-.md) | `DragSortItemViewCheckable(context: Context)` |

### Functions

| [isChecked](is-checked.md) | `open fun isChecked(): Boolean` |
| [setChecked](set-checked.md) | `open fun setChecked(checked: Boolean): Unit` |
| [toggle](toggle.md) | `open fun toggle(): Unit` |

### Inherited Functions

| [getGravity](../-drag-sort-item-view/get-gravity.md) | `open fun getGravity(): Int` |
| [setGravity](../-drag-sort-item-view/set-gravity.md) | `open fun setGravity(gravity: Int): Unit` |

