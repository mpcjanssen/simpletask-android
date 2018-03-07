/*
 * LrangeCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LrangeCmd.java,v 1.2 2000/03/17 23:31:30 mo Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Util;

/**
 * This class implements the built-in "lrange" command in Tcl.
 */

public class LrangeCmd implements Command {
	/**
	 * See Tcl user documentation for details.
	 * 
	 * @exception TclException
	 *                If incorrect number of arguments.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length != 4) {
			throw new TclNumArgsException(interp, 1, argv, "list first last");
		}

		int size = TclList.getLength(interp, argv[1]);
		int first;
		int last;

		first = Util.getIntForIndex(interp, argv[2], size - 1);
		last = Util.getIntForIndex(interp, argv[3], size - 1);

		if (last < 0) {
			interp.resetResult();
			return;
		}
		if (first >= size) {
			interp.resetResult();
			return;
		}
		if (first <= 0 && last >= size) {
			interp.setResult(argv[1]);
			return;
		}

		if (first < 0) {
			first = 0;
		}
		if (first >= size) {
			first = size - 1;
		}
		if (last < 0) {
			last = 0;
		}
		if (last >= size) {
			last = size - 1;

		}
		if (first > last) {
			interp.resetResult();
			return;
		}

		TclObject list = TclList.newInstance();

		list.preserve();
		try {
			for (int i = first; i <= last; i++) {
				TclList.append(interp, list, TclList.index(interp, argv[1], i));
			}
			interp.setResult(list);
		} finally {
			list.release();
		}
	}
}
