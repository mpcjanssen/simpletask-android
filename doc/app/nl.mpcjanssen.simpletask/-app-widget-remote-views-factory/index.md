[app](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [AppWidgetRemoteViewsFactory](.)

# AppWidgetRemoteViewsFactory

`data class AppWidgetRemoteViewsFactory : RemoteViewsFactory`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AppWidgetRemoteViewsFactory(intent: Intent)` |

### Properties

| Name | Summary |
|---|---|
| [intent](intent.md) | `val intent: Intent` |
| [visibleTasks](visible-tasks.md) | `var visibleTasks: `[`ArrayList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-list/index.html)`<`[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`>` |
| [widgetId](widget-id.md) | `val widgetId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Functions

| Name | Summary |
|---|---|
| [getCount](get-count.md) | `fun getCount(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [getFilter](get-filter.md) | `fun getFilter(): `[`ActiveFilter`](../-active-filter/index.md) |
| [getItemId](get-item-id.md) | `fun getItemId(arg0: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [getLoadingView](get-loading-view.md) | `fun getLoadingView(): RemoteViews?` |
| [getViewAt](get-view-at.md) | `fun getViewAt(position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): RemoteViews?` |
| [getViewTypeCount](get-view-type-count.md) | `fun getViewTypeCount(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [hasStableIds](has-stable-ids.md) | `fun hasStableIds(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [moduleName](module-name.md) | `fun moduleName(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [onCreate](on-create.md) | `fun onCreate(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onDataSetChanged](on-data-set-changed.md) | `fun onDataSetChanged(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onDestroy](on-destroy.md) | `fun onDestroy(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setFilteredTasks](set-filtered-tasks.md) | `fun setFilteredTasks(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Companion Object Properties

| Name | Summary |
|---|---|
| [TAG](-t-a-g.md) | `val TAG: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
