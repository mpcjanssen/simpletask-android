/*
 * GlobalCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: GlobalCmd.java,v 1.5 2006/03/15 23:07:22 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Var;

/**
 * This class implements the built-in "global" command in Tcl.
 */

public class GlobalCmd implements Command {
	/**
	 * See Tcl user documentation for details.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"varName ?varName ...?");
		}

		// If we are not executing inside a Tcl procedure, just return.

		if ((interp.varFrame == null) || !interp.varFrame.isProcCallFrame) {
			return;
		}

		for (int i = 1; i < objv.length; i++) {
			String varName = objv[i].toString();
			String varTail = NamespaceCmd.tail(varName);

			// Link to the variable "varName" in the global :: namespace.
			// A local link var named varTail is defined.

			Var.makeUpvar(interp, null, varName, null, TCL.GLOBAL_ONLY,
					varTail, 0, -1);
		}
	}
}
