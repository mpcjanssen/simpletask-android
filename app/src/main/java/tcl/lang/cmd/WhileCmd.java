/*
 * WhileCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: WhileCmd.java,v 1.1.1.1 1998/10/14 21:09:20 cvsadmin Exp $
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
 * This class implements the built-in "while" command in Tcl.
 */

public class WhileCmd implements Command {
	/**
	 * This procedure is invoked to process the "while" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 * @exception TclException
	 *                if script causes error.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length != 3) {
			throw new TclNumArgsException(interp, 1, argv, "test command");
		}
		String test = argv[1].toString();
		TclObject command = argv[2];

		loop: {
			while (true) {
				boolean exprTest;
				try {
					exprTest = interp.expr.evalBoolean(interp, test);
					if (! exprTest) break loop;
				} catch (TclException e1) {
					if (e1.getCompletionCode()==TCL.ERROR) {
						if (interp.errInProgress) interp.addErrorInfo("\n    (\"while\" test expression)");
					}
					throw e1;
				}
				try {
					interp.eval(command, 0);
				} catch (TclException e) {
					switch (e.getCompletionCode()) {
					case TCL.BREAK:
						break loop;

					case TCL.CONTINUE:
						continue;

					case TCL.ERROR:
						interp.addErrorInfo("\n    (\"while\" body line "
								+ interp.errorLine + ")");
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
