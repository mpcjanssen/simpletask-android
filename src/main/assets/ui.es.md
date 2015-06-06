Interfaz de usuario
===================

Esta página explica la interfaz de usuario. De momento esto es un trabajo en marcha por lo que no cubre todos los elementos todavía.


## Actividad de Filtrado

### Invertir filtro

La opción invertir es para utilizar el negativo de los elementos seleccionados. Para dar un ejemplo de por qué esto es útil.

Imagine que tiene las siguientes listas definidas:

- `@trabajo`
- `@casa`
- `Proyecto`
- `AlgúnDía`

Dos de estas listas contienen acciones próximas (`@trabajo` y `@casa`) las otras dos no. Ahora si quiere crear un filtro para mostrar únicamente las acciones próximas hay dos maneras para hacerlo. Una manera incorrecta y una manera correcta.

### La manera 'incorrecta'

Crear un filtro con las opciones `@trabajo` y `@casa` marcadas.  Inicialmente esto funcionará. Sin embargo, cuándo cree un elemento con la lista `@compras` qué también sea una acción próxima, tendrá que actualizar el filtro para acciones próximas. Así que es fácil que acciones próximas pasen inadvertidas.

### La manera 'correcta'

Crear un filtro con las opciones `Proyecto` y `AlgúnDía` marcadas y también marcar `Invertir filtro`.  Ahora todos los  elementos qué no estén en las listas `Proyecto` y `AlgúnDía` se mostrarán después de filtrar. Esto significa que si añade una tarea `@compras` ésta también será incluida en los resultados.

Naturalmente, si añade un nuevo listado que no contiene acciones próximas (p. ej. `@ProyectoPrivado`) estas también se mostrarán y el filtro requerirá ser cambiado. La gran diferencia con la manera 'incorrecta' es que este camino podría mostrar demasiada información en lugar de demasiado poca, lo cual es preferible.
