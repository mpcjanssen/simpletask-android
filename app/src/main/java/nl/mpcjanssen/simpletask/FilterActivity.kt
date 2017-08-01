package nl.mpcjanssen.simpletask

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import nl.mpcjanssen.simpletask.remote.FileStore
import nl.mpcjanssen.simpletask.remote.FileStoreInterface
import nl.mpcjanssen.simpletask.task.Priority
import nl.mpcjanssen.simpletask.task.TodoList
import nl.mpcjanssen.simpletask.util.*
import java.io.File
import java.io.IOException
import java.util.*

class FilterActivity : ThemedNoActionBarActivity() {

    internal var asWidgetConfigure = false
    internal var asWidgetReConfigure = false
    internal lateinit var mFilter: ActiveFilter

    internal lateinit var m_app: TodoApplication
    val prefs = Config.prefs

    private var pager: ViewPager? = null
    private var m_menu: Menu? = null
    private var log = Logger
    private var pagerAdapter: ScreenSlidePagerAdapter? = null
    private var scriptFragment: FilterScriptFragment? = null
    private var m_page = 0

    override fun onBackPressed() {
        if (!asWidgetConfigure && !asWidgetReConfigure) {
            applyFilter()
        }
        super.onBackPressed()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.info(TAG, "Called with intent: " + intent.toString())
        m_app = application as TodoApplication

        setContentView(R.layout.filter)
        val toolbar = findViewById(R.id.toolbar_edit_filter) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)

        var arguments: Bundle

        val intent = intent
        var environment: String = "mainui"
        if (intent.action != null) {
            asWidgetConfigure = getIntent().action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            environment = "widget" + getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0).toString()
        }

        mFilter = ActiveFilter(FilterOptions(luaModule = environment))
        val context = applicationContext

        if (asWidgetConfigure) {
            if (intent.getBooleanExtra(Constants.EXTRA_WIDGET_RECONFIGURE, false)) {
                asWidgetReConfigure = true
                asWidgetConfigure = false
                setTitle(R.string.config_widget)
                val preferences = context.getSharedPreferences("" + intent.getIntExtra(Constants.EXTRA_WIDGET_ID, -1), Context.MODE_PRIVATE)
                mFilter.initFromPrefs(preferences)
            } else {
                setTitle(R.string.create_widget)
                mFilter.initFromPrefs(prefs)
            }
        } else {
            mFilter.initFromIntent(intent)
        }

        pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)

        // Fill arguments for fragment
        arguments = Bundle()
        arguments.putStringArrayList(FILTER_ITEMS,
                alfaSortList(TodoList.contexts, Config.sortCaseSensitive, "-"))
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, mFilter.contexts)
        arguments.putBoolean(INITIAL_NOT, mFilter.contextsNot)
        arguments.putString(TAB_TYPE, CONTEXT_TAB)
        val contextTab = FilterListFragment()
        contextTab.arguments = arguments
        pagerAdapter!!.add(contextTab)

        // Fill arguments for fragment
        arguments = Bundle()
        arguments.putStringArrayList(FILTER_ITEMS,
                alfaSortList(TodoList.projects, Config.sortCaseSensitive, "-"))
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, mFilter.projects)
        arguments.putBoolean(INITIAL_NOT, mFilter.projectsNot)
        arguments.putString(TAB_TYPE, PROJECT_TAB)
        val projectTab = FilterListFragment()
        projectTab.arguments = arguments
        pagerAdapter!!.add(projectTab)

        // Fill arguments for fragment
        arguments = Bundle()
        arguments.putStringArrayList(FILTER_ITEMS, Priority.inCode(TodoList.priorities))
        arguments.putStringArrayList(INITIAL_SELECTED_ITEMS, Priority.inCode(mFilter.priorities))
        arguments.putBoolean(INITIAL_NOT, mFilter.prioritiesNot)
        arguments.putString(TAB_TYPE, PRIO_TAB)
        val prioTab = FilterListFragment()
        prioTab.arguments = arguments
        pagerAdapter!!.add(prioTab)

        // Fill arguments for fragment
        arguments = Bundle()
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, mFilter.hideCompleted)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, mFilter.hideFuture)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, mFilter.hideLists)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, mFilter.hideTags)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, mFilter.hideCreateDate)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_HIDDEN_FILTER, mFilter.hideHidden)
        arguments.putBoolean(ActiveFilter.INTENT_CREATE_AS_THRESHOLD, mFilter.createIsThreshold)
        arguments.putString(TAB_TYPE, OTHER_TAB)
        val otherTab = FilterOtherFragment()
        otherTab.arguments = arguments
        pagerAdapter!!.add(otherTab)

        // Fill arguments for fragment
        arguments = Bundle()
        arguments.putStringArrayList(FILTER_ITEMS, mFilter.getSort(Config.defaultSorts))
        arguments.putString(TAB_TYPE, SORT_TAB)
        val sortTab = FilterSortFragment()
        sortTab.arguments = arguments
        pagerAdapter!!.add(sortTab)

        arguments = Bundle()
        arguments.putString(ActiveFilter.INTENT_LUA_MODULE, environment)

        arguments.putBoolean(ActiveFilter.INTENT_USE_SCRIPT_FILTER, mFilter.useScript)
        arguments.putString(ActiveFilter.INTENT_SCRIPT_FILTER, mFilter.script)
        arguments.putString(ActiveFilter.INTENT_SCRIPT_TEST_TASK_FILTER, mFilter.scriptTestTask)
        arguments.putString(TAB_TYPE, SCRIPT_TAB)
        val scriptTab = FilterScriptFragment()
        scriptFragment = scriptTab
        scriptTab.arguments = arguments
        pagerAdapter!!.add(scriptTab)

        pager = findViewById(R.id.pager) as ViewPager
        pager!!.adapter = pagerAdapter
        // Give the TabLayout the ViewPager
        val tabLayout = findViewById(R.id.sliding_tabs) as TabLayout
        tabLayout.setupWithViewPager(pager as ViewPager)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        pager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                return
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                return
            }

            override fun onPageSelected(position: Int) {
                log.info(TAG, "Page $position selected")
                m_page = position
            }
        })
        val activePage = prefs.getInt(getString(R.string.last_open_filter_tab), 0)
        if (activePage < pagerAdapter?.count ?: 0) {
            pager?.setCurrentItem(activePage, false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        val inflater = menuInflater
        inflater.inflate(R.menu.filter, menu)
        m_menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_filter_action ->
                if (asWidgetConfigure) {
                    askWidgetName()
                } else if (asWidgetReConfigure) {
                    updateWidget()
                    finish()
                } else {
                    applyFilter()
                }
            R.id.menu_filter_load_script -> openScript(object : FileStoreInterface.FileReadListener {
                override fun fileRead(contents: String?) {
                    runOnMainThread(
                            Runnable { setScript(contents) })
                }
            })
        }
        return true
    }

    private fun openScript(file_read: FileStoreInterface.FileReadListener) {
        runOnMainThread(Runnable {
            val dialog = FileStore.FileDialog(this@FilterActivity, File(Config.todoFileName).parent, false)
            dialog.addFileListener(object : FileStoreInterface.FileSelectedListener {
                override fun fileSelected(file: String) {
                    Thread(Runnable {
                        try {

                            FileStore.readFile(file, file_read)
                        } catch (e: IOException) {
                            showToastShort(this@FilterActivity, "Failed to load script.")
                            e.printStackTrace()
                        }
                    }).start()
                }
            })
            dialog.createFileDialog(this@FilterActivity, FileStore)
        })
    }

    private fun createFilterIntent(): Intent {
        val target = Intent(this, Simpletask::class.java)
        target.action = Constants.INTENT_START_FILTER
        updateFilterFromFragments()
        mFilter.name = mFilter.proposedName
        mFilter.saveInIntent(target)

        target.putExtra("name", mFilter.proposedName)
        return target
    }

    private fun updateFilterFromFragments() {
        for (f in pagerAdapter!!.fragments) {
            when (f.arguments.getString(TAB_TYPE, "")) {
                "" -> {
                }
                OTHER_TAB -> {
                    val of = f as FilterOtherFragment
                    mFilter.hideCompleted = of.hideCompleted
                    mFilter.hideFuture = of.hideFuture
                    mFilter.hideLists = of.hideLists
                    mFilter.hideTags = of.hideTags
                    mFilter.hideCreateDate = of.hideCreateDate
                    mFilter.hideHidden = of.hideHidden
                    mFilter.createIsThreshold = of.createAsThreshold
                }
                CONTEXT_TAB -> {
                    val lf = f as FilterListFragment
                    mFilter.contexts = lf.getSelectedItems()
                    mFilter.contextsNot = lf.getNot()
                }
                PROJECT_TAB -> {
                    val pf = f as FilterListFragment
                    mFilter.projects = pf.getSelectedItems()
                    mFilter.projectsNot = pf.getNot()
                }
                PRIO_TAB -> {
                    val prf = f as FilterListFragment
                    mFilter.priorities = Priority.toPriority(prf.getSelectedItems())
                    mFilter.prioritiesNot = prf.getNot()
                }
                SORT_TAB -> {
                    val sf = f as FilterSortFragment
                    mFilter.setSort(sf.selectedItem)
                }
                SCRIPT_TAB -> {
                    val scrf = f as FilterScriptFragment
                    mFilter.useScript = scrf.useScript
                    mFilter.script = scrf.script
                    mFilter.scriptTestTask = scrf.testTask
                }
            }
        }
    }

    private fun setScript(script: String?) {
        if (scriptFragment == null) {
            // fragment was never intialized
            showToastShort(this, "Script tab not visible??")
        } else {
            script?.let { scriptFragment!!.script = script }
        }
    }

    private fun updateWidget() {
        updateFilterFromFragments()
        val widgetId = intent.getIntExtra(Constants.EXTRA_WIDGET_ID, 0)
        log.info(TAG, "Saving settings for widget $widgetId")
        val preferences = applicationContext.getSharedPreferences("" + widgetId, Context.MODE_PRIVATE)
        mFilter.saveInPrefs(preferences)
        broadcastRefreshWidgets(m_app.localBroadCastManager)
    }

    private fun createWidget(name: String) {
        val mAppWidgetId: Int

        val intent = intent
        val extras = intent.extras
        updateFilterFromFragments()
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)

            val context = applicationContext

            // Store widget filter
            val preferences = context.getSharedPreferences("" + mAppWidgetId, Context.MODE_PRIVATE)
            mFilter.name = name
            mFilter.saveInPrefs(preferences)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            MyAppWidgetProvider.updateAppWidget(context, appWidgetManager,
                    mAppWidgetId, name)

            val resultValue = Intent(applicationContext, AppWidgetService::class.java)
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    private fun applyFilter() {
        val data = createFilterIntent()
        startActivity(data)
        finish()
    }

    private fun askWidgetName() {
        val name: String
        val alert = AlertDialog.Builder(this)

        alert.setTitle("Create widget")
        alert.setMessage("Widget title")
        updateFilterFromFragments()
        name = mFilter.proposedName

        // Set an EditText view to get user input
        val input = EditText(this)
        alert.setView(input)
        input.setText(name)

        alert.setPositiveButton("Ok") { dialog, whichButton ->
            val value = input.text.toString()
            if (value == "") {
                showToastShort(applicationContext, R.string.widget_name_empty)
            } else {
                createWidget(value)
            }
        }

        alert.setNegativeButton("Cancel") { dialog, whichButton -> }

        alert.show()

    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.edit().putInt(getString(R.string.last_open_filter_tab), m_page).commit()
        pager?.clearOnPageChangeListeners()
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        val fragments: ArrayList<Fragment>

        init {
            fragments = ArrayList<Fragment>()
        }

        fun add(frag: Fragment) {
            fragments.add(frag)
        }

        override fun getPageTitle(position: Int): CharSequence {
            val f = fragments[position]
            val type = f.arguments.getString(TAB_TYPE, "unknown")
            when (type) {
                PROJECT_TAB -> return Config.tagTerm
                CONTEXT_TAB -> return Config.listTerm
                else -> return type
            }
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

    }

    companion object {

        val TAG = "FilterActivity"
        val TAB_TYPE = "type"
        val CONTEXT_TAB = "context"
        val PROJECT_TAB = "project"
        val PRIO_TAB = getString(R.string.filter_tab_header_prio)
        val OTHER_TAB = getString(R.string.filter_tab_header_other)
        val SORT_TAB = getString(R.string.filter_tab_header_sort)
        val SCRIPT_TAB = getString(R.string.filter_tab_header_script)

        // Constants for saving state
        val FILTER_ITEMS = "items"
        val INITIAL_SELECTED_ITEMS = "initialSelectedItems"
        val INITIAL_NOT = "initialNot"
    }
}

