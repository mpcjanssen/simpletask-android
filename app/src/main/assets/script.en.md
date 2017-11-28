Scripting with Lua
==================

Simpletask embeds a Lua scripting engine that can be used for configuration and callbacks.
The configuration is read whenever you restart the app or when you change it in the Lua Config screen.
The callbacks are executed when certain events occur (such as filtering).
Both the configuration and callbacks will call specific Lua functions, see below for the details of the supported callbacks.

All code (config and callbacks) will be executed in the same Lua interpreter.
This way you can define helper functions in the config and use them in callbacks.

*NB: The filtering callback has been changed from pre 8.0 versions (See below).*

To change existing (app and widget) filters to the 8.0 format do the following.

Add `function onFilter(t,f,e)` before and `end` after the existing script. Prefix all fields with `f.` i.e. `due` -> `f.due`. Example:

    if due~=nil then
        return os.time() >= due;
    end
    --- tasks with no due date are not overdue.
    return false;


becomes

    function onFilter(t,f,e)
        if f.due~=nil then
            return os.time() >= f.due;
        end
        --- tasks with no due date are not overdue.
        return false;
    end


Helper functions
================

### `toast (string) -> nil`

Shows `string` as an Android toast. Useful for debugging scripts.

### Notes

Don't use toasts inside functions. This is a good way to make Simpletask hang.

Callbacks
=========

Most functions get passed the same collection of parameters:

### General parameters ###

* `task`: The task as a string.
* `fields`: Parts of the task converted to different types (such as a timestamp for `createdate`)
    * `completed`: Boolean indicating if the task is completed.
    * `completiondate`: The completion date in seconds of the task or `nil` if not set.
    * `createdate`: The created date  in seconds of the task or `nil` if not set.
    * `due`: The due date in seconds or `nil` if not set.
    * `lists`: A table with the lists of the task as keys. `fields.lists` itself will never be `nil`
    * `priority`: The priority of the task as string.
    * `recurrence`: The recurrence pattern of the task as string or `nil` if not set.
    * `tags`: A table with the tags of the task as keys. `fields.tags` itself will never be `nil`
    * `task`: The full task as string.
    * `threshold`: The threshold date in seconds or `nil` if not set.
    * `text`: The text entered when the task was created.
    * `tokens`: Array of all the tasks tokens (type and text).
* `extensions`: A table with the Todo.txt extensions (`key:val`)of the task as key value pairs. There is only one entry for every key, this is to make use easier. If you need multiple `key:val` pairs with the same key, you can parse the task in Lua.



### `onFilter (task, fields, extensions) -> boolean`

Called for every task as part of filtering the todo list.

### Returns

* `true` if the task should be shown
* `false` if the task should not be shown

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `true`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the `onFilter` function in the filter (not in the configuration). Defining it in the main configuration will not work, if the Filter script is empty, the `onFilter` function will be undefined.

### `onGroup (task, fields, extensions) -> string`

Called for every task as part of filtering the todo list.


### Returns

* The group this task belongs to.

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `""`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the `onGroup` function in the filter (not in the configuration). Defining it in the main configuration will not work, if the Filter script is empty, the `onGroup` function will be undefined.


### `onSort (task, fields, extensions) -> string`

Called for every task as part of sorting the todo list. This function should return a string for every task. This string
is then used to sort the tasks.


### Returns

* The string to use for task sorting.

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `""`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the `onSort` function in the filter (not in the configuration). Defining it in the main configuration will not work, if the Filter script is empty, the `onGroup` function will be undefined.



### `onDisplay (task, fields, extensions) -> string`

Called for every task before it is displayed.

### Returns

* A string which is displayed.

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `""`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the `onDisplay` function in the filter (not in the configuration). Defining it in the main configuration will not work, if the Filter script is empty, the `onDisplay` function will be undefined.

### `onAdd (task, fields, extensions) -> string`

Called for every task before it is added.

### Returns

* A string which will be the actual task that is added.

### Notes

* If there is a Lua error in the callback, the original task text is saved.
* You should define the `onAdd` callback in the main Lua configuration as it is not filter specific. Defining it in a filter will not work.

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `""`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.
* You should define the `onDisplay` function in the filter (not in the configuration). Defining it in the main configuration will not work, if the Filter script is empty, the `onDisplay` function will be undefined.

### `onTextSearch (taskText, searchText, caseSensitive) -> boolean`

Called for every task as when searching for text.

### Parameters

* `taskText`: The task text as it appears in the `todo.txt` file
* `searchText`: Text being searched for
* `caseSensitive`: `true` if case sensitive searching is configured in the settings.

### Returns

* `true` if the task should be shown
* `false` if the task should not be shown

### Notes

* If there is a Lua error in the callback, it behaves as if it had returned `true`
* Considering this function is called a lot (for every task in the list) it should be fast. If it is too slow Simpletask might give ANRs.

Configuration
=============

Configuration is read on app start and whenever it is changed or ran from the Lua Config screen.
Configuration from Lua will always override the value from the settings (Lua wins).

### `theme () -> string`

### Parameters

None

### Returns

* `"dark"` for the Material dark theme
* `"black"` for the black theme (works well on Amoled devices).
* `"light_darkactionbar"` for the Material light theme

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

Show tasks without tags or lists (the GTD Inbox):

    function onFilter(t,f,e)
       return next(f.tags)==nil and next(f.lists)==nil
    end

Show all tasks on the `@errands` list:

    function onFilter(t,f,e)
       return f.lists["errands"]
    end

Change the font size of the main task list to `16sp`:

    function tasklistTextSize()
       return 16.0
    end

The 8.0.0 fuzzy search in Lua:

    function onTextSearch(text, search, case)
        pat = string.gsub(search, ".", "%0.*")
        res = string.match(text, pat)
        return res~=nil
    end


A group callback to group by list with custom empty header:

    function onGroup(t,f,e)
        if not next(f.lists) then
            return "Inbox"
        else
            return next(f.lists)
        end
    end
    
Don't group at all and don't show any headers (regardless of sort order)

    function onGroup()
        return ""
    end

A callback to modify the display of a task:

    function onDisplay(t,f,e)
       if f.due~=nil and os.time() > f.due then
         --- Display overdue tasks in uppercase. (Prefixing with '=' replaces entire task.)
         return "="..string.upper(f.tasktext)
       end
       return f.tasktext
    end

Learning Lua
============

Googling should turn up plenty of good resources. [*Programming in Lua*](https://www.lua.org/pil/contents.html) should cover almost everything.





