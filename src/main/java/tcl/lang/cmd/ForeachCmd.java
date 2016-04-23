/*
 * ForeachCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ForeachCmd.java,v 1.5 2005/11/08 05:20:53 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * This class implements the built-in "Foreach" command in Tcl.
 */

public class ForeachCmd implements Command {
	/**
	 * Tcl_ForeachObjCmd -> ForeachCmd.cmdProc
	 * 
	 * This procedure is invoked to process the "foreach" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param objv
	 *            command arguments.
	 * @exception TclException
	 *                if script causes error.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length < 4 || (objv.length % 2) != 0) {
			throw new TclNumArgsException(interp, 1, objv,
					"varList list ?varList list ...? command");
		}

		// foreach {n1 n2} {1 2 3 4} {n3} {1 2} {puts $n1-$n2-$n3}
		// name[0] = {n1 n2} value[0] = {1 2 3 4}
		// name[1] = {n3} value[0] = {1 2}

		TclObject[] name = new TclObject[(objv.length - 2) / 2];
		TclObject[] value = new TclObject[(objv.length - 2) / 2];

		int c, i, j, base;
		int maxIter = 0;
		TclObject command = objv[objv.length - 1];
		boolean done = false;

		for (i = 0; i < objv.length - 2; i += 2) {
			int x = i / 2;
			name[x] = objv[i + 1];
			value[x] = objv[i + 2];

			int nSize = TclList.getLength(interp, name[x]);
			int vSize = TclList.getLength(interp, value[x]);

			if (nSize == 0) {
				throw new TclException(interp, "foreach varlist is empty");
			}

			int iter = (vSize + nSize - 1) / nSize;
			if (maxIter < iter) {
				maxIter = iter;
			}
		}

		for (c = 0; !done && c < maxIter; c++) {
			// Set up the variables

			for (i = 0; i < objv.length - 2; i += 2) {
				int x = i / 2;
				int nSize = TclList.getLength(interp, name[x]);
				base = nSize * c;
				for (j = 0; j < nSize; j++) {
					try {
						if (base + j >= TclList.getLength(interp, value[x])) {
							interp.setVar(TclList.index(interp, name[x], j),
									TclString.newInstance(""), 0);
						} else {
							interp.setVar(TclList.index(interp, name[x], j),
									TclList.index(interp, value[x], base + j),
									0);
						}
					} catch (TclException e) {
						throw new TclException(interp,
								"couldn't set loop variable: \""
										+ TclList.index(interp, name[x], j)
										+ "\"");
					}
				}
			}

			// Execute the script

			try {
				interp.eval(command, 0);
			} catch (TclException e) {
				switch (e.getCompletionCode()) {
				case TCL.BREAK:
					done = true;
					break;

				case TCL.CONTINUE:
					continue;

				case TCL.ERROR:
					interp.addErrorInfo("\n    (\"foreach\" body line "
							+ interp.errorLine + ")");
					throw e;

				default:
					throw e;
				}
			}
		}

		interp.resetResult();
	}
}
