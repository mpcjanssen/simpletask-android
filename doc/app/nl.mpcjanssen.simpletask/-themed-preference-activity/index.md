[app](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [ThemedPreferenceActivity](.)

# ThemedPreferenceActivity

`abstract class ThemedPreferenceActivity : `[`AppCompatPreferenceActivity`](../-app-compat-preference-activity/index.md)

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ThemedPreferenceActivity()` |

### Functions

| Name | Summary |
|---|---|
| [onCreate](on-create.md) | `open fun onCreate(savedInstanceState: Bundle?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inherited Functions

| Name | Summary |
|---|---|
| [addContentView](../-app-compat-preference-activity/add-content-view.md) | `open fun addContentView(view: View, params: LayoutParams): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [getMenuInflater](../-app-compat-preference-activity/get-menu-inflater.md) | `open fun getMenuInflater(): MenuInflater` |
| [getSupportActionBar](../-app-compat-preference-activity/get-support-action-bar.md) | `open fun getSupportActionBar(): ActionBar` |
| [invalidateOptionsMenu](../-app-compat-preference-activity/invalidate-options-menu.md) | `open fun invalidateOptionsMenu(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onConfigurationChanged](../-app-compat-preference-activity/on-configuration-changed.md) | `open fun onConfigurationChanged(newConfig: Configuration): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setContentView](../-app-compat-preference-activity/set-content-view.md) | `open fun setContentView(layoutResID: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>`open fun setContentView(view: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>`open fun setContentView(view: View, params: LayoutParams): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setSupportActionBar](../-app-compat-preference-activity/set-support-action-bar.md) | `open fun setSupportActionBar(toolbar: Toolbar?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [Preferences](../-preferences/index.md) | `class Preferences : ThemedPreferenceActivity, OnSharedPreferenceChangeListener` |
