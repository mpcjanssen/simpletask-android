/*
 * SplitCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SplitCmd.java,v 1.3 2006/06/06 04:48:03 mdejong Exp $
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
 * This class implements the built-in "split" command in Tcl.
 */

public class SplitCmd implements Command {

	// Default characters for splitting up strings.

	private final static char defSplitChars[] = { ' ', '\n', '\t', '\r' };

	/**
	 * This procedure is invoked to process the "split" Tcl command. See Tcl
	 * user documentation for details.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param objv
	 *            command arguments.
	 * @exception TclException
	 *                If incorrect number of arguments.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		int numSplitChars;
		String splitString = null;

		if (objv.length == 2) {
			numSplitChars = defSplitChars.length;
		} else if (objv.length == 3) {
			splitString = objv[2].toString();
			if (splitString.equals("")) {
				numSplitChars = 0;
			} else if (splitString.length() == 1) {
				numSplitChars = 1;
			} else {
				numSplitChars = splitString.length();
			}
		} else {
			throw new TclNumArgsException(interp, 1, objv,
					"string ?splitChars?");
		}

		String string = objv[1].toString();
		final int slen = string.length();
		int elemStart = 0;
		int i = 0;

		TclObject list = TclList.newInstance();
		list.preserve();

		try {
			if (numSplitChars == 0) {
				// Splitting on every character.

				for (; i < slen; i++) {
					TclObject tobj = interp.checkCommonCharacter(string
							.charAt(i));
					if (tobj == null) {
						tobj = TclString
								.newInstance(string.substring(i, i + 1));
					}
					TclList.append(interp, list, tobj);
				}
			} else if (numSplitChars == 1) {
				// Splitting on one character.
				// Discard instances of the split character.

				char splitChar = splitString.charAt(0);

				for (; i < slen; i++) {
					if (string.charAt(i) == splitChar) {
						appendElement(interp, list, string, elemStart, i);
						elemStart = i + 1;
					}
				}
				if (i != 0) {
					appendElement(interp, list, string, elemStart, i);
				}
			} else {
				// Splitting on any char in a group of character.
				// Discard instances of the split characters.

				char[] splitChars;
				if (objv.length == 2) {
					splitChars = defSplitChars;
				} else {
					splitChars = splitString.toCharArray();
				}

				for (; i < slen; i++) {
					char c = string.charAt(i);
					for (int j = 0; j < numSplitChars; j++) {
						if (c == splitChars[j]) {
							appendElement(interp, list, string, elemStart, i);
							elemStart = i + 1;
							break;
						}
					}
				}
				if (i != 0) {
					appendElement(interp, list, string, elemStart, i);
				}
			}

			interp.setResult(list);
		} finally {
			list.release();
		}
	}

	// Util method used to append a string range
	// to a TclList. The range might indicate
	// an empty string or a single character, so
	// use shared objects to optimize those cases.

	static void appendElement(Interp interp, TclObject list, String string,
			int starti, int endi) throws TclException {
		TclObject tobj;

		switch (endi - starti) {
		case 0: {
			tobj = interp.checkCommonString(null);
			break;
		}
		case 1: {
			tobj = interp.checkCommonCharacter(string.charAt(starti));
			if (tobj != null) {
				break;
			}
			// Fall through when not a common character
		}
		default: {
			tobj = TclString.newInstance(string.substring(starti, endi));
			break;
		}
		}

		TclList.append(interp, list, tobj);
		return;
	}

}
