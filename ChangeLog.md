# ChangeLog

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
