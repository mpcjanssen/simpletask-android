Simpletask
==========
See in [English](./index.en.md).

[Simpletask](https://github.com/mpcjanssen/simpletask-android) está basado en el brillante [todo.txt](http://todotxt.com) de [Gina Trapani](http://ginatrapani.org/). El objetivo de la aplicación es proporcionar una herramienta para utilizar GTD ([Getting Things Done](https://es.m.wikipedia.org/wiki/Getting_Things_Done)) sin proporcionar una cantidad agobiante de opciones. Pese a que Simpletask puede ser personalizado con una cantidad considerable de Ajustes, los predeterminados tendrían que ser adecuados y no requerir ningún cambio.

[Simpletask](http://mpcjanssen.nl/doc/simpletask/) puede ser utilizado como un gestor muy sencillo de listas de tareas o como un gestor de acciones más complejas para GTD o [Manage Your Now (Gestione Su Ahora)](./MYN.es.md).

Extensiones
-----------

Simpletask soporta las siguientes extensiones de todo.txt:

-   Fecha de vencimiento: `due:YYYY-MM-DD` (año-mes-día)
-   Fecha umbral/de inicio: `t:YYYY-MM-DD`
-   Tareas recurrentes: `rec:[0-9]+[dwmy]` (días, semanas, meses, años) como se describe [aquí](https://github.com/bram85/topydo/wiki/Recurrence) pero con alguna variación.
    - De forma predeterminada Simpletask utilizará las fechas originales de la tarea para crear la tarea recurrente, no la fecha de conclusión como se describe en el enlace. Este comportamiento puede ser configurado de los Ajustes.
    - La descripción del formato unas lineas más arriba es una [expresión regular](http://es.wikipedia.org/wiki/Expresi%C3%B3n_regular), o sea, que en lenguaje humano la sintaxis es `rec:` seguido de 1 o más números (el `+`) seguido por una de las siguientes `d` día, `w` semana, `m` mes o `y` año. Por ejemplo `rec:12d` especifica una tarea recurrente cada 12 días.
- Tareas escondidas: `h:1`, esto permite tareas ficticias asignadas a listas y etiquetas predefinidas de modo que las mismas estén disponibles incluso si la última tarea que las contenía se borra del archivo `todo.txt`. Estas tareas no serán mostradas de forma predeterminada. Temporalmente se pueden mostrar usando los Ajustes.

Soporte
-------

Simpletask es traducido utilizando [Weblate](https://hosted.weblate.org/engage/simpletask/). Siga el enlace para contribuir.

Únase al chat en  [![Gitter](images/gitter.png)](https://gitter.im/mpcjanssen/simpletask-android?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge), IRC en [#simpletask](https://webchat.freenode.net/?channels=simpletask) en Freenode o Matrix en [#simpletask:matrix.org](https://matrix.to/#/#simpletask:matrix.org).

Si  quiere informar de un problema o pedir una característica nueva para [Simpletask](https://github.com/mpcjanssen/simpletask-android/) puede ir al [tracker](https://github.com/mpcjanssen/simpletask-android/issues). Si encuentra Simpletask útil, puedes comprar la aplicación (vea Ajustes) o donarme unas cervezas vía [Paypal](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=mpc%2ejanssen%40gmail%2ecom&lc=NL&item_name=mpcjanssen%2enl&item_number=Simpletask&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted).

Vea el menú para encontrar más secciones de ayuda o haga clic abajo.

- [Interfaz de usuario](./ui.es.md) Ayuda para la interfaz de usuario.
- [Changelog](./changelog.md)
- [Listas y Etiquetas](./listsandtags.es.md) ¿Por qué Simpletask usa listas y etiquetas en lugar de Contextos y Proyectos de todo.txt?
- [Definiendo intents](./intents.es.md) Intents que pueden ser utilizados para automatizar Simpletask
- [Utilizando Simpletask para 1MTD/MYN - 1 Minute ToDo List/Manage Your Now](./MYN.es.md)
- [Lua](./script.es.md)
