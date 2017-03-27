Utilizando el nuevo sistema de compilación Gradle
=================================================

El sistema de compilación ha sido cambiado de ant/Eclipse/Idea a gradle por dos razones principales:

1.  Gradle hace muy fácil tener diferentes variantes de su aplicación con el mismo código (p. ej. una libre y una de donación)
2.  Gradle es el nuevo sistema de compilación oficial de Android así que el cambio tenia que pasar en todo caso.

Descripción del proceso de compilación
--------------------------------------

Use `gradle(w) tasks` para conseguir una lista completa, la tareas útiles ya definidas son:

-   `assembleReleaseFree`: compilación de la apk libre para la Tienda de Google
-   `assembleReleaseDonate`: compilación de la apk de donación
-   `installDevDebug`: compilación de una versión del código fuente actual que pueden ser instalada en paralelo con la versión de la Tienda de Google.
-   `connectedInstrumentTestUnitDebug`: Una variante especial para testeo unitario. No quiero utilizar la versión Dev para esto porque se desinstala después de la ejecución de prueba.
-   `check`: Depende de `connectedInstrumentTestUnitDebug` de modo que ejecuta la batería de pruebas.
-   `build`: Compila todas las variantes de la aplicación de golpe, también depende de `check` de modo que correrá la batería de pruebas

