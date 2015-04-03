Scripting
=========

Explanation
===========

Simpletask provides (experimental) advanced filtering of tasks using Lua.  To use Lua you will need to enable it from the settings.

After enabling Lua, the filter activity will show an additional SCRIPT tab.  For every task, a piece of Lua is executed.  Depending on the boolean value of the return statement, the task is visible (in case of `true`) or filtered out (in case of `false`).  To help with writing the script you can test the script on an example task and the raw result and the result interpreted as boolean are shown.

Defined variables
-----------------
 
To make the filtering easier, for every task a couple of global vars are defined.  You can use these in your code.

* `completed`: Boolean indicating if the task is completed.
* `completiondate`: The completion date in seconds of the task or `nil` if not set.
* `createdate`: The created date  in secondsof the task or `nil` if not set.
* `due`: The due date in seconds or `nil` if not set.
* `lists`: An array of strings with the lists of the task.
* `priority`: The priority of the task as string.
* `recurrence`: The recurrence pattern of the task as string or `nil` if not set.
* `tags`: An array of strings with the tags of the task.
* `task`: The full task as string.
* `threshold`: The threshold date in seconds or `nil` if not set.

Example
-------

The following code will show only overdue tasks where tasks without a due date, will never be overdue.

    if (due~=nil) {
         return os.time() > due;
    }
    --- tasks with no due date are not overdue.
    return false;

One run of the filter over all tasks uses a single evaluation context, so any other global state is retained from task to task.  This allows some "interesting" things like showing the first hundred tasks:

    if c == nil then
        c = 0
    end
    c = c + 1; 
    return c <= 100;

Notes
-----

* The script is run for every task when displaying it, so make sure it's fast.  Doing too much work in the script will make Simpletask crawl.
* Any ANR reports where the script is set in the filter will have a high chance of being ignored.  _With great power comes great responsibility_.


What next?
----------

This explanation should be enough to get you started.





