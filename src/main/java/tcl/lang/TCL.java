/*
 * TCL.java --
 *
 *	This class stores all the public constants for the tcl.lang.
 *	The exact values should match those in tcl.h.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TCL.java,v 1.6 2005/09/11 20:56:57 mdejong Exp $
 *
 */

package tcl.lang;

// This class holds all the publicly defined constants contained by the
// tcl.lang package.

public class TCL {

	// Flag values passed to variable-related procedures. THESE VALUES
	// MUST BE CONSISTANT WITH THE C IMPLEMENTATION OF TCL.

	public static final int GLOBAL_ONLY = 1;
	public static final int NAMESPACE_ONLY = 2;
	public static final int APPEND_VALUE = 4;
	public static final int LIST_ELEMENT = 8;
	public static final int TRACE_READS = 0x10;
	public static final int TRACE_WRITES = 0x20;
	public static final int TRACE_UNSETS = 0x40;
	public static final int TRACE_DESTROYED = 0x80;
	public static final int INTERP_DESTROYED = 0x100;
	public static final int LEAVE_ERR_MSG = 0x200;
	public static final int PARSE_PART1 = 0x400; // deprecated!
	public static final int TRACE_ARRAY = 0x800;

	// When an TclException is thrown, its compCode may contain any
	// of the following values:
	//
	// TCL.ERROR The command couldn't be completed successfully;
	// the interpreter's result describes what went wrong.
	// TCL.RETURN The command requests that the current procedure
	// return; the interpreter's result contains the
	// procedure's return value.
	// TCL.BREAK The command requests that the innermost loop
	// be exited; the interpreter's result is meaningless.
	// TCL.CONTINUE Go on to the next iteration of the current loop;
	// the interpreter's result is meaningless.

	public static final int ERROR = 1;
	public static final int RETURN = 2;
	public static final int BREAK = 3;
	public static final int CONTINUE = 4;

	// TCL.OK is not typically used as an exception completion code. A
	// TclException would not normally be invoked with the TCL.OK code,
	// but there are some cases where this value could be used. For
	// example, code that invoked Interp.updateReturnInfo() and
	// invokes the Command.cmdProc() API directly without using
	// Interp.eval().

	public static final int OK = 0;

	// The following value is used by the Interp::commandComplete(). It's used
	// to report that a script is not complete.

	protected static final int INCOMPLETE = 10;

	// Flag values to pass to Tcl_DoOneEvent to disable searches
	// for some kinds of events:

	public static final int DONT_WAIT = (1 << 1);
	public static final int WINDOW_EVENTS = (1 << 2);
	public static final int FILE_EVENTS = (1 << 3);
	public static final int TIMER_EVENTS = (1 << 4);
	public static final int IDLE_EVENTS = (1 << 5);
	public static final int ALL_EVENTS = (~DONT_WAIT);

	// The largest positive and negative integer values that can be
	// represented in Tcl.

	public static final long INT_MAX = Long.MAX_VALUE;
	public static final long INT_MIN = Long.MIN_VALUE;

	// These values are used by Util.strtoul and Util.strtod to
	// report conversion errors.

	public static final int INVALID_INTEGER = -1;
	public static final int INTEGER_RANGE = -2;
	public static final int INVALID_DOUBLE = -3;
	public static final int DOUBLE_RANGE = -4;

	// Positions to pass to Tcl_QueueEvent. THESE VALUES
	// MUST BE CONSISTANT WITH THE C IMPLEMENTATION OF TCL.

	public static final int QUEUE_TAIL = 0;
	public static final int QUEUE_HEAD = 1;
	public static final int QUEUE_MARK = 2;

	// Flags used to control the TclIndex.get method.

	public static final int EXACT = 1; // Matches must be exact.

	// Flag values passed to recordAndEval and/or evalObj.
	// These values must match those defined in tcl.h !!!

	// Note: EVAL_DIRECT is not currently used in Jacl.

	public static final int NO_EVAL = 0x10000;
	public static final int EVAL_GLOBAL = 0x20000;
	public static final int EVAL_DIRECT = 0x40000;

} // end TCL

