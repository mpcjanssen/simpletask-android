/*
 * RenameCmd.java
 *
 * Copyright (c) 1999 Mo DeJong.
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RenameCmd.java,v 1.2 1999/08/03 03:07:54 mo Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "rename" command in Tcl.
 */

public class RenameCmd implements Command {
	/**
	 *----------------------------------------------------------------------
	 * 
	 * Tcl_RenameObjCmd -> RenameCmd.cmdProc
	 * 
	 * This procedure is invoked to process the "rename" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * Results: A standard Tcl object result.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 *----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		String oldName, newName;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 1, objv, "oldName newName");
		}

		oldName = objv[1].toString();
		newName = objv[2].toString();

		interp.renameCommand(oldName, newName);
		return;
	}
}
