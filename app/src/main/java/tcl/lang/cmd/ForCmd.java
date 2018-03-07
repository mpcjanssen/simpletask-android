/*
 * ForCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ForCmd.java,v 1.2 2005/11/16 21:19:13 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "for" command in Tcl.
 */

public class ForCmd implements Command {
	/*
	 * This procedure is invoked to process the "for" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * @param interp the current interpreter.
	 * 
	 * @param argv command arguments.
	 * 
	 * @exception TclException if script causes error.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length != 5) {
			throw new TclNumArgsException(interp, 1, argv,
					"start test next command");
		}

		TclObject start = argv[1];
		String test = argv[2].toString();
		TclObject next = argv[3];
		TclObject command = argv[4];

		boolean done = false;
		try {
			interp.eval(start, 0);
		} catch (TclException e) {
			interp.addErrorInfo("\n    (\"for\" initial command)");
			throw e;
		}

		while (!done) {
			if (!interp.expr.evalBoolean(interp, test)) {
				break;
			}

			try {
				interp.eval(command, 0);
			} catch (TclException e) {
				switch (e.getCompletionCode()) {
				case TCL.BREAK:
					done = true;
					break;

				case TCL.CONTINUE:
					break;

				case TCL.ERROR:
					interp.addErrorInfo("\n    (\"for\" body line "
							+ interp.errorLine + ")");
					throw e;

				default:
					throw e;
				}
			}

			if (!done) {
				try {
					interp.eval(next, 0);
				} catch (TclException e) {
					switch (e.getCompletionCode()) {
					case TCL.BREAK:
						done = true;
						break;

					case TCL.ERROR:
						interp.addErrorInfo("\n    (\"for\" loop-end command)");
						throw e;

					default:
						throw e;
					}
				}
			}
		}

		interp.resetResult();
	}
}
