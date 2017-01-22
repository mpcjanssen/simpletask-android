[simpletask](../../index.md) / [com.buildware.widget.indeterm](../index.md) / [IndeterminateCheckBox](.)

# IndeterminateCheckBox

`open class IndeterminateCheckBox : AppCompatCheckBox, `[`IndeterminateCheckable`](../-indeterminate-checkable/index.md) [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/com/buildware/widget/indeterm/IndeterminateCheckBox.java#L19)

A CheckBox with additional 3rd "indeterminate" state. By default it is in "determinate" (checked or unchecked) state.

**Author**
Svetlozar Kostadinov (sevarbg@gmail.com)

### Types

| [OnStateChangedListener](-on-state-changed-listener/index.md) | `interface OnStateChangedListener`<br>Interface definition for a callback to be invoked when the checked state changed. |

### Constructors

| [&lt;init&gt;](-init-.md) | `IndeterminateCheckBox(context: Context)`<br>`IndeterminateCheckBox(context: Context, attrs: AttributeSet)`<br>`IndeterminateCheckBox(context: Context, attrs: AttributeSet, defStyleAttr: Int)` |

### Functions

| [getAccessibilityClassName](get-accessibility-class-name.md) | `open fun getAccessibilityClassName(): CharSequence` |
| [getState](get-state.md) | `open fun getState(): Boolean` |
| [isIndeterminate](is-indeterminate.md) | `open fun isIndeterminate(): Boolean` |
| [setChecked](set-checked.md) | `open fun setChecked(checked: Boolean): Unit` |
| [setIndeterminate](set-indeterminate.md) | `open fun setIndeterminate(indeterminate: Boolean): Unit` |
| [setIndeterminateUsed](set-indeterminate-used.md) | `open fun setIndeterminateUsed(bool: Boolean): Unit` |
| [setOnStateChangedListener](set-on-state-changed-listener.md) | `open fun setOnStateChangedListener(listener: `[`OnStateChangedListener`](-on-state-changed-listener/index.md)`): Unit`<br>Register a callback to be invoked when the indeterminate or checked state changes. The standard `OnCheckedChangedListener` will still be called prior to OnStateChangedListener. |
| [setState](set-state.md) | `open fun setState(state: Boolean): Unit` |
| [toggle](toggle.md) | `open fun toggle(): Unit` |

