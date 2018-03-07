/*
 * LassignCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * This class implements the built-in "lassign" command in Tcl.
 */

public class LassignCmd implements Command {
	private TclObject empty;
	
	/**
	 * See Tcl user documentation for details.
	 * 
	 * @exception TclException
	 *                If incorrect number of arguments.
	 */
	
	public LassignCmd() {
		empty = TclString.newInstance("");
		empty.preserve();  // incr ref count
		empty.preserve();  // mark it as shared
	}

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length < 3) {
			throw new TclNumArgsException(interp, 1, argv,
					"list varName ?varName ...?");
		}

		
		TclObject[] elems = TclList.getElements(interp, argv[1]);
		
		int argc = 2;
		int elemc = 0;
		while (argc < argv.length && elemc < elems.length) {
			interp.setVar(argv[argc++], elems[elemc++], 0);
		}
		
		// any left over varName arguments? set to empty value
		if (argc < argv.length) {
			while (argc < argv.length) {
				interp.setVar(argv[argc++], empty, 0);
			}
		}

		// any left over list elements? set as result value
		if (elemc < elems.length) {
			TclObject listCopy = TclList.newInstance();
			TclList.append(interp, listCopy, elems, elemc, elems.length);
			interp.setResult(listCopy);
		}

	}
}
