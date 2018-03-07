/*
 * LlengthCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LlengthCmd.java,v 1.2 2005/10/07 06:50:09 mdejong Exp $
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
 * This class implements the built-in "llength" command in Tcl.
 */

public class LlengthCmd implements Command {
	/**
	 * See Tcl user documentation for details.
	 * 
	 * @exception TclException
	 *                If incorrect number of arguments.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length != 2) {
			throw new TclNumArgsException(interp, 1, argv, "list");
		}
		interp.setResult(TclList.getLength(interp, argv[1]));
	}
}
