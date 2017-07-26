[app](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [ActiveFilter](.)

# ActiveFilter

`class ActiveFilter`

Active filter, has methods for serialization in several formats

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ActiveFilter(options: `[`FilterOptions`](../-filter-options/index.md)`)`<br>Active filter, has methods for serialization in several formats |

### Properties

| Name | Summary |
|---|---|
| [contexts](contexts.md) | `var contexts: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [contextsNot](contexts-not.md) | `var contextsNot: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [createIsThreshold](create-is-threshold.md) | `var createIsThreshold: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hideCompleted](hide-completed.md) | `var hideCompleted: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hideCreateDate](hide-create-date.md) | `var hideCreateDate: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hideFuture](hide-future.md) | `var hideFuture: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hideHidden](hide-hidden.md) | `var hideHidden: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hideLists](hide-lists.md) | `var hideLists: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hideTags](hide-tags.md) | `var hideTags: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [name](name.md) | `var name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
| [options](options.md) | `val options: `[`FilterOptions`](../-filter-options/index.md) |
| [prefName](pref-name.md) | `var prefName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
| [priorities](priorities.md) | `var priorities: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`Priority`](../../nl.mpcjanssen.simpletask.task/-priority/index.md)`>` |
| [prioritiesNot](priorities-not.md) | `var prioritiesNot: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [projects](projects.md) | `var projects: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [projectsNot](projects-not.md) | `var projectsNot: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [proposedName](proposed-name.md) | `val proposedName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [script](script.md) | `var script: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
| [scriptTestTask](script-test-task.md) | `var scriptTestTask: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
| [search](search.md) | `var search: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
| [useScript](use-script.md) | `var useScript: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

### Functions

| Name | Summary |
|---|---|
| [apply](apply.md) | `fun apply(items: `[`Sequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence/index.html)`<`[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`>?): `[`Sequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence/index.html)`<`[`Task`](../../nl.mpcjanssen.simpletask.task/-task/index.md)`>` |
| [clear](clear.md) | `fun clear(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [getSort](get-sort.md) | `fun getSort(defaultSort: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>?): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>` |
| [getTitle](get-title.md) | `fun getTitle(visible: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, total: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, prio: `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html)`, tag: `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html)`, list: `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html)`, search: `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html)`, script: `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html)`, filterApplied: `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html)`, noFilter: `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [hasFilter](has-filter.md) | `fun hasFilter(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [initFromIntent](init-from-intent.md) | `fun initFromIntent(intent: Intent): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [initFromJSON](init-from-j-s-o-n.md) | `fun initFromJSON(json: JSONObject?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [initFromPrefs](init-from-prefs.md) | `fun initFromPrefs(prefs: SharedPreferences): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [saveInIntent](save-in-intent.md) | `fun saveInIntent(target: Intent?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [saveInJSON](save-in-j-s-o-n.md) | `fun saveInJSON(json: JSONObject): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [saveInPrefs](save-in-prefs.md) | `fun saveInPrefs(prefs: SharedPreferences?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setSort](set-sort.md) | `fun setSort(sort: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [toString](to-string.md) | `fun toString(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Companion Object Properties

| Name | Summary |
|---|---|
| [INTENT_CONTEXTS_FILTER](-i-n-t-e-n-t_-c-o-n-t-e-x-t-s_-f-i-l-t-e-r.md) | `const val INTENT_CONTEXTS_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_CONTEXTS_FILTER_NOT](-i-n-t-e-n-t_-c-o-n-t-e-x-t-s_-f-i-l-t-e-r_-n-o-t.md) | `const val INTENT_CONTEXTS_FILTER_NOT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_CREATE_AS_THRESHOLD](-i-n-t-e-n-t_-c-r-e-a-t-e_-a-s_-t-h-r-e-s-h-o-l-d.md) | `const val INTENT_CREATE_AS_THRESHOLD: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_EXTRA_DELIMITERS](-i-n-t-e-n-t_-e-x-t-r-a_-d-e-l-i-m-i-t-e-r-s.md) | `const val INTENT_EXTRA_DELIMITERS: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_HIDE_COMPLETED_FILTER](-i-n-t-e-n-t_-h-i-d-e_-c-o-m-p-l-e-t-e-d_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_COMPLETED_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_HIDE_CREATE_DATE_FILTER](-i-n-t-e-n-t_-h-i-d-e_-c-r-e-a-t-e_-d-a-t-e_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_CREATE_DATE_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_HIDE_FUTURE_FILTER](-i-n-t-e-n-t_-h-i-d-e_-f-u-t-u-r-e_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_FUTURE_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_HIDE_HIDDEN_FILTER](-i-n-t-e-n-t_-h-i-d-e_-h-i-d-d-e-n_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_HIDDEN_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_HIDE_LISTS_FILTER](-i-n-t-e-n-t_-h-i-d-e_-l-i-s-t-s_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_LISTS_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_HIDE_TAGS_FILTER](-i-n-t-e-n-t_-h-i-d-e_-t-a-g-s_-f-i-l-t-e-r.md) | `const val INTENT_HIDE_TAGS_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_JSON](-i-n-t-e-n-t_-j-s-o-n.md) | `const val INTENT_JSON: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_LUA_MODULE](-i-n-t-e-n-t_-l-u-a_-m-o-d-u-l-e.md) | `const val INTENT_LUA_MODULE: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_PRIORITIES_FILTER](-i-n-t-e-n-t_-p-r-i-o-r-i-t-i-e-s_-f-i-l-t-e-r.md) | `const val INTENT_PRIORITIES_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_PRIORITIES_FILTER_NOT](-i-n-t-e-n-t_-p-r-i-o-r-i-t-i-e-s_-f-i-l-t-e-r_-n-o-t.md) | `const val INTENT_PRIORITIES_FILTER_NOT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_PROJECTS_FILTER](-i-n-t-e-n-t_-p-r-o-j-e-c-t-s_-f-i-l-t-e-r.md) | `const val INTENT_PROJECTS_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_PROJECTS_FILTER_NOT](-i-n-t-e-n-t_-p-r-o-j-e-c-t-s_-f-i-l-t-e-r_-n-o-t.md) | `const val INTENT_PROJECTS_FILTER_NOT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_SCRIPT_FILTER](-i-n-t-e-n-t_-s-c-r-i-p-t_-f-i-l-t-e-r.md) | `const val INTENT_SCRIPT_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_SCRIPT_TEST_TASK_FILTER](-i-n-t-e-n-t_-s-c-r-i-p-t_-t-e-s-t_-t-a-s-k_-f-i-l-t-e-r.md) | `const val INTENT_SCRIPT_TEST_TASK_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_SORT_ORDER](-i-n-t-e-n-t_-s-o-r-t_-o-r-d-e-r.md) | `const val INTENT_SORT_ORDER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_TITLE](-i-n-t-e-n-t_-t-i-t-l-e.md) | `const val INTENT_TITLE: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [INTENT_USE_SCRIPT_FILTER](-i-n-t-e-n-t_-u-s-e_-s-c-r-i-p-t_-f-i-l-t-e-r.md) | `const val INTENT_USE_SCRIPT_FILTER: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [NORMAL_SORT](-n-o-r-m-a-l_-s-o-r-t.md) | `const val NORMAL_SORT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [REVERSED_SORT](-r-e-v-e-r-s-e-d_-s-o-r-t.md) | `const val REVERSED_SORT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [SORT_SEPARATOR](-s-o-r-t_-s-e-p-a-r-a-t-o-r.md) | `const val SORT_SEPARATOR: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
