/*
 * TclNumArgsException.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclVarException.java,v 1.1.1.1 1998/10/14 21:09:19 cvsadmin Exp $
 *
 */

package tcl.lang;

/**
 * This exception is used to report variable errors in Tcl.
 */

public class TclVarException extends TclException {

	/**
	 * Creates an exception with the appropiate Tcl error message to indicate an
	 * error with variable access.
	 * 
	 * @param interp
	 *            currrent interpreter.
	 * @param name1
	 *            first part of a variable name.
	 * @param name2
	 *            second part of a variable name. May be null.
	 * @param operation
	 *            either "read" or "set".
	 * @param reason
	 *            a string message to explain why the operation fails..
	 */

	public TclVarException(Interp interp, String name1, String name2,
			String operation, String reason) {
		super(TCL.ERROR);
		if (interp != null) {
			interp.resetResult();
			if (name2 == null) {
				interp.setResult("can't " + operation + " \"" + name1 + "\": "
						+ reason);
			} else {
				interp.setResult("can't " + operation + " \"" + name1 + "("
						+ name2 + ")\": " + reason);
			}
		}
	}
}
