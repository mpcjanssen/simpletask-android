Javascript
==========

Explanation for programmers
===========================

Simpletask provides (experimental) advanced filtering of tasks using Javascript. To use Javascript you will need to enable it from the settings.

After enabling Javascript, the filter activity will show an additional SCRIPT tab. For every task, a piece of Javascript is executed. Depending on the boolean value of the last expression, the task is visible (in case of `true`) or filtered out (in case of `false`). To help with writing the script you can test the script on an example task and the raw result and the result interpreted as boolean are shown.

Defined variables
-----------------
 
To make the filtering easier, for every task a couple of global vars are defined. You can use these in your code.

* `completed`: Boolean indicating if the task is completed.
* `completiondate`: The completion date of the task or `null` if not set.
* `createdate`: The created date of the task or `null` if not set.
* `due`: The due date or `null` if not set.
* `lists`: An array of strings with the lists of the task.
* `priority`: The priority of the task as string.
* `recurrence`: The recurrence pattern of the task as string or `null` if not set.
* `tags`: An array of strings with the tags of the task.
* `task`: The full task as string.
* `threshold`: The threshold date or `null` if not set.

Example
-------

The following code will show only overdue tasks where tasks without a due date, will never be overdue.

    var result=true;
    if (due!=null) {
        result = new Date() > due;
    }
    result;

One run of the filter over all tasks uses a single evaluation context, so any other global state is retained from task to task. This allows some "interesting" things like showing the first hundred tasks:

    
    if (c === undefined) { 
        var c = 0; 
    } 
    c++; 
    c <= 100;

Notes
-----

* The script is run for every task when displaying it, so make sure it's fast. Doing too much work in the script will make Simpletask crawl.
* Any ANR reports where the script is set in the filter will have a high chance of being ignored. _With great power comes great responsibility_.


Explanation for mere mortals
============================

How is the script used to filter?
---------------------------------

To start using the Rhino Javascript engine to filter tasks, you will first need to enable it from the Settings menu. Go to Settings->Experimental and Check the "Use Rhino" checkbox. Now from the main screen open the filter activity (press the labels icon) and activate the script tab. If it's not visible you might have to swipe the tab bar to the left.

You now see 2 text fields you can edit (Script and Test task) and two text views you can't edit (result and result as boolean). You can use these fields to define the filter script and to do some testing with it as well.

How does Simpletask use the script to filter tasks? As mentioned above it:

> Depending on the boolean value of the last expression, the task is visible (in case of `true`) or filtered out (in case of `false`). 

What does this mean?  In Javascript, every line of code is an expression (this is a simplification, but it's close enough). Every expression also has a value. For example in the script field add:

    1+1;

Now press the test button and it will show the result `4`.  Another example:

    1==2;

Results in:

    false;

in Javascript any kind of value can be converted to a boolean (true or false). The rules for this are defined in the [ECMA standard](http://www.ecma-international.org/ecma-262/5.1/#sec-9.2).  To make this easier for you, Simpletask shows the second field (result as boolean) which will do this conversion for you. For example in the case of `1+1` the result as boolean was `true`;

Based on this result Simpletask will filter your task list. If it is `true` the task will be included, if it is `false` it will be excluded.  

If you script contains an error, the error will be shown in the results field. And the value will show `error`. If you use this as filter, the result will be assumed to be `false`.

How are the task contents used for filtering?
---------------------------------------------

Of course the examples above were not very useful for filtering because they are constant values. If we want to create a result based on the task contents we will need to use these contents in some way. To get access to the task contents in the script, Simpletask defines some global variables you can use. One of these is the `task` variable which contains the full text of the task. Lets try an example with this. In the script field type:

    task.search("simple")!=-1;

This uses the [search](http://www.ecma-international.org/ecma-262/5.1/#sec-15.5.4.12) method to check if the task contains the text `simple`.  If you now press the Test button, the result will be `false`. This is where the Test task field comes into play. The contents of the test task field are used to execute the script on and determine the results. If you add "simpletask" to this field and press the Test button again, the result will be `true` as expected.

To help you with picking apart the different task elements (such as list, tags, due and threshold dates, etc.), Simpletask will provide several other global variable besides `task` (see above).

What next?
----------

This explanation should be enough to get you started. If you want to use the full power of this feature in Javascript it will help to:

* Learn Javascript: Most tutorials focus on learning Javascript in the browser which is less than helpful for us. To get a good introduction to Javascript the language you could try part 1 of [this book](http://eloquentjavascript.net/).
* Play around and share your scripts: If you have questions or want to share your script, you can do that at the [Simpletask Google+ community](https://plus.google.com/u/0/communities/103696467805364479108?cfem=1).





