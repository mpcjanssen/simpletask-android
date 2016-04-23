/*
 * RegsubCmd.java
 *
 * 	This contains the Jacl implementation of the built-in Tcl
 *	"regsub" command.
 *
 * Copyright (c) 1997-1999 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegsubCmd.java,v 1.12 2009/10/04 20:08:56 mdejong Exp $
 */

package tcl.lang.cmd;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.Regex;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * This class implements the built-in "regsub" command in Tcl.
 */

public class RegsubCmd implements Command {

	private static final String validOpts[] = { "-all", "-nocase", "-expanded",
			"-line", "-linestop", "-lineanchor", "-start", "--" };

	private static final int OPT_ALL = 0;
	private static final int OPT_NOCASE = 1;
	private static final int OPT_EXPANDED = 2;
	private static final int OPT_LINE = 3;
	private static final int OPT_LINESTOP = 4;
	private static final int OPT_LINEANCHOR = 5;
	private static final int OPT_START = 6;
	private static final int OPT_LAST = 7;

	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked to process the "regsub" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * 
	 * 
	 * --------------------------------------------------------------------------
	 * ---
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Arguments to "regsub" command.
			throws TclException {
		int idx;
		boolean all = false;
		boolean last = false;
		int flags;
		int offset = 0;
		String result;

		flags = 0;

		for (idx = 1; idx < objv.length; idx++) {
			if (last) {
				break;
			}

			TclObject obj = objv[idx];

			if ((obj.toString().length() == 0)
					|| (obj.toString().charAt(0) != '-')) {
				// Not an option
				break;
			}

			int index = TclIndex.get(interp, obj, validOpts, "switch", 0);

			switch (index) {
			case OPT_ALL:
				all = true;
				break;
			case OPT_EXPANDED:
				flags |= Regex.TCL_REG_EXPANDED;
				break;
			case OPT_LINESTOP:
				flags |= Regex.TCL_REG_NLSTOP;
				break;
			case OPT_LINEANCHOR:
				flags |= Regex.TCL_REG_NLANCH;
				break;
			case OPT_LINE:
				flags |= Regex.TCL_REG_NLANCH;
				flags |= Regex.TCL_REG_NLSTOP;
				break;
			case OPT_NOCASE:
				flags |= Regex.TCL_REG_NOCASE;
				break;
			case OPT_START:
				if (++idx == objv.length) {
					// break the switch, the index out of bounds exception
					// will be caught later

					break;
				}

				offset = TclInteger.getInt(interp, objv[idx]);

				if (offset < 0) {
					offset = 0;
				}
				break;
			case OPT_LAST:
				last = true;
				break;
			}
		} // end options for loop

		if (objv.length - idx < 3 || objv.length - idx > 4) {
			throw new TclNumArgsException(interp, 1, objv,
					"?switches? exp string subSpec ?varName?");
		}

		// get cmd's params

		String exp = objv[idx++].toString();
		String string = objv[idx++].toString();
		String subSpec = objv[idx++].toString();
		String varName = null;

		if ((objv.length - idx) > 0) {
			varName = objv[idx++].toString();
		}


		Regex reg = null;
		try {
			reg = new Regex(exp, string, offset, flags);
		} catch (PatternSyntaxException ex) {
            interp.setErrorCode(TclString
                    .newInstance("REGEXP COMPILE_ERROR {" + Regex.getPatternSyntaxMessage(ex) + "}"));
            throw new TclException(interp, Regex.getPatternSyntaxMessage(ex));
        } catch (Exception e) {
            interp.setErrorCode(TclString
                    .newInstance("REGEXP COMPILE_ERROR {" + e.getMessage() + "}"));
            throw new TclException(interp, e.getMessage());
        }

		// do the replacement process
        result = reg.replace(subSpec, all);
        int matchCount = reg.getCount();

        // Emulate an apparent bug in TCL C Implementation. It 
        // has a special case in which it doesn't
        // use the regular expression engine.  That special case
        // is conditioned on '-all' being used.  And that special
        // case returns an empty string for 'regsub -all "" "" A'
        // even though 'regsub "" "" A" returns 'A' and 
        // 'regex "" ""' returns 1.
        if (all && exp.length()==0 && string.length()==0) {
            result = "";
            matchCount = 0;
        } 

		try {
			if (varName != null) {
				interp.setResult(matchCount);
				interp.setVar(varName, result, 0);
			} else {
				interp.setResult(result);
			}
		} catch (TclException e) {
			throw new TclException(interp, "couldn't set variable \"" + varName
					+ "\"");
		}
	}

} // end class RegsubCmd

