[app](../../index.md) / [nl.mpcjanssen.simpletask](../index.md) / [TimePreference](.)

# TimePreference

`class TimePreference : DialogPreference`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `TimePreference(ctx: Context)`<br>`TimePreference(ctx: Context, attrs: AttributeSet?, defStyle: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)` = android.R.attr.dialogPreferenceStyle)` |

### Functions

| Name | Summary |
|---|---|
| [onBindDialogView](on-bind-dialog-view.md) | `fun onBindDialogView(v: View): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onCreateDialogView](on-create-dialog-view.md) | `fun onCreateDialogView(): View` |
| [onDialogClosed](on-dialog-closed.md) | `fun onDialogClosed(positiveResult: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onGetDefaultValue](on-get-default-value.md) | `fun onGetDefaultValue(a: TypedArray, index: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [onSetInitialValue](on-set-initial-value.md) | `fun onSetInitialValue(restoreValue: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`, defaultValue: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [valueInSummary](../android.preference.-preference/value-in-summary.md) | `fun Preference.valueInSummary(any: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`? = null): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
