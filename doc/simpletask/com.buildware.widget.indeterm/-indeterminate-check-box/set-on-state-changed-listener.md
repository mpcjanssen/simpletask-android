[simpletask](../../index.md) / [com.buildware.widget.indeterm](../index.md) / [IndeterminateCheckBox](index.md) / [setOnStateChangedListener](.)

# setOnStateChangedListener

`open fun setOnStateChangedListener(listener: `[`OnStateChangedListener`](-on-state-changed-listener/index.md)`): Unit` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/buildware/widget/indeterm/IndeterminateCheckBox.java#L152)

Register a callback to be invoked when the indeterminate or checked state changes. The standard `OnCheckedChangedListener` will still be called prior to OnStateChangedListener.

### Parameters

`listener` - the callback to call on indeterminate or checked state change