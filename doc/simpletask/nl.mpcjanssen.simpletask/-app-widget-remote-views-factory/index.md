[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [AppWidgetRemoteViewsFactory](.)

# AppWidgetRemoteViewsFactory

`data class AppWidgetRemoteViewsFactory : RemoteViewsFactory` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/AppWidgetService.kt#L31)

### Constructors

| [&lt;init&gt;](-init-.md) | `AppWidgetRemoteViewsFactory(intent: Intent)` |

### Properties

| [intent](intent.md) | `val intent: Intent` |
| [visibleTasks](visible-tasks.md) | `var visibleTasks: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>` |
| [widgetId](widget-id.md) | `val widgetId: Int` |

### Functions

| [getCount](get-count.md) | `fun getCount(): Int` |
| [getFilter](get-filter.md) | `fun getFilter(): `[`ActiveFilter`](../-active-filter/index.md) |
| [getItemId](get-item-id.md) | `fun getItemId(arg0: Int): Long` |
| [getLoadingView](get-loading-view.md) | `fun getLoadingView(): RemoteViews?` |
| [getViewAt](get-view-at.md) | `fun getViewAt(position: Int): RemoteViews?` |
| [getViewTypeCount](get-view-type-count.md) | `fun getViewTypeCount(): Int` |
| [hasStableIds](has-stable-ids.md) | `fun hasStableIds(): Boolean` |
| [moduleName](module-name.md) | `fun moduleName(): String` |
| [onCreate](on-create.md) | `fun onCreate(): Unit` |
| [onDataSetChanged](on-data-set-changed.md) | `fun onDataSetChanged(): Unit` |
| [onDestroy](on-destroy.md) | `fun onDestroy(): Unit` |
| [setFilteredTasks](set-filtered-tasks.md) | `fun setFilteredTasks(): Unit` |

### Companion Object Properties

| [TAG](-t-a-g.md) | `val TAG: String` |

