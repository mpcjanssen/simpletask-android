Scripting with Lua
==================

Simpletask embeds a Lua scripting engine that can be used for configuration and callbacks.
The configuration is read whenever you restart the app or when you change it in the Lua Config screen.
The callbacks are executed when certain events occur (such as filtering).
Both the configuration and callbacks will call specific Lua functions, see below for the details of the supported callbacks.

All code (config and callbacks) will be executed in the same Lua interpreter.
This way you can define helper functions in the config and use them in callbacks.

*NB: The filtering callback has been changed from pre 8.0 versions (See below).*


Helper functions
================

### `toast (string) -> nil`

Shows `string` as an Android toast. Useful for debugging scripts.

Callbacks
=========


### `onFilter (task, fields, extensions) -> boolean`

Called for every task as part of filtering the todo list.

### Parameters

* `task`: The task as a string.
* `fields`:
   * `completed`: Boolean indicating if the task is completed.
   * `completiondate`: The completion date in seconds of the task or `nil` if not set.
   * `createdate`: The created date  in seconds of the task or `nil` if not set.
   * `due`: The due date in seconds or `nil` if not set.
   * `lists`: A table with the lists of the task as keys. `fields.lits` itself will never be `nil`
   * `priority`: The priority of the task as string.
   * `recurrence`: The recurrence pattern of the task as string or `nil` if not set.
   * `tags`: A table with the tags of the task as keys. `fields.tags` itself will never be `nil`
   * `task`: The full task as string.
   * `threshold`: The threshold date in seconds or `nil` if not set.
* `extensions`: A table with the Todo.txt extensions (`key:val`)of the task as key value pairs. **NOT IMPLEMENTED YET**

### Returns

* `true` if the task should be shown
* `false` if the task should not be shown

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `true`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the onFilter function in the filter (not in the configuration).
Defining it in the main configuration will work, but then the Lua script will be the same for all
filters.


Configuration
=============

Configuration is read on app start and whenever it is changed or ran from the Lua Config screen.

### `theme () -> string`

### Parameters

None

### Returns

* `"dark"` for the dark theme
* `"light_darkactionbar"` if the task should not be shown

### Notes

* Requires an application restart to take effect (more accurately it needs to recreate the activity)

### `tasklistTextSize () -> float`

### Parameters

None

### Returns

The font size of the main task list in SP as a float.

### Notes

* Requires an application restart to take effect (more accurately it needs to recreate the activity)
* The default size in Android at the moment is `14sp`

Examples
========

The following code will show only overdue tasks where tasks without a due date, will never be overdue.

    function onFilter(t,f,e)
       if f.due~=nil then
           return os.time() > f.due;
       end
       --- tasks with no due date are not overdue.
       return false;
    end

Show all tasks with the `@errands` tag:

    function onFilter(t,f,e)
       return f.tags["@errands"]
    end


Learning Lua
============

Googling should turn up plenty of good resources. [*Programming in Lua*](https://www.lua.org/pil/contents.html) should cover almost everything.





