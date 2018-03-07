/*
 * UplevelCmd.java --
 *
 *	Implements the "uplevel" command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: UplevelCmd.java,v 1.5 2005/11/16 21:08:11 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.CallFrame;
import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Util;

/*
 * This class implements the built-in "uplevel" command in Tcl.
 */

public class UplevelCmd implements Command {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_UplevelObjCmd -> UplevelCmd.cmdProc
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "uplevel" Tcl command. See the user documentation for details on what it
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
		String optLevel;
		int result;
		CallFrame savedVarFrame, frame;
		int objc = objv.length;
		int objv_index;
		TclObject cmd;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"?level? command ?arg ...?");
		}

		// Find the level to use for executing the command.

		optLevel = objv[1].toString();
		// Java does not support passing a reference by refernece so use an
		// array
		CallFrame[] frameArr = new CallFrame[1];
		result = CallFrame.getFrame(interp, optLevel, frameArr);
		frame = frameArr[0];

		objc -= (result + 1);
		if (objc == 0) {
			throw new TclNumArgsException(interp, 1, objv,
					"?level? command ?arg ...?");
		}
		objv_index = (result + 1);

		// Modify the interpreter state to execute in the given frame.

		savedVarFrame = interp.varFrame;
		interp.varFrame = frame;

		// Execute the residual arguments as a command.

		if (objc == 1) {
			cmd = objv[objv_index];
		} else {
			cmd = Util.concat(objv_index, objv.length - 1, objv);
		}

		try {
			interp.eval(cmd, 0);
		} catch (TclException e) {
			if (e.getCompletionCode() == TCL.ERROR) {
				interp.addErrorInfo("\n    (\"uplevel\" body line "
						+ interp.errorLine + ")");
			}
			throw e;
		} finally {
			interp.varFrame = savedVarFrame;
		}
	}

} // end UplevelCmd
