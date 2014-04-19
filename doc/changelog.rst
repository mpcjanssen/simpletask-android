Changelog
+++++++++

4.1.12
======

- Updated launcher icons thanks to Robert Chudy.

4.1.11
======

- Document todo.txt extensions on main help page.

4.1.10
======

- Fix crash with "Archive now" from settings.
- Moved donation and tracker links to Help screen. (#75)

4.1.9
=====

-  Possible fix for crash when using Android "Share…" mechanism to add
   task.
-  Added Holo Light theme. 
-  App doesn't have to be restarted for theme change.

4.1.8
=====

-  Added in app documentation.

-  Switched to Android plugin 0.8 (requires gradle 1.10).

4.1.7
=====

-  Added setting to hide the checkboxes for completing tasks.

-  Fix grouping by threshold date.

4.1.6
=====

-  Set correct create date for recurring tasks. Fixes
   `b5608a1a97 <http://mpcjanssen.nl/fossil/simpletask/tktview?name%3Db5608a1a97>`__.

-  Fixed website URL (Thanks Kyle)

-  Fixed cursor position when adding a task with prefilled list or tag.

-  Recognize dates at beginning of task. Fixes
   `#46 <http://mpcjanssen.nl/tracker/issues/46>`__.

4.1.5
=====

-  Fix crash when removing priority and cursor in last position.

4.1.4
=====

-  Added setting to only display ``.txt`` files in todo file browser.

-  ChangeLog is viewable from inside the help menu.

-  Add tasks from locksreen widget without unlocking.

-  Setting to add new tasks at beginning of todo.txt.

-  Allow completing and uncompleting of tasks from CAB.

-  Speed up task archiving and possible fix for
   `#58 <http://mpcjanssen.nl/tracker/issues/58>`__.

4.1.3
=====

-  Removed Dropbox dependency of cloudless version. Only released
   through FDroid.

4.1.2
=====

-  Archive selected tasks only.

-  Edit multiple tasks at once.

-  Re-added donation link for f-droid users.

-  Changed default wordwrapping in ``AddTask`` to on.

4.1.1
=====

-  Fix crashes with invalid dates such as ``2013-11-31``.

-  Refactored header handling.

-  Setting to hide headers with only hidden tasks.

4.1.0
=====

-  Support hidden tasks (with h:1). This allows persistent list/tags.
   Tasks can be unhidden from the settings.

4.0.10
======

-  Don't crash on a ``++tag`` when show tags is unchecked. Fixes
   `9c5902 <http://mpcjanssen.nl/fossil/simpletask/tktview?name=9c5902>`__.

4.0.9
=====

-  Add setting to capitalize tasks.

-  Sort list and tag popups in AddTask screen.

-  Add option to toggle wordwrap when adding tasks.

-  Add filter option to hide tags and lists on task display.

4.0.8
=====

-  When using "Share" with simpletask, just add the task. You can
   restore the old behavior of showing the edit activity in the
   settings.

4.0.7
=====

-  Configure widget transparency.

4.0.6
=====

-  Fix rare crash when starting app.

4.0.5
=====

-  Fix recurring tasks.

4.0.4
=====

-  Fix issue with unsorted or duplicate headers.

4.0.3
=====

-  Save text search in saved filter.

4.0.2
=====

-  Revert left drawer to checked views.

-  Allow renaming and updating of saved filters.

4.0.1
=====

-  Fix dark theme for AddTask.

4.0.0
=====

-  Big update which should make starting with Simpletask more intuitive:

-  Merged tag and list navigation drawer into the left drawer.

-  Click tag or list header to invert the filter.

-  Right drawer with favourite filters.

-  Long click saved filter to create homescreen shortcut.

-  Checkboxes in tasklist to quickly complete/uncomplete tasks.

-  Improved tag and list selection dialogs where you can also add new
   items.

-  Updated priority colors to reflect urgency better.

-  Added 'expert' user settings "hide hints" and "hide confirmation
   dialogs"

-  Keep priority when completing tasks.

-  Remember last used filter tab.

3.2.3
=====

-  Simplified version numbering.

3.2.2
=====

-  Make landscape mode configurable between fixed and sliding drawers.

3.2.1
=====

-  Fix issues when adding tasks with some soft keyboards such as Swype.

3.2.0
=====

-  Show tasks in widget as they are displayed in the main screen. One
   line view is still available as setting.

3.1.0
=====

-  New setting for recurring to use the original due or threshold date
   instead of today. (Default is true)

3.0.12
======

-  Also change threshold date for the new task when completing a
   recurring task.

3.0.11
======

-  Fixed issue with recurring tasks if auto archive is active.

3.0.10
======

-  Remove spurious padding of widgets.

-  Use more space for navigation drawers.

-  Keep priority on recurring tasks.

3.0.9
=====

-  Make extended left drawer configurable.

3.0.8
=====

-  Show/Hide completed and future tasks from left navigation drawer.

3.0.7
=====

-  Improve relative date display around month boundaries. 30 sep - 1 oct
   is 1 day not 1 month.

3.0.6
=====

-  Replace existing due and threshold dates in Add Task screen, also
   prevents duplication caused by Android DatePicker bug
   http://code.google.com/p/android/issues/detail?id=34860.

3.0.5
=====

-  Back button configuration to apply filter.

-  Don't reset ``Other`` filters when clearing filter.

3.0.4
=====

-  Redid defer dialogs to require only one click.

-  Setting to save todos when pressing back key from Add Task screen.

3.0.3
=====

-  Fix widget filters using inverted List filters.

-  Track file events on correct path after opening a different todo
   file.

3.0.2
=====

-  Fix FC on start.

3.0.1
=====

-  Fix FCs when trying to open another todo file.

-  Add setting for automatic sync when opening app.

3.0.0
=====

-  Enable switching of todo files ``Menu->Open todo file``.

2.9.1
=====

-  Make the todo.txt extensions case insensitive, e.g. ``Due:`` or
   ``due:`` or ``DUE:`` now all work

-  Make use of the Split Action Bar configurable to have either easily
   reachable buttons or more screen real estate.

-  Don't add empty tasks from Add Task screen.

2.9.0
=====

-  Set due and threshold date for selected tasks from main screen.

-  Insert due or threshold date from Add Task screen.

-  Updated Add Task screen.

-  Create recurring tasks with the ``rec:[0-9]+[mwd]`` format. See
   http://github.com/bram85/todo.txt-tools/wiki/Recurrence

-  Removed setting for deferrable due date, both due date and threshold
   date can be set and deferred from the main menu now so this setting
   is not needed anymore.

2.8.2
=====

-  Allow 1x1 widget size.

-  Filter completed tasks and tasks with threshold date in future.
   1MTD/MYN is fully supported now.

2.8.1
=====

-  Solved issue which could lead to Dropbox login loops.

2.8.0
=====

-  Use long click to start drag and drop in sort screen. Old arrows can
   still be enabled in settings.

2.7.11
======

-  Fix FC in share task logging.

2.7.10
======

-  Fix FC in add task screen.

-  Split drawers on tablet landscape to better use space.

2.7.9
=====

-  Fix coloring of tasks if it contains creation, due or threshold date.

2.7.8
=====

-  Display due and threshold dates below task. Due dates can be colored
   (setting).

-  Removed work offline option, you should at least log in into dropbox
   once. If that's not wanted, then use Simpletask Cloudless.

-  Show warning when logging out of dropbox that unsaved changes will be
   lost.

-  Don't prefill new task when filter is inverted.

-  Quick access to filter and sort from actionbar.

2.7.7
=====

-  Fixed crash when installing for the first time.

2.7.6
=====

-  Updates to intent handling for easier automation with tasker or am
   shell scripts. See website for documentation.

-  Clean up widget configuration when removing a widget from the
   homescreen.

2.7.5
=====

-  Fix issue with changing widget theme show "Loading" or nothing at all
   after switching.

-  Refactored Filter handling in a separate class.

-  Change detection of newline in todo.txt.

-  Do not trim whitespace from tasks.

2.7.4
=====

-  Explicitly set task reminder start date to prevent 1970 tasks.

-  Reinitialize due and threshold date after updating a task. This fixes
   weird sort and defer issues.

-  Allow adding tasks while updating an existing task and use same enter
   behaviour as with Add Task.

2.7.3
=====

-  Add checkbox when adding multiple tasks to copy tags and lists from
   the previous line.

-  Better handling of {Enter} in the Add Task screen. It will always
   insert a new line regardless of position in the current line.

-  Add Intent to create task for automation tools such as tasker see
   `help <intents.md>`__.

-  Make application intents package specific so you can install
   different simpletask versions at the same time.

-  Integrate cloudless build so all versions are based on same source
   code

-  Add Archive to context menu so you don't have to go to preferences to
   archive your tasks

-  Changed complete icons to avoid confusion with CAB dismiss

2.7.2
=====

-  Don't crash while demo-ing navigation drawers.

2.7.1
=====

-  Added black theme for widgets. Widget and app theme can be configured
   seperately.

-  Remove custom font size deltas, it kills perfomance (and thus
   battery). Will be re-added if there is a better way.

2.7.0
=====

-  Support for a Holo Dark theme. Can be configured from the
   Preferences.

-  Added grouping by threshold date and priority.

-  Demonstrate Navigation drawers on first run.

-  Properly initialize side drawes after first sync with Dropbox.

-  Do not reset preferences to default after logging out of Dropbox and
   logging in again.

-  Fixed some sorting issues caused by bug in Alphabetical sort.

-  Refactored header functionality so it will be easier to add new
   groupings.

2.6.10
======

-  Fix issues with widgets where the PendingIntents were not correctly
   filled. This cause the title click and + click to misbehave.

2.6.8
=====

-  Refresh the task view when updating task(s) through the drawer.

2.6.7
=====

-  Automatically detect the line break used when opening a todo file and
   make that the default. Your line endings will now stay the same
   without need to configure anything. If you want to change the used
   linebreak to windows () or linux (), you can still do so in the
   settings.

2.6.6
=====

-  Fixed a bug which could lead to duplication of tasks when editing
   them from Simpletask.

2.6.5
=====

-  Removed the donate button from the free version and created a
   separate paid version. This also makes Simpletask suitable for
   `Google Play for
   Education <http://developer.android.com/distribute/googleplay/edu/index.html>`__


