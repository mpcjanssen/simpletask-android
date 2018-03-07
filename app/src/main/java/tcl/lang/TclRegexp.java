/*
 * TclRegexp.java
 *
 * Copyright (c) 1999 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * SCCS: %Z% %M% %I% %E% %U%
 */

package tcl.lang;

import java.util.regex.PatternSyntaxException;

public class TclRegexp {
	private TclRegexp() {
	}

	public static Regex compile(Interp interp, TclObject exp, String str)
			throws TclException {
		try {
			return new Regex(exp.toString(), str, 0);
		} catch (PatternSyntaxException ex) {
			throw new TclException(interp, Regex.getPatternSyntaxMessage(ex));
		}
	}
}
