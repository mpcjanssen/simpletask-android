/*
 * SubstCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SubstCmd.java,v 1.5 2007/06/07 02:35:26 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.BackSlashResult;
import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.ParseResult;
import tcl.lang.Parser;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "subst" command in Tcl.
 */

public class SubstCmd implements Command {
	static final private String validCmds[] = { "-nobackslashes", "-nocommands", "-novariables" };

	static final int OPT_NOBACKSLASHES = 0;
	static final int OPT_NOCOMMANDS = 1;
	static final int OPT_NOVARS = 2;

	/**
	 * This method is invoked to process the "subst" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 * @exception TclException
	 *                if wrong # of args or invalid argument(s).
	 */
	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		int currentObjIndex, len, i;
		int objc = argv.length - 1;
		boolean doBackslashes = true;
		boolean doCmds = true;
		boolean doVars = true;
		StringBuffer result = new StringBuffer();
		String s;
		char c;

		if (objc == 0) {
			throw new TclNumArgsException(interp, 1, argv, "?-nobackslashes? ?-nocommands? ?-novariables? string");
		}

		for (currentObjIndex = 1; currentObjIndex < objc; currentObjIndex++) {
			int opt = TclIndex.get(interp, argv[currentObjIndex], validCmds, "switch", 0);
			switch (opt) {
			case OPT_NOBACKSLASHES:
				doBackslashes = false;
				break;
			case OPT_NOCOMMANDS:
				doCmds = false;
				break;
			case OPT_NOVARS:
				doVars = false;
				break;
			default:
				throw new TclException(interp, "SubstCmd.cmdProc: bad option " + opt + " index to cmds");
			}
		}

		/*
		 * Scan through the string one character at a time, performing command,
		 * variable, and backslash substitutions.
		 */
		s = argv[currentObjIndex].toString();
		len = s.length();
		i = 0;

		while (i < len) {
			c = s.charAt(i);

			if ((c == '[') && doCmds) {
				/*
				 * eval the command in brackets
				 */
				interp.evalFlags = Parser.TCL_BRACKET_TERM;
				try {
					interp.eval(s.substring(i + 1, len));
					i = doCmdOrVarSub(null, s, i, interp.getResult().toString(), result, interp);
				} catch (TclException e) {
					i = doCmdOrVarSub(e, s, i, interp.getResult().toString(), result, interp);
				}

			} else if ((c == '$') && doVars) {
				/*
				 * Substitute the variable
				 */
				ParseResult vres = null;
				try {
					vres = Parser.parseVar(interp, s.substring(i, len));
					/*
					 * don't use doCmdOrVarSub(), because it only handles
					 * $var(array_value). Can't get break, continue or return
					 * exception in any other variable form
					 */
					result.append(vres.value.toString()); // var sub with no
															// exception
					i += vres.nextIndex;
				} catch (TclException e) {
					i = doCmdOrVarSub(e, s, i, vres == null ? "" : vres.value.toString(), result, interp);
				} finally {
					if (vres != null) {
						vres.release();
					}
				}

			} else if ((c == '\\') && doBackslashes) {
				/*
				 * Substitute for backslash
				 */
				BackSlashResult bs = Interp.backslash(s, i, len);
				i = bs.nextIndex;
				if (bs.isWordSep) {
					break;
				} else {
					result.append(bs.c);
				}

				/*
				 * Just copy the character
				 */
			} else {
				result.append(c);
				i++;
			}
		}

		interp.setResult(result.toString());
	}

	/**
	 * Catch any break, continue or return exception and stuff the appropriate
	 * substitution string into result
	 * 
	 * @param e
	 *            The TclException that occurred
	 * @param originalString
	 *            The complete original string passed to subst
	 * @param offset
	 *            The offset of the '[' or '$' being substituted
	 * @param replacementSubstring
	 *            The value of the replacement
	 * @param result
	 *            The StringBuffer containing substitutions that will be
	 *            returned from subst
	 * @return Index of next character in 'originalString' to process
	 * @throws TclException
	 *             If the exception was an error, rather than a break, continue
	 *             or return
	 */
	private static int doCmdOrVarSub(TclException e, String originalString, int offset, String replacementSubstring,
			StringBuffer result, Interp interp) throws TclException {

		if (e != null && e.getCompletionCode() == TCL.ERROR) {
			throw e; // don't catch errors
		}
		int bracketLevel = 0;
		int braceLevel = 0;
		int parenLevel = 0;
		int end = offset;
		boolean done = false;

		while (!done && end < originalString.length()) {
			switch (originalString.charAt(end)) {
			case '\\':
				++end; // skip past
				break;
			case '[':
				if (braceLevel == 0)
					++bracketLevel;
				break;
			case ']':
				if (braceLevel == 0) {
					--bracketLevel;
					/* if processing command sub, stop at close bracket */
					if (bracketLevel == 0 && originalString.charAt(offset) == '[') {
						done = true;
					}
				}
				break;
			case '{':
				++braceLevel;
				break;
			case '}':
				--braceLevel;
				break;
			case '(':
				if (braceLevel == 0)
					++parenLevel;
				break;
			case ')':
				if (braceLevel == 0) {
					--parenLevel;
					/* if processing variable (array) sub, stop at close paren */
					if (parenLevel == 0 && originalString.charAt(offset) == '$') {
						done = true;
					}
				}
				break;
			default:
			}
			++end; // go to next character
		}

		/*
		 * Now do the appropriate substitution, depending on the Exception type
		 */
		int ccode = e == null ? TCL.OK : e.getCompletionCode();
		int newOffset = end;
		switch (ccode) {
		case TCL.OK:
			result.append(replacementSubstring);
			break;
		case TCL.RETURN:
			// if we got a return, append the return value, which is in
			// interp.getResult()
			result.append(interp.getResult().toString());
			break;
		case TCL.CONTINUE:
			// empty substitution
			break;
		case TCL.BREAK:
			// empty substitution, and end substituting
			newOffset = originalString.length();
			break;
		}

		if (!done && end >= originalString.length()) {
			if (braceLevel > 0) {
				throw new TclException(interp, "missing close-brace");
			}
			if (bracketLevel > 0) {
				throw new TclException(interp, "missing close-bracket");
			}
			if (parenLevel > 0) {
				throw new TclException(interp, "missing close-parenthesis");
			}
		}

		/*
		 * If there are un-parsable commands after a continue or return
		 * in a command substitution, it is copied literally into result
		 */
		int trailingIndex = offset + interp.termOffset + 1;
		if (ccode != TCL.OK && ccode != TCL.BREAK && originalString.charAt(offset) == '[' 
			&& originalString.charAt(trailingIndex - 1) == ';') {

			// get past white space
			while (trailingIndex < end && Character.isWhitespace(originalString.charAt(trailingIndex))) {
				++trailingIndex;
			}
			
			if (! Parser.isParseableScript(originalString.substring(trailingIndex,end-1),false)) {
				result.append(originalString.substring(trailingIndex,end));
			}
		}
		return newOffset;
	}
}
