/*
 * TclParserExtension.java
 *
 *    Load parser package commands
 *
 * Copyright (c) 2005 Mo DeJong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclParserExtension.java,v 1.2 2005/10/20 18:54:23 mdejong Exp $
 *
 */

package tcl.lang;

public class TclParserExtension extends Extension implements Command {

	// name and version of this package

	static String packageName = "parser";
	static String packageVersion = "1.4";

	/*
	 * Called via [java::load tcl.lang.TclParserExtension] or from the
	 * jaclloadparser command implemented below.
	 */

	public void init(Interp interp) throws TclException {
		interp.createCommand("parse", new TclParser());
		interp.pkgProvide(packageName, packageVersion);
	}

	/*
	 * Invoked when loaded into a safe interp.
	 */

	public void safeInit(Interp safeInterp) throws TclException {
		this.init(safeInterp);
	}

	/*
	 * Invoked when [package require parser] is run from Tcl. This method is
	 * needed so that parser can be loaded without having first loaded the Java
	 * package.
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Arguments to "jaclloadparser" command.
			throws TclException {
		// This method takes no arguments
		if (objv.length != 1) {
			throw new TclNumArgsException(interp, 1, objv, "");
		}

		this.init(interp);

		interp.deleteCommand(objv[0].toString());
	}
}
