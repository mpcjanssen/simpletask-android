[simpletask](../index.md) / [nl.mpcjanssen.simpletask.util](.)

## Package nl.mpcjanssen.simpletask.util

### Types

| [ActionQueue](-action-queue/index.md) | `object ActionQueue : `[`Thread`](http://docs.oracle.com/javase/6/docs/api/java/lang/Thread.html) |
| [Config](-config/index.md) | `object Config : OnSharedPreferenceChangeListener` |
| [FontManager](-font-manager/index.md) | `open class FontManager` |
| [FontPreference](-font-preference/index.md) | `open class FontPreference : DialogPreference, OnClickListener` |
| [InputDialogListener](-input-dialog-listener/index.md) | `interface InputDialogListener` |
| [ListenerList](-listener-list/index.md) | `class ListenerList<L>` |
| [LoggingRunnable](-logging-runnable/index.md) | `class LoggingRunnable : `[`Runnable`](http://docs.oracle.com/javase/6/docs/api/java/lang/Runnable.html) |

### Extensions for External Classes

| [java.util.ArrayList](java.util.-array-list/index.md) |  |
| [kotlin.String](kotlin.-string/index.md) |  |

### Properties

| [TAG](-t-a-g.md) | `val TAG: String` |
| [log](log.md) | `val log: `[`Logger`](../nl.mpcjanssen.simpletask/-logger/index.md) |
| [todayAsString](today-as-string.md) | `val todayAsString: String` |

### Functions

| [addBusinessDays](add-business-days.md) | `fun addBusinessDays(originalDate: DateTime, days: Int): DateTime` |
| [addHeaderLines](add-header-lines.md) | `fun addHeaderLines(visibleTasks: List<`[`TodoItem`](../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>, sorts: List<String>, no_header: String, createIsThreshold: Boolean, moduleName: String?): List<`[`VisibleLine`](../nl.mpcjanssen.simpletask/-visible-line/index.md)`>`<br>`fun addHeaderLines(visibleTasks: List<`[`TodoItem`](../nl.mpcjanssen.simpletask.dao.gentodo/-todo-item/index.md)`>, filter: `[`ActiveFilter`](../nl.mpcjanssen.simpletask/-active-filter/index.md)`, no_header: String): List<`[`VisibleLine`](../nl.mpcjanssen.simpletask/-visible-line/index.md)`>` |
| [addInterval](add-interval.md) | `fun addInterval(dateTimeStr: String?, interval: String): DateTime?`<br>`fun addInterval(date: DateTime?, interval: String): DateTime?` |
| [appVersion](app-version.md) | `fun appVersion(ctx: Context): String` |
| [broadcastFileChanged](broadcast-file-changed.md) | `fun broadcastFileChanged(broadcastManager: LocalBroadcastManager): Unit` |
| [broadcastRefreshUI](broadcast-refresh-u-i.md) | `fun broadcastRefreshUI(broadcastManager: LocalBroadcastManager): Unit` |
| [broadcastRefreshWidgets](broadcast-refresh-widgets.md) | `fun broadcastRefreshWidgets(broadcastManager: LocalBroadcastManager): Unit` |
| [copyFile](copy-file.md) | `fun copyFile(sourceFile: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`, destFile: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`): Unit` |
| [createAlertDialog](create-alert-dialog.md) | `fun createAlertDialog(act: Activity, titleId: Int, alert: String): AlertDialog` |
| [createCachedDatabase](create-cached-database.md) | `fun createCachedDatabase(context: Context, dbFile: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`): Unit` |
| [createCachedFile](create-cached-file.md) | `fun createCachedFile(context: Context, fileName: String, content: String): Unit` |
| [createDeferDialog](create-defer-dialog.md) | `fun createDeferDialog(act: Activity, titleId: Int, listener: `[`InputDialogListener`](-input-dialog-listener/index.md)`): AlertDialog` |
| [createParentDirectory](create-parent-directory.md) | `fun createParentDirectory(dest: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`?): Unit` |
| [getCheckedItems](get-checked-items.md) | `fun getCheckedItems(listView: ListView, checked: Boolean): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [getRelativeAge](get-relative-age.md) | `fun getRelativeAge(task: `[`Task`](../nl.mpcjanssen.simpletask.task/-task/index.md)`, app: `[`TodoApplication`](../nl.mpcjanssen.simpletask/-todo-application/index.md)`): String?` |
| [getRelativeDueDate](get-relative-due-date.md) | `fun getRelativeDueDate(task: `[`Task`](../nl.mpcjanssen.simpletask.task/-task/index.md)`, app: `[`TodoApplication`](../nl.mpcjanssen.simpletask/-todo-application/index.md)`): SpannableString?` |
| [getRelativeThresholdDate](get-relative-threshold-date.md) | `fun getRelativeThresholdDate(task: `[`Task`](../nl.mpcjanssen.simpletask.task/-task/index.md)`, app: `[`TodoApplication`](../nl.mpcjanssen.simpletask/-todo-application/index.md)`): String?` |
| [getString](get-string.md) | `fun getString(resId: Int): String` |
| [initTaskWithFilter](init-task-with-filter.md) | `fun initTaskWithFilter(task: `[`Task`](../nl.mpcjanssen.simpletask.task/-task/index.md)`, mFilter: `[`ActiveFilter`](../nl.mpcjanssen.simpletask/-active-filter/index.md)`): Unit` |
| [isEmptyOrNull](is-empty-or-null.md) | `fun isEmptyOrNull(s: String?): Boolean`<br>Checks the passed in string to see if it is null or an blank string |
| [join](join.md) | `fun join(s: Collection<String>?, delimiter: String): String` |
| [loadFromFile](load-from-file.md) | `fun loadFromFile(file: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`): List<String>` |
| [markdownAssetAsHtml](markdown-asset-as-html.md) | `fun markdownAssetAsHtml(ctxt: Context, name: String): String` |
| [prefixItems](prefix-items.md) | `fun prefixItems(prefix: String, items: `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [readAsset](read-asset.md) | `fun readAsset(assets: AssetManager, name: String): String` |
| [runOnMainThread](run-on-main-thread.md) | `fun runOnMainThread(r: `[`Runnable`](http://docs.oracle.com/javase/6/docs/api/java/lang/Runnable.html)`): Unit` |
| [setColor](set-color.md) | `fun setColor(ss: SpannableString, color: Int, s: String): Unit`<br>`fun setColor(ss: SpannableString, color: Int, items: List<String>): Unit`<br>`fun setColor(ss: SpannableString, color: Int): Unit` |
| [shareText](share-text.md) | `fun shareText(act: Activity, subject: String, text: String): Unit` |
| [shortAppVersion](short-app-version.md) | `fun shortAppVersion(): String` |
| [showChangelogOverlay](show-changelog-overlay.md) | `fun showChangelogOverlay(act: Activity): Dialog?` |
| [showConfirmationDialog](show-confirmation-dialog.md) | `fun showConfirmationDialog(cxt: Context, msgid: Int, okListener: OnClickListener, titleid: Int): Unit`<br>`fun showConfirmationDialog(cxt: Context, msgid: Int, okListener: OnClickListener, title: CharSequence): Unit` |
| [showLoadingOverlay](show-loading-overlay.md) | `fun showLoadingOverlay(act: Activity, visibleDialog: Dialog?, show: Boolean): Dialog?` |
| [showToastLong](show-toast-long.md) | `fun showToastLong(cxt: Context, resid: Int): Unit`<br>`fun showToastLong(cxt: Context, msg: String): Unit` |
| [showToastShort](show-toast-short.md) | `fun showToastShort(cxt: Context, resid: Int): Unit`<br>`fun showToastShort(cxt: Context, msg: String): Unit` |
| [sortWithPrefix](sort-with-prefix.md) | `fun sortWithPrefix(items: List<String>, caseSensitive: Boolean, prefix: String?): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>`<br>`fun sortWithPrefix(items: Set<String>, caseSensitive: Boolean, prefix: String?): `[`ArrayList`](http://docs.oracle.com/javase/6/docs/api/java/util/ArrayList.html)`<String>` |
| [writeToFile](write-to-file.md) | `fun writeToFile(contents: String, file: `[`File`](http://docs.oracle.com/javase/6/docs/api/java/io/File.html)`, append: Boolean): Unit` |

