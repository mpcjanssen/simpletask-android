/*
 * AssocData.java --
 *
 *	The API for registering named data objects in the Tcl
 *	interpreter.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: AssocData.java,v 1.2 1999/05/11 23:10:03 dejong Exp $
 *
 */

package tcl.lang;

/**
 * This interface is the API for registering named data objects in the Tcl
 * interpreter.
 */

public interface AssocData {

	/**
	 * This method is called when the interpreter is destroyed or when
	 * Interp.deleteAssocData is called on a registered AssocData instance.
	 * 
	 * @param interp the interpreter in which this AssocData instance is registered in
	 */
	public void disposeAssocData(Interp interp); 
}
