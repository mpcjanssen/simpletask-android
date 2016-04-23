/*
 * AppendCmd.java --
 *
 *	Implements the built-in "append" Tcl command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: AppendCmd.java,v 1.2 1999/07/28 01:59:49 mo Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/*
 * This class implements the built-in "append" command in Tcl.
 */

public class AppendCmd implements Command {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "append" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Argument list.
			throws TclException // A standard Tcl exception.
	{
		TclObject varValue = null;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"varName ?value value ...?");
		} else if (objv.length == 2) {
			interp.setResult(interp.getVar(objv[1], 0));
		} else {
			for (int i = 2; i < objv.length; i++) {
				varValue = interp.setVar(objv[1], objv[i], TCL.APPEND_VALUE);
			}

			if (varValue != null) {
				interp.setResult(varValue);
			} else {
				interp.resetResult();
			}
		}
	}

} // end AppendCmd

