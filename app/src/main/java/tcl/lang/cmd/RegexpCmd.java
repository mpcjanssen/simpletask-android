/*
 * RegexpCmd.java --
 *
 * 	This file contains the Jacl implementation of the built-in Tcl
 *	"regexp" command. 
 *
 * Copyright (c) 1997-1999 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: RegexpCmd.java,v 1.15 2010/02/19 06:19:00 mdejong Exp $
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
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * This class implements the built-in "regexp" command in Tcl.
 */

public class RegexpCmd implements Command {

	// switches for regexp command

	private static final String validOpts[] = { "-all", "-about", "-indices",
			"-inline", "-expanded", "-line", "-linestop", "-lineanchor",
			"-nocase", "-start", "--" };

	private static final String validOptsWithXFlags[] = { "-all", "-about", "-indices",
			"-inline", "-expanded", "-line", "-linestop", "-lineanchor",
			"-nocase", "-start", "--", "-xflags" };

	private static final int OPT_ALL = 0;
	private static final int OPT_ABOUT = 1;
	private static final int OPT_INDICES = 2;
	private static final int OPT_INLINE = 3;
	private static final int OPT_EXPANDED = 4;
	private static final int OPT_LINE = 5;
	private static final int OPT_LINESTOP = 6;
	private static final int OPT_LINEANCHOR = 7;
	private static final int OPT_NOCASE = 8;
	private static final int OPT_START = 9;
	private static final int OPT_LAST = 10;
	private static final int OPT_XFLAGS = 11;

    private boolean allowXFlags = false;
	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * init --
	 * 
	 * This procedure is invoked to connect the regexp and regsub commands to
	 * the CmdProc method of the RegexpCmd and RegsubCmd classes, respectively.
	 * Avoid the AutoloadStub class because regexp and regsub need a stub with a
	 * switch to check for the existence of the tcl.regexp package.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The regexp and regsub commands are now connected to the
	 * CmdProc method of the RegexpCmd and RegsubCmd classes, respectively.
	 * 
	 * 
	 * 
	 * --------------------------------------------------------------------------
	 * ---
	 */

	public static void init(Interp interp) // Current interpreter.
	{
		interp.createCommand("regexp", new tcl.lang.cmd.RegexpCmd());
		interp.createCommand("regsub", new tcl.lang.cmd.RegsubCmd());
	}

    /**
     * Sets this instance of RegexpCmd to interpret and pass on
     * the arguments of -xflags.  By default, allowXFlags is false.
     *
     * @param allow Set to true to allow -xflags processing
     */
    public void setAllowXFlags(boolean allow) {
        allowXFlags = allow;
    }

    /**
	 * Returns a list for -about when a compile error occur.
	 * first element of the list is a subexpression count (0). The second element is
	 * a should be a list of property names that describe various attributes of the regular
	 * expression. Here we use just "COMPILE_ERROR"; reg.test is adjusted accordingly.
	 * 
	 * Primarily intended for debugging purposes.
	 * 
	 * @param interp
	 *            current Jacl interpreter object
	 * @return A list containing information about the regular expression.
	 * @throws TclException
	 */

	protected TclObject getAboutCompileError(Interp interp) throws TclException {
		TclObject props = TclList.newInstance();

        // 0 for subexpression count
		TclList.append(interp, props, TclString.newInstance("0"));

        // For now, provie "COMPILE_ERROR" on any compile problem,
        // or an empty list.  This result interacts with 'reg.test',
        // which we've changed to expect COMPILE_ERROR instead of implementation
        // specific compile error messages.
        // Perhaps we should emulate TclRegAbout() in the future?
		TclList.append(interp, props, TclString.newInstance("COMPILE_ERROR"));

		return props;
	}

	/*
	 * --------------------------------------------------------------------------
	 * ---
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked to process the "regexp" Tcl command. See the
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
			TclObject[] objv) // Arguments to "regexp" command.
			throws TclException {
		boolean indices = false;
		boolean doinline = false;
		boolean about = false;
		boolean last = false;
		int all = 0;
		int flags;
		int offset = 0; // the index offset of the string to start matching the
		int objc = 0;
		// regular expression at
		TclObject result;
		int i;
        String xflags = null;

		flags = 0;

		for (i = 1; i < objv.length; i++) {
			if (last) {
				break;
			}

			TclObject obj = objv[i];

			if ((obj.toString().length() == 0)
					|| (obj.toString().charAt(0) != '-')) {
				// Not an option
				break;
			}

			int index = TclIndex.get(interp, obj, 
                                     allowXFlags ? validOptsWithXFlags : validOpts,
                                     "switch", 0);

			switch (index) {
			case OPT_ABOUT:
				about = true;
				break;
			case OPT_EXPANDED:
				flags |= Regex.TCL_REG_EXPANDED;
				break;
			case OPT_INDICES:
				indices = true;
				break;
			case OPT_LINESTOP:
				flags |= Regex.TCL_REG_NLSTOP;
				break;
			case OPT_LINEANCHOR:
				flags |= Regex.TCL_REG_NLANCH;
				break;
			case OPT_LINE:
				flags |= Regex.TCL_REG_NLSTOP;
				flags |= Regex.TCL_REG_NLANCH;
				break;
			case OPT_NOCASE:
				flags |= Regex.TCL_REG_NOCASE;
				break;
			case OPT_ALL:
				all = 1;
				break;
			case OPT_INLINE:
				doinline = true;
				break;
			case OPT_START:
				if (++i >= objv.length) {
					// break the switch, the index out of bounds exception
					// will be caught later
					break;
				}

				offset = TclInteger.getInt(interp, objv[i]);

				if (offset < 0) {
					offset = 0;
				}

				break;
			case OPT_LAST:
				last = true;
				break;
            case OPT_XFLAGS:
				if (++i >= objv.length) {
					// break the switch, the index out of bounds exception
					// will be caught later
					break;
				}

				xflags = objv[i].toString();
                last = true; // according to test case reg-11.18.execute
                break;
			} // end of switch block
		} // end of for loop

		if ((objv.length - i) < (2 - (about ? 1 : 0))) {
			throw new TclNumArgsException(interp, 1, objv,
					"?switches? exp string ?matchVar?"
							+ " ?subMatchVar subMatchVar ...?");
		}

		if (doinline && ((objv.length - i - 2) != 0)) {
			// User requested -inline, but specified match variables - a
			// no-no.

			throw new TclException(interp,
					"regexp match variables not allowed when using -inline");
		}

		String exp = objv[i++].toString();

		String string;

		if (about) {
			string = "";
		} else {
			string = objv[i++].toString();
		}

		Regex reg = null;
		result = TclInteger.newInstance(0);

		try {
            if (xflags==null)
			    reg = new Regex(exp, string, offset, flags);
            else
			    reg = new Regex(exp, string, offset, flags, xflags);
		} catch (PatternSyntaxException ex) {
			interp.setErrorCode(TclString
					.newInstance("REGEXP COMPILE_ERROR {" + Regex.getPatternSyntaxMessage(ex) + "}"));
			throw new TclException(interp, Regex.getPatternSyntaxMessage(ex));
		} catch (Exception e) {
			interp.setErrorCode(TclString
					.newInstance("REGEXP COMPILE_ERROR {" + e.getMessage() + "}"));
			throw new TclException(interp, e.getMessage());
        }

		if (about) {
            interp.setResult(reg.getInfo(interp));
			return;
		}


		boolean matched;

		// The following loop is to handle multiple matches within the
		// same source string; each iteration handles one match. If
		// "-all" hasn't been specified then the loop body only gets
		// executed once. We terminate the loop when the starting offset
		// is past the end of the string.

		while (true) {
			matched = reg.match();

			if (!matched) {
				// We want to set the value of the intepreter result only
				// when this is the first time through the loop.

				if (all <= 1) {
					// If inlining, set the interpreter's object result
					// to an empty list, otherwise set it to an integer
					// object w/ value 0.

					if (doinline) {
						interp.resetResult();
					} else {
						interp.setResult(0);
					}
					return;
				}

				break;
			}

			int groupCount = reg.groupCount();
			int group = 0;

			if (doinline) {
				// It's the number of substitutions, plus one for the
				// matchVar at index 0

				objc = groupCount + 1;
			} else {
				objc = objv.length - i;
			}

			// loop for each variable or list element that stores a result

			for (int j = 0; j < objc; j++) {
				TclObject obj;

				if (indices) {
					int start;
					int end;

					if (group <= groupCount) {
						start = reg.start(group);
						end = reg.end(group);
						group++;


						if (end >= reg.getOffset()) {
							end--;
						}

					} else {
						start = -1;
						end = -1;
					}

					obj = TclList.newInstance();
					TclList.append(interp, obj, TclInteger.newInstance(start));
					TclList.append(interp, obj, TclInteger.newInstance(end));
				} else {
					if (group <= groupCount) {
						// group 0 is the whole match, the groups
						// 1 to groupCount indicate submatches
						// but note that the number of variables
						// could be more than the number of matches.
						// Also, optional matches groups might not
						// match a range in the input string.

						int start = reg.start(group);

						if (start == -1) {
							// Optional group did not match input
							obj = TclList.newInstance();
						} else {
							int end = reg.end(group);
							String substr = string.substring(start, end);
							obj = TclString.newInstance(substr);
						}

						group++;
					} else {
						obj = TclList.newInstance();
					}
				}

				if (doinline) {
					interp.appendElement(obj.toString());
				} else {
					String varName = objv[i + j].toString();
					try {
						interp.setVar(varName, obj, 0);
					} catch (TclException e) {
						throw new TclException(interp,
								"couldn't set variable \"" + varName + "\"");
					}
				}
			} // end of for loop

			if (all == 0) {
				break;
			}

            all++;

            /* Emulate a possible TCL bug here.  In the C TCL implementation
             * this match() loop exits when reg.end() of a non-zero-length
             * match is >= string.length(), or when reg.end()+1 of
             * a zero-length match is >= string.length().  The "bug" is
             * that any further zero-length match will not be seen.
             * regsub -all does not suffer from this same malady.
             * For example,
             *   regexp -all -inline {a*} {a}  returns {a}
             *   regsub -all {a*} {a} {Z} returns {ZZ}, one 'Z' for the
             *     length=1 match on 'a', and another for the 0-length
             *     match right after 'a' that regexp misses.  
             * Without this fix, regexp would act like regsub and return
             * {a {}}   
             */
            int endMatch = reg.end();
            if (reg.start() == reg.end())
                ++endMatch;
            if (endMatch >= string.length()) break;
		} // end of while loop

		// Set the interpreter's object result to an integer object with
		// value 1 if -all wasn't specified, otherwise it's all-1 (the
		// number of times through the while - 1). Get the resultPtr again
		// as the Tcl_ObjSetVar2 above may have cause the result to change.
		// [Patch #558324] (watson).

		if (!doinline) {
			interp.setResult((all != 0) ? (all - 1) : 1);
		}
	} // end cmdProc
} // end RegexpCmd

