[app](../../index.md) / [nl.mpcjanssen.simpletask.util](../index.md) / [ListenerList](.)

# ListenerList

`class ListenerList<L>`

### Types

| Name | Summary |
|---|---|
| [FireHandler](-fire-handler/index.md) | `interface FireHandler<in L>` |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ListenerList()` |

### Functions

| Name | Summary |
|---|---|
| [add](add.md) | `fun add(listener: L): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [fireEvent](fire-event.md) | `fun fireEvent(fireHandler: `[`FireHandler`](-fire-handler/index.md)`<L>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [getListenerList](get-listener-list.md) | `fun getListenerList(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<L>` |
| [remove](remove.md) | `fun remove(listener: L): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
