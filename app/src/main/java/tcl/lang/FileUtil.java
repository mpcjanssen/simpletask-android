/*
 * FileUtil.java --
 *
 *	This file contains utility methods for file-related operations.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FileUtil.java,v 1.8 2009/07/20 08:52:29 rszulgo Exp $
 *
 */

package tcl.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

/*
 * This class implements utility methods for file-related operations.
 */

public class FileUtil {

	public static final int PATH_RELATIVE = 0;
	public static final int PATH_VOLUME_RELATIVE = 1;
	public static final int PATH_ABSOLUTE = 2;
	/**
	 * Stores the /etc/passwd file for lookup of user directories.
	 * This should be replaced with a real native code lookup,
	 * and also support other non-unix platforms.
	 */
	private static HashMap<String, String> userDirs = null;

	/**
	 * In the Windows file system, one type of absolute path follows this
	 * regular expression: ^(//+[a-zA-Z]+/+[a-zA-Z]+)
	 * 
	 * If "path" doesn't fit the pattern, then return 0. If the stopEarly bool
	 * is true, then return the index of the first non-slash character in path,
	 * as soon as we know that path fits the pattern. Otherwise, return the
	 * index of the slash (or end of string) following the entire absolute path.
	 * 
	 * Results: Returns an integer index in path.
	 * 
	 * Side effects: If "path" fits the pattern, and "stopEarly" is not chosen,
	 * the absolute path is coppied (without extra slashes) to "absBuf".
	 * Otherwise, absBuf is set to "".
	 * 
	 * @param path
	 *            Path to compute home path of
	 * @param stopEarly
	 *            set to true to skip side effect
	 * @param absBuf
	 *            buffer to store side effect
	 * @return an integer index into the path
	 */
	private static int getWinHomePath(String path, // Path to compute home path
			// of.
			boolean stopEarly, // Flag to skip side effect.
			StringBuffer absBuf) // Buffer to store side effect.
	{
		int pIndex, oldIndex, firstNonSlash;

		// The first 2 or more chars must be slashes.

		for (pIndex = 0; pIndex < path.length(); pIndex++) {
			if (path.charAt(pIndex) != '/') {
				break;
			}
		}
		if (pIndex < 2) {
			absBuf.setLength(0);
			return 0;
		}
		firstNonSlash = pIndex;

		// The next 1 or more chars may not be slashes.

		for (; pIndex < path.length(); pIndex++) {
			if (path.charAt(pIndex) == '/') {
				break;
			}
		}
		if (pIndex == firstNonSlash) {
			absBuf.setLength(0);
			return 0;
		}
		absBuf.ensureCapacity(absBuf.length() + path.length());
		absBuf.append("//");
		absBuf.append(path.substring(firstNonSlash, pIndex));

		// The next 1 or more chars must be slashes.

		oldIndex = pIndex;
		for (; pIndex < path.length(); pIndex++) {
			if (path.charAt(pIndex) != '/') {
				if (pIndex == oldIndex) {
					absBuf.setLength(0);
					return 0;
				}

				// We know that the path fits the pattern.

				if (stopEarly) {
					absBuf.setLength(0);
					return firstNonSlash;
				}
				firstNonSlash = pIndex;

				// Traverse the path until a new slash (or end of string) is
				// found.
				// Return the index of the new slash.

				pIndex++;
				for (; pIndex < path.length(); pIndex++) {
					if (path.charAt(pIndex) == '/') {
						break;
					}
				}
				absBuf.append('/');
				absBuf.append(path.substring(firstNonSlash, pIndex));
				return pIndex;
			}
		}
		absBuf.setLength(0);
		return 0;
	}

	/**
	 * Determine whether a given windows path begins with [a-zA-Z]:
	 * 
	 * @param path
	 *            windows path to test
	 * @return O if path doesn't begin with [a-zA-Z]: Return 3 if path begins
	 *         with [a-zA-Z]:/ Otherwise, return 2.
	 */
	private static int beginsWithLetterColon(String path) // Path to check start
	// pattern.
	{
		if ((path.length() > 1) && (Character.isLetter(path.charAt(0))) && (path.charAt(1) == ':')) {

			int pIndex;
			for (pIndex = 2; pIndex < path.length(); pIndex++) {
				if (path.charAt(pIndex) != '/') {
					break;
				}
			}
			return pIndex;
		}
		return 0;
	}

	/**
	 * Side effects: If "path" begins with [A-Z]: or '/', copy the absolute (or
	 * volume realtive) path up to the index returned into absBuf, removing
	 * extra slashes.
	 * 
	 * @param path
	 *            of which to get abs path
	 * @param absBuf
	 *            buffer to store side effect
	 * 
	 * @return If "path" begins with [A-Z]: or '/', return the index of the
	 *         character (or end of string) following the absolute (or volume
	 *         realtive) path. Otherwise, return 0.
	 */

	private static int getWinAbsPath(String path, // Path for which we find abs
			// path.
			StringBuffer absBuf) // Buffer to store side effect.
	{
		absBuf.setLength(0);

		if (path.length() < 1) {
			return 0;
		}

		absBuf.ensureCapacity(absBuf.length() + path.length());

		int colonIndex = beginsWithLetterColon(path);
		if (colonIndex > 0) {
			if (colonIndex > 2) {
				absBuf.append(path.substring(0, 3));
			} else {
				absBuf.append(path.substring(0, 2));
			}
			return colonIndex;
		} else {
			int absIndex = getWinHomePath(path, false, absBuf);
			if (absIndex > 0) {
				return absIndex;
			} else if (path.charAt(0) == '/') {
				int pIndex;
				for (pIndex = 1; pIndex < path.length(); pIndex++) {
					if (path.charAt(pIndex) != '/') {
						break;
					}
				}
				absBuf.append("/");
				return pIndex;
			}
		}
		return 0;
	}

	/**
	 * @param path
	 *            to check
	 * @return the index of the 1st char (or end of string) which nolonger
	 *         follows the degenerate unix-style name pattern:
	 *         ^(/+([.][.]?/+)*([.][.]?)?)
	 */
	private static int getDegenerateUnixPath(String path) // Path to check.
	{
		int pIndex = 0;

		while ((pIndex < path.length()) && (path.charAt(pIndex) == '/')) {
			++pIndex;
		}

		// "path" doesn't begin with a '/'.

		if (pIndex == 0) {
			return 0;
		}
		while (pIndex < path.length()) {
			String tmpPath = path.substring(pIndex);
			if (tmpPath.startsWith("./")) {
				pIndex += 2;
			} else if (tmpPath.startsWith("../")) {
				pIndex += 3;
			} else {
				break;
			}
			while ((pIndex < path.length()) && (path.charAt(pIndex) == '/')) {
				++pIndex;
			}
		}
		if ((pIndex < path.length()) && (path.charAt(pIndex) == '.')) {
			++pIndex;
		}
		if ((pIndex < path.length()) && (path.charAt(pIndex) == '.')) {
			++pIndex;
		}

		// pIndex may be 1 past the end of "path".

		return pIndex;
	}

	/**
	 * @param path
	 *            String path to test
	 * @return PATH_RELATIVE if path is relative, PATH_ABSOLUTE if path is an
	 *         absolute path, PATH_VOLUME_RELATIVE if path is relative to a
	 *         volume
	 */

	public static int getPathType(String path) // Path for which we find
	// pathtype.
	{
		char c;
		if (path.length() < 1) {
			return PATH_RELATIVE;
		}

		switch (JACL.PLATFORM) {
		case JACL.PLATFORM_WINDOWS:
			path = path.replace('\\', '/');

			// Windows absolute pathes start with '~' or [a-zA-Z]:/ or home
			// path.

			c = path.charAt(0);
			if (c == '~') {
				return PATH_ABSOLUTE;
			}
			if (c == '/') {
				StringBuffer absBuf = new StringBuffer();
				if (getWinHomePath(path, true, absBuf) > 0) {
					return PATH_ABSOLUTE;
				}
				return PATH_VOLUME_RELATIVE;
			}
			int colonIndex = beginsWithLetterColon(path);
			if (colonIndex > 0) {
				if (colonIndex > 2) {
					return PATH_ABSOLUTE;
				}
				return PATH_VOLUME_RELATIVE;
			}
			return PATH_RELATIVE;

		case JACL.PLATFORM_MAC:
			if (path.charAt(0) == '~') {
				return PATH_ABSOLUTE;
			}

			switch (path.indexOf(':')) {
			case -1:
				// Unix-style name contains no colons. Return absolute iff
				// "path"
				// begins with '/' and is not degenerate. Otherwise, return
				// relative.

				if ((path.charAt(0) == '/') && (getDegenerateUnixPath(path) < path.length())) {
					return PATH_ABSOLUTE;
				}
				break;
			case 0:
				// Mac-style name contains a colon in the first position.

				return PATH_RELATIVE;
			default:
				// Mac-style name contains a colon, but not in the first
				// position.

				return PATH_ABSOLUTE;
			}
			return PATH_RELATIVE;

		default:
			// Unix absolute pathes start with either '/' or '~'.

			c = path.charAt(0);
			if ((c == '/') || (c == '~')) {
				return PATH_ABSOLUTE;
			}
		}
		return PATH_RELATIVE;
	}

	/**
	 * @param interp
	 *            current interpreter
	 * @param fileName
	 *            filename following Tcl rules
	 * @return Java File object for given file, as an absolute file
	 * @throws TclException
	 */
	public static File getNewFileObj(Interp interp, // Current interpreter.
			String fileName) // File to create object for.
			throws TclException {
		final boolean debug = false;
		fileName = translateFileName(interp, fileName);
		if (debug) {
			System.out.println("File name is \"" + fileName + "\"");
		}
		switch (getPathType(fileName)) {
		case PATH_RELATIVE:
			if (debug) {
				System.out.println("File name is PATH_RELATIVE");
			}
			return new File(interp.getWorkingDir(), fileName);
		case PATH_VOLUME_RELATIVE:
			if (debug) {
				System.out.println("File name is PATH_VOLUME_RELATIVE");
			}

			// Something is very wrong if interp.getWorkingDir()
			// does not start with C: or another drive letter
			String cwd = interp.getWorkingDir().toString();
			int index = beginsWithLetterColon(cwd);
			if (index == 0) {
				throw new TclRuntimeError("interp working directory \"" + cwd + "\" does not start with a drive letter");
			}

			// We can not use the joinPath() method because joing("D:/",
			// "/f.txt")
			// returns "/f.txt" for some wacky reason. Just do it ourselves.
			StringBuilder buff = new StringBuilder();
			buff.append(cwd.substring(0, 2));
			buff.append('\\');
			for (int i = 0; i < fileName.length(); i++) {
				if (fileName.charAt(i) != '\\') {
					// Once we skip all the \ characters at the front
					// append the rest of the fileName onto the buffer
					buff.append(fileName.substring(i));
					break;
				}
			}

			fileName = buff.toString();

			if (debug) {
				System.out.println("After PATH_VOLUME_RELATIVE join \"" + fileName + "\"");
			}

			return new File(fileName);
		case PATH_ABSOLUTE:
			if (debug) {
				System.out.println("File name is PATH_ABSOLUTE");
			}
			return new File(fileName);
		default:
			throw new TclRuntimeError("type for fileName \"" + fileName + "\" not matched in case statement");
		}
	}

	/**
	 * 
	 * Append "component" to "buf" while eliminating extra slashes.
	 * 
	 * @param component
	 *            strin to append to buf,
	 * @param compIndex
	 *            Current index in the component.
	 * @param compSize
	 *            Index following last in component.
	 * @param buf
	 *            Buffer to append the component.
	 */
	private static void appendComponent(String component, // Component to
			// append.
			int compIndex, // 
			int compSize, // 
			StringBuffer buf) // 
	{
		for (; compIndex < component.length(); compIndex++) {
			char c = component.charAt(compIndex);
			if (c == '/') {
				// Eliminate duplicate slashes.

				while ((compIndex < compSize) && (component.charAt(compIndex + 1) == '/')) {
					compIndex++;
				}

				// Only add a slash if following non-slash elements exist.

				if (compIndex < compSize) {
					buf.ensureCapacity(buf.length() + 1);
					buf.append('/');
				}
			} else {
				buf.ensureCapacity(buf.length() + 1);
				buf.append(c);
			}
		}
	}

	/**
	 * Combine a list of pathes into one path. It is necessary to perform system
	 * specific operations.
	 * 
	 * @param interp
	 *            Current interpreter
	 * @param argv
	 *            List of pathes to be joined.
	 * @param startIndex
	 *            1st item in argv to join.
	 * @param endIndex
	 *            1st item to ignore.
	 * @return a path String.
	 * @throws TclException
	 */
	public static String joinPath(Interp interp, TclObject[] argv, // 
			int startIndex, // 
			int endIndex) // 
			throws TclException // Thrown if TclList ops fail.
	{
		StringBuffer result = new StringBuffer();

		switch (JACL.PLATFORM) {
		case JACL.PLATFORM_WINDOWS:
			// Iterate over all of the components. If a component is
			// absolute, then reset the result and start building the
			// path from the current component on.

			for (int i = startIndex; i < endIndex; i++) {

				String p = argv[i].toString().replace('\\', '/');
				int pIndex = 0;
				int pLastIndex = p.length() - 1;

				if (p.length() == 0) {
					continue;
				}

				StringBuffer absBuf = new StringBuffer();
				pIndex = getWinAbsPath(p, absBuf);
				if (pIndex > 0) {
					// If the path is absolute or volume relative (except those
					// beginning with '~'), reset the result buffer to the
					// absolute
					// substring.

					result = absBuf;
				} else if (p.charAt(0) == '~') {
					// If the path begins with '~', reset the result buffer to
					// "".

					result.setLength(0);
				} else {
					// This is a relative path. Remove the ./ from tilde
					// prefixed
					// elements unless it is the first component.

					if ((result.length() != 0) && (p.regionMatches(pIndex, "./~", 0, 3))) {
						pIndex = 2;
					}

					// Check to see if we need to append a separator before
					// adding
					// this relative component.

					if (result.length() != 0) {
						char c = result.charAt(result.length() - 1);
						if ((c != '/') /* && (c != ':') */) {
							result.append('/');
						}
					}
				}

				// Append the element.

				appendComponent(p, pIndex, pLastIndex, result);
				pIndex = p.length();
			}
			return result.toString();

		case JACL.PLATFORM_MAC:
			// Iterate over all of the components. If a component is
			// absolute, then reset the result and start building the
			// path from the current component on.

			boolean needsSep = true;
			for (int i = startIndex; i < endIndex; i++) {

				TclObject splitArrayObj[] = TclList.getElements(interp, splitPath(interp, argv[i].toString()));

				if (splitArrayObj.length == 0) {
					continue;
				}

				// If 1st path element is absolute, reset the result to "" and
				// append the 1st path element to it.

				int start = 0;
				String p = splitArrayObj[0].toString();
				if ((p.charAt(0) != ':') && (p.indexOf(':') != -1)) {
					result.setLength(0);
					result.append(p);
					start++;
					needsSep = false;
				}

				// Now append the rest of the path elements, skipping
				// : unless it is the first element of the path, and
				// watching out for :: et al. so we don't end up with
				// too many colons in the result.

				for (int j = start; j < splitArrayObj.length; j++) {

					p = splitArrayObj[j].toString();

					if (p.equals(":")) {
						if (result.length() != 0) {
							continue;
						} else {
							needsSep = false;
						}
					} else {
						char c = 'o';
						if (p.length() > 1) {
							c = p.charAt(1);
						}
						if (p.charAt(0) == ':') {
							if (!needsSep) {
								p = p.substring(1);
							}
						} else {
							if (needsSep) {
								result.append(':');
							}
						}
						if (c == ':') {
							needsSep = false;
						} else {
							needsSep = true;
						}
					}
					result.append(p);
				}
			}
			return result.toString();

		default:
			// Unix platform.

			for (int i = startIndex; i < endIndex; i++) {

				String p = argv[i].toString();
				int pIndex = 0;
				int pLastIndex = p.length() - 1;

				if (p.length() == 0) {
					continue;
				}

				if (p.charAt(pIndex) == '/') {
					// If the path is absolute (except those beginning with
					// '~'),
					// reset the result buffer to the absolute substring.

					while ((pIndex <= pLastIndex) && (p.charAt(pIndex) == '/')) {
						pIndex++;
					}
					result.setLength(0);
					result.append('/');
				} else if (p.charAt(pIndex) == '~') {
					// If the path begins with '~', reset the result buffer to
					// "".

					result.setLength(0);
				} else {
					// This is a relative path. Remove the ./ from tilde
					// prefixed
					// elements unless it is the first component.

					if ((result.length() != 0) && (p.regionMatches(pIndex, "./~", 0, 3))) {
						pIndex += 2;
					}

					// Append a separator if needed.

					if ((result.length() != 0) && (result.charAt(result.length() - 1) != '/')) {
						result.ensureCapacity(result.length() + 1);
						result.append('/');
					}
				}

				// Append the element.

				appendComponent(p, pIndex, pLastIndex, result);
				pIndex = p.length();
			}
		}
		return result.toString();
	}

	/**
	 * Turn one path into a list of components.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param path
	 *            Path to split
	 * @throws TclException
	 * 
	 * @return Tcl List Object containing the components of path
	 */

	public static TclObject splitPath(Interp interp, // Current interpreter for
			// path
			// split.
			String path) // Path to be split.
			throws TclException // Thrown if TclList ops fail.
	{
		TclObject resultListObj = TclList.newInstance();
		TclObject componentObj;
		String component = "";
		String tmpPath;
		boolean foundComponent = false;
		boolean convertDotToColon = false;
		boolean isColonSeparator = false;
		boolean appendColon = false;
		boolean prependColon = false;
		String thisDir = "./";

		// If the path is the empty string, returnan empty result list.

		if (path.length() == 0) {
			return resultListObj;
		}

		// Handling the 1st component is file system dependent.

		switch (JACL.PLATFORM) {
		case JACL.PLATFORM_WINDOWS:
			tmpPath = path.replace('\\', '/');

			StringBuffer absBuf = new StringBuffer();
			int absIndex = getWinAbsPath(tmpPath, absBuf);
			if (absIndex > 0) {
				componentObj = TclString.newInstance(absBuf.toString());
				TclList.append(interp, resultListObj, componentObj);
				tmpPath = tmpPath.substring(absIndex);
				foundComponent = true;
			}
			break;

		case JACL.PLATFORM_MAC:

			tmpPath = "";
			thisDir = ":";

			switch (path.indexOf(':')) {
			case -1:
				// Unix-style name contains no colons.

				if (path.charAt(0) != '/') {
					tmpPath = path;
					convertDotToColon = true;
					if (path.charAt(0) == '~') {
						// If '~' is the first char, then append a colon to end
						// of the 1st component.

						appendColon = true;
					}
					break;
				}
				int degenIndex = getDegenerateUnixPath(path);
				if (degenIndex < path.length()) {
					// First component of absolute unix path is followed by a
					// ':',
					// instead of being preceded by a degenerate unix-style
					// pattern.

					tmpPath = path.substring(degenIndex);
					convertDotToColon = true;
					appendColon = true;
					break;
				}

				// Degenerate unix path can't be split. Return a list with one
				// element: ":" prepended to "path".

				componentObj = TclString.newInstance(":" + path);
				TclList.append(interp, resultListObj, componentObj);
				return resultListObj;
			case 0:
				// Relative mac-style name contains a colon in the first
				// position.

				if (path.length() == 1) {
					// If path == ":", then return a list with ":" as its only
					// element.

					componentObj = TclString.newInstance(":");
					TclList.append(interp, resultListObj, componentObj);
					return resultListObj;
				}

				// For each component, if slashes exist in the remaining
				// filename,
				// prepend a colon to the component. Since this path is
				// relative,
				// pretend that we have already processed 1 components so a
				// tilde-prefixed 1st component will have ":" prepended to it.

				tmpPath = path.substring(1);
				foundComponent = true;
				prependColon = true;
				isColonSeparator = true;
				break;

			default:
				// Absolute mac-style name contains a colon, but not in the
				// first
				// position. Append a colon to the first component, and, for
				// each
				// following component, if slashes exist in the remaining
				// filename,
				// prepend a colon to the component.

				tmpPath = path;
				appendColon = true;
				prependColon = true;
				isColonSeparator = true;
				break;
			}
			break;

		default:
			// Unix file name: if the first char is a "/", append "/" to the
			// result
			// list.

			if (path.charAt(0) == '/') {
				componentObj = TclString.newInstance("/");
				TclList.append(interp, resultListObj, componentObj);
				tmpPath = path.substring(1);
				foundComponent = true;
			} else {
				tmpPath = path;
			}
		}

		// Iterate over all of the components of the path.

		int sIndex = 0;
		while (sIndex != -1) {
			if (isColonSeparator) {
				sIndex = tmpPath.indexOf(":");
				// process adjacent ':'

				if (sIndex == 0) {
					componentObj = TclString.newInstance("::");
					TclList.append(interp, resultListObj, componentObj);
					foundComponent = true;
					tmpPath = tmpPath.substring(sIndex + 1);
					continue;
				}
			} else {
				sIndex = tmpPath.indexOf("/");
				// Ignore a redundant '/'

				if (sIndex == 0) {
					tmpPath = tmpPath.substring(sIndex + 1);
					continue;
				}
			}
			if (sIndex == -1) {
				// Processing the last component. If it is empty, exit loop.

				if (tmpPath.length() == 0) {
					break;
				}
				component = tmpPath;
			} else {
				component = tmpPath.substring(0, sIndex);
			}

			if (convertDotToColon && (component.equals(".") || component.equals(".."))) {
				// If platform = MAC, convert .. to :: or . to :

				component = component.replace('.', ':');
			}
			if (foundComponent) {
				if (component.charAt(0) == '~') {
					// If a '~' preceeds a component (other than the 1st one),
					// then
					// prepend "./" or ":" to the component.

					component = thisDir + component;
				} else if (prependColon) {
					// If the prependColon flag is set, either unset it or
					// prepend
					// ":" to the component, depending on whether any '/'s
					// remain
					// in tmpPath.

					if (tmpPath.indexOf('/') == -1) {
						prependColon = false;
					} else {
						component = ":" + component;
					}
				}
			} else if (appendColon) {
				// If platform = MAC, append a ':' to the first component.

				component = component + ":";
			}
			componentObj = TclString.newInstance(component);
			TclList.append(interp, resultListObj, componentObj);
			foundComponent = true;
			tmpPath = tmpPath.substring(sIndex + 1);
		}
		return resultListObj;
	}

	/**
	 * Given a string following a tilde, this routine returns the corresponding
	 * home directory.
	 * 
	 * @param interp
	 *            interpreter
	 * @param user
	 *            username following the tilde
	 * @throws TclException
	 *             if env(HOME) is not set or if an unknown user is requested
	 *@return A string containing the home directory in native format.
	 */

	public static String doTildeSubst(Interp interp, // Current interpreter.
			String user) // User whose home we must find.
			throws TclException // Thrown if env(HOME) is not set or if
	// another user is requested.
	{
		String dir;

		if (user.length() == 0) {
			try {
				dir = interp.getVar("env", "HOME", TCL.GLOBAL_ONLY).toString();
			} catch (Exception e) {
				throw new TclException(interp, "couldn't find HOME environment variable to expand path");
			}
			return dir;
		}

		// Try to use /etc/passwd for users; this really should be done with native code and also
		// should support other platforms
		
		if (Util.isUnix()) {
			if (userDirs == null) {
				try {
					userDirs = new HashMap<>();
					File passwd = new File("/etc/passwd");
					BufferedReader reader = new BufferedReader(new FileReader(passwd));
					while (true) {
						String line = reader.readLine();
						if (line==null) break;
						String [] fields = line.split(":");
						if (fields.length >= 6 && fields[0].length() > 0 && fields[5].length() > 0) {
							userDirs.put(fields[0], fields[5]);
						}
					}
					reader.close();
				} catch (IOException e) {
						userDirs = null;
				}
			}
			if (userDirs != null && userDirs.containsKey(user)) {
				return userDirs.get(user);
			}
		}

		throw new TclException(interp, "user \"" + user + "\" doesn't exist");
	}

	/**
	 * If the path starts with a tilde, do tilde substitution on the first
	 * component and join it with the remainder of the path. Otherwise, do
	 * nothing.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param path
	 *            path to be translationed
	 * @return the tilde-substituted path
	 */

	public static String translateFileName(Interp interp, String path) throws TclException {
		String fileName = "";

		if ((path.length() == 0) || (path.charAt(0) != '~')) {
			// fileName = path;
			TclObject joinArrayObj[] = new TclObject[1];
			joinArrayObj[0] = TclString.newInstance(path);
			fileName = joinPath(interp, joinArrayObj, 0, 1);
		} else {
			TclObject splitArrayObj[] = TclList.getElements(interp, splitPath(interp, path));

			String user = splitArrayObj[0].toString().substring(1);

			// Strip the trailing ':' off of a Mac path
			// before passing the user name to DoTildeSubst.

			if ((JACL.PLATFORM == JACL.PLATFORM_MAC) && (user.endsWith(":"))) {
				user = user.substring(0, user.length() - 1);
			}

			user = doTildeSubst(interp, user);

			// if (splitArrayObj.length < 2) {
			// fileName = user;
			// } else {
			splitArrayObj[0] = TclString.newInstance(user);
			fileName = joinPath(interp, splitArrayObj, 0, splitArrayObj.length);
			// }
		}

		// Convert forward slashes to backslashes in Windows paths because
		// some system interfaces don't accept forward slashes.

		if (JACL.PLATFORM == JACL.PLATFORM_WINDOWS) {
			fileName = fileName.replace('/', '\\');
		}
		return fileName;
	}

	/**
	 * Split the path. If there is only one component, and it starts with a
	 * tilde, do tilde substitution and split its result.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param path
	 *            to be split
	 * @return a TclList object containing the components of path
	 */

	public static TclObject splitAndTranslate(Interp interp, // Current
			// interpreter
			// for
			// path split.
			String path) // Path to be split.
			throws TclException // Thrown if tilde subst, which may be
	// called by translateFileName, fails.
	{
		TclObject splitResult = splitPath(interp, path);

		int len = TclList.getLength(interp, splitResult);
		if (len == 1) {
			String fileName = TclList.index(interp, splitResult, 0).toString();
			if (fileName.charAt(0) == '~') {
				String user = translateFileName(interp, fileName);
				splitResult = splitPath(interp, user);
			}
		}
		return splitResult;
	}

	/**
	 * 
	 * @return the platform separator. Note that the separator is
	 *         platform-dependent. On MACs separator depends on the given path
	 *         ('/' or ':')
	 */
	public static final String getSeparators(String arg) {
		String separators;

		switch (JACL.PLATFORM) {
		case JACL.PLATFORM_WINDOWS:
			separators = "/\\:";
			break;
		case JACL.PLATFORM_MAC:
			if (arg != null && arg.indexOf(':') != -1) {
				separators = ":";
			} else {
				separators = "/";
			}
			break;
		default:
			separators = "/";
		}

		return separators;
	}

	
	/**
	 * This important function attempts to extract from the given TclObject a
	 * unique normalized path representation, whose string value can be used as
	 * a unique identifier for the file.
	 * 
	 * @return NULL or a valid path object reference.
	 * 
	 *         Side effects: none -
	 */

	public static final TclObject getNormalizedPath(Interp interp, TclObject pathObj) {

		File file;

		try {
			file = FileUtil.getNewFileObj(interp, pathObj.toString());
			String name = file.getName();
			File parent = file.getParentFile();
			File normalized;
			if (".".equals(name) || "..".equals(name)) {
				normalized = file.getCanonicalFile();
			} else {
				normalized = new File(parent.getCanonicalFile(), name);
			}
			return TclString.newInstance(normalized);
		} catch (TclException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Get the target file of a link, or null if the specified file is not a
	 * link.  Note that this returns a full absolute path to the link
	 * target; even if the link target is  relative. FIXME: Java 1.7 will provide
	 * a better link test
	 * 
	 * @param link
	 *            File to test
	 * @return target of the 'link' File, if it is a link, or null if not.
	 */
	public static File getLinkTarget(File link) {
		try {
			File absFile = link.getAbsoluteFile();

			String name = absFile.getName();
			if (".".equals(name) || "..".equals(name)) {
				return null; // can't be a link
			} else {
				File parent = absFile.getParentFile();
				String normalized = new File(parent.getCanonicalFile(), name).getPath();
				File canNormalizedFile = new File(normalized);
				String canNormalized = canNormalizedFile.getCanonicalPath();
				
				if (normalized==null || canNormalized==null) return null;
				if (normalized.equals(canNormalized)) return null;  // no link since they resolve the same
				return canNormalizedFile;
			}
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Tests if the given file is executable by the current user. If Java 1.6+
	 * File.canExecute() is not available, will make its best guess
	 * 
	 * @param fileObj
	 *            File to test for executability
	 * @return true if file is executable, false otherwise
	 */
	public static boolean isExecutable(File fileObj) {

		/* It might be possible to use Java 1.6+ API */
		try {
			Method canExecute = fileObj.getClass().getMethod("canExecute");
			Boolean result = (Boolean) canExecute.invoke(fileObj);
			return result;

		} catch (Exception e) {

			/* Revert to pre-Java 1.6 guess */
			boolean isExe = false;

			// A file must exist to be executable. Directories are always
			// executable.

			if (fileObj.exists()) {
				isExe = fileObj.isDirectory();
				if (isExe)
					return true;

				if (Util.isWindows()) {
					// File that ends with .exe, .com, or .bat is
					// executable.
					String fileName = fileObj.getName();
					isExe = (fileName.endsWith(".exe") || fileName.endsWith(".com") || fileName.endsWith(".bat"));
				} else {
					/* Just return true for Mac and Unix */
					isExe = true;
				}
				return isExe;
			} else {
				return false;
			}
		}
	}

} // end FileUtil class
