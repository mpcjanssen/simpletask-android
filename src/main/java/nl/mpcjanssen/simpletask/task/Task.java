/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task;

import android.content.res.Resources;
import android.text.SpannableString;

import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.util.RelativeDate;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;

import java.io.Serializable;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("serial")
public class Task implements Serializable, Comparable<Task> {

    public final static int DUE_DATE = 0;
    public final static int THRESHOLD_DATE = 1;

    private static final long serialVersionUID = 0L;

    private static final Pattern TAG_PATTERN = Pattern
            .compile("^\\S*[\\p{javaLetterOrDigit}_]$");
    private static final Pattern DUE_PATTERN =  Pattern
            .compile("\\s[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern THRESHOLD_PATTERN =  Pattern
            .compile("\\s[Tt]:(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern RECURRENCE_PATTERN =  Pattern
            .compile("\\s[Rr][Ee][Cc]:(\\d{1,}[dDwWmMyY])");

    private static SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);

    private static final String COMPLETED = "x ";

    private String text;
    private long id = 0;


    public static boolean validTag(String tag) {
        return TAG_PATTERN.matcher(tag).find();
    }

    public Task(long id, String rawText, Date defaultPrependedDate) {
        this.id = id;
        this.init(rawText, defaultPrependedDate);
    }

    public Task(long id, String rawText) {
        this(id, rawText, null);
    }

    public void update(String rawText) {
        this.init(rawText, null);
    }

    public void init(String rawText, Date defaultPrependedDate) {
        this.text = rawText;
        if (defaultPrependedDate != null
                && getPrependedDate()==null) {
            this.text = formatter.format(defaultPrependedDate) + " " + text;
        }
    }

    public Date getDueDate() {
        Date dueDate = null;
        Matcher matcher = DUE_PATTERN.matcher(this.text);
        if (matcher.find()) {
            try {
                dueDate = formatter.parse(matcher.group(1));
            } catch (ParseException e) {
                dueDate = null;
            }
        }
        return dueDate;
    }

    public Date getThresholdDate() {
        return this.thresholdDate;
    }

    public String getText() {
        return text;
    }

    public long getId() {
        return id;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Priority getPriority() {
        return priority;
    }

    public List<String> getContexts() {
        return contexts;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void setPrependedDate(String date) {
        this.prependedDate = date;
        try {
            Date d = formatter.parse(this.prependedDate);
            this.relativeAge = RelativeDate.getRelativeDate(d);
        } catch (ParseException e) {
            // e.printStackTrace();
        }
    }

    public void setPrependedDate(Date dt) {
        this.prependedDate = formatter.format(dt);
        this.relativeAge = RelativeDate.getRelativeDate(dt);

    }

    public String getPrependedDate() {
        return prependedDate;
    }

    public String getRelativeAge() {
        return relativeAge;
    }

    public SpannableString getRelativeDueDate(Resources res, boolean useColor) {

        if (dueDate!=null) {
            String relativeDate = RelativeDate.getRelativeDate(dueDate);
            SpannableString ss = new SpannableString("Due: " +  relativeDate);
            if (relativeDate.equals(res.getString(R.string.dates_today)) && useColor) {
                Util.setColor(ss,res.getColor(android.R.color.holo_green_light));
            } else if (dueDate.before(new Date()) && useColor) {
                Util.setColor(ss,res.getColor(android.R.color.holo_red_light));
            }
            return ss;
        } else {
            return null;
        }
    }
    public String getRelativeThresholdDate() {
        if (thresholdDate!=null) {
            return "T: " + RelativeDate.getRelativeDate(thresholdDate);
        } else {
            return null;
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isCompleted() {
        return completed;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public List<String> getMailAddresses() {
        return mailAddresses;
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public List<URL> getLinks() {
        return links;
    }

    public String getCompletionDate() {
        return completionDate;
    }

    public void markComplete(Date date) {
        if (!this.completed) {
            this.completionDate = formatter.format(date);
            this.deleted = false;
            this.completed = true;
        }
    }

    public void markIncomplete() {
        if (this.completed) {
            this.completionDate = "";
            this.completed = false;
        }
    }

    public void delete() {
        this.update("");
    }

    // TODO need a better solution (TaskFormatter?) here
    public String inScreenFormat() {
        StringBuilder sb = new StringBuilder();
        if (this.completed) {
            sb.append(COMPLETED).append(this.completionDate).append(" ");
        }
        if (this.getPriority()!=Priority.NONE) {
            sb.append(this.getPriority().inFileFormat()).append(" ");
        }
        sb.append(this.text.trim());
        return sb.toString();
    }

    public boolean inFuture() {
        if (this.getThresholdDate()==null) {
            return false;
        } else {
            Date thresholdDate = this.getThresholdDate();
            Date now = new Date();
            return thresholdDate.after(now);
        }
    }

    public String inFileFormat() {
        StringBuilder sb = new StringBuilder();
        if (this.completed) {
            sb.append(COMPLETED);
            if(!Strings.isEmptyOrNull(this.completionDate)){
                sb.append(this.completionDate).append(" ");
            }

        }

        if (priority != Priority.NONE) {
            sb.append(priority.inFileFormat()).append(" ");
        }
        if (!Strings.isEmptyOrNull(this.prependedDate)) {
            sb.append(this.prependedDate).append(" ");
            }

        sb.append(this.text);
        return sb.toString();
    }

    public void copyInto(Task destination) {
        destination.id = this.id;
        destination.init(this.inFileFormat(), null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Task other = (Task) obj;
        if (id != other.id)
            return false;
        return (this.text.equals(other.text));
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    public void initWithFilter(ActiveFilter mFilter) {

        // Ignore empty contexts and projects for initializing the task
        ArrayList<String> filterContexts = mFilter.getContexts();
        filterContexts.remove("-");
        if ((filterContexts.size() == 1) && (mFilter.getContextsNot()!=true)) {
            contexts.clear();
            contexts.add(filterContexts.get(0));
        }
        ArrayList<String> filterProjects = mFilter.getProjects();
        filterProjects.remove("-");
        if ((filterProjects.size() == 1) && (mFilter.getProjectsNot()!=true)) {
            projects.clear();
            projects.add(filterProjects.get(0));
        }
    }

    /**
     * @param another Task to compare this task to
     * @return comparison of the position of the tasks in the file
     */
    @Override
    public int compareTo(Task another) {
        return ((Long) this.getId()).compareTo(another.getId());
    }

	public void append(String string) {
		this.init(inFileFormat() + " " + string , null);
	}

    public void removeTag(String tag) {
        String escapedTag = Pattern.quote(tag);
        String regexp = "\\s" + escapedTag +"(?:\\s|$)";
        String newText = inFileFormat().replaceAll(regexp, " ");
        newText = newText.trim();
        this.init(newText , null);
    }

    /* Adds the task to list Listname
    ** If the task is already on that list, it does nothing
     */
    public void addList(String listName) {
        if (!getContexts().contains(listName)) {
            append ("@" + listName);
        }
    }

    /* Tags the task with tag
    ** If the task already has te tag, it does nothing
    */
    public void addTag(String tag) {
        if (!getProjects().contains(tag)) {
            append ("+" + tag);
        }
    }


    public void setDueDate(Date dueDate) {
        setDueDate(formatter.format(dueDate));
    }

    public void setDueDate(String dueDateString) {
        String taskContents = inFileFormat();
        if (dueDateString.equals("")) {
            taskContents =

                    taskContents.replaceAll(DUE_PATTERN.pattern(),"");
        } else if (this.dueDate!=null) {
            taskContents = taskContents.replaceFirst(DUE_PATTERN.pattern(), " due:" + dueDateString);
        } else {
            taskContents = taskContents + " due:" + dueDateString;
        }
        init(taskContents, null);
    }

    public void setThresholdDate(Date thresholdDate) {
        setThresholdDate(formatter.format(thresholdDate));
    }

    public void setThresholdDate(String thresholdDateString) {
        String taskContents = inFileFormat();
        if (thresholdDateString.equals("")) {
            taskContents = taskContents.replaceAll(THRESHOLD_PATTERN.pattern(),"");
        } else if (this.thresholdDate!=null) {
            taskContents = taskContents.replaceFirst(THRESHOLD_PATTERN.pattern(), " t:" + thresholdDateString);
        } else {
            taskContents = taskContents + " t:" + thresholdDateString;
        }
        init(taskContents, null);
    }

    public void deferThresholdDate(String deferString, boolean original) {
        if (deferString.equals("")) {
            setThresholdDate("");
            return;
        }
        Date olddate = new Date();
        if (original) {
            olddate = getThresholdDate();
        }
        Date newDate = Util.addInterval(olddate,deferString);
        if (newDate!=null) {
            setThresholdDate(newDate);
        }
    }

    public void deferDueDate(String deferString, boolean original) {
        if (deferString.equals("")) {
            setDueDate("");
            return;
        }
        Date olddate = new Date();
        if (original) {
            olddate = getDueDate();
        }
        Date newDate = Util.addInterval(olddate, deferString);
        if (newDate!=null) {
            setDueDate(newDate);
        }
    }

    public String getThresholdDateString(String empty) {
        if (this.thresholdDate==null) {
            return empty;
        } else {
            return formatter.format(thresholdDate);
        }
    }

    public String getHeader(String sort, String empty) {
        if (sort.contains("by_context")) {
            if (getContexts().size()>0) {
                return getContexts().get(0);
            } else {
                return empty;
            }
        } else if (sort.contains("by_project")) {
            if (getProjects().size()>0) {
                return getProjects().get(0);
            } else {
                return empty;
            }
        } else if (sort.contains("by_threshold_date")) {
            return getThresholdDateString(empty);
        } else if (sort.contains("by_prio")) {
            return getPriority().getCode();
        }
        return "";
    }

    public CharSequence datelessScreenFormat() {
        String text = inScreenFormat();
        // remove due dates
        text = text.replaceAll(DUE_PATTERN.pattern(),"");
        // remove threshold dates
        text = text.replaceAll(THRESHOLD_PATTERN.pattern(),"");
        return text;
    }
}
