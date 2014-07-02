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

import android.text.SpannableString;
import android.util.Log;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.util.RelativeDate;
import nl.mpcjanssen.simpletask.util.Util;


@SuppressWarnings("serial")
public class Task implements Serializable, Comparable<Task> {
    public static String TAG = Task.class.getName();
    public final static int DUE_DATE = 0;
    public final static int THRESHOLD_DATE = 1;
    private static final long serialVersionUID = 1L;
    private final static Pattern LIST_PATTERN = Pattern
            .compile("^@(\\S*\\w)(.*)");
    private final static Pattern TAG_PATTERN = Pattern
            .compile("^\\+(\\S*\\w)(.*)");
    private static final Pattern HIDDEN_PATTERN = Pattern
            .compile("(^|\\s)[Hh]:1");
    private static final Pattern DUE_PATTERN = Pattern
            .compile("(^||\\s)[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern THRESHOLD_PATTERN = Pattern
            .compile("(^||\\s)[Tt]:(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern RECURRENCE_PATTERN = Pattern
            .compile("(^||\\s)[Rr][Ee][Cc]:(\\d{1,}[dDwWmMyY])");
    private final static Pattern PRIORITY_PATTERN = Pattern
            .compile("^\\(([A-Z])\\) (.*)");
    private final static Pattern SINGLE_DATE_PATTERN = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2})(.*)");
    private final static Pattern SINGLE_DATE_PREFIX = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2}) (.*)");
    private final static Pattern COMPLETED_PATTERN = Pattern
            .compile("^([Xx] )(.*)");
    private String text;


    private long id = 0;
    private ArrayList<Token> mTokens = new ArrayList<Token>();
    private boolean mCompleted;
    private ArrayList<String> mLists;
    private ArrayList<String> mTags;
    private String mCompletionDate;
    private String mRelativeAge;
    private String mCreateDate;
    private Priority mPrio;


    public Task(long id, String rawText, DateTime defaultPrependedDate) {
        this.id = id;
        this.init(rawText, defaultPrependedDate);
    }

    public Task(long id, String rawText) {
        this(id, rawText, null);
    }

    public void update(String rawText) {
        this.init(rawText, null);
    }

    public void init(String rawText, DateTime defaultCreateDate) {
        this.text = rawText;
        parse(rawText);
        if (defaultCreateDate != null
            && getCreateDate() == null) {
            // fixme
        }
    }

    public ArrayList<Token> getTokens() {
        return mTokens;
    }

    private DateTime getDate(Pattern datePattern) {
        DateTime date = null;
        Matcher matcher = datePattern.matcher(this.text);
        if (matcher.find()) {
            String dateString = matcher.group(2);
            if (DateTime.isParseable(dateString)) {
                date = new DateTime(dateString);
            } else {
                date = null;
            }
        }
        return date;
    }

    public DateTime getDueDate() {
        return getDate(DUE_PATTERN);
    }

    public void setDueDate(DateTime dueDate) {
        setDueDate(dueDate.format(Constants.DATE_FORMAT));
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

    public DateTime getThresholdDate() {
        return getDate(THRESHOLD_PATTERN);
    }

    public void setThresholdDate(DateTime thresholdDate) {
        setThresholdDate(thresholdDate.format(Constants.DATE_FORMAT));
    }

    public void setThresholdDate(String thresholdDateString) {
        if (thresholdDateString.equals("")) {
            text = text.replaceAll(THRESHOLD_PATTERN.pattern(),"");
        } else if (this.getThresholdDate()!=null) {
            text = text.replaceFirst(THRESHOLD_PATTERN.pattern(), " t:" + thresholdDateString);
        } else {
            text = text + " t:" + thresholdDateString;
        }
    }

    public String getText() {
        return text;
    }

    public long getId() {
        return id;
    }

    public Priority getPriority() {
        return mPrio;
    }

    public void setPriority(Priority priority) {
        ArrayList<Token> temp = new ArrayList<Token>();
        if (mTokens.size()>0 && mTokens.get(0).type == Token.COMPLETED) {
            temp.add(mTokens.get(0));
            mTokens.remove(0);
        }
        if (mTokens.size()>0 && mTokens.get(0).type == Token.COMPLETED_DATE) {
            temp.add(mTokens.get(0));
            mTokens.remove(0);
        }
        if (mTokens.size()>0 && mTokens.get(0).type == Token.PRIO) {
            mTokens.remove(0);
        }
        if (!priority.equals(Priority.NONE)) {
            temp.add(new Token(Token.PRIO, priority.inFileFormat()+" "));
        }
        temp.addAll(mTokens);
        mTokens = temp;
        mPrio = priority;
    }

    public List<String> getTags() {
        return mTags;
    }

    public String getCreateDate() {
        return mCreateDate;
    }

    public String getRelativeAge() {
        return mRelativeAge;
    }

    public SpannableString getRelativeDueDate(int dueTodayColor, int overDueColor, boolean useColor) {
        DateTime dueDate = getDueDate();
        DateTime today = DateTime.today(TimeZone.getDefault());
        if (dueDate!=null) {
            String relativeDate = RelativeDate.getRelativeDate(dueDate);
            SpannableString ss = new SpannableString("Due: " +  relativeDate);
            if (dueDate.isSameDayAs(today) && useColor) {
                Util.setColor(ss, dueTodayColor);
            } else if (dueDate.isInThePast(TimeZone.getDefault()) && useColor) {
                Util.setColor(ss, overDueColor);
            }
            return ss;
        } else {
            return null;
        }
    }

    public String getRelativeThresholdDate() {
        DateTime thresholdDate = getThresholdDate();
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
        Matcher matcher = RECURRENCE_PATTERN.matcher(this.text);
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            return null;
        }
    }

    public List<URL> getLinks() {
        return LinkParser.getInstance().parse(text);
    }

    private String getTextWithoutCompletionInfo() {
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

    private String getTextWithoutCompletionAndPriority() {
        String rest = getTextWithoutCompletionInfo() ;
        Matcher prioMatch = PRIORITY_PATTERN.matcher(rest);
        if (!prioMatch.matches()) {
            return rest;
        } else {
            return prioMatch.group(2);
        }
    }

    private String getCompletionPrefix() {
        Matcher xMatch = COMPLETED_PATTERN.matcher(text);
        String result = "";
        if (!xMatch.matches()) {
            return result;
        } else {
            result = xMatch.group(1);
        }
        String restText = xMatch.group(2);
        Matcher dateMatch = SINGLE_DATE_PREFIX.matcher(restText);
        if (!dateMatch.matches()) {
            return result;
        } else {
            return result + dateMatch.group(1) + " ";
        }

    }

    public String getCompletionDate() {
        return mCompletionDate;
    }

    public boolean isCompleted() {
        return this.mCompleted;
    }

    public boolean isVisible() {
        Matcher hiddenMatch = HIDDEN_PATTERN.matcher(text);
        return !hiddenMatch.find();
    }

    public void markComplete(DateTime date) {
        if (!this.isCompleted()) {
            String completionDate = date.format(Constants.DATE_FORMAT);
            this.text = "x " + completionDate + " " + inFileFormat();
            parse(this.text);
        }
    }

    public void markIncomplete() {
        this.text = getTextWithoutCompletionInfo();
        parse(this.text);
    }

    public void delete() {
        this.update("");
    }

    public String inScreenFormat(int flags) {
        StringBuilder sb = new StringBuilder();
        for (Token token: mTokens) {
            if ((flags & token.type)!=0) {
                sb.append(token.value);
            }

        }
        return sb.toString().trim();
    }

    public boolean inFuture() {
        if (this.getThresholdDate() == null) {
            return false;
        } else {
            DateTime thresholdDate = this.getThresholdDate();
            DateTime now = DateTime.today(TimeZone.getDefault());
            return thresholdDate.gt(now);
        }
    }

    public String inFileFormat() {
        StringBuilder sb = new StringBuilder();
        for (Token t: mTokens) {
            sb.append(t.value);
        }
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
        if (((Object) this).getClass() != obj.getClass())
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
        if (!mFilter.getContextsNot() && mFilter.getContexts().size()==1) {
            addList(mFilter.getContexts().get(0));
        }

        if (!mFilter.getProjectsNot() && mFilter.getProjects().size()==1) {
            addTag(mFilter.getProjects().get(0));
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
        if (!mLists.contains(listName)) {
            append("@" + listName);
        }
    }

    /* Tags the task with tag
    ** If the task already has te tag, it does nothing
    */
    public void addTag(String tag) {
        if (!mTags.contains(tag)) {
            append("+" + tag);
        }
    }

    public void deferThresholdDate(String deferString, boolean original) {
        if (deferString.equals("")) {
            setThresholdDate("");
            return;
        }
        DateTime olddate = DateTime.today(TimeZone.getDefault());
        if (original) {
            olddate = getThresholdDate();
        }
        DateTime newDate = Util.addInterval(olddate,deferString);
        if (newDate!=null) {
            setThresholdDate(newDate);
        }
    }

    public void deferDueDate(String deferString, boolean original) {
        if (deferString.equals("")) {
            setDueDate("");
            return;
        }
        DateTime olddate = DateTime.today(TimeZone.getDefault());
        if (original) {
            olddate = getDueDate();
        }
        DateTime newDate = Util.addInterval(olddate, deferString);
        if (newDate!=null) {
            setDueDate(newDate);
        }
    }


    public String getThresholdDateString(String empty) {
        Matcher matcher = THRESHOLD_PATTERN.matcher(this.text);
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            return empty;
        }
    }

    public String getHeader(String sort, String empty) {
        if (sort.contains("by_context")) {
            if (mLists.size() > 0) {
                return mLists.get(0);
            } else {
                return empty;
            }
        } else if (sort.contains("by_project")) {
            if (mTags.size() > 0) {
                return mTags.get(0);
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

    public String withoutCreateAndCompletionDate() {
        // remove completion and creation dates
        String stext = getTextWithoutCompletionAndPriority();

        // remove possible create date
        Matcher m = SINGLE_DATE_PREFIX.matcher(stext);
        if (m.matches()) {
            stext = m.group(2);
        }
        // Re add priority
        if (getPriority()!=Priority.NONE) {
            stext = getPriority().inFileFormat() + " " + stext.trim();
        }
        return stext;
    }

    private void parse(String text) {
        String remaining = text;
        mTokens.clear();
        Matcher m;
        mLists = new ArrayList<String>();
        mTags =  new ArrayList<String>();
        mCompleted = false;
        if (remaining.startsWith("x ")) {
            mTokens.add(new Token(Token.COMPLETED, "x "));
            mCompleted = true;
            remaining = text.substring(2);
            m = SINGLE_DATE_PATTERN.matcher(remaining);
            // read optional completion date (this 'violates' the format spec)
            // be liberal with date format errors
            if (m.matches()) {
                mCompletionDate = m.group(1);
                remaining = m.group(2);
                mTokens.add(new Token(Token.COMPLETED_DATE,mCompletionDate));
            }
        }

        // Check for optional priority
        m = PRIORITY_PATTERN.matcher(remaining);
        if (m.matches()) {
            mPrio = Priority.toPriority(m.group(1));
            remaining = m.group(2);
            mTokens.add(new Token(Token.PRIO,mPrio.inFileFormat()+" "));
        } else {
            mPrio = Priority.NONE;
        }
        // Check for optional creation date
        m = SINGLE_DATE_PATTERN.matcher(remaining);
        if (m.matches()) {
            mCreateDate = m.group(1);
            remaining = m.group(2);
            mTokens.add(new Token(Token.CREATION_DATE,mCreateDate));
            if (!DateTime.isParseable(mCreateDate)) {
                mRelativeAge = mCreateDate;
            } else {
                DateTime dt = new DateTime(mCreateDate);
                mRelativeAge = RelativeDate.getRelativeDate(dt);
            }
        }

        while (remaining.length()>0) {
            if (remaining.startsWith(" ")) {
                String leading = "";
                while (remaining.length()>0 && remaining.startsWith(" ")) {
                    leading = leading + " ";
                    remaining = remaining.substring(1);
                }
                Token ws = new Token(Token.WHITE_SPACE,leading);
                mTokens.add(ws);
                continue;
            }
            m = LIST_PATTERN.matcher(remaining);
            if (m.matches()) {
                String list = m.group(1);
                remaining = m.group(2);
                Token listToken = new Token(Token.LIST,"@"+list);
                mTokens.add(listToken);
                mLists.add(list);
                continue;
            }
            m = TAG_PATTERN.matcher(remaining);
            if (m.matches()) {
                String match = m.group(1);
                remaining = m.group(2);
                Token listToken = new Token(Token.TAG,"+"+match);
                mTokens.add(listToken);
                mTags.add(match);
                continue;
            }

            if (!remaining.startsWith(" ")) {
                String leading = "";
                while (remaining.length()>0 && !remaining.startsWith(" ")) {
                    leading = leading + remaining.substring(0,1);
                    remaining = remaining.substring(1);
                }
                Token ws = new Token(Token.TEXT,leading);
                mTokens.add(ws);
                continue;
            }


        }
        Collections.sort(mLists);
        Collections.sort(mTags);
    }

    public ArrayList<String> getLists() {
        return mLists;
    }
}
