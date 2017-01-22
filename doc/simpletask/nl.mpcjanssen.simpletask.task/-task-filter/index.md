[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.task](../index.md) / [TaskFilter](.)

# TaskFilter

`interface TaskFilter` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/task/TaskFilter.kt#L3)

### Functions

| [apply](apply.md) | `abstract fun apply(task: `[`Task`](../-task/index.md)`): Boolean` |

### Inheritors

| [ByContextFilter](../-by-context-filter/index.md) | `class ByContextFilter : TaskFilter`<br>A filter that matches Tasks containing the specified contexts |
| [ByPriorityFilter](../-by-priority-filter/index.md) | `class ByPriorityFilter : TaskFilter`<br>A filter that matches Tasks containing the specified priorities |
| [ByProjectFilter](../-by-project-filter/index.md) | `class ByProjectFilter : TaskFilter`<br>A filter that matches Tasks containing the specified projects |
| [ByTextFilter](../-by-text-filter/index.md) | `class ByTextFilter : TaskFilter`<br>A filter that matches Tasks containing the specified text |

