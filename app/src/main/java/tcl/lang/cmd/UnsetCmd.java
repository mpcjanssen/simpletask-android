/*
 * UnsetCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: UnsetCmd.java,v 1.3 2009/07/08 15:49:25 rszulgo Exp $
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
 * This class implements the built-in "unset" command in Tcl.
 */

public class UnsetCmd implements Command {
	/**
	 * Tcl_UnsetObjCmd -> UnsetCmd.cmdProc
	 * 
	 * Unsets Tcl variable (s). See Tcl user documentation * for details.
	 * 
	 * @exception TclException
	 *                If tries to unset a variable that does not exist.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		int firstArg = 1;
		String opt;
		boolean noComplain = false;

		if (objv.length < 2) {
			return;
		}

		/*
		 * Simple, restrictive argument parsing. The only options are -- and
		 * -nocomplain (which must come first and be given exactly to be an
		 * option).
		 */

		opt = objv[firstArg].toString();

		if (opt.startsWith("-")) {
			if ("-nocomplain".equals(opt)) {
				noComplain = true;
				if (++firstArg < objv.length) {
					opt = objv[firstArg].toString();
				}
			}
			if ("--".equals(opt)) {
				firstArg++;
			}
		}
		for (int i = firstArg; i < objv.length; i++) {
			try {
				interp.unsetVar(objv[i], noComplain ? 0 : TCL.LEAVE_ERR_MSG);
			} catch (TclException e) {
				if (!noComplain) {
					throw e;
				} else {
					interp.resetResult();
				}
			}
		}

		return;
	}
}
