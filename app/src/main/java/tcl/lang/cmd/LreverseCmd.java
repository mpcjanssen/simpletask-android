/*
 * LreverseCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "lreverse" command in Tcl.
 */

public class LreverseCmd implements Command {

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length != 2) {
			throw new TclNumArgsException(interp, 1, argv, "list");
		}
		
		TclObject[] elems = TclList.getElements(interp, argv[1]);
		
		if (elems.length == 0) {
			interp.setResult(argv[1]);
		} else {
			TclObject result = TclList.newInstance();
			for (int i = 0, j = elems.length-1; i < elems.length; i++, j--) {
				TclList.append(interp, result, elems[j]);
			}
			interp.setResult(result);
		}

	}
}
