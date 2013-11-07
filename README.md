# Simpletask

An Android application based on the brilliant [todo.txt](http://todotxt.com) by
[Gina Trapani](http://ginatrapani.org) and [the Todo.txt community](http://groups.yahoo.com/group/todotxt/)

The goal of the application is to provide a tool to do GTD without providing an overwhelming amount of
options.
You can download the latest versions from the Play Store. Simpletask comes in three version. You can check the differences between these versions [here](./doc/Versions.md)

To follow code and ticket updates follow [the rss feed](../../timeline.rss).

Extensions
----------

Simpletask supports the following todo.txt extensions:

* Due date as `due:YYYY-MM-DD`
* Start/threshold date as `t:YYYY-MM-DD`
* Recurrence with `rec:[0-9]+[dwmy]` as described [here]( https://github.com/bram85/todo.txt-tools/wiki/Recurrence) but with a twist.
   - By default Simpletask will use the dates in that task to create the recurring task not the date of completion as descibed in the link. This behaviour can be configured from the settings.

ChangeLog
---------------------

See [ChangeLog](./ChangeLog.md)

Design considerations
---------------------

*  Should follow the Android Design Guidelines as closely as possible
*  Any valid todo.txt file should be read into the application without issues
*  It should remain simple
*  It should always show an as accurate as possible view of your task list
*  It should be battery efficient

FAQ
---------------------

#### Why does Simpletask use lists and tags?

See [Lists and Tags](./doc/ListsAndTags.md)

#### Which intents can I use to automate simpletask?

See [Intents](./doc/Intents.md)

#### How can I configure Simpletask for MYN?

See [MYN](./doc/MYN.md)

#### Why shouldn't `Defer Tasks` use the due date?

See [Defer Tasks](./doc/DeferTasks.md)

#### Why the switch to Gradle?

See [Gradle](./doc/Gradle.md)
