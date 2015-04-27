Changelog
=========

5.3.4
-----

- Several calendar sync fixes.
- Added Korean translation. Courtesy halcyonera from Github.

5.3.3
-----

- Prevent NPE while handling `Load from script` filter menu item.

5.3.2
-----

- Don't remind for tasks in the past when enabling calendar sync. Fixes [#222] (https://github.com/mpcjanssen/simpletask-android/issues/222). 
- Prevent NPE while handling `Load from script` filter menu item.

5.3.1
-----

- Allow loading of lua scripts from file. This is only tested for Cloudless.
- Don't create multiple calendars for reminders.
- Updates in German translation courtesy from twckr.
- Added a link in help to gitter chat at http://gitter.im/mpcjanssen/simpletask-android.
- Customisable reminder time for due dates and threshold dates.
- Add a setting to clear the filter when pressing the back button.

5.3.0
-----

- Added calendar sync. Many thanks to Vojtech Kral for implenenting this.
- Switched to Lua for scripting. It's less verbose to write and performs better. Plus it works without having to keep a copy of the source in the simpletask repository.

5.2.18
------

- Fix archive, it should append.

5.2.17
------

- Don't use atomic files for reading, silly dev.

5.2.16
------

- Use atomic file in Cloudless build. Fixes[#205] (https://github.com/mpcjanssen/simpletask-android/issues/205).

5.2.15
------

- Only update menu if it is actually initialized. Should fix crash on 5.1.

5.2.14
------

- Fix an issue with multiline shared texts being treated as a single task.
- Add menu item `Refresh` to force manual sync with Dropbox.
- Use proper string resource in new tag dialog. Fixes [#179](https://github.com/mpcjanssen/simpletask-android/issues/179).
- Fix possible NPE reported via Developer Console.
- Prepend date to "Note to self" messages if auto date is active.
- Don't set selection outside of text range. Fixes [#148](https://github.com/mpcjanssen/simpletask-android/issues/148).
- Observe Back saves setting when using up icon. Fixes [#185](https://github.com/mpcjanssen/simpletask-android/issues/185).

5.2.13
------

- Add option to send logs directly from the app. Fixes [#174](https://github.com/mpcjanssen/simpletask-android/issues/174).
- Respect calendar setting when changing dates in AddTask activity. Fixes [#145](https://github.com/mpcjanssen/simpletask-android/issues/145).
- Allow sharing of application log from the settings for better FDroid and debugging support.

5.2.12
------

- Added Italian translation (thanks to Carlo D.)
- Fix link to issue tracker.
- Observe .txt only setting for Cloudless build.  Fixes [#154](https://github.com/mpcjanssen/simpletask-android/issues/154).
- Updated Dropbox API to 3.1.1. Possible fix for Lollipop issues [#163](https://github.com/mpcjanssen/simpletask-android/issues/163).
	
5.2.11
------

- Fix another NPE in the text search code.

5.2.10
------

- Fix a rare NPE in text search.
- Add a setting to remove the prio of completed tasks. Fixes [#137](https://github.com/mpcjanssen/simpletask-android/issues/137).


5.2.9
-----

- Add `-` to tag and list filters. Fixes [#135](https://github.com/mpcjanssen/simpletask-android/issues/135).
- Share text directly if possible. Fixes [#134](https://github.com/mpcjanssen/simpletask-android/issues/134).

5.2.7-8
-------

- Build dependencies updates for FDroid.

5.2.6
-----

- Include VCS revision in version number.
- Documented `h:1` extension in help.
- Don't show create date in simple widget. Fixes [#123](https://github.com/mpcjanssen/simpletask-android/issues/123).
- Better filter description. Fixes [#120](https://github.com/mpcjanssen/simpletask-android/issues/120).
- Expanded Javascript filtering documentation.
- Expose task parser information to Javascript.


5.2.5
-----

- Make use of calendar for setting dates a setting (default off).
- Fix file order sorting for newly added tasks. Fixes [#117](https://github.com/mpcjanssen/simpletask-android/issues/117).
- Make filterbar better suited for small devices.
- Use a ContentProvider for sharing tasks. Fixes [#118](https://github.com/mpcjanssen/simpletask-android/issues/118).

5.2.4
------

- Fix archiving on Dropbox.

5.2.3
-----

- Fix add task bug if "Date new tasks" not selected. Fixes [#116](https://github.com/mpcjanssen/simpletask-android/issues/116).
- Use calendarview to select dates instead of spinners.

5.2.2
-----

- Updated Dropbox Sync API.

5.2.1
-----

- Prevent race conditions for possible fixes of [#99](https://github.com/mpcjanssen/simpletask-android/issues/99) and [#103](http://github.com/mpcjanssen/simpletask-android/issues/103).

5.2.0
-----

- (Experimental) Included Javascript engine to allow advanced filtering. See [Javascript](./javascript.md) for usage.
- Don't use `removeAll` in task stores. Fixes [#112](https://github.com/mpcjanssen/simpletask-android/issues/112).
- Return to filter activity when switching to another app and back instead of to the main screen.

5.1.8
-----

- Finish files with EOL on Cloudless. Possible fix for [#103](https://github.com/mpcjanssen/simpletask-android/issues/103)
5.1.7
-----

- Updated German translations.
- Fix rec:1y recurrence. Fixes [#108](https://github.com/mpcjanssen/simpletask-android/issues/108).
- Better initial Dropbox sync feedback.

5.1.6
-----

- Back will close the left navigation drawer if it's open. Fixes [#100](https://github.com/mpcjanssen/simpletask-android/issues/100).
- Select current priority when changing a single task's priority.
- Ensure todo file always ends with an EOL. Possible fix for [#103](https://github.com/mpcjanssen/simpletask-android/issues/103).
- Save incremental search and fix search submit. Fixes [#104](https://github.com/mpcjanssen/simpletask-android/issues/104).

5.1.5
-----

- Consider complete task inFileFormat when text searching. Fixes [#98](https://github.com/mpcjanssen/simpletask-android/issues/98).

5.1.4
-----

- Prevent tight loop if initial Dropbox sync fails.
- Improve logging in case of initial Dropbox sync failure.

5.1.2
-----

- Don't allow browsing for files if the initial Dropbox sync hasn't finished.

5.1.1
-----

- Possible fix for ANR when first syncing with Dropbox.
- Fix issue with resetting filter when launching.
- Improve Dropbox syncing feedback.

5.1.0
-----

- Don't pass invalid filenames to Dropbox, should fix crash with archiving on Dropbox.
- Don't try to update the widgets if we are not authenticated on Dropbox.

5.0.9
-----

- Don't update UI if loading the file from Dropbox failed. Fixes infinite loops and crashes when not authenticated.

5.0.8
-----

- More NPE checks.

5.0.7
-----

- More NPE checks in Dropbox backend code.

5.0.6
-----

- More NPE fixes in widget code.

5.0.5
-----

- Fix crash in widget if not logged in to Dropbox.
- Fix occasional crash when updating tasks.
- Observe auto-archive setting.
- Fix task count with "Select all".

5.0.4
-----

- Hide due and threshold dates in extended view (#109).
- Fix issue wit recurrence and due dates (#108).

5.0.3
-----

- Fix issue with deferral not counting from today (#106).

5.0.2
-----

- Fix issue with monitoring of todo file after opening a different one.

5.0.1
-----

- Updated Changelog.
- Removed context sensitive help.

5.0.0
-----

- New icons courtesy of Robert Chudy.
- Performance improvements, performance is snappy with lists with hundreds of tasks.
- Dropbox: switched to Dropbox Sync API. Changes on Dropbox are immediately reflected if you are connected.
- Incremental text search.
- After changing application theme, apply it without restart.
- Updated German translations.

4.2.5
-----

- Don't cache done.txt from Dropbox. This should fix issues with done.txt being overwritten ([#58](http://mpcjanssen.nl/tracker/issues/58)).

4.2.4
-----

- Updated German translations thanks to Christian Orjeda.
- Don't show spurious whitespace when hiding tags or lists (fixes [#50](http://mpcjanssen.nl/tracker/issues/50))

4.2.3
-----

- Added initial German translation thanks to Christian Orjeda.
- Allow selecting all visible tasks from CAB (fixes [#100](http://mpcjanssen.nl/tracker/issues/100))
- Renamed "Next copies" to "Prefill next" which I hope is more intuitive.

4.2.2
-----

- Fix crash with datepicker in AddTask.

4.2.1
-----

- Fix issue with filter reapplying after refresh.
- Added setting for font size.
- Fix NPE with empty widget sort.

4.2.0
-----

- Removed JodaTime and Jsoup dependencies for smaller apk size and better performance.
- Ignore create date and prio with alphabetical sort.
- Fixed tracker url.
- Added donation via play store.(Fixes [#93](http://mpcjanssen.nl/tracker/issues/93))

4.1.19
------

- Fixed issue with sort resetting in Filter activity when switching tabs (Fixes [#89](http://mpcjanssen.nl/tracker/issues/89))

4.1.18
------

- Updated documentation so that donate page opens in desktop view (mobile view doesn't work).
- Added rationale for Lists and Tags in help.
- Improve back button handling when reading help.

4.1.17
------

-   Fixed issue with filter activity not applying.
-   Fixed issue with launching from widget.

4.1.16
------

-   Retain task selection when rotating the device.

4.1.15
------

-   Fix an issue with widgets not refreshing when changing the todo.txt in the background (cloudless only).
-   Fix issues with syncing with dropbox not working.

4.1.14
------

-   Possible fix for truncated done.txt files.
-   Select correct task when opening from widget (Fixes part of \#78).
-   Fix crash when incorrectly calling AddTask intent from for example tasker.

4.1.13
------

-   Scaled down launcher icons to satisfy lint.

4.1.12
------

-   Updated launcher icons thanks to Robert Chudy.

4.1.11
------

-   Document todo.txt extensions on main help page.

4.1.10
------

-   Fix crash with "Archive now" from settings.
-   Moved donation and tracker links to Help screen. (\#75)

4.1.9
-----

-   Possible fix for crash when using Android "Share…" mechanism to add task.
-   Added Holo Light theme.
-   App doesn't have to be restarted for theme change.

4.1.8
-----

-   Added in app documentation.
-   Switched to Android plugin 0.8 (requires gradle 1.10).

4.1.7
-----

-   Added setting to hide the checkboxes for completing tasks.
-   Fix grouping by threshold date.

4.1.6
-----

-   Set correct create date for recurring tasks. Fixes [b5608a1a97](http://mpcjanssen.nl/fossil/simpletask/tktview?name%3Db5608a1a97).
-   Fixed website URL (Thanks Kyle)
-   Fixed cursor position when adding a task with prefilled list or tag.
-   Recognize dates at beginning of task. Fixes [\#46](http://mpcjanssen.nl/tracker/issues/46).

4.1.5
-----

-   Fix crash when removing priority and cursor in last position.

4.1.4
-----

-   Added setting to only display `.txt` files in todo file browser.
-   ChangeLog is viewable from inside the help menu.
-   Add tasks from locksreen widget without unlocking.
-   Setting to add new tasks at beginning of todo.txt.
-   Allow completing and uncompleting of tasks from CAB.
-   Speed up task archiving and possible fix for [\#58](http://mpcjanssen.nl/tracker/issues/58).

4.1.3
-----

-   Removed Dropbox dependency of cloudless version. Only released through FDroid.

4.1.2
-----

-   Archive selected tasks only.
-   Edit multiple tasks at once.
-   Re-added donation link for f-droid users.
-   Changed default wordwrapping in `AddTask` to on.

4.1.1
-----

-   Fix crashes with invalid dates such as `2013-11-31`.
-   Refactored header handling.
-   Setting to hide headers with only hidden tasks.

4.1.0
-----

-   Support hidden tasks (with h:1). This allows persistent list/tags. Tasks can be unhidden from the settings.

4.0.10
------

-   Don't crash on a `++tag` when show tags is unchecked. Fixes [9c5902](http://mpcjanssen.nl/fossil/simpletask/tktview?name=9c5902).

4.0.9
-----

-   Add setting to capitalize tasks.
-   Sort list and tag popups in AddTask screen.
-   Add option to toggle wordwrap when adding tasks.
-   Add filter option to hide tags and lists on task display.

4.0.8
-----

-   When using "Share" with simpletask, just add the task. You can restore the old behavior of showing the edit activity in the settings.

4.0.7
-----

-   Configure widget transparency.

4.0.6
-----

-   Fix rare crash when starting app.

4.0.5
-----

-   Fix recurring tasks.

4.0.4
-----

-   Fix issue with unsorted or duplicate headers.

4.0.3
-----

-   Save text search in saved filter.

4.0.2
-----

-   Revert left drawer to checked views.
-   Allow renaming and updating of saved filters.

4.0.1
-----

-   Fix dark theme for AddTask.

4.0.0
-----

-   Big update which should make starting with Simpletask more intuitive:
-   Merged tag and list navigation drawer into the left drawer.
-   Click tag or list header to invert the filter.
-   Right drawer with favourite filters.
-   Long click saved filter to create homescreen shortcut.
-   Checkboxes in tasklist to quickly complete/uncomplete tasks.
-   Improved tag and list selection dialogs where you can also add new items.
-   Updated priority colors to reflect urgency better.
-   Added 'expert' user settings "hide hints" and "hide confirmation dialogs"
-   Keep priority when completing tasks.
-   Remember last used filter tab.

3.2.3
-----

-   Simplified version numbering.

3.2.2
-----

-   Make landscape mode configurable between fixed and sliding drawers.

3.2.1
-----

-   Fix issues when adding tasks with some soft keyboards such as Swype.

3.2.0
-----

-   Show tasks in widget as they are displayed in the main screen. One line view is still available as setting.

3.1.0
-----

-   New setting for recurring to use the original due or threshold date instead of today. (Default is true)

3.0.12
------

-   Also change threshold date for the new task when completing a recurring task.

3.0.11
------

-   Fixed issue with recurring tasks if auto archive is active.

3.0.10
------

-   Remove spurious padding of widgets.
-   Use more space for navigation drawers.
-   Keep priority on recurring tasks.

3.0.9
-----

-   Make extended left drawer configurable.

3.0.8
-----

-   Show/Hide completed and future tasks from left navigation drawer.

3.0.7
-----

-   Improve relative date display around month boundaries. 30 sep - 1 oct is 1 day not 1 month.

3.0.6
-----

-   Replace existing due and threshold dates in Add Task screen, also prevents duplication caused by Android DatePicker bug <http://code.google.com/p/android/issues/detail?id=34860>.

3.0.5
-----

-   Back button configuration to apply filter.
-   Don't reset `Other` filters when clearing filter.

3.0.4
-----

-   Redid defer dialogs to require only one click.
-   Setting to save todos when pressing back key from Add Task screen.

3.0.3
-----

-   Fix widget filters using inverted List filters.
-   Track file events on correct path after opening a different todo file.

3.0.2
-----

-   Fix FC on start.

3.0.1
-----

-   Fix FCs when trying to open another todo file.
-   Add setting for automatic sync when opening app.

3.0.0
-----

-   Enable switching of todo files `Menu->Open todo file`.

2.9.1
-----

-   Make the todo.txt extensions case insensitive, e.g. `Due:` or `due:` or `DUE:` now all work
-   Make use of the Split Action Bar configurable to have either easily reachable buttons or more screen real estate.
-   Don't add empty tasks from Add Task screen.

2.9.0
-----

-   Set due and threshold date for selected tasks from main screen.
-   Insert due or threshold date from Add Task screen.
-   Updated Add Task screen.
-   Create recurring tasks with the `rec:[0-9]+[mwd]` format. See <http://github.com/bram85/todo.txt-tools/wiki/Recurrence>
-   Removed setting for deferrable due date, both due date and threshold date can be set and deferred from the main menu now so this setting is not needed anymore.

2.8.2
-----

-   Allow 1x1 widget size.
-   Filter completed tasks and tasks with threshold date in future. 1MTD/MYN is fully supported now.

2.8.1
-----

-   Solved issue which could lead to Dropbox login loops.

2.8.0
-----

-   Use long click to start drag and drop in sort screen. Old arrows can still be enabled in settings.

2.7.11
------

-   Fix FC in share task logging.

2.7.10
------

-   Fix FC in add task screen.
-   Split drawers on tablet landscape to better use space.

2.7.9
-----

-   Fix coloring of tasks if it contains creation, due or threshold date.

2.7.8
-----

-   Display due and threshold dates below task. Due dates can be colored (setting).
-   Removed work offline option, you should at least log in into dropbox once. If that's not wanted, then use Simpletask Cloudless.
-   Show warning when logging out of dropbox that unsaved changes will be lost.
-   Don't prefill new task when filter is inverted.
-   Quick access to filter and sort from actionbar.

2.7.7
-----

-   Fixed crash when installing for the first time.

2.7.6
-----

-   Updates to intent handling for easier automation with tasker or am shell scripts. See website for documentation.
-   Clean up widget configuration when removing a widget from the homescreen.

2.7.5
-----

-   Fix issue with changing widget theme show "Loading" or nothing at all after switching.
-   Refactored Filter handling in a separate class.
-   Change detection of newline in todo.txt.
-   Do not trim whitespace from tasks.

2.7.4
-----

-   Explicitly set task reminder start date to prevent 1970 tasks.
-   Reinitialize due and threshold date after updating a task. This fixes weird sort and defer issues.
-   Allow adding tasks while updating an existing task and use same enter behaviour as with Add Task.

2.7.3
-----

-   Add checkbox when adding multiple tasks to copy tags and lists from the previous line.
-   Better handling of {Enter} in the Add Task screen. It will always insert a new line regardless of position in the current line.
-   Add Intent to create task for automation tools such as tasker see [help](intents.md).
-   Make application intents package specific so you can install different simpletask versions at the same time.
-   Integrate cloudless build so all versions are based on same source code
-   Add Archive to context menu so you don't have to go to preferences to archive your tasks
-   Changed complete icons to avoid confusion with CAB dismiss

2.7.2
-----

-   Don't crash while demo-ing navigation drawers.

2.7.1
-----

-   Added black theme for widgets. Widget and app theme can be configured seperately.
-   Remove custom font size deltas, it kills perfomance (and thus battery). Will be re-added if there is a better way.

2.7.0
-----

-   Support for a Holo Dark theme. Can be configured from the Preferences.
-   Added grouping by threshold date and priority.
-   Demonstrate Navigation drawers on first run.
-   Properly initialize side drawes after first sync with Dropbox.
-   Do not reset preferences to default after logging out of Dropbox and logging in again.
-   Fixed some sorting issues caused by bug in Alphabetical sort.
-   Refactored header functionality so it will be easier to add new groupings.

2.6.10
------

-   Fix issues with widgets where the PendingIntents were not correctly filled. This cause the title click and + click to misbehave.

2.6.8
-----

-   Refresh the task view when updating task(s) through the drawer.

2.6.7
-----

-   Automatically detect the line break used when opening a todo file and make that the default. Your line endings will now stay the same without need to configure anything. If you want to change the used linebreak to windows () or linux (), you can still do so in the settings.

2.6.6
-----

-   Fixed a bug which could lead to duplication of tasks when editing them from Simpletask.

2.6.5
-----

-   Removed the donate button from the free version and created a separate paid version. This also makes Simpletask suitable for [Google Play for Education](http://developer.android.com/distribute/googleplay/edu/index.html)

