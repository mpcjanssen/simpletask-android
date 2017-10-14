Changelog
=========

10.0.5
------

- Greatly improve behaviour in bad connectivity scenarios. Fixes #803.

10.0.4
------

- Sort empty dates last, fixes #800.

10.0.3
------

- Ensure Priority.NONE sorts after all other priorities. Fixes #799.

10.0.2
------

- Always update view after underlying adapter change. Should fix index out of bounds exceptions.

10.0.1
------

- Fix race condition when updating drawers. Fixes #798.


10.0.0
------

- Added a Nexcloud version (daniel). See Simpletask Nextcloud in the Play Store.
- Numerous code refactorings to make writing a new storage backend much easier.
- Fix Nextcloud archiving.
- Shorten the names of the versions with other backends.
- Don't add extra EOLs at end of task list.
- Merge #797 (smichel17), sort before filtering so scripts work as expected.
- Select the task by index in the todolist, not by index in the view. Fixes #733.
- Translation updates (ddsanti, vistausss,


9.1.3
-----

-  Add some padding for "Invert filter". Fixes #792.

9.1.2
-----

- Fix archive if done.txt doesn't exist.

9.1.1
-----

- Upload file even if local version is unknown. Fixes changes not saving to Dropbox (#789).

9.1.0
-----

- Switched to Dropbox v2 API as v1 API was disabled.
- Merged #759 (smichel). Correctly respond when font size and sync threshold change
- Fix error in Lua example, fixes #761.
- Fix typo in Lua scripting doc, fixes #762.
- Use lazy sequences while filtering and sorting.
- Remove Dropbox polling for now until it's clear how to include it with SDK v2.
- Refresh todo list when app comes to foreground.
- Update hint text in AddTask.
- Translated using Weblate (Dutch) (vistausss)
- Translated using Weblate (Japanese) (naofum)
- Translated using Weblate (Spanish) (ddisanti)
- Translated using Weblate (German) (rdnz)
- Translated using Weblate (English) (rdnz)
- Translated using Weblate (Chinese (Simplified) (朱陈锬)
- Translated using Weblate (Portuguese) (luisfsr)
- Merge #768 (abowles). Add Missing strings for translation, fixes #723.
- Merge #770 (daniel). Sync TodoList on adding tasks.
- Merge nextcloud variant (daniel)
- Set prefill from widget filter. Fixes #732.
- Merge #774 (Geehu). Add onDisplay() feature.
- Merge #775 (Geehu). Move filter clear button to right-side of screen.
- Merge #777 (Geehu). If 'Invert filter' is active, do not include list or tag when creating a new task.
- Merge #779 (Geehu). Ensure "Create is threshold" remains checked after switching away from page.
- Merge #781 (Geehu). Observe theme on Help pages.
- Lua, added tokens in the field parameters and a `print_table` diagnostic function for use in onDisplay.
- Fix offline Dropbox use.
- Log exceptions when saving to Dropbox in the application log.
- Inform the user if a save to Dropbox failed.

9.0.7
-----

- Fix custom font size setting.

9.0.6
-----

- Fix some reported crashes on Google Play.

9.0.5
-----

- Fix crash if no datebar is shown.

9.0.4
-----

- Fix crash with filter bar.

9.0.3
-----

- Merged: Added French translation (lmsteffan).
- Merged: Added Russian translation (ksabee).
- Merged: Added Ukranian translation (autumnus.v).
- Fix `root://` links on N and provide proper mime-type.
- Target API23 to prevent issues with `file://` links.
- Use Kotlin Android extension to access views.
- Merged: Use diffing algorithm when syncing calendar. Should improve stability and perfromance (#752 vojtechkral)
- Split new list and tags on whitespace. Fixes #750.
- Don't show filter bar if there is no `onFilter` callback.
- Don't prefill tasks if the only filtering is for no tags or lists. Fixes #753.
- Change launch screen background to black. Makes #749 less obvious.
- Added German translation of the help files courtesy of doranism.
- Merged: Added Chines translation (tangenters).
- Merged: Updated Spanish translation (ddisanti).

9.0.2
-----

- Merged: Updated Japanese translation (naofum).
- Merged: Updated Dutch translation (vistausss).
- Shallow copy the todolist before filter or looping over it. Prevents Concurrent Modification Exceptions.
- Merged: Fix README headers. (smichel17)
- Clear tasks to edit when closing AddTask. Fixes #738.
- Allow forced refresh for cloudless as workaround for #735.
- Merged: Fix link in README. (rodrigoaguilera)
- Fixed auto archive issues.
- Reorganized sources to standard.
- Merged: clean up phrasing of Color preference (smichel)
- Merged: Add buttons to get the app (Poussinou)


9.0.1
-----

- Merged: Updated Portuguese translation (luisfsr)
- Merged: Updated German translation (Thomas Radünz)
- Always update cache when saving todo list. Fixes #726.
- `needRefresh` should be true if the todo.txt doesn't exist. Fixes #729.
- Pass a pre-fill string to the AddTask activity instead of the active filter. Fixes #728.

9.0.0
-----

- Use overflow for task context menu on smaller devices. Fixes #686.
- Replace markdown rendering library with much faster version.
- Add background tag by default for better discoverability of the feature.
- Remove edit on share, use background tag instead and edit from main UI.
- Many small bug and perfomance fixes.
- Always add the tag or list from the textbox. Fixes #708.
- Use consistent list and tag sort in drawers and menus. Fixes #239.
- Restore completion checkbox setting. Fixes #526.
- Save recurred tasks when completing. Fixes #689.
- Use indeterminate images in task context bar to fix light theme checkboxes. Fixes #690.
- Make a setting to keep selection after changes.
- Don't clear selection after changes.
- Don't use a database backend for caching the todos. It complicates the code.
- Reduce spacing in drawes. Fixes #703.
- Updated translations from weblate (Many thanks to: Michal Čihař, naofum, David Di Santi, wldmr, amjr, Heimen Stoffels, luisfsr)


8.3.1
-----

- Add testing of Lua callbacks besides `onFilter`.
- Merged: Code and changes and UI tweaks. (smichel17)
- Merged: AddTask cursor placement consistency (smichel17). Fixes #651.
- Reverted, show selected tasks regardless of filter until a proper way to implement it is done.
- Changed behavior of completion checkbox with multiple tasks selected.
- Select tasks when clicking on widget. Fixes #681.

8.3.0
-----

- Removed tablet view for now. It needs a complete redesign.
- Lots of UI changes. Many thanks to smichel17 for implementing them. 
- Don't refilter task list when closing drawers or selection for #568.
- Close filter drawer after selecting an item.
- Close drawers when pressing back.
- Updated icons and menus (smichel17)
- Reorganized some settings (smichel17)
- Allow changing task capitalization and hint visibility from AddTask screen. (smichel17)
- Locale-aware sort order. fixes #584 (smichel17)
- add lua config import/export for #601 (smichel17)
- Redraw list when selection mode closes. Fixes #582.
- Fallback to `en` help assets if not translation exists. Fixes #606.
- Code cleanup (smichel17)

8.2.3
-----

- Merged: Major UI updates. (smichel17)
- Initially hide filter bar so no `X` is displayed on first start.
- Merged: Fix typo. (#529 rutsky)
- Add `onGroup` filter callback for #516, fixes #284.
- Keep selected items visible regardless of filter. Fixes #43.
- Merged: Fix grouping when `Create is threshold` is selected. Fixes #445. (#540 djibux)
- Fix build. (mailed patch from LukeG)
- Allow simultaneous installation of release and debug builds (smichel17) 


8.2.2
-----

- Add Lua config `Share` button.
- Proper black theme on emulator and older devices. Fixes #524.
- Merged: Add scrollbar in Lua config. Fixes #522. (#525 smichel17)
- Commit updated items in the DB.

8.2.1
-----

- Save scroll position of tasklist when editing, rotating or going to the background. Fixes #520.
- Update completed items in the DB. Fixes #521.

8.2.0
-----

- Use a single thread for logging. Reduces memory usage.
- Switch to database backed internal tasklist storage. Fixes Concurrent Modification exceptions.
- Restore calendar option for date entry setting on older devices.
- All Lua is run in a single interpreter again.
- Implement `extension` parameter for `onFilter` callback.
- Close right drawer when pressing Up navigation. Fixes #505.
- Don't crash when trying to open URIs from tasks without any application handling the URI.
- Reduce Lua filter logging.

8.1.2
-----

- Fix Lua interpreter initialisation.

8.1.1
-----

- Don't recreate activities when changing font size preference, fixes #441.
- Refactored selection and moves some menus to actionbar. Fixes #481.
- Always show checkboxes for code simplification and fuller toolbar.
- Always show complete and uncomplete context actions (in menu) for Select all -> (un)complete use case.
- Set the proper toolbar popup theme. Fixes #463.
- Create new tasks with a proper line number for file order sorting. Fixes #499.
- Use a separate Lua interpreter for every filter, fixes #502.
- Recreate widgets if filter changed to updated filterbar click intent.
- Code cleanup and reduced memory usage.
- Sort items by line before saving.

8.1.0
-----

- Merged updated German translation. (#490 twickr).
- Add all task list activities (modification and reading) in the todoQueue to prevent race conditions.
- Add Todo History in the main menu.
- Add link to scripting help from Lua Config screen for #488.
- Add black theme for application.
- Restore last active filter tab.
- Greatly improve selection performance. Fixes #491.
- When selection active, updateCache the selected state of task when closing it with the back button. Fixes #491.
- Removed last visual task updateCache artifacts. Fixes #474.

8.0.6
-----

- Update the widget after reconfiguring the filter.

8.0.5
-----

- Restore task list font size in Settings.
- Remove unimplemented font selector.
- Run widget scripting code in a separate context to prevent interference between apps and widgets. Fixes #486.
- Allow reconfiguration of widgets. Fixes #90.
- Fix crash with reversed sort for non MM devices.
- Add explanation to change 7.x to 8.0 Lua filters. See scripting help.
- Handle selected state of a task in the ListView adapter.
- Switched to RecyclerView. Fixes erratic completion feedback #474 and improves selection smoothness.
- Executing actions always requires a long click.

8.0.4
-----

- Tagged only FDroid release.

8.0.3
-----

- Actually load Lua script from filter. Fixes #479.

8.0.2
-----

- Restore old (non fuzzy) text search.
- Add text filter Lua callback `onTextFilter` and the 8.0.0 fuzzy search as an example script.

8.0.1
-----

- Replace corrupted Cloudless PlayStore build, it crashed when opening filter.

8.0.0
-----

- **INCOMPATIBLE CHANGE**: Define Lua scripts as callbacks for #349.
- **INCOMPATIBLE CHANGE**: Represent lists and tags as Lua table keys instead of Lua arrays for #349.
- **INCOMPATIBLE CHANGE**: Removed text size from settings. It's much easier and more flexible to set it from Lua.

- Added application wide configuration using Lua. Fixes #349. See help for details.

- Re-use filter export format (JSON) to store active filter in preferences. This will reset the filter after the updateCache.
- Recreate activity when some changing preferences, fixes #435.
- Sort checked tags and lists to top when updating. Implements #180.
- Added a font size between large and huge.
- Merged calendar fixes (#436 vojtechkral)
- Calendar sync: Filter out irrelevant task tokens for #369.
- Calendar sync: Fix error handling #398.
- Moved created as threshold setting from preferences to active filter.
- Performance improvements.
- Fixed some memory leaks.
- Added a setting for relative datebar size.
- Filter and sort in a background thread. Makes a big performance difference.
- Improved task filtering visual feedback.
- Open Donate URL with Intent so it also works without internet permissions on cloudless.
- Add checkbox to enable or disable filter script, prevent clearing script when clearing filter.
- Merged settings updateCache (#442 smichel17)
- Improve cloudless file handling logging
- Updated Kotlin to 1.0.3.
- Proper logging ordering in debug info.
- Use due date as default reminder date. Fixes #456.
- Fix crash in Font code.
- Add fuzzy search, fixes #459.
- Fix drawers background to match current theme (matheusdev #464)
- Restart browse from root on error, fixes #455.
- Only log application start after DB logger is setup.
- Add #simpletask on Freenode link in help.
- Show changelog on first launch of a new version.
- Select task added in background if "Edit background" is enabled. Fixes #473"
- Don't count empty lines in total task count. Fixes #469.
- Updated add/save task icon.

7.2.2
-----

- Fix completely broken updateCache lists and tags when scrolling occurs. Fixes #426.

7.2.1
-----

- Fix bug with updating tasks.

7.2.0
-----

- Don't poll Dropbox when app is in background. Improves battery usage and fixes #424.
- Don't updateCache filter when renaming. Fixes #425.
- Allow toggle back to indeterminate state when changing tags or lists. Fixes #405.
- Reorganized settings thanks to smichel17. Fixes #401.
- Updated icons to new material colors and sizes.
- Moved checkboxes in AddTask to overflow menu. Fixes #409.
- Removed `Back saves` setting. Back always saves. The new left corner icon can be used to cancel. Fixes #389.
- Show current selected values in settings. Fixes #404.


7.1.1
-----

- Updated to Kotlin 1.0.1.
- Export and import saved filters in `saved_filters.txt` in the same directory as the `todo.txt` file. Fixes #101.
- Move Floating Action Button in AddTask to fix #397.
- Move version information to Debug screen. Fixes #403.

7.1.0
-----

- Use proper tri-state checkbox instead of colored radiobuttons. Fixes #395.

7.0.16
------

- Merged: Updated German translation (twckr #422).
- Add tristate buttons for lists and tags selection. Fixes #149.
- Indicate connectivity and Dropbox pending changes with UI elements instead of chatty toasts. Implements #388.
- Add a task with a Floating Action Button. Implements #385.
- Allow linking to any shareable content. Implements #393.

7.0.15
------

- Copy the todo item collection when modifying to prevent `ConcurrentModification` exceptions.

7.0.14
------

- Add support for `rec:..b` to recur by weekdays.
- Fill `recurrence` variable in Lua as documented. Fixes #371.
- Added support for local `file://` links. Fixes #376.
- Merged: Updated German translation (twckr #375).
- Don't trim whitespace of tasks to retain indentation.
- Hopefully fixed crashes on some Samsung 4.2.2 devices.
- Fix crash when adding tags or list to an empty task in the AddTask screen.
- Merged: Initial Polish translation (GarciaPL #377).

7.0.13
------

- Don't make full task list available in Lua as an array. Improves performance.
- Fix bug with threshold and due date dialog in Add Task screen. Fixes #367.

7.0.12
------

- Log uncaught exceptions in database before crashing.
- Fix crash when adding tags/lists in empty addtask edit box. Fixes #361

7.0.11
------

- New version to replace corrupt 7.0.10 google play upload.

7.0.10
------

- Fix toast typo, fixes #360.
- Fix tablet mode crash.
- Move `show hidden` option to filter. As a result `show empty headers` is removed. Fixes #190.
- Add item count to headers. Fixes #30.
- Add setting to remove date bar from main view. Workaround for #331.

7.0.9
-----

- Fix crash with manual refresh.

7.0.8
-----

- Merged: Updated German translation (twckr #356).
- Don't calculate the current date for every tasks. Greatly improves performance. Fixes #359.
- Several Kotlin transition related bug fixes. 


7.0.7
-----

- Don't fail archive if done.txt doesn't exist yet. Fixes #353.
- Ask for Storage permission on M. Fixes #340.

7.0.6
-----

- Fix off by one errors when editing priority from the edit screen. Fixes #352.
- Don't show cancel button in Edit task screen to prevent accidental data loss. Cancel is now in the menu.

7.0.5
-----

- Save changes in threshold and due date.

7.0.4
-----

- Save changes made when editing tasks.

7.0.3
-----

- Fix ArrayOutOfBounds if list is empty.

7.0.2
-----

- Complete task before auto-archiving. Fixes #347.
- Defer both due and threshold date when recurring.

7.0.1
-----

- Save changes to tasks. Fixes #346.

7.0.0
-----

- Keep task list in a database.
- Fix issue with task count being one off.
- Fix task selection from widget.
- Ask for Calendar permissions on M if needed. Fixes #334.

6.3.7
-----

- Revert to old default location for Cloudless. People are losing their tasks.
- Revert to old name for Cloudlesss. Renaming seems to give problems with Google Play updates.

6.3.6
-----

- Don't crash with cloudless on older Android versions.

6.3.5
-----

- Fix bug in pending changes handling for Dropbox, which could lead to data loss.
- Show a small red bar if there are unsaved changes.
- Name both versions Simpletask. Simpletask Cloudless looks ugly in a launcher.
- Add a setting to use the `created date` when sorting by threshold date and the threshold date is empty.
- Extend Dropbox polling timeout. Should reduce battery usage.
- Don't change create date on updated tasks, fixes #337.
- Refresh views and clear logs at midnight. Fixes #93.
- Merged: Completed German translation (twckr #335).
- Remove partial `Select parts` functionality when sharing tasks for now.


6.3.4
-----

- Fix issue with writing each line of the todo file to the todo history on Cloudless.
- Much improved loading performance for Cloudless because of above fix.

6.3.3
-----

- Restore cursor position when modifying the priority or due/threshold date. Fixes #329.
- Fix crash in threshold date sort.
- Merged: Update German translations (twckr #333).

6.3.2
-----

- Don't crash if todolist not initialized (for instance when launching from widget and Simpletask is not running)
- Revert use of create date as threshold date, it had unintended consequences in the UI.

6.3.1
-----

- Fix crash when trying to add tags or lists.

6.3.0
-----

- Use create date as threshold if threshold date is not set.
- Merged: Don't crash if no calendar is available (vojtechkral #326). Fixes #324.
- Add grouping by due date. Fixes #322.
- Merged: Don't prefix event titles in calendar (vojtechkral #323).
- Parse #xxx as a github issue in changelog.
- Merged: Updated German translations (twckr #325).
- Fix some selection bugs and crashes (hopefully).
- Switch parts of the code to Kotlin for better null safety and less verbose code.
- Removed Guava dependency and replace with kotlin code.
- Merged: Refactored calendar sync error handling (vojtechkral #330)


6.2.0
-----

- Try to force FDroid rebuild again.
- Keep enclosing whitespace when updating or adding tasks. Partial fix for #315.

6.1.2
-----

- Try to force FDroid rebuild.

6.1.1
-----

- Fix build error caused by duplicate strings.

6.1.0
-----

- Merged use of all day events for calendar sync (vojtechkral).
- Merged updated settings and default for calendar sync (vojtechkral).
- Give proper focus to SearchView so keyboard pops up. Fixes #318. 
- Add preference for fast scrolling of task view. Fixes #319.
- Merged updated German translation (twckr).


6.0.9
-----

- Always add configured text for tasks added in background.
- Restore the edit after share option. Fixes #292

6.0.8
-----

- Fix crash with `Share all`.
- Add support for Dropbox App Folder permissions.
- Save todo file after acrhive.  Fixes #290

6.0.7
-----

- Fix crash with `select all`.
- Don't log actual task contents in application logging.
- Fix an issue with sharing large todo files.

6.0.6
-----

- Fix some punctuation errors. (Shayne Holmes)
- Don't save todo list after a reloadLuaConfig. This should prevent another case of conflicts on Dropbox.
- Hide last empty header if `show empty headers` is disabled.  Fixes #283
- Fix NPE in broadcast receiver.
- Link to the topydo project rather than the (obsoleted) todo.txt-tools. (Bram Schoenmakers)
- Don't hide FAB, removes required external dependency.
- Properly split multiline texts added in background. Possible fix for #277.
- Don't add empty tasks in the background. Possible fix for #277.


6.0.5
-----

- Leave updated tasks in the original location in the todo file. Fixes #274
- Fix issues with resetting filters after opening filter screen. Fixes #276

6.0.4
-----

- Fix issue with conflicted files when archiving.

6.0.3
-----

- Fix rare crash if Dropbox metadata is not filled when receiving a response.

6.0.2
-----

- Add a setting to hide creation date if this is the only specified date. Fixes #272
- Fix widget colors.
- Add setting to use `context` and `project` instead of `list` and `tag` to match `todo.txt`.  #242


6.0.1
-----

- Re-enable `script` filter tab.

6.0.0
-----

- Material, material, material. Material themes only also on KitKat, Jellybean and Ice Cream Sandwich.
- Always enable script filter support.
- Introduced floating action button.
- View and share application logging (From settings).
- View and clear todo file history database (From settings).
- Improved Dropbox background polling.
- Changed the way adding tasks in the background is handled. You can configure a string to append to these tasks. So for example `+background` will allow easy filtering. 
- Don't scroll filter drawer list if item is checked.
- Use `rec:+` to mean recur from original date instead of a using setting. Fixes #29

5.5.8
-----

- Switched to logback logging for more useful log files from inside the app.
- Revert using back for todo file navigation. Fixes #255.
- Code cleanup.
- Don't clear todolist if reloading a file failed.
- Queue todo list and file system actions to fix some weird race conditions.
- Properly save tasks in the cache if offline. Fixes lost tasks after coming online again.


5.5.7

- Delay re-enabling of Cloudless background listener while saving todo file. Fixes #265

5.5.6
-----

- Only show supported themes for the application and widgets. Fixes #263
- Save defer dates when using calendar picker.

5.5.5
-----

- Save the todolist after changing priorities.
- Properly terminate files with a newline as to mirror todo.txt behavior. Fixes #262

5.5.4
-----

- Add create date to tasks if specified. Fixes #261.
- Don't select header lines. Fixes `Select All` crashes.

5.5.3
-----

- Synchronize Cloudless background threads.
- Save completed tasks if using the menu to complete. Fixes #259.
- Clear selected tasks after adding or updating them.

5.5.2
-----

- Fix issue with tasks being added while device is offline not being written to Dropbox.
- Fix issue with Dropbox chnages not being detected after having been offline.
- Show toasts on main thread.
- Add a setting to share the internal backup SQLite database.
- Fix issue with `keep prio`. If unset while completing a recurring taks, the new task had the prio unset instead of the completed task.
- Added huge (36sp) font size.

5.5.1
-----

- Fix setting due and threshold dates.
- Add the new task when completing a task with recurrence, Fixes #258.

5.5.0
-----

- Redid the file loading feedback. If a file is loading the UI is locked.
- Fixed file ordering (I hope for real now).
- Improved connection detection and conflict handling for Dropbox. A `(conflicted file)` is automatically created and opened.
- Keep the list position after modifying tasks.
- Properly refresh the navigation drawer if lists/tags are added or removed.
- Fixed a filter issue with empty Lua filter scripts being treated as false.

- Store last two days of todo.txt version in an internal database. This allows recovery in case of bugs.
- Redid task memory backend. Fixes #252.
- Don't use AsyncTask if not necessary. This make background activity much more robust.
- Switched back to Dropbox Core API. The Sync API is not supported anymore.
- With each file load a new TodoList is created, this fixes some concurrent modification issues.
- Major refactoring for updated performance and easier debugging.
- Show cached todo if Dropbox/device is offline.



5.4.7
-----

- Fix issue with crash in landscape mode if `landscape drawers` is enabled.


5.4.6
-----

- Fix NPE reported through play in updateFilterBar().
- Don't write to todo file if it is still loading. Fixes #251.
- Fix file sort for newly created task, should really fix #248.

5.4.5
-----

- Don't crash when sorting on create date and the file has an invalid create date.

5.4.4
-----

- Fix broken File Order sort #248.
- Fix possible NPE while handling threshold dates.


5.4.3
-----

- Don't save the sorted todo.txt. Save in original order.

5.4.2
-----

- Possible fix for crash with sorting.

5.4.1
-----

- Don't display empty lines in todo.txt as empty tasks.

5.4.0
-----

- Add support for todo://... to link to different todo files.
- Show todo file path in the titlebar (enable in settings).
- Updated back button behavior to switch to previous file if next file was openend via todo://... link.
- When creating a widget pre-fill it with the current active application filter.
- Observe case sensitive sorting setting in all sorts, partial fix for #239.
- Added Spanish translation of help. (courtesy Martin Laclaustra)
- Added Korean translation. (courtesy halcyonera)
- Updated German translation. (courtesy twickr)
- Fixed some issues with calendar reminder sync. (courtesy vojtechkral)
- Support for receiving stream sharing (for example a text file shared from Dropbox).
- Added Material theme for Lollipop devices.
- Fixed issue with appending to done.txt not adding a new line in the beginning.
- Observe split action bar setting in all activities.
- Notes with an action (email, phone-number or link) will trigger a popup when clicked instead of adding a menu item. This makes launching actions easier. Long click always selects.

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

- Only updateCache menu if it is actually initialized. Should fix crash on 5.1.

5.2.14
------

- Fix an issue with multiline shared texts being treated as a single task.
- Add menu item `Refresh` to force manual sync with Dropbox.
- Use proper string resource in new tag dialog. Fixes #179.
- Fix possible NPE reported via Developer Console.
- Prepend date to "Note to self" messages if auto date is active.
- Don't set selection outside of text range. Fixes #148.
- Observe Back saves setting when using up icon. Fixes #185.

5.2.13
------

- Add option to send logs directly from the app. Fixes #174.
- Respect calendar setting when changing dates in AddTask activity. Fixes #145.
- Allow sharing of application log from the settings for better FDroid and debugging support.

5.2.12
------

- Added Italian translation (thanks to Carlo D.)
- Fix link to issue tracker.
- Observe .txt only setting for Cloudless build.  Fixes #154.
- Updated Dropbox API to 3.1.1. Possible fix for Lollipop issues #163.
	
5.2.11
------

- Fix another NPE in the text search code.

5.2.10
------

- Fix a rare NPE in text search.
- Add a setting to remove the prio of completed tasks. Fixes #137.


5.2.9
-----

- Add `-` to tag and list filters. Fixes #135.
- Share text directly if possible. Fixes #134.

5.2.7-8
-------

- Build dependencies updates for FDroid.

5.2.6
-----

- Include VCS revision in version number.
- Documented `h:1` extension in help.
- Don't show create date in simple widget. Fixes #123.
- Better filter description. Fixes #120.
- Expanded Javascript filtering documentation.
- Expose task parser information to Javascript.


5.2.5
-----

- Make use of calendar for setting dates a setting (default off).
- Fix file order sorting for newly added tasks. Fixes #117.
- Make filterbar better suited for small devices.
- Use a ContentProvider for sharing tasks. Fixes #118.

5.2.4
------

- Fix archiving on Dropbox.

5.2.3
-----

- Fix add task bug if "Date new tasks" not selected. Fixes #116.
- Use calendarview to select dates instead of spinners.

5.2.2
-----

- Updated Dropbox Sync API.

5.2.1
-----

- Prevent race conditions for possible fixes of #99.

5.2.0
-----

- (Experimental) Included Javascript engine to allow advanced filtering. See [Javascript](./javascript.md) for usage.
- Don't use `removeAll` in task stores. Fixes #112.
- Return to filter activity when switching to another app and back instead of to the main screen.

5.1.8
-----

- Finish files with EOL on Cloudless. Possible fix for #103
5.1.7
-----

- Updated German translations.
- Fix rec:1y recurrence. Fixes #108.
- Better initial Dropbox sync feedback.

5.1.6
-----

- Back will close the left navigation drawer if it's open. Fixes #100.
- Select current priority when changing a single task's priority.
- Ensure todo file always ends with an EOL. Possible fix for #103.
- Save incremental search and fix search submit. Fixes #104.

5.1.5
-----

- Consider complete task inFileFormat when text searching. Fixes #98.

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
- Don't try to updateCache the widgets if we are not authenticated on Dropbox.

5.0.9
-----

- Don't updateCache UI if loading the file from Dropbox failed. Fixes infinite loops and crashes when not authenticated.

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

-   Big updateCache which should make starting with Simpletask more intuitive:
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

