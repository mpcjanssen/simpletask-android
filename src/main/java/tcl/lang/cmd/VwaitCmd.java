/*
 * VwaitCmd.java --
 *
 *	This file implements the Tcl "vwait" command.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: VwaitCmd.java,v 1.3 2005/10/07 06:50:09 mdejong Exp $
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Var;
import tcl.lang.VarTrace;
import tcl.lang.channel.FileEvent;

/*
 * This class implements the built-in "vwait" command in Tcl.
 */

public class VwaitCmd implements Command {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "vwait" Tcl command. See the user documentation for details on what it
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
		if (argv.length != 2) {
			throw new TclNumArgsException(interp, 1, argv, "name");
		}

		VwaitTrace trace = new VwaitTrace();
		Var.traceVar(interp, argv[1].toString(), null, TCL.GLOBAL_ONLY
				| TCL.TRACE_WRITES | TCL.TRACE_UNSETS, trace);

		/* Allow stdin file events to be fired, since we aren't reading stdin for commands now */
		boolean stdinUsed = FileEvent.setStdinUsedForCommandInput(false);
		int foundEvent = 1;
		while (!trace.done && (foundEvent != 0)) {
			foundEvent = interp.getNotifier().doOneEvent(TCL.ALL_EVENTS);
		}
		/* Reset stdin use flag */
		FileEvent.setStdinUsedForCommandInput(stdinUsed);

		Var.untraceVar(interp, argv[1].toString(), null, TCL.GLOBAL_ONLY
				| TCL.TRACE_WRITES | TCL.TRACE_UNSETS, trace);

		// Clear out the interpreter's result, since it may have been set
		// by event handlers.

		interp.resetResult();

		if (foundEvent == 0) {
			throw new TclException(interp, "can't wait for variable \""
					+ argv[1] + "\":  would wait forever");
		}
	}

} // end VwaitCmd

/*
 * This class handle variable traces for the "vwait" command.
 */

class VwaitTrace implements VarTrace {

	/*
	 * TraceCmd.cmdProc continuously watches this variable across calls to
	 * doOneEvent(). It returns immediately when done is set to true.
	 */

	boolean done = false;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * traceProc --
	 * 
	 * This function gets called when the variable that "vwait" is currently
	 * watching is written to.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The done variable is set to true, so that "vwait" will
	 * break the waiting loop.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void traceProc(Interp interp, // The current interpreter.
			String part1, // A Tcl variable or array name.
			String part2, // Array element name or NULL.
			int flags) // Mode flags: Should only be TCL.TRACE_WRITES.
	{
		done = true;
	}

} // end VwaitTrace
