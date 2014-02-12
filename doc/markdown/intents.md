# Introduction

Simpletask supports a couple of intents which can be used by other applications (e.g. tasker) to create tasks or display lists.

## Create task in background

To create a task in the background, so without showing simpletask, you can use the intent:
-   Intent action: `nl.mpcjanssen.simpletask.BACKGROUND_TASK`
-   Intent string extra: `task`

the intent will have one extra string `task` which contains the task to be added.

For example to create a task from tasker use the following action:

-   Action: `nl.mpcjanssen.simpletask.BACKGROUND_TASK`
-   Cat: Default
-   Mime Type: text/\*
-   Extra: task: `<Task text with possible variables here> +tasker`
-   Target: Activity

I like to add the `+tasker` tag to be able to quickly filter tasks that were created by tasker.

## Open with specific filter

To open Simpletask with a specific filter you can use the intent:
-   Intent action: `nl.mpcjanssen.simpletask.START_WITH_FILTER`
-   Intent extras: The following extras can be added as part of the
    intent. Note that currently the names still reflect the original
    naming of lists/tags.

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="left" />

<col  class="left" />

<col  class="left" />
</colgroup>
<thead>
<tr>
<th scope="col" class="left">Name</th>
<th scope="col" class="left">Type</th>
<th scope="col" class="left">Description</th>
</tr>
</thead>

<tbody>
<tr>
<td class="left">`CONTEXTS`</td>
<td class="left">String</td>
<td class="left">list of lists in filter separated by '\n' or ','</td>
</tr>


<tr>
<td class="left">`PROJECTS`</td>
<td class="left">String</td>
<td class="left">list of tags in filter separated by '\n' or ','</td>
</tr>


<tr>
<td class="left">`PRIORITIES`</td>
<td class="left">String</td>
<td class="left">list of priorities in filter separated by '\n' or ',</td>
</tr>


<tr>
<td class="left">`CONTEXTSnot`</td>
<td class="left">Boolean</td>
<td class="left">true to invert the lists filter</td>
</tr>


<tr>
<td class="left">`PROJECTSnot`</td>
<td class="left">Boolean</td>
<td class="left">true to invert the tags filter</td>
</tr>


<tr>
<td class="left">`PRIORITIESnot`</td>
<td class="left">Boolean</td>
<td class="left">true to invert the priorities filter</td>
</tr>


<tr>
<td class="left">`HIDECOMPLETED`</td>
<td class="left">Boolean</td>
<td class="left">true to hide completed tasks</td>
</tr>


<tr>
<td class="left">`HIDEFUTURE`</td>
<td class="left">Boolean</td>
<td class="left">true to hide tasks with a threshold date</td>
</tr>


<tr>
<td class="left">`SORTS`</td>
<td class="left">String</td>
<td class="left">active sort (see below)</td>
</tr>
</tbody>
</table>

### Sorts extra

SORTS contains a comma or '\n' separated list of sort keys and their
direction with a `!` in between. Giving `<direction>!<sort key>`.

#### Direction

-   `+` : Ascending
-   `-` : Descending

#### Sort keys

See list in <http://mpcjanssen.nl/fossil/simpletask/artifact/ac6b9bf579b8d1a9c23083031852a0fdd81efb75?ln=42-51>

#### Example

-   The sort `+!completed,+!alphabetical` sorts completed tasks last and then sorts alphabetical.
-   The sort `+!completed,-!alphabetical` sorts completed tasks last and
    then sorts reversed alphabetical.

### Tasker example

-   Action: `nl.mpcjanssen.simpletask.START_WITH_FILTER`
-   Cat: `Default`
-   Mime Type:
-   Extra: `CONTEXTS:Office,Online`
-   Extra: `SORTS:+!completed,+!alphabetical`
-   Target: `Activity`

Due to limitations in Tasker you can only add 2 extras. So instead you can use the am shell command. For example:

    am start -a nl.mpcjanssen.simpletask.START_WITH_FILTER -e SORTS +!completed,+!alphabetical -e PROJECTS project1,project2 -e CONTEXTS @errands,@computer --ez CONTEXTSnot true -c android.intent.category.DEFAULT -S

The `-S` at the end will ensure the app is properly restarted if it's
already visible. However with tasker the `-S` seems not to work. So there try it without.