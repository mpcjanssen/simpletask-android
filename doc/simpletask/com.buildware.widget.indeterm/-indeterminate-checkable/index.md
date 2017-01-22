[simpletask](../../index.md) / [com.buildware.widget.indeterm](../index.md) / [IndeterminateCheckable](.)

# IndeterminateCheckable

`interface IndeterminateCheckable : Checkable` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/buildware/widget/indeterm/IndeterminateCheckable.java#L12)

Extension to Checkable interface with addition "indeterminate" state represented by `getState()`. Value meanings: null = indeterminate state true = checked state false = unchecked state

### Functions

| [getState](get-state.md) | `abstract fun getState(): Boolean` |
| [setIndeterminateUsed](set-indeterminate-used.md) | `abstract fun setIndeterminateUsed(bool: Boolean): Unit` |
| [setState](set-state.md) | `abstract fun setState(state: Boolean): Unit` |

### Inheritors

| [IndeterminateCheckBox](../-indeterminate-check-box/index.md) | `open class IndeterminateCheckBox : AppCompatCheckBox, IndeterminateCheckable`<br>A CheckBox with additional 3rd "indeterminate" state. By default it is in "determinate" (checked or unchecked) state. |
| [IndeterminateRadioButton](../-indeterminate-radio-button/index.md) | `open class IndeterminateRadioButton : AppCompatRadioButton, IndeterminateCheckable`<br>A RadioButton with additional 3rd "indeterminate" state. By default it is in "determinate" (checked or unchecked) state. |

