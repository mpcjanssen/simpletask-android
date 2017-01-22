[simpletask](../../../index.md) / [com.buildware.widget.indeterm](../../index.md) / [IndeterminateCheckBox](../index.md) / [OnStateChangedListener](index.md) / [onStateChanged](.)

# onStateChanged

`abstract fun onStateChanged(checkBox: `[`IndeterminateCheckBox`](../index.md)`, @Nullable newState: @Nullable Boolean?): Unit` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/buildware/widget/indeterm/IndeterminateCheckBox.java#L44)

Called when the indeterminate state has changed.

### Parameters

`checkBox` - The checkbox view whose state has changed.

`newState` - The new state of checkBox. Value meanings: null = indeterminate state true = checked state false = unchecked state