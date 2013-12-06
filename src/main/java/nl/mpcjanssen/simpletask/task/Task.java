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
import nl.mpcjanssen.simpletask.util.Util;

import java.io.Serializable;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private static final Pattern DUE_PATTERN = Pattern
            .compile("\\s[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern THRESHOLD_PATTERN = Pattern
            .compile("\\s[Tt]:(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern RECURRENCE_PATTERN = Pattern
            .compile("\\s[Rr][Ee][Cc]:(\\d{1,}[dDwWmMyY])");
    private final static Pattern PRIORITY_PATTERN = Pattern
            .compile("^\\(([A-Z])\\) (.*)");
    private final static Pattern SINGLE_DATE_PATTERN = Pattern
            .compile("(\\s|^)(\\d{4}-\\d{2}-\\d{2})");
    private final static Pattern SINGLE_DATE_PREFIX = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2}) (.*)");
    private final static Pattern COMPLETED_PATTERN = Pattern
            .compile("^([Xx] )(.*)");
    private static final String COMPLETED = "x ";
    private static SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
    private String text;
    private long id = 0;


    public Task(long id, String rawText, Date defaultPrependedDate) {
        this.id = id;
        this.init(rawText, defaultPrependedDate);
    }

    public Task(long id, String rawText) {
        this(id, rawText, null);
    }

    public static boolean validTag(String tag) {
        return TAG_PATTERN.matcher(tag).find();
    }

    public void update(String rawText) {
        this.init(rawText, null);
    }

    public void init(String rawText, Date defaultPrependedDate) {
        this.text = rawText;
        if (defaultPrependedDate != null
                && getPrependedDate() == null) {
            this.text = formatter.format(defaultPrependedDate) + " " + text;
        }
    }

    private Date getDate(Pattern datePattern) {
        Date date = null;
        Matcher matcher = datePattern.matcher(this.text);
        if (matcher.find()) {
            try {
                date = formatter.parse(matcher.group(1));
            } catch (ParseException e) {
                date = null;
            }
        }
        return date;
    }

    public Date getDueDate() {
        return getDate(DUE_PATTERN);
    }

    public void setDueDate(Date dueDate) {
        setDueDate(formatter.format(dueDate));
    }

    public void setDueDate(String dueDateString) {
        if (dueDateString.equals("")) {
            text = text.replaceAll(DUE_PATTERN.pattern(),"");
        } else if (this.getDueDate()!=null) {
            text = text.replaceFirst(DUE_PATTERN.pattern(), " due:" + dueDateString);
        } else {
            text = text + " due:" + dueDateString;
        }
    }

    public Date getThresholdDate() {
        return getDate(THRESHOLD_PATTERN);
    }

    public void setThresholdDate(Date thresholdDate) {
        setThresholdDate(formatter.format(thresholdDate));
    }

    public void setThresholdDate(String thresholdDateString) {
        //TODO implement
    }

    public String getText() {
        return text;
    }

    public long getId() {
        return id;
    }

    public Priority getPriority() {
        Matcher m = PRIORITY_PATTERN.matcher(getTextWithoutCompletionInfo());
        if (m.matches()) {
            return Priority.toPriority(m.group(1));
        } else {
            return Priority.NONE;
        }
    }

    public void setPriority(Priority priority) {
        //TODO implement
    }

    public List<String> getLists() {
        return ListParser.getInstance().parse(text);
    }

    public List<String> getTags() {
        return TagParser.getInstance().parse(text);
    }

    public void setPrependedDate(String date) {
        //TODO implement
    }

    public String getPrependedDate() {
        //TODO implement
        return null;
    }

    public void setPrependedDate(Date dt) {
        //TODO implement

    }

    public String getRelativeAge() {
        //TODO implement
        return null;
    }

    public SpannableString getRelativeDueDate(Resources res, boolean useColor) {
        Date dueDate = getDueDate();
        if (dueDate!=null) {
            String relativeDate = RelativeDate.getRelativeDate(dueDate);
            SpannableString ss = new SpannableString("Due: " +  relativeDate);
            if (relativeDate.equals(res.getString(R.string.dates_today)) && useColor) {
                Util.setColor(ss, res.getColor(android.R.color.holo_green_light));
            } else if (dueDate.before(new Date()) && useColor) {
                Util.setColor(ss,res.getColor(android.R.color.holo_red_light));
            }
            return ss;
        } else {
            return null;
        }
    }

    public String getRelativeThresholdDate() {
        Date thresholdDate = getThresholdDate();
        if (thresholdDate!=null) {
            return "T: " + RelativeDate.getRelativeDate(thresholdDate);
        } else {
            return null;
        }
    }

    public List<String> getPhoneNumbers() {
        return PhoneNumberParser.getInstance().parse(text);
    }

    public List<String> getMailAddresses() {
        return MailAddressParser.getInstance().parse(text);
    }

    public String getRecurrencePattern() {
        //TODO implement
        return null;
    }

    public List<URL> getLinks() {
        return LinkParser.getInstance().parse(text);
    }

    public String getTextWithoutCompletionInfo() {
        Matcher xMatch = COMPLETED_PATTERN.matcher(text);
        if (!xMatch.matches()) {
            return text;
        }
        String restText = xMatch.group(2);
        Matcher dateMatch = SINGLE_DATE_PREFIX.matcher(restText);
        if (!dateMatch.matches()) {
            return restText;
        } else {
            return dateMatch.group(2);
        }

    }

    public String getCompletionDate() {
        Matcher xMatch = COMPLETED_PATTERN.matcher(text);
        if (!xMatch.matches()) {
            return null;
        }
        String restText = xMatch.group(2);
        Matcher dateMatch = SINGLE_DATE_PREFIX.matcher(restText);
        if (!dateMatch.matches()) {
            return "";
        } else {
            return dateMatch.group(1);
        }
    }

    public boolean isCompleted() {
        return getCompletionDate() != null;
    }

    public void markComplete(Date date) {
        if (!this.isCompleted()) {
            String completionDate = formatter.format(date);
            this.text = "x " + completionDate + " " + text;
        }
    }

    public void markIncomplete() {
        this.text = getTextWithoutCompletionInfo();
    }

    public void delete() {
        this.update("");
    }

    public String inScreenFormat() {
        return text;
    }

    public boolean inFuture() {
        if (this.getThresholdDate() == null) {
            return false;
        } else {
            Date thresholdDate = this.getThresholdDate();
            Date now = new Date();
            return thresholdDate.after(now);
        }
    }

    public String inFileFormat() {
        return text;
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
        // TODO implement
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
        this.init(inFileFormat() + " " + string, null);
    }

    public void removeTag(String tag) {
        String escapedTag = Pattern.quote(tag);
        String regexp = "\\s" + escapedTag + "(?:\\s|$)";
        String newText = inFileFormat().replaceAll(regexp, " ");
        newText = newText.trim();
        this.init(newText, null);
    }

    /* Adds the task to list Listname
    ** If the task is already on that list, it does nothing
     */
    public void addList(String listName) {
        if (!getLists().contains(listName)) {
            append("@" + listName);
        }
    }

    /* Tags the task with tag
    ** If the task already has te tag, it does nothing
    */
    public void addTag(String tag) {
        if (!getTags().contains(tag)) {
            append("+" + tag);
        }
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
        Matcher matcher = THRESHOLD_PATTERN.matcher(this.text);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return empty;
        }
    }

    public String getHeader(String sort, String empty) {
        if (sort.contains("by_context")) {
            if (getLists().size() > 0) {
                return getLists().get(0);
            } else {
                return empty;
            }
        } else if (sort.contains("by_project")) {
            if (getTags().size() > 0) {
                return getTags().get(0);
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
        // remove completion and creation dates
        text = text.replaceAll(SINGLE_DATE_PATTERN.pattern(),"");
        // remove due dates
        text = text.replaceAll(DUE_PATTERN.pattern(), "");
        // remove threshold dates
        text = text.replaceAll(THRESHOLD_PATTERN.pattern(), "");
        return text.trim();
    }
}
