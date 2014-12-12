Simpletask
==========

[Simpletask](https://github.com/mpcjanssen/simpletask-android) is based on the brilliant [todo.txt](http://todotxt.com) by [Gina Trapani](http://ginatrapani.org/). The goal of the application is to provide a tool to do GTD without providing an overwhelming amount of options. Even though Simpletask can be customised by a fairly large amount of settings, the defaults should be sane and require no change.

[Simpletask](http://mpcjanssen.nl/doc/simpletask/) can be used as a very simple todo list manager or as a more complex action manager for GTD or [Manage Your Now](./MYN.md).

Extensions
----------

Simpletask supports the following todo.txt extensions:

-   Due date as `due:YYYY-MM-DD`
-   Start/threshold date as `t:YYYY-MM-DD`
-   Recurrence with `rec:[0-9]+[dwmy]` as described [here](https://github.com/bram85/todo.txt-tools/wiki/Recurrence) but with a twist.
    - By default Simpletask will use the dates in that task to create the recurring task not the date of completion as descibed in the link. This behaviour can be configured from the settings.
    - The format is described by a regular expression, so in words the syntax is `rec:` followd by 1 or more numbers (the `+`) followed by one of `d`ay, `w`eek, `m`onth or `y`ear. For example rec:12d sets up a 12 day recurring task.
- Hidden tasks with `h:1`, this allows dummy tasks with predefined lists and tags so that lists and tags will be available even if the last task with the tag/list is removed from `todo.txt`. These tasks will not be shown by default. You can temporarily display them from the Settings.

Support
-------

If you want to log an issue or feature request for [Simpletask](https://github.com/mpcjanssen/simpletask-android/) you can go to [the tracker](https://github.com/mpcjanssen/simpletask-android/issues). If you find Simpletask useful, you can buy the donate app (see Settings) or donate via [Paypal](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=mpc%2ejanssen%40gmail%2ecom&lc=NL&item_name=mpcjanssen%2enl&item_number=Simpletask&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted) me some beers.

Check the menu for more help sections or click below.

- [User Interface](./ui.md) Help on the user interface.
- [Changelog](./changelog.md)
- [Lists and Tags](./listsandtags.md) Why does Simpletask use lists and Tags instead of the Contexts and Projects from todo.txt?
- [Defined intents](./intents.md) Intents that can be used for automating Simpletask
- [Using Simpletask for 1MTD/MYN](./MYN.md)
- [Filtering with Javascript](./javascript.md)

