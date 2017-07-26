[app](../../index.md) / [com.robobunny](../index.md) / [SeekBarPreference](.)

# SeekBarPreference

`class SeekBarPreference : Preference, OnSeekBarChangeListener`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SeekBarPreference(context: Context, attrs: AttributeSet)`<br>`SeekBarPreference(context: Context, attrs: AttributeSet, defStyle: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)` |

### Functions

| Name | Summary |
|---|---|
| [onBindView](on-bind-view.md) | `fun onBindView(view: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onDependencyChanged](on-dependency-changed.md) | `fun onDependencyChanged(dependency: Preference, disableDependent: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onGetDefaultValue](on-get-default-value.md) | `fun onGetDefaultValue(ta: TypedArray, index: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [onProgressChanged](on-progress-changed.md) | `fun onProgressChanged(seekBar: SeekBar, progress: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, fromUser: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onSetInitialValue](on-set-initial-value.md) | `fun onSetInitialValue(restoreValue: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`, defaultValue: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onStartTrackingTouch](on-start-tracking-touch.md) | `fun onStartTrackingTouch(seekBar: SeekBar): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onStopTrackingTouch](on-stop-tracking-touch.md) | `fun onStopTrackingTouch(seekBar: SeekBar): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setEnabled](set-enabled.md) | `fun setEnabled(enabled: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>make sure that the seekbar is disabled if the preference is disabled |
| [updateView](update-view.md) | `fun updateView(view: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Update a SeekBarPreference view with our current state |

### Extension Functions

| Name | Summary |
|---|---|
| [valueInSummary](../../nl.mpcjanssen.simpletask/android.preference.-preference/value-in-summary.md) | `fun Preference.valueInSummary(any: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`? = null): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
