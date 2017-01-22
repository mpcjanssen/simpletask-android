[simpletask](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [AppCompatPreferenceActivity](.)

# AppCompatPreferenceActivity

`abstract class AppCompatPreferenceActivity : PreferenceActivity` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/AppCompatPreferenceActivity.java#L39)

A android.preference.PreferenceActivity which implements and proxies the necessary calls to be used with AppCompat. This technique can be used with an android.app.Activity class, not just android.preference.PreferenceActivity.

### Constructors

| [&lt;init&gt;](-init-.md) | `AppCompatPreferenceActivity()`<br>A android.preference.PreferenceActivity which implements and proxies the necessary calls to be used with AppCompat. This technique can be used with an android.app.Activity class, not just android.preference.PreferenceActivity. |

### Functions

| [addContentView](add-content-view.md) | `open fun addContentView(view: View, params: LayoutParams): Unit` |
| [getMenuInflater](get-menu-inflater.md) | `open fun getMenuInflater(): MenuInflater` |
| [getSupportActionBar](get-support-action-bar.md) | `open fun getSupportActionBar(): ActionBar` |
| [invalidateOptionsMenu](invalidate-options-menu.md) | `open fun invalidateOptionsMenu(): Unit` |
| [onConfigurationChanged](on-configuration-changed.md) | `open fun onConfigurationChanged(newConfig: Configuration): Unit` |
| [setContentView](set-content-view.md) | `open fun setContentView(layoutResID: Int): Unit`<br>`open fun setContentView(view: View): Unit`<br>`open fun setContentView(view: View, params: LayoutParams): Unit` |
| [setSupportActionBar](set-support-action-bar.md) | `open fun setSupportActionBar(toolbar: Toolbar?): Unit` |

### Inheritors

| [ThemedPreferenceActivity](../-themed-preference-activity/index.md) | `abstract class ThemedPreferenceActivity : AppCompatPreferenceActivity` |

