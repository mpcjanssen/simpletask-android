/*
 * ErrorCmd.java --
 *
 *	Implements the "error" command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ErrorCmd.java,v 1.1.1.1 1998/10/14 21:09:19 cvsadmin Exp $
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
 * This class implements the built-in "error" command in Tcl.
 */

public class ErrorCmd implements Command {

	/**
	 * This procedure is invoked as part of the Command interface to process the
	 * "error" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject argv[]) // Argument list.
			throws TclException // A standard Tcl exception.
	{
		if (argv.length < 2 || argv.length > 4) {
			throw new TclNumArgsException(interp, 1, argv,
					"message ?errorInfo? ?errorCode?");
		}

		if (argv.length >= 3) {
			String errorInfo = argv[2].toString();

			if (!errorInfo.equals("")) {
				interp.addErrorInfo(errorInfo);
				interp.errAlreadyLogged = true;
			}
		}

		if (argv.length == 4) {
			boolean errAlreadyLogged = interp.errAlreadyLogged;
			interp.setErrorCode(argv[3]);
			interp.errAlreadyLogged = errAlreadyLogged; // in case errorCode var trace in effect
		}

		interp.setResult(argv[1]);
		throw new TclException(TCL.ERROR);
	}

} // end ErrorCmd

