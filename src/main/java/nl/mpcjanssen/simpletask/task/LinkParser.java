/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 * <p/>
 * Copyright (c) 2009-2013 Todo.txt contributors (http://todotxt.com)
 * <p/>
 * LICENSE:
 * <p/>
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * <p/>
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2013 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.mpcjanssen.simpletask.TodoException;

public class LinkParser {
    private static final Pattern LINK_PATTERN = Pattern
            .compile("(http|https|todo)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");
    private static final LinkParser INSTANCE = new LinkParser();

    private LinkParser() {
    }

    @NotNull
    public static LinkParser getInstance() {
        return INSTANCE;
    }

    @NotNull
    public List<String> parse(@Nullable String inputText) {
        if (inputText == null) {
            return Collections.emptyList();
        }

        Matcher m = LINK_PATTERN.matcher(inputText);
        List<String> links = new ArrayList<String>();
        while (m.find()) {
            String link;
            link = new String(m.group());
            links.add(link);
        }
        return links;
    }
}
