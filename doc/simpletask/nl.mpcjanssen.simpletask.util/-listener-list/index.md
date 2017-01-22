[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.util](../index.md) / [ListenerList](.)

# ListenerList

`class ListenerList<L>` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/util/ListenerList.kt#L5)

### Types

| [FireHandler](-fire-handler/index.md) | `interface FireHandler<in L>` |

### Constructors

| [&lt;init&gt;](-init-.md) | `ListenerList()` |

### Functions

| [add](add.md) | `fun add(listener: L): Unit` |
| [fireEvent](fire-event.md) | `fun fireEvent(fireHandler: `[`FireHandler`](-fire-handler/index.md)`<L>): Unit` |
| [getListenerList](get-listener-list.md) | `fun getListenerList(): List<L>` |
| [remove](remove.md) | `fun remove(listener: L): Unit` |

