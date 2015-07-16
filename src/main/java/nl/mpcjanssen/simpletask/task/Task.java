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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import com.google.common.base.Strings;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.ActiveFilter;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.task.token.*;
import nl.mpcjanssen.simpletask.util.RelativeDate;
import nl.mpcjanssen.simpletask.util.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("serial")
public class Task implements Serializable {
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
            .compile("(^||\\s)[Rr][Ee][Cc]:(\\d+[dDwWmMyY])");
    private final static Pattern PRIORITY_PATTERN = Pattern
            .compile("^(\\(([A-Z])\\) )(.*)");
    private final static Pattern SINGLE_DATE_PATTERN = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2} )(.*)");
    private final static String COMPLETED_PREFIX = "x ";

    @NonNull
    private ArrayList<Token> mTokens = new ArrayList<>();
    private boolean mCompleted;
    private ArrayList<String> mLists;
    private ArrayList<String> mTags;
    @Nullable
    private String mCompletionDate;
    @Nullable
    private String mCreateDate;
    @Nullable
    private Priority mPrio;
    @Nullable
    private String mThresholdate;
    @Nullable
    private String mDuedate;
    private boolean mIsHidden;


    public Task(@NonNull String rawText, DateTime defaultPrependedDate) {
        this.init(rawText, defaultPrependedDate);
    }

    public Task(@NonNull String rawText) {
        this(rawText, null);
    }

    public void update(@NonNull String rawText) {
        this.init(rawText, null);
    }

    public void init(@NonNull String rawText, @Nullable DateTime defaultCreateDate) {
        parse(rawText);
        if (defaultCreateDate != null
            && getCreateDate() == null) {
            setCreateDate(defaultCreateDate.format(Constants.DATE_FORMAT));
        }
    }

    @NonNull
    public ArrayList<Token> getTokens() {
        return mTokens;
    }

    @Nullable
    public DateTime getDueDate() {
        return stringToDateTime(mDuedate);
    }

    public void setDueDate(@NonNull DateTime dueDate) {
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
            mTokens.add(currentIdx, new WHITE_SPACE(" "));
            mTokens.add(currentIdx, newTok);
        } else {
            mTokens.add(new WHITE_SPACE(" "));
            mTokens.add(newTok);
        }
    }

    @Nullable
    public DateTime stringToDateTime(@NonNull String dateString) {
        DateTime date;
        if (DateTime.isParseable(dateString)) {
            date = new DateTime(dateString);
        } else {
            date = null;
        }
        return date;
    }

    @Nullable
    public DateTime getThresholdDate() {
        if (mThresholdate == null) {
            return null;
        } else {
            return stringToDateTime(mThresholdate);
        }
    }

    public void setThresholdDate(@NonNull DateTime thresholdDate) {
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
            mTokens.add(currentIdx, new WHITE_SPACE(" "));
            mTokens.add(currentIdx, newTok);
        } else {
            mTokens.add(new WHITE_SPACE(" "));
            mTokens.add(newTok);
        }
    }

    @NonNull
    public Priority getPriority() {
        if (mPrio == null) {
            return Priority.NONE;
        }
        return mPrio;
    }

    public void setCreateDate(String newCreateDate) {
        ArrayList<Token> temp = new ArrayList<>();
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
    }

    public void setPriority(@NonNull Priority priority) {
        ArrayList<Token> temp = new ArrayList<>();
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


    @Nullable
    public SpannableString getRelativeDueDate(Context ctx, int dueTodayColor, int overDueColor, boolean useColor) {
        DateTime dueDate = getDueDate();
        DateTime today = DateTime.today(TimeZone.getDefault());
        if (dueDate!=null) {
            String relativeDate = RelativeDate.getRelativeDate(ctx, dueDate);
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

    @Nullable
    public String getRelativeThresholdDate(Context ctx) {
        DateTime thresholdDate = getThresholdDate();
        if (thresholdDate!=null) {
            return "T: " + RelativeDate.getRelativeDate(ctx, thresholdDate);
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

    @Nullable
    public String getRecurrencePattern() {
        Matcher matcher = RECURRENCE_PATTERN.matcher(inFileFormat());
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            return null;
        }
    }

    public List<String> getLinks() {
        return LinkParser.getInstance().parse(inFileFormat());
    }

    @Nullable
    public String getCompletionDate() {
        return mCompletionDate;
    }

    public boolean isCompleted() {
        return this.mCompleted;
    }

    public boolean isVisible() {
        return !mIsHidden;
    }

    @Nullable
    public Task markComplete(@NonNull DateTime date, boolean useOriginalDate) {
        Task newTask = null;
        if (!this.isCompleted()) {
            String completionDate = date.format(Constants.DATE_FORMAT);
            String deferFromDate = "";
            if (!useOriginalDate) {
                deferFromDate = completionDate;
            }
            parse(COMPLETED_PREFIX + completionDate + " " + inFileFormat());
            if (getRecurrencePattern() != null) {
                newTask = new Task(getTextWithoutCompletionInfo());
                if (newTask.getDueDate() == null && newTask.getThresholdDate() == null) {
                    newTask.deferDueDate(getRecurrencePattern(), deferFromDate);
                } else {
                    if (newTask.getDueDate() != null) {
                        newTask.deferDueDate(getRecurrencePattern(), deferFromDate);
                    }
                    if (newTask.getThresholdDate() != null) {
                        newTask.deferThresholdDate(getRecurrencePattern(), deferFromDate);
                    }
                }
                if (!Strings.isNullOrEmpty(getCreateDate())) {
                    newTask.setCreateDate(date.format(Constants.DATE_FORMAT));
                }
            }
        }
        return newTask;
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

    @NonNull
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
        DateTime tDate = this.getThresholdDate();
        if (tDate == null) {
            return false;
        } else {
            DateTime now = DateTime.today(TimeZone.getDefault());
            return tDate.gt(now);
        }
    }

    @NonNull
    public String inFileFormat() {
       return showParts(Token.SHOW_ALL);
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result +  inFileFormat().hashCode();
        return result;
    }

    public void initWithFilter(@NonNull ActiveFilter mFilter) {
        if (!mFilter.getContextsNot() && mFilter.getContexts().size()==1) {
            addList(mFilter.getContexts().get(0));
        }

        if (!mFilter.getProjectsNot() && mFilter.getProjects().size()==1) {
            addTag(mFilter.getProjects().get(0));
        }

    }

    /**
     * @param another Task to compare this task to
     * @return only returns true if this Task is actually the same object
     * this makes it possible to distinguish tasks with the same text representation
     */
    @Override
    public boolean equals (Object another) {
        return (this==another);
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

    public void deferThresholdDate(@NonNull String deferString, @NonNull String deferFromDate) {
        if (DateTime.isParseable(deferString)) {
            setThresholdDate(deferString);
            return;
        }
        if (deferString.equals("")) {
            setThresholdDate("");
            return;
        }

        DateTime olddate;
        if (Strings.isNullOrEmpty(deferFromDate)) {
            olddate = getThresholdDate();
        } else {
            olddate = new DateTime(deferFromDate);
        }
        DateTime newDate = Util.addInterval(olddate,deferString);
        if (newDate!=null) {
            setThresholdDate(newDate);
        }
    }

    public void deferDueDate(@NonNull String deferString, @NonNull String deferFromDate) {
        if (DateTime.isParseable(deferString)) {
            setDueDate(deferString);
            return;
        }
        if (deferString.equals("")) {
            setDueDate("");
            return;
        }
        DateTime olddate;
        if (Strings.isNullOrEmpty(deferFromDate)) {
            olddate = getDueDate();
        } else {
            olddate = new DateTime(deferFromDate);
        }

        DateTime newDate = Util.addInterval(olddate, deferString);
        if (newDate!=null) {
            setDueDate(newDate);
        }
    }


    @NonNull
    public String getThresholdDateString(String empty) {
        if (mThresholdate==null) {
            return empty;
        } else {
            return mThresholdate;
        }
    }

    @NonNull
    public String getHeader(@NonNull String sort, String empty) {
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

    @NonNull
    public String getTextWithoutCompletionInfo() {
        int flags = Token.SHOW_ALL;
        flags = flags  & ~Token.COMPLETED;
        flags =  flags & ~Token.COMPLETED_DATE; 
        flags = flags & ~Token.CREATION_DATE;
        return showParts(flags);
    }

    private void parse(@NonNull String text) {
        mTokens.clear();
        mThresholdate = null;
        mDuedate = null;
        mPrio = null;
        mCompleted = false;
        mCompletionDate = null;
        mCreateDate = null;
        mIsHidden = false;
        mLists = new ArrayList<>();
        mTags =  new ArrayList<>();

        Matcher m;
        String remaining = text;
        if (remaining.startsWith(COMPLETED_PREFIX)) {
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
                // read possible create date
                if (m.matches()) {
                    mCreateDate = m.group(1).trim();
                    remaining = m.group(2);
                    mTokens.add(new CREATION_DATE(m.group(1)));

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
                mIsHidden = match.equals("1");
                continue;
            }
            String leading = "";
            while (remaining.length() > 0 && !remaining.startsWith(" ")) {
                leading = leading + remaining.substring(0, 1);
                remaining = remaining.substring(1);
            }
            Token txt = new TEXT(leading);
            mTokens.add(txt);
        }
        Collections.sort(mLists);
        Collections.sort(mTags);
    }

    private String calculateRelativeAge(Context ctx, @NonNull String date) {
        String result;
        if (!DateTime.isParseable(date)) {
            result = date;
        } else {
            DateTime dt = new DateTime(date);
            result = RelativeDate.getRelativeDate(ctx, dt);
        }
        return result;
    }

    public ArrayList<String> getLists() {
        return mLists;
    }

    public List<String> getTags() {
        return mTags;
    }

    @Nullable
    public String getCreateDate() {
        // Handle invalid date as empty
        if (!DateTime.isParseable(mCreateDate)) {
            return null;
        }
        return mCreateDate;
    }

    @Nullable
    public String getRelativeAge(Context ctx) {
        if (mCreateDate!=null) {
            return (calculateRelativeAge(ctx, mCreateDate));
        }
        return null;
    }

    @NonNull
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
