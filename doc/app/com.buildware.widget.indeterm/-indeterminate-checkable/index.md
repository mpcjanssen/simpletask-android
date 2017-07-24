[app](../../index.md) / [com.buildware.widget.indeterm](../index.md) / [IndeterminateCheckable](.)

# IndeterminateCheckable

`interface IndeterminateCheckable : Checkable`

Extension to Checkable interface with addition "indeterminate" state represented by `getState()`. Value meanings: null = indeterminate state true = checked state false = unchecked state

### Functions

| Name | Summary |
|---|---|
| [getState](get-state.md) | `abstract fun getState(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [setIndeterminateUsed](set-indeterminate-used.md) | `abstract fun setIndeterminateUsed(bool: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [setState](set-state.md) | `abstract fun setState(state: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Inheritors

| Name | Summary |
|---|---|
| [IndeterminateCheckBox](../-indeterminate-check-box/index.md) | `open class IndeterminateCheckBox : AppCompatCheckBox, IndeterminateCheckable`<br>A CheckBox with additional 3rd "indeterminate" state. By default it is in "determinate" (checked or unchecked) state. |
| [IndeterminateRadioButton](../-indeterminate-radio-button/index.md) | `open class IndeterminateRadioButton : AppCompatRadioButton, IndeterminateCheckable`<br>A RadioButton with additional 3rd "indeterminate" state. By default it is in "determinate" (checked or unchecked) state. |
