[app](../../index.md) / [com.buildware.widget.indeterm](../index.md) / [IndeterminateRadioButton](.)

# IndeterminateRadioButton

`open class IndeterminateRadioButton : AppCompatRadioButton, `[`IndeterminateCheckable`](../-indeterminate-checkable/index.md)

A RadioButton with additional 3rd "indeterminate" state. By default it is in "determinate" (checked or unchecked) state.

**Author**
Svetlozar Kostadinov (sevarbg@gmail.com)

### Types

| Name | Summary |
|---|---|
| [OnStateChangedListener](-on-state-changed-listener/index.md) | `interface OnStateChangedListener`<br>Interface definition for a callback to be invoked when the checked state changed. |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `IndeterminateRadioButton(context: Context)`<br>`IndeterminateRadioButton(context: Context, attrs: AttributeSet)`<br>`IndeterminateRadioButton(context: Context, attrs: AttributeSet, defStyleAttr: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)` |

### Functions

| Name | Summary |
|---|---|
| [getAccessibilityClassName](get-accessibility-class-name.md) | `open fun getAccessibilityClassName(): `[`CharSequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/index.html) |
| [getState](get-state.md) | `open fun getState(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isIndeterminate](is-indeterminate.md) | `open fun isIndeterminate(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onRestoreInstanceState](on-restore-instance-state.md) | `open fun onRestoreInstanceState(state: Parcelable): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onSaveInstanceState](on-save-instance-state.md) | `open fun onSaveInstanceState(): Parcelable` |
| [setChecked](set-checked.md) | `open fun setChecked(checked: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setIndeterminate](set-indeterminate.md) | `open fun setIndeterminate(indeterminate: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setIndeterminateUsed](set-indeterminate-used.md) | `open fun setIndeterminateUsed(bool: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setOnStateChangedListener](set-on-state-changed-listener.md) | `open fun setOnStateChangedListener(listener: `[`OnStateChangedListener`](-on-state-changed-listener/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Register a callback to be invoked when the indeterminate or checked state changes. The standard `OnCheckedChangedListener` will still be called prior to OnStateChangedListener. |
| [setState](set-state.md) | `open fun setState(state: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [toggle](toggle.md) | `open fun toggle(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
