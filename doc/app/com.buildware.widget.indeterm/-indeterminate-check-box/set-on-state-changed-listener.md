[app](../../index.md) / [com.buildware.widget.indeterm](../index.md) / [IndeterminateCheckBox](index.md) / [setOnStateChangedListener](.)

# setOnStateChangedListener

`open fun setOnStateChangedListener(listener: `[`OnStateChangedListener`](-on-state-changed-listener/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Register a callback to be invoked when the indeterminate or checked state changes. The standard `OnCheckedChangedListener` will still be called prior to OnStateChangedListener.

### Parameters

`listener` - the callback to call on indeterminate or checked state change