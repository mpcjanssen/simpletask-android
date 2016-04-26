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
import hirondelle.date4j.DateTime

import nl.mpcjanssen.simpletask.adapters.ItemDialogAdapter
import nl.mpcjanssen.simpletask.adapters.DrawerAdapter

import nl.mpcjanssen.simpletask.task.*
import nl.mpcjanssen.simpletask.util.InputDialogListener
import nl.mpcjanssen.simpletask.util.*

import java.io.File

import java.util.*


class MainActivity : ThemedActivity(), AdapterView.OnItemLongClickListener {

    internal var options_menu: Menu? = null
    internal lateinit var m_app: SimpletaskApplication
    internal var mFilter: ActiveFilter? = null
    internal var m_adapter: TaskAdapter? = null
    private var m_broadcastReceiver: BroadcastReceiver? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager

    // Drawer vars
    private var m_leftDrawerList: ListView? = null
    private var m_rightDrawerList: ListView? = null
    private var m_drawerLayout: DrawerLayout? = null
    private var m_drawerToggle: ActionBarDrawerToggle? = null
    private var m_savedInstanceState: Bundle? = null

    private val log = Logger


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.info(TAG, "onCreate")
        m_app = application as SimpletaskApplication
        m_savedInstanceState = savedInstanceState
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_TODOLIST_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_THEME_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_DATEBAR_SIZE_CHANGED)
        intentFilter.addAction(Constants.BROADCAST_HIGHLIGHT_SELECTION)

        localBroadcastManager = m_app.localBroadCastManager

        m_broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, receivedIntent: Intent) {
                log.info(TAG, "Handling broadcast ${receivedIntent.action}")
                if (receivedIntent.action == Constants.BROADCAST_TODOLIST_CHANGED ||
                        receivedIntent.action == Constants.BROADCAST_UPDATE_UI) {
                    if (m_adapter == null) {
                        return
                    }
                    m_adapter!!.setFilteredTasks()
                    updateDrawers()
                } else if (receivedIntent.action == Constants.BROADCAST_HIGHLIGHT_SELECTION) {
                    handleIntent()
                } else if ( receivedIntent.action == Constants.BROADCAST_THEME_CHANGED ||
                        receivedIntent.action == Constants.BROADCAST_DATEBAR_SIZE_CHANGED) {
                    recreate()
                }
            }
        }
        localBroadcastManager.registerReceiver(m_broadcastReceiver, intentFilter)


        // Set the proper view
        if (m_app.hasLandscapeDrawers()) {
            setContentView(R.layout.main_landscape)
        } else {
            setContentView(R.layout.main)
        }

        // Replace drawables if the theme is dark
        if (m_app.isDarkTheme) {
            val actionBarClear = findViewById(R.id.actionbar_clear) as ImageView?
            actionBarClear?.setImageResource(R.drawable.ic_action_content_clear)
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

    private fun showHelp() {
        val i = Intent(this, HelpActivity::class.java)
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
        for (item in todoList.selectedTasks) {
            result.add(item.task.inFileFormat())
        }
        return join(result, "\n")
    }

    private fun selectAllTasks() {
        for (visibleLine in m_adapter!!.visibleLines) {
            // Only check tasks that are not checked yet
            // and skip headers
            // This prevents double counting in the CAB title
            if (!visibleLine.header) {
                visibleLine.item?.selected  = true
            }
        }
        handleIntent()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (m_drawerToggle != null) {
            m_drawerToggle!!.onConfigurationChanged(newConfig)
        }
    }


    private fun handleIntent() {


        mFilter = ActiveFilter()

        m_leftDrawerList = findViewById(R.id.left_drawer) as ListView
        m_rightDrawerList = findViewById(R.id.right_drawer_list) as ListView

        m_drawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout?

        // Set the list's click listener
        m_leftDrawerList!!.onItemClickListener = DrawerItemClickListener()

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
                    // setTitle(R.string.app_label);
                }

                /** Called when a drawer has settled in a completely open state.  */
                override fun onDrawerOpened(drawerView: View?) {
                    // setTitle(R.string.changelist);
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
            log!!.info(TAG, "handleIntent: launched with filter" + mFilter!!)
            val extras = intent.extras
            if (extras != null) {
                for (key in extras.keySet()) {
                    val value = extras.get(key)
                    if (value != null) {
                        log!!.debug(TAG, "%s %s (%s)".format(key, value.toString(), value.javaClass.name))
                    } else {
                        log!!.debug(TAG, "%s %s)".format(key, "<null>"))
                    }

                }

            }
            log!!.info(TAG, "handleIntent: saving filter in prefs")
            mFilter!!.saveInPrefs(m_app.prefs)
        } else {
            // Set previous filters and sort
            log!!.info(TAG, "handleIntent: from m_prefs state")
            mFilter!!.initFromPrefs(m_app.prefs)
        }

        // Initialize Adapter
        if (m_adapter == null) {
            m_adapter = TaskAdapter(layoutInflater, application)
        }
        m_adapter!!.setFilteredTasks()

        listView.adapter = this.m_adapter

        val lv = listView
        lv.isTextFilterEnabled = true
        lv.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        lv.isClickable = true
        lv.isLongClickable = true
        lv.onItemLongClickListener = this


        lv.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val links = ArrayList<String>()
            val actions = ArrayList<String>()
            lv.setItemChecked(position, !lv.isItemChecked(position))
            if (todoList.selectedTasks.size > 0) {
                onItemLongClick(parent, view, position, id)
                return@OnItemClickListener
            }
            val item = getTaskAt(position)
            if (item != null) {
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
            }
            if (links.size == 0) {
                onItemLongClick(parent, view, position, id)
            } else {
                // Decorate the links array
                val titles = ArrayList<String>()
                for (i in links.indices) {
                    when (actions[i]) {
                        ACTION_SMS -> titles.add(i, "SMS: " + links[i])
                        ACTION_PHONE -> titles.add(i, "Call: " + links[i])
                        else -> titles.add(i, links[i])
                    }
                }
                val build = AlertDialog.Builder(this@MainActivity)
                build.setTitle(R.string.task_action)
                val titleArray = titles.toArray<String>(arrayOfNulls<String>(titles.size))
                build.setItems(titleArray) { dialog, which ->
                    val actionIntent: Intent
                    val url = links[which]
                    log.info(TAG, "" + actions[which] + ": " + url)
                    when (actions[which]) {
                        ACTION_LINK -> {
                            actionIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(actionIntent)
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
        }

        lv.isFastScrollEnabled = m_app.useFastScroll()

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { startAddTaskActivity() }
        highlightSelectedTasks()
    }


    private fun highlightSelectedTasks() {
        val items = todoList.selectedTasks
        if (items.size == 0) {
            return
        }
        openSelectionMode()
        val lv = listView
        for (t in items) {
            val position = m_adapter!!.getPosition(t)
            if (position != -1) {
                lv.setItemChecked(position, t.selected)
            }
        }
    }

    private fun updateFilterBar() {
        val lv = listView
        val index = lv.firstVisiblePosition
        val v = lv.getChildAt(0)
        val top = if (v == null) 0 else v.top
        lv.setSelectionFromTop(index, top)

        val actionbar = findViewById(R.id.actionbar) as LinearLayout
        val filterText = findViewById(R.id.filter_text) as TextView
        if (mFilter!!.hasFilter()) {
            actionbar.visibility = View.VISIBLE
        } else {
            actionbar.visibility = View.GONE
        }
        val count = if (m_adapter != null) m_adapter!!.countVisibleTodoItems else 0
        val total = todoList.size().toLong()

        filterText.text = mFilter!!.getTitle(
                count,
                total,
                getText(R.string.priority_prompt),
                m_app.tagTerm,
                m_app.listTerm,
                getText(R.string.search),
                getText(R.string.script),
                getText(R.string.title_filter_applied),
                getText(R.string.no_filter))
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager!!.unregisterReceiver(m_broadcastReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("position", listView.firstVisiblePosition)
    }

    override fun onResume() {
        super.onResume()
        handleIntent()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        this.options_menu = menu
        if (todoList.selectedTasks.size > 0) {
            openSelectionMode()
        } else {
            populateMainMenu(menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun populateMainMenu(menu: Menu?) {

        if (menu == null) {
            log!!.warn(TAG, "Menu was null")
            return
        }
        menu.clear()
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)

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
                        mFilter = ActiveFilter()
                    }
                    mFilter!!.search = newText
                    mFilter!!.saveInPrefs(m_app.prefs)
                    if (m_adapter != null) {
                        m_adapter!!.setFilteredTasks()
                    }
                }
                return true
            }
        })
    }

    private fun getTaskAt(pos: Int): TodoItem? {
        if (pos < m_adapter!!.count) {
            return m_adapter!!.getItem(pos)
        }
        return null
    }

    private fun shareTodoList(format: Int) {
        val text = StringBuilder()
        for (i in 0..m_adapter!!.count - 1 - 1) {
            val item = m_adapter!!.getItem(i)
            if (item != null) {
                text.append(item.task.showParts(format)).append("\n")
            }
        }
        shareText(this, "Simpletask list", text.toString())
    }


    private fun prioritizeTasks(items: List<TodoItem>) {
        val strings = Priority.rangeInCode(Priority.NONE, Priority.Z)
        val priorityArr = strings.toTypedArray()

        var priorityIdx = 0
        if (items.size == 1) {
            priorityIdx = strings.indexOf(items[0].task.priority.code)
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.select_priority)
        builder.setSingleChoiceItems(priorityArr, priorityIdx, { dialog, which ->
            dialog.dismiss()
            val priority = Priority.toPriority(priorityArr[which])
            todoList.prioritize(items, priority)
            closeSelectionMode()
        })
        builder.show()

    }

    private fun completeTasks(task: TodoItem) {
        val tasks = ArrayList<TodoItem>()
        tasks.add(task)
        completeTasks(tasks)
    }

    private fun completeTasks(items: List<TodoItem>) {
        todoList.complete(items, m_app.hasKeepPrio(), m_app.hasAppendAtEnd())
        if (m_app.isAutoArchive) {
            archiveTasks(items)
        }
    }

    private fun undoCompleteTasks(task: TodoItem) {
        val tasks = ArrayList<TodoItem>()
        tasks.add(task)
        undoCompleteTasks(tasks)
    }

    private fun undoCompleteTasks(tasks: List<TodoItem>) {
        todoList.undoComplete(tasks)
        closeSelectionMode()
    }

    private fun deferTasks(tasks: List<TodoItem>, dateType: DateType) {
        var titleId = R.string.defer_due
        if (dateType === DateType.THRESHOLD) {
            titleId = R.string.defer_threshold
        }
        val d = createDeferDialog(this, titleId, object : InputDialogListener {
            override fun onClick(input: String) {
                if (input == "pick") {
                    val today = DateTime.today(TimeZone.getDefault())
                    val dialog = DatePickerDialog(this@MainActivity, DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                        var startMonth = month
                        startMonth++
                        val date = DateTime.forDateOnly(year, startMonth, day)
                        m_app.todoList.defer(date.format(Constants.DATE_FORMAT), tasks, dateType)
                        closeSelectionMode()
                    },
                            today.year!!,
                            today.month!! - 1,
                            today.day!!)
                    val showCalendar = m_app.showCalendar()

                    dialog.datePicker.calendarViewShown = showCalendar
                    dialog.datePicker.spinnersShown = !showCalendar
                    dialog.show()
                } else {

                    m_app.todoList.defer(input, tasks, dateType)
                    closeSelectionMode()
                }

            }
        })
        d.show()
    }

    private fun deleteTasks(tasks: List<TodoItem>) {
        m_app.showConfirmationDialog(this, R.string.delete_task_message, DialogInterface.OnClickListener { dialogInterface, i ->
            m_app.todoList.remove(tasks)
            closeSelectionMode()
        }, R.string.delete_task_title)
    }

    private fun archiveTasks(tasksToArchive: List<TodoItem>?) {
        todoList.archive(tasksToArchive)
        closeSelectionMode()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (m_drawerToggle != null && m_drawerToggle!!.onOptionsItemSelected(item)) {
            return true
        }
        log!!.info(TAG, "onMenuItemSelected: " + item.itemId)
        when (item.itemId) {
            R.id.search -> {
            }
            R.id.preferences -> startPreferencesActivity()
            R.id.filter -> startFilterActivity()
            R.id.share -> startActivityForResult(Intent(baseContext, TaskDisplayActivity::class.java), REQUEST_SHARE_PARTS)
            R.id.help -> showHelp()
            R.id.archive -> m_app.showConfirmationDialog(this, R.string.delete_task_message, DialogInterface.OnClickListener { dialogInterface, i -> archiveTasks(null) }, R.string.archive_task_title)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun startAddTaskActivity() {
        log!!.info(TAG, "Starting addTask activity")
        val intent = Intent(this, AddTask::class.java)
        mFilter!!.saveInIntent(intent)
        startActivity(intent)
    }

    private fun startPreferencesActivity() {
        val settingsActivity = Intent(baseContext,
                PreferencesActivity::class.java)
        startActivityForResult(settingsActivity, REQUEST_PREFERENCES)
    }

    /**
     * Handle clear filter click *
     */
    @Suppress("UNUSED")
    fun onClearClick(@Suppress("UNUSED_PARAMETER") v: View) = clearFilter()

    val savedFilters: ArrayList<ActiveFilter>
        get() {
            val saved_filters = ArrayList<ActiveFilter>()
            val saved_filter_ids = getSharedPreferences("filters", Context.MODE_PRIVATE)
            val filterIds = saved_filter_ids.getStringSet("ids", HashSet<String>())
            for (id in filterIds) {
                val filter_pref = getSharedPreferences(id, Context.MODE_PRIVATE)
                val filter = ActiveFilter()
                filter.initFromPrefs(filter_pref)
                filter.prefName = id
                saved_filters.add(filter)
            }
            return saved_filters
        }

    @Suppress("UNUSED")
    fun onAddFilterClick(@Suppress("UNUSED_PARAMETER") v: View) {
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
                updateRightDrawer()
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
        if (m_drawerLayout != null) {
            if (m_drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
                m_drawerLayout!!.closeDrawer(GravityCompat.START)
                return
            }
            if (m_drawerLayout!!.isDrawerOpen(GravityCompat.END)) {
                m_drawerLayout!!.closeDrawer(GravityCompat.END)
                return
            }
        }
        if (todoList.selectedTasks.size > 0) {
            closeSelectionMode()
            return
        }
        if (m_app.backClearsFilter() && mFilter != null && mFilter!!.hasFilter()) {
            clearFilter()
            onNewIntent(intent)
            return
        }

        super.onBackPressed()
    }

    private fun closeSelectionMode() {
        todoList.clearSelection()
        listView.clearChoices()
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.visibility = View.VISIBLE
        toolbar.visibility = View.GONE
        //getTodoList().clearSelectedTasks();
        populateMainMenu(options_menu)
        //updateDrawers();

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
            log!!.warn(TAG, "Not implemented search")
        } else if (intent.extras != null) {
            // Only change intent if it actually contains a filter
            setIntent(intent)
        }
        log!!.info(TAG, "onNewIntent: " + intent)

    }

    internal fun clearFilter() {
        // Also clear the intent so we wont get the old filter after
        // switching back to app later fixes [1c5271ee2e]
        val intent = Intent()
        mFilter!!.clear()
        mFilter!!.saveInIntent(intent)
        mFilter!!.saveInPrefs(m_app.prefs)
        setIntent(intent)
        closeSelectionMode()
        updateDrawers()
        m_adapter!!.setFilteredTasks()
    }

    private fun updateDrawers() {
        updateLeftDrawer()
        updateRightDrawer()
    }

    private fun updateRightDrawer() {
        val names = ArrayList<String>()
        val filters = savedFilters
        Collections.sort(filters) { f1, f2 -> f1.name!!.compareTo(f2.name!!, ignoreCase = true) }
        for (f in filters) {
            names.add(f.name!!)
        }
        m_rightDrawerList!!.adapter = ArrayAdapter(this, R.layout.drawer_list_item, names)
        m_rightDrawerList!!.choiceMode = AbsListView.CHOICE_MODE_NONE
        m_rightDrawerList!!.isLongClickable = true
        m_rightDrawerList!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            mFilter = filters[position]
            val intent = intent
            mFilter!!.saveInIntent(intent)
            setIntent(intent)
            mFilter!!.saveInPrefs(m_app.prefs)
            m_adapter!!.setFilteredTasks()
            if (m_drawerLayout != null) {
                m_drawerLayout!!.closeDrawer(GravityCompat.END)
            }
            updateDrawers()
        }
        m_rightDrawerList!!.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, view, position, id ->
            val filter = filters[position]
            val prefsName = filter.prefName!!
            val popupMenu = PopupMenu(this@MainActivity, view)
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
        val deleted_filter = ActiveFilter()
        deleted_filter.initFromPrefs(filter_prefs)
        filter_prefs.edit().clear().apply()
        val prefs_path = File(this.filesDir, "../shared_prefs")
        val prefs_xml = File(prefs_path, prefsName + ".xml")
        val deleted = prefs_xml.delete()
        if (!deleted) {
            log!!.warn(TAG, "Failed to delete saved filter: " + deleted_filter.name!!)
        }
        updateRightDrawer()
    }

    private fun updateSavedFilter(prefsName: String) {
        val filter_pref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val old_filter = ActiveFilter()
        old_filter.initFromPrefs(filter_pref)
        val filterName = old_filter.name
        mFilter!!.name = filterName
        mFilter!!.saveInPrefs(filter_pref)
        updateRightDrawer()
    }

    private fun renameSavedFilter(prefsName: String) {
        val filter_pref = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val old_filter = ActiveFilter()
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
                updateRightDrawer()
            }
        }

        alert.setNegativeButton("Cancel") { dialog, whichButton -> }

        alert.show()
    }


    private fun updateLeftDrawer() {
        val taskBag = todoList
        val decoratedContexts = sortWithPrefix(taskBag.decoratedContexts, m_app.sortCaseSensitive(), "@-")
        val decoratedProjects = sortWithPrefix(taskBag.decoratedProjects, m_app.sortCaseSensitive(), "+-")
        val drawerAdapter = DrawerAdapter(layoutInflater,
                m_app.listTerm,
                decoratedContexts,
                m_app.tagTerm,
                decoratedProjects)

        m_leftDrawerList!!.adapter = drawerAdapter
        m_leftDrawerList!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
        m_leftDrawerList!!.onItemClickListener = DrawerItemClickListener()

        for (context in mFilter!!.contexts) {
            val position = drawerAdapter.getIndexOf("@" + context)
            if (position != -1) {
                m_leftDrawerList!!.setItemChecked(position, true)
            }
        }

        for (project in mFilter!!.projects) {
            val position = drawerAdapter.getIndexOf("+" + project)
            if (position != -1) {
                m_leftDrawerList!!.setItemChecked(position, true)
            }
        }
        m_leftDrawerList!!.setItemChecked(drawerAdapter.contextHeaderPosition, mFilter!!.contextsNot)
        m_leftDrawerList!!.setItemChecked(drawerAdapter.projectsHeaderPosition, mFilter!!.projectsNot)
    }

    private val todoList: TodoList
        get() = m_app.todoList


    fun startFilterActivity() {
        val i = Intent(this, FilterActivity::class.java)
        mFilter!!.saveInIntent(i)
        startActivity(i)
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        val t = getTaskAt(position) ?: return false
        val selected = !listView.isItemChecked(position)
        if (selected) {
            t.selected = true
        } else {
            t.selected = false
        }
        listView.setItemChecked(position, selected)
        val numSelected = todoList.selectedTasks.size
        if (numSelected == 0) {
            closeSelectionMode()
        } else {
            openSelectionMode()
        }
        return true
    }

    private fun openSelectionMode() {
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        if (options_menu == null) {
            return
        }
        options_menu!!.clear()
        val inflater = menuInflater
        val menu = toolbar.menu
        menu.clear()
        inflater.inflate(R.menu.task_context, toolbar.menu)

        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            val checkedTasks = todoList.selectedTasks
            val menuId = item.itemId
            val intent: Intent
            when (menuId) {
                R.id.complete -> completeTasks(checkedTasks)
                R.id.select_all -> {
                    selectAllTasks()
                    return@OnMenuItemClickListener true
                }
                R.id.uncomplete -> undoCompleteTasks(checkedTasks)
                R.id.update -> startAddTaskActivity()
                R.id.delete -> deleteTasks(checkedTasks)
                R.id.archive -> archiveTasks(checkedTasks)
                R.id.defer_due -> deferTasks(checkedTasks, DateType.DUE)
                R.id.defer_threshold -> deferTasks(checkedTasks, DateType.THRESHOLD)
                R.id.priority -> {
                    prioritizeTasks(checkedTasks)
                    return@OnMenuItemClickListener true
                }
                R.id.share -> {
                    val shareText = selectedTasksAsString()
                    shareText(this@MainActivity, "Simpletask tasks", shareText)
                }
                R.id.calendar -> {
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
                    val calDate = GregorianCalendar()
                    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                            calDate.timeInMillis)
                    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                            calDate.timeInMillis + 60 * 60 * 1000)
                    startActivity(intent)
                }
                R.id.update_lists -> {
                    updateLists(checkedTasks)
                    return@OnMenuItemClickListener true
                }
                R.id.update_tags -> {
                    updateTags(checkedTasks)
                    return@OnMenuItemClickListener true
                }
            }
            true
        })
        if (!m_app.showCompleteCheckbox()) {
            menu.findItem(R.id.complete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.findItem(R.id.uncomplete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
    }


    val listView: ListView
        get() {
            val lv = findViewById(android.R.id.list)
            return lv as ListView
        }


    data class ViewHolder(var taskText: TextView? = null,
                          var taskAge: TextView? = null,
                          var taskDue: TextView? = null,
                          var taskThreshold: TextView? = null,
                          var cbCompleted: CheckBox? = null)

    inner class TaskAdapter(private val m_inflater: LayoutInflater, val mContext: Context) : BaseAdapter(), ListAdapter {


        internal var visibleLines = ArrayList<VisibleLine>()

        internal fun setFilteredTasks() {
            log.info(TAG, "setFilteredTasks called: " + todoList)
            val visibleTasks: List<TodoItem>

            val activeFilter = mFilter ?: return
            val sorts = activeFilter.getSort(m_app.defaultSorts)
            visibleTasks = todoList.getSortedTasksCopy(activeFilter, sorts, m_app.sortCaseSensitive())
            visibleLines.clear()

            log.info(TAG,"Adding headers..")
            var firstGroupSortIndex = 0
            if (sorts.size > 1 && sorts[0].contains("completed") || sorts[0].contains("future")) {
                firstGroupSortIndex++
                if (sorts.size > 2 && sorts[1].contains("completed") || sorts[1].contains("future")) {
                    firstGroupSortIndex++
                }
            }


            val firstSort = sorts[firstGroupSortIndex]
            visibleLines.addAll(addHeaderLines(visibleTasks, firstSort, getString(R.string.no_header)))
            log.info(TAG,"Adding headers..done")
            notifyDataSetChanged()
            updateFilterBar()
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

        override fun getCount(): Int {
            return visibleLines.size + 1
        }

        override fun getItem(position: Int): TodoItem? {
            val line = visibleLines[position]
            if (line.header) {
                return null
            }
            return line.item
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true // To change body of implemented methods use File |
            // Settings | File Templates.
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var view = convertView
            if (position == visibleLines.size) {
                if (view == null) {
                    view = m_inflater.inflate(R.layout.empty_list_item, parent, false)
                }
                return view
            }

            val line = visibleLines[position]
            if (line.header) {
                if (view == null) {
                    view = m_inflater.inflate(R.layout.list_header, parent, false)
                }
                val t = view!!.findViewById(R.id.list_header_title) as TextView
                t.text = line.title
                t.textSize = m_app.activeFontSize

            } else {
                var holder: ViewHolder
                if (view == null) {
                    view = m_inflater.inflate(R.layout.list_item, parent, false)
                    holder = ViewHolder()
                    holder.taskText = view!!.findViewById(R.id.tasktext) as TextView
                    holder.taskAge = view.findViewById(R.id.taskage) as TextView
                    holder.taskDue = view.findViewById(R.id.taskdue) as TextView
                    holder.taskThreshold = view.findViewById(R.id.taskthreshold) as TextView
                    holder.cbCompleted = view.findViewById(R.id.checkBox) as CheckBox
                    view.tag = holder
                } else {
                    holder = view.tag as ViewHolder
                }
                val task = line.item?.task
                if (task==null) return view

                if (m_app.showCompleteCheckbox()) {
                    holder.cbCompleted!!.visibility = View.VISIBLE
                } else {
                    holder.cbCompleted!!.visibility = View.GONE
                }
                if (!m_app.hasExtendedTaskView()) {
                    val taskBar = view.findViewById(R.id.datebar)
                    taskBar.visibility = View.GONE
                }
                var tokensToShow = ALL
                // Hide dates if we have a date bar
                if (m_app.hasExtendedTaskView()) {
                    tokensToShow = tokensToShow and COMPLETED_DATE.inv()
                    tokensToShow = tokensToShow and THRESHOLD_DATE.inv()
                    tokensToShow = tokensToShow and DUE_DATE.inv()
                }
                tokensToShow = tokensToShow and CREATION_DATE.inv()
                tokensToShow = tokensToShow and COMPLETED.inv()

                if (mFilter!!.hideLists) {
                    tokensToShow = tokensToShow and LIST.inv()
                }
                if (mFilter!!.hideTags) {
                    tokensToShow = tokensToShow and TTAG.inv()
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
                val taskText = holder.taskText!!
                val taskAge = holder.taskAge!!
                val taskDue = holder.taskDue!!
                val taskThreshold = holder.taskThreshold!!

                taskAge.textSize = m_app.activeFontSize * m_app.dateBarRelativeSize
                taskDue.textSize = m_app.activeFontSize * m_app.dateBarRelativeSize
                taskThreshold.textSize = m_app.activeFontSize * m_app.dateBarRelativeSize

                val cb = holder.cbCompleted!!
                taskText.text = ss

                handleEllipsis(holder.taskText as TextView)


                if (completed) {
                    // log.info( "Striking through " + task.getText());
                    taskText.paintFlags = taskText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    holder.taskAge!!.paintFlags = taskAge.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    cb.isChecked = true
                    cb.setOnClickListener({
                        line.item?.task?.markIncomplete()
                        closeSelectionMode()
                        todoList.save()
                    })
                } else {
                    taskText.paintFlags = taskText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    taskAge.paintFlags = taskAge.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    cb.isChecked = false

                    cb.setOnClickListener {
                        line.item?.task?.markComplete(m_app.today)
                        closeSelectionMode()
                        todoList.save()
                    }

                }

                val relAge = getRelativeAge(task, mContext)
                val relDue = getRelativeDueDate(task, m_app, ContextCompat.getColor(m_app, android.R.color.holo_green_light),
                        ContextCompat.getColor(m_app, android.R.color.holo_red_light),
                        m_app.hasColorDueDates())
                val relativeThresholdDate = getRelativeThresholdDate(task, mContext)
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
            }
            return view
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

        override fun getViewTypeCount(): Int {
            return 3
        }

        override fun isEmpty(): Boolean {
            return visibleLines.size == 0
        }

        override fun areAllItemsEnabled(): Boolean {
            return false
        }

        override fun isEnabled(position: Int): Boolean {
            if (position == visibleLines.size) {
                return false
            }

            if (visibleLines.size < position + 1) {
                return false
            }
            val line = visibleLines[position]
            return !line.header
        }
    }

    private fun handleEllipsis(taskText: TextView) {
        val noEllipsizeValue = "no_ellipsize"
        val ellipsizeKey = m_app.getString(R.string.task_text_ellipsizing_pref_key)
        val ellipsizePref = m_app.prefs.getString(ellipsizeKey, noEllipsizeValue)

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
                                  checkedItems: List<TodoItem>,
                                  allItems: ArrayList<String>,
                                  retrieveFromTask: (Task) -> SortedSet<String>,
                                  addToTask: (Task, String) -> Unit,
                                  removeFromTask: (Task, String) -> Unit
    ) {
        val checkedTaskItems = ArrayList<HashSet<String>>()
        for (item in checkedItems) {
            val items = HashSet<String>()
            items.addAll(retrieveFromTask(item.task))
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
        val layoutManager = LinearLayoutManager(this);
        rcv.layoutManager = layoutManager;
        val itemAdapter = ItemDialogAdapter(sortedAllItems, onAllTasks.toHashSet(), onSomeTasks.toHashSet())
        rcv.adapter = itemAdapter

        val ed = view.findViewById(R.id.editText) as EditText

        val builder = AlertDialog.Builder(this)
        builder.setView(view)

        builder.setPositiveButton(R.string.ok) { dialog, which ->
            val newText = ed.text.toString()
            if (newText.isNotEmpty()) {
                for (i in checkedItems) {
                    val t = i.task
                    addToTask(t, newText)
                }
            }
            val updatedValues = itemAdapter.currentState
            for (i in 0..updatedValues.lastIndex) {
                when (updatedValues[i] ) {
                    false -> {
                        for (item in checkedItems) {
                            val t = item.task
                            removeFromTask(t, sortedAllItems[i])
                        }
                    }
                    true -> {
                        for (item in checkedItems) {
                            val t = item.task
                            addToTask(t, sortedAllItems[i])
                        }
                    }
                }
            }
            todoList.update(checkedItems)
            closeSelectionMode()
            todoList.save()
        }
        builder.setNegativeButton(R.string.cancel) { dialog, id -> }
        // Create the AlertDialog
        val dialog = builder.create()
        dialog.setTitle(title)
        dialog.show()
    }

    private fun updateLists(checkedTasks: List<TodoItem>) {
        updateItemsDialog(
                m_app.listTerm,
                checkedTasks,
                sortWithPrefix(todoList.contexts, m_app.sortCaseSensitive(), null),
                { task -> task.lists },
                { task, list -> task.addList(list) },
                { task, list -> task.removeList(list) }
        )
    }

    private fun updateTags(checkedTasks: List<TodoItem>) {
        updateItemsDialog(
                m_app.tagTerm,
                checkedTasks,
                sortWithPrefix(todoList.projects, m_app.sortCaseSensitive(), null),
                { task -> task.tags },
                { task, tag -> task.addTag(tag) },
                { task, tag -> task.removeTag(tag) }
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
            mFilter!!.saveInPrefs(m_app.prefs)
            setIntent(intent)
            closeSelectionMode()
            m_adapter!!.setFilteredTasks()
        }
    }

    companion object {

        private val REQUEST_SHARE_PARTS = 1
        private val REQUEST_PREFERENCES = 2

        private val ACTION_LINK = "link"
        private val ACTION_SMS = "sms"
        private val ACTION_PHONE = "phone"
        private val ACTION_MAIL = "mail"

        val URI_BASE = Uri.fromParts("Simpletask", "", null)
        val URI_SEARCH = Uri.withAppendedPath(URI_BASE, "search")
        private val TAG = "Simpletask"
    }
}
