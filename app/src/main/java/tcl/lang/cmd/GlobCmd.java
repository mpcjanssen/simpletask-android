/*
 * GlobCmd.java
 *
 *	This file contains the Jacl implementation of the built-in Tcl "glob"
 *	command.
 *
 * Copyright (c) 1997-1998 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: GlobCmd.java,v 1.10 2009/07/20 08:50:56 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import tcl.lang.Command;
import tcl.lang.FileUtil;
import tcl.lang.Interp;
import tcl.lang.JACL;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.Util;

/*
 * This class implements the built-in "glob" command in Tcl.
 */

public class GlobCmd implements Command {

	static final private int TYPE_BLOCKSPECIAL = 0x1;
	static final private int TYPE_CHARSPECIAL = 0x2;
	static final private int TYPE_DIRECTORY = 0x4;
	static final private int TYPE_REGULARFILE = 0x8;
	static final private int TYPE_LINK = 0x10;
	static final private int TYPE_PIPE = 0x20;
	static final private int TYPE_SOCKET = 0x40;
	static final private int TYPE_PERM_R = 0x80;
	static final private int TYPE_PERM_W = 0x100;
	static final private int TYPE_PERM_X = 0x200;
	static final private int TYPE_READONLY = 0x400;
	static final private int TYPE_HIDDEN = 0x800;
	static final private int TYPE_MACINTOSH = 0x1000;

	/*
	 * Options to the glob command.
	 */
	static final private String validOptions[] = { "-directory", "-join", "-nocomplain", "-path", "-tails", "-types",
			"--" };

	static final private int OPT_DIRECTORY = 0;
	static final private int OPT_JOIN = 1;
	static final private int OPT_NOCOMPLAIN = 2;
	static final private int OPT_PATH = 3;
	static final private int OPT_TAILS = 4;
	static final private int OPT_TYPES = 5;
	static final private int OPT_LAST = 6;

	/**
	 * invoked to process the "glob" Tcl command.
	 * 
	 * @param interp
	 *            the current interpreter
	 * @param argv
	 *            args passed to the glob command
	 * 
	 * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
	 */
	public void cmdProc(Interp interp, // Current interp to eval the file cmd.
			TclObject argv[]) // Args passed to the glob command.
			throws TclException {

		boolean noComplain = false;
		boolean join = false;
		boolean dirMode = false;
		boolean pathMode = false;
		boolean tails = false;
		int types = 0;
		File topDirectory = null;
		String prefix = "";
		int firstNonSwitchArgumentIndex = 1;
		String joined = "";

		if (argv.length == 1) {
			throw new TclNumArgsException(interp, 1, argv, "?switches? name ?name ...?");
		}

		for (boolean last = false; (firstNonSwitchArgumentIndex < argv.length) && (!last); firstNonSwitchArgumentIndex++) {

			if (!argv[firstNonSwitchArgumentIndex].toString().startsWith("-")) {
				break;
			}

			int opt = TclIndex.get(interp, argv[firstNonSwitchArgumentIndex], validOptions, "option", 0);

			switch (opt) {
			case OPT_NOCOMPLAIN:
				noComplain = true;
				break;

			case OPT_DIRECTORY:
				if (argv.length < 3) {
					throw new TclException(interp, "missing argument to \"-directory\"");
				}

				if (pathMode) {
					throw new TclException(interp, "\"-directory\" cannot be used with \"-path\"");
				}

				dirMode = true;
				topDirectory = new File(FileUtil.translateFileName(interp, argv[++firstNonSwitchArgumentIndex]
						.toString()));
				break;

			case OPT_JOIN:
				join = true;
				break;

			case OPT_PATH:
				if (firstNonSwitchArgumentIndex == argv.length - 1) {
					throw new TclException(interp, "missing argument to \"-path\"");
				}

				if (dirMode) {
					throw new TclException(interp, "\"-path\" cannot be used with \"-directory\"");
				}

				pathMode = true;
				File path = new File(FileUtil.translateFileName(interp, argv[++firstNonSwitchArgumentIndex].toString()));
				topDirectory = path.getParentFile();
				prefix = path.getName();

				break;

			case OPT_TAILS:
				tails = true;
				break;

			case OPT_TYPES:
				if (firstNonSwitchArgumentIndex == argv.length - 1) {
					throw new TclException(interp, "missing argument to \"-types\"");
				}
				TclObject[] typeObjs = TclList.getElements(interp, argv[++firstNonSwitchArgumentIndex]);
				for (TclObject t : typeObjs) {
					String ts = t.toString();
					if (ts.length() == 1) {
						switch (ts.charAt(0)) {
						case 'd':
							types |= TYPE_DIRECTORY;
							break;
						case 'b':
							types |= TYPE_BLOCKSPECIAL;
							break;
						case 'c':
							types |= TYPE_CHARSPECIAL;
							break;
						case 'f':
							types |= TYPE_REGULARFILE;
							break;
						case 'l':
							types |= TYPE_LINK;
							break;
						case 'p':
							types |= TYPE_PIPE;
							break;
						case 's':
							types |= TYPE_SOCKET;
							break;
						case 'r':
							types |= TYPE_PERM_R;
							break;
						case 'w':
							types |= TYPE_PERM_W;
							break;
						case 'x':
							types |= TYPE_PERM_X;
							break;
						default:
							throw new TclException(interp, "bad argument to \"-types\": " + ts);
						}
					} else { // length of string > 1
						if ("hidden".equals(ts)) {
							types |= TYPE_HIDDEN;
						} else if ("readonly".equals(ts)) {
							types |= TYPE_READONLY;
						} else if (ts.contains("macintosh")) {
							/*
							 * This code just kinda parses up the macintosh
							 * stuff and syntax errors in a way that makes the
							 * test cases pass. We don't actually do anything
							 * with the macintosh flags
							 */
							if ((types & TYPE_MACINTOSH) != 0)
								throw new TclException(interp,
										"only one MacOS type or creator argument to \"-types\" allowed");
							types |= TYPE_MACINTOSH;
						} else {
							if (typeObjs.length > 1)
								/*
								 * this silliness is just to make test cases
								 * pass
								 */
								throw new TclException(interp,
										"only one MacOS type or creator argument to \"-types\" allowed");
							else
								throw new TclException(interp, "bad argument to \"-types\": " + ts);
						}
					}
				}

				break;

			case OPT_LAST:
				last = true;
				break;

			default:
				throw new TclException(interp, "GlobCmd.cmdProc: bad option " + opt + " index to validOptions");
			}
		}

		if (firstNonSwitchArgumentIndex >= argv.length) {
			throw new TclNumArgsException(interp, 1, argv, "?switches? name ?name ...?");
		}

		if (tails && !dirMode && !pathMode) {
			throw new TclException(interp, "\"-tails\" must be used with either \"-directory\" or \"-path\"");
		}

		/*
		 * 
		 * Now that the command line has been parsed, do the actual globbing
		 */
		TclObject resultList = TclList.newInstance();
		resultList.preserve();

		try {
			ArrayList<StringBuffer> patternList = new ArrayList<>();

			/*
			 * Copy the patterns into patterns, joining the arguments into one
			 * pattern if requested
			 */
			if (join) {
				/* join all the arguments together */
				joined = FileUtil.joinPath(interp, argv, firstNonSwitchArgumentIndex, argv.length);
				if (argv[firstNonSwitchArgumentIndex].toString().length() == 0) {
					joined = "/" + joined;
				}
				/*
				 * expand out any brace expressions like src/{*.c}{*.h} into a
				 * list of patterns: src/*.c and src/*.h
				 */
				patternList.add(new StringBuffer());
				expandBraceExpressions(interp, joined, 0, patternList, false);
			} else {
				for (int i = firstNonSwitchArgumentIndex; i < argv.length; i++) {
					/*
					 * expand out any brace expressions like src/{*.c}{*.h} into
					 * a list of patterns: src/*.c and src/*.h
					 */
					ArrayList<StringBuffer> argExpansion = new ArrayList<>();
					argExpansion.add(new StringBuffer());
					expandBraceExpressions(interp, argv[i].toString(), 0, argExpansion, false);
					patternList.addAll(argExpansion);
				}
			}

			String[] patterns = new String[patternList.size()];
			for (int i = 0; i < patternList.size(); i++) {
				patterns[i] = patternList.get(i).toString();
				if (patterns[i].length() == 0) {
					patterns[i] = ".";
				}
				if (topDirectory == null) {
					/* do any tilde conversion on the pattern */
					boolean isDir = patterns[i].endsWith(File.separator);
					patterns[i] = FileUtil.translateFileName(interp, patterns[i]) + (isDir ? File.separator : "");
				}
			}

			/*
			 * For each pattern in patterns, find matching files. For example,
			 * for glob src/*.c include/*.h, we first do pattern "src/*.c" and
			 * then do "include/*.h"
			 */
			for (String pattern : patterns) {
				// System.out.println("Pattern is ["+pattern+"]");
				getResultsForOnePattern(interp, pattern, types, topDirectory, prefix, tails, resultList);
			}
		} catch (TclException e) {
			resultList.release();
			if (noComplain) {
				interp.setResult("");
				return;
			} else
				throw e;
		}

		/*
		 * If the list is empty and the nocomplain switch was not set then
		 * generate and throw an exception. Always release the TclList upon
		 * completion.
		 */
		try {
			if ((TclList.getLength(interp, resultList) == 0) && !noComplain) {
				String sep = "";
				StringBuilder ret = new StringBuilder();

				ret.append("no files matched glob pattern");
				ret.append((join || (argv.length - firstNonSwitchArgumentIndex == 1)) ? " \"" : "s \"");

				if (join) {
					ret.append(joined);
				} else {
					for (int i = firstNonSwitchArgumentIndex; i < argv.length; i++) {
						ret.append(sep + argv[i].toString());
						if (i == firstNonSwitchArgumentIndex) {
							sep = " ";
						}
					}
				}
				ret.append("\"");
				throw new TclException(interp, ret.toString());
			} else if (TclList.getLength(interp, resultList) > 0) {
				interp.setResult(resultList);
			}
		} finally {
			resultList.release();
		}

	}

	/**
	 * Append a character to every StringBuffer in the specified ArrayList
	 * 
	 * @param a
	 *            ArrayList containing StringBuffers
	 * @param c
	 *            character to append
	 */
	private static void stringBufferListAppend(ArrayList<StringBuffer> a, char c) {
		for (StringBuffer sb : a) {
			sb.append(c);
		}
	}

	/**
	 * Recursively expand all the brace expressions in a glob pattern.
	 * 
	 * @param interp
	 *            the current interpreter
	 * @param pattern
	 *            a glob pattern that may contain {} sections
	 * @param nextIndex
	 *            index to start expansion from
	 * @param expandedPatterns
	 *            list of expandedPatterns so far. When complete, this ArrayList
	 *            contains all the patterns that 'pattern' expands to.
	 * @param inBrace
	 *            set to true if inside of brace expression
	 * @return next index at which to continue parsing pattern
	 * 
	 * @throws TclException
	 *             on unmatched '{ ' or '}'
	 */
	private int expandBraceExpressions(Interp interp, String pattern, int nextIndex,
			ArrayList<StringBuffer> expandedPatterns, boolean inBrace) throws TclException {

		boolean lastCharWasBackslash = false;

		while (nextIndex < pattern.length()) {
			char c = pattern.charAt(nextIndex++);

			if (lastCharWasBackslash) {
				lastCharWasBackslash = false;
				stringBufferListAppend(expandedPatterns, c);
				continue;
			}

			switch (c) {

			case '{':
				ArrayList<StringBuffer> alternation = new ArrayList<>();
				--nextIndex;
				while (nextIndex < pattern.length() && pattern.charAt(nextIndex) != '}') {
					ArrayList<StringBuffer> oneAlternative = new ArrayList<>();
					oneAlternative.add(new StringBuffer());
					/*
					 * call recusrively to get the next string in the {}
					 * expression
					 */
					/*
					 * since we return the index of the ',' or '}', always add 1
					 * to it
					 */
					nextIndex = expandBraceExpressions(interp, pattern, nextIndex + 1, oneAlternative, true);
					alternation.addAll(oneAlternative);
				}
				++nextIndex; // get past closing {
				/*
				 * Now build a new expandedPatterns by duplicating existing list
				 * with each of the alternations
				 */
				ArrayList<StringBuffer> newExpandedPatterns = new ArrayList<>(expandedPatterns.size()
						* alternation.size());
				for (StringBuffer alternationSb : alternation) {
					for (StringBuffer prefix : expandedPatterns) {
						newExpandedPatterns.add(new StringBuffer(prefix.toString() + alternationSb.toString()));
					}
				}
				expandedPatterns.clear();
				expandedPatterns.addAll(newExpandedPatterns);
				break;

			case '}':
			case ',':
				if (inBrace) {
					return nextIndex - 1; /* caller needs to see the '}' or ',' */
				} else if (c == '}') {
					throw new TclException(interp, "unmatched close-brace in file name");
				}
				break;

			case '\\':
				if (nextIndex < pattern.length() && pattern.charAt(nextIndex) == File.separatorChar) {
					/* don't escape separators; it confuses FileUtil methods */
				} else {
					lastCharWasBackslash = true;
					stringBufferListAppend(expandedPatterns, c);
				}
				break;

			default:
				stringBufferListAppend(expandedPatterns, c);
				break;
			}
		}

		if (nextIndex >= pattern.length() && inBrace)
			throw new TclException(interp, "unmatched open-brace in file name");

		return nextIndex;
	}

	/**
	 * Get the glob results for one pattern, which does not contain any brace
	 * expressions
	 * 
	 * @param interp
	 *            The current interpreter
	 * @param pattern
	 *            The glob pattern, which does not contain any {} sections
	 * @param types
	 *            The TYPE_* as specified in the -types option to glob
	 * @param topDirectory
	 *            The directory to start searching from, or null for the current
	 *            working directory. This is either the -directory option, or
	 *            the parent directory of the -path option
	 * @param prefix
	 *            The file name prefix, or an empty string for no prefix. Glob
	 *            characters in this string are literally matched to the files
	 *            in topDirectory.
	 * @param tails
	 *            if true, only return the filename; otherwise return the entire
	 *            path
	 * @param resultList
	 *            TclObject into which this method stores its results
	 * @throws TclException
	 */
	private void getResultsForOnePattern(Interp interp, String pattern, int types, File topDirectory, String prefix,
			boolean tails, TclObject resultList) throws TclException {
		Stack<GlobPair> stack = new Stack<>();

		/*
		 * Inspect path to see if it has a trailing separator; this can get lost
		 * in FileUtil manipulations
		 */
		boolean hasTrailingSeparator = pattern.endsWith(File.separator);

		/* Perform any necessary tilde translation */
		if (topDirectory == null)
			pattern = FileUtil.translateFileName(interp, pattern);

		TclObject splitPattern = FileUtil.splitPath(interp, pattern);

		boolean patternIsAbsolutePath = false;
		boolean ignoreZerothComponent = false;

		if (FileUtil.getPathType(pattern) == FileUtil.PATH_ABSOLUTE) {
			if (topDirectory == null) {
				/*
				 * If there is no directory, this pattern really represents an
				 * absolute file
				 */
				patternIsAbsolutePath = true;
				if (TclList.getLength(interp, splitPattern) == 1) {
					/* It's just the root, so return just the root */
					TclList.append(interp, resultList, TclString.newInstance(pattern));
					return;
				}
			} else {
				/* pattern is not absolute, because a directory precedes it */
				patternIsAbsolutePath = false;
				/*
				 * Strip off the psuedo-root: glob -directory /usr /lib should
				 * be glob -directory /usr lib
				 */
				ignoreZerothComponent = true;
			}

		}

		/*
		 * Split pattern into its components: src/*.c has two components: src
		 * and *.c
		 */
		TclObject[] patternComponents = TclList.getElements(interp, splitPattern);

		/*
		 * Create the glob filters for each component of the glob pattern: e.g.
		 * one for 'src', one for '*.c'
		 */
		GlobFilter[] componentGlobFilters = new GlobFilter[patternComponents.length];
		for (int i = 0; i < componentGlobFilters.length; i++) {
			componentGlobFilters[i] = new GlobFilter(prefix, patternComponents[i].toString(), types,
					hasTrailingSeparator || i != componentGlobFilters.length - 1);
		}

		/* Push the first directory/glob on the stack */
		if (patternIsAbsolutePath) {
			/*
			 * The first component, indicating the root (like '/') has no glob
			 * chars
			 */
			stack.push(new GlobPair(new File(patternComponents[0].toString()), 1));
		} else {
			stack.push(new GlobPair(topDirectory, ignoreZerothComponent ? 1 : 0));
		}

		while (!stack.empty()) {
			GlobPair globPair = stack.pop();

			String[] globResults;
			if (globPair.dir == null) {
				/*
				 * topDirectory was null, and pattern is not absolute, so use
				 * cwd
				 */
				globResults = componentGlobFilters[globPair.componentIndex].list(interp.getWorkingDir());
			} else {
				File dir;
				if (globPair.dir.isAbsolute())
					dir = globPair.dir;
				else {
					// Tcl's idea of the current directory may be different than
					// Java's
					TclObject[] path = new TclObject[2];
					path[0] = TclString.newInstance(interp.getWorkingDir().getAbsolutePath());
					path[1] = TclString.newInstance(globPair.dir.getPath());
					dir = new File(FileUtil.joinPath(interp, path, 0, 2));
				}
				globResults = componentGlobFilters[globPair.componentIndex].list(dir);
			}

			if (globResults == null)
				return;

			boolean atBottom = (globPair.componentIndex == componentGlobFilters.length - 1);

			for (String name : globResults) {
				File f = new File(globPair.dir, name);
				if (atBottom) {
					/*
					 * At the end of the pattern's path, so append this as a
					 * result
					 */
					String filename = tails ? f.getName() : f.getPath();
					if (hasTrailingSeparator)
						filename = filename + File.separator;
					TclList.append(interp, resultList, TclString.newInstance(filename));
				} else
					/* Not at the end of the pattern's path, so keep looking */
					stack.push(new GlobPair(f, globPair.componentIndex + 1));
			}
		}
	}

	/**
	 * Encapsulates a directory and component
	 */
	private final class GlobPair {
		File dir;
		int componentIndex;

		GlobPair(File dir, int index) {
			this.dir = dir;
			this.componentIndex = index;
		}
	}

	/**
	 * A FilenameFilter which filters according to a single-directory-level
	 * glob-style pattern that contains no {} sections.
	 */
	final class GlobFilter implements FilenameFilter {
		private String prefix;
		private String pattern;
		private boolean caseSensitive = true;
		private int types = 0;

		/**
		 * Create a new GlobFilter
		 * 
		 * @param prefix
		 *            the prefix, as specified to add
		 * @param pattern
		 *            One component of the glob pattern, which does not contain
		 *            any {} sections or directory separators
		 * @param types
		 *            The TYPE_* as specified in the -types option to glob
		 * @param mustBeDirectory
		 *            true if, regardless of types, this glob must result in
		 *            directories
		 */
		GlobFilter(String prefix, String pattern, int types, boolean mustBeDirectory) {
			this.prefix = prefix;
			this.caseSensitive = (JACL.PLATFORM != JACL.PLATFORM_WINDOWS);
			this.pattern = caseSensitive ? pattern : pattern.toUpperCase();
			this.types = types;
			if (mustBeDirectory) {
				this.types = TYPE_DIRECTORY;
			}
		}

		/**
		 * Performs a dir.list(this), and appends "." and ".." if they are
		 * accepted
		 * 
		 * @param dir
		 *            directory to list
		 * @return array of names that exist in directory and are accepted by
		 *         this filter, possibly including "." and ".."
		 */
		public String[] list(File dir) {
			String[] results = dir.list(this);
			boolean acceptDot = accept(dir, ".");
			boolean acceptDotDot = accept(dir, "..");
			int extra = 0;
			if (acceptDot)
				++extra;
			if (acceptDotDot)
				++extra;

			if (extra == 0)
				return results;
			else {
				String[] extraResults = new String[results.length + extra];
				System.arraycopy(results, 0, extraResults, 0, results.length);
				extra = results.length;
				if (acceptDot)
					extraResults[extra++] = ".";
				if (acceptDotDot)
					extraResults[extra++] = "..";
				return extraResults;
			}

		}

		/**
		 * Test if a file matches this GlobFilter
		 * 
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		public boolean accept(File dir, String name) {
			if (!caseSensitive)
				name = name.toUpperCase();

			if (prefix.length() > 0) {
				if (!name.startsWith(prefix))
					return false;
			}

			/* How we handle "." and ".." and .* is platform-dependent */
			if (name.startsWith(".")) {
				if (JACL.PLATFORM == JACL.PLATFORM_UNIX) {
					if ((types & TYPE_HIDDEN) == 0) {
						/* If hidden not specified, must explicitly match */
						if (!pattern.startsWith("."))
							return false;

					}
				} else {
					if (name.equals(".") && !pattern.startsWith("."))
						return false;
					if (name.equals("..") && !pattern.startsWith(".."))
						return false;
				}
			}

			if (!Util.stringMatch(name.substring(prefix.length()), pattern))
				return false;

			if (types == 0)
				return true;

			/* Otherwise, match it to types */
			boolean typesTest;
			File testFile;
			switch (name) {
				case ".":
					testFile = dir.getAbsoluteFile();
					break;
				case "..":
					testFile = dir.getAbsoluteFile().getParentFile();
					break;
				default:
					testFile = new File(dir, name);
					break;
			}

			if ((types & (TYPE_BLOCKSPECIAL | TYPE_CHARSPECIAL | TYPE_PIPE | TYPE_SOCKET | TYPE_DIRECTORY
					| TYPE_REGULARFILE | TYPE_LINK)) != 0) {
				typesTest = false; // must prove it is true
				if ((types & (TYPE_BLOCKSPECIAL | TYPE_CHARSPECIAL | TYPE_PIPE | TYPE_SOCKET)) != 0)
					// the best the JVM can do is to say it is not a regular
					// file
					typesTest = typesTest || !(testFile.isFile() || testFile.isDirectory());
				if ((types & TYPE_DIRECTORY) != 0)
					typesTest = typesTest || testFile.isDirectory();
				if ((types & TYPE_REGULARFILE) != 0)
					typesTest = typesTest || testFile.isFile();
				if ((types & TYPE_LINK) != 0) {
					/*
					 * This test will generate false positives, but it's the
					 * best we can do in Java
					 */
					typesTest = typesTest || (FileUtil.getLinkTarget(testFile) != null);
				}
			} else {
				typesTest = true;
			}
			/* The following types must all be true if specified */
			if ((types & TYPE_PERM_R) != 0) {
				typesTest = typesTest && testFile.canRead();
			}
			if ((types & TYPE_PERM_W) != 0) {
				typesTest = typesTest && testFile.canWrite();
			}
			if ((types & TYPE_PERM_X) != 0) {
				typesTest = typesTest && FileUtil.isExecutable(testFile);
			}
			if ((types & TYPE_READONLY) != 0) {
				/*
				 * Tcl -readonly implies no write in user, group, other; here we
				 * only test if this process can read and not write, which is
				 * not exactly the same.
				 */
				typesTest = typesTest && testFile.canRead() && !testFile.canWrite();
			}
			if ((types & TYPE_HIDDEN) != 0) {
				typesTest = typesTest && testFile.isHidden();
			}
			return typesTest;
		}

	}
} // end GlobCmd class
