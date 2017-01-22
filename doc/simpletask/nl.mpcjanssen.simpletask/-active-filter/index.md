[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [ActiveFilter](.)

# ActiveFilter

`class ActiveFilter` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/ActiveFilter.kt#L21)

Active filter, has methods for serialization in several formats

### Constructors

| [&lt;init&gt;](-init-.md) | `ActiveFilter(options: `[`FilterOptions`](../-filter-options/index.md)`)`<br>Active filter, has methods for serialization in several formats |

### Properties

| [contexts](contexts.md) | `var contexts: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [contextsNot](contexts-not.md) | `var contextsNot: Boolean` |
| [createIsThreshold](create-is-threshold.md) | `var createIsThreshold: Boolean` |
| [hideCompleted](hide-completed.md) | `var hideCompleted: Boolean` |
| [hideCreateDate](hide-create-date.md) | `var hideCreateDate: Boolean` |
| [hideFuture](hide-future.md) | `var hideFuture: Boolean` |
| [hideHidden](hide-hidden.md) | `var hideHidden: Boolean` |
| [hideLists](hide-lists.md) | `var hideLists: Boolean` |
| [hideTags](hide-tags.md) | `var hideTags: Boolean` |
| [name](name.md) | `var name: String?` |
| [options](options.md) | `val options: `[`FilterOptions`](../-filter-options/index.md) |
| [prefName](pref-name.md) | `var prefName: String?` |
| [priorities](priorities.md) | `var priorities: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`Priority`](../../nl.mpcjanssen.simpletask.task/-priority/index.md)`>` |
| [prioritiesNot](priorities-not.md) | `var prioritiesNot: Boolean` |
| [projects](projects.md) | `var projects: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [projectsNot](projects-not.md) | `var projectsNot: Boolean` |
| [proposedName](proposed-name.md) | `val proposedName: String` |
| [script](script.md) | `var script: String?` |
| [scriptTestTask](script-test-task.md) | `var scriptTestTask: String?` |
| [search](search.md) | `var search: String?` |
| [useScript](use-script.md) | `var useScript: Boolean` |

### Functions

| [apply](apply.md) | `fun apply(items: List<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>?): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>` |
| [clear](clear.md) | `fun clear(): Unit` |
| [getSort](get-sort.md) | `fun getSort(defaultSort: Array<String>?): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [getTitle](get-title.md) | `fun getTitle(visible: Int, total: Long, prio: CharSequence, tag: CharSequence, list: CharSequence, search: CharSequence, script: CharSequence, filterApplied: CharSequence, noFilter: CharSequence): String` |
| [hasFilter](has-filter.md) | `fun hasFilter(): Boolean` |
| [initFromIntent](init-from-intent.md) | `fun initFromIntent(intent: Intent): Unit` |
| [initFromJSON](init-from-j-s-o-n.md) | `fun initFromJSON(json: JSONObject?): Unit` |
| [initFromPrefs](init-from-prefs.md) | `fun initFromPrefs(prefs: SharedPreferences): Unit` |
| [saveInIntent](save-in-intent.md) | `fun saveInIntent(target: Intent?): Unit` |
| [saveInJSON](save-in-j-s-o-n.md) | `fun saveInJSON(json: JSONObject): Unit` |
| [saveInPrefs](save-in-prefs.md) | `fun saveInPrefs(prefs: SharedPreferences?): Unit` |
| [setSort](set-sort.md) | `fun setSort(sort: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>): Unit` |
| [toString](to-string.md) | `fun toString(): String` |

### Companion Object Properties

| [INTENT_CONTEXTS_FILTER](-i-n-t-e-n-t_-c-o-n-t-e-x-t-s_-f-i-l-t-e-r.md) | `const val INTENT_CONTEXTS_FILTER: String` |
| [INTENT_CONTEXTS_FILTER_NOT](-i-n-t-e-n-t_-c-o-n-t-e-x-t-s_-f-i-l-t-e-r_-n-o-t.md) | `const val INTENT_CONTEXTS_FILTER_NOT: String` |
| [INTENT_CREATE_AS_THRESHOLD](-i-n-t-e-n-t_-c-r-e-a-t-e_-a-s_-t-h-r-e-s-h-o-l-d.md) | `const val INTENT_CREATE_AS_THRESHOLD: String` |
| [INTENT_EXTRA_DELIMITERS](-i-n-t-e-n-t_-e-x-t-r-a_-d-e-l-i-m-i-t-e-r-s.md) | `const val INTENT_EXTRA_DELIMITERS: String` |
| [INTENT_HIDE_COMPLETED_FILTER](-i-n-t-e-n-t_-h-i-d-e_-c-o-m-p-l-e-t-e-d_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_COMPLETED_FILTER: String` |
| [INTENT_HIDE_CREATE_DATE_FILTER](-i-n-t-e-n-t_-h-i-d-e_-c-r-e-a-t-e_-d-a-t-e_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_CREATE_DATE_FILTER: String` |
| [INTENT_HIDE_FUTURE_FILTER](-i-n-t-e-n-t_-h-i-d-e_-f-u-t-u-r-e_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_FUTURE_FILTER: String` |
| [INTENT_HIDE_HIDDEN_FILTER](-i-n-t-e-n-t_-h-i-d-e_-h-i-d-d-e-n_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_HIDDEN_FILTER: String` |
| [INTENT_HIDE_LISTS_FILTER](-i-n-t-e-n-t_-h-i-d-e_-l-i-s-t-s_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_LISTS_FILTER: String` |
| [INTENT_HIDE_TAGS_FILTER](-i-n-t-e-n-t_-h-i-d-e_-t-a-g-s_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_TAGS_FILTER: String` |
| [INTENT_JSON](-i-n-t-e-n-t_-j-s-o-n.md) | `const val INTENT_JSON: String` |
| [INTENT_LUA_MODULE](-i-n-t-e-n-t_-l-u-a_-m-o-d-u-l-e.md) | `const val INTENT_LUA_MODULE: String` |
| [INTENT_PRIORITIES_FILTER](-i-n-t-e-n-t_-p-r-i-o-r-i-t-i-e-s_-f-i-l-t-e-r.md) | `const val INTENT_PRIORITIES_FILTER: String` |
| [INTENT_PRIORITIES_FILTER_NOT](-i-n-t-e-n-t_-p-r-i-o-r-i-t-i-e-s_-f-i-l-t-e-r_-n-o-t.md) | `const val INTENT_PRIORITIES_FILTER_NOT: String` |
| [INTENT_PROJECTS_FILTER](-i-n-t-e-n-t_-p-r-o-j-e-c-t-s_-f-i-l-t-e-r.md) | `const val INTENT_PROJECTS_FILTER: String` |
| [INTENT_PROJECTS_FILTER_NOT](-i-n-t-e-n-t_-p-r-o-j-e-c-t-s_-f-i-l-t-e-r_-n-o-t.md) | `const val INTENT_PROJECTS_FILTER_NOT: String` |
| [INTENT_SCRIPT_FILTER](-i-n-t-e-n-t_-s-c-r-i-p-t_-f-i-l-t-e-r.md) | `const val INTENT_SCRIPT_FILTER: String` |
| [INTENT_SCRIPT_TEST_TASK_FILTER](-i-n-t-e-n-t_-s-c-r-i-p-t_-t-e-s-t_-t-a-s-k_-f-i-l-t-e-r.md) | `const val INTENT_SCRIPT_TEST_TASK_FILTER: String` |
| [INTENT_SORT_ORDER](-i-n-t-e-n-t_-s-o-r-t_-o-r-d-e-r.md) | `const val INTENT_SORT_ORDER: String` |
| [INTENT_TITLE](-i-n-t-e-n-t_-t-i-t-l-e.md) | `const val INTENT_TITLE: String` |
| [INTENT_USE_SCRIPT_FILTER](-i-n-t-e-n-t_-u-s-e_-s-c-r-i-p-t_-f-i-l-t-e-r.md) | `const val INTENT_USE_SCRIPT_FILTER: String` |
| [NORMAL_SORT](-n-o-r-m-a-l_-s-o-r-t.md) | `const val NORMAL_SORT: String` |
| [REVERSED_SORT](-r-e-v-e-r-s-e-d_-s-o-r-t.md) | `const val REVERSED_SORT: String` |
| [SORT_SEPARATOR](-s-o-r-t_-s-e-p-a-r-a-t-o-r.md) | `const val SORT_SEPARATOR: String` |

