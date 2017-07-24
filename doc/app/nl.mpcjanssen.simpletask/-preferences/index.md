[app](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [Preferences](.)

# Preferences

`class Preferences : `[`ThemedPreferenceActivity`](../-themed-preference-activity/index.md)`, OnSharedPreferenceChangeListener`

### Types

| Name | Summary |
|---|---|
| [AppearancePrefFragment](-appearance-pref-fragment/index.md) | `class AppearancePrefFragment : `[`PrefFragment`](-pref-fragment/index.md)`, OnSharedPreferenceChangeListener` |
| [CalendarPrefFragment](-calendar-pref-fragment/index.md) | `class CalendarPrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [ConfigurationPrefFragment](-configuration-pref-fragment/index.md) | `class ConfigurationPrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [DonatePrefFragment](-donate-pref-fragment/index.md) | `class DonatePrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [InterfacePrefFragment](-interface-pref-fragment/index.md) | `class InterfacePrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [OtherPrefFragment](-other-pref-fragment/index.md) | `class OtherPrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [PrefFragment](-pref-fragment/index.md) | `abstract class PrefFragment : PreferenceFragment` |
| [WidgetPrefFragment](-widget-pref-fragment/index.md) | `class WidgetPrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `Preferences()` |

### Properties

| Name | Summary |
|---|---|
| [app](app.md) | `lateinit var app: `[`TodoApplication`](../-todo-application/index.md) |
| [prefs](prefs.md) | `lateinit var prefs: SharedPreferences` |

### Functions

| Name | Summary |
|---|---|
| [isValidFragment](is-valid-fragment.md) | `fun isValidFragment(fragmentName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onBuildHeaders](on-build-headers.md) | `fun onBuildHeaders(target: `[`MutableList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)`<Header>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onCreate](on-create.md) | `fun onCreate(savedInstanceState: Bundle?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onDestroy](on-destroy.md) | `fun onDestroy(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onOptionsItemSelected](on-options-item-selected.md) | `fun onOptionsItemSelected(item: MenuItem): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onPause](on-pause.md) | `fun onPause(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onResume](on-resume.md) | `fun onResume(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onSharedPreferenceChanged](on-shared-preference-changed.md) | `fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
