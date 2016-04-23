/**
 * Resolver.java
 *
 *	Interface for resolvers that can be added to
 *	the Tcl Interpreter or to a namespace.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 2001 Christian Krone
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Resolver.java,v 1.2 2005/09/12 00:00:50 mdejong Exp $
 */

package tcl.lang;

/**
 * The Resolver interface specifies the methods that a new Tcl resolver must
 * implement. See the addInterpResolver method of the Interp class to see how to
 * add a new resolver to an interperter or the setNamespaceResolver of the
 * NamespaceCmd class.
 */

public interface Resolver {

	public WrappedCommand resolveCmd(Interp interp, // The current interpreter.
			String name, // Command name to resolve.
			Namespace context, // The namespace to look in.
			int flags) // 0 or TCL.LEAVE_ERR_MSG.
			throws TclException; // Tcl exceptions are thrown for Tcl errors.

	public Var resolveVar(Interp interp, // The current interpreter.
			String name, // Variable name to resolve.
			Namespace context, // The namespace to look in.
			int flags) // 0 or TCL.LEAVE_ERR_MSG.
			throws TclException; // Tcl exceptions are thrown for Tcl errors.

} // end Resolver
