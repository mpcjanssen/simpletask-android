/*
 * TclNumArgsException.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclNumArgsException.java,v 1.3 2003/01/12 02:44:28 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This exception is used to report wrong number of arguments in Tcl scripts.
 */

public class TclNumArgsException extends TclException {

	/**
	 * Creates a TclException with the appropiate Tcl error message for having
	 * the wring number of arguments to a Tcl command.
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * 
	 * if (argv.length != 3) {
	 * 	throw new TclNumArgsException(interp, 1, argv, &quot;option name&quot;);
	 * }
	 * </pre>
	 * 
	 * @param interp
	 *            current Interpreter.
	 * @param argc
	 *            the number of arguments to copy from the offending command to
	 *            put into the error message.
	 * @param argv
	 *            the arguments of the offending command.
	 * @param message
	 *            extra message to appear in the error message that explains the
	 *            proper usage of the command.
	 * @exception TclException
	 *                is always thrown.
	 */

	public TclNumArgsException(Interp interp, int argc, TclObject argv[],
			String message) throws TclException {
		super(TCL.ERROR);

		if (interp != null) {
			StringBuilder buff = new StringBuilder(50);
			buff.append("wrong # args: should be \"");

			for (int i = 0; i < argc; i++) {
				if (argv[i].getInternalRep() instanceof TclIndex) {
					buff.append(argv[i].getInternalRep().toString());
				} else {
					buff.append(argv[i].toString());
				}
				if (i < (argc - 1)) {
					buff.append(" ");
				}
			}
			if ((message != null) && (message.length() != 0)) {
				buff.append(" " + message);
			}
			buff.append("\"");
			interp.setResult(buff.toString());
		}
	}
}
