/*
 * Command.java
 *
 *	Interface for Commands that can be added to the Tcl Interpreter.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Command.java,v 1.3 1999/08/05 03:43:27 mo Exp $
 */

package tcl.lang;

/**
 * The Command interface specifies the method that a new Tcl command must
 * implement. See the createCommand method of the Interp class to see how to add
 * a new command to an interperter.
 */

public interface Command {
	/**
	 * This method implements the functionality of the command. However, calling
	 * it directly in application code will bypass execution traces. Instead,
	 * commands should be called with WrappedCommand.invoke
	 * 
	 * @param interp
	 *            The interpreter for setting the results and which contains the
	 *            context
	 * @param objv
	 *            the argument list for the command; objv[0[ is the command name
	 *            itself
	 * @throws TclException
	 *             on any errors
	 */
	abstract public void cmdProc(Interp interp, TclObject[] objv) throws TclException;
}
