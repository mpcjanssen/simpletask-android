Intents
=======

Simpletask unterstützt einige Android-Intents, die von anderen Apps (z. B. Tasker) verwendet werden können, um Aufgaben zu erstellen oder Listen anzuzeigen.

Neue Aufgabe im Hintergrund
---------------------------

Um eine Aufgabe im Hintergrund zu erstellen, also ohne Simpletask anzuzeigen, kann dieser Intent verwendet werden:

-   Intent action: `nl.mpcjanssen.simpletask.BACKGROUND_TASK`
-   Intent string extra: `task`

Der Intent übergibt zusätzlich die Zeichenkette `task`, welche die zu erstellende Aufgabe enthält.

Zum Beispiel kann mit Tasker eine neue Aufgabe erstellt werden, indem man folgende Aktion definiert: 

-   Action: `nl.mpcjanssen.simpletask.BACKGROUND_TASK`
-   Cat: Default
-   Mime Type: text/\*
-   Extra: task: `<Aufgabentext mit möglichen Variablen> +tasker`
-   Target: Activity

Ich füge das `+tasker`-Tag hinzu, um später die von Tasker erstellten Aufgaben schneller filtern zu können.

Öffnen mit aktiviertem Filter
-----------------------------

Um Simpletask mit einem bestimmten Filter zu öffnen, kann folgender Intent genutzt werden:

-   Intent action: `nl.mpcjanssen.simpletask.START_WITH_FILTER`
-   Intent extras: Die folgenden Extras können als Teil des Intents hinzugefügt werden. Beachten Sie, dass die Namen derzeit noch die ursprüngliche Namensgebung, also Contexts und Projects widerspiegeln.

<table>
<colgroup>
<col width="19%" />
<col width="12%" />
<col width="67%" />
</colgroup>
<thead>
<tr class="header">
<th align="left">Name</th>
<th align="left">Typ</th>
<th align="left">Syntax</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td align="left">CONTEXTS</td>
<td align="left">String</td>
<td align="left">filtert nach einer Liste von Listen, Trennzeichen '\n'</td>
</tr>
<tr class="even">
<td align="left">PROJECTS</td>
<td align="left">String</td>
<td align="left">filtert nach einer Liste von Tags, Trennzeichen '\n'</td>
</tr>
<tr class="odd">
<td align="left">PRIORITIES</td>
<td align="left">String</td>
<td align="left">filtert nach einer Liste von Prioritäten, Trennzeichen '\n'</td>
</tr>
<tr class="even">
<td align="left">CONTEXTSnot</td>
<td align="left">Boolean</td>
<td align="left">"true" invertiert den Filter für Listen</td>
</tr>
<tr class="odd">
<td align="left">PROJECTSnot</td>
<td align="left">Boolean</td>
<td align="left">"true" invertiert den Filter für Tags</td>
</tr>
<tr class="even">
<td align="left">PRIORITIESnot</td>
<td align="left">Boolean</td>
<td align="left">"true" invertiert den Filter für Prioritäten</td>
</tr>
<tr class="odd">
<td align="left">HIDECOMPLETED</td>
<td align="left">Boolean</td>
<td align="left">"true" blendet erledigte Aufgaben aus</td>
</tr>
<tr class="even">
<td align="left">HIDEFUTURE</td>
<td align="left">Boolean</td>
<td align="left">"true" blendet Aufgaben mit Anfangsdatum in der Zukunft aus</td>
</tr>
<tr class="odd">
<td align="left">SORTS</td>
<td align="left">String</td>
<td align="left">aktiviert eine Sortierung (siehe unten)</td>
</tr>
</tbody>
</table>

### Sortierung

SORTS enthält eine mit Kommas oder '' getrennte Liste von Sortierschlüsseln und deren Richtung mit einem `!` dazwischen, also `<Richtung>!<Sortierschlüssel>`.

#### Richtung

- `+`: Aufsteigend
- `-`: Absteigend

#### Sortierschlüssel

Siehe Liste in [hier](https://github.com/mpcjanssen/simpletask-android/blob/master/app/src/main/res/values/donottranslate.xml#L45-59)
#### Beispiel

- Die Sortierung `+!completed,+!alphabetical` sortiert abgeschlossene Aufgaben ans Ende und dann alphabetisch.
- Die Sortierung `+!completed,-!alphabetical` sortiert abgeschlossene Aufgaben ans Ende und dann alphabetisch in umgekehrter Richtung.

### Beispiel für Tasker

-   Action: `nl.mpcjanssen.simpletask.START_WITH_FILTER`
-   Cat: `Default`
-   Mime Type:
-   Extra: `CONTEXTS:Office,Online`
-   Extra: `SORTS:+!completed,+!alphabetical`
-   Target: `Activity`

Aufgrund von Einschränkungen in Tasker können nur bis zu zwei Extras hinzugefügt werden. Stattdessen können Sie den am-shell-Befehl verwenden, also beispielsweise:

    am start -a nl.mpcjanssen.simpletask.START_WITH_FILTER -e SORTS +!completed,+!alphabetical -e PROJECTS project1,project2 -e CONTEXTS @errands,@computer --ez CONTEXTSnot true -c android.intent.category.DEFAULT -S

Das `-S` am Ende wird sicherstellen, dass die App korrekt neu gestartet wird, wenn sie bereits sichtbar ist. Dagegen scheint das `-S` mit Tasker nicht zu funktionieren.
