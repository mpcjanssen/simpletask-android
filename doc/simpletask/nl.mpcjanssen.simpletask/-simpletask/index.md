[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [Simpletask](.)

# Simpletask

`class Simpletask : `[`ThemedNoActionBarActivity`](../-themed-no-action-bar-activity/index.md) [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/Simpletask.kt#L64)

### Types

| [Mode](-mode/index.md) | `enum class Mode` |
| [TaskAdapter](-task-adapter/index.md) | `inner class TaskAdapter : Adapter<`[`TaskViewHolder`](-task-view-holder/index.md)`>` |
| [TaskViewHolder](-task-view-holder/index.md) | `class TaskViewHolder : ViewHolder` |

### Constructors

| [&lt;init&gt;](-init-.md) | `Simpletask()` |

### Properties

| [listView](list-view.md) | `val listView: RecyclerView?` |
| [savedFilters](saved-filters.md) | `val savedFilters: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`ActiveFilter`](../-active-filter/index.md)`>` |
| [textSize](text-size.md) | `var textSize: Float` |

### Functions

| [createFilterShortcut](create-filter-shortcut.md) | `fun createFilterShortcut(filter: `[`ActiveFilter`](../-active-filter/index.md)`): Unit` |
| [exportFilters](export-filters.md) | `fun exportFilters(exportFile: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`): Unit` |
| [importFilters](import-filters.md) | `fun importFilters(importFile: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`): Unit` |
| [onActivityResult](on-activity-result.md) | `fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Unit` |
| [onAddFilterClick](on-add-filter-click.md) | `fun onAddFilterClick(): Unit`<br>Handle add filter click * |
| [onBackPressed](on-back-pressed.md) | `fun onBackPressed(): Unit` |
| [onClearClick](on-clear-click.md) | `fun onClearClick(v: View): Unit`<br>Handle clear filter click * |
| [onConfigurationChanged](on-configuration-changed.md) | `fun onConfigurationChanged(newConfig: Configuration): Unit` |
| [onCreate](on-create.md) | `fun onCreate(savedInstanceState: Bundle?): Unit` |
| [onCreateOptionsMenu](on-create-options-menu.md) | `fun onCreateOptionsMenu(menu: Menu): Boolean` |
| [onDestroy](on-destroy.md) | `fun onDestroy(): Unit` |
| [onNewIntent](on-new-intent.md) | `fun onNewIntent(intent: Intent): Unit` |
| [onOptionsItemSelected](on-options-item-selected.md) | `fun onOptionsItemSelected(item: MenuItem): Boolean` |
| [onPause](on-pause.md) | `fun onPause(): Unit` |
| [onPostCreate](on-post-create.md) | `fun onPostCreate(savedInstanceState: Bundle?): Unit` |
| [onRequestPermissionsResult](on-request-permissions-result.md) | `fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Unit` |
| [onResume](on-resume.md) | `fun onResume(): Unit` |
| [onSearchRequested](on-search-requested.md) | `fun onSearchRequested(): Boolean` |
| [showListViewProgress](show-list-view-progress.md) | `fun showListViewProgress(show: Boolean): Unit` |
| [startFilterActivity](start-filter-activity.md) | `fun startFilterActivity(): Unit` |

### Companion Object Properties

| [URI_BASE](-u-r-i_-b-a-s-e.md) | `val URI_BASE: Uri` |
| [URI_SEARCH](-u-r-i_-s-e-a-r-c-h.md) | `val URI_SEARCH: Uri` |

