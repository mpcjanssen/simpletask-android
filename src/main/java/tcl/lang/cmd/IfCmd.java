/*
 * IfCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: IfCmd.java,v 1.3 2005/11/08 05:20:53 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclObject;

// This class implements the built-in "if" command in Tcl.

public class IfCmd implements Command {

	// See Tcl user documentation for details.
	// @exception TclException If incorrect number of arguments.

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		int i;
		boolean value = false;
		boolean executedBody= false;

		i = 1;
		while (true) {
			/*
			 * objv[i] is an expression to test, from either 'if' or 'elseif'
			 */

			if (i >= objv.length) {
				throw new TclException(interp,
						"wrong # args: no expression after \"" + objv[i - 1]
								+ "\" argument");
			}
			try {
				if (! executedBody)
					value = interp.expr.evalBoolean(interp, objv[i].toString());
			} catch (TclException e) {
				switch (e.getCompletionCode()) {
				case TCL.ERROR:
					interp.addErrorInfo("\n    (\"if\" test expression)");
					break;
				}
				throw e;
			}

			i++;

			/*
			 * objv[i] is either 'then' or the the body to execute
			 */
			if ((i < objv.length) && (objv[i].toString().equals("then"))) {
				i++;
			}
			
			/*
			 * objv[i] is the body to execute
			 */
			
			if (i >= objv.length) {
				throw new TclException(interp,
						"wrong # args: no script following \"" + objv[i - 1]
								+ "\" argument");
			}
			if (value && ! executedBody) {
				executedBody = true;
				try {
					interp.eval(objv[i], 0);
				} catch (TclException e) {
					switch (e.getCompletionCode()) {
					case TCL.ERROR:
						interp.addErrorInfo("\n    (\"if\" then script line "
								+ interp.errorLine + ")");
						break;
					}
					throw e;
				}
			} 
	
			i++;
			
			/*
			 * objv[i], if it exists, is either 'else' or 'elseif'.  If it doesn't exist,
			 * we are done with the if command.
			 */
			if (i >= objv.length) {
				if (! executedBody) interp.resetResult();
				return;
			}
			if (objv[i].toString().equals("elseif")) {
				i++;
				continue;
			}
			break;
		}

		/*
		 * objv[i] must be 'else' or junk
		 */
		if (objv[i].toString().equals("else")) {
			i++;
			if (i >= objv.length) {
				throw new TclException(interp,
						"wrong # args: no script following \"else\" argument");
			} else if (i != (objv.length - 1)) {
				throw new TclException(interp,
						"wrong # args: extra words after \"else\" clause in "
								+ "\"if\" command");
			}
		} else {
			// Not else, if there is more than 1 more argument
			// then generate an error.

			if (i != (objv.length - 1)) {
				throw new TclException(interp,
						"wrong # args: extra words after \"else\" clause in \"if\" command");
			}
		}
		try {
			if (! executedBody) {
				executedBody = true;
				interp.eval(objv[i], 0);
			}
		} catch (TclException e) {
			switch (e.getCompletionCode()) {
			case TCL.ERROR:
				interp.addErrorInfo("\n    (\"if\" else script line "
						+ interp.errorLine + ")");
				break;
			}
			throw e;
		}
	}
}
