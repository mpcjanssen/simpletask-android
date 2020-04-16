Intents
=======

Simpletask soporta un par de intents que pueden ser utilizados por otras aplicaciones (p. ej. Tasker) para crear tareas o mostrar listas.

Crear tarea entre bastidores
----------------------------

Para crear una tarea en segundo plano, sin mostrar Simpletask,  puede utilizar el intent:

-   Intent action: `nl.mpcjanssen.simpletask.BACKGROUND_TASK`
-   Intent string extra: `task`

El intent tendrá una cadena extra `task` que contiene la tarea a ser añadida.

Por ejemplo para crear una tarea desde Tasker, use la acción siguiente:

-   Action: `nl.mpcjanssen.simpletask.BACKGROUND_TASK`
-   Cat: Default
-   Mime Type: text/\*
-   Extra: task: `<Task text with possible variables here> +tasker`
-   Target: Activity

Me gusta añadir la etiqueta `+tasker` para posder filtrar rápidamente la tareas creadas por tasker.

Abrir con un filtro específico
------------------------------

Para abrir Simpletask con un filtro específico puede utilizar el intent:

-   Intent action: `nl.mpcjanssen.simpletask.START_WITH_FILTER`
-   Intent extras: Los extras siguientes pueden ser añadidos como parte del intent. Note que actualmente los nombres todavía reflejan los nombres originales de listas/etiquetas.

<table>
<colgroup>
<col width="19%" />
<col width="12%" />
<col width="67%" />
</colgroup>
<thead>
<tr class="header">
<th align="left">Nobre</th>
<th align="left">Tipo</th>
<th align="left">Descrición</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td align="left">CONTEXTS</td>
<td align="left">String</td>
<td align="left">lista de listas en filtro separadas por '\n'</td>
</tr>
<tr class="even">
<td align="left">PROJECTS</td>
<td align="left">String</td>
<td align="left">lista de etiquetas en filtro separadas por '\n'</td>
</tr>
<tr class="odd">
<td align="left">PRIORITIES</td>
<td align="left">String</td>
<td align="left">lista de prioridades en filtro separadas por '\n'</td>
</tr>
<tr class="even">
<td align="left">CONTEXTSnot</td>
<td align="left">Boolean</td>
<td align="left">true para invertir el filtro de listas</td>
</tr>
<tr class="odd">
<td align="left">PROJECTSnot</td>
<td align="left">Boolean</td>
<td align="left">true para invertir el filtro de etiquetas</td>
</tr>
<tr class="even">
<td align="left">PRIORITIESnot</td>
<td align="left">Boolean</td>
<td align="left">true para invertir el filtro de prioridades</td>
</tr>
<tr class="odd">
<td align="left">HIDECOMPLETED</td>
<td align="left">Boolean</td>
<td align="left">true para ocultar las tareas completadas</td>
</tr>
<tr class="even">
<td align="left">HIDEFUTURE</td>
<td align="left">Boolean</td>
<td align="left">true para ocultar las tareas con fecha umbral</td>
</tr>
<tr class="odd">
<td align="left">SORTS</td>
<td align="left">String</td>
<td align="left">ordenación vigente (ver abajo)</td>
</tr>
</tbody>
</table>

### Extra ordenación

SORTS contiene una lista separada por comas o comillas de claves para ordenación y su dirección separadas por un `!`. Suministrar `<dirección>!<clave para ordenación>`.

#### Dirección

-   `+` : Ascendente
-   `-` : Descendente

#### Claves para ordenación

Vea la lista [aquí](https://github.com/mpcjanssen/simpletask-android/app/blob/master/src/main/res/values/donottranslate.xml#L45-59)

#### Ejemplo

-   La ordenación `+!completed,+!alphabetical` ordena las tareas colocando las completadas detrás y después las ordena alfabéticamente.
-   La ordenación `+!completed,-!alphabetical` ordena las tareas colocando las completadas detrás y después las ordena en orden alfabético inverso.

### Ejemplo de Tasker

-   Action: `nl.mpcjanssen.simpletask.START_WITH_FILTER`
-   Cat: `Default`
-   Mime Type:
-   Extra: `CONTEXTS:Officina,Online`
-   Extra: `SORTS:+!completed,+!alphabetical`
-   Target: `Activity`

Debido a limitaciones en Tasker sólo puede añadir 2 extras. Si precisa más puede utilizar el comando shell am. Por ejemplo:

    am start -a nl.mpcjanssen.simpletask.START_WITH_FILTER -e SORTS +!completed,+!alphabetical -e PROJECTS proyecto1,proyecto2 -e CONTEXTS @compras,@ordenador --ez CONTEXTSnot true -c android.intent.category.DEFAULT -S

La `-S` al final asegurará que la aplicación arranca de nuevo correctamente si ya estaba visible. Aun así con Tasker la `-S` parece no funcionar, así que pruebe sin esta opción.

