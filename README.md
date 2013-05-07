# Simpletask

An Android application based on the brilliant [todo.txt](http://todotxt.com) by
[Gina Trapani](http://ginatrapani.org) and [the Todo.txt community](http://groups.yahoo.com/group/todotxt/)

The goal of the application is to provide a tool to do GTD without providing an overwhelming amount of
options.
You can download the latest version for free from [this fossil repository](http://mpcjanssen.nl/cgi-bin/simpletask/doc/trunk/apk/Simpletask.apk)

Design considerations
---------------------

*  Any valid todo.txt file should be read into the application without issues
*  It should remain simple
*  It should always show an as accurate as possible view of your task list
*  It should be battery efficient

Design decisions based on the considerations above
--------------------------------------------------

*  Widgets will not be re-added unless there is a way to fix:
    *  Keeping widgets in sync and accurate requires continuous background syncing which is not nice on the battery
    *  Easy access to a specific filtered list is already provided by homescreen shortcuts (which also survive an app uninstall/reinstall)


