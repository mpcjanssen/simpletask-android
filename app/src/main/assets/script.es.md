Scripting with Lua
==================

Simpletask incorpora un motor de secuencias de comandos Lua que se puede usar para configuración y devoluciones de llamadas (callbacks).
La configuración se lee cada vez que se reinicia la aplicación o cuando se edita en la pantalla Lua Config.
Las devoluciones de llamada (callbacks) se ejecutan cuando tienen lugar ciertos eventos (como el filtrado de tareas).

Tanto la configuración como las devoluciones de llamada llamarán a funciones específicas de Lua. Consulte a continuación los detalles de las devoluciones de llamada admitidas.

Todo el código (configuración y devoluciones de llamada) serán ejecutadas en el mismo intérprete Lua.

De esta manera se pueden definir funciones auxiliares en la configuración y utilizarlas en las devoluciones de llamada.

'*La devolución de llamada de filtrado se ha cambiado desde las versiones anteriores a la versión 8.0 (ver más abajo).*'

Para cambiar un filtro existente (aplicación y widget) al formato 8.0 se debe hacer lo siguiente:

Agregar `function onFilter(t,f,e)` antes y `end` luego del guión (scritp) existente. Prefijar todos los campos con `f.`, es decir `due` será `f.due`. Ejemplo:

    if due~=nil then
        return os.time() >= due;
    end
    --- las tareas sin fecha de vencimiento no están vencidas.
    return false;

se convierte en

    function onFilter(t,f,e)
        if f.due~=nil then
            return os.time() >= f.due;
        end
        --- las tareas sin fecha de vencimiento no están vencidas.
        return false;
    end


Funciones auxiliares
====================

### `toast (string) -> nil`

Muestra `string` como una notificación toast. Es útil para la depuración de scripts.

### `log (string) -> nil`

Registra `string` en el registro de logcat de Android con nivel de información y etiqueta` LuaInterp`.

### Nota
No use notificaciones toasts dentro de las funciones. Es es una buena manera de hacer que Simpletask se cuelgue.

Callbacks (devoluciones de llamada)
===================================

La mayoría de las funciones utilizan la misma colección de parámetros:

### Parámetros generales ###

* `task`: La tarea como una cadena.
* `fields`: Partes de la tarea convertidas a diferentes tipos (por ejemplo, una marca de tiempo para `createdate`)
    * `completed`: Valor booleano que indica si la tarea se ha completado.
    * `completiondate`: La fecha de finalización en segundos de la tarea o `nil` si no está establecida.
    * `createdate`: La fecha de creación en segundos de la tarea o `nil` si no está establecida.
    * `due`: La fecha de vencimiento en segundos o `nil` si no está establecida.
    * `lists`: Una tabla con las listas de la tarea como claves. `fields.lists` en sí mismo nunca será` nil`
    * `priority`: La prioridad de la tarea como una cadena.
    * `recurrence`: El patrón de recurrencia o repetición de la tarea como cadena o `nil` si no está establecida.
    * `tags`: Una tabla con las etiquetas de la tarea como claves. `fields.tags` en sí mismo nunca será `nil`.
    * `task`: La tarea completa como una cadena.
    * `threshold`: La fecha límite en segundos o `nil` si no está establecida.
    * `text`: El texto ingresado cuando se creó la tarea.
* `extensions`: Una tabla con las extensiones de todo.txt (`key: val`) de la tarea como pares de valores clave. Solo hay una entrada para cada clave, esto es para facilitar su uso. Si necesita varios pares `key: val` con la misma clave, puede analizar la tarea en Lua.

### `onFilter (task, fields, extensions) -> boolean`

Llamada para cada tarea como parte del filtrado de la lista de tareas pendientes.
#### Devuelve

* `verdadero` si la tarea debe mostrarse
* `falso` si la tarea no debe mostrarse

#### Notas

* Si hay un error de Lua en la devolución de llamada, se comporta como si hubiera devuelto `true`
* Teniendo en cuenta que esta función es llamada muchas veces (para cada tarea en la lista) debería ser rápida. Si es demasiada lenta, Simpletask podría terminar en ANR (Android Not Responding).
* Debe definir la función `onFilter` en el filtro (no en la configuración Lua). Definirlo en la configuración principal no funcionará. Si el script de filtrado está vacío, la función `onFilter`  será indefinida.

### `onGroup (task, fields, extensions) -> string`

Called for every task as part of filtering the todo list.


### Returns

* The group this task belongs to.

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `""`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the `onGroup` function in the filter (not in the configuration). Defining it in the main configuration will not work, if the Filter script is empty, the `onGroup` function will be undefined.


### `onSort (task, fields, extensions) -> string`

Called for every task as part of sorting the todo list. This function should return a string for every task. This string
is then used to sort the tasks.


### Returns

* The string to use for task sorting.

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `""`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the `onSort` function in the filter (not in the configuration). Defining it in the main configuration will not work, if the Filter script is empty, the `onGroup` function will be undefined.



### `onDisplay (task, fields, extensions) -> string`

Called for every task before it is displayed.

### Returns

* A string which is displayed.

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `""`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the `onDisplay` function in the filter (not in the configuration). Defining it in the main configuration will not work, if the Filter script is empty, the `onDisplay` function will be undefined.

### `onAdd (task, fields, extensions) -> string`

Called for every task before it is added.

### Returns

* A string which will be the actual task that is added.

### Notes

* If there is a Lua error in the callback, the original task text is saved.
* You should define the `onAdd` callback in the main Lua configuration as it is not filter specific. Defining it in a filter will not work.

### `onTextSearch (taskText, searchText, caseSensitive) -> boolean`

Called for every task as when searching for text.

### Parameters

* `taskText`: The task text as it appears in the `todo.txt` file
* `searchText`: Text being searched for
* `caseSensitive`: `true` if case sensitive searching is configured in the settings.

### Returns

* `true` if the task should be shown
* `false` if the task should not be shown

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `true`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.

Configuration
=============

Configuration is read on app start and whenever it is changed or ran from the Lua Config screen.
Configuration from Lua will always override the value from the settings (Lua wins).

### `theme () -> string`

### Parameters

None

### Returns

* `"dark"` for the Material dark theme
* `"black"` for the black theme (works well on Amoled devices).
* `"light_darkactionbar"` for the Material light theme

### Notes

* Requires an application restart to take effect (more accurately it needs to recreate the activity)

### `tasklistTextSize () -> float`

### Parameters

None

### Returns

The font size of the main task list in SP as a float.

### Notes

* Requires an application restart to take effect (more accurately it needs to recreate the activity)
* The default size in Android at the moment is `14sp`

Examples
========

The following code will show only overdue tasks where tasks without a due date, will never be overdue.

    function onFilter(t,f,e)
       if f.due~=nil then
           return os.time() > f.due;
       end
       --- tasks with no due date are not overdue.
       return false;
    end

Show tasks without tags or lists (the GTD Inbox):

    function onFilter(t,f,e)
       return next(f.tags)==nil and next(f.lists)==nil
    end

Show all tasks on the `@errands` list:

    function onFilter(t,f,e)
       return f.lists["errands"]
    end

Change the font size of the main task list to `16sp`:

    function tasklistTextSize()
       return 16.0
    end

The 8.0.0 fuzzy search in Lua:

    function onTextSearch(text, search, case)
        pat = string.gsub(search, ".", "%0.*")
        res = string.match(text, pat)
        return res~=nil
    end


A group callback to group by list with custom empty header:

    function onGroup(t,f,e)
        if not next(f.lists) then
            return "Inbox"
        else
            return next(f.lists)
        end
    end
    
Don't group at all and don't show any headers (regardless of sort order)

    function onGroup()
        return ""
    end

A callback to modify the display of a task:

    function onDisplay(t,f,e)
       if f.due~=nil and os.time() > f.due then
         --- Display overdue tasks in uppercase. (Prefixing with '=' replaces entire task.)
         return "="..string.upper(f.tasktext)
       end
       return f.tasktext
    end

Learning Lua
============

Googling should turn up plenty of good resources. [*Programming in Lua*](https://www.lua.org/pil/contents.html) should cover almost everything.





resources. [*Programming in Lua*](https://www.lua.org/pil/contents.html) should cover almost everything.


