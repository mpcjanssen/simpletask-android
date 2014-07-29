Javascript
==========

Simpletask provides (experimental) advanced filtering of tasks using Javascript. To use Javascript you will need to enable it from the settings.

After enabling Javascript, the filter activity will show an additional SCRIPT tab. For every task, a piece of Javascript is executed. Depending on the boolean value of the last expression, the task is visible (in case of `true`) or filtered out (in case of `false`). To help with writing the script you can test the script on an example task and the raw result and the result interpreted as boolean are shown.

Defined variables
-----------------
 
To make the filtering easier, for every task a couple of global vars are defined. You can use these in your code.

* `task`: The full task as string.
* `createdate`: The created date of the task or `null` if not set.
* `completed`: Boolean indicating if the task is completed.
* `completiondate`: The completion date of the task or `null` if not set.
* `due`: The due date or `null` if not set.
* `threshold`: The threshold date or `null` if not set.
* `priority`: The priority of the task as string.
* `recurrence`: The recurrence pattern of the task as string or `null` if not set.
* `tags`: An array of strings with the tags of the task.
* `lists`: An array of strings with the lists of the task.

Example
-------

The following code will show only overdue tasks where tasks without a due date, will never be overdue.

    var result=true;
    if (due!=null) {
        result = new Date() > due;
    }
    result;

Notes
-----

* One run of the filter over all tasks uses a single evaluation context, so any other global state is retained from task to task.
* The script is run for every task when displaying it, so make sure it's fast. Doing too much work in the script will make Simpletask crawl.
* Any ANR reports where the script is set in the filter will have a high chance of being ignored. _With great power comes great responsibility_.