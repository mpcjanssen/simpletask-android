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

import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.util.RelativeDate;
import nl.mpcjanssen.simpletask.util.Strings;

import java.io.Serializable;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


@SuppressWarnings("serial")
public class Task implements Serializable, Comparable<Task> {
    private static final String COMPLETED = "x ";
    private String originalText;
    private final Priority originalPriority;

    private long id;
    private Priority priority;
    private boolean deleted = false;
    private boolean completed = false;
    private String text;
    private String completionDate;
    private String prependedDate;
    private String relativeAge = "";
    private List<String> contexts;
    private List<String> projects;
    private List<String> phoneNumbers;
    private List<String> mailAddresses;
    private List<URL> links;


    public Task(long id, String rawText, Date defaultPrependedDate) {
        this.id = id;
        this.init(rawText, defaultPrependedDate);
        this.originalPriority = priority;
        this.originalText = text;
    }

    public Task(long id, String rawText) {
        this(id, rawText, null);
    }

    public void update(String rawText) {
        this.init(rawText, null);
    }

    public void init(String rawText, Date defaultPrependedDate) {
        TextSplitter splitter = TextSplitter.getInstance();
        TextSplitter.SplitResult splitResult = splitter.split(rawText);
        this.priority = splitResult.priority;
        this.text = splitResult.text;
        this.prependedDate = splitResult.prependedDate;
        this.completed = splitResult.completed;
        this.completionDate = splitResult.completedDate;
        this.originalText = rawText;

        this.phoneNumbers = PhoneNumberParser.getInstance().parse(text);
        this.mailAddresses = MailAddressParser.getInstance().parse(text);
        this.links = LinkParser.getInstance().parse(text);
        this.contexts = ContextParser.getInstance().parse(text);
        this.projects = ProjectParser.getInstance().parse(text);
        this.deleted = Strings.isEmptyOrNull(text);

        if (defaultPrependedDate != null
                && Strings.isEmptyOrNull(this.prependedDate)) {
            SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT,Locale.US);
            this.prependedDate = formatter.format(defaultPrependedDate);
        }

        if (!Strings.isEmptyOrNull(this.prependedDate)) {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
            try {
                Date d = sdf.parse(this.prependedDate);
                this.relativeAge = RelativeDate.getRelativeDate(d);
            } catch (ParseException e) {
                // e.printStackTrace();
            }
        }
    }

    public Priority getOriginalPriority() {
        return originalPriority;
    }

    public String getOriginalText() {
        return originalText;
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

    public String getPrependedDate() {
        return prependedDate;
    }

    public void setPrependedDate(String date) {
        this.prependedDate = date;
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
        try {
            Date d = sdf.parse(this.prependedDate);
            this.relativeAge = RelativeDate.getRelativeDate(d);
         } catch (ParseException e) {
         // e.printStackTrace();
         }
    }

    public String getRelativeAge() {
        return relativeAge;
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

    public List<URL> getLinks() {
        return links;
    }

    public boolean inFuture() {
        if (Strings.isEmptyOrNull(this.getPrependedDate())) {
            return false;
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
            try {
                Date createDate = sdf.parse(this.prependedDate);
                Date now = new Date();
                return createDate.after(now);
            } catch (ParseException e) {
                // e.printStackTrace();
                return false;
            }
        }
    }

    public String getCompletionDate() {
        return completionDate;
    }

    public void markComplete(Date date) {
        if (!this.completed) {
            this.completionDate = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US)
                    .format(date);
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
        sb.append(this.text);
        return sb.toString();
    }

    public String inFileFormat() {
        StringBuilder sb = new StringBuilder();
        if (this.completed) {
            sb.append(COMPLETED).append(this.completionDate).append(" ");
            if (!Strings.isEmptyOrNull(this.prependedDate)) {
                sb.append(this.prependedDate).append(" ");
            }
        } else {
            if (priority != Priority.NONE) {
                sb.append(priority.inFileFormat()).append(" ");
            }
            if (!Strings.isEmptyOrNull(this.prependedDate)) {
                sb.append(this.prependedDate).append(" ");
            }
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

        if (id != ((Task)obj).id)
            return false;
        return (this.inFileFormat().equals(((Task)obj).inFileFormat()));
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + (completed ? 1231 : 1237);
        result = prime * result
                + ((completionDate == null) ? 0 : completionDate.hashCode());
        result = prime * result
                + ((contexts == null) ? 0 : contexts.hashCode());
        result = prime * result + (deleted ? 1231 : 1237);
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result
                + ((prependedDate == null) ? 0 : prependedDate.hashCode());
        result = prime * result
                + ((priority == null) ? 0 : priority.hashCode());
        result = prime * result
                + ((projects == null) ? 0 : projects.hashCode());
        result = prime * result
                + ((relativeAge == null) ? 0 : relativeAge.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    public void initWithFilters(ArrayList<String> ctxts, ArrayList<String> pjs) {
        if ((ctxts != null) && (ctxts.size() == 1)) {
            contexts.clear();
            contexts.add(ctxts.get(0));
        }
        if ((pjs != null) && (pjs.size() == 1)) {
            projects.clear();
            projects.add(pjs.get(0));
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
}
