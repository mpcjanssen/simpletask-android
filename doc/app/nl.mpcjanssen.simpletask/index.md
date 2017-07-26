[app](../index.md) / [nl.mpcjanssen.simpletask](.)

## Package nl.mpcjanssen.simpletask

### Types

| Name | Summary |
|---|---|
| [ActiveFilter](-active-filter/index.md) | `class ActiveFilter`<br>Active filter, has methods for serialization in several formats |
| [AddTask](-add-task/index.md) | `class AddTask : `[`ThemedActionBarActivity`](-themed-action-bar-activity/index.md) |
| [AddTaskBackground](-add-task-background/index.md) | `class AddTaskBackground : Activity` |
| [AddTaskShortcut](-add-task-shortcut/index.md) | `class AddTaskShortcut : `[`ThemedNoActionBarActivity`](-themed-no-action-bar-activity/index.md) |
| [AlarmReceiver](-alarm-receiver/index.md) | `class AlarmReceiver : BroadcastReceiver` |
| [AppCompatPreferenceActivity](-app-compat-preference-activity/index.md) | `abstract class AppCompatPreferenceActivity : PreferenceActivity`<br>`abstract class AppCompatPreferenceActivity : PreferenceActivity`<br>A android.preference.PreferenceActivity which implements and proxies the necessary calls to be used with AppCompat. This technique can be used with an android.app.Activity class, not just android.preference.PreferenceActivity. |
| [AppWidgetRemoteViewsFactory](-app-widget-remote-views-factory/index.md) | `data class AppWidgetRemoteViewsFactory : RemoteViewsFactory` |
| [AppWidgetService](-app-widget-service/index.md) | `class AppWidgetService : RemoteViewsService` |
| [BuildConfig](-build-config/index.md) | `class BuildConfig`<br>`class BuildConfig`<br>`class BuildConfig`<br>`class BuildConfig` |
| [CachedFileProvider](-cached-file-provider/index.md) | `class CachedFileProvider : ContentProvider` |
| [CalendarSync](-calendar-sync/index.md) | `object CalendarSync` |
| [Constants](-constants/index.md) | `object Constants` |
| [DateType](-date-type/index.md) | `enum class DateType` |
| [DebugInfoScreen](-debug-info-screen/index.md) | `class DebugInfoScreen : `[`ThemedActionBarActivity`](-themed-action-bar-activity/index.md) |
| [EvtKey](-evt-key/index.md) | `data class EvtKey` |
| [FilterActivity](-filter-activity/index.md) | `class FilterActivity : `[`ThemedNoActionBarActivity`](-themed-no-action-bar-activity/index.md) |
| [FilterListFragment](-filter-list-fragment/index.md) | `class FilterListFragment : Fragment` |
| [FilterOptions](-filter-options/index.md) | `data class FilterOptions` |
| [FilterOtherFragment](-filter-other-fragment/index.md) | `open class FilterOtherFragment : Fragment`<br>`open class FilterOtherFragment : Fragment` |
| [FilterScriptFragment](-filter-script-fragment/index.md) | `class FilterScriptFragment : Fragment` |
| [FilterSortFragment](-filter-sort-fragment/index.md) | `class FilterSortFragment : Fragment` |
| [FlavourPrefFragment](-flavour-pref-fragment/index.md) | `class FlavourPrefFragment : PreferenceFragment` |
| [HeaderLine](-header-line/index.md) | `data class HeaderLine : `[`VisibleLine`](-visible-line/index.md) |
| [HelpScreen](-help-screen/index.md) | `class HelpScreen : `[`ThemedActionBarActivity`](-themed-action-bar-activity/index.md) |
| [HistoryScreen](-history-screen/index.md) | `class HistoryScreen : `[`ThemedActionBarActivity`](-themed-action-bar-activity/index.md) |
| [Logger](-logger/index.md) | `object Logger : `[`Thread`](http://docs.oracle.com/javase/6/docs/api/java/lang/Thread.html) |
| [LuaConfigScreen](-lua-config-screen/index.md) | `class LuaConfigScreen : `[`ThemedActionBarActivity`](-themed-action-bar-activity/index.md) |
| [LuaInterpreter](-lua-interpreter/index.md) | `object LuaInterpreter` |
| [LuaToastShort](-lua-toast-short/index.md) | `class LuaToastShort : OneArgFunction` |
| [MyAppWidgetProvider](-my-app-widget-provider/index.md) | `open class MyAppWidgetProvider : AppWidgetProvider`<br>`open class MyAppWidgetProvider : AppWidgetProvider` |
| [Preferences](-preferences/index.md) | `class Preferences : `[`ThemedPreferenceActivity`](-themed-preference-activity/index.md)`, OnSharedPreferenceChangeListener` |
| [R](-r/index.md) | `class R`<br>`class R`<br>`class R`<br>`class R` |
| [Simpletask](-simpletask/index.md) | `class Simpletask : `[`ThemedNoActionBarActivity`](-themed-no-action-bar-activity/index.md) |
| [TaskLine](-task-line/index.md) | `data class TaskLine : `[`VisibleLine`](-visible-line/index.md) |
| [ThemedActionBarActivity](-themed-action-bar-activity/index.md) | `abstract class ThemedActionBarActivity : AppCompatActivity` |
| [ThemedNoActionBarActivity](-themed-no-action-bar-activity/index.md) | `abstract class ThemedNoActionBarActivity : AppCompatActivity` |
| [ThemedPreferenceActivity](-themed-preference-activity/index.md) | `abstract class ThemedPreferenceActivity : `[`AppCompatPreferenceActivity`](-app-compat-preference-activity/index.md) |
| [TimePreference](-time-preference/index.md) | `class TimePreference : DialogPreference` |
| [TodoApplication](-todo-application/index.md) | `class TodoApplication : Application, `[`FileChangeListener`](../nl.mpcjanssen.simpletask.remote/-file-store-interface/-file-change-listener/index.md)`, `[`BackupInterface`](../nl.mpcjanssen.simpletask.remote/-backup-interface/index.md) |
| [VisibleLine](-visible-line/index.md) | `interface VisibleLine` |

### Exceptions

| Name | Summary |
|---|---|
| [TodoException](-todo-exception/index.md) | `class TodoException : `[`RuntimeException`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-runtime-exception/index.html) |

### Extensions for External Classes

| Name | Summary |
|---|---|
| [android.preference.Preference](android.preference.-preference/index.md) |  |

### Properties

| Name | Summary |
|---|---|
| [MainFilter](-main-filter.md) | `var MainFilter: `[`ActiveFilter`](-active-filter/index.md) |
