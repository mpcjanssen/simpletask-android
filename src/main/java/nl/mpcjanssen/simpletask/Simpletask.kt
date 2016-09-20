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
import android.app.Activity
import android.app.DatePickerDialog
import android.app.SearchManager
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.MenuItemCompat.OnActionExpandListener
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.SpannableString
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.adapters.DrawerAdapter
import nl.mpcjanssen.simpletask.adapters.ItemDialogAdapter
import nl.mpcjanssen.simpletask.dao.gentodo.TodoItem
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.task.Priority
import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import android.R.id as androidId


class Simpletask : ThemedActivity() {

    enum class Mode {
        NAV_DRAWER, FILTER_DRAWER, SELECTION, MAIN
    }

    var textSize: Float = 14.0F

    internal var options_menu: Menu? = null
    internal lateinit var m_app: TodoApplication
    internal var mFilter: ActiveFilter? = null
    internal var m_adapter: TaskAdapter? = null
    private var m_broadcastReceiver: BroadcastReceiver? = null
    private var localBroadcastManager: LocalBroadcastManager? = null

    // Drawer side
    private val NAV_DRAWER = GravityCompat.END
    private val FILTER_DRAWER = GravityCompat.START

    // Drawer vars
    private var m_filterDrawerList: ListView? = null
    private var m_navDrawerList: ListView? = null
    private var m_drawerLayout: DrawerLayout? = null
    private var m_drawerToggle: ActionBarDrawerToggle? = null
    private var m_savedInstanceState: Bundle? = null
    internal var m_scrollPosition = 0

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
        intentFilter.addAction(Constants.BROADCAST_SYNC_START)
        intentFilter.addAction(Constants.BROADCAST_THEME_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_DATEBAR_SIZE_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_SYNC_DONE)
        intentFilter.addAction(Constants.BROADCAST_UPDATE_PENDING_CHANGES)
        intentFilter.addAction(Constants.BROADCAST_HIGHLIGHT_SELECTION)

        textSize = Config.tasklistTextSize ?: textSize
        log.info(TAG, "Text size = $textSize")

        localBroadcastManager = m_app.localBroadCastManager

        m_broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, receivedIntent: Intent) {
                if (receivedIntent.action == Constants.BROADCAST_ACTION_ARCHIVE) {
                    archiveTasks(null, false)
                } else {
                    if (receivedIntent.action == Constants.BROADCAST_ACTION_LOGOUT) {
                        log.info(TAG, "Logging out from Dropbox")
                        FileStore.logout()
                        finish()
                        startActivity(intent)
                    } else if (receivedIntent.action == Constants.BROADCAST_UPDATE_UI) {
                        log.info(TAG, "Updating UI because of broadcast")
                        textSize = Config.tasklistTextSize ?: textSize
                        if (m_adapter == null) {
                            return
                        }
                        m_adapter!!.setFilteredTasks()
                        updateDrawers()
                    } else if (receivedIntent.action == Constants.BROADCAST_SYNC_START) {
                        showListViewProgress(true)
                    } else if (receivedIntent.action == Constants.BROADCAST_SYNC_DONE) {
                        showListViewProgress(false)
                    } else if (receivedIntent.action == Constants.BROADCAST_UPDATE_PENDING_CHANGES) {
                        updateConnectivityIndicator()
                    } else if (receivedIntent.action == Constants.BROADCAST_HIGHLIGHT_SELECTION) {
                        handleIntent()
                    } else if ( receivedIntent.action == Constants.BROADCAST_THEME_CHANGED ||
                            receivedIntent.action == Constants.BROADCAST_DATEBAR_SIZE_CHANGED) {
                        recreate()
                    }
                }
            }
        }
        localBroadcastManager!!.registerReceiver(m_broadcastReceiver, intentFilter)

        // Landscape mode
        if (Config.hasLandscapeDrawers()) {
            setContentView(R.layout.main_landscape)
            val toolbar = findViewById(R.id.saved_filter_toolbar) as Toolbar
            toolbar.setOnMenuItemClickListener{ item ->
                onOptionsItemSelected(item)
            }
            val menu = toolbar.menu
            menu.clear()
            val inflater = menuInflater
            inflater.inflate(R.menu.nav_drawer, toolbar.menu)
        } else {
            setContentView(R.layout.main)
            val toolbar = findViewById(R.id.main_actionbar) as Toolbar
            setSupportActionBar(toolbar);
        }

        // Replace drawables if the theme is dark
        if (Config.isDarkTheme) {
            val actionBarClear = findViewById(R.id.actionbar_clear) as ImageView?
            actionBarClear?.setImageResource(R.drawable.ic_close_white_24dp)
        }
        val versionCode = BuildConfig.VERSION_CODE
        if (m_app.isAuthenticated && Config.latestChangelogShown < versionCode) {
            showChangelogOverlay(this)
            Config.latestChangelogShown = versionCode
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SHARE_PARTS -> if (resultCode != Activity.RESULT_CANCELED) {
                val flags = resultCode - Activity.RESULT_FIRST_USER
                shareTodoList(flags)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> m_app.switchTodoFile(Config.todoFileName)
        }
    }

    private fun openLuaConfig() {
        val i = Intent(this, LuaConfigScreen::class.java)
        startActivity(i)
    }
    private fun showHelp() {
        val i = Intent(this, HelpScreen::class.java)
        startActivity(i)
    }

    override fun onSearchRequested(): Boolean {
        if (options_menu == null) {
            return false
        }
        val searchMenuItem = options_menu!!.findItem(R.id.search)
        MenuItemCompat.expandActionView(searchMenuItem)

        return true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (m_drawerToggle != null) {
            m_drawerToggle!!.syncState()
        }
    }

    private fun selectedTasksAsString(): String {
        val result = ArrayList<String>()
        TodoList.selectedTasks.forEach {
            result.add(it.task.inFileFormat())
        }
        return join(result, "\n")
    }

    private fun selectAllTasks() {
        val selectedTasks = ArrayList<TodoItem>()
        var count = 0
        for (visibleLine in m_adapter!!.visibleLines) {
            // Only check tasks that are not checked yet
            // and skip headers
            // This prevents double counting in the CAB title
            if (!visibleLine.header) {
                selectedTasks.add(visibleLine.task!!)
                count++
            }
        }
        TodoList.selectTodoItems(selectedTasks)
        invalidateOptionsMenu()
        m_adapter?.notifyDataSetChanged()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (m_drawerToggle != null) {
            m_drawerToggle!!.onConfigurationChanged(newConfig)
        }
    }


    private fun handleIntent() {
        if (!m_app.isAuthenticated) {
            log.info(TAG, "handleIntent: not authenticated")
            startLogin()
            return
        }

        // Check if we have SDCard permission for cloudless
        if (!FileStore.getWritePermission(this, REQUEST_PERMISSION)) {
            return
        }

        mFilter = ActiveFilter(FilterOptions(luaModule = "mainui", showSelected = true))

        m_filterDrawerList = findViewById(R.id.filter_drawer) as ListView
        m_navDrawerList = findViewById(R.id.nav_drawer) as ListView

        m_drawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout?

        // Set the list's click listener
        m_filterDrawerList!!.onItemClickListener = DrawerItemClickListener()

        if (m_drawerLayout != null) {
            m_drawerToggle = object : ActionBarDrawerToggle(this, /* host Activity */
                    m_drawerLayout, /* DrawerLayout object */
                    R.string.changelist, /* "open drawer" description */
                    R.string.app_label /* "close drawer" description */) {

                /**
                 * Called when a drawer has settled in a completely closed
                 * state.
                 */
                override fun onDrawerClosed(view: View?) {
                    invalidateOptionsMenu()
                }

                /** Called when a drawer has settled in a completely open state.  */
                override fun onDrawerOpened(drawerView: View?) {
                    invalidateOptionsMenu()
                }
            }

            // Set the drawer toggle as the DrawerListener
            val toggle = m_drawerToggle as ActionBarDrawerToggle
            m_drawerLayout!!.removeDrawerListener(toggle)
            m_drawerLayout!!.addDrawerListener(toggle)
            val actionBar = supportActionBar
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.setHomeButtonEnabled(true)
                m_drawerToggle!!.isDrawerIndicatorEnabled = true
            }
            m_drawerToggle!!.syncState()
        }

        // Show search or filter results
        val intent = intent
        if (Constants.INTENT_START_FILTER == intent.action) {
            mFilter!!.initFromIntent(intent)
            log.info(TAG, "handleIntent: launched with filter" + mFilter!!)
            val extras = intent.extras
            if (extras != null) {
                for (key in extras.keySet()) {
                    val value = extras.get(key)
                    if (value != null) {
                        log.debug(TAG, "%s %s (%s)".format(key, value.toString(), value.javaClass.name))
                    } else {
                        log.debug(TAG, "%s %s)".format(key, "<null>"))
                    }

                }

            }
            log.info(TAG, "handleIntent: saving filter in prefs")
            mFilter!!.saveInPrefs(Config.prefs)
        } else {
            // Set previous filters and sort
            log.info(TAG, "handleIntent: from m_prefs state")
            mFilter!!.initFromPrefs(Config.prefs)
        }

        // Initialize Adapter
        if (m_adapter == null) {
            m_adapter = TaskAdapter(layoutInflater)
        }
        m_adapter!!.setFilteredTasks()

        listView?.layoutManager = LinearLayoutManager(this)

        listView?.adapter = this.m_adapter



        // If we were started from the widget, select the pushed task
        // and scroll to its position
        if (intent.hasExtra(Constants.INTENT_SELECTED_TASK_LINE)) {
            val line = intent.getLongExtra(Constants.INTENT_SELECTED_TASK_LINE, -1)
            intent.removeExtra(Constants.INTENT_SELECTED_TASK_LINE)
            setIntent(intent)
            if (!line.equals(-1)) {
                TodoList.clearSelection()
                TodoList.selectLine(line)
                localBroadcastManager?.sendBroadcast(Intent(Constants.BROADCAST_HIGHLIGHT_SELECTION))
            }
        }
        val selection = TodoList.selectedTasks
        if (selection.size > 0) {
            val selectedTask = selection[0]
            m_scrollPosition = m_adapter!!.getPosition(selectedTask)

        }

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { startAddTaskActivity() }

        updateDrawers()
    }

    private fun updateConnectivityIndicator() {
        // Show connectivity status indicator
        // Red -> changes pending
        // Yellow -> offline
        val pendingChangesIndicator = findViewById(R.id.pendingchanges)
        val offlineIndicator = findViewById(R.id.offline)
        if (FileStore.changesPending()) {
            pendingChangesIndicator?.visibility = View.VISIBLE
            offlineIndicator?.visibility = View.GONE
        } else if (!FileStore.isOnline){
            pendingChangesIndicator?.visibility = View.GONE
            offlineIndicator?.visibility = View.VISIBLE
        } else {
            pendingChangesIndicator?.visibility = View.GONE
            offlineIndicator?.visibility = View.GONE
        }
    }

    private fun updateFilterBar() {

        val actionbar = findViewById(R.id.actionbar) as LinearLayout
        val filterText = findViewById(R.id.filter_text) as TextView
        if (mFilter!!.hasFilter()) {
            actionbar.visibility = View.VISIBLE
        } else {
            actionbar.visibility = View.GONE
        }
        val count = if (m_adapter != null) m_adapter!!.countVisibleTodoItems else 0
        val total = TodoList.getTaskCount()

        filterText.text = mFilter!!.getTitle(
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

    private fun startLogin() {
        m_app.startLogin(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager!!.unregisterReceiver(m_broadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        FileStore.pause(false)
        handleIntent()
    }

    override fun onPause() {
        FileStore.pause(true)
        val manager = listView?.layoutManager as LinearLayoutManager?
        if (manager!=null) {
            val position = manager.findFirstVisibleItemPosition()
            val firstItemView = manager.findViewByPosition(position)
            val offset = firstItemView?.top ?: 0
            Logger.info(TAG, "Saving scroll offset $position, $offset")
            Config.lastScrollPosition = position
            Config.lastScrollOffset = offset
        }
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        log.info(TAG, "Recreating options menu")
        this.options_menu = menu

        val inflater = menuInflater
        val fab = findViewById(R.id.fab) as FloatingActionButton
        val toolbar = findViewById(R.id.toolbar) as Toolbar
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
                val actionColor = ContextCompat.getDrawable(this, R.color.gray81)
                actionBar.setBackgroundDrawable(actionColor)

                /* window.setStatusBarColor(R.color.gray87) */

                inflater.inflate(R.menu.task_context_actionbar, menu)
                title = "${TodoList.numSelected()}"
                toggle.setDrawerIndicatorEnabled(false)
                fab.visibility = View.GONE
                toolbar.setOnMenuItemClickListener { item ->
                    onOptionsItemSelected(item)
                }
                toolbar.visibility = View.VISIBLE
                toolbar.menu.clear()
                inflater.inflate(R.menu.task_context, toolbar.menu)
            }
            Mode.MAIN -> {
                val a = this.obtainStyledAttributes(intArrayOf(R.attr.colorPrimary))
                val colorPrimary = ContextCompat.getDrawable(this, a.getResourceId(0, 0))
                actionBar.setBackgroundDrawable(colorPrimary)

                /* val b = this.obtainStyledAttributes(intArrayOf(R.attr.colorPrimaryDark)) */
                /* window.setStatusBarColor(b.getResourceId(0, R.color.simple_primary_dark)) */
                
                inflater.inflate(R.menu.main, menu)

                if (!FileStore.supportsSync()) {
                    val mItem = menu.findItem(R.id.sync)
                    mItem.isVisible = false
                }
                populateSearch(menu)
                if (Config.showTodoPath()) {
                    title = Config.todoFileName.replace("([^/])[^/]*/".toRegex(), "$1/")
                } else {
                    setTitle(R.string.app_label)
                }
                toggle.setDrawerIndicatorEnabled(true)
                fab.visibility = View.VISIBLE
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
        if (isDrawerOpen(FILTER_DRAWER)) return Mode.FILTER_DRAWER
        if (TodoList.selectedTasks.size!=0) return Mode.SELECTION
        return Mode.MAIN
    }

    private fun isDrawerOpen(drawer: Int): Boolean {
        val layout = m_drawerLayout
        if (layout == null) {
            log.warn(TAG, "Layout was null")
            return false
        }
        return layout.isDrawerOpen(drawer)
    }

    private fun closeDrawer(drawer: Int) {
        val layout = m_drawerLayout ?: return
        layout.closeDrawer(drawer)
    }

    private fun populateSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchMenu = menu.findItem(R.id.search)

        val searchView = searchMenu.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        searchView.setIconifiedByDefault(false)
        MenuItemCompat.setOnActionExpandListener(searchMenu, object : OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // Do something when collapsed
                return true  // Return true to collapse action view
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                //get focus
                item.actionView.requestFocus()
                //get input method
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
                return true  // Return true to expand action view
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
                    if (mFilter == null) {
                        mFilter = ActiveFilter(FilterOptions(luaModule = "mainui", showSelected = true))
                    }
                    mFilter!!.search = newText
                    mFilter!!.saveInPrefs(Config.prefs)
                    if (m_adapter != null) {
                        m_adapter!!.setFilteredTasks()
                    }
                }
                return true
            }
        })
    }

/*    private fun getTaskAt(pos: Int): TodoListItem? {
        if (pos < m_adapter!!.count) {
            return m_adapter!!.getItem(pos)
        }
        return null
    }*/

    private fun shareTodoList(format: Int) {
        val text = StringBuilder()
        for (line in m_adapter!!.visibleLines) {
            if (!line.header) {
                val item = line.task ?: continue
                text.append(item.task.showParts(format)).append("\n")
            }
        }
        shareText(this, "Simpletask list", text.toString())
    }


    private fun prioritizeTasks(tasks: List<TodoItem>) {
        val strings = Priority.rangeInCode(Priority.NONE, Priority.Z)
        val priorityArr = strings.toTypedArray()

        var priorityIdx = 0
        if (tasks.size == 1) {
            priorityIdx = strings.indexOf(tasks[0].task.priority.code)
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.select_priority)
        builder.setSingleChoiceItems(priorityArr, priorityIdx, { dialog, which ->
            dialog.dismiss()
            val priority = Priority.toPriority(priorityArr[which])
            TodoList.prioritize(tasks, priority)
            TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)
            /* closeSelectionMode() */
            TodoList.clearSelection()
            invalidateOptionsMenu()
        })
        builder.show()

    }

    private fun completeTasks(task: TodoItem) {
        val tasks = ArrayList<TodoItem>()
        tasks.add(task)
        completeTasks(tasks)
    }

    private fun completeTasks(tasks: List<TodoItem>) {
        for (t in tasks) {
            TodoList.complete(t, Config.hasKeepPrio(), Config.hasAppendAtEnd())
        }
        if (Config.isAutoArchive) {
            archiveTasks(null, false)
        }
        TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)
    }

    private fun undoCompleteTasks(task: TodoItem) {
        val tasks = ArrayList<TodoItem>()
        tasks.add(task)
        undoCompleteTasks(tasks)
    }

    private fun undoCompleteTasks(tasks: List<TodoItem>) {
        TodoList.undoComplete(tasks)
        TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)
    }

    private fun deferTasks(tasks: List<TodoItem>, dateType: DateType) {
        var titleId = R.string.defer_due
        if (dateType === DateType.THRESHOLD) {
            titleId = R.string.defer_threshold
        }
        val d = createDeferDialog(this, titleId, object : InputDialogListener {
            /* @Suppress("DEPRECATION") */
            override fun onClick(input: String) {
                if (input == "pick") {
                    val today = DateTime.today(TimeZone.getDefault())
                    val dialog = DatePickerDialog(this@Simpletask, DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                        var startMonth = month
                        startMonth++
                        val date = DateTime.forDateOnly(year, startMonth, day)
                        TodoList.defer(date.format(Constants.DATE_FORMAT), tasks, dateType)
                        TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)
                    },
                            today.year!!,
                            today.month!! - 1,
                            today.day!!)

                    val showCalendar = Config.showCalendar()
                    dialog.datePicker.calendarViewShown = showCalendar
                    dialog.datePicker.spinnersShown = !showCalendar
                    dialog.show()
                } else {

                    TodoList.defer(input, tasks, dateType)
                    TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)

                }

            }
        })
        d.show()
    }

    private fun deleteTasks(tasks: List<TodoItem>) {
        showConfirmationDialog(this, R.string.delete_task_message, DialogInterface.OnClickListener { dialogInterface, i ->
            for (t in tasks) {
                TodoList.remove(t)
            }
            TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)
            invalidateOptionsMenu()
        }, R.string.delete_task_title)
    }

    private fun archiveTasks(tasksToArchive: List<TodoItem>?, areYouSureDialog: Boolean) {

        val archiveAction = {
            if (Config.todoFileName == m_app.doneFileName) {
                showToastShort(this, "You have the done.txt file opened.")
            }
            TodoList.archive(Config.todoFileName, m_app.doneFileName, tasksToArchive, Config.eol)
            invalidateOptionsMenu()
        }
        if (areYouSureDialog) {
            showConfirmationDialog(this, R.string.delete_task_message, DialogInterface.OnClickListener { dialogInterface, i -> archiveAction() }, R.string.archive_task_title)
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
                        closeDrawer(FILTER_DRAWER)
                    }
                    Mode.SELECTION -> {
                        TodoList.clearSelection()
                        invalidateOptionsMenu()
                    }
                    Mode.MAIN -> {
                        val toggle = m_drawerToggle ?: return true
                        toggle.onOptionsItemSelected(item)
                    }
                }
            }
            R.id.search -> { }
            R.id.preferences -> startPreferencesActivity()
            R.id.filter -> startFilterActivity()
            R.id.context_delete -> deleteTasks(checkedTasks)
            R.id.context_select_all -> selectAllTasks()
            R.id.share -> {
                val shareText = TodoList.todoItems.map { it.task.inFileFormat() }.joinToString(separator = "\n")
                shareText(this@Simpletask, "Simpletask list", shareText)
            }
            R.id.context_share -> {
                val shareText = selectedTasksAsString()
                shareText(this@Simpletask, "Simpletask tasks", shareText)
            }
            R.id.context_archive -> archiveTasks(checkedTasks, true)
            R.id.context_calendar -> createCalendarAppointment(checkedTasks)
            R.id.help -> showHelp()
            R.id.open_lua -> openLuaConfig()
            R.id.sync -> FileStore.sync()
            R.id.archive -> archiveTasks(null, true)
            R.id.open_file -> m_app.browseForNewFile(this)
            R.id.history -> startActivity(Intent(this, HistoryScreen::class.java))
            R.id.btn_filter_add -> onAddFilterClick()
            R.id.clear_filter -> clearFilter()
            R.id.complete -> completeTasks(checkedTasks)
            R.id.uncomplete -> undoCompleteTasks(checkedTasks)
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

    private fun createCalendarAppointment(checkedTasks: List<TodoItem>) {
        var calendarTitle = getString(R.string.calendar_title)
        var calendarDescription = ""
        if (checkedTasks.size == 1) {
            // Set the task as title
            calendarTitle = checkedTasks[0].task.text
        } else {
            // Set the tasks as description
            calendarDescription = selectedTasksAsString()

        }
        intent = Intent(Intent.ACTION_EDIT).setType(Constants.ANDROID_EVENT).putExtra(Events.TITLE, calendarTitle).putExtra(Events.DESCRIPTION, calendarDescription)
        // Explicitly set start and end date/time.
        // Some calendar providers need this.
        val dueDate =  checkedTasks[0].task.dueDate
        val calDate = if (checkedTasks.size == 1 && dueDate != null ) {
            val year = dueDate.substring(0,4).toInt()
            val month =  dueDate.substring(5,7).toInt()-1
            val day =  dueDate.substring(8,10).toInt()
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
        val intent = Intent(this, AddTask::class.java)
        mFilter!!.saveInIntent(intent)
        startActivity(intent)
    }

    private fun startPreferencesActivity() {
        val settingsActivity = Intent(baseContext,
                Preferences::class.java)
        startActivityForResult(settingsActivity, REQUEST_PREFERENCES)
    }

    /**
     * Handle clear filter click *
     */
    @Suppress("unused")
    fun onClearClick(@Suppress("UNUSED_PARAMETER") v: View) = clearFilter()

    val savedFilters: ArrayList<ActiveFilter>
        get() {
            val saved_filters = ArrayList<ActiveFilter>()
            val saved_filter_ids = getSharedPreferences("filters", Context.MODE_PRIVATE)
            val filterIds = saved_filter_ids.getStringSet("ids", HashSet<String>())
            for (id in filterIds) {
                val filter_pref = getSharedPreferences(id, Context.MODE_PRIVATE)
                val filter = ActiveFilter(FilterOptions(luaModule = "mainui"))
                filter.initFromPrefs(filter_pref)
                filter.prefName = id
                saved_filters.add(filter)
            }
            return saved_filters
        }

    fun importFilters (importFile: File) {
        val r = Runnable() {
            try {
                val contents = FileStore.readFile(importFile.canonicalPath, null)
                val jsonFilters = JSONObject(contents)
                jsonFilters.keys().forEach {
                    val filter = ActiveFilter(FilterOptions(luaModule = "mainui"))
                    filter.initFromJSON(jsonFilters.getJSONObject(it))
                    saveFilterInPrefs(it,filter)
                }
                localBroadcastManager?.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_UI))
            } catch (e: IOException) {
                log.error(TAG, "Import filters, cant read file ${importFile.canonicalPath}", e)
                showToastLong(this, "Error reading file ${importFile.canonicalPath}")
            }
        }
        Thread(r).start()
    }

    fun exportFilters (exportFile: File) {
        val jsonFilters = JSONObject()
        savedFilters.forEach {
            val jsonItem = JSONObject()
            it.saveInJSON(jsonItem)
            jsonFilters.put(it.name,jsonItem)
        }
	try {
            FileStore.writeFile(exportFile,jsonFilters.toString(2))
	    showToastShort(this, "Filters exported")
	} catch (e: Exception) {
            log.error(TAG, "Export filteres failed", e)
	    showToastLong(this, "Error exporting filters")
        }
    }
    /**
     * Handle add filter click *
     */
    fun onAddFilterClick() {
        val alert = AlertDialog.Builder(this)

        alert.setTitle(R.string.save_filter)
        alert.setMessage(R.string.save_filter_message)

        // Set an EditText view to get user input
        val input = EditText(this)
        alert.setView(input)
        input.setText(mFilter!!.proposedName)

        alert.setPositiveButton("Ok") { dialog, whichButton ->
            val text = input.text
            val value: String
            if (text == null) {
                value = ""
            } else {
                value = text.toString()
            }
            if (value == "") {
                showToastShort(applicationContext, R.string.filter_name_empty)
            } else {
                saveFilterInPrefs(value, mFilter!!)
                updateNavDrawer()
            }
        }

        alert.setNegativeButton("Cancel") { dialog, whichButton -> }
        alert.show()
    }

    private fun saveFilterInPrefs(name: String, filter: ActiveFilter) {
        val saved_filters = getSharedPreferences("filters", Context.MODE_PRIVATE)
        val newId = saved_filters.getInt("max_id", 1) + 1
        val filters = saved_filters.getStringSet("ids", HashSet<String>())
        filters.add("filter_" + newId)
        saved_filters.edit().putStringSet("ids", filters).putInt("max_id", newId).apply()
        val test_filter_prefs = getSharedPreferences("filter_" + newId, Context.MODE_PRIVATE)
        filter.name = name
        filter.saveInPrefs(test_filter_prefs)
    }

    override fun onBackPressed() {
        when (activeMode()) {
            Mode.NAV_DRAWER -> {
                closeDrawer(NAV_DRAWER)
            }
            Mode.FILTER_DRAWER -> {
                closeDrawer(FILTER_DRAWER)
            }
            Mode.SELECTION -> {
                TodoList.clearSelection()
                invalidateOptionsMenu()
                val lay = listView?.layoutManager ?: return
                for ( i in 0..lay.childCount-1 ) {
                    val view = lay.getChildAt(i)
                    view.isActivated = false
                }
            }
            Mode.MAIN -> {
                if (!Config.backClearsFilter() ||  mFilter == null || !mFilter!!.hasFilter()) {
                    return super.onBackPressed()
                }
                clearFilter()
                onNewIntent(intent)
            }
        }
        return
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_SEARCH == intent.action) {
            val currentIntent = getIntent()
            currentIntent.putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY))
            setIntent(currentIntent)
            if (options_menu == null) {
                return
            }
            options_menu!!.findItem(R.id.search).collapseActionView()

        } else if (CalendarContract.ACTION_HANDLE_CUSTOM_EVENT == intent.action) {
            // Uri uri = Uri.parse(intent.getStringExtra(CalendarContract.EXTRA_CUSTOM_APP_URI));
            log.warn(TAG, "Not implemented search")
        } else if (intent.extras != null) {
            // Only change intent if it actually contains a filter
            setIntent(intent)
        }
        Config.lastScrollPosition = -1
        log.info(TAG, "onNewIntent: " + intent)

    }

    internal fun clearFilter() {
        // Also clear the intent so we wont get the old filter after
        // switching back to app later fixes [1c5271ee2e]
        val intent = Intent()
        mFilter!!.clear()
        mFilter!!.saveInIntent(intent)
        mFilter!!.saveInPrefs(Config.prefs)
        setIntent(intent)
        updateDrawers()
        m_adapter!!.setFilteredTasks()
    }

    private fun updateDrawers() {
        updateFilterDrawer()
        updateNavDrawer()
    }

    private fun updateNavDrawer() {
        val names = ArrayList<String>()
        val filters = savedFilters
        Collections.sort(filters) { f1, f2 -> f1.name!!.compareTo(f2.name!!, ignoreCase = true) }
        for (f in filters) {
            names.add(f.name!!)
        }
        m_navDrawerList!!.adapter = ArrayAdapter(this, R.layout.drawer_list_item, names)
        m_navDrawerList!!.choiceMode = AbsListView.CHOICE_MODE_NONE
        m_navDrawerList!!.isLongClickable = true
        m_navDrawerList!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            mFilter = filters[position]
            val intent = intent
            mFilter!!.saveInIntent(intent)
            setIntent(intent)
            mFilter!!.saveInPrefs(Config.prefs)
            m_adapter!!.setFilteredTasks()
            closeDrawer(NAV_DRAWER)
            updateDrawers()
        }
        m_navDrawerList!!.onItemLongClickListener = OnItemLongClickListener { parent, view, position, id ->
            val filter = filters[position]
            val prefsName = filter.prefName!!
            val popupMenu = PopupMenu(this@Simpletask, view)
            popupMenu.setOnMenuItemClickListener { item ->
                val menuId = item.itemId
                when (menuId) {
                    R.id.menu_saved_filter_delete -> deleteSavedFilter(prefsName)
                    R.id.menu_saved_filter_shortcut -> createFilterShortcut(filter)
                    R.id.menu_saved_filter_rename -> renameSavedFilter(prefsName)
                    R.id.menu_saved_filter_update -> updateSavedFilter(prefsName)
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

    fun createFilterShortcut(filter: ActiveFilter) {
        val shortcut = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
        val target = Intent(Constants.INTENT_START_FILTER)
        filter.saveInIntent(target)

        target.putExtra("name", filter.name)

        // Setup target intent for shortcut
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target)

        // Set shortcut icon
        val iconRes = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher)
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes)
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, filter.name)
        sendBroadcast(shortcut)
    }

    private fun deleteSavedFilter(prefsName: String) {
        val saved_filters = getSharedPreferences("filters", Context.MODE_PRIVATE)
        val ids = HashSet<String>()
        ids.addAll(saved_filters.getStringSet("ids", HashSet<String>()))
        ids.remove(prefsName)
        saved_filters.edit().putStringSet("ids", ids).apply()
        val filter_prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val deleted_filter = ActiveFilter(FilterOptions(luaModule = "mainui"))
        deleted_filter.initFromPrefs(filter_prefs)
        filter_prefs.edit().clear().apply()
        val prefs_path = File(this.filesDir, "../shared_prefs")
        val prefs_xml = File(prefs_path, prefsName + ".xml")
        val deleted = prefs_xml.delete()
        if (!deleted) {
            log.warn(TAG, "Failed to delete saved filter: " + deleted_filter.name!!)
        }
        updateNavDrawer()
    }

    private fun updateSavedFilter(prefsName: String) {
        val filter_pref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val old_filter = ActiveFilter(FilterOptions(luaModule = "mainui"))
        old_filter.initFromPrefs(filter_pref)
        val filterName = old_filter.name
        mFilter!!.name = filterName
        mFilter!!.saveInPrefs(filter_pref)
        updateNavDrawer()
    }

    private fun renameSavedFilter(prefsName: String) {
        val filter_pref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val old_filter = ActiveFilter(FilterOptions(luaModule = "mainui"))
        old_filter.initFromPrefs(filter_pref)
        val filterName = old_filter.name
        val alert = AlertDialog.Builder(this)

        alert.setTitle(R.string.rename_filter)
        alert.setMessage(R.string.rename_filter_message)

        // Set an EditText view to get user input
        val input = EditText(this)
        alert.setView(input)
        input.setText(filterName)

        alert.setPositiveButton("Ok") { dialog, whichButton ->
            val text = input.text
            val value: String
            if (text == null) {
                value = ""
            } else {
                value = text.toString()
            }
            if (value == "") {
                showToastShort(applicationContext, R.string.filter_name_empty)
            } else {
                old_filter.name = value
                old_filter.saveInPrefs(filter_pref)
                updateNavDrawer()
            }
        }

        alert.setNegativeButton("Cancel") { dialog, whichButton -> }

        alert.show()
    }


    private fun updateFilterDrawer() {
        val taskBag = TodoList
        val decoratedContexts = sortWithPrefix(taskBag.decoratedContexts, Config.sortCaseSensitive(), "@-")
        val decoratedProjects = sortWithPrefix(taskBag.decoratedProjects, Config.sortCaseSensitive(), "+-")
        val drawerAdapter = DrawerAdapter(layoutInflater,
                Config.listTerm,
                decoratedContexts,
                Config.tagTerm,
                decoratedProjects)

        m_filterDrawerList!!.adapter = drawerAdapter
        m_filterDrawerList!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
        m_filterDrawerList!!.onItemClickListener = DrawerItemClickListener()

        for (context in mFilter!!.contexts) {
            val position = drawerAdapter.getIndexOf("@" + context)
            if (position != -1) {
                m_filterDrawerList!!.setItemChecked(position, true)
            }
        }

        for (project in mFilter!!.projects) {
            val position = drawerAdapter.getIndexOf("+" + project)
            if (position != -1) {
                m_filterDrawerList!!.setItemChecked(position, true)
            }
        }
        m_filterDrawerList!!.setItemChecked(drawerAdapter.contextHeaderPosition, mFilter!!.contextsNot)
        m_filterDrawerList!!.setItemChecked(drawerAdapter.projectsHeaderPosition, mFilter!!.projectsNot)
        m_filterDrawerList!!.deferNotifyDataSetChanged()
    }


    fun startFilterActivity() {
        val i = Intent(this, FilterActivity::class.java)
        mFilter!!.saveInIntent(i)
        startActivity(i)
    }

    val listView: RecyclerView?
        get() {
            val lv = findViewById(androidId.list)
            return lv as RecyclerView?
        }

    fun showListViewProgress(show: Boolean) {
        val progressBar = findViewById(R.id.empty_progressbar)
        if (show) {
            progressBar?.visibility = View.VISIBLE
        } else {
            progressBar?.visibility = View.GONE
        }
    }

    class TaskViewHolder(itemView: View, val viewType : Int)  : RecyclerView.ViewHolder(itemView) {

    }

    inner class TaskAdapter(private val m_inflater: LayoutInflater) : RecyclerView.Adapter <TaskViewHolder>() {
        override fun getItemCount(): Int {
            return visibleLines.size + 1
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): TaskViewHolder {
            val view = when (viewType) {
                0 -> {
                    // Header
                    m_inflater.inflate(R.layout.list_header, parent, false)
                }
                1 -> {
                    // Task
                    m_inflater.inflate(R.layout.list_item, parent, false)
                }
                else -> {
                    // Empty at end
                    m_inflater.inflate(R.layout.empty_list_item, parent, false)
                }

            }
            return TaskViewHolder(view, viewType)
        }

        override fun onBindViewHolder(holder: TaskViewHolder?, position: Int) {
            if (holder == null) return
            when(holder.viewType) {
                0 -> bindHeader(holder, position)
                1 -> bindTask(holder,position)
                else -> return
            }
        }

        fun bindHeader(holder : TaskViewHolder, position: Int) {
            val t = holder.itemView.findViewById(R.id.list_header_title) as TextView
            val line = visibleLines[position]
            t.text = line.title
            t.textSize = textSize
        }

        fun bindTask (holder : TaskViewHolder, position: Int) {
            val line = visibleLines[position]
            val item = line.task ?: return
            val view = holder.itemView
            val taskText = view!!.findViewById(R.id.tasktext) as TextView
            val taskAge = view.findViewById(R.id.taskage) as TextView
            val taskDue = view.findViewById(R.id.taskdue) as TextView
            val taskThreshold = view.findViewById(R.id.taskthreshold) as TextView
            val cbCompleted = view.findViewById(R.id.checkBox) as CheckBox

            val task = item.task

            if (!Config.hasExtendedTaskView()) {
                val taskBar = view.findViewById(R.id.datebar)
                taskBar.visibility = View.GONE
            }
            var tokensToShow = TToken.ALL
            // Hide dates if we have a date bar
            if (Config.hasExtendedTaskView()) {
                tokensToShow = tokensToShow and TToken.COMPLETED_DATE.inv()
                tokensToShow = tokensToShow and TToken.THRESHOLD_DATE.inv()
                tokensToShow = tokensToShow and TToken.DUE_DATE.inv()
            }
            tokensToShow = tokensToShow and TToken.CREATION_DATE.inv()
            tokensToShow = tokensToShow and TToken.COMPLETED.inv()

            if (mFilter!!.hideLists) {
                tokensToShow = tokensToShow and TToken.LIST.inv()
            }
            if (mFilter!!.hideTags) {
                tokensToShow = tokensToShow and TToken.TTAG.inv()
            }
            val txt = task.showParts(tokensToShow)

            val ss = SpannableString(txt)

            val colorizeStrings = ArrayList<String>()
            val contexts = task.lists
            for (context in contexts) {
                colorizeStrings.add("@" + context)
            }
            setColor(ss, Color.GRAY, colorizeStrings)
            colorizeStrings.clear()
            val projects = task.tags
            for (project in projects) {
                colorizeStrings.add("+" + project)
            }
            setColor(ss, Color.GRAY, colorizeStrings)

            val priorityColor: Int
            val priority = task.priority
            when (priority) {
                Priority.A -> priorityColor = ContextCompat.getColor(m_app, android.R.color.holo_red_dark)
                Priority.B -> priorityColor = ContextCompat.getColor(m_app, android.R.color.holo_orange_dark)
                Priority.C -> priorityColor = ContextCompat.getColor(m_app, android.R.color.holo_green_dark)
                Priority.D -> priorityColor = ContextCompat.getColor(m_app, android.R.color.holo_blue_dark)
                else -> priorityColor = ContextCompat.getColor(m_app, android.R.color.darker_gray)
            }
            setColor(ss, priorityColor, priority.inFileFormat())
            val completed = task.isCompleted()


            taskAge.textSize = textSize * Config.dateBarRelativeSize
            taskDue.textSize = textSize * Config.dateBarRelativeSize
            taskThreshold.textSize = textSize * Config.dateBarRelativeSize

            val cb = cbCompleted
            taskText.text = ss
            taskText.textSize = textSize
            handleEllipsis(taskText)


            if (completed) {
                // log.info( "Striking through " + task.getText());
                taskText.paintFlags = taskText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                taskAge.paintFlags = taskAge.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                cb.setOnClickListener({
                    undoCompleteTasks(item)
                    TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)
                })
            } else {
                taskText.paintFlags = taskText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                taskAge.paintFlags = taskAge.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

                cb.setOnClickListener {
                    completeTasks(item)
                    TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)
                }

            }
            cb.isChecked = completed

            val relAge = getRelativeAge(task, m_app)
            val relDue = getRelativeDueDate(task, m_app)
            val relativeThresholdDate = getRelativeThresholdDate(task, m_app)
            if (!isEmptyOrNull(relAge) && !mFilter!!.hideCreateDate) {
                taskAge.text = relAge
                taskAge.visibility = View.VISIBLE
            } else {
                taskAge.text = ""
                taskAge.visibility = View.GONE
            }

            if (relDue != null) {
                taskDue.text = relDue
                taskDue.visibility = View.VISIBLE
            } else {
                taskDue.text = ""
                taskDue.visibility = View.GONE
            }
            if (!isEmptyOrNull(relativeThresholdDate)) {
                taskThreshold.text = relativeThresholdDate
                taskThreshold.visibility = View.VISIBLE
            } else {
                taskThreshold.text = ""
                taskThreshold.visibility = View.GONE
            }
            // Set selected state
            view.isActivated = TodoList.isSelected(item)

            // Set click listeners
            view.setOnClickListener { it ->

                val newSelectedState = !TodoList.isSelected(item)
                if (newSelectedState) {
                    TodoList.selectTodoItem(item)
                } else {
                    TodoList.unSelectTodoItem(item)
                }
                it.isActivated = newSelectedState
                invalidateOptionsMenu()
            }

            view.setOnLongClickListener {
                val links = ArrayList<String>()
                val actions = ArrayList<String>()
                val t = item.task
                for (link in t.links) {
                    actions.add(ACTION_LINK)
                    links.add(link)
                }
                for (number in t.phoneNumbers) {
                    actions.add(ACTION_PHONE)
                    links.add(number)
                    actions.add(ACTION_SMS)
                    links.add(number)
                }
                for (mail in t.mailAddresses) {
                    actions.add(ACTION_MAIL)
                    links.add(mail)
                }
                if (actions.size != 0) {


                    val titles = ArrayList<String>()
                    for (i in links.indices) {
                        when (actions[i]) {
                            ACTION_SMS -> titles.add(i, "SMS: " + links[i])
                            ACTION_PHONE -> titles.add(i, "Call: " + links[i])
                            else -> titles.add(i, links[i])
                        }
                    }
                    val build = AlertDialog.Builder(this@Simpletask)
                    build.setTitle(R.string.task_action)
                    val titleArray = titles.toArray<String>(arrayOfNulls<String>(titles.size))
                    build.setItems(titleArray) { dialog, which ->
                        val actionIntent: Intent
                        val url = links[which]
                        log.info(TAG, "" + actions[which] + ": " + url)
                        when (actions[which]) {
                            ACTION_LINK -> if (url.startsWith("todo://")) {
                                val todoFolder = Config.todoFile.parentFile
                                val newName = File(todoFolder, url.substring(7))
                                m_app.switchTodoFile(newName.absolutePath)
                            } else if (url.startsWith("root://")) {
                                val rootFolder = Config.localFileRoot
                                val file = File(rootFolder, url.substring(7))
                                actionIntent = Intent(Intent.ACTION_VIEW, Uri.fromFile(file))
                                startActivity(actionIntent)
                            } else {
                                try {
                                    actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    startActivity(actionIntent)
                                } catch(e: ActivityNotFoundException) {
                                    log.info(TAG, "No handler for task action $url")
                                    showToastLong(TodoApplication.app, "No handler for $url" )
                                }
                            }
                            ACTION_PHONE -> {
                                actionIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(url)))
                                startActivity(actionIntent)
                            }
                            ACTION_SMS -> {
                                actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + Uri.encode(url)))
                                startActivity(actionIntent)
                            }
                            ACTION_MAIL -> {
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
            }
        }
        internal var visibleLines = ArrayList<VisibleLine>()

        internal fun setFilteredTasks() {
            ActionQueue.add("setFilterTasks", Runnable {
                runOnUiThread() {
                    showListViewProgress(true)
                }
                val visibleTasks: List<TodoItem>
                log.info(TAG, "setFilteredTasks called: " + TodoList)
                val activeFilter = mFilter ?: return@Runnable
                val sorts = activeFilter.getSort(Config.defaultSorts)
                visibleTasks = TodoList.getSortedTasks(activeFilter, sorts, Config.sortCaseSensitive())
                val newVisibleLines = ArrayList<VisibleLine>()

                newVisibleLines.addAll(addHeaderLines(visibleTasks, activeFilter, getString(R.string.no_header)))
                runOnUiThread {
                    // Replace the array in the main thread to prevent OutOfIndex exceptions
                    visibleLines = newVisibleLines
                    notifyDataSetChanged()
                    updateConnectivityIndicator()
                    updateFilterBar()
                    showListViewProgress(false)
                    if (Config.lastScrollPosition != -1) {
                        val manager = listView?.layoutManager as LinearLayoutManager?
                        val position = Config.lastScrollPosition
                        val offset = Config.lastScrollOffset
                        Logger.info(TAG, "Restoring scroll offset $position, $offset")
                        manager?.scrollToPositionWithOffset(position, offset )
                        Config.lastScrollPosition = -1
                    }
                }
            })
        }


        val countVisibleTodoItems: Int
            get() {
                var count = 0
                for (line in visibleLines) {
                    if (!line.header) {
                        count++
                    }
                }
                return count
            }

        /*
        ** Get the adapter position for task
        */
        fun getPosition(task: TodoItem): Int {
            val line = TaskLine(task)
            return visibleLines.indexOf(line)
        }


        override fun getItemId(position: Int): Long {
            return position.toLong()
        }



        override fun getItemViewType(position: Int): Int {
            if (position == visibleLines.size) {
                return 2
            }
            val line = visibleLines[position]
            if (line.header) {
                return 0
            } else {
                return 1
            }
        }
    }

    private fun handleEllipsis(taskText: TextView) {
        val noEllipsizeValue = "no_ellipsize"
        val ellipsizeKey = m_app.getString(R.string.task_text_ellipsizing_pref_key)
        val ellipsizePref = Config.prefs.getString(ellipsizeKey, noEllipsizeValue)

        if (noEllipsizeValue != ellipsizePref) {
            val truncateAt: TextUtils.TruncateAt?
            when (ellipsizePref) {
                "start" -> truncateAt = TextUtils.TruncateAt.START
                "end" -> truncateAt = TextUtils.TruncateAt.END
                "middle" -> truncateAt = TextUtils.TruncateAt.MIDDLE
                "marquee" -> truncateAt = TextUtils.TruncateAt.MARQUEE
                else -> truncateAt = null
            }

            if (truncateAt != null) {
                taskText.maxLines = 1
                taskText.setHorizontallyScrolling(true)
                taskText.ellipsize = truncateAt
            } else {
                log.warn(TAG, "Unrecognized preference value for task text ellipsis: {} !" + ellipsizePref)
            }
        }
    }



    private fun updateItemsDialog(title: String,
                                  checkedTasks: List<TodoItem>,
                                  allItems: ArrayList<String>,
                                  retrieveFromTask: (Task) -> SortedSet<String>,
                                  addToTask: (Task, String) -> Unit,
                                  removeFromTask: (Task, String) -> Unit
    ) {
        val checkedTaskItems = ArrayList<HashSet<String>>()
        checkedTasks.forEach {
            val items = HashSet<String>()
            items.addAll(retrieveFromTask.invoke(it.task))
            checkedTaskItems.add(items)
        }

        // Determine items on all tasks (intersection of the sets)
        val onAllTasks = checkedTaskItems.intersection()

        // Determine items on some tasks (union of the sets)
        var onSomeTasks = checkedTaskItems.union()
        onSomeTasks -= onAllTasks

        allItems.removeAll(onAllTasks)
        allItems.removeAll(onSomeTasks)

        // TODO add setting for this

        val sortedAllItems = ArrayList<String>()
        sortedAllItems += onAllTasks.sorted()
        sortedAllItems += onSomeTasks.sorted()
        sortedAllItems += allItems.sorted()

        @SuppressLint("InflateParams")
        val view = layoutInflater.inflate(R.layout.list_dialog, null, false)
        val rcv = view.findViewById(R.id.recyclerView) as RecyclerView
        rcv.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        rcv.layoutManager = layoutManager


        val itemAdapter = ItemDialogAdapter(sortedAllItems, onAllTasks.toHashSet(), onSomeTasks.toHashSet())
        rcv.adapter = itemAdapter

        val ed = view.findViewById(R.id.editText) as EditText

        val builder = AlertDialog.Builder(this)
        builder.setView(view)

        builder.setPositiveButton(R.string.ok) { dialog, which ->
            val newText = ed.text.toString()
            if (newText.isNotEmpty()) {
                checkedTasks.forEach {
                    addToTask(it.task,newText)
                }
            }
            val updatedValues = itemAdapter.currentState
            for (i in 0..updatedValues.lastIndex) {
                when (updatedValues[i] ) {
                    false -> {
                        checkedTasks.forEach {
                            removeFromTask(it.task,sortedAllItems[i])
                        }
                    }
                    true -> {
                        checkedTasks.forEach {
                            addToTask(it.task,sortedAllItems[i])
                        }
                    }
                }
            }
            TodoList.update(checkedTasks)
            TodoList.notifyChanged(Config.todoFileName, Config.eol, m_app, true)
        }
        builder.setNegativeButton(R.string.cancel) { dialog, id -> }
        // Create the AlertDialog
        val dialog = builder.create()
        dialog.setTitle(title)
        dialog.show()
    }

    private fun updateLists(checkedTasks: List<TodoItem>) {
        updateItemsDialog(
                Config.listTerm,
                checkedTasks,
                sortWithPrefix(TodoList.contexts, Config.sortCaseSensitive(), null),
                {task -> task.lists},
                {task, list -> task.addList(list)},
                {task, list -> task.removeList(list)}
        )
    }

    private fun updateTags(checkedTasks: List<TodoItem>) {
        updateItemsDialog(
                Config.tagTerm,
                checkedTasks,
                sortWithPrefix(TodoList.projects, Config.sortCaseSensitive(), null),
                {task -> task.tags},
                {task, tag -> task.addTag(tag)},
                {task, tag -> task.removeTag(tag)}
        )
    }

    private inner class DrawerItemClickListener : AdapterView.OnItemClickListener {

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int,
                                 id: Long) {
            val tags: ArrayList<String>
            val lv = parent as ListView
            val adapter = lv.adapter as DrawerAdapter
            if (adapter.projectsHeaderPosition == position) {
                mFilter!!.projectsNot = !mFilter!!.projectsNot
                updateDrawers()
            }
            if (adapter.contextHeaderPosition == position) {
                mFilter!!.contextsNot = !mFilter!!.contextsNot
                updateDrawers()
            } else {
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
                mFilter!!.contexts = filteredContexts
                mFilter!!.projects = filteredProjects
            }
            val intent = intent
            mFilter!!.saveInIntent(intent)
            mFilter!!.saveInPrefs(Config.prefs)
            setIntent(intent)
            closeDrawer(FILTER_DRAWER)
            m_adapter!!.setFilteredTasks()
        }
    }

    companion object {

        private val REQUEST_SHARE_PARTS = 1
        private val REQUEST_PREFERENCES = 2
        private val REQUEST_PERMISSION = 3

        private val ACTION_LINK = "link"
        private val ACTION_SMS = "sms"
        private val ACTION_PHONE = "phone"
        private val ACTION_MAIL = "mail"

        val URI_BASE = Uri.fromParts("Simpletask", "", null)!!
        val URI_SEARCH = Uri.withAppendedPath(URI_BASE, "search")!!
        private val TAG = "Simpletask"
    }
}
