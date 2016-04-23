/*
 * CaseCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: CaseCmd.java,v 1.2 1999/05/08 23:52:18 dejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Util;

/**
 * This class implements the built-in "case" command in Tcl.
 */

public class CaseCmd implements Command {
	/**
	 * Executes a "case" statement. See Tcl user documentation for details.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 * @exception TclException
	 *                If incorrect number of arguments.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length < 3) {
			throw new TclNumArgsException(interp, 1, argv,
					"string ?in? patList body ... ?default body?");
		}

		int i;
		int body;
		TclObject caseArgv[];
		String string;

		string = argv[1].toString();
		caseArgv = argv;
		body = -1;

		if (argv[2].toString().equals("in")) {
			i = 3;
		} else {
			i = 2;
		}

		/*
		 * If all of the pattern/command pairs are lumped into a single
		 * argument, split them out again.
		 */

		if (argv.length - i == 1) {
			caseArgv = TclList.getElements(interp, argv[i]);
			i = 0;
		}

		match_loop: {
			for (; i < caseArgv.length; i += 2) {
				int j;

				if (i == (caseArgv.length - 1)) {
					throw new TclException(interp,
							"extra case pattern with no body");
				}

				/*
				 * Check for special case of single pattern (no list) with no
				 * backslash sequences.
				 */

				String caseString = caseArgv[i].toString();
				int len = caseString.length();
				for (j = 0; j < len; j++) {
					char c = caseString.charAt(j);
					if (Character.isWhitespace(c) || (c == '\\')) {
						break;
					}
				}
				if (j == len) {
					if (caseString.equals("default")) {
						body = i + 1;
					}
					if (Util.stringMatch(string, caseString)) {
						body = i + 1;
						break match_loop;
					}
					continue;
				}

				/*
				 * Break up pattern lists, then check each of the patterns in
				 * the list.
				 */

				int numPats = TclList.getLength(interp, caseArgv[i]);
				for (j = 0; j < numPats; j++) {
					if (Util.stringMatch(string, TclList.index(interp,
							caseArgv[i], j).toString())) {
						body = i + 1;
						break match_loop;
					}
				}
			}
		}

		if (body != -1) {
			try {
				interp.eval(caseArgv[body], 0);
			} catch (TclException e) {
				if (e.getCompletionCode() == TCL.ERROR) {
					interp.addErrorInfo("\n    (\"" + caseArgv[body - 1]
							+ "\" arm line " + interp.errorLine + ")");
				}
				throw e;
			}
		}
	}
}
