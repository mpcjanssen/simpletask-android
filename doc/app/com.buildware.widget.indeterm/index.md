[app](../index.md) / [com.buildware.widget.indeterm](.)

## Package com.buildware.widget.indeterm

### Types

| Name | Summary |
|---|---|
| [IndeterminateCheckBox](-indeterminate-check-box/index.md) | `open class IndeterminateCheckBox : AppCompatCheckBox, `[`IndeterminateCheckable`](-indeterminate-checkable/index.md)<br>A CheckBox with additional 3rd "indeterminate" state. By default it is in "determinate" (checked or unchecked) state. |
| [IndeterminateCheckable](-indeterminate-checkable/index.md) | `interface IndeterminateCheckable : Checkable`<br>Extension to Checkable interface with addition "indeterminate" state represented by `getState()`. Value meanings: null = indeterminate state true = checked state false = unchecked state |
| [IndeterminateRadioButton](-indeterminate-radio-button/index.md) | `open class IndeterminateRadioButton : AppCompatRadioButton, `[`IndeterminateCheckable`](-indeterminate-checkable/index.md)<br>A RadioButton with additional 3rd "indeterminate" state. By default it is in "determinate" (checked or unchecked) state. |
