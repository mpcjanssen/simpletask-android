/*
 * VariableCmd.java
 *
 * Copyright (c) 1987-1994 The Regents of the University of California.
 * Copyright (c) 1994-1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-1999 by Scriptics Corporation.
 * Copyright (c) 1999      by Moses DeJong.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: VariableCmd.java,v 1.7 2006/03/27 00:06:42 mdejong Exp $
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclObject;
import tcl.lang.Var;

/**
 * This class implements the built-in "variable" command in Tcl.
 */

public class VariableCmd implements Command {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * VariableCmd --
	 * 
	 * Invoked to implement the "variable" command that creates one or more
	 * global variables. Handles the following syntax:
	 * 
	 * variable ?name value...? name ?value?
	 * 
	 * One or more variables can be created. The variables are initialized with
	 * the specified values. The value for the last variable is optional.
	 * 
	 * If the variable does not exist, it is created and given the optional
	 * value. If it already exists, it is simply set to the optional value.
	 * Normally, "name" is an unqualified name, so it is created in the current
	 * namespace. If it includes namespace qualifiers, it can be created in
	 * another namespace.
	 * 
	 * If the variable command is executed inside a Tcl procedure, it creates a
	 * local variable linked to the newly-created namespace variable.
	 * 
	 * Results: Returns TCL_OK if the variable is found or created. Returns
	 * TCL_ERROR if anything goes wrong.
	 * 
	 * Side effects: If anything goes wrong, this procedure returns an error
	 * message as the result in the interpreter's result object.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		String varName;
		int tail, cp;
		Var var, array;
		TclObject varValue;
		int i;

		if (objv.length==1) {
			throw new TclException(interp,"wrong # args: should be \"variable ?name value...? name ?value?\"");
		}
		for (i = 1; i < objv.length; i = i + 2) {
			// Look up each variable in the current namespace context, creating
			// it if necessary.

			varName = objv[i].toString();
			if (varName.endsWith(")") && varName.contains("(")) {
				throw new TclException(interp, "can't define \""+varName+"\": name refers to an element in an array");					
				
			}
			Var[] result = Var.lookupVar(interp, varName, null,
					(TCL.NAMESPACE_ONLY | TCL.LEAVE_ERR_MSG), "define", true,
					false);
			if (result == null) {
				// FIXME:
				throw new TclException(interp, "");
			}

			var = result[0];
			array = result[1];
			
			// Mark the variable as a namespace variable and increment its
			// reference count so that it will persist until its namespace is
			// destroyed or until the variable is unset.

			if (!var.isVarNamespace()) {
				var.setVarNamespace();
				var.refCount++;
			}

			// If a value was specified, set the variable to that value.
			// Otherwise, if the variable is new, leave it undefined.
			// (If the variable already exists and no value was specified,
			// leave its value unchanged; just create the local link if
			// we're in a Tcl procedure).

			if (i + 1 < objv.length) { // a value was specified
				varValue = Var.setVar(interp, objv[i].toString(), null,
						objv[i + 1], (TCL.NAMESPACE_ONLY | TCL.LEAVE_ERR_MSG));

				if (varValue == null) {
					// FIXME:
					throw new TclException(interp, "");
				}
			}

			// If we are executing inside a Tcl procedure, create a local
			// variable linked to the new namespace variable "varName".

			if ((interp.varFrame != null) && interp.varFrame.isProcCallFrame) {

				// varName might have a scope qualifier, but the name for the
				// local "link" variable must be the simple name at the tail.

				String varTail = NamespaceCmd.tail(varName);

				// Create a local link "tail" to the variable "varName" in the
				// current namespace.

				Var.makeUpvar(interp, null, varName, null, TCL.NAMESPACE_ONLY,
						varTail, 0, -1);
			}
		}
	}
}
