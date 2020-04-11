Usando scripts de Lua
=====================

Simpletask incorpora un motor de scripting de Lua que se puede usar para configuración y devoluciones de llamadas (callbacks).
La configuración se lee cada vez que se reinicia la aplicación o cuando se edita en la pantalla Configuración Lua.
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
    --- las tareas sin fecha de vencimiento no se consideran vencidas.
    return false;

se convierte en

    function onFilter(t,f,e)
        if f.due~=nil then
            return os.time() >= f.due;
        end
        --- las tareas sin fecha de vencimiento no se consideran vencidas.
        return false;
    end

Funciones auxiliares
====================

### `toast (string) -> nil`

Muestra `string` como una notificación toast. Es útil para la depuración de scripts.

### `log (string) -> nil`

Registra `string` en el registro logcat de Android con nivel `info` y etiqueta `LuaInterp`.

### Nota

No use notificaciones toasts dentro de las funciones. Es una buena manera de hacer que Simpletask se congele.

Devoluciones de llamada (callbacks)
===================================

La mayoría de las funciones utilizan la misma colección de parámetros:

### Parámetros generales

* `task`: La tarea como una cadena.
* `fields`: Partes de la tarea convertidas a diferentes tipos (por ejemplo, una marca de tiempo para `createdate`)
  * `completed`: Valor booleano que indica si la tarea se ha completado.
  * `completiondate`: La fecha de finalización en segundos de la tarea o `nil` si no está establecida.
  * `createdate`: La fecha de creación en segundos de la tarea o `nil` si no está establecida.
  * `due`: La fecha de vencimiento en segundos o `nil` si no está establecida.
  * `lists`: Una tabla con las listas de la tarea como claves. `fields.lists` en sí misma nunca será` nil`
  * `priority`: La prioridad de la tarea como una cadena.
  * `recurrence`: El patrón de recurrencia o repetición de la tarea como cadena o `nil` si no está establecida.
  * `tags`: Una tabla con las etiquetas de la tarea como claves. `fields.tags` en sí misma nunca será `nil`.
  * `task`: La tarea completa como una cadena.
  * `threshold`: La fecha límite en segundos o `nil` si no está establecida.
  * `text`: El texto ingresado cuando se creó la tarea.
* `extensions`: Una tabla con las extensiones de todo.txt (`key: val`) de la tarea como pares de valores clave. Sólo hay una entrada para cada clave, esto es para facilitar su uso. Si necesita varios pares `key: val` con la misma clave, puede procesar la tarea en Lua.

### `onFilter (task, fields, extensions) -> boolean`

Llamada para cada tarea como parte del filtrado de la lista de tareas.

#### Devuelve

* `verdadero` si la tarea debe mostrarse
* `falso` si la tarea no debe mostrarse

#### Notas

* Si hay un error de Lua en la devolución de llamada, se comporta como si hubiera devuelto `true`.
* Teniendo en cuenta que esta función es llamada muchas veces (para cada tarea en la lista) debería ser rápida. Si es demasiada lenta, Simpletask podría terminar en ANR (Android Not Responding).
* Debe definir la función `onFilter` en el filtro (no en la configuración Lua). Definirlo en la configuración principal no funcionará. Si el script de filtrado está vacío, la función `onFilter`  será indefinida.

### `onGroup (task, fields, extensions) -> string`

Llamada para cada tarea como parte del filtrado de la lista de tareas.

#### Devuelve

* El grupo al que pertenece la tarea.

#### Notas

* Si hay un error de Lua en la devolución de llamada, se comporta como si hubiera devuelto `" "`.
* Teniendo en cuenta que esta función es llamada muchas veces (para cada tarea en la lista), debería ser rápida. Simpletask podría terminar en ANR (Android Not Responding).
* Se debe definir la función `onGroup` en el filtro (no en la configuración de Lua). Definirla en la configuración principal no funcionará, si el script de filtrado está vacío, la función `onGroup` no estará definida.

### `onSort (task, fields, extensions) -> string`

Llamada para cada tarea como parte del ordenamiento de la lista de tareas. Esta función debería devolver una cadena para cada tarea. Esta cadena se usa para ordenar las tareas.

#### Returns

* La cadena que se utilizará para ordenar tareas.

#### Notas

- Si hay un error de Lua en la devolución de llamada, se comporta como si hubiera devuelto `""`.
- Teniendo en cuenta que esta función es llamada muchas veces (para cada tarea en la lista), debería ser rápida. Simpletask podría terminar en ANR (Android Not Responding).
- Se debe definir la función `onSort` en el filtro (no en la configuración de Lua). Definirla en la configuración principal no funcionará, si el script de filtrado está vacío, la función `onGroup` no estará definida.

### `onDisplay (task, fields, extensions) -> string`

Llamada para cada tarea antes de ser mostrada.

#### Devuelve

* La cadena que se mostrará.

#### Notas

- Si hay un error de Lua en la devolución de llamada, se comporta como si hubiera devuelto `""`.
- Teniendo en cuenta que esta función es llamada muchas veces (para cada tarea en la lista), debería ser rápida. Simpletask podría terminar en ANR (Android Not Responding).
- Se debe definir la función `onDisplay` en el filtro (no en la configuración de Lua). Definirla en la configuración principal no funcionará, si el script de filtrado está vacío, la función `onDisplay` no estará definida.

### `onAdd (task, fields, extensions) -> string`

Llamada para cada tarea antes de ser agregada a la lista de tareas.

#### Returns

* Una cadena que será la tarea a ser agregada.

#### Notes

* Si hay un error de Lua en la devolución de llamada, el texto original de la tarea será guardado.
* Debe definir la devolución de llamada `onAdd` en la configuración principal de Lua, ya que no debe usarse en un filtro. Definirla en un filtro no funcionará.

### `onTextSearch (taskText, searchText, caseSensitive) -> boolean`

Llamada para cada tarea al buscar una cadena de texto

#### Parameters

* `taskText`: El texto de la tarea tal como aparece en el archivo `todo.txt`.
* `searchText`: La cadena de texto a ser buscada.
* `caseSensitive`: `true`  si está activada en la configuración la busquéda indiferente a mayúsculas y minúsculas.

#### Returns

* `true` si la tarea debe mostrarse
* `false` si la tarea no debe mostrarse

### Notes

* Si ocurre un error de Lua en la devolución de llamada, se comporta como si hubiera devuelto `true`.
* Teniendo en cuenta que esta función es llamada muchas veces (para cada tarea en la lista), debería ser rápida. Si es demasiada lenta, Simpletask podría terminar en ANR (Android Not Responding).

Configuración
=============

La configuracón es leída en el inicio de la aplicación y siempre que cambie o se ejecute desde la pantalla de Configuración Lua.

La configuración de Lua siempre sobreescribirá el valor definido en la Configuración (Lua gana).

### `theme () -> string`

#### Parameters

Ninguno

#### Devuelve

* `"dark"`  para el tema Material Oscuro
* `"black"` para el tema Negro (funciona bien en dispositivos con pantalla Amoled)
* `"light"` para el tema Material Claro

#### Notas

* Requiere de un reinicio de la aplicación para que tenga efecto (más precisamente, se necesita recrear la actividad).

### `tasklistTextSize () -> float`

#### Parámetros

Ninguno

#### Devuelve

El tamaño de la fuente de la lista de tareas en `sp` (Scale-independent Pixels) como un número flotante.

#### Notas

* Requiere de un reinicio de la aplicación para que tenga efecto (más precisamente, se necesita recrear la actividad).
* El tamaño por defecto en Android es `14sp`.

Ejemplos
========

El siguiente código mostrará sólo tareas vencidas donde las tareas sin una fecha de vencimiento establecida se considerarán que nunca vencen.

The following code will show only overdue tasks where tasks without a due date, will never be overdue.

Mostrar tareas sin etiquetas ni listas (el buzón de entrada de GTD):

    function onFilter(t,f,e)
       return next(f.tags)==nil and next(f.lists)==nil
    end

Mostrar todas las tareas en la lista `errands`:

    function onFilter(t,f,e)
       return f.lists["errands"]
    end

Cambiar el tamaño de la fuente de la lista de tareas a `16sp`:

    function tasklistTextSize()
       return 16.0
    end

La búsqueda difusa 8.0.0 en Lua (fuzzy search):

    function onTextSearch(text, search, case)
        pat = string.gsub(search, ".", "%0.*")
        res = string.match(text, pat)
        return res~=nil
    end

Una devolución de llamada de grupo para agrupar por lista con encabezado vacío personalizado si la tarea no pertenece a ninguna lista:

    function onGroup(t,f,e)
        if not next(f.lists) then
            return "Inbox"
        else
            return next(f.lists)
        end
    end

No agrupar y no mostrar ningún encabezado (independientemente del orden de clasificación):

    function onGroup()
        return ""
    end

Una devolución de llamada para modificar como se muestra una tarea:

    function onDisplay(t,f,e)
       if f.due~=nil and os.time() > f.due then
         --- Mostrar tareas vencidas en mayúsculas.
         --- (Poner un '=' delante reemplaza la tarea entera)
         return "="..string.upper(f.tasktext)
       end
       return f.tasktext
    end

Aprender Lua
============

Una búsqueda en Google debería devolver muchos y buenos recursos. [*Programando en Lua*](https://www.lua.org/pil/contents.html) debería cubrir casi todo.
