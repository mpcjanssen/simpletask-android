[simpletask](../../index.md) / [nl.mpcjanssen.simpletask.sort](../index.md) / [MultiComparator](.)

# MultiComparator

`class MultiComparator : `[`Comparator`](http://docs.oracle.com/javase/6/docs/api/java/util/Comparator.html)`<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>` [(source)](https://github.com/mpcjanssen/simpletask-android/blob/master/src/main/java/nl/mpcjanssen/simpletask/sort/MultiComparator.kt#L10)

### Constructors

| [&lt;init&gt;](-init-.md) | `MultiComparator(sorts: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>, today: String, caseSensitve: Boolean, createAsBackup: Boolean)` |

### Properties

| [comparators](comparators.md) | `var comparators: `[`Comparator`](http://docs.oracle.com/javase/6/docs/api/java/util/Comparator.html)`<`[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>?` |
| [defaultComparator](default-comparator.md) | `val defaultComparator: `[`FileOrderComparator`](../-file-order-comparator/index.md) |

### Functions

| [compare](compare.md) | `fun compare(o1: `[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`?, o2: `[`TodoItem`](../../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`?): Int` |

