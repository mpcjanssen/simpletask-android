/*
 * VarTrace.java --
 *
 *	Interface for creating variable traces.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: VarTrace.java,v 1.1.1.1 1998/10/14 21:09:14 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This interface is used to make variable traces. To make a variable
 * trace, write a class that implements the VarTrace and call
 * Interp.traceVar with an instance of that class.
 * 
 */

public interface VarTrace {

	/**
	 * This function gets called when a variable is accessed.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The traceProc can cause arbitrary side effects. If a
	 * TclException is thrown, error message is stored in the result of the
	 * interpreter.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param part1
	 *            first part of variable name
	 * @param part2
	 *            array index, or if null, part1 is a scalar or whole array
	 * @param flags
	 *            TCL.TRACE_READS, TCL.TRACE_WRITES or TCL.TRACE_UNSETS
	 *            (exactly one of these bits will be set.)
	 * @throws TclException
	 */
	abstract public void traceProc(Interp interp, // Current interpreter.
			String part1, // First part of the variable name.
			String part2, // Second part of the var name. May be null.
			int flags) // 
			throws TclException; // The traceProc may throw a TclException
	// to indicate an error during the trace.

} // end VarTrace

