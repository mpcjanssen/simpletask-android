Todo.txt Extensions
-------------------

Simpletask supports the following todo.txt extensions:

-   Due date as `due:YYYY-MM-DD`
-   Start/threshold date as `t:YYYY-MM-DD`
-   Recurrence with `rec:\+?[0-9]+[dwmyb]` as described [here](https://github.com/bram85/topydo/wiki/Recurrence) but with a twist.
    - By default Simpletask will use the date of completion for recurring as described in the link. However if the rec includes a plus (e.g. `rec:+2w`), the date is determined from the original due or threshold date..
    - `rec:1b` will recur after 1 weekday (mnemonic *b*usiness-day). 
    - The format is described by a regular expression, so in words the syntax is `rec:` followed by an optional `+` then 1 or more numbers and then followed by one of `d`ay, `w`eek, `m`onth or `y`ear. For example `rec:12d` sets up a 12 day recurring task.
- Hidden tasks with `h:1`, this allows dummy tasks with predefined lists and tags so that lists and tags will be available even if the last task with the tag/list is removed from `todo.txt`. These tasks will not be shown by default. You can temporarily display them from the Settings.