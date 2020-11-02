/**
 * @author Mark Janssen
 * @author Vojtech Kral
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
 * @copyright 2015 Vojtech Kral
 */

package nl.mpcjanssen.simpletask

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.SearchManager
import android.content.*
import android.content.res.Configuration
import android.content.res.TypedArray
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import androidx.annotation.StyleableRes
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import androidx.core.content.FileProvider
import androidx.core.widget.NestedScrollView

import hirondelle.date4j.DateTime
import kotlinx.android.synthetic.main.main.*
import nl.mpcjanssen.simpletask.adapters.DrawerAdapter
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.task.*
import nl.mpcjanssen.simpletask.util.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import android.R.id as androidId

class Simpletask : ThemedNoActionBarActivity() {
    companion object {
        private val REQUEST_PREFERENCES = 1

        val URI_BASE = Uri.fromParts("Simpletask", "", null)!!
        val URI_SEARCH = Uri.withAppendedPath(URI_BASE, "search")!!
        private val TAG = "Simpletask"
        // Drawer side
        private val SAVED_FILTER_DRAWER = GravityCompat.END
        private val QUICK_FILTER_DRAWER = GravityCompat.START
    }
    private var options_menu: Menu? = null

    lateinit var taskAdapter: TaskAdapter
    private var m_broadcastReceiver: BroadcastReceiver? = null
    private var localBroadcastManager: LocalBroadcastManager? = null



    private var m_drawerToggle: ActionBarDrawerToggle? = null
    private var m_savedInstanceState: Bundle? = null

    private val uiHandler = UiHandler()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        m_savedInstanceState = savedInstanceState
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_ACTION_LOGOUT)
        intentFilter.addAction(Constants.BROADCAST_AUTH_FAILED)
        intentFilter.addAction(Constants.BROADCAST_TASKLIST_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_SYNC_START)
        intentFilter.addAction(Constants.BROADCAST_THEME_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_DATEBAR_SIZE_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_MAIN_FONTSIZE_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_SYNC_DONE)
        intentFilter.addAction(Constants.BROADCAST_STATE_INDICATOR)
        intentFilter.addAction(Constants.BROADCAST_HIGHLIGHT_SELECTION)

        taskAdapter = TaskAdapter(
                completeAction = {
                    completeTasks(it)
                    // Update the tri state checkbox
                    handleMode(mapOf(Mode.SELECTION to { invalidateOptionsMenu() }))
                    TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, save = false, refreshMainUI = true)
                },
                unCompleteAction = {
                    uncompleteTasks(it)
                    // Update the tri state checkbox
                    handleMode(mapOf(Mode.SELECTION to { invalidateOptionsMenu() }))
                    TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)
                },
                onClickAction = {
                    val newSelectedState = !TodoApplication.todoList.isSelected(it)
                    if (newSelectedState) {
                        TodoApplication.todoList.selectTasks(listOf(it))
                    } else {
                        TodoApplication.todoList.unSelectTasks(listOf(it))
                    }
                    invalidateOptionsMenu()
                },
                onLongClickAction = {
                    val links = ArrayList<String>()
                    val actions = ArrayList<Action>()
                    val t = it
                    for (link in t.links) {
                        actions.add(Action.LINK)
                        links.add(link)
                    }
                    for (number in t.phoneNumbers) {
                        actions.add(Action.PHONE)
                        links.add(number)
                        actions.add(Action.SMS)
                        links.add(number)
                    }
                    for (mail in t.mailAddresses) {
                        actions.add(Action.MAIL)
                        links.add(mail)
                    }
                    if (actions.size != 0) {

                        val titles = ArrayList<String>()
                        for (i in links.indices) {
                            when (actions[i]) {
                                Action.SMS -> titles.add(i, getString(R.string.action_pop_up_sms) + links[i])
                                Action.PHONE -> titles.add(i, getString(R.string.action_pop_up_call) + links[i])
                                else -> titles.add(i, links[i])
                            }
                        }
                        val build = AlertDialog.Builder(this@Simpletask)
                        build.setTitle(R.string.task_action)
                        val titleArray = titles.toArray<String>(arrayOfNulls<String>(titles.size))
                        build.setItems(titleArray) { _, which ->
                            val actionIntent: Intent
                            val url = links[which]
                            Log.i(TAG, "" + actions[which] + ": " + url)
                            when (actions[which]) {
                                Action.LINK -> when {
                                    url.startsWith("todo://") -> {
                                        val todoFolder = TodoApplication.config.todoFile.parentFile
                                        val newName = File(todoFolder, url.substring(7))
                                        TodoApplication.app.switchTodoFile(newName)
                                    }
                                    url.startsWith("root://") -> {
                                        val rootFolder = TodoApplication.config.localFileRoot
                                        val file = File(rootFolder, url.substring(7))
                                        actionIntent = Intent(Intent.ACTION_VIEW)

                                        val contentUri = FileProvider.getUriForFile(Simpletask@this, BuildConfig.APPLICATION_ID + ".provider",file);
                                        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                                        actionIntent.setDataAndType(contentUri, mime)
                                        actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        startActivity(actionIntent)
                                    }
                                    else -> try {
                                        actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        startActivity(actionIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        Log.i(TAG, "No handler for task action $url")
                                        showToastLong(TodoApplication.app, "No handler for $url")
                                    }
                                }
                                Action.PHONE -> {
                                    actionIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(url)))
                                    startActivity(actionIntent)
                                }
                                Action.SMS -> {
                                    actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + Uri.encode(url)))
                                    startActivity(actionIntent)
                                }
                               Action.MAIL -> {
                                    actionIntent = Intent(Intent.ACTION_SEND, Uri.parse(url))
                                    actionIntent.putExtra(Intent.EXTRA_EMAIL,
                                            arrayOf(url))
                                    actionIntent.type = "text/plain"
                                    startActivity(actionIntent)
                                }
                            }
                        }
                        build.create().show()
                    }
                    true
                })


        setContentView(R.layout.main)

        localBroadcastManager = TodoApplication.app.localBroadCastManager

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, receivedIntent: Intent) {
                if (receivedIntent.action == Constants.BROADCAST_ACTION_LOGOUT) {
                    Log.i(TAG, "Logging out from Dropbox")
                    finish()
                    FileStoreActionQueue.add("Logout") {
                        try {
                            FileStore.logout()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error logging out.", e)
                        }
                        startLogin()
                    }
                } else if (receivedIntent.action == Constants.BROADCAST_TASKLIST_CHANGED) {
                    Log.i(TAG, "Tasklist changed, refiltering adapter")
                    taskAdapter.setFilteredTasks(this@Simpletask, TodoApplication.config.mainQuery)
                    runOnUiThread {
                        uiHandler.forEvent(Event.TASK_LIST_CHANGED)
                    }
                } else if (receivedIntent.action == Constants.BROADCAST_HIGHLIGHT_SELECTION) {
                    Log.i(TAG, "Highligh selection")
                    taskAdapter.notifyDataSetChanged()
                    invalidateOptionsMenu()
                } else if (receivedIntent.action == Constants.BROADCAST_SYNC_START) {
                    showListViewProgress(true)
                } else if (receivedIntent.action == Constants.BROADCAST_SYNC_DONE) {
                    showListViewProgress(false)
                } else if (receivedIntent.action == Constants.BROADCAST_STATE_INDICATOR) {
                    uiHandler.forEvent(Event.UPDATE_PENDING_CHANGES)
                } else if (receivedIntent.action == Constants.BROADCAST_MAIN_FONTSIZE_CHANGED) {
                    uiHandler.forEvent(Event.FONT_SIZE_CHANGED)
                } else if (receivedIntent.action == Constants.BROADCAST_THEME_CHANGED ||
                        receivedIntent.action == Constants.BROADCAST_DATEBAR_SIZE_CHANGED) {
                    recreate()
                } else if (receivedIntent.action == Constants.BROADCAST_AUTH_FAILED) {
                    startLogin()
                }
            }
        }
        localBroadcastManager!!.registerReceiver(broadcastReceiver, intentFilter)
        m_broadcastReceiver = broadcastReceiver
        setSupportActionBar(main_actionbar)

        // Replace drawables if the theme is dark
        if (TodoApplication.config.isDarkTheme || TodoApplication.config.isBlackTheme) {
            actionbar_clear?.setImageResource(R.drawable.ic_close_white_24dp)
        }
        val versionCode = BuildConfig.VERSION_CODE
        if (TodoApplication.app.isAuthenticated) {
            if (TodoApplication.config.latestChangelogShown < versionCode) {
                showChangelogOverlay(this)
                TodoApplication.config.latestChangelogShown = versionCode
            } else if (!TodoApplication.config.rightDrawerDemonstrated) {
                TodoApplication.config.rightDrawerDemonstrated = true
                openSavedFilterDrawer()
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when {
            Intent.ACTION_SEARCH == intent.action -> {
                val currentIntent = getIntent()
                currentIntent.putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY))
                setIntent(currentIntent)
                options_menu?.findItem(R.id.search)?.collapseActionView() ?: return

            }
            CalendarContract.ACTION_HANDLE_CUSTOM_EVENT == intent.action -> // Uri uri = Uri.parse(intent.getStringExtra(CalendarContract.EXTRA_CUSTOM_APP_URI));
                Log.w(TAG, "Not implemented search")
           // Only change intent if it actually contains a applyFilter
        }
        Log.i(TAG, "onNewIntent: $intent")

    }

    override fun onResume() {
        super.onResume()

        Log.i(TAG, "onResume")
        TodoApplication.todoList.reload(reason = "Main activity resume")
        Log.i(TAG,"onResume -> handleIntent")
        handleIntent()
        Log.i(TAG,"onResume <- handleIntent")
        uiHandler.forEvent(Event.RESUME)
    }

    override fun onSearchRequested(): Boolean {
        options_menu?.let {
            val searchMenuItem = it.findItem(R.id.search)
            searchMenuItem.expandActionView()
            return true
        }
        return false
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        m_drawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        m_drawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        m_broadcastReceiver?.let {
            localBroadcastManager!!.unregisterReceiver(it)
        }
    }



    override fun onPause() {
        listView?.let{updateScrollPosition(it)}
        super.onPause()
    }

    private fun updateScrollPosition(view: RecyclerView) {
        (view.layoutManager as LinearLayoutManager?)?.let { manager ->
            val position = manager.findFirstVisibleItemPosition()
            val firstItemView = manager.findViewByPosition(position)
            val offset = firstItemView?.top ?: 0
            TodoApplication.config.lastScrollPosition = position
            TodoApplication.config.lastScrollOffset = offset
        }
    }

    override fun onBackPressed() {
        handleMode(mapOf(
                Mode.SAVED_FILTER_DRAWER to {
                    closeDrawer(SAVED_FILTER_DRAWER)
                },
                Mode.QUICK_FILTER_DRAWER to {
                    closeDrawer(QUICK_FILTER_DRAWER)
                }, Mode.SELECTION to {
            closeSelectionMode()
        }, Mode.MAIN to {
            if (!TodoApplication.config.backClearsFilter || !TodoApplication.config.mainQuery.hasFilter()) {
                super.onBackPressed()
            } else {
                clearFilter()
                uiHandler.forEvent(Event.CLEAR_FILTER)
            }
        }
        ))
    }


    private fun openLuaConfig() {
        val i = Intent(this, ScriptConfigScreen::class.java)
        startActivity(i)
    }

    private fun showHelp() {
        val i = Intent(this, HelpScreen::class.java)
        startActivity(i)
    }




    private fun closeSelectionMode() {
        TodoApplication.todoList.clearSelection()
        if (TodoApplication.config.hasKeepSelection) {
            // Refilter tasks to remove selected tasks which don't match the current filter
            uiHandler.forEvent(Event.FILTER_CHANGED)
        } else {
            uiHandler.forEvent(Event.CLEAR_SELECTION)
        }
    }

    private fun selectedTasksAsString(): String {
        val result = ArrayList<String>()
        TodoApplication.todoList.selectedTasks.forEach { task ->
            val luaTxt = Interpreter.onDisplayCallback(TodoApplication.config.mainQuery.luaModule, task)
            result.add(luaTxt ?: task.inFileFormat(TodoApplication.config.useUUIDs))
        }
        return join(result, "\n")
    }

    private fun selectAllTasks() {
        val selectedTasks = taskAdapter.visibleLines
                .filterNot(VisibleLine::header)
                .map { it.task!! }
        TodoApplication.todoList.selectTasks(selectedTasks)
    }



    private fun handleIntent() {
        if (!TodoApplication.app.isAuthenticated) {
            Log.i(TAG, "handleIntent: not authenticated")
            startLogin()
            return
        }

        drawer_layout?.let { drawerLayout ->
            m_drawerToggle = object : ActionBarDrawerToggle(this, /* host Activity */
                    drawerLayout, /* DrawerLayout object */
                    R.string.changelist, /* "open drawer" description */
                    R.string.app_label /* "close drawer" description */) {

                /**
                 * Called when a drawer has settled in a completely closed
                 * state.
                 */
                override fun onDrawerClosed(view: View) {
                    invalidateOptionsMenu()
                }

                /** Called when a drawer has settled in a completely open state.  */
                override fun onDrawerOpened(drawerView: View) {
                    invalidateOptionsMenu()
                }
            }

            // Set the drawer toggle as the DrawerListener
            val toggle = m_drawerToggle as ActionBarDrawerToggle
            drawerLayout.removeDrawerListener(toggle)
            drawerLayout.addDrawerListener(toggle)
            supportActionBar?.let {
                it.setDisplayHomeAsUpEnabled(true)
                it.setHomeButtonEnabled(true)
                m_drawerToggle!!.isDrawerIndicatorEnabled = true
            }
            m_drawerToggle!!.syncState()
        }

        // Show search or applyFilter results
        val currentIntent = intent
        val query = if (Constants.INTENT_START_FILTER == currentIntent.action) {
            Log.i(TAG, "handleIntent")
            currentIntent.extras?.let { extras ->
                extras.keySet().map { Pair(it, extras[it]) }.forEach { (key, value) ->
                    val debugString = value?.let { v ->
                        "$v (${v.javaClass.name})"
                    } ?: "<null>"
                    Log.d(TAG, "$key $debugString")
                }
            }
            val newQuery = Query(currentIntent, "mainui")
            TodoApplication.config.mainQuery = newQuery
            // Query is saved clear action from intent
            intent.action = ""
            newQuery
        } else {
            // Set previous filters and sort
            Log.i(TAG, "handleIntent: from m_prefs state")
            TodoApplication.config.mainQuery
        }

        if (currentIntent.hasExtra(Constants.INTENT_SELECTED_TASK_LINE)) {
            Log.d(TAG, "Selection from intent")
            val position = currentIntent.getIntExtra(Constants.INTENT_SELECTED_TASK_LINE, -1)
            currentIntent.removeExtra(Constants.INTENT_SELECTED_TASK_LINE)
            intent = currentIntent
            if (position > -1) {
                val itemAtPosition = TodoApplication.todoList.getTaskAt(position)
                itemAtPosition?.let {
                    TodoApplication.todoList.clearSelection()
                    TodoApplication.todoList.selectTasks(listOf(itemAtPosition))
                    TodoApplication.config.lastScrollPosition = taskAdapter.getPosition(it)
                    TodoApplication.config.lastScrollOffset = 0
                }

            }
        }

        listView?.layoutManager = LinearLayoutManager(this)
        listView?.adapter = this.taskAdapter

        taskAdapter.setFilteredTasks(this, query)
        val listener = ViewTreeObserver.OnScrollChangedListener {
            listView?.let { updateScrollPosition(it) }
        }
        listView?.viewTreeObserver?.addOnScrollChangedListener(listener)

        fab.setOnClickListener { startAddTaskActivity() }
    }



    private fun startLogin() {
        TodoApplication.app.startLogin(this)
        finish()
    }

    private fun updateCompletionCheckboxState() {
        val cbItem = toolbar.menu.findItem(R.id.multicomplete_checkbox) ?: return
        val selectedTasks = TodoApplication.todoList.selectedTasks
        val count = selectedTasks.count()
        val completedCount = selectedTasks.count { it.isCompleted() }
        when (completedCount) {
            0 -> {
                cbItem.setIcon(R.drawable.ic_check_box_outline_blank_white_24dp)
                cbItem.setOnMenuItemClickListener { completeTasks(selectedTasks) ; true }
            }
            count -> {
                cbItem.setIcon(R.drawable.ic_check_box_white_24dp)
                cbItem.setOnMenuItemClickListener { uncompleteTasks(selectedTasks) ; true }
            }
            else -> {
                cbItem.setIcon(R.drawable.ic_indeterminate_check_box_white_24dp)
                cbItem.setOnMenuItemClickListener {
                    val popup = PopupMenu(this, toolbar)
                    val menuInflater = popup.menuInflater
                    menuInflater.inflate(R.menu.completion_popup, popup.menu)
                    popup.show()
                    popup.setOnMenuItemClickListener popup@{ item ->
                        val menuId = item.itemId
                        when (menuId) {
                            R.id.complete -> completeTasks(selectedTasks)
                            R.id.uncomplete -> uncompleteTasks(selectedTasks)
                        }
                        return@popup true
                    } ; true
                }
            }
        }
    }


    @SuppressLint("Recycle")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.i(TAG, "Recreating options menu")
        this.options_menu = menu

        val inflater = menuInflater
        val toggle = m_drawerToggle ?: return super.onCreateOptionsMenu(menu)
        val actionBar = supportActionBar ?: return super.onCreateOptionsMenu(menu)
        handleMode(mapOf(
                Mode.SAVED_FILTER_DRAWER to {
                    inflater.inflate(R.menu.nav_drawer, menu)
                    setTitle(R.string.filter_saved_prompt)
                },
                Mode.QUICK_FILTER_DRAWER to {
                    inflater.inflate(R.menu.filter_drawer, menu)
                    setTitle(R.string.title_filter_drawer)
                },
                Mode.SELECTION to {
                    val actionColor = ContextCompat.getDrawable(this, R.color.gray74)
                    actionBar.setBackgroundDrawable(actionColor)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        window.statusBarColor = ContextCompat.getColor(this, R.color.gray87)
                    }

                    inflater.inflate(R.menu.task_context_actionbar, menu)
                    title = "${TodoApplication.todoList.numSelected()}"
                    toggle.isDrawerIndicatorEnabled = false
                    fab.visibility = View.GONE
                    toolbar.setOnMenuItemClickListener { item ->
                        onOptionsItemSelected(item)
                    }
                    toolbar.visibility = View.VISIBLE
                    toolbar.menu.clear()
                    inflater.inflate(R.menu.task_context, toolbar.menu)
                    if (!TodoApplication.config.useListAndTagIcons) {
                        toolbar.menu?.apply {
                            findItem(R.id.update_lists)?.setIcon(R.drawable.ic_action_todotxt_lists)
                            findItem(R.id.update_tags)?.setIcon(R.drawable.ic_action_todotxt_tags)
                        }
                    }


                    updateCompletionCheckboxState()
                    selection_fab.visibility = View.VISIBLE
                    selection_fab.setOnClickListener {
                        createCalendarAppointment(TodoApplication.todoList.selectedTasks)
                    }
                },

                Mode.MAIN to {
                    @StyleableRes
                    val primaryIdx = 0
                    @StyleableRes
                    val primaryDarkIdx = 1

                    val a: TypedArray = obtainStyledAttributes(intArrayOf(R.attr.colorPrimary, R.attr.colorPrimaryDark))
                    try {
                        val colorPrimary = ContextCompat.getDrawable(this, a.getResourceId(primaryIdx, 0))

                        actionBar.setBackgroundDrawable(colorPrimary)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            window.statusBarColor = ContextCompat.getColor(this, a.getResourceId(primaryDarkIdx, 0))
                        }
                    } finally {
                        a.recycle()
                    }

                    inflater.inflate(R.menu.main, menu)

                    populateSearch(menu)
                    if (TodoApplication.config.showTodoPath) {
                        title = TodoApplication.config.todoFile.canonicalPath.replace("([^/])[^/]*/".toRegex(), "$1/")
                    } else {
                        setTitle(R.string.app_label)
                    }
                    toggle.isDrawerIndicatorEnabled = true
                    fab.visibility = View.VISIBLE
                    selection_fab.visibility = View.GONE
                    toolbar.visibility = View.GONE
                    true
                }))
        return true
    }

    /**
     * isDrawerOpen only returns true only if m_drawerLayout != null, so
     * if this returns either _DRAWER, m_drawerLayout!!. calls are safe to make
     */
    private fun handleMode(actions: Map<Mode, () -> Any?>) {
        Log.d(TAG, "Handle mode")
        runOnUiThread {
            when {
                isDrawerOpen(SAVED_FILTER_DRAWER) -> actions[Mode.SAVED_FILTER_DRAWER]?.invoke()
                isDrawerOpen(QUICK_FILTER_DRAWER) -> actions[Mode.QUICK_FILTER_DRAWER]?.invoke()
                TodoApplication.todoList.selectedTasks.isNotEmpty() -> actions[Mode.SELECTION]?.invoke()
                else -> actions[Mode.MAIN]?.invoke()
            }
        }
    }

    private fun isDrawerOpen(drawer: Int): Boolean {
        if (drawer_layout == null) {
            Log.w(TAG, "Layout was null")
            return false
        }
        return drawer_layout.isDrawerOpen(drawer)
    }

    private fun closeDrawer(drawer: Int) {
        drawer_layout?.closeDrawer(drawer)
    }

    private fun openSavedFilterDrawer() {
        closeDrawer(QUICK_FILTER_DRAWER)
        if (!isDrawerOpen(SAVED_FILTER_DRAWER)) {
            drawer_layout.openDrawer(SAVED_FILTER_DRAWER)
        }
    }

    private fun populateSearch(menu: Menu?) {
        if (menu == null) {
            return
        }
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchMenu = menu.findItem(R.id.search)

        val searchView = searchMenu.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        val activeTextSearch = TodoApplication.config.mainQuery.search
        if (!activeTextSearch.isNullOrEmpty()) {
            searchView.setQuery(activeTextSearch, false)
            searchView.isActivated = true
        }

        searchView.setIconifiedByDefault(false)
        searchMenu.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // Do something when collapsed
                return true // Return true to collapse action view
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                //get focus
                item.actionView.requestFocus()
                //get input method
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
                return true // Return true to expand action view
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            var m_ignoreSearchChangeCallback: Boolean = false

            override fun onQueryTextSubmit(query: String): Boolean {
                // Stupid searchView code will call onQueryTextChange callback
                // When the actionView collapse and the textView is reset
                // ugly global hack around this
                m_ignoreSearchChangeCallback = true
                menu.findItem(R.id.search).collapseActionView()
                m_ignoreSearchChangeCallback = false
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (!m_ignoreSearchChangeCallback) {
                    val query = TodoApplication.config.mainQuery
                    query.search = newText
                    TodoApplication.config.mainQuery = query
                    uiHandler.forEvent(Event.FILTER_CHANGED)
                }
                return true
            }
        })
    }

    private fun prioritizeTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            return
        }
        val priorityArr = Priority.codes.toTypedArray()

        val first = tasks[0].priority.code

        val priorityIdx = if (tasks.all { it.priority.code == first }) {
            priorityArr.indexOf(first)
        } else 0

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.select_priority)
        builder.setSingleChoiceItems(priorityArr, priorityIdx, { dialog, which ->
            dialog.dismiss()
            val priority = Priority.toPriority(priorityArr[which])
            TodoApplication.todoList.prioritize(tasks, priority)
            TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)
        })
        builder.show()

    }

    private fun completeTasks(task: Task) {
        val tasks = ArrayList<Task>()
        tasks.add(task)
        completeTasks(tasks)
    }

    private fun completeTasks(tasks: List<Task>) {
        TodoApplication.todoList.complete(tasks, TodoApplication.config.hasKeepPrio, TodoApplication.config.hasAppendAtEnd)
        if (TodoApplication.config.isAutoArchive) {
            archiveTasks(false)
        }
        TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)
    }

    private fun uncompleteTasks(task: Task) {
        val tasks = ArrayList<Task>()
        tasks.add(task)
        uncompleteTasks(tasks)
    }

    private fun uncompleteTasks(tasks: List<Task>) {
        TodoApplication.todoList.uncomplete(tasks)
        TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)
    }

    private fun deferTasks(tasks: List<Task>, dateType: DateType) {
        var titleId = R.string.defer_due
        if (dateType === DateType.THRESHOLD) {
            titleId = R.string.defer_threshold
        }
        val d = createDeferDialog(this, titleId, object : InputDialogListener {
            /* Suppress showCalendar deprecation message. It works fine on older devices
             * and newer devices don't really have an alternative */
            @Suppress("DEPRECATION")
            override fun onClick(input: String) {
                if (input == "pick") {
                    val today = DateTime.today(TimeZone.getDefault())
                    val dialog = DatePickerDialog(this@Simpletask, DatePickerDialog.OnDateSetListener { _, year, month, day ->
                        var startMonth = month
                        startMonth++
                        val date = DateTime.forDateOnly(year, startMonth, day)
                        TodoApplication.todoList.defer(date.format(Constants.DATE_FORMAT), tasks, dateType)
                        TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)
                    },
                            today.year!!,
                            today.month!! - 1,
                            today.day!!)

                    val showCalendar = TodoApplication.config.showCalendar
                    dialog.datePicker.calendarViewShown = showCalendar
                    dialog.datePicker.spinnersShown = !showCalendar
                    dialog.show()
                } else {

                    TodoApplication.todoList.defer(input, tasks, dateType)
                    TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)

                }

            }
        })
        d.show()
    }

    private fun deleteTasks(tasks: List<Task>) {
        val numTasks = tasks.size
        val title = getString(R.string.delete_task_title)
                .replaceFirst(Regex("%s"), numTasks.toString())
        val delete = DialogInterface.OnClickListener { _, _ ->
            TodoApplication.todoList.removeAll(tasks)
            TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)
            invalidateOptionsMenu()
        }
        showConfirmationDialog(this, R.string.delete_task_message, delete, title)
    }

    private fun archiveTasks(showDialog: Boolean = true) {
        val selection = TodoApplication.todoList.selectedTasks

        val tasksToArchive =  ArrayList<Task>()
        if (selection.isNotEmpty()) {
            tasksToArchive.addAll(selection)
        } else {
            tasksToArchive.addAll(taskAdapter.visibleLines.asSequence()
                    .filterNot { it.header }
                    .map { (it as TaskLine).task }
                    .filter {it.isCompleted()})
        }

        val archiveAction = {
            if (TodoApplication.config.todoFile.canonicalPath == TodoApplication.config.doneFile.canonicalPath) {
                showToastShort(this, "You have the done.txt file opened.")
            } else {
                TodoApplication.todoList.archive(TodoApplication.config.todoFile, TodoApplication.config.doneFile, tasksToArchive, TodoApplication.config.eol)
                invalidateOptionsMenu()
            }
        }
        val numTasks = tasksToArchive.size
        if (numTasks == 0) {
            showToastLong(this, R.string.no_tasks_to_archive)
            return
        }
        if (showDialog) {
            val title = getString(R.string.archive_task_title).replaceFirst(Regex("%s"), numTasks.toString())
            val archive = DialogInterface.OnClickListener { _, _ -> archiveAction() }
            showConfirmationDialog(this, R.string.delete_task_message, archive, title)
        } else {
            archiveAction()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "onMenuItemSelected: " + item.itemId)
        val checkedTasks = TodoApplication.todoList.selectedTasks
        when (item.itemId) {
            androidId.home -> {
                handleMode(mapOf(
                        Mode.SAVED_FILTER_DRAWER to { closeDrawer(SAVED_FILTER_DRAWER) },
                        Mode.QUICK_FILTER_DRAWER to { closeDrawer(QUICK_FILTER_DRAWER) },
                        Mode.SELECTION to { closeSelectionMode() },
                        Mode.MAIN to { m_drawerToggle?.onOptionsItemSelected(item) }
                ))
                return true
            }
            R.id.search -> {
            }
            R.id.preferences -> startPreferencesActivity()
            R.id.filter -> startFilterActivity()
            R.id.context_delete -> deleteTasks(checkedTasks)
            R.id.context_select_all -> selectAllTasks()
            R.id.share -> {
                val shareText = TodoApplication.todoList.fileFormat
                shareText(this@Simpletask, "Simpletask list", shareText)
            }
            R.id.context_share -> {
                val shareText = selectedTasksAsString()
                shareText(this@Simpletask, "Simpletask tasks", shareText)
            }
            R.id.context_archive -> archiveTasks(TodoApplication.config.showConfirmationDialogs)
            R.id.help -> showHelp()
            R.id.open_lua -> openLuaConfig()
            R.id.sync -> {
                broadcastFileSync(TodoApplication.app.localBroadCastManager)
            }
            R.id.archive -> archiveTasks(true)
            R.id.show_filter_drawer -> openSavedFilterDrawer()
            R.id.open_file -> TodoApplication.app.browseForNewFile(this)
            R.id.history -> startActivity(Intent(this, HistoryScreen::class.java))
            R.id.btn_filter_add -> onAddFilterClick()
            R.id.clear_filter -> clearFilter()
            R.id.update -> startAddTaskActivity()
            R.id.defer_due -> deferTasks(checkedTasks, DateType.DUE)
            R.id.defer_threshold -> deferTasks(checkedTasks, DateType.THRESHOLD)
            R.id.priority -> prioritizeTasks(checkedTasks)
            R.id.update_lists -> updateLists(checkedTasks)
            R.id.update_tags -> updateTags(checkedTasks)
            R.id.menu_export_filter_export -> {
                FileStoreActionQueue.add("Exporting filters") {
                    try {
                        QueryStore.exportFilters(File(TodoApplication.config.todoFile.parentFile, "saved_filters.txt"))
                        showToastShort(this, R.string.saved_filters_exported)
                    } catch (e: Exception) {
                        Log.e(TAG, "Export filters failed", e)
                        showToastLong(this, "Error exporting filters")
                    }
                }
            }
            R.id.menu_export_filter_import -> {
                FileStoreActionQueue.add("Importing filters") {
                    val importFile = File(TodoApplication.config.todoFile.parentFile, "saved_filters.txt")
                    try {
                        QueryStore.importFilters(importFile)
                        showToastShort(this, R.string.saved_filters_imported)
                        uiHandler.forEvent(Event.SAVED_FILTERS_IMPORTED)

                    } catch (e: Exception) {
                        // Need to catch generic exception because Dropbox errors don't inherit from IOException
                        Log.e(TAG, "Import filters, cant read file ${importFile}", e)
                        showToastLong(this, "Error reading file ${importFile}")
                    }
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun createCalendarAppointment(checkedTasks: List<Task>) {
        var calendarTitle = getString(R.string.calendar_title)
        var calendarDescription = ""
        if (checkedTasks.isEmpty()) {
            return
        }
        if (checkedTasks.size == 1) {
            // Set the task as title
            val task = checkedTasks[0]
            val luaTxt = Interpreter.onDisplayCallback(TodoApplication.config.mainQuery.luaModule, task)
            calendarTitle = luaTxt ?: task.text
        } else {
            // Set the tasks as description
            calendarDescription = selectedTasksAsString()
        }

        intent = Intent(Intent.ACTION_EDIT).apply {
            type = Constants.ANDROID_EVENT
            putExtra(Events.TITLE, calendarTitle)
            putExtra(Events.DESCRIPTION, calendarDescription)
        }
        // Explicitly set start and end date/time.
        // Some calendar providers need this.
        val dueDate = checkedTasks.getOrNull(0)?.dueDate
        val calDate = if (checkedTasks.size == 1 && dueDate != null) {
            val year = dueDate.substring(0, 4).toInt()
            val month = dueDate.substring(5, 7).toInt() - 1
            val day = dueDate.substring(8, 10).toInt()
            GregorianCalendar(year, month, day)
        } else {
            GregorianCalendar()
        }

        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                calDate.timeInMillis)
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                calDate.timeInMillis + 60 * 60 * 1000)
        startActivity(intent)
    }

    private fun startAddTaskActivity() {
        Log.i(TAG, "Starting addTask activity")

        TodoApplication.todoList.editTasks(this, TodoApplication.todoList.selectedTasks, TodoApplication.config.mainQuery.prefill)
    }

    private fun startPreferencesActivity() {
        val settingsActivity = Intent(baseContext,
                nl.mpcjanssen.simpletask.Preferences::class.java)
        startActivityForResult(settingsActivity, REQUEST_PREFERENCES)
    }

    /**
     * Handle clear applyFilter click *
     */
    @Suppress("unused")
    fun onClearClick(@Suppress("UNUSED_PARAMETER") v: View) = clearFilter()


    /**
     * Handle add applyFilter click *
     */
    fun onAddFilterClick() {
        val alert = AlertDialog.Builder(this)

        alert.setTitle(R.string.save_filter)
        alert.setMessage(R.string.save_filter_message)

        // Set an EditText view to get user input
        val input = EditText(this)
        alert.setView(input)
        input.setText(TodoApplication.config.mainQuery.proposedName)

        alert.setPositiveButton("Ok") { _, _ ->
            val value = input.text?.toString()?.takeIf { it.isNotBlank() }
            value?.let {
                QueryStore.save(TodoApplication.config.mainQuery, it)
                uiHandler.forEvent(Event.SAVED_FILTER_ADDED)
            }
            value ?: showToastShort(applicationContext, R.string.filter_name_empty)
        }

        alert.setNegativeButton("Cancel") { _, _ -> }
        alert.show()
    }

    private fun clearFilter() {
        TodoApplication.config.mainQuery = TodoApplication.config.mainQuery.clear()
        intent = TodoApplication.config.mainQuery.saveInIntent(intent)
        uiHandler.forEvent(Event.CLEAR_FILTER)
    }

    fun createFilterShortcut(namedQuery: NamedQuery) {
        val target = Intent(Constants.INTENT_START_FILTER)
        namedQuery.query.saveInIntent(target)
        createShortcut(this, "simpletaskLauncher", namedQuery.name, R.drawable.ic_launcher, target)
    }

    private fun deleteSavedQuery(id: String) {
        QueryStore.delete(id)
        uiHandler.forEvent(Event.SAVED_FILTER_DELETED)
    }

    private fun updateSavedQuery(oldQuery: NamedQuery, newQuery: Query) {
        QueryStore.save(newQuery, oldQuery.name)
        uiHandler.forEvent(Event.SAVED_FILTER_UPDATED)
    }

    private fun renameSavedQuery(id: String) {
        val squery = QueryStore.get(id)
        val alert = AlertDialog.Builder(this)

        alert.setTitle(R.string.rename_filter)
        alert.setMessage(R.string.rename_filter_message)

        // Set an EditText view to get user input
        val input = EditText(this)
        alert.setView(input)
        input.setText(squery.name)

        alert.setPositiveButton("Ok") { _, _ ->
            val value = input.text?.toString()?.takeIf { it.isNotBlank() }

            value?.let {
                QueryStore.rename(squery, it)
                uiHandler.forEvent(Event.SAVED_FILTER_RENAMED)
            } ?: showToastShort(applicationContext, R.string.filter_name_empty)
        }

        alert.setNegativeButton("Cancel") { _, _ -> }

        alert.show()
    }



    fun startFilterActivity() {
        val i = Intent(this, FilterActivity::class.java)
        TodoApplication.config.mainQuery.saveInIntent(i)
        startActivity(i)
    }

    val listView: RecyclerView?
        get() {
            val lv = list
            return lv
        }

    fun showListViewProgress(show: Boolean) {
        runOnUiThread {
            if (show) {
                sync_progress.visibility = View.VISIBLE
            } else {
                sync_progress.visibility = View.GONE
            }
        }
    }

    private fun updateLists(checkedTasks: List<Task>) {
        updateItemsDialog(
                TodoApplication.config.listTerm,
                checkedTasks,
                TodoApplication.todoList.contexts,
                Task::lists,
                Task::addList,
                Task::removeList
        ) {
            TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)
        }
    }

    private fun updateTags(checkedTasks: List<Task>) {
        updateItemsDialog(
                TodoApplication.config.tagTerm,
                checkedTasks,
                TodoApplication.todoList.projects,
                Task::tags,
                Task::addTag,
                Task::removeTag
        ) {
            TodoApplication.todoList.notifyTasklistChanged(TodoApplication.config.todoFile, true)
        }
    }

    private inner class UiHandler () {
        fun forEvent(event: Event) {
            val tag = "Event"
            Log.d(tag, "update UI for event ${event.name}")
            runOnUiThread {
                when (event) {
                    Event.SAVED_FILTER_ITEM_CLICK -> {
                        updateTaskList(TodoApplication.config.mainQuery) {
                            updateFilterBar()
                            updateQuickFilterDrawer()
                        }
                    }
                    Event.QUICK_FILTER_ITEM_CLICK,
                    Event.CLEAR_FILTER -> {
                        updateTaskList(TodoApplication.config.mainQuery) {
                            updateFilterBar()
                            updateQuickFilterDrawer()
                        }

                    }
                    Event.SAVED_FILTER_ADDED,
                    Event.SAVED_FILTER_RENAMED,
                    Event.SAVED_FILTER_UPDATED,
                    Event.SAVED_FILTER_DELETED,
                    Event.SAVED_FILTERS_IMPORTED -> {
                        updateSavedFilterDrawer()
                    }
                    Event.TASK_LIST_CHANGED -> {
                        updateTaskList(TodoApplication.config.mainQuery) {
                            updateFilterBar()
                            updateQuickFilterDrawer()
                            updateCompletionCheckboxState()
                        }
                    }
                    Event.FILTER_CHANGED -> {
                        updateTaskList(TodoApplication.config.mainQuery) {
                            updateFilterBar()
                            updateQuickFilterDrawer()
                        }
                    }
                    Event.RESUME -> {
                        updateFilterBar()
                        updateSavedFilterDrawer()
                        updateQuickFilterDrawer()
                        updateConnectivityIndicator()
                    }
                    Event.FONT_SIZE_CHANGED -> {
                        updateTaskList(TodoApplication.config.mainQuery) {
                            updateFilterBar()
                            updateQuickFilterDrawer()
                        }
                    }
                    Event.UPDATE_PENDING_CHANGES -> {
                        updateConnectivityIndicator()
                    }
                    Event.CLEAR_SELECTION -> {
                        invalidateOptionsMenu()
                    }
                }
            }
        }

        private fun updateFilterBar() {
            actionbar.visibility = when {
                TodoApplication.config.mainQuery.hasFilter() -> View.VISIBLE
                else -> View.GONE
            }
            Log.d(TAG, "Update applyFilter bar")
            val count = taskAdapter.countVisibleTasks
            val total = taskAdapter.countTotalTasks
            filter_text.text = TodoApplication.config.mainQuery.getTitle(
                    count,
                    total,
                    getText(R.string.priority_prompt),
                    TodoApplication.config.tagTerm,
                    TodoApplication.config.listTerm,
                    getText(R.string.search),
                    getText(R.string.script),
                    getText(R.string.title_filter_applied),
                    getText(R.string.no_filter))
        }

        private fun updateSavedFilterDrawer() {
            val idQueryPairs = QueryStore.ids().mapTo(mutableListOf()) { Pair(it, QueryStore.get(it)) }
            val hasQueries = !idQueryPairs.isEmpty()
            val queries = idQueryPairs.sortedBy { it.second.name }
            val names = if (hasQueries) {
                queries.map { it.second.name }
            } else {
                val result = ArrayList<String>()
                result.add(getString(R.string.nav_drawer_hint))
                result
            }
            nav_drawer.adapter = ArrayAdapter(this@Simpletask, R.layout.drawer_list_item, names)
            if (hasQueries) {
                nav_drawer.choiceMode = AbsListView.CHOICE_MODE_NONE
                nav_drawer.isLongClickable = true
                nav_drawer.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    queries[position].let {
                        val query = it.second.query
                        intent = query.saveInIntent(intent)
                        TodoApplication.config.mainQuery = query
                        taskAdapter.setFilteredTasks(this@Simpletask, query)
                        runOnUiThread {
                            closeDrawer(SAVED_FILTER_DRAWER)
                            updateQuickFilterDrawer()
                        }
                    }

                }
                nav_drawer.onItemLongClickListener = OnItemLongClickListener { _, view, position, _ ->
                    val query = queries[position]
                    val popupMenu = PopupMenu(this@Simpletask, view)
                    popupMenu.setOnMenuItemClickListener { item ->
                        val menuId = item.itemId
                        when (menuId) {
                            R.id.menu_saved_filter_delete -> deleteSavedQuery(query.first)
                            R.id.menu_saved_filter_shortcut -> createFilterShortcut(query.second)
                            R.id.menu_saved_filter_rename -> renameSavedQuery(query.first)
                            R.id.menu_saved_filter_update -> updateSavedQuery(query.second, TodoApplication.config.mainQuery)
                            else -> {
                            }
                        }
                        true
                    }
                    val inflater = popupMenu.menuInflater
                    inflater.inflate(R.menu.saved_filter, popupMenu.menu)
                    popupMenu.show()
                    true
                }
            }
        }

        private fun updateTaskList(query: Query, afterOnUi: ()->Unit) {
            runOnMainThread (Runnable {
                taskAdapter.setFilteredTasks(this@Simpletask, query)
                runOnUiThread(afterOnUi)
            })
        }

        private fun updateQuickFilterDrawer() {
            updateFilterBar()
            val decoratedContexts = alfaSort(TodoApplication.todoList.contexts, TodoApplication.config.sortCaseSensitive, prefix = "-").map { "@$it" }
            val decoratedProjects = alfaSort(TodoApplication.todoList.projects, TodoApplication.config.sortCaseSensitive, prefix = "-").map { "+$it" }
            val drawerAdapter = DrawerAdapter(layoutInflater,
                    TodoApplication.config.listTerm,
                    decoratedContexts,
                    TodoApplication.config.tagTerm,
                    decoratedProjects)

            filter_drawer.adapter = drawerAdapter
            filter_drawer.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            filter_drawer.onItemClickListener = DrawerItemClickListener()


            TodoApplication.config.mainQuery.contexts.asSequence()
                    .map { drawerAdapter.getIndexOf("@$it") }
                    .filter { it != -1 }
                    .forEach { filter_drawer.setItemChecked(it, true) }

            TodoApplication.config.mainQuery.projects.asSequence()
                    .map { drawerAdapter.getIndexOf("+$it") }
                    .filter { it != -1 }
                    .forEach { filter_drawer.setItemChecked(it, true) }
            filter_drawer.setItemChecked(drawerAdapter.contextHeaderPosition, TodoApplication.config.mainQuery.contextsNot)
            filter_drawer.setItemChecked(drawerAdapter.projectsHeaderPosition, TodoApplication.config.mainQuery.projectsNot)
            filter_drawer.deferNotifyDataSetChanged()
        }

        private fun updateConnectivityIndicator() {
            // Show connectivity status indicator
            // Red -> changes pending
            // Yellow -> offline
            if (TodoApplication.config.changesPending) {
                pendingchanges.visibility = View.VISIBLE
                offline.visibility = View.GONE
            } else if (!FileStore.isOnline) {
                pendingchanges.visibility = View.GONE
                offline.visibility = View.VISIBLE
            } else {
                pendingchanges.visibility = View.GONE
                offline.visibility = View.GONE
            }
        }
    }

    private inner class DrawerItemClickListener : AdapterView.OnItemClickListener {

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int,
                                 id: Long) {
            val tags: ArrayList<String>
            val lv = parent as ListView
            val adapter = lv.adapter as DrawerAdapter
            val query = TodoApplication.config.mainQuery
            if (adapter.projectsHeaderPosition == position) {
                query.projectsNot = !query.projectsNot
            }
            if (adapter.contextHeaderPosition == position) {
                query.contextsNot = !query.contextsNot
            }

            tags = getCheckedItems(lv, true)
            val filteredContexts = ArrayList<String>()
            val filteredProjects = ArrayList<String>()

            for (tag in tags) {
                if (tag.startsWith("+")) {
                    filteredProjects.add(tag.substring(1))
                } else if (tag.startsWith("@")) {
                    filteredContexts.add(tag.substring(1))
                }
            }
            query.contexts = filteredContexts
            query.projects = filteredProjects

            TodoApplication.config.mainQuery = query
            if (!TodoApplication.config.hasKeepSelection) {
                TodoApplication.todoList.clearSelection()
            }
            uiHandler.forEvent(Event.QUICK_FILTER_ITEM_CLICK)
        }
    } }

enum class Event {
    TASK_LIST_CHANGED,
    FILTER_CHANGED,
    QUICK_FILTER_ITEM_CLICK,
    SAVED_FILTER_ITEM_CLICK,
    SAVED_FILTER_ADDED,
    SAVED_FILTER_UPDATED,
    SAVED_FILTER_DELETED,
    SAVED_FILTERS_IMPORTED,
    SAVED_FILTER_RENAMED,
    RESUME,
    FONT_SIZE_CHANGED,
    UPDATE_PENDING_CHANGES,
    CLEAR_FILTER,
    CLEAR_SELECTION
}

enum class Mode {
    SAVED_FILTER_DRAWER, QUICK_FILTER_DRAWER, SELECTION, MAIN
}

enum class Action {
    LINK,
    SMS,
    PHONE,
    MAIL
}