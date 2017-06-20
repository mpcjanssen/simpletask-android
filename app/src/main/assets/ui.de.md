Benutzeroberfläche
==================

Diese Seite ist zur Zeit in Arbeit und wird noch ergänzt.

## Filtern von Aufgaben

### Filter umkehren

Mit der Option `Filter umkehren` werden die markierten Elemente ausgeblendet. Das folgende Beispiel zeigt, warum dies sinnvoll sein kann.

Stellen Sie sich vor, dass Sie folgende Listen definiert haben:

- `@arbeit`
- `@privat`
- `Projekt`
- `Irgendwann`

Zwei dieser Listen enthalten nächste Aktionen (`@arbeit` und `@privat`), die beiden anderen nicht. Wenn Sie nun einen Filter erstellen möchten, der nur nächste Aktionen zeigt, gibt es zwei Möglichkeiten.

#### Der 'falsche' Weg

Wenn Sie einen Filter erstellen, in dem die Listen `@arbeit` und `@privat` markiert sind, wird dies Anfangs ohne Probleme funktionieren. Wenn Sie nun eine neue Aufgabe mit der Liste `@einkauf` erstellen und diese ebenfalls eine nächste Aktion ist, wird sie jedoch erst angezeigt, wenn Sie den Filter auf die Liste `@einkauf` erweitern. Dadurch besteht die Gefahr, dass neue Aufgaben übersehen werden.

#### Der 'richtige' Weg

Erstellen Sie einen Filter, in welchem die Listen `Projekt` und `Irgendwann` sowie die Option `Filter umkehren` markiert sind. Nun werden alle Aufgaben angezeigt, die nicht auf den Listen `Projekt` oder `Irgendwann` stehen. Wenn Sie nun eine neue Aufgabe mit der Liste `@einkauf` hinzufügen, wird diese ebenfalls in den Ergebnissen enthalten sein.

Natürlich zeigt der Filter nun jede neu erstellte Liste an, auch wenn sie keine nächsten Aktionen enthält. Um sie auszublenden, muss der Filter ebenfalls aktualisiert werden.
Der Vorteil dieser Methode ist, dass eher zu viele als zu wenig Informationen angezeigt werden.
