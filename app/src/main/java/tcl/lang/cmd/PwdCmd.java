/*
 * PwdCmd.java
 *
 *	This file contains the Jacl implementation of the built-in Tcl "pwd"
 *	command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: PwdCmd.java,v 1.2 1999/05/09 01:12:14 dejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.JACL;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/*
 * This class implements the built-in "pwd" command in Tcl.
 */

public class PwdCmd implements Command {

	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked to process the "pwd" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * 
	 * 
	 * --------------------------------------------------------------------------
	 * ---
	 */

	public void cmdProc(Interp interp, // Current interp to eval the file cmd.
			TclObject argv[]) // Args passed to the file command.
			throws TclException {
		if (argv.length != 1) {
			throw new TclNumArgsException(interp, 1, argv, null);
		}

		// Get the name of the working dir.

		String dirName = interp.getWorkingDir().toString();

		// Java File Object methods use backslashes on Windows.
		// Convert them to forward slashes before returning the dirName to Tcl.

		if (JACL.PLATFORM == JACL.PLATFORM_WINDOWS) {
			dirName = dirName.replace('\\', '/');
		}

		interp.setResult(dirName);
	}

} // end PwdCmd class

