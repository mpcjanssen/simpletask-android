[simpletask](../../index.md) / [com.robobunny](../index.md) / [SeekBarPreference](.)

# SeekBarPreference

`class SeekBarPreference : Preference, OnSeekBarChangeListener` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/robobunny/SeekBarPreference.kt#L18)

### Constructors

| [&lt;init&gt;](-init-.md) | `SeekBarPreference(context: Context, attrs: AttributeSet)`<br>`SeekBarPreference(context: Context, attrs: AttributeSet, defStyle: Int)` |

### Functions

| [onBindView](on-bind-view.md) | `fun onBindView(view: View): Unit` |
| [onDependencyChanged](on-dependency-changed.md) | `fun onDependencyChanged(dependency: Preference, disableDependent: Boolean): Unit` |
| [onGetDefaultValue](on-get-default-value.md) | `fun onGetDefaultValue(ta: TypedArray, index: Int): Any` |
| [onProgressChanged](on-progress-changed.md) | `fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean): Unit` |
| [onSetInitialValue](on-set-initial-value.md) | `fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?): Unit` |
| [onStartTrackingTouch](on-start-tracking-touch.md) | `fun onStartTrackingTouch(seekBar: SeekBar): Unit` |
| [onStopTrackingTouch](on-stop-tracking-touch.md) | `fun onStopTrackingTouch(seekBar: SeekBar): Unit` |
| [setEnabled](set-enabled.md) | `fun setEnabled(enabled: Boolean): Unit`<br>make sure that the seekbar is disabled if the preference is disabled |
| [updateView](update-view.md) | `fun updateView(view: View): Unit`<br>Update a SeekBarPreference view with our current state |

### Extension Functions

| [valueInSummary](../../nl.mpcjanssen.simpletask/android.preference.-preference/value-in-summary.md) | `fun Preference.valueInSummary(any: Any? = null): Unit` |

