/*
 * ApplyCmd.java --
 *
 * Copyright (C) 2010 Neil Madden &lt;nem@cs.nott.ac.uk&gt.
 *
 * See the file "license.terms" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id$
 */

package tcl.lang.cmd;

import tcl.lang.*;

/**
 * Implementation of the [apply] command.
 *
 * @author  Neil Madden &lt;nem@cs.nott.ac.uk&gt;
 * @version $Revision$
 */
public class ApplyCmd implements Command {

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv, "lambdaExpr ?arg ...?");
		}

		TclLambda.apply(interp, objv[1], objv);
	}

}