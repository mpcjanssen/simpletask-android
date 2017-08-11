package nl.mpcjanssen.simpletask

import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences

import nl.mpcjanssen.simpletask.task.*
import nl.mpcjanssen.simpletask.util.isEmptyOrNull
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.todayAsString
import org.json.JSONObject
import org.luaj.vm2.LuaError
import java.util.*

data class FilterOptions(val luaModule: String, val showSelected : Boolean = false)

/**
 * Active filter, has methods for serialization in several formats
 */
class ActiveFilter (val options : FilterOptions) {
    private val log: Logger
    var priorities = ArrayList<Priority>()
    var contexts = ArrayList<String>()
    var projects = ArrayList<String>()
    private var m_sorts: ArrayList<String>? = ArrayList()
    var projectsNot = false
    var search: String? = null
    var prioritiesNot = false
    var contextsNot = false
    var hideCompleted = false
    var hideFuture = false
    var hideHidden = true
    var hideLists = false
    var hideTags = false
    var hideCreateDate = false
    var createIsThreshold = false
    var useScript: Boolean = false
    var script: String? = null
    var scriptTestTask: String? = null

    override fun toString(): String {
        return join(m_sorts, ",")
    }

    // The name of the shared preference this filter came from
    var prefName: String? = null

    var name: String? = null

    init {
        log = Logger
    }

    val prefill
        get() : String  {
            val prefillLists = if (contexts.size == 1 && contexts[0] != "-") "@${contexts[0]}" else ""
            val prefillTags = if (projects.size == 1 && projects[0] != "-") "+${projects[0]}" else ""
            return " $prefillLists $prefillTags".trimEnd()
        }

    fun saveInJSON(json: JSONObject) {
        json.put(INTENT_TITLE, name)
        json.put(INTENT_CONTEXTS_FILTER, join(contexts, "\n"))
        json.put(INTENT_CONTEXTS_FILTER_NOT, contextsNot)
        json.put(INTENT_PROJECTS_FILTER, join(projects, "\n"))
        json.put(INTENT_PROJECTS_FILTER_NOT, projectsNot)
        json.put(INTENT_PRIORITIES_FILTER, join(Priority.inCode(priorities), "\n"))
        json.put(INTENT_PRIORITIES_FILTER_NOT, prioritiesNot)
        json.put(INTENT_SORT_ORDER, join(m_sorts, "\n"))
        json.put(INTENT_HIDE_COMPLETED_FILTER, hideCompleted)
        json.put(INTENT_HIDE_FUTURE_FILTER, hideFuture)
        json.put(INTENT_HIDE_LISTS_FILTER, hideLists)
        json.put(INTENT_HIDE_TAGS_FILTER, hideTags)
        json.put(INTENT_HIDE_CREATE_DATE_FILTER, hideCreateDate)
        json.put(INTENT_HIDE_HIDDEN_FILTER, hideHidden)
        json.put(INTENT_CREATE_AS_THRESHOLD, createIsThreshold)
        json.put(INTENT_SCRIPT_FILTER, script)
        json.put(INTENT_USE_SCRIPT_FILTER, useScript)
        json.put(INTENT_SCRIPT_TEST_TASK_FILTER, scriptTestTask)
        json.put(SearchManager.QUERY, search)
    }

    fun initFromJSON(json: JSONObject?) {
        val prios: String?
        val projects: String?
        val contexts: String?
        val sorts: String?

        if (json == null) {
            return
        }
        name = json.optString(INTENT_TITLE, "No title")
        prios = json.optString(INTENT_PRIORITIES_FILTER)
        projects = json.optString(INTENT_PROJECTS_FILTER)
        contexts = json.optString(INTENT_CONTEXTS_FILTER)
        sorts = json.optString(INTENT_SORT_ORDER)

        useScript = json.optBoolean(INTENT_USE_SCRIPT_FILTER, false)
        script = json.optString(INTENT_SCRIPT_FILTER)
        scriptTestTask = json.optString(INTENT_SCRIPT_TEST_TASK_FILTER)

        prioritiesNot = json.optBoolean(INTENT_PRIORITIES_FILTER_NOT)
        projectsNot = json.optBoolean(INTENT_PROJECTS_FILTER_NOT)
        contextsNot = json.optBoolean(INTENT_CONTEXTS_FILTER_NOT)
        hideCompleted = json.optBoolean(INTENT_HIDE_COMPLETED_FILTER)
        hideFuture = json.optBoolean(
                INTENT_HIDE_FUTURE_FILTER)
        hideLists = json.optBoolean(
                INTENT_HIDE_LISTS_FILTER)
        hideTags = json.optBoolean(
                INTENT_HIDE_TAGS_FILTER)
        hideCreateDate = json.optBoolean(
                INTENT_HIDE_CREATE_DATE_FILTER)
        hideHidden = json.optBoolean(
                INTENT_HIDE_HIDDEN_FILTER)
        createIsThreshold = json.optBoolean(
                INTENT_CREATE_AS_THRESHOLD)
        search = json.optString(SearchManager.QUERY)
        if (sorts != null && sorts != "") {
            m_sorts = ArrayList(
                    Arrays.asList(*sorts.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        }
        if (prios != null && prios != "") {
            priorities = Priority.toPriority(Arrays.asList(*prios.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        }
        if (projects != null && projects != "") {
            this.projects = ArrayList(Arrays.asList(*projects.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        }
        if (contexts != null && contexts != "") {
            this.contexts = ArrayList(Arrays.asList(*contexts.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        }
    }

    fun hasFilter(): Boolean {
        return contexts.size + projects.size + priorities.size > 0
                || !isEmptyOrNull(search) || LuaInterpreter.hasFilterCallback(options.luaModule)
    }

    fun getTitle(visible: Int, total: Long, prio: CharSequence, tag: CharSequence, list: CharSequence, search: CharSequence, script: CharSequence, filterApplied: CharSequence, noFilter: CharSequence): String {
        var filterTitle = "" + filterApplied
        if (hasFilter()) {
            filterTitle = "($visible/$total) $filterTitle"
            val activeParts = ArrayList<String>()
            if (priorities.size > 0) {
                activeParts.add(prio.toString())
            }

            if (projects.size > 0) {
                activeParts.add(tag.toString())
            }

            if (contexts.size > 0) {
                activeParts.add(list.toString())
            }
            if (!isEmptyOrNull(this.search)) {
                activeParts.add(search.toString())
            }
            if (!isEmptyOrNull(this.script)) {
                activeParts.add(script.toString())
            }
            filterTitle = filterTitle + " " + join(activeParts, ",")
        } else {
            filterTitle = "" + noFilter
        }
        return filterTitle
    }

    val proposedName: String
        get() {
            val appliedFilters = ArrayList<String>()
            appliedFilters.addAll(contexts)
            appliedFilters.remove("-")
            appliedFilters.addAll(Priority.inCode(priorities))
            appliedFilters.addAll(projects)
            appliedFilters.remove("-")
            if (appliedFilters.size == 1) {
                return appliedFilters[0]
            } else {
                return ""
            }
        }

    fun getSort(defaultSort: Array<String>?): ArrayList<String> {
        var sorts = m_sorts
        if (sorts == null || sorts.size == 0
                || isEmptyOrNull(sorts[0])) {
            // Set a default sort
            sorts = ArrayList<String>()
            if (defaultSort == null) return sorts
            for (type in defaultSort) {
                sorts.add(NORMAL_SORT + SORT_SEPARATOR
                        + type)
            }

        }
        return sorts
    }

    fun saveInIntent(target: Intent?) {
        if (target != null) {
            val json = JSONObject()
            this.saveInJSON(json)
            target.putExtra(INTENT_JSON, json.toString(2))
        }
    }

    fun saveInPrefs(prefs: SharedPreferences?) {
        if (prefs != null) {
            val editor = prefs.edit()
            val json = JSONObject()
            this.saveInJSON(json)
            editor.putString(INTENT_JSON, json.toString(2))
            editor.commit()
        }
    }

    fun clear() {
        priorities = ArrayList<Priority>()
        contexts = ArrayList<String>()
        projects = ArrayList<String>()
        projectsNot = false
        search = null
        prioritiesNot = false
        contextsNot = false
        useScript = false
    }

    fun apply(items: Sequence<Task>?): Sequence<Task> {
        if (useScript) {
            log.info(TAG, "Filtering with Lua $script")
        }
        val filter = AndFilter()
        if (items == null) {
            return emptySequence()
        }
        val code = if (useScript) { script } else { null }
        val today = todayAsString
        try {
            log.info(TAG, "Resetting onFilter callback in module ${options.luaModule}")
            LuaInterpreter.clearOnFilter(options.luaModule)
            LuaInterpreter.evalScript(options.luaModule, code)
            return items.filter {
                    if (options.showSelected && TodoList.isSelected(it)) {
                        return@filter true
                    }
                    if (this.hideCompleted && it.isCompleted()) {
                        return@filter false
                    }
                    if (this.hideFuture && it.inFuture(today)) {
                        return@filter false
                    }
                    if (this.hideHidden && it.isHidden()) {
                        return@filter false
                    }
                    if ("" == it.inFileFormat().trim { it <= ' ' }) {
                        return@filter false
                    }
                    if (!filter.apply(it)) {
                        return@filter false
                    }
                    if (useScript && !LuaInterpreter.onFilterCallback(options.luaModule, it)) {
                        return@filter false
                    }
                return@filter true
                }
            } catch (e: LuaError) {
                log.debug(TAG, "Lua execution failed " + e.message)
            }
        return emptySequence()
    }

    fun setSort(sort: ArrayList<String>) {
        this.m_sorts = sort
    }

    private inner class AndFilter {
        private val filters = ArrayList<TaskFilter>()

        init {
            filters.clear()
            if (priorities.size > 0) {
                addFilter(ByPriorityFilter(priorities, prioritiesNot))
            }
            if (contexts.size > 0) {
                addFilter(ByContextFilter(contexts, contextsNot))
            }
            if (projects.size > 0) {
                addFilter(ByProjectFilter(projects, projectsNot))
            }

            if (!isEmptyOrNull(search)) {
                addFilter(ByTextFilter(options.luaModule, search, false))
            }
        }

        fun addFilter(filter: TaskFilter?) {
            if (filter != null) {
                filters.add(filter)
            }
        }

        fun apply(input: Task): Boolean {
            for (f in filters) {
                if (!f.apply(input)) {
                    return false
                }
            }
            return true
        }
    }

    companion object {
        internal val TAG = ActiveFilter::class.java.simpleName
        const val NORMAL_SORT = "+"
        const val REVERSED_SORT = "-"
        const val SORT_SEPARATOR = "!"

        /* Strings used in intent extras and other_preferences
     * Do NOT modify this without good reason.
     * Changing this will break existing shortcuts and widgets
     */
        const val INTENT_TITLE = "TITLE"
        const val INTENT_JSON = "JSON"
        const val INTENT_SORT_ORDER = "SORTS"
        const val INTENT_CONTEXTS_FILTER = "CONTEXTS"
        const val INTENT_PROJECTS_FILTER = "PROJECTS"
        const val INTENT_PRIORITIES_FILTER = "PRIORITIES"
        const val INTENT_CONTEXTS_FILTER_NOT = "CONTEXTSnot"
        const val INTENT_PROJECTS_FILTER_NOT = "PROJECTSnot"
        const val INTENT_PRIORITIES_FILTER_NOT = "PRIORITIESnot"

        const val INTENT_HIDE_COMPLETED_FILTER = "HIDECOMPLETED"
        const val INTENT_HIDE_FUTURE_FILTER = "HIDEFUTURE"
        const val INTENT_HIDE_LISTS_FILTER = "HIDELISTS"
        const val INTENT_HIDE_TAGS_FILTER = "HIDETAGS"
        const val INTENT_HIDE_HIDDEN_FILTER = "HIDEHIDDEN"
        const val INTENT_HIDE_CREATE_DATE_FILTER = "HIDECREATEDATE"

        const val INTENT_CREATE_AS_THRESHOLD = "CREATEISTHRESHOLD"

        const val INTENT_USE_SCRIPT_FILTER = "USE_SCRIPT"
        const val INTENT_LUA_MODULE = "MODULE"
        const val INTENT_SCRIPT_FILTER = "LUASCRIPT"
        const val INTENT_SCRIPT_TEST_TASK_FILTER = "LUASCRIPT_TEST_TASK"

        const val INTENT_EXTRA_DELIMITERS = "\n|,"
    }

    fun initFromIntent(intent: Intent) {
        if (intent.hasExtra(INTENT_JSON)) {
            val jsonFromIntent = intent.getStringExtra(INTENT_JSON)
            initFromJSON(JSONObject(jsonFromIntent))
        } else {
            // Older non JSON version of filter. Use legacy loading
            val prios: String?
            val projects: String?
            val contexts: String?
            val sorts: String?

            prios = intent.getStringExtra(INTENT_PRIORITIES_FILTER)
            projects = intent.getStringExtra(INTENT_PROJECTS_FILTER)
            contexts = intent.getStringExtra(INTENT_CONTEXTS_FILTER)
            sorts = intent.getStringExtra(INTENT_SORT_ORDER)

            script = intent.getStringExtra(INTENT_SCRIPT_FILTER)
            scriptTestTask = intent.getStringExtra(INTENT_SCRIPT_TEST_TASK_FILTER)

            prioritiesNot = intent.getBooleanExtra(
                    INTENT_PRIORITIES_FILTER_NOT, false)
            projectsNot = intent.getBooleanExtra(
                    INTENT_PROJECTS_FILTER_NOT, false)
            contextsNot = intent.getBooleanExtra(
                    INTENT_CONTEXTS_FILTER_NOT, false)
            hideCompleted = intent.getBooleanExtra(
                    INTENT_HIDE_COMPLETED_FILTER, false)
            hideFuture = intent.getBooleanExtra(
                    INTENT_HIDE_FUTURE_FILTER, false)
            hideLists = intent.getBooleanExtra(
                    INTENT_HIDE_LISTS_FILTER, false)
            hideTags = intent.getBooleanExtra(
                    INTENT_HIDE_TAGS_FILTER, false)
            hideCreateDate = intent.getBooleanExtra(
                    INTENT_HIDE_CREATE_DATE_FILTER, false)
            hideHidden = intent.getBooleanExtra(
                    INTENT_HIDE_HIDDEN_FILTER, true)
            search = intent.getStringExtra(SearchManager.QUERY)
            if (sorts != null && sorts != "") {
                m_sorts = ArrayList(
                        Arrays.asList(*sorts.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            }
            if (prios != null && prios != "") {
                priorities = Priority.toPriority(Arrays.asList(*prios.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            }
            if (projects != null && projects != "") {
                this.projects = ArrayList(Arrays.asList(*projects.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            }
            if (contexts != null && contexts != "") {
                this.contexts = ArrayList(Arrays.asList(*contexts.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            }
        }
    }

    fun initFromPrefs(prefs: SharedPreferences) {
        val jsonFromPref = prefs.getString(INTENT_JSON, null)
        if (jsonFromPref != null) {
            initFromJSON(JSONObject(jsonFromPref))
        } else {
            // Older non JSON version of filter. Use legacy loading
            m_sorts = ArrayList<String>()
            m_sorts!!.addAll(Arrays.asList(*prefs.getString(INTENT_SORT_ORDER, "")!!.split(INTENT_EXTRA_DELIMITERS.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            contexts = ArrayList(prefs.getStringSet(
                    INTENT_CONTEXTS_FILTER, emptySet<String>()))
            priorities = Priority.toPriority(ArrayList(prefs.getStringSet(INTENT_PRIORITIES_FILTER, emptySet<String>())))
            projects = ArrayList(prefs.getStringSet(
                    INTENT_PROJECTS_FILTER, emptySet<String>()))
            contextsNot = prefs.getBoolean(INTENT_CONTEXTS_FILTER_NOT, false)
            prioritiesNot = prefs.getBoolean(INTENT_PRIORITIES_FILTER_NOT, false)
            projectsNot = prefs.getBoolean(INTENT_PROJECTS_FILTER_NOT, false)
            hideCompleted = prefs.getBoolean(INTENT_HIDE_COMPLETED_FILTER, false)
            hideFuture = prefs.getBoolean(INTENT_HIDE_FUTURE_FILTER, false)
            hideLists = prefs.getBoolean(INTENT_HIDE_LISTS_FILTER, false)
            hideTags = prefs.getBoolean(INTENT_HIDE_TAGS_FILTER, false)
            hideCreateDate = prefs.getBoolean(INTENT_HIDE_CREATE_DATE_FILTER, false)
            hideHidden = prefs.getBoolean(INTENT_HIDE_HIDDEN_FILTER, true)
            name = prefs.getString(INTENT_TITLE, "Simpletask")
            search = prefs.getString(SearchManager.QUERY, null)
            script = prefs.getString(INTENT_SCRIPT_FILTER, null)
            scriptTestTask = prefs.getString(INTENT_SCRIPT_TEST_TASK_FILTER, null)
        }
    }

}
