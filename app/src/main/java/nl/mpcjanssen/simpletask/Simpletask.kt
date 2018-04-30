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
import android.support.annotation.StyleableRes
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import hirondelle.date4j.DateTime
import kotlinx.android.synthetic.main.main.*
import nl.mpcjanssen.simpletask.adapters.DrawerAdapter
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.task.*
import nl.mpcjanssen.simpletask.task.TodoList.fileStoreQueue
import nl.mpcjanssen.simpletask.task.TodoList.todoQueue
import nl.mpcjanssen.simpletask.util.*
import org.json.JSONObject
import java.io.File
import java.util.*
import android.R.id as androidId

class Simpletask : ThemedNoActionBarActivity() {
    enum class Mode {
        NAV_DRAWER, FILTER_DRAWER, SELECTION, MAIN
    }

    private var options_menu: Menu? = null
    internal lateinit var m_app: TodoApplication

    internal var m_adapter: TaskAdapter? = null
    private var m_broadcastReceiver: BroadcastReceiver? = null
    private var localBroadcastManager: LocalBroadcastManager? = null

    // Drawer side
    private val NAV_DRAWER = GravityCompat.END
    private val QUICK_FILTER_DRAWER = GravityCompat.START

    private var m_drawerToggle: ActionBarDrawerToggle? = null
    private var m_savedInstanceState: Bundle? = null
    private var m_scrollPosition = 0

    private var log = Logger

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.info(TAG, "onCreate")
        m_app = application as TodoApplication
        m_savedInstanceState = savedInstanceState
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_ACTION_ARCHIVE)
        intentFilter.addAction(Constants.BROADCAST_ACTION_LOGOUT)
        intentFilter.addAction(Constants.BROADCAST_UPDATE_UI)
        intentFilter.addAction(Constants.BROADCAST_TASKLIST_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_SYNC_START)
        intentFilter.addAction(Constants.BROADCAST_THEME_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_DATEBAR_SIZE_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_SYNC_DONE)
        intentFilter.addAction(Constants.BROADCAST_UPDATE_PENDING_CHANGES)
        intentFilter.addAction(Constants.BROADCAST_HIGHLIGHT_SELECTION)

        setContentView(R.layout.main)

        localBroadcastManager = m_app.localBroadCastManager

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, receivedIntent: Intent) {
                if (receivedIntent.action == Constants.BROADCAST_ACTION_ARCHIVE) {
                    archiveTasks()
                } else {
                    if (receivedIntent.action == Constants.BROADCAST_ACTION_LOGOUT) {
                        log.info(TAG, "Logging out from Dropbox")
                        finish()
                        fileStoreQueue("Logout") {
                            try {
                                FileStore.logout()
                            } catch (e: Exception) {
                                log.error(TAG, "Error logging out.", e)
                            }
                            startLogin()
                        }
                    } else if (receivedIntent.action == Constants.BROADCAST_TASKLIST_CHANGED) {
                        log.info(TAG, "Tasklist changed, refiltering adapter")
                        m_adapter!!.setFilteredTasks(this@Simpletask, Config.mainQuery)
                    } else if (receivedIntent.action == Constants.BROADCAST_UPDATE_UI) {
                        log.info(TAG, "Updating UI because of broadcast")
                        refreshUI()
                    } else if (receivedIntent.action == Constants.BROADCAST_HIGHLIGHT_SELECTION) {
                        log.info(TAG, "Highligh selection")
                        m_adapter?.notifyDataSetChanged()
                        invalidateOptionsMenu()
                    } else if (receivedIntent.action == Constants.BROADCAST_SYNC_START) {
                        showListViewProgress(true)
                    } else if (receivedIntent.action == Constants.BROADCAST_SYNC_DONE) {
                        showListViewProgress(false)
                    } else if (receivedIntent.action == Constants.BROADCAST_UPDATE_PENDING_CHANGES) {
                        updateConnectivityIndicator()
                    } else if (receivedIntent.action == Constants.BROADCAST_THEME_CHANGED ||
                            receivedIntent.action == Constants.BROADCAST_DATEBAR_SIZE_CHANGED) {
                        recreate()
                    }
                }
            }
        }
        localBroadcastManager!!.registerReceiver(broadcastReceiver, intentFilter)
        m_broadcastReceiver = broadcastReceiver
        setSupportActionBar(main_actionbar)

        // Replace drawables if the theme is dark
        if (Config.isDarkTheme || Config.isBlackTheme) {
            actionbar_clear?.setImageResource(R.drawable.ic_close_white_24dp)
        }
        val versionCode = BuildConfig.VERSION_CODE
        if (m_app.isAuthenticated) {
            if (Config.latestChangelogShown < versionCode) {
                showChangelogOverlay(this)
                Config.latestChangelogShown = versionCode
            } else if (!Config.rightDrawerDemonstrated) {
                Config.rightDrawerDemonstrated = true
                openNavDrawer()
            }
        }
    }

    private fun refreshUI() {
        todoQueue("Refresh UI") {
            runOnUiThread {
                updateConnectivityIndicator()
                invalidateOptionsMenu()
                updateFilterBar()
                updateDrawers()
            }
        }
    }

    private fun openLuaConfig() {
        val i = Intent(this, ScriptConfigScreen::class.java)
        startActivity(i)
    }

    private fun showHelp() {
        val i = Intent(this, HelpScreen::class.java)
        startActivity(i)
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

    private fun selectedTasksAsString(): String {
        val result = ArrayList<String>()
        TodoList.selectedTasks.forEach { task ->
            val luaTxt = Interpreter.onDisplayCallback(Config.mainQuery.luaModule, task)
            result.add(luaTxt ?: task.inFileFormat())
        }
        return join(result, "\n")
    }

    private fun selectAllTasks() {
        val selectedTasks = m_adapter!!.visibleLines
                .filterNot(VisibleLine::header)
                .map { it.task!! }
        TodoList.selectTasks(selectedTasks)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        m_drawerToggle?.onConfigurationChanged(newConfig)
    }

    private fun handleIntent() {
        if (!m_app.isAuthenticated) {
            log.info(TAG, "handleIntent: not authenticated")
            startLogin()
            return
        }

        // Set the list's click listener
        filter_drawer?.onItemClickListener = DrawerItemClickListener()

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
            log.info(TAG, "handleIntent")
            currentIntent.extras?.let { extras ->
                extras.keySet().map { Pair(it, extras[it]) }.forEach { (key, value) ->
                    val debugString = value?.let { v ->
                        "$v (${v.javaClass.name})"
                    } ?: "<null>"
                    log.debug(TAG, "$key $debugString")
                }
            }
            val newQuery = Query(currentIntent, "mainui")
            Config.mainQuery = newQuery
            intent = newQuery.saveInIntent(intent)
            newQuery
        } else {
            // Set previous filters and sort
            log.info(TAG, "handleIntent: from m_prefs state")
            Config.mainQuery
        }

        val adapter = m_adapter ?: TaskAdapter(query, layoutInflater,
                completeAction = {
                    completeTasks(it)
                    // Update the tri state checkbox
                    if (activeMode() == Simpletask.Mode.SELECTION) invalidateOptionsMenu()
                    TodoList.notifyTasklistChanged(Config.todoFileName, m_app, false)
                },
                unCompleteAction = {
                    uncompleteTasks(it)
                    // Update the tri state checkbox
                    if (activeMode() == Simpletask.Mode.SELECTION) invalidateOptionsMenu()
                    TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
                },
                onClickAction = {
                    val newSelectedState = !TodoList.isSelected(it)
                    if (newSelectedState) {
                        TodoList.selectTasks(listOf(it))
                    } else {
                        TodoList.unSelectTasks(listOf(it))
                    }
                    invalidateOptionsMenu()
                },
                onLongClickAction = {
                    val links = ArrayList<String>()
                    val actions = ArrayList<String>()
                    val t = it
                    for (link in t.links) {
                        actions.add(Simpletask.ACTION_LINK)
                        links.add(link)
                    }
                    for (number in t.phoneNumbers) {
                        actions.add(Simpletask.ACTION_PHONE)
                        links.add(number)
                        actions.add(Simpletask.ACTION_SMS)
                        links.add(number)
                    }
                    for (mail in t.mailAddresses) {
                        actions.add(Simpletask.ACTION_MAIL)
                        links.add(mail)
                    }
                    if (actions.size != 0) {

                        val titles = ArrayList<String>()
                        for (i in links.indices) {
                            when (actions[i]) {
                                Simpletask.ACTION_SMS -> titles.add(i, getString(R.string.action_pop_up_sms) + links[i])
                                Simpletask.ACTION_PHONE -> titles.add(i, getString(R.string.action_pop_up_call) + links[i])
                                else -> titles.add(i, links[i])
                            }
                        }
                        val build = AlertDialog.Builder(this@Simpletask)
                        build.setTitle(R.string.task_action)
                        val titleArray = titles.toArray<String>(arrayOfNulls<String>(titles.size))
                        build.setItems(titleArray) { _, which ->
                            val actionIntent: Intent
                            val url = links[which]
                            log.info(Simpletask.TAG, "" + actions[which] + ": " + url)
                            when (actions[which]) {
                                Simpletask.ACTION_LINK -> when {
                                    url.startsWith("todo://") -> {
                                        val todoFolder = Config.todoFile.parentFile
                                        val newName = File(todoFolder, url.substring(7))
                                        m_app.switchTodoFile(newName.absolutePath)
                                    }
                                    url.startsWith("root://") -> {
                                        val rootFolder = Config.localFileRoot
                                        val file = File(rootFolder, url.substring(7))
                                        actionIntent = Intent(Intent.ACTION_VIEW)
                                        val contentUri = Uri.fromFile(file)
                                        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                                        actionIntent.setDataAndType(contentUri, mime)
                                        actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        startActivity(actionIntent)
                                    }
                                    else -> try {
                                        actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        startActivity(actionIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        log.info(Simpletask.TAG, "No handler for task action $url")
                                        showToastLong(TodoApplication.app, "No handler for $url")
                                    }
                                }
                                Simpletask.ACTION_PHONE -> {
                                    actionIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(url)))
                                    startActivity(actionIntent)
                                }
                                Simpletask.ACTION_SMS -> {
                                    actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + Uri.encode(url)))
                                    startActivity(actionIntent)
                                }
                                Simpletask.ACTION_MAIL -> {
                                    actionIntent = Intent(Intent.ACTION_SEND, Uri.parse(url))
                                    actionIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
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
        m_adapter = adapter
        showListViewProgress(true)
        if (currentIntent.hasExtra(Constants.INTENT_SELECTED_TASK_LINE)) {
            TodoList.todoQueue("Selection from intent") {
                val position = currentIntent.getIntExtra(Constants.INTENT_SELECTED_TASK_LINE, -1)
                currentIntent.removeExtra(Constants.INTENT_SELECTED_TASK_LINE)
                setIntent(currentIntent)
                if (position > -1) {
                    val itemAtPosition = TodoList.getTaskAt(position)
                    itemAtPosition?.let {
                        TodoList.clearSelection()
                        TodoList.selectTasks(listOf(itemAtPosition))
                    }

                }
            }
        }
        m_adapter!!.setFilteredTasks(this, query)


        listView?.layoutManager = LinearLayoutManager(this)
        listView?.adapter = this.m_adapter

        fab.setOnClickListener { startAddTaskActivity() }

        // If we were started from the widget, select the pushed task
        // next scroll to the first selected item


        TodoActionQueue.add("Scroll selection", Runnable {
            val selection = TodoList.selectedTasks
            if (selection.isNotEmpty()) {
                val selectedTask = selection[0]
                m_scrollPosition = adapter.getPosition(selectedTask)
            }
        })
    }

    private fun updateConnectivityIndicator() {
        // Show connectivity status indicator
        // Red -> changes pending
        // Yellow -> offline
        if (Config.changesPending) {
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

    private fun updateFilterBar() {

        actionbar.visibility = when {
            Config.mainQuery.hasFilter() -> View.VISIBLE
            else -> View.GONE
        }
        TodoList.todoQueue("Update applyFilter bar") {
            runOnUiThread {
                val count = m_adapter?.countVisibleTasks ?: 0
                val total = m_adapter?.countTotalTasks ?: 0
                filter_text.text = Config.mainQuery.getTitle(
                        count,
                        total,
                        getText(R.string.priority_prompt),
                        Config.tagTerm,
                        Config.listTerm,
                        getText(R.string.search),
                        getText(R.string.script),
                        getText(R.string.title_filter_applied),
                        getText(R.string.no_filter))
            }
        }
    }

    private fun startLogin() {
        m_app.startLogin(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        m_broadcastReceiver?.let {
            localBroadcastManager!!.unregisterReceiver(it)
        }
    }

    override fun onResume() {
        super.onResume()
        log.info(TAG, "onResume")
        TodoList.reload(TodoApplication.app, reason = "Main activity resume")
        handleIntent()
        refreshUI()
    }

    override fun onPause() {
        (listView?.layoutManager as LinearLayoutManager?)?.let { manager ->
            val position = manager.findFirstVisibleItemPosition()
            val firstItemView = manager.findViewByPosition(position)
            val offset = firstItemView?.top ?: 0
            Logger.info(TAG, "Saving scroll offset $position, $offset")
            Config.lastScrollPosition = position
            Config.lastScrollOffset = offset
        }
        super.onPause()
    }

    @SuppressLint("Recycle")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        log.info(TAG, "Recreating options menu")
        this.options_menu = menu

        val inflater = menuInflater
        val toggle = m_drawerToggle ?: return super.onCreateOptionsMenu(menu)
        val actionBar = supportActionBar ?: return super.onCreateOptionsMenu(menu)

        when (activeMode()) {
            Mode.NAV_DRAWER -> {
                inflater.inflate(R.menu.nav_drawer, menu)
                setTitle(R.string.filter_saved_prompt)
            }
            Mode.FILTER_DRAWER -> {
                inflater.inflate(R.menu.filter_drawer, menu)
                setTitle(R.string.title_filter_drawer)
            }
            Mode.SELECTION -> {
                val actionColor = ContextCompat.getDrawable(this, R.color.gray74)
                actionBar.setBackgroundDrawable(actionColor)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.statusBarColor = ContextCompat.getColor(this, R.color.gray87)
                }

                inflater.inflate(R.menu.task_context_actionbar, menu)
                title = "${TodoList.numSelected()}"
                toggle.isDrawerIndicatorEnabled = false
                fab.visibility = View.GONE
                toolbar.setOnMenuItemClickListener { item ->
                    onOptionsItemSelected(item)
                }
                toolbar.visibility = View.VISIBLE
                toolbar.menu.clear()
                inflater.inflate(R.menu.task_context, toolbar.menu)

                val cbItem = toolbar.menu.findItem(R.id.multicomplete_checkbox)
                val selectedTasks = TodoList.selectedTasks
                val initialCompleteTasks = ArrayList<Task>()
                val initialIncompleteTasks = ArrayList<Task>()

                val first: Boolean? = selectedTasks.getOrNull(0)?.isCompleted()

                val cbState: Boolean? = selectedTasks.fold(first) { stateSoFar, task ->
                    val completed = task.isCompleted()
                    if (completed) {
                        initialCompleteTasks.add(task)
                    } else {
                        initialIncompleteTasks.add(task)
                    }
                    stateSoFar?.takeIf { it == completed }
                }
                when (cbState) {
                    null -> cbItem.setIcon(R.drawable.ic_indeterminate_check_box_white_24dp)
                    false -> cbItem.setIcon(R.drawable.ic_check_box_outline_blank_white_24dp)
                    true -> cbItem.setIcon(R.drawable.ic_check_box_white_24dp)
                }

                cbItem.setOnMenuItemClickListener { _ ->
                    log.info(TAG, "Clicked on completion checkbox, state: $cbState")
                    when (cbState) {
                        false -> completeTasks(selectedTasks)
                        true -> uncompleteTasks(selectedTasks)
                        null -> {
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
                            }
                        }
                    }
                    return@setOnMenuItemClickListener true
                }

                selection_fab.visibility = View.VISIBLE
                selection_fab.setOnClickListener {
                    createCalendarAppointment(selectedTasks)
                }
            }

            Mode.MAIN -> {
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
                if (Config.showTodoPath) {
                    title = Config.todoFileName.replace("([^/])[^/]*/".toRegex(), "$1/")
                } else {
                    setTitle(R.string.app_label)
                }
                toggle.isDrawerIndicatorEnabled = true
                fab.visibility = View.VISIBLE
                selection_fab.visibility = View.GONE
                toolbar.visibility = View.GONE
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * isDrawerOpen only returns true only if m_drawerLayout != null, so
     * if this returns either _DRAWER, m_drawerLayout!!. calls are safe to make
     */
    private fun activeMode(): Mode {
        if (isDrawerOpen(NAV_DRAWER)) return Mode.NAV_DRAWER
        if (isDrawerOpen(QUICK_FILTER_DRAWER)) return Mode.FILTER_DRAWER
        if (TodoList.selectedTasks.isNotEmpty()) return Mode.SELECTION
        return Mode.MAIN
    }

    private fun isDrawerOpen(drawer: Int): Boolean {
        if (drawer_layout == null) {
            log.warn(TAG, "Layout was null")
            return false
        }
        return drawer_layout.isDrawerOpen(drawer)
    }

    private fun closeDrawer(drawer: Int) {
        drawer_layout?.closeDrawer(drawer)
    }

    private fun openNavDrawer() {
        closeDrawer(QUICK_FILTER_DRAWER)
        if (!isDrawerOpen(NAV_DRAWER)) {
            drawer_layout.openDrawer(NAV_DRAWER)
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
        val activeTextSearch = Config.mainQuery.search
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
                    val query = Config.mainQuery
                    query.search = newText
                    Config.mainQuery = query
                    m_adapter?.setFilteredTasks(this@Simpletask, query)
                    updateFilterBar()
                }
                return true
            }
        })
    }

    private fun prioritizeTasks(tasks: List<Task>) {
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
            TodoList.prioritize(tasks, priority)
            TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
        })
        builder.show()

    }

    private fun completeTasks(task: Task) {
        val tasks = ArrayList<Task>()
        tasks.add(task)
        completeTasks(tasks)
    }

    private fun completeTasks(tasks: List<Task>) {
        TodoList.complete(tasks, Config.hasKeepPrio, Config.hasAppendAtEnd)
        if (Config.isAutoArchive) {
            archiveTasks()
        }
        TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
    }

    private fun uncompleteTasks(task: Task) {
        val tasks = ArrayList<Task>()
        tasks.add(task)
        uncompleteTasks(tasks)
    }

    private fun uncompleteTasks(tasks: List<Task>) {
        TodoList.uncomplete(tasks)
        TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
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
                        TodoList.defer(date.format(Constants.DATE_FORMAT), tasks, dateType)
                        TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
                    },
                            today.year!!,
                            today.month!! - 1,
                            today.day!!)

                    val showCalendar = Config.showCalendar
                    dialog.datePicker.calendarViewShown = showCalendar
                    dialog.datePicker.spinnersShown = !showCalendar
                    dialog.show()
                } else {

                    TodoList.defer(input, tasks, dateType)
                    TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)

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
            TodoList.removeAll(tasks)
            TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
            invalidateOptionsMenu()
        }

        showConfirmationDialog(this, R.string.delete_task_message, delete, title)
    }

    private fun archiveTasks() {
        archiveTasks(null, false)
    }

    private fun archiveTasks(tasks: List<Task>?, showDialog: Boolean = true) {
        val archiveAction = {
            if (Config.todoFileName == m_app.doneFileName) {
                showToastShort(this, "You have the done.txt file opened.")
            }
            TodoList.archive(Config.todoFileName, m_app.doneFileName, tasks, Config.eol)
            invalidateOptionsMenu()
        }

        if (showDialog) {
            val numTasks = (tasks ?: TodoList.completedTasks).size.toString()
            val title = getString(R.string.archive_task_title)
                    .replaceFirst(Regex("%s"), numTasks)
            val archive = DialogInterface.OnClickListener { _, _ -> archiveAction() }
            showConfirmationDialog(this, R.string.delete_task_message, archive, title)
        } else {
            archiveAction()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        log.info(TAG, "onMenuItemSelected: " + item.itemId)
        val checkedTasks = TodoList.selectedTasks
        when (item.itemId) {
            androidId.home -> {
                when (activeMode()) {
                    Mode.NAV_DRAWER -> {
                        closeDrawer(NAV_DRAWER)
                    }
                    Mode.FILTER_DRAWER -> {
                        closeDrawer(QUICK_FILTER_DRAWER)
                    }
                    Mode.SELECTION -> {
                        closeSelectionMode()
                    }
                    Mode.MAIN -> {
                        val toggle = m_drawerToggle ?: return true
                        toggle.onOptionsItemSelected(item)
                    }
                }
            }
            R.id.search -> {
            }
            R.id.preferences -> startPreferencesActivity()
            R.id.filter -> startFilterActivity()
            R.id.context_delete -> deleteTasks(checkedTasks)
            R.id.context_select_all -> selectAllTasks()
            R.id.share -> {
                val shareText = TodoList.todoItems.map(Task::inFileFormat).joinToString(separator = "\n")
                shareText(this@Simpletask, "Simpletask list", shareText)
            }
            R.id.context_share -> {
                val shareText = selectedTasksAsString()
                shareText(this@Simpletask, "Simpletask tasks", shareText)
            }
            R.id.context_archive -> archiveTasks(checkedTasks)
            R.id.help -> showHelp()
            R.id.open_lua -> openLuaConfig()
            R.id.sync -> {
                broadcastFileSync(TodoApplication.app.localBroadCastManager)
            }
            R.id.archive -> archiveTasks()
            R.id.open_file -> m_app.browseForNewFile(this)
            R.id.history -> startActivity(Intent(this, HistoryScreen::class.java))
            R.id.btn_filter_add -> onAddFilterClick()
            R.id.clear_filter -> clearFilter()
            R.id.update -> startAddTaskActivity()
            R.id.defer_due -> deferTasks(checkedTasks, DateType.DUE)
            R.id.defer_threshold -> deferTasks(checkedTasks, DateType.THRESHOLD)
            R.id.priority -> prioritizeTasks(checkedTasks)
            R.id.update_lists -> updateLists(checkedTasks)
            R.id.update_tags -> updateTags(checkedTasks)
            R.id.menu_export_filter_export -> exportFilters(File(Config.todoFile.parent, "saved_filters.txt"))
            R.id.menu_export_filter_import -> importFilters(File(Config.todoFile.parent, "saved_filters.txt"))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun createCalendarAppointment(checkedTasks: List<Task>) {
        var calendarTitle = getString(R.string.calendar_title)
        var calendarDescription = ""
        if (checkedTasks.size == 1) {
            // Set the task as title
            val task = checkedTasks[0]
            val luaTxt = Interpreter.onDisplayCallback(Config.mainQuery.luaModule, task)
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
        val dueDate = checkedTasks[0].dueDate
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
        log.info(TAG, "Starting addTask activity")

        TodoList.editTasks(this, TodoList.selectedTasks, Config.mainQuery.prefill)
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

    private fun importFilters(importFile: File) {
        val r = Runnable {
            try {
                FileStore.readFile(importFile.canonicalPath) { contents ->
                    val jsonFilters = JSONObject(contents)
                    jsonFilters.keys().forEach { name ->
                        val json = jsonFilters.getJSONObject(name)
                        val newQuery = Query(json, luaModule = "mainui")
                        QueryStore.save(newQuery, name)
                    }
                    localBroadcastManager?.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_UI))
                    showToastShort(this, R.string.saved_filters_imported) }
            } catch (e: Exception) {
                // Need to catch generic exception because Dropbox errors don't inherit from IOException
                log.error(TAG, "Import filters, cant read file ${importFile.canonicalPath}", e)
                showToastLong(this, "Error reading file ${importFile.canonicalPath}")
            }
        }
        Thread(r).start()
    }

    private fun exportFilters(exportFile: File) {
        val queries = QueryStore.ids().map {
            QueryStore.get(it)
        }
        val jsonFilters = queries.fold(JSONObject()) { acc, query ->
            acc.put(query.name, query.query.saveInJSON())
        }
        val r = Runnable {
            try {
                FileStore.writeFile(exportFile, jsonFilters.toString(2))
                showToastShort(this, R.string.saved_filters_exported)
            } catch (e: Exception) {
                log.error(TAG, "Export filters failed", e)
                showToastLong(this, "Error exporting filters")
            }
        }
        Thread(r).start()
    }

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
        input.setText(Config.mainQuery.proposedName)

        alert.setPositiveButton("Ok") { _, _ ->
            val value = input.text?.toString()?.takeIf { it.isNotBlank() }
            value?.let {
                QueryStore.save(Config.mainQuery, it)
                updateNavDrawer()
            }
            value ?: showToastShort(applicationContext, R.string.filter_name_empty)
        }

        alert.setNegativeButton("Cancel") { _, _ -> }
        alert.show()
    }

    override fun onBackPressed() {
        when (activeMode()) {
            Mode.NAV_DRAWER -> {
                closeDrawer(NAV_DRAWER)
            }
            Mode.FILTER_DRAWER -> {
                closeDrawer(QUICK_FILTER_DRAWER)
            }
            Mode.SELECTION -> {
                closeSelectionMode()
            }
            Mode.MAIN -> {
                if (!Config.backClearsFilter || !Config.mainQuery.hasFilter()) {
                    return super.onBackPressed()
                }
                clearFilter()
                onNewIntent(intent)
            }
        }
        return
    }

    private fun closeSelectionMode() {
        TodoList.clearSelection()
        invalidateOptionsMenu()
        m_adapter?.setFilteredTasks(this, Config.mainQuery)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when {
            Intent.ACTION_SEARCH == intent.action -> {
                val currentIntent = getIntent()
                currentIntent.putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY))
                setIntent(currentIntent)
                options_menu?.findItem(R.id.search)?.collapseActionView() ?: return

            }
            CalendarContract.ACTION_HANDLE_CUSTOM_EVENT == intent.action -> // Uri uri = Uri.parse(intent.getStringExtra(CalendarContract.EXTRA_CUSTOM_APP_URI));
                log.warn(TAG, "Not implemented search")
            intent.extras != null -> // Only change intent if it actually contains a applyFilter
                setIntent(intent)
        }
        Config.lastScrollPosition = -1
        log.info(TAG, "onNewIntent: $intent")

    }

    private fun clearFilter() {
        Config.mainQuery = Config.mainQuery.clear()
        intent = Config.mainQuery.saveInIntent(intent)
        broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)
        broadcastRefreshUI(TodoApplication.app.localBroadCastManager)
    }

    private fun updateDrawers() {
        todoQueue("Update drawers") {
            runOnUiThread {
                updateFilterDrawer()
                updateNavDrawer()
            }
        }
    }

    private fun updateNavDrawer() {
        val idQueryPairs = QueryStore.ids().map { Pair(it, QueryStore.get(it)) }.toList()
        val hasQueries = !idQueryPairs.isEmpty()
        val queries = idQueryPairs.sortedBy { it.second.name }
        val names = if (hasQueries) {
            queries.map { it.second.name }
        } else {
            val result = ArrayList<String>()
            result.add(getString(R.string.nav_drawer_hint))
            result
        }
        nav_drawer.adapter = ArrayAdapter(this, R.layout.drawer_list_item, names)
        if (hasQueries) {
            nav_drawer.choiceMode = AbsListView.CHOICE_MODE_NONE
            nav_drawer.isLongClickable = true
            nav_drawer.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                queries[position].let {
                    Config.mainQuery = it.second.query
                }
                closeDrawer(NAV_DRAWER)
                broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)
                broadcastRefreshUI(TodoApplication.app.localBroadCastManager)
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
                        R.id.menu_saved_filter_update -> updateSavedQuery(query.second, Config.mainQuery)
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

    fun createFilterShortcut(namedQuery: NamedQuery) {
        val target = Intent(Constants.INTENT_START_FILTER)
        namedQuery.query.saveInIntent(target)
        createShortcut(this, "simpletaskLauncher", namedQuery.name,  R.drawable.ic_launcher, target)
    }

    private fun deleteSavedQuery(id: String) {
        QueryStore.delete(id)
        updateNavDrawer()
    }

    private fun updateSavedQuery(oldQuery: NamedQuery, newQuery: Query) {
        QueryStore.save(newQuery, oldQuery.name)
        updateNavDrawer()
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
                updateNavDrawer()
            } ?: showToastShort(applicationContext, R.string.filter_name_empty)
        }

        alert.setNegativeButton("Cancel") { _, _ -> }

        alert.show()
    }

    private fun updateFilterDrawer() {
        val decoratedContexts = alfaSort(TodoList.contexts, Config.sortCaseSensitive, prefix = "-").map { "@" + it }
        val decoratedProjects = alfaSort(TodoList.projects, Config.sortCaseSensitive, prefix = "-").map { "+" + it }
        val drawerAdapter = DrawerAdapter(layoutInflater,
                Config.listTerm,
                decoratedContexts,
                Config.tagTerm,
                decoratedProjects)

        filter_drawer.adapter = drawerAdapter
        filter_drawer.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
        filter_drawer.onItemClickListener = DrawerItemClickListener()

        Config.mainQuery.contexts
                .map { drawerAdapter.getIndexOf("@" + it) }
                .filter { it != -1 }
                .forEach { filter_drawer.setItemChecked(it, true) }

        Config.mainQuery.projects
                .map { drawerAdapter.getIndexOf("+" + it) }
                .filter { it != -1 }
                .forEach { filter_drawer.setItemChecked(it, true) }
        filter_drawer.setItemChecked(drawerAdapter.contextHeaderPosition, Config.mainQuery.contextsNot)
        filter_drawer.setItemChecked(drawerAdapter.projectsHeaderPosition, Config.mainQuery.projectsNot)
        filter_drawer.deferNotifyDataSetChanged()
    }

    fun startFilterActivity() {
        val i = Intent(this, FilterActivity::class.java)
        Config.mainQuery.saveInIntent(i)
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
                Config.listTerm,
                checkedTasks,
                TodoList.contexts,
                Task::lists,
                Task::addList,
                Task::removeList
        ) {
            TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
        }
    }

    private fun updateTags(checkedTasks: List<Task>) {
        updateItemsDialog(
                Config.tagTerm,
                checkedTasks,
                TodoList.projects,
                Task::tags,
                Task::addTag,
                Task::removeTag
        ) {
            TodoList.notifyTasklistChanged(Config.todoFileName, m_app, true)
        }
    }

    private inner class DrawerItemClickListener : AdapterView.OnItemClickListener {

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int,
                                 id: Long) {
            val tags: ArrayList<String>
            val lv = parent as ListView
            val adapter = lv.adapter as DrawerAdapter
            val query = Config.mainQuery
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

            Config.mainQuery = query
            if (!Config.hasKeepSelection) {
                TodoList.clearSelection()
            }

            broadcastTasklistChanged(TodoApplication.app.localBroadCastManager)
            broadcastRefreshUI(TodoApplication.app.localBroadCastManager)
        }
    }

    companion object  {
        private val REQUEST_PREFERENCES = 1

        private val ACTION_LINK = "link"
        private val ACTION_SMS = "sms"
        private val ACTION_PHONE = "phone"
        private val ACTION_MAIL = "mail"

        val URI_BASE = Uri.fromParts("Simpletask", "", null)!!
        val URI_SEARCH = Uri.withAppendedPath(URI_BASE, "search")!!
        private val TAG = "Simpletask"
    }
}
