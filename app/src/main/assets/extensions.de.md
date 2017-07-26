Todo.txt Erweiterungen
----------------------

Simpletask unterstützt die folgenden Erweiterungen des todo.txt-Formates:

- Fälligkeitsdatum mit `due:YYYY-MM-DD`
- Anfangsdatum mit `t:YYYY-MM-DD`

- Wiederholung mit `rec:\+?[0-9]+[dwmyb]` wie [hier](https://github.com/bram85/topydo/wiki/Recurrence) beschrieben, aber mit einer Ergänzung:

    - Standardmäßig verwendet Simpletask das Fertigstellungsdatum für die Wiederholung, wie im Link beschrieben. Schreibt man jedoch vor die Zahl ein Plus (z. B. `rec:+2w`), wird das neue Datum vom ursprünglichen Anfangs- oder Fälligkeitsdatum aus berechnet.
    - `rec:1b` bewirkt die Wiederholung nach 1 Werktag (Abkürzung für `b`usiness-day).
    - Das Format wird durch einen regulären Ausdruck beschrieben. In Worten ist die Syntax `rec:` optional gefolgt von einem `+` dann einer Zahl und am Ende ein `d`ay, `w`eek, `m`onth oder `y`ear. Eine Aufgabe mit "rec: 12d" wird zum Beispiel nach 12 Tagen wiederholt.
- Versteckte Aufgaben mit `h:1`

  dienen dazu, Listen oder Tags vorzudefinieren und permanent zu machen. Die vordefinierten Tags und Listen bleiben auch dann noch erhalten, wenn alle anderen Aufgaben mit diesen Tags oder Listen aus der `todo.txt`-Datei entfernt wurden. Standardmäßig werden diese Aufgaben nicht angezeigt, bei Bedarf können sie jedoch in den Einstellungen eingeblendet werden.

