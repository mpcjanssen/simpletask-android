/*
 * LsortCmd.java
 *
 *	The file implements the Tcl "lsort" command.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LsortCmd.java,v 1.4 2009/07/08 14:12:35 rszulgo Exp $
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.QSort;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Util;

/*
 * This LsortCmd class implements the Command interface for specifying a new
 * Tcl command.  The Lsort command implements the built-in Tcl command "lsort"
 * which is used to sort Tcl lists.  See user documentation for more details.
 */

public class LsortCmd implements Command {

	/*
	 * List of switches that are legal in the lsort command.
	 */

	static final private String validOpts[] = { "-ascii", "-command",
			"-decreasing", "-dictionary", "-increasing", "-index", "-integer",
			"-real", "-unique" };

	static final int OPT_ASCII = 0;
	static final int OPT_COMMAND = 1;
	static final int OPT_DECREASING = 2;
	static final int OPT_DICTIONARY = 3;
	static final int OPT_INCREASING = 4;
	static final int OPT_INDEX = 5;
	static final int OPT_INTEGER = 6;
	static final int OPT_REAL = 7;
	static final int OPT_UNIQUE = 8;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "lsort" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, /* Current interpreter. */
	TclObject argv[]) /* Argument list. */
	throws TclException /* A standard Tcl exception. */
	{
		if (argv.length < 2) {
			throw new TclNumArgsException(interp, 1, argv, "?options? list");
		}

		String command = null;
		int sortMode = QSort.ASCII;
		int sortIndex = -1;
		boolean sortIncreasing = true;
		boolean unique = false;

		for (int i = 1; i < argv.length - 1; i++) {
			int index = TclIndex.get(interp, argv[i], validOpts, "option", 0);

			switch (index) {
			case OPT_ASCII: /* -ascii */
				sortMode = QSort.ASCII;
				break;

			case OPT_COMMAND: /* -command */
				if (i == argv.length - 2) {
					throw new TclException(interp,
							"\"-command\" option must be"
									+ " followed by comparison command");
				}
				sortMode = QSort.COMMAND;
				command = argv[i + 1].toString();
				i++;
				break;

			case OPT_DECREASING: /* -decreasing */
				sortIncreasing = false;
				break;

			case OPT_DICTIONARY: /* -dictionary */
				sortMode = QSort.DICTIONARY;
				break;

			case OPT_INCREASING: /* -increasing */
				sortIncreasing = true;
				break;

			case OPT_INDEX: /* -index */
				if (i == argv.length - 2) {
					throw new TclException(interp,
							"\"-index\" option must be followed by list index");
				}
				// this caused 'end-1' to be reported as '-2 - 1' = -3.
				sortIndex = Util.getIntForIndex(interp, argv[i + 1], -2);
				i++;
				break;

			case OPT_INTEGER: /* -integer */
				sortMode = QSort.INTEGER;
				break;

			case OPT_REAL: /* -real */
				sortMode = QSort.REAL;
				break;

			case OPT_UNIQUE:
				unique = true;
				break;
			}
		}
		TclObject list = argv[argv.length - 1];
		boolean isDuplicate = false;

		// If the list object is unshared we can modify it directly. Otherwise
		// we create a copy to modify: this is "copy on write".

		if (list.isShared()) {
			list = list.duplicate();
			isDuplicate = true;
		}

		try {
			TclList.sort(interp, list, sortMode, sortIndex, sortIncreasing,
					unique, command);
			interp.setResult(list);
		} catch (TclException e) {
			if (isDuplicate) {
				list.release();
			}
			throw e;
		}
	}

} // LsortCmd
