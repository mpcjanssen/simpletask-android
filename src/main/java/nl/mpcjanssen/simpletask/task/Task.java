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
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.task.token.*;
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
            .compile("^[Hh]:([01])(.*)");
    private static final Pattern DUE_PATTERN = Pattern
            .compile("^[Dd][Uu][Ee]:(\\d{4}-\\d{2}-\\d{2})(.*)");
    private static final Pattern THRESHOLD_PATTERN = Pattern
            .compile("^[Tt]:(\\d{4}-\\d{2}-\\d{2})(.*)");
    private static final Pattern RECURRENCE_PATTERN = Pattern
            .compile("(^||\\s)[Rr][Ee][Cc]:(\\d{1,}[dDwWmMyY])");
    private final static Pattern PRIORITY_PATTERN = Pattern
            .compile("^(\\(([A-Z])\\) )(.*)");
    private final static Pattern SINGLE_DATE_PATTERN = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2} )(.*)");
    private final static Pattern SINGLE_DATE_PREFIX = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2} )(.*)");
    private final static Pattern COMPLETED_PATTERN = Pattern
            .compile("^([Xx] )(.*)");
    //private String text;


    private long id = 0;
    private ArrayList<Token> mTokens = new ArrayList<Token>();
    private boolean mCompleted;
    private ArrayList<String> mLists;
    private ArrayList<String> mTags;
    private String mCompletionDate;
    private String mRelativeAge;
    private String mCreateDate;
    private Priority mPrio;
    private String mThresholdate;
    private String mDuedate;
    private boolean mIsHidden;


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
        parse(rawText);
        if (defaultCreateDate != null
            && getCreateDate() == null) {
            setCreateDate(defaultCreateDate.format(Constants.DATE_FORMAT));
        }
    }

    public ArrayList<Token> getTokens() {
        return mTokens;
    }

    public DateTime getDueDate() {
        return stringToDateTime(mDuedate);
    }

    public void setDueDate(DateTime dueDate) {
        setDueDate(dueDate.format(Constants.DATE_FORMAT));
    }

    public void setDueDate(String dueDateString) {
        int currentIdx;
        int size = mTokens.size();
        // Find and remove current threshold token
        for (currentIdx=0; currentIdx<size; currentIdx++) {
            if (mTokens.get(currentIdx).type == Token.DUE_DATE) {
                mTokens.remove(currentIdx);
                if (currentIdx>0 && mTokens.get(currentIdx-1).type==Token.WHITE_SPACE) {
                    mTokens.remove(currentIdx-1);
                }
                break;
            }
        }
        if ("".equals(dueDateString)) {
            mDuedate = null;
            return;
        }
        mDuedate = dueDateString;
        Token newTok = new DUE_DATE(dueDateString);
        if (currentIdx < size-1) {
            mTokens.add(currentIdx, newTok);
        } else {
            mTokens.add(new WHITE_SPACE(" "));
            mTokens.add(newTok);
        }
    }

    public DateTime stringToDateTime(String dateString) {
        DateTime date;
        if (DateTime.isParseable(dateString)) {
            date = new DateTime(dateString);
        } else {
            date = null;
        }
        return date;
    }

    public DateTime getThresholdDate() {
        return stringToDateTime(mThresholdate);
    }

    public void setThresholdDate(DateTime thresholdDate) {
        setThresholdDate(thresholdDate.format(Constants.DATE_FORMAT));
    }

    public void setThresholdDate(String thresholdDateString) {
        int currentIdx;
        int size = mTokens.size();
        // Find and remove current threshold token
        for (currentIdx=0; currentIdx<size; currentIdx++) {
            if (mTokens.get(currentIdx).type == Token.THRESHOLD_DATE) {
                mTokens.remove(currentIdx);
                if (currentIdx>0 && mTokens.get(currentIdx-1).type==Token.WHITE_SPACE) {
                    mTokens.remove(currentIdx-1);
                }
                break;
            }
        }
        if ("".equals(thresholdDateString)) {
            mThresholdate = null;
            return;
        }
        mThresholdate = thresholdDateString;
        Token newTok = new THRESHOLD_DATE(thresholdDateString);
        if (currentIdx < size-1) {
            mTokens.add(currentIdx, newTok);
        } else {
            mTokens.add(new WHITE_SPACE(" "));
            mTokens.add(newTok);
        }
    }

    public long getId() {
        return id;
    }

    public Priority getPriority() {
        return mPrio;
    }

    public void setCreateDate(String newCreateDate) {
        ArrayList<Token> temp = new ArrayList<Token>();
        if (mTokens.size() > 0 && mTokens.get(0).type == Token.COMPLETED) {
            temp.add(mTokens.get(0));
            mTokens.remove(0);
            if (mTokens.size() > 0 && mTokens.get(0).type == Token.COMPLETED_DATE) {
                temp.add(mTokens.get(0));
                mTokens.remove(0);
            }
        }
        if (mTokens.size()>0 && mTokens.get(0).type == Token.PRIO) {
            temp.add(mTokens.get(0));
            mTokens.remove(0);
        }
        if (mTokens.size()>0 && mTokens.get(0).type == Token.CREATION_DATE) {
            mTokens.remove(0);
        }
        temp.add(new CREATION_DATE(newCreateDate));
        temp.add(new WHITE_SPACE(" "));
        temp.addAll(mTokens);
        mTokens = temp;
        mCreateDate = newCreateDate;
        mRelativeAge = calculateRelativeAge(newCreateDate);
    }

    public void setPriority(Priority priority) {
        ArrayList<Token> temp = new ArrayList<Token>();
        if (mTokens.size() > 0 && mTokens.get(0).type == Token.COMPLETED) {
            temp.add(mTokens.get(0));
            mTokens.remove(0);
            if (mTokens.size() > 0 && mTokens.get(0).type == Token.COMPLETED_DATE) {
                temp.add(mTokens.get(0));
                mTokens.remove(0);
            }
            while (mTokens.size()>0 && mTokens.get(0).type == Token.WHITE_SPACE) {
                temp.add(mTokens.get(0));
                mTokens.remove(0);
            }
        }

        if (mTokens.size()>0 && mTokens.get(0).type == Token.PRIO) {
            mTokens.remove(0);
        }
        if (!priority.equals(Priority.NONE)) {
            temp.add(new PRIO(priority.inFileFormat()+" "));
        }
        temp.addAll(mTokens);
        mTokens = temp;
        mPrio = priority;
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
        return PhoneNumberParser.getInstance().parse(inFileFormat());
    }

    public List<String> getMailAddresses() {
        return MailAddressParser.getInstance().parse(inFileFormat());
    }

    public String getRecurrencePattern() {
        Matcher matcher = RECURRENCE_PATTERN.matcher(inFileFormat());
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            return null;
        }
    }

    public List<URL> getLinks() {
        return LinkParser.getInstance().parse(inFileFormat());
    }

    public String getCompletionDate() {
        return mCompletionDate;
    }

    public boolean isCompleted() {
        return this.mCompleted;
    }

    public boolean isVisible() {
        return !mIsHidden;
    }

    public void markComplete(DateTime date) {
        if (!this.isCompleted()) {
            String completionDate = date.format(Constants.DATE_FORMAT);
            parse("x " + completionDate + " " + inFileFormat());
        }
    }

    public void markIncomplete() {
        this.mCompleted = false;
        this.mCompletionDate=null;
        if (new COMPLETED().equals(mTokens.get(0))) {
            mTokens.remove(0);
            if (mTokens.size()>0 && mTokens.get(0).type==Token.COMPLETED_DATE) {
                mTokens.remove(0);
            }
        }
    }

    public void delete() {
        this.update("");
    }

    public String showParts(int flags) {
        StringBuilder sb = new StringBuilder();
        for (Token token: mTokens) {
            if ((flags & token.type)!=0) {
                sb.append(token.value);
            }

        }
        return sb.toString();
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
       return showParts(Token.SHOW_ALL);
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
        return (this.inFileFormat().equals(other.inFileFormat()));
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result +  inFileFormat().hashCode();
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
        if (DateTime.isParseable(deferString)) {
            setThresholdDate(deferString);
            return;
        }
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
        if (DateTime.isParseable(deferString)) {
            setDueDate(deferString);
            return;
        }
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
        if (mThresholdate==null) {
            return empty;
        } else {
            return mThresholdate;
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

    public String getTextWithoutCompletionInfo() {
        int flags = Token.SHOW_ALL & ~Token.COMPLETED &~ Token.COMPLETED_DATE &~Token.CREATION_DATE;
        return showParts(flags);
    }

    private void parse(String text) {
        mTokens.clear();
        mThresholdate = null;
        mDuedate = null;
        mPrio = null;
        mCompleted = false;
        mCompletionDate = null;
        mCreateDate = null;
        mIsHidden = false;
        mLists = new ArrayList<String>();
        mTags =  new ArrayList<String>();
        mRelativeAge = null;

        Matcher m;
        String remaining = text;
        if (remaining.startsWith("x ")) {
            mTokens.add(new COMPLETED());
            mCompleted = true;
            remaining = text.substring(2);
            m = SINGLE_DATE_PATTERN.matcher(remaining);
            // read optional completion date (this 'violates' the format spec)
            // be liberal with date format errors
            if (m.matches()) {
                mCompletionDate = m.group(1).trim();
                remaining = m.group(2);
                mTokens.add(new COMPLETED_DATE(m.group(1)));
                m = SINGLE_DATE_PATTERN.matcher(remaining);
                // read optional completion date (this 'violates' the format spec)
                // be liberal with date format errors
                if (m.matches()) {
                    mCompletionDate = m.group(1);
                    remaining = m.group(2);
                    mTokens.add(new COMPLETED_DATE(m.group(1)));

                }
            }
        }

        // Check for optional priority
        m = PRIORITY_PATTERN.matcher(remaining);
        if (m.matches()) {
            mPrio = Priority.toPriority(m.group(2));
            remaining = m.group(3);
            mTokens.add(new PRIO(m.group(1)));
        } else {
            mPrio = Priority.NONE;
        }
        // Check for optional creation date
        m = SINGLE_DATE_PATTERN.matcher(remaining);
        if (m.matches()) {
            mCreateDate = m.group(1).trim();
            remaining = m.group(2);
            mTokens.add(new CREATION_DATE(m.group(1)));
            mRelativeAge = calculateRelativeAge(mCreateDate);
        }

        while (remaining.length()>0) {
            if (remaining.startsWith(" ")) {
                String leading = "";
                while (remaining.length()>0 && remaining.startsWith(" ")) {
                    leading = leading + " ";
                    remaining = remaining.substring(1);
                }
                Token ws = new WHITE_SPACE(leading);
                mTokens.add(ws);
                continue;
            }
            m = LIST_PATTERN.matcher(remaining);
            if (m.matches()) {
                String list = m.group(1);
                remaining = m.group(2);
                Token listToken = new LIST("@"+list);
                mTokens.add(listToken);
                mLists.add(list);
                continue;
            }
            m = TAG_PATTERN.matcher(remaining);
            if (m.matches()) {
                String match = m.group(1);
                remaining = m.group(2);
                Token listToken = new TTAG("+"+match);
                mTokens.add(listToken);
                mTags.add(match);
                continue;
            }
            m = THRESHOLD_PATTERN.matcher(remaining);
            if (m.matches()) {
                String match = m.group(1);
                remaining = m.group(2);
                Token tok = new THRESHOLD_DATE(match);
                mTokens.add(tok);
                mThresholdate = match;
                continue;
            }
            m = DUE_PATTERN.matcher(remaining);
            if (m.matches()) {
                String match = m.group(1);
                remaining = m.group(2);
                Token tok = new DUE_DATE(match);
                mTokens.add(tok);
                mDuedate = match;
                continue;
            }
            m = HIDDEN_PATTERN.matcher(remaining);
            if (m.matches()) {
                String match = m.group(1);
                remaining = m.group(2);
                Token tok = new HIDDEN(match);
                mTokens.add(tok);
                if (match.equals("1")) {
                    mIsHidden = true;
                } else {
                    mIsHidden = false;
                }
                continue;
            }
            if (!remaining.startsWith(" ")) {
                String leading = "";
                while (remaining.length()>0 && !remaining.startsWith(" ")) {
                    leading = leading + remaining.substring(0,1);
                    remaining = remaining.substring(1);
                }
                Token ws = new TEXT(leading);
                mTokens.add(ws);
                continue;
            }


        }
        Collections.sort(mLists);
        Collections.sort(mTags);
    }

    private String calculateRelativeAge(String date) {
        String result;
        if (!DateTime.isParseable(date)) {
            result = date;
        } else {
            DateTime dt = new DateTime(date);
            result = RelativeDate.getRelativeDate(dt);
        }
        return result;
    }

    public ArrayList<String> getLists() {
        return mLists;
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

    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (Token t : mTokens) {
            switch (t.type) {
                case Token.PRIO:
                case Token.TEXT:
                case Token.WHITE_SPACE:
                    sb.append(t.value);
                    break;
                default:
                    break;

            }
        }
        return sb.toString();
    }
}
