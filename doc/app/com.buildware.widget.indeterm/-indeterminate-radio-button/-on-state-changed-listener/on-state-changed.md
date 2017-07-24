[app](../../../index.md) / [com.buildware.widget.indeterm](../../index.md) / [IndeterminateRadioButton](../index.md) / [OnStateChangedListener](index.md) / [onStateChanged](.)

# onStateChanged

`abstract fun onStateChanged(radioButton: `[`IndeterminateRadioButton`](../index.md)`, @Nullable state: @Nullable `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Called when the indeterminate state has changed.

### Parameters

`radioButton` - The RadioButton whose state has changed.

`state` - The state of buttonView. Value meanings: null = indeterminate state true = checked state false = unchecked state