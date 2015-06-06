Scripting
=========

Explicación
===========

Simpletask proporciona (experimentalmente) filtrado avanzado de tareas utilizando Lua. Para utilizar Lua  necesitará habilitarlo en los ajustes.

Después de habilitar Lua, la actividad de filtrado mostrará una solapa adicional SCRIPT. Para cada tarea se ejecutará una pieza de Lua.  Dependiendo del valor booleano de la orden return, la tarea es visible (en caso de `true`) o filtrada y excluida (en caso de `false`). Para ayudar con escribir el script puede probar el script en una tarea de ejemplo y el resultado crudo y el resultado interpretado en forma de booleano se mostrará.

Variables definidas
-------------------
 
Para hacer el filtrado más fácil, para cada tarea un par de variables globales están definidas. Puede utilizarlas en su código.

* `completed`: Booleano indicando si la tarea está completada.
* `completiondate`: La fecha de conclusión de la tarea en segundos o `nil` si no está establecida.
* `createdate`: La fecha de creación de la tarea en segundos o `nil` si no está establecida.
* `due`: La fecha de vencimiento en segundos o `nil` si no está establecida.
* `lists`: Una matriz de cadenas alfanuméricas con las listas de la tarea.
* `priority`: La prioridad de la tarea cuando cuerda.
* `recurrence`: El patrón de recurrencia de la tarea como cadena alfanumérica o `nil` si no está establecida.
* `tags`: Una matriz de cadenas alfanuméricas con las etiquetas de la tarea.
* `task`: la tarea completa como cadena alfanumérica.
* `threshold`: La fecha umbral en segundos o `nil` si no está establecida.

Ejemplo
-------

El código siguiente mostrará solo las tareas pasadas de fecha considerando que las tareas sin una fecha prevista, nunca estarán pasadas de fecha.

    if (due~=nil) {
         return os.time() > due;
    }
    --- tareas sin la fecha prevista no es estaran pasadas de fecha.
    return false;

Una ejecución del filtro sobre todas las  tareas utiliza un contexto de evaluación único, de modo que cualquier otro estado global se retiene de tarea a tarea. Esto permite hacer algunas cosas "interesantes" como mostrar las cien primeras tareas:

    if c == nil then
        c = 0
    end
    c = c + 1; 
    return c <= 100;

Notas
-----

* El script se ejecuta para cada tarea al mostrarla, así que asegúrese de que sea rápido. Hacer demasiado trabajo en el script lentificará Simpletask.
* Cualquier informe ANR para el cual el script este puesto en el filtro tendrá altas posibilidad de que se ignore.  _A un gran poder la acompaña una gran responsabilidad_.


Qué sigue?
----------

Esta explicación tendría que ser suficiente para empezar a funcionar.

