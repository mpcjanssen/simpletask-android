/*
 * SetCmd.java --
 *
 *	Implements the built-in "set" Tcl command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SetCmd.java,v 1.2 1999/05/09 01:23:19 dejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/*
 * This class implements the built-in "set" command in Tcl.
 */

public class SetCmd implements Command {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "set" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject argv[]) // Argument list.
			throws TclException // A standard Tcl exception.
	{
		final boolean debug = false;

		if (argv.length == 2) {
			if (debug) {
				System.out.println("getting value of \"" + argv[1].toString()
						+ "\"");
				System.out.flush();
			}
			interp.setResult(interp.getVar(argv[1], 0));
		} else if (argv.length == 3) {
			if (debug) {
				System.out.println("setting value of \"" + argv[1].toString()
						+ "\" to \"" + argv[2].toString() + "\"");
				System.out.flush();
			}
			interp.setResult(interp.setVar(argv[1], argv[2], 0));
		} else {
			throw new TclNumArgsException(interp, 1, argv, "varName ?newValue?");
		}
	}

} // end SetCmd

