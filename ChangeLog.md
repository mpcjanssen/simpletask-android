# ChangeLog

### 3.2.0

* [UI] Show tasks in widget as they are displayed in the main screen. One line view is still available as setting.

### 3.1.0

* [Feature] New setting for recurring to use the original due or threshold date instead of today. (Default is true)

### 3.0.12

* [Feature] Also change threshold date for the new task when completing a recurring task.

### 3.0.11

* [Bugfix] Fixed issue with recurring tasks if auto archive is active.

### 3.0.10

* [UI] Remove spurious padding of widgets.
* [UI] Use more space for navigation drawers.
* [BugFix] Keep priority on recurring tasks.

### 3.0.9

* [UI] Make extended left drawer configurable.

### 3.0.8

* [UI] Show/Hide completed and future tasks from left navigation drawer.

### 3.0.7

* [UI] Improve relative date display around month boundaries. 30 sep - 1 oct is 1 day not 1 month.

### 3.0.6

* [Feature] Replace existing due and threshold dates in Add Task screen, also prevents duplication caused by Android DatePicker bug http://code.google.com/p/android/issues/detail?id=34860.

### 3.0.5

* [Feature] Back button configuration to apply filter.
* [Bugfix] Don't reset 'Other' filters when clearing filter.

### 3.0.4

* [UI] Redid defer dialogs to require only one click.
* [Feature] Setting to save todos when pressing back key from Add Task screen.

### 3.0.3

* [Bugfix] Fix widget filters using inverted List filters.
* [Bugfix] Track file events on correct path after opening a different todo file.

### 3.0.2

* [Bugfix] [Cloudless] Fix FC on start.

### 3.0.1

* [Bugfix] Fix FCs when trying to open another todo file.
* [Feature] [Dropbox] Add setting for automatic sync when opening app.

### 3.0.0

* [Feature] Enable switching of todo files (Menu->Open todo file).

### 2.9.1

* [Bugfix] Make the todo.txt extensions case insensitive, e.g. Due: or due: or DUE: now all work
* [UI] Make use of the Split Action Bar configurable to have either easily reachable buttons or more screen real estate.
* [Bugfix] Don't add empty tasks from Add Task screen.

### 2.9.0

* [Feature] Set due and threshold date for selected tasks from main screen.
* [Feature] Insert due or threshold date from Add Task screen.
* [UI] Updated Add Task screen.
* [Feature] Create recurring tasks with the rec:[0-9]+[mwd] format
  [http://github.com/bram85/todo.txt-tools/wiki/Recurrence](http://github.com/bram85/todo.txt-tools/wiki/Recurrence)
* [Feature] Removed setting for deferable due date, both due date and threshold
  date can be set and defered from the main menu now so this setting is not
  needed anymore.

### 2.8.2

* [Feature] Allow 1x1 widget size.
* [Feature] Filter completed tasks and tasks with threshold date in future.
  1MTD/MYN is fully supported now.

### 2.8.1

* [Bugfix] Solved issue which could lead to Dropbox login loops.

### 2.8.0

* [UI] Use long click to start drag and drop in sort screen. Old arrows can
  still be enabled in settings.

### 2.7.11

* [Bugfix] Fix FC in share task logging.

### 2.7.10

* [Bugfix] Fix FC in add task screen.
* [UI] Split drawers on tablet landscape to better use space.

### 2.7.9

* [Bugfix] Fix coloring of tasks if it contains creation, due or threshold date.

### 2.7.8

* [Feature] Display due and threshold dates below task. Due dates can be colored (setting).
* [Bugfix] Removed work offline option, you should at least log in into dropbox once. If that's not wanted, then use Simpletask Cloudless.
* [Bugfix] Show warning when logging out of dropbox that unsaved changes will be lost.
* [Bugfix] Don't prefill new task when filter is inverted.
* [Feature] Quick access to filter and sort from actionbar.

### 2.7.7

* [Bugfix] Fixed crash when installing for the first time.

### 2.7.6

* [Feature] Updates to intent handling for easier automation with tasker or am shell scripts. See website for documentation.
* [Bugfix] Clean up widget configuration when removing a widget from the homescreen.


### 2.7.5
* [Bugfix] Fix issue with changing widget theme show "Loading" or nothing at all after switching
* [Code] Refactored Filter handling in a separate class
* [Code] Change detection of newline in todo.txt
* [Bugfix] Do not trim whitespace from tasks

### 2.7.4

* [Bugfix] Explicitly set task reminder start date to prevent 1970 tasks.
* [Bugfix] Reinitialize due and threshold date after updating a task. This fixes weird sort and defer issues.
* [Bugfix] Allow adding tasks while updating an existing task and use same enter behaviour as with Add Task.


### 2.7.3

* [Feature] Add checkbox when adding multiple tasks to copy tags and lists from the previous line.
* [Feature] Better handling of {Enter} in the Add Task screen. It will always insert a new line regardless of position in the current line.
* [Feature] Add Intent to create task for automation tools such as tasker see http://goo.gl/v3tr2D
* [Bugfix] Make application intents package specific so you can install different simpletask versions at the same time.
* [Build] Integrate cloudless build so all versions are based on same source code
* [Feature] Add Archive to context menu so you don't have to go to preferences to archive your tasks
* [UI] Changed complete icons to avoid confusion with CAB dismiss

### 2.7.2

* [Bugfix] Don't crash while demo-ing navigation drawers.

### 2.7.1

* [Feature] Added black theme for widgets. Widget and app theme can be configured seperately/
* [Code] Remove custom font size deltas, it kills perfomance (and thus battery). Will be re-added if there is a better way.

### 2.7.0

* [Feature] Support for a Holo Dark theme. Can be configured from the Preferences.
* [Feature] Added grouping by threshold date and priority.
* [Feature] Demonstrate Navigation drawers on first run.
* [Bugfix] Properly initialize side drawes after first sync with Dropbox.
* [Bugfix] Do not reset preferences to default after logging out of Dropbox and logging in again.
* [Bugfix] Fixed some sorting issues caused by bug in Alphabetical sort.
* [Code] Refactored header functionality so it will be easier to add new groupings.


### 2.6.10

* [Bugfix] Fix issues with widgets where the PendingIntents were not correctly filled. This cause the title click and + click to misbehave.

### 2.6.8

* [Bugfix] Refresh the task view when updating task(s) through the drawer.


### 2.6.7

* [Feature] Automatically detect the line break used when opening a todo file and make that the default. Your line endings will now stay the same without need to configure anything. If you want to change the used linebreak to windows (\\r\\n) or linux (\\n), you can still do so in the settings.

### 2.6.6

* [Bugfix] Fixed a bug which could lead to duplication of tasks when editing them from Simpletask.

### 2.6.5

* [Feature] Removed the donate button from the free version and created a separate paid version. This also makes Simpletask suitable for [Google Play for Education](http://developer.android.com/distribute/googleplay/edu/index.html).
