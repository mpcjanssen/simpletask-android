Extensiones todo.txt
--------------------------

Simpletask soporta las siguientes extensiones todo.txt:

-   Fecha de vencimiento: `due:YYYY-MM-DD`

-   Fecha de inicio/límite: `t:YYYY-MM-DD`

-   Repetición: `rec:\+?[0-9]+[dwmyb]` tal como se describe [aquí](https://github.com/bram85/topydo/wiki/Recurrence) pero con una salvedad:
    
    - Por defecto, Simpletask utilizará la fecha de completado para la repetición de la tarea, como se describe en el enlace. Sin embargo, si `rec` se acompaña de un signo `+` (por ejemplo `rec:+2w`), la fecha se calcula a partir de las fecha originales de vencimiento o inicio.
    - `rec:1b`: se repetirá luego de un día laborable (mnemotécnica **b**usiness-day).
    - El formato se describe como una expresión regular, por lo tanto en palabras, la sintáxis es `rec` seguido de un `+` opcional, luego uno o más números seguidos luego por `d`ay (día) o `w`eek (semana) o `m`onth o `y`ear (año). Por ejemplo, `rec:12d` significa que la tarea se repetirá cada 12 días.

- Tareas ocultas con la etiqueta `h:1`, lo que permite definir  tareas ficticias con listas y etiquetas predefinidas para que las listas y las etiquetas estén disponibles incluso si la última tarea con la etiqueta / lista se elimina de `todo.txt`. Estas tareas no son visibles por defecto. Se pueden visualizar temporalmente desde la configuración.