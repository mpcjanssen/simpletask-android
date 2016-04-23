/*
 * UpvarCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: UpvarCmd.java,v 1.4 2006/03/15 23:07:22 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.CallFrame;
import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Var;

/**
 * This class implements the built-in "upvar" command in Tcl.
 */

public class UpvarCmd implements Command {
	/**
	 * Tcl_UpvarObjCmd -> UpvarCmd.cmdProc
	 * 
	 * This procedure is invoked to process the "upvar" Tcl command. See the
	 * user documentation for details on what it does.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		CallFrame frame;
		String frameSpec, otherVarName, myVarName;
		int p;
		int objc = objv.length, objv_index;
		int result;

		if (objv.length < 3) {
			throw new TclNumArgsException(interp, 1, objv,
					"?level? otherVar localVar ?otherVar localVar ...?");
		}

		// Find the call frame containing each of the "other variables" to be
		// linked to.

		frameSpec = objv[1].toString();
		// Java does not support passing a reference by refernece so use an
		// array
		CallFrame[] frameArr = new CallFrame[1];
		result = CallFrame.getFrame(interp, frameSpec, frameArr);
		frame = frameArr[0];
		objc -= result + 1;
		if ((objc & 1) != 0) {
			throw new TclNumArgsException(interp, 1, objv,
					"?level? otherVar localVar ?otherVar localVar ...?");
		}
		objv_index = result + 1;

		// Iterate over each (other variable, local variable) pair.
		// Divide the other variable name into two parts, then call
		// MakeUpvar to do all the work of linking it to the local variable.

		for (; objc > 0; objc -= 2, objv_index += 2) {
			myVarName = objv[objv_index + 1].toString();
			otherVarName = objv[objv_index].toString();

			int otherLength = otherVarName.length();
			p = otherVarName.indexOf('(');
			if ((p != -1) && (otherVarName.charAt(otherLength - 1) == ')')) {
				// This is an array variable name
				Var.makeUpvar(interp, frame, otherVarName.substring(0, p),
						otherVarName.substring(p + 1, otherLength - 1), 0,
						myVarName, 0, -1);
			} else {
				// This is a scalar variable name
				Var.makeUpvar(interp, frame, otherVarName, null, 0, myVarName,
						0, -1);
			}
		}
		interp.resetResult();
		return;
	}
}
