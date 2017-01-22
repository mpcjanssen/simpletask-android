[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.adapters](../index.md) / [ItemDialogAdapter](.)

# ItemDialogAdapter

`class ItemDialogAdapter : Adapter<`[`ViewHolder`](-view-holder/index.md)`>` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/adapters/ItemDialogAdapter.kt#L12)

### Types

| [ViewHolder](-view-holder/index.md) | `class ViewHolder : ViewHolder` |

### Constructors

| [&lt;init&gt;](-init-.md) | `ItemDialogAdapter(mItems: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>, onAll: `[`HashSet`](http://docs.oracle.com/javase/6/docs/api/java/util/HashSet.html)`<String>, onSome: `[`HashSet`](http://docs.oracle.com/javase/6/docs/api/java/util/HashSet.html)`<String>)` |

### Properties

| [currentState](current-state.md) | `val currentState: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<Boolean?>` |

### Functions

| [getItemCount](get-item-count.md) | `fun getItemCount(): Int` |
| [onBindViewHolder](on-bind-view-holder.md) | `fun onBindViewHolder(holder: `[`ViewHolder`](-view-holder/index.md)`, position: Int): Unit` |
| [onCreateViewHolder](on-create-view-holder.md) | `fun onCreateViewHolder(parent: ViewGroup, viewType: Int): `[`ViewHolder`](-view-holder/index.md) |

