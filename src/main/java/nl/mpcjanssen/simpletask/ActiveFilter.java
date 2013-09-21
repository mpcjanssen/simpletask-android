package nl.mpcjanssen.simpletask;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

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

    public final static String INTENT_EXTRA_DELIMITERS = "\n|,";

    private  Resources mResources;

    private ArrayList<Priority> m_prios = new ArrayList<Priority>();
    private ArrayList<String> m_contexts = new ArrayList<String>();
    private ArrayList<String> m_projects = new ArrayList<String>();
    private ArrayList<String> m_sorts = new ArrayList<String>();
    private boolean m_projectsNot = false;
    private String m_search;
    private boolean m_priosNot;
    private boolean m_contextsNot;
    private boolean m_hideCompleted;
    private boolean m_hideFuture;

    private String mName;

    public ActiveFilter(Resources resources) {
        this.mResources = resources;
    }


    public void initFromBundle(Bundle bundle) {
        m_prios = Priority.toPriority(bundle.getStringArrayList(INTENT_PRIORITIES_FILTER));
        m_contexts = bundle.getStringArrayList(INTENT_CONTEXTS_FILTER);
        m_projects = bundle.getStringArrayList(INTENT_PROJECTS_FILTER);
        m_search = bundle.getString(SearchManager.QUERY);
        m_contextsNot = bundle.getBoolean(INTENT_CONTEXTS_FILTER_NOT);
        m_priosNot = bundle.getBoolean(INTENT_PRIORITIES_FILTER_NOT);
        m_projectsNot = bundle.getBoolean(INTENT_PROJECTS_FILTER_NOT);
        m_sorts = bundle.getStringArrayList(INTENT_SORT_ORDER);
        m_hideCompleted = bundle.getBoolean(INTENT_HIDE_COMPLETED_FILTER);
        m_hideFuture = bundle.getBoolean(INTENT_HIDE_FUTURE_FILTER);
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
        mName = prefs.getString(INTENT_TITLE, "Simpletask");
    }

    public boolean hasFilter() {
        return m_contexts.size() + m_projects.size() + m_prios.size() > 0
                || !Strings.isEmptyOrNull(m_search);
    }

    public String getTitle () {
        String filterTitle = mResources.getString(R.string.title_filter_applied);
        if (hasFilter()) {
            if (m_prios.size() > 0) {
                filterTitle += " " + mResources.getString(R.string.priority_prompt);
            }

            if (m_projects.size() > 0) {
                filterTitle += " " + mResources.getString(R.string.project_prompt);
            }

            if (m_contexts.size() > 0) {
                filterTitle += " " + mResources.getString(R.string.context_prompt);
            }
            if (m_search != null) {
                filterTitle += " " + mResources.getString(R.string.search);
            }
        } else {
                filterTitle = mResources.getString(R.string.no_filter);
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

    public ArrayList<String> getSort() {
        if (m_sorts == null || m_sorts.size() == 0
                || Strings.isEmptyOrNull(m_sorts.get(0))) {
            // Set a default sort
            m_sorts = new ArrayList<String>();
            for (String type : mResources.getStringArray(R.array.sortKeys)) {
                m_sorts.add(NORMAL_SORT + SORT_SEPARATOR
                        + type);
            }

        }
        return m_sorts;
    }

    public void saveInBundle(Bundle bundle) {
        bundle.putStringArrayList(INTENT_PRIORITIES_FILTER, Priority.inCode(m_prios));
        bundle.putStringArrayList(INTENT_CONTEXTS_FILTER, m_contexts);
        bundle.putStringArrayList(INTENT_PROJECTS_FILTER, m_projects);
        bundle.putStringArrayList(INTENT_SORT_ORDER, m_sorts);
        bundle.putBoolean(INTENT_PRIORITIES_FILTER_NOT, m_priosNot);
        bundle.putBoolean(INTENT_PROJECTS_FILTER_NOT, m_projectsNot);
        bundle.putBoolean(INTENT_CONTEXTS_FILTER_NOT, m_contextsNot);
        bundle.putBoolean(INTENT_HIDE_COMPLETED_FILTER, m_hideCompleted);
        bundle.putBoolean(INTENT_HIDE_FUTURE_FILTER, m_hideFuture);
        bundle.putString(SearchManager.QUERY, m_search);
    }

    public void saveInPrefs(SharedPreferences prefs) {
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
        editor.putBoolean(INTENT_HIDE_COMPLETED_FILTER,m_hideCompleted);
        editor.putBoolean(INTENT_HIDE_FUTURE_FILTER,m_hideFuture);
        editor.commit();
    }

    public void saveInIntent(Intent target) {
        target.putExtra(INTENT_CONTEXTS_FILTER, Util.join(m_contexts, "\n"));
        target.putExtra(INTENT_CONTEXTS_FILTER_NOT, m_contextsNot);
        target.putExtra(INTENT_PROJECTS_FILTER, Util.join(m_projects, "\n"));
        target.putExtra(INTENT_PROJECTS_FILTER_NOT, m_projectsNot);
        target.putExtra(INTENT_PRIORITIES_FILTER, Util.join(m_prios, "\n"));
        target.putExtra(INTENT_PRIORITIES_FILTER_NOT, m_priosNot);
        target.putExtra(INTENT_SORT_ORDER, Util.join(m_sorts, "\n"));
        target.putExtra(INTENT_HIDE_COMPLETED_FILTER,m_hideCompleted);
        target.putExtra(INTENT_HIDE_FUTURE_FILTER,m_hideFuture);
        target.putExtra(SearchManager.QUERY, m_search);
    }


    public void clear() {
        m_prios = new ArrayList<Priority>();
        m_contexts = new ArrayList<String>();
        m_projects = new ArrayList<String>();
        m_projectsNot = false;
        m_hideFuture=false;
        m_hideCompleted=false;
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

    public ArrayList<Task> apply(ArrayList<Task> tasks) {
        AndFilter filter = new AndFilter();
        ArrayList<Task> matched = new ArrayList<Task>();
        for (Task t : tasks) {
            if (t.isCompleted() && this.getHideCompleted()) {
                continue;
            }
            if (t.inFuture() && this.getHideFuture()) {
                continue;
            }
            if (filter.apply(t)) {
                matched.add(t);
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

    public void setHideCompleted(boolean hide) {
        this.m_hideCompleted = hide;
    }

    public void setHideFuture(boolean hide) {
        this.m_hideFuture = hide;
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
