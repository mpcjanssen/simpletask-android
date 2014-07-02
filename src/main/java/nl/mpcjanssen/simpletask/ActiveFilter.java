package nl.mpcjanssen.simpletask;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import nl.mpcjanssen.simpletask.task.ByContextFilter;
import nl.mpcjanssen.simpletask.task.ByPriorityFilter;
import nl.mpcjanssen.simpletask.task.ByProjectFilter;
import nl.mpcjanssen.simpletask.task.ByTextFilter;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskFilter;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;

/**
 * Active filter, has methods for serialization in several formats
 */
public class ActiveFilter {
    static public final String NORMAL_SORT = "+";
    static public final String REVERSED_SORT = "-";
    static public final String SORT_SEPARATOR = "!";

    /* Strings used in intent extras and preferences
     * Do NOT modify this without good reason.
     * Changing this will break existing shortcuts and widgets
     */
    public final static String INTENT_TITLE = "TITLE";
    public final static String INTENT_SORT_ORDER = "SORTS";
    public final static String INTENT_CONTEXTS_FILTER = "CONTEXTS";
    public final static String INTENT_PROJECTS_FILTER = "PROJECTS";
    public final static String INTENT_PRIORITIES_FILTER = "PRIORITIES";
    public final static String INTENT_CONTEXTS_FILTER_NOT = "CONTEXTSnot";
    public final static String INTENT_PROJECTS_FILTER_NOT = "PROJECTSnot";
    public final static String INTENT_PRIORITIES_FILTER_NOT = "PRIORITIESnot";

    public final static String INTENT_HIDE_COMPLETED_FILTER = "HIDECOMPLETED";
    public final static String INTENT_HIDE_FUTURE_FILTER = "HIDEFUTURE";
    public final static String INTENT_HIDE_LISTS_FILTER = "HIDELISTS";
    public final static String INTENT_HIDE_TAGS_FILTER =  "HIDETAGS";

    public final static String INTENT_EXTRA_DELIMITERS = "\n|,";

    private ArrayList<Priority> m_prios = new ArrayList<Priority>();
    private ArrayList<String> m_contexts = new ArrayList<String>();
    private ArrayList<String> m_projects = new ArrayList<String>();
    private ArrayList<String> m_sorts = new ArrayList<String>();
    private boolean m_projectsNot = false;
    private String m_search;
    private boolean m_priosNot = false;
    private boolean m_contextsNot = false;
    private boolean m_hideCompleted = false;
    private boolean m_hideFuture = false;
    private boolean m_hideLists = false;
    private boolean m_hideTags = false;

    public String getPrefName() {
        return mPrefName;
    }

    public void setPrefName(String mPrefName) {
        this.mPrefName = mPrefName;
    }

    // The name of the shared preference this filter came from
    private String mPrefName;

    private String mName;

    public ActiveFilter() {
    }

    public void initFromIntent(Intent intent) {
        String prios;
        String projects;
        String contexts;
        String sorts;

        prios = intent.getStringExtra(INTENT_PRIORITIES_FILTER);
        projects = intent.getStringExtra(INTENT_PROJECTS_FILTER);
        contexts = intent.getStringExtra(INTENT_CONTEXTS_FILTER);
        sorts = intent.getStringExtra(INTENT_SORT_ORDER);
        m_priosNot = intent.getBooleanExtra(
                INTENT_PRIORITIES_FILTER_NOT, false);
        m_projectsNot = intent.getBooleanExtra(
                INTENT_PROJECTS_FILTER_NOT, false);
        m_contextsNot = intent.getBooleanExtra(
                INTENT_CONTEXTS_FILTER_NOT, false);
        m_hideCompleted = intent.getBooleanExtra(
                INTENT_HIDE_COMPLETED_FILTER, false);
        m_hideFuture = intent.getBooleanExtra(
                INTENT_HIDE_FUTURE_FILTER, false);
        m_hideLists = intent.getBooleanExtra(
                INTENT_HIDE_LISTS_FILTER, false);
        m_hideTags = intent.getBooleanExtra(
                INTENT_HIDE_TAGS_FILTER, false);
        m_search = intent.getStringExtra(SearchManager.QUERY);
        if (sorts != null && !sorts.equals("")) {
            m_sorts = new ArrayList<String>(
                    Arrays.asList(sorts.split(INTENT_EXTRA_DELIMITERS)));
        }
        if (prios != null && !prios.equals("")) {
            m_prios = Priority.toPriority(Arrays.asList(prios.split(INTENT_EXTRA_DELIMITERS)));
        }
        if (projects != null && !projects.equals("")) {
            m_projects = new ArrayList<String>(Arrays.asList(projects
                    .split(INTENT_EXTRA_DELIMITERS)));
        }
        if (contexts != null && !contexts.equals("")) {
            m_contexts = new ArrayList<String>(Arrays.asList(contexts
                    .split(INTENT_EXTRA_DELIMITERS)));
        }
    }

    public void initFromPrefs(SharedPreferences prefs) {
        m_sorts = new ArrayList<String>();
        m_sorts.addAll(Arrays.asList(prefs.getString(INTENT_SORT_ORDER, "")
                .split(INTENT_EXTRA_DELIMITERS)));
        m_contexts = new ArrayList<String>(prefs.getStringSet(
                INTENT_CONTEXTS_FILTER, Collections.<String>emptySet()));
        m_prios = Priority.toPriority(new ArrayList<String>(prefs
                .getStringSet(INTENT_PRIORITIES_FILTER, Collections.<String>emptySet())));
        m_projects = new ArrayList<String>(prefs.getStringSet(
                INTENT_PROJECTS_FILTER, Collections.<String>emptySet()));
        m_contextsNot = prefs.getBoolean(INTENT_CONTEXTS_FILTER_NOT, false);
        m_priosNot = prefs.getBoolean(INTENT_PRIORITIES_FILTER_NOT, false);
        m_projectsNot = prefs.getBoolean(INTENT_PROJECTS_FILTER_NOT, false);
        m_hideCompleted = prefs.getBoolean(INTENT_HIDE_COMPLETED_FILTER, false);
        m_hideFuture = prefs.getBoolean(INTENT_HIDE_FUTURE_FILTER, false);
        m_hideLists = prefs.getBoolean(INTENT_HIDE_LISTS_FILTER, false);
        m_hideTags = prefs.getBoolean(INTENT_HIDE_TAGS_FILTER, false);
        mName = prefs.getString(INTENT_TITLE, "Simpletask");
        m_search = prefs.getString(SearchManager.QUERY, null);
    }

    public boolean hasFilter() {
        return m_contexts.size() + m_projects.size() + m_prios.size() > 0
                || !Strings.isEmptyOrNull(m_search);
    }

    public String getTitle (CharSequence prio, CharSequence tag, CharSequence list, CharSequence search, CharSequence filterApplied, CharSequence noFilter) {
        String filterTitle = "" + filterApplied ;
        if (hasFilter()) {
            if (m_prios.size() > 0) {
                filterTitle += " " + prio;
            }

            if (m_projects.size() > 0) {
                filterTitle += tag;
            }

            if (m_contexts.size() > 0) {
                filterTitle += list;
            }
            if (m_search != null) {
                filterTitle += search;
            }
        } else {
                filterTitle = "" + noFilter;
        }
        return filterTitle;
    }

    public String getProposedName() {
        ArrayList<String> appliedFilters = new ArrayList<String>();
        appliedFilters.addAll(m_contexts);
        appliedFilters.remove("-");
        appliedFilters.addAll(Priority.inCode(m_prios));
        appliedFilters.addAll(m_projects);
        appliedFilters.remove("-");
        if (appliedFilters.size() == 1) {
            return appliedFilters.get(0);
        } else {
            return "";
        }
    }

    public ArrayList<String> getSort(String[] defaultSort) {
        if (m_sorts == null || m_sorts.size() == 0
                || Strings.isEmptyOrNull(m_sorts.get(0))) {
            // Set a default sort
            m_sorts = new ArrayList<String>();
            if (defaultSort==null) return  m_sorts;
            for (String type : defaultSort) {
                m_sorts.add(NORMAL_SORT + SORT_SEPARATOR
                        + type);
            }

        }
        return m_sorts;
    }

    public void saveInIntent(Intent target) {
        if (target != null) {
            target.putExtra(INTENT_CONTEXTS_FILTER, Util.join(m_contexts, "\n"));
            target.putExtra(INTENT_CONTEXTS_FILTER_NOT, m_contextsNot);
            target.putExtra(INTENT_PROJECTS_FILTER, Util.join(m_projects, "\n"));
            target.putExtra(INTENT_PROJECTS_FILTER_NOT, m_projectsNot);
            target.putExtra(INTENT_PRIORITIES_FILTER, Util.join(m_prios, "\n"));
            target.putExtra(INTENT_PRIORITIES_FILTER_NOT, m_priosNot);
            target.putExtra(INTENT_SORT_ORDER, Util.join(m_sorts, "\n"));
            target.putExtra(INTENT_HIDE_COMPLETED_FILTER, m_hideCompleted);
            target.putExtra(INTENT_HIDE_FUTURE_FILTER, m_hideFuture);
            target.putExtra(INTENT_HIDE_LISTS_FILTER, m_hideLists);
            target.putExtra(INTENT_HIDE_TAGS_FILTER, m_hideTags);
            target.putExtra(SearchManager.QUERY, m_search);
        }
    }

    public void saveInPrefs(SharedPreferences prefs) {
        if (prefs!=null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(INTENT_TITLE, mName);
            editor.putString(INTENT_SORT_ORDER, Util.join(m_sorts, "\n"));
            editor.putStringSet(INTENT_CONTEXTS_FILTER, new HashSet<String>(m_contexts));
            editor.putStringSet(INTENT_PRIORITIES_FILTER,
                    new HashSet<String>(Priority.inCode(m_prios)));
            editor.putStringSet(INTENT_PROJECTS_FILTER, new HashSet<String>(m_projects));
            editor.putBoolean(INTENT_CONTEXTS_FILTER_NOT, m_contextsNot);
            editor.putBoolean(INTENT_PRIORITIES_FILTER_NOT, m_priosNot);
            editor.putBoolean(INTENT_PROJECTS_FILTER_NOT, m_projectsNot);
            editor.putBoolean(INTENT_HIDE_COMPLETED_FILTER, m_hideCompleted);
            editor.putBoolean(INTENT_HIDE_FUTURE_FILTER, m_hideFuture);
            editor.putBoolean(INTENT_HIDE_LISTS_FILTER, m_hideLists);
            editor.putBoolean(INTENT_HIDE_TAGS_FILTER, m_hideTags);
            editor.putString(SearchManager.QUERY, m_search);
            editor.commit();
        }
    }

    public void clear() {
        m_prios = new ArrayList<Priority>();
        m_contexts = new ArrayList<String>();
        m_projects = new ArrayList<String>();
        m_projectsNot = false;
        m_search = null;
        m_priosNot = false;
        m_contextsNot = false;
    }

    public void setSearch(String search) {
        this.m_search = search;
    }

    public ArrayList<String> getContexts() {
        return m_contexts;
    }

    public boolean getContextsNot() {
        return m_contextsNot;
    }

    public void setContextsNot(boolean state) {
        this.m_contextsNot = state;
    }

    public ArrayList<String> getProjects() {
        return m_projects;
    }

    public boolean getProjectsNot() {
        return m_projectsNot;
    }

    public ArrayList<Priority> getPriorities() {
        return m_prios;
    }

    public void setProjectsNot(boolean state) {
        this.m_projectsNot = state;
    }

    public void setContexts(ArrayList<String> contexts) {
        this.m_contexts = contexts;
    }

    public void setProjects(ArrayList<String> projects) {
        this.m_projects = projects;
    }

    public ArrayList<Integer> apply(ArrayList<Task> tasks) {
        AndFilter filter = new AndFilter();
        ArrayList<Integer> matched = new ArrayList<Integer>();
        for (int i= 0 ; i<tasks.size(); i++) {
            Task t = tasks.get(i);
            if (t.isCompleted() && this.getHideCompleted()) {
                continue;
            }
            if (t.inFuture() && this.getHideFuture()) {
                continue;
            }
            if (filter.apply(t)) {
                matched.add(i);
            }
        }
        return matched;
    }

    public boolean getPrioritiesNot() {
        return m_priosNot;
    }

    public void setPriorities(ArrayList<String> prios) {
        m_prios = Priority.toPriority(prios);
    }

    public void setPrioritiesNot(boolean prioritiesNot) {
        this.m_priosNot = prioritiesNot;
    }

    public void setSort(ArrayList<String> sort) {
        this.m_sorts = sort;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public boolean getHideCompleted() {
        return m_hideCompleted;
    }

    public boolean getHideFuture() {
        return m_hideFuture;
    }

    public boolean getHideLists() {
        return m_hideLists;
    }

    public boolean getHideTags() {
        return m_hideTags;
    }
    public void setHideCompleted(boolean hide) {
        this.m_hideCompleted = hide;
    }

    public void setHideFuture(boolean hide) {
        this.m_hideFuture = hide;
    }

    public void setHideLists(boolean hide) {
        this.m_hideLists = hide;
    }

    public void setHideTags(boolean hide) {
        this.m_hideTags = hide;
    }

    private class AndFilter {
        private ArrayList<TaskFilter> filters = new ArrayList<TaskFilter>();

        private AndFilter() {
            filters.clear();
            if (m_prios.size() > 0) {
                addFilter(new ByPriorityFilter(m_prios, m_priosNot));
            }
            if (m_contexts.size() > 0) {
                addFilter(new ByContextFilter(m_contexts, m_contextsNot));
            }
            if (m_projects.size() > 0) {
                addFilter(new ByProjectFilter(m_projects, m_projectsNot));
            }

            if (!Strings.isEmptyOrNull(m_search)) {
                addFilter(new ByTextFilter(m_search, false));
            }
        }

        public void addFilter(TaskFilter filter) {
            if (filter != null) {
                filters.add(filter);
            }
        }

        public boolean apply(Task input) {
            for (TaskFilter f : filters) {
                if (!f.apply(input)) {
                    return false;
                }
            }
            return true;
        }
    }
}
