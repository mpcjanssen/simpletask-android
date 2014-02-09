# simpletask

An Android application based on the brilliant [todo.txt][] by
[Gina Trapani][] and the [Todo.txt community][]
The goal of the application is to provide a tool to do GTD without providing an overwhelming amount of
options. Even though Simpletask can be customised by a fairly large amount of settings, the defaults should be sane and require no change.

You can download the latest versions from the Play Store. Simpletask comes in three version. 

You can look at the [code][] or track changes by the [rss feed][]. If you encounter any issues or want to request new features, you can use the [issue tracker][].

[todo.txt]: http://todotxt.com 
[Gina Trapani]: http://ginatrapani.org 
[Todo.txt community]: http://groups.yahoo.com/group/todotxt/
[code]: ../../timeline
[rss feed]: ../../timeline.rss
[issue tracker]: ../../reportlist

----


##Getting started

###Installing Simpletask

Simpletask can be downloaded from Google Play or from [Fdroid][]

Google Play has 2 different versions:

- [Simpletask][]
- [Simpletask Cloudless][]

Fdroid_ only offers the Cloudless version. The donate versions of the app are exactly the same as the free versions. They offer no additional functionallity and should only be installed if you want to donate me some beer cash. This leaves only 2 distinct versions.


####Simpletask

- Version with built in support for Dropbox syncing. 
- Todo files are stored in application storage so cleaned up on uninstall
- Use if you already have a Dropbox account and want a no hassle install which quickly works.

####Simpletask Cloudless

- No built in sync support. Depends on other offerings (such as Dropsync or Foldersync) to sync to a large number of cloud options.
- Doesn't require INTERNET permission so is acceptable for stringent export
  laws.
- Todo files are stored on sdcard (by default in `data/nl.mpcjanssen.simpletask`) so can also be used in offline mode.
- Will reload the tasklist if an external program changes the todo file.
- Use this if you want to sync to other cloud options than Dropbox or if you want to use it in offline mode.



#Todo.txt Extensions

Even though Simpletask tries to follow the todo.txt spec as closely as possible, it does use the extension mechanism to offer several extensions,

## Due date

Date as `due:YYYY-MM-DD`

## Recurrence

Recurrence with `rec:[0-9]+[dwmy]` as described [here]( https://github.com/bram85/todo.txt-tools/wiki/Recurrence) but with a twist.
   - By default Simpletask will use the dates in that task to create the recurring task not the date of completion as descibed in the link. This behaviour can be configured from the settings.

## Threshold date

Date as `t:YYYY-MM-DD`

The threshold functionality consists of two parts:

1. The actual threshold date
2. The show tasks with threshold date in the future checkbox in the show filter.

If you uncheck this checkbox, any task with a threshold date in the future will not be shown. This is very useful if you have tasks which you want to keep, but you also want them out of your face for a while or they will only become active at a certain time.
In that case you will set the threshold date at the specific date in the future and only then will they become visible on your tasklist.

What I do when I see a task which I don't want to keep seeing for a couple of weeks, I will set the threshold date 2 weeks in the future and it's out of my face for that time.

The threshold syntax is based on the todo.txt extension with the same function: https://github.com/ginatrapani/todo.txt-cli/wiki/Todo.sh-Add-on-Directory#wiki-future-tasks

The main reason this is added to Simpletask as well is because it is needed to implement the Manage Your Now method https://www.michaellinenberger.com/1MTDvsMYN.html which uses the "out of your face" approach extensively.

## Hidden tasks

Tasks can be hidden by adding the key value pair: `h:1`. This is useful for making sure there is always a single task with a specific list or tag applied.

For example if all `@@errands` tasks have been completed and deleted or archived, the `@errands` list will not be show in the menus or filters anymore. Adding the task:

`@@errands h:1`

will ensure that the list is always there.

#ChangeLog

See [Changelog](./doc/markdown/Changelog.md)

#Design considerations

-  Should follow the Android Design Guidelines as closely as possible
-  Any valid todo.txt file should be read into the application without issues
-  It should remain simple
-  It should always show an as accurate as possible view of your task list
-  It should be battery efficient


##Why does Simpletask use lists and tags?

###Contexts vs Lists

Instead of the contexts and projects in todo.txt, simpletask calls `@something`
a list and `+something` a tag. Why was this chosen, isn't GTD all about
contexts?

Well no actually. Even though this is not as clearly articulated in the original
Getting Things Done book as it could be, in GTD the main organizing element is
the list. Some of these lists contain next actions for a certain context (e.g.
@computer) but some don't (e.g. Projects or Waiting For).

So even though a lot of the lists in GTD will contain next actions for
a certain context, this doesn't imply that context is the main organizing
element (e.g. Waiting For is not a context)

###Projects vs Tags

Projects are renamed to tags because tags is a more general concept. A tag can
be anything from a project to a person's name. This allows using tasks like:

`@Call +DavidAllen regarding +GTD`

In this case neither of the tags are projects.

#Automate Simpletask with Intents

Simpletask supports a couple of intents which can be used by other applications (e.g. tasker) to create tasks or display lists.

##Create task in background

To create a task in the background, so without showing simpletask, you can use the intent:

* Intent action: `nl.mpcjanssen.simpletask.BACKGROUND_TASK`
* Intent string extra: `task`

the intent will have one extra string `task` which contains the task to be added.

For example to create a task from tasker use the following action:

* Action: nl.mpcjanssen.simpletask.BACKGROUND_TASK
* Cat: Default
* Mime Type: text/*
* Extra: task:\<Task text with possible variables here\> +tasker
* Target: Activity

I like to add the +tasker tag to be able to quickly filter tasks that were created by tasker.

##Open with specific filter

To open Simpletask with a specific filter you can use the intent:

* Intent action: `nl.mpcjanssen.simpletask.START_WITH_FILTER`
* Intent string extra: `CONTEXTS` list of contexts in filter separated by '\\n' or ','
* Intent string extra: `PROJECTS` list of contexts in filter separated by '\\n' or ','
* Intent string extra: `PRIORITIES` list of contexts in filter separated by '\\n' or ','
* Intent boolean extra: `CONTEXTSnot` true to invert the contexts filter.
* Intent boolean extra: `PROJECTSnot` true to invert the projects filter.
* Intent boolean extra: `PRIORITIESnot` true to invert the priorities filter.
* Intent boolean extra: `PRIORITIESnot` true to invert the priorities filter.
* Intent boolean extra: `HIDECOMPLETED` true to hide completed tasks.
* Intent boolean extra: `HIDEFUTURE` true to hide tasks with a threshold date
  in the future.
* Intent string extra: `SORTS` active sort.

###Sorts extra

SORTS contains a comma or '\\n' separated list of sort keys and their direction with a `!` in between.

####Direction

* `+`: Ascending
* `-`: Descending

####Sort keys

See list in [source code](http://mpcjanssen.nl/fossil/simpletask/artifact/ac6b9bf579b8d1a9c23083031852a0fdd81efb75?ln=42-51)

###Example

* The sort `+!completed,+!alphabetical` sorts completed tasks last and then sorts alphabetical.
* The sort `+!completed,-!alphabetical` sorts completed tasks last and then sorts reversed alphabetical.

Due to limitations in Tasker you can only add 2 extras. So instead you can use the am shell command. For example:

`am start -a nl.mpcjanssen.simpletask.START_WITH_FILTER -e SORTS +!completed,+!alphabetical -e PROJECTS project1,project2 -e CONTEXTS @errands,@computer --ez CONTEXTSnot true -c android.intent.category.DEFAULT -S`

The `-S` at the end will ensure the app is properly restarted if it's already visible. However with tasker the `-S` seems not to work. So there try it without.


##How can I configure Simpletask for MYN?

See [MYN](./doc/MYN.md)

## Why shouldn't `Defer Tasks` use the due date?

See [Defer Tasks](./doc/DeferTasks.md)

## Why the switch to Gradle?

See [Gradle](./doc/Gradle.md)

*NB: [Here](http://mpcjanssen.nl/fossil/simpletask) is the main version of this file, use this in case of bad links.*

[Simpletask]: https://play.google.com/store/apps/details?id=nl.mpcjanssen.todotxtholo&hl=en
[Simpletask Cloudless]: https://play.google.com/store/apps/details?id=nl.mpcjanssen.simpletask&hl=en
[Fdroid]: https://f-droid.org/

