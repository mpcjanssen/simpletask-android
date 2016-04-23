/*
 * IncrCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997-1998 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: IncrCmd.java,v 1.3 2006/01/11 21:24:50 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclInteger;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Var;

/**
 * This class implements the built-in "incr" command in Tcl.
 */
public class IncrCmd implements Command {
	/**
	 * This procedure is invoked to process the "incr" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * @exception TclException
	 *                if wrong # of args or increment is not an integer.
	 */

	public void cmdProc(Interp interp, TclObject objv[]) throws TclException {
		long incrAmount;
		TclObject newValue;

		if ((objv.length != 2) && (objv.length != 3)) {
			throw new TclNumArgsException(interp, 1, objv,
					"varName ?increment?");
		}

		// Calculate the amount to increment by.

		if (objv.length == 2) {
			incrAmount = 1;
		} else {
			try {
				incrAmount = TclInteger.getLong(interp, objv[2]);
			} catch (TclException e) {
				interp.addErrorInfo("\n    (reading increment)");
				throw e;
			}
		}

		// Increment the variable's value.

		newValue = Var.incrVar(interp, objv[1].toString(), null, incrAmount,
				TCL.LEAVE_ERR_MSG);

		// FIXME: we need to look at this exception throwing problem again
		/*
		 * if (newValue == null) { return TCL_ERROR; }
		 */

		// Set the interpreter's object result to refer to the variable's new
		// value object.

		interp.setResult(newValue);
		return;
	}
}
