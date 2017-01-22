[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [Preferences](.)

# Preferences

`class Preferences : `[`ThemedPreferenceActivity`](../-themed-preference-activity/index.md)`, OnSharedPreferenceChangeListener` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/Preferences.kt#L45)

### Types

| [AppearancePrefFragment](-appearance-pref-fragment/index.md) | `class AppearancePrefFragment : `[`PrefFragment`](-pref-fragment/index.md)`, OnSharedPreferenceChangeListener` |
| [CalendarPrefFragment](-calendar-pref-fragment/index.md) | `class CalendarPrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [ConfigurationPrefFragment](-configuration-pref-fragment/index.md) | `class ConfigurationPrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [DonatePrefFragment](-donate-pref-fragment/index.md) | `class DonatePrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [InterfacePrefFragment](-interface-pref-fragment/index.md) | `class InterfacePrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [OtherPrefFragment](-other-pref-fragment/index.md) | `class OtherPrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |
| [PrefFragment](-pref-fragment/index.md) | `abstract class PrefFragment : PreferenceFragment` |
| [WidgetPrefFragment](-widget-pref-fragment/index.md) | `class WidgetPrefFragment : `[`PrefFragment`](-pref-fragment/index.md) |

### Constructors

| [&lt;init&gt;](-init-.md) | `Preferences()` |

### Properties

| [app](app.md) | `lateinit var app: `[`TodoApplication`](../-todo-application/index.md) |
| [prefs](prefs.md) | `lateinit var prefs: SharedPreferences` |

### Functions

| [isValidFragment](is-valid-fragment.md) | `fun isValidFragment(fragmentName: String): Boolean` |
| [onBuildHeaders](on-build-headers.md) | `fun onBuildHeaders(target: MutableList<Header>): Unit` |
| [onCreate](on-create.md) | `fun onCreate(savedInstanceState: Bundle?): Unit` |
| [onDestroy](on-destroy.md) | `fun onDestroy(): Unit` |
| [onOptionsItemSelected](on-options-item-selected.md) | `fun onOptionsItemSelected(item: MenuItem): Boolean` |
| [onPause](on-pause.md) | `fun onPause(): Unit` |
| [onResume](on-resume.md) | `fun onResume(): Unit` |
| [onSharedPreferenceChanged](on-shared-preference-changed.md) | `fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String): Unit` |

