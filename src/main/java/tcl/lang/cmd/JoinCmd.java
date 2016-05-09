/*
 * JoinCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: JoinCmd.java,v 1.1.1.1 1998/10/14 21:09:18 cvsadmin Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "join" command in Tcl.
 */
public class JoinCmd implements Command {

	/**
	 * See Tcl user documentation for details.
	 */
	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		String sep = null;

		if (argv.length == 2) {
			sep = null;
		} else if (argv.length == 3) {
			sep = argv[2].toString();
		} else {
			throw new TclNumArgsException(interp, 1, argv, "list ?joinString?");
		}
		TclObject list = argv[1];
		int size = TclList.getLength(interp, list);

		if (size == 0) {
			interp.resetResult();
			return;
		}

		StringBuilder sbuf = new StringBuilder(TclList.index(interp, list, 0)
				.toString());

		for (int i = 1; i < size; i++) {
			if (sep == null) {
				sbuf.append(' ');
			} else {
				sbuf.append(sep);
			}
			sbuf.append(TclList.index(interp, list, i).toString());
		}
		interp.setResult(sbuf.toString());
	}
}
