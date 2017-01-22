[simpletask](../../../index.md) / [nl.mpcjanssen.simpletask](../../index.md) / [Simpletask](../index.md) / [TaskAdapter](.)

# TaskAdapter

`inner class TaskAdapter : Adapter<`[`TaskViewHolder`](../-task-view-holder/index.md)`>` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/Simpletask.kt#L1224)

### Constructors

| [&lt;init&gt;](-init-.md) | `TaskAdapter(m_inflater: LayoutInflater)` |

### Properties

| [countVisibleTodoItems](count-visible-todo-items.md) | `val countVisibleTodoItems: Int` |

### Functions

| [bindHeader](bind-header.md) | `fun bindHeader(holder: `[`TaskViewHolder`](../-task-view-holder/index.md)`, position: Int): Unit` |
| [bindTask](bind-task.md) | `fun bindTask(holder: `[`TaskViewHolder`](../-task-view-holder/index.md)`, position: Int): Unit` |
| [getItemCount](get-item-count.md) | `fun getItemCount(): Int` |
| [getItemId](get-item-id.md) | `fun getItemId(position: Int): Long` |
| [getItemViewType](get-item-view-type.md) | `fun getItemViewType(position: Int): Int` |
| [getPosition](get-position.md) | `fun getPosition(task: `[`TodoItem`](../../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`): Int` |
| [onBindViewHolder](on-bind-view-holder.md) | `fun onBindViewHolder(holder: `[`TaskViewHolder`](../-task-view-holder/index.md)`?, position: Int): Unit` |
| [onCreateViewHolder](on-create-view-holder.md) | `fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): `[`TaskViewHolder`](../-task-view-holder/index.md) |

