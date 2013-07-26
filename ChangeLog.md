# ChangeLog


### 2.7.0

* [Feature] Support for a Holo Dark theme. Can be configured from the Preferences.
* [Feature] Added grouping by threshold date and priority.
* [Feature] Demonstrate Navigation drawers on first run.
* [Bugfix] Properly initialize side drawes after first sync with Dropbox.
* [Bugfix] Do not reset preferences to default after loggin out of Dropbox and logging in again.
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
