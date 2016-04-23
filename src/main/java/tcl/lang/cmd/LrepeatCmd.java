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
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "lrepeat" command in Tcl.
 */

public class LrepeatCmd implements Command {

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length < 3) {
			throw new TclNumArgsException(interp, 1, argv, "positiveCount value ?value ...?");
		}
		
		int count = TclInteger.getInt(interp, argv[1]);
		if (count < 1) {
			throw new TclException(interp, "must have a count of at least 1");
		}
		
		TclObject result = TclList.newInstance();
		
		for (int i = 0; i < count; i++) {
			for (int j = 2; j < argv.length; j++) {
				TclList.append(interp, result, argv[j]);
			}
		}
		interp.setResult(result);
	}
}
