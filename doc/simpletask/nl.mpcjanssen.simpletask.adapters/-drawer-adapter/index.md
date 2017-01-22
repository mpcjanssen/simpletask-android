[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.adapters](../index.md) / [DrawerAdapter](.)

# DrawerAdapter

`class DrawerAdapter : BaseAdapter, ListAdapter` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/adapters/DrawerAdapter.kt#L13)

### Constructors

| [&lt;init&gt;](-init-.md) | `DrawerAdapter(m_inflater: LayoutInflater, contextHeader: String, contexts: List<String>, projectHeader: String, projects: List<String>)` |

### Properties

| [contextHeaderPosition](context-header-position.md) | `var contextHeaderPosition: Int` |
| [projectsHeaderPosition](projects-header-position.md) | `var projectsHeaderPosition: Int` |

### Functions

| [areAllItemsEnabled](are-all-items-enabled.md) | `fun areAllItemsEnabled(): Boolean` |
| [getCount](get-count.md) | `fun getCount(): Int` |
| [getIndexOf](get-index-of.md) | `fun getIndexOf(item: String): Int` |
| [getItem](get-item.md) | `fun getItem(position: Int): String` |
| [getItemId](get-item-id.md) | `fun getItemId(position: Int): Long` |
| [getItemViewType](get-item-view-type.md) | `fun getItemViewType(position: Int): Int` |
| [getView](get-view.md) | `fun getView(position: Int, convertView: View?, parent: ViewGroup): View` |
| [getViewTypeCount](get-view-type-count.md) | `fun getViewTypeCount(): Int` |
| [hasStableIds](has-stable-ids.md) | `fun hasStableIds(): Boolean` |
| [isEmpty](is-empty.md) | `fun isEmpty(): Boolean` |
| [isEnabled](is-enabled.md) | `fun isEnabled(position: Int): Boolean` |

