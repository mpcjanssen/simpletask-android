/*
 * ReturnCmd.java --
 *
 *	This file implements the Tcl "return" command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ReturnCmd.java,v 1.1.1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclInteger;
import tcl.lang.TclObject;

/*
 * This class implements the built-in "return" command in Tcl.
 */

public class ReturnCmd implements Command {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "return" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject argv[]) // Argument list.
			throws TclException // A standard Tcl exception.
	{
		interp.errorCode = null;
		interp.errorInfo = null;
		int returnCode, i;

		/*
		 * Note: returnCode is the value given by the -code option. Don't
		 * confuse this value with the compCode variable of the TclException
		 * thrown by this method, which is always TCL.RETURN.
		 */

		returnCode = TCL.OK;
		for (i = 1; i < argv.length - 1; i += 2) {
			if (argv[i].toString().equals("-code")) {
				if (argv[i + 1].toString().equals("ok")) {
					returnCode = TCL.OK;
				} else if (argv[i + 1].toString().equals("error")) {
					returnCode = TCL.ERROR;
				} else if (argv[i + 1].toString().equals("return")) {
					returnCode = TCL.RETURN;
				} else if (argv[i + 1].toString().equals("break")) {
					returnCode = TCL.BREAK;
				} else if (argv[i + 1].toString().equals("continue")) {
					returnCode = TCL.CONTINUE;
				} else {
					try {
						returnCode = TclInteger.getInt(interp, argv[i + 1]);
					} catch (TclException e) {
						throw new TclException(interp, "bad completion code \""
								+ argv[i + 1]
								+ "\": must be ok, error, return, break, "
								+ "continue, or an integer");
					}
				}
			} else if (argv[i].toString().equals("-errorcode")) {
				interp.errorCode = argv[i + 1].toString();
			} else if (argv[i].toString().equals("-errorinfo")) {
				interp.errorInfo = argv[i + 1].toString();
			} else {
				throw new TclException(interp, "bad option \"" + argv[i]
						+ "\": must be -code, -errorcode, or -errorinfo");
			}
		}
		if (i != argv.length) {
			interp.setResult(argv[argv.length - 1]);
		}

		interp.returnCode = returnCode;
		throw new TclException(TCL.RETURN);
	}

} // end ReturnCmd

