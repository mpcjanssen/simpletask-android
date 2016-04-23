/*
 * SourceCmd.java
 *
 *	Implements the "source" command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SourceCmd.java,v 1.2 2005/11/07 07:41:51 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "source" command in Tcl.
 */

public class SourceCmd implements Command {

	/**
	 * cmdProc --
	 * 
	 * This cmdProc is invoked to process the "source" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: A standard Tcl result is stored in the interpreter. See the
	 * user documentation.
	 * 
	 * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
	 */
	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		String fileName = null;
		boolean url = false;
		String encoding = EncodingCmd.systemTclEncoding;
		
		for (int i=1; i<argv.length; i++) {
			String argstr = argv[i].toString();
			if (i==argv.length-1) {
				fileName = argstr;
			} else if (argstr.equals("-url")) {
				url = true;
			} else if (argstr.equals("-encoding")) {
				if (i==argv.length-1) encoding=null;
				else encoding = argv[++i].toString();
			} else {
				throw new TclException(interp,"bad option \""+argstr+"\": must be -encoding or -url");
			}
		}
		

		if (fileName == null || encoding==null) {
			throw new TclNumArgsException(interp, 1, argv, "?-encoding name? ?-url? fileName");
		}
		String javaEncoding = EncodingCmd.getJavaName(encoding);
		if (javaEncoding == null) {
			throw new TclException(interp, "unknown encoding \""
					+ encoding + "\"");
		}
		
		try {
			if (fileName.startsWith("resource:/")) {
				interp.evalResource(fileName.substring(9), javaEncoding);
			} else if (url) {
				interp.evalURL(null, fileName, javaEncoding);
			} else {
				interp.evalFile(fileName,javaEncoding);
			}
		} catch (TclException e) {
			int code = e.getCompletionCode();

			if (code == TCL.RETURN) {
				int realCode = interp.updateReturnInfo();
				if (realCode != TCL.OK) {
					e.setCompletionCode(realCode);
					throw e;
				}
			} else  {				
				throw e;
			}
		}
	}

}
