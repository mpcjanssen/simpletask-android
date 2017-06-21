Scripten mit Lua
====================

Simpletask hat eine eingebaute Engine für Lua-Skripte, die für die Konfiguration und für Callbacks verwendet werden kann.
Die Konfiguration wird gelesen, wenn Sie die App neu starten oder wenn Sie sie im Lua-Konfigurationsbildschirm ändern.
Callbacks werden ausgeführt, wenn bestimmte Ereignisse eintreten (z.B. Filtern der Liste).
Sowohl die Konfiguration als auch die Callbacks rufen bestimmte Lua-Funktionen auf. Details zu den unterstützten Callbacks finden Sie weiter unten.

Jeglicher Code (sowohl für Konfiguration als auch für Callbacks) wird im selben Lua-Interpreter ausgeführt.
Auf diese Weise können Sie Helferfunktionen in der Konfiguration definieren und sie anschließend in Callbacks verwenden. *

**Anmerkung: Der Callback für Filter wurde in der Version 8.0 geändert (siehe unten).*

Um bestehende Filter in App oder Widgets in das 8.0-Format zu übertragen, gehen Sie wie folgt vor:

Fügen Sie am Anfang des Skriptes `function onFilter(t,f,e)` ein und an dessen Ende `end`. Stellen Sie allen Feldern ein `f.` voran: `due` wird zu `f.due` usw. Ein Beispiel:

aus

    if due~=nil then
        return os.time() >= due;
    end
    --- tasks with no due date are not overdue.
    return false;

wird

    function onFilter(t,f,e)
        if f.due~=nil then
            return os.time() >= f.due;
        end
        --- tasks with no due date are not overdue.
        return false;
    end


Helferfunktionen
================

## `toast (string) -> nil`

Zeigt `string` als einen Android Toast an. Nützlich für das Debuggen von Skripten.

#### Hinweis

Verwenden Sie keine Toasts innnerhalb von Funktionen! Dies kann zum Absturz von Simpletask führen.


Callbacks
=========

## `onFilter (task, fields, extensions) -> Boolean`

Wird beim Filtern der Aufgabenliste für jede einzelne Aufgabe aufgerufen.

#### Parameter

* `task`: Die Aufgabe als Zeichenkette (String).
* `fields`: Teile der Aufgabe, die in verschiedene Typen umgewandelt wurden (z. B. ein Zeitstempel für `createdate`)
    * `completed`: Boolean (Schaltvariable), die anzeigt, ob die Aufgabe erledigt ist.
    * `completiondate`: Das Fertigstellungsdatum der Aufgabe in Sekunden oder `nil`, wenn nicht gesetzt.
    * `createdate`: Das Erstellungsdatum der Aufgabe in Sekunden oder `nil`, wenn nicht gesetzt.
    * `due`: Das Fertigstellungsdatum der Aufgabe in Sekunden oder `nil`, wenn nicht gesetzt.
    * `lists`: Eine Tabelle mit den Listen der Aufgabe als Werte. `fields.lists` selbst wird niemals `nil` sein.
    * `priority`: Die Priorität der Aufgabe als String.
    * `recurrence`: Das Wiederholungsmuster der Aufgabe als String oder `nil`, wenn nicht gesetzt.
    * `tags`: Eine Tabelle mit den Tags der Aufgabe als Schlüssel. `fields.tags` selbst wird niemals `nil` sein.
    * `task`: Die gesamte Aufgabe als String.
    * `threshold`: Das Anfangsdatum der Aufgabe in Sekunden oder `nil`, wenn nicht gesetzt.
* `extensions`: Eine Tabelle mit den Todo.txt Erweiterungen (`key:val`) der Aufgabe als Schlüssel-Wert-Paare. Für jeden Schlüssel gibt es nur einen Eintrag, um die Anwendung einfach zu halten.
Wenn Sie mehrere `key:val`-Paare mit dem gleichen Schlüssel benötigen, können Sie die Aufgabe in Lua parsen.

#### Rückgabewerte

* `true` wenn die Aufgabe angezeigt werden soll
* `false` wenn die Aufgabe nicht angezeigt werden soll

#### Hinweise

* Wenn der Callback einen Lua-Fehler enthält, wird `true` zurückgeben.
* Da diese Funktion sehr oft aufgerufen wird (für jede Aufgabe in der Liste) sollte sie schnell sein. Wenn sie zu langsam ist, kann dies "Application not Responding"-Meldungen in Android verursachen.
* Sie sollten die Funktion "onFilter" im Filter definieren, nicht in der Konfiguration. Sie in der Hauptkonfiguration zu definieren funktioniert nicht, wenn das Filter-Skript leer ist, bleibt die Funktion `onFilter` undefiniert.

## `onGroup (task, fields, extensions) -> String`

Wird beim Filtern der Aufgabenliste für jede einzelne Aufgabe aufgerufen.

#### Parameter

* `task`: Die Aufgabe als Zeichenkette (String).
* `fields`: Teile der Aufgabe, die in verschiedene Typen umgewandelt wurden (z. B. ein Zeitstempel für `createdate`)
    * `completed`: Boolean, die anzeigt, ob die Aufgabe erledigt ist.
    * `completiondate`: Das Fertigstellungsdatum der Aufgabe in Sekunden oder `nil`, wenn nicht gesetzt.
    * `createdate`: Das Erstellungsdatum der Aufgabe in Sekunden oder `nil`, wenn nicht gesetzt.
    * `due`: Das Fertigstellungsdatum der Aufgabe in Sekunden oder `nil`, wenn nicht gesetzt.
    * `lists`: Eine Tabelle mit den Listen der Aufgabe als Werte. `fields.lists` selbst wird niemals `nil` sein.
    * `priority`: Die Priorität der Aufgabe als String.
    * `recurrence`: Das Wiederholungsmuster der Aufgabe als String oder `nil`, wenn nicht gesetzt.
    * `tags`: Eine Tabelle mit den Tags der Aufgabe als Schlüssel. `fields.tags` selbst wird niemals `nil` sein.
    * `task`: Die gesamte Aufgabe als String.
    * `threshold`: Das Anfangsdatum der Aufgabe in Sekunden oder `nil`, wenn nicht gesetzt.
* `extensions`: Eine Tabelle mit den Todo.txt Erweiterungen (`key:val`) der Aufgabe als Schlüssel-Wert-Paare. Für jeden Schlüssel gibt es nur einen Eintrag, um die Anwendung einfach zu halten.
Wenn Sie mehrere `key:val`-Paare mit dem gleichen Schlüssel benötigen, können Sie die Aufgabe in Lua parsen.

#### Rückgabewerte

* Die Gruppe, zu der diese Aufgabe gehört.

#### Hinweise

* Wenn der Callback einen Lua-Fehler enthält, wird `true` zurückgegeben.
* Da diese Funktion sehr oft aufgerufen wird (für jede Aufgabe in der Liste) sollte sie schnell sein. Wenn sie zu langsam ist, kann dies "Application not Responding"-Meldungen in Android verursachen.
* Sie sollten die Funktion `onGroup` im Filter definieren, nicht in der Konfiguration. Sie in der Hauptkonfiguration zu definieren funktioniert nicht, wenn das Filter-Skript leer ist, bleibt die Funktion `onGroup` undefiniert.

## `onTextSearch (taskText, caseSensitive) -> Boolean`

Wird bei der Volltextsuche für jede einzelne Aufgabe aufgerufen.

#### Parameter

* `TaskText`: Der Text der Aufgabe, wie er in der `todo.txt`-Datei erscheint
* `SearchText`: Der Text, nach dem gesucht werden soll.
* `caseSensitive`: `true` wenn in den Einstellungen konfiguriert ist, dass beim Suchen die Groß-/Kleinschreibung berücksichtigt werden soll.

#### Rückgabewerte

* `true` wenn die Aufgabe angezeigt werden soll
* `false` wenn die Aufgabe nicht angezeigt werden soll

#### Hinweise

* Wenn der Callback einen Lua-Fehler enthält, wird `true` zurückgegeben.
* Da diese Funktion sehr oft aufgerufen wird (für jede Aufgabe in der Liste) sollte sie schnell sein. Wenn sie zu langsam ist, kann dies "Application not Responding"-Meldungen in Android verursachen.

Konfiguration
=============

Die Konfiguration wird beim Start von Simpletask gelesen oder wenn sie geändert oder im Lua-Konfigurationsbildschirm ausgeführt wird.
Die Lua-Konfiguration überschreibt immer die Werte aus den Einstellungen (Lua gewinnt).

## `theme () -> String`

Ändert das Design-Thema der Benutzeroberfläche 

#### Parameter

Keine

#### Rückgabewerte

* `"dark"` für das dunkle Material-Design-Thema
* `"black"` für das schwarze Thema (funktioniert gut auf Amoled Geräten).
* `"light_darkactionbar"` für das helle Material-Design-Thema

#### Hinweise

* Erfordert einen Neustart der Anwendung, um wirksam zu werden (genauer gesagt muss sie die Aktivität neu erstellen)

## `tasklistTextSize () -> Float`

#### Parameter

Keine

#### Rückgabewert

* Die Schriftgröße des Hauptbildschirms von Simpletask als Float

#### Notes

* Erfordert einen Neustart der Anwendung um wirksam zu werden (genauer gesagt muss sie die Aktivität neu erstellen)
* Die Standard-Schriftgröße in Android ist momentan `14sp`

Beispiele
=========

Der folgende Code zeigt nur überfällige Aufgaben. Aufgaben ohne Fälligkeitsdatum werden ausgeblendet.

    function onFilter(t,f,e)
       if f.due~=nil then
           return os.time() > f.due;
       end
       --- tasks with no due date are not overdue.
       return false;
    end

Zeige alle Aufgaben ohne Listen oder Tags (der Posteingang bei GTD):

    function onFilter(t,f,e)
       return next(f.tags)==nil and next(f.lists)==nil
    end

Zeige alle Aufgaben mit dem `@errands`-List:

    function onFilter(t,f,e)
       return f.lists["errands"]
    end

Ändere die Schriftgröße des Hauptbildschirms auf `16sp`:

    function tasklistTextSize()
       return 16.0
    end

Fuzzy-Suche in Lua ab Version 8.0.0:

    function onTextSearch(text, search, case)
        pat = string.gsub(search, ".", "%0.*")
        res = string.match(text, pat)
        return res~=nil
    end

Callback zur Gruppierung nach Liste mit angepasster leerer Titelzeile:

    function onGroup(t,f,e)
        if not next(f.lists) then
            return "Inbox"
        else
            return next(f.lists)
        end
    end

Gruppiere überhaupt nicht und blende die Titelzeilen aus (unabhängig von der Sortierreihenfolge)

    function onGroup()
        return ""
    end

Lua lernen
==========

Mit googeln findet man viele gute Quellen. [*Programmierung in Lua*](https://www.lua.org/pil/contents.html) sollte so gut wie alle Fragen beantworten.






