/*
 * CatchCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: CatchCmd.java,v 1.3 2005/10/07 06:50:09 mdejong Exp $
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
 * This class implements the built-in "catch" command in Tcl.
 */

public class CatchCmd implements Command {
	/**
	 * This procedure is invoked to process the "catch" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 * @exception TclException
	 *                if wrong number of arguments.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length != 2 && argv.length != 3) {
			throw new TclNumArgsException(interp, 1, argv, "command ?varName?");
		}

		TclObject result;
		int code = TCL.OK;

		try {
			interp.eval(argv[1], 0);
		} catch (TclException e) {
			code = e.getCompletionCode();
		}

		result = interp.getResult();

		if (argv.length == 3) {
			try {
				interp.setVar(argv[2], result, 0);
			} catch (TclException e) {
				throw new TclException(interp,
						"couldn't save command result in variable");
			}
		}

		interp.resetResult();
		interp.setResult(code);
	}
}
