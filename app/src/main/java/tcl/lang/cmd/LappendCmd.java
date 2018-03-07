/*
 * LappendCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-1999 by Scriptics Corporation.
 * Copyright (c) 1999 Mo DeJong.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LappendCmd.java,v 1.5 2006/01/27 23:39:02 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Var;

/**
 * This class implements the built-in "lappend" command in Tcl.
 */
public class LappendCmd implements Command {
	/**
	 * 
	 * Tcl_LappendObjCmd -> LappendCmd.cmdProc
	 * 
	 * This procedure is invoked to process the "lappend" Tcl command. See the
	 * user documentation for details on what it does.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		TclObject varValue, newValue = null;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"varName ?value value ...?");
		}
	
		if (objv.length == 2) {
			try {
				newValue = interp.getVar(objv[1], 0);

			} catch (TclException e) {
				// The variable doesn't exist yet. Just create it with an empty
				// initial value.
				varValue = TclList.newInstance();

				try {
					newValue = interp.setVar(objv[1], varValue, 0);
				} finally {
					if (newValue == null) {
						varValue.release(); // free unneeded object
					}
				}

				interp.resetResult();
				return;
			}
			// ensure the target variable is a list, or can be converted to one
			TclList.getLength(interp, newValue);
		} else {
			newValue = lappendVar(interp, objv[1].toString(), objv, 2);
		}

		// Set the interpreter's object result to refer to the variable's value
		// object.

		interp.setResult(newValue);
		return;
	}

	

	/**
	 * Append values to a list value 
	 * 
	 * @param interp current interpreter
	 * @param varName name of variable
	 * @param values TclObject values to append
	 * @param valuesInd index of values to start at
	 * @return new value for variable
	 * @throws TclException
	 */
	public static TclObject lappendVar(Interp interp, String varName, // name of
			// variable
			TclObject[] values, // TclObject values to append
			int valuesInd) // index of values to start at
			throws TclException {
		boolean createdNewObj = false;
		boolean createVar = true;
		TclObject varValue, newValue;
		int i;

		// We have arguments to append. We used to call Tcl_SetVar2 to
		// append each argument one at a time to ensure that traces were run
		// for each append step. We now append the arguments all at once
		// because it's faster. Note that a read trace and a write trace for
		// the variable will now each only be called once. Also, if the
		// variable's old value is unshared we modify it directly, otherwise
		// we create a new copy to modify: this is "copy on write".

		try {
			varValue = interp.getVar(varName, 0);
			
		} catch (TclException e) {
			
			// We couldn't read the old value: either the var doesn't yet
			// exist or it's an array element. If it's new, we will try to
			// create it with Tcl_ObjSetVar2 below.

			if (Var.isArrayVarname(varName)) {
				
				/* If the var doesn't exist and the array doesn't exist yet,
				 * we have to call the READ traces by hand.  This is inconsistent
				 * with, for example, unset arravar; set arrayvar(b) which would
				 * not call a read trace on arrayvar.  But it's what C Tcl does, 
				 * according to append.test 
				 */
				int part1End = varName.indexOf('(');
				Var [] result = Var.lookupVar(interp, 
						varName.substring(0, part1End), null,
					0, "read", false, false);
				if (result != null && result[0].isVarUndefined()) {
					Var.callTraces(interp, null, result[0], varName.substring(0, part1End), varName.substring(part1End+1, 
							varName.length()-1), TCL.TRACE_READS);
				} 
				createVar = false;
			}

			varValue = TclList.newInstance();
			createdNewObj = true;
		}

		// We only take this branch when the catch branch was not run
		if (!createdNewObj && varValue.isShared()) {
			varValue = varValue.duplicate();
			createdNewObj = true;
		}

		// Insert the new elements at the end of the list.

		final int len = values.length;
		if ((len - valuesInd) == 1) {
			TclList.append(interp, varValue, values[valuesInd]);
		} else {
			TclList.append(interp, varValue, values, valuesInd, len);
		}

		// No need to call varValue.invalidateStringRep() since it
		// is called during the TclList.append() method.

		// Now store the list object back into the variable. If there is an
		// error setting the new value, decrement its ref count if it
		// was new and we didn't create the variable.

		try {
			newValue = interp.setVar(varName, varValue, 0);
		} catch (TclException e) {
			if (createdNewObj && !createVar) {
				varValue.release(); // free unneeded obj
			}
			throw e;
		}

		return newValue;
	}
}
