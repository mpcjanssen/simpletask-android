/*
 * ListCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ListCmd.java,v 1.3 2010/02/12 03:43:50 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "list" command in Tcl.
 */
public class ListCmd implements Command {

	/**
	 * See Tcl user documentation for details.
	 */
	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		TclObject list = TclList.newInstance();

		try {
			for (int i = 1; i < objv.length; i++) {
				TclList.append(interp, list, objv[i]);
			}
			interp.setResult(list);
		} catch (TclException te) {
			list.release();
			throw te;
		}
	}
}
