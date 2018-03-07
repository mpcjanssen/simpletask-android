/*
 * SwitchCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SwitchCmd.java,v 1.3 2005/11/04 21:02:14 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.Util;

/**
 * This class implements the built-in "switch" command in Tcl.
 */

public class SwitchCmd implements Command {

	static final private String validCmds[] = { "-exact", "-glob", "-regexp",
			"--" };
	private static final int EXACT = 0;
	private static final int GLOB = 1;
	private static final int REGEXP = 2;
	private static final int LAST = 3;

	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked to process the "switch" Tcl statement. See the
	 * user documentation for details on what it does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * 
	 * 
	 * --------------------------------------------------------------------------
	 * ---
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Arguments to "switch" statement.
			throws TclException {
		int i, mode, pbStart, pbOffset;
		boolean matched, foundmode, splitObjs;
		String string;
		TclObject[] switchObjv = null;

		mode = EXACT;
		foundmode = false;
		for (i = 1; i < objv.length; i++) {
			if (!objv[i].toString().startsWith("-")) {
				break;
			}
			int opt = TclIndex.get(interp, objv[i], validCmds, "option", 0);
			if (opt == LAST) {
				i++;
				break;
			} else if (opt > LAST) {
				throw new TclException(interp, "SwitchCmd.cmdProc: bad option "
						+ opt + " index to validCmds");
			} else {
				if (foundmode) {
					throw new TclException(interp, "bad option \"" + objv[i]
							+ "\": " + validCmds[mode]
							+ " option already found");
				}
				foundmode = true;
				mode = opt;
			}
		}

		if (objv.length - i < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"?switches? string pattern body ... ?default body?");
		}
		string = objv[i].toString();
		i++;

		// If all of the pattern/command pairs are lumped into a single
		// argument, split them out again.

		splitObjs = false;
		if (objv.length - i == 1) {
			switchObjv = TclList.getElements(interp, objv[i]);

			// Ensure that the list is non-empty.

			if (switchObjv.length == 0) {
				throw new TclNumArgsException(interp, 1, objv,
						"?switches? string {pattern body ... ?default body?}");
			}
			pbStart = 0;
			splitObjs = true;
		} else {
			switchObjv = objv;
			pbStart = i;
		}

		if (((switchObjv.length - pbStart) % 2) != 0) {
			interp.resetResult();

			// Check if this can be due to a badly placed comment
			// in the switch block.
			//
			// The following is an heuristic to detect the infamous
			// "comment in switch" error: just check if a pattern
			// begins with '#'.

			if (splitObjs) {
				for (int off = pbStart; off < switchObjv.length; off += 2) {
					if (switchObjv[off].toString().startsWith("#")) {
						throw new TclException(
								interp,
								"extra switch pattern with no body"
										+ ", this may be due to a "
										+ "comment incorrectly placed outside of a "
										+ "switch body - see the \"switch\" "
										+ "documentation");
					}
				}
			}
			throw new TclException(interp, "extra switch pattern with no body");
		}

		// Find pattern that matches string, return offset from first pattern

		pbOffset = SwitchCmd.getBodyOffset(interp, switchObjv, pbStart, string,
				mode);

		if (pbOffset != -1) {
			try {
				interp.eval(switchObjv[pbStart + pbOffset], 0);
				return;
			} catch (TclException e) {
				// Figure out which pattern matched the body
				int pIndex;
				for (pIndex = pbStart + pbOffset - 1; pIndex >= pbStart; pIndex -= 2) {
					if (!switchObjv[pIndex].toString().equals("-")) {
						break;
					}
				}

				if (e.getCompletionCode() == TCL.ERROR) {
					interp.addErrorInfo("\n    (\"" + switchObjv[pIndex]
							+ "\" arm line " + interp.errorLine + ")");
				}
				throw e;
			}
		}

		// Nothing matched: return nothing.
	}

	// Util method that accepts switch command arguments and
	// returns an index indicating an offset from the first
	// pattern element for the script that should be executed.
	// This method assumes that a null body element is valid
	// and returns the index.

	public static int getBodyOffset(Interp interp, TclObject[] switchObjv, // array
			// that
			// contains
			// pattern/body
			// strings
			int pbStart, // index of first pattern in switchObjv
			String string, // String argument
			int mode) // Match mode
			throws TclException {
		int body;
		boolean matched;
		final int slen = switchObjv.length;

		// Complain if the last body is a continuation. Note that this
		// check assumes that the list is non-empty!

		if (switchObjv[slen - 1] != null
				&& switchObjv[slen - 1].toString().equals("-")) {
			interp.resetResult();
			throw new TclException(interp, "no body specified for pattern \""
					+ switchObjv[slen - 2].toString() + "\"");
		}

		for (int i = pbStart; i < slen; i += 2) {
			// See if the pattern matches the string.

			matched = false;
			String pattern = switchObjv[i].toString();

			if ((i == slen - 2) && pattern.equals("default")) {
				matched = true;
			} else {
				switch (mode) {
				case EXACT:
					matched = string.equals(pattern);
					break;
				case GLOB:
					matched = Util.stringMatch(string, pattern);
					break;
				case REGEXP:
					matched = Util.regExpMatch(interp, string, switchObjv[i]);
					break;
				}
			}
			if (!matched) {
				continue;
			}

			// We've got a match. Find a body to execute, skipping bodies
			// that are "-".

			for (body = i + 1;; body += 2) {
				if (body >= slen) {
					// This shouldn't happen since we've checked that the
					// last body is not a continuation...
					throw new TclRuntimeError(
							"fall-out when searching for body to match pattern");
				}

				if (switchObjv[body] == null
						|| !switchObjv[body].toString().equals("-")) {
					break;
				}
			}

			// Return offset of match (from first pattern)
			return body - pbStart;
		}

		// Nothing matched: return nothing.
		return -1;
	}

} // end SwitchCmd

