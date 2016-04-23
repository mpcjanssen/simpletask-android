/*
 * DebugInfo.java --
 *
 *	This class stores debug information for the interpreter.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: DebugInfo.java,v 1.1.1.1 1998/10/14 21:09:18 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This class stores debug information for the interpreter.
 */

class DebugInfo {

	/*
	 * The name of the source file that contains code for a given debug stack
	 * level. May be null for an unknown source file (if the debug stack is
	 * activated by an "eval" command or if the Interp is running in
	 * non-debugging mode.)
	 */

	String fileName;

	/*
	 * The beginning line of the current command under execution. 1 means the
	 * first line inside a file. 0 means the line number is unknown.
	 */

	int cmdLine;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * DebugInfo --
	 * 
	 * Construct a DebugInfo object with the given info.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Member fields are initialized.
	 * 
	 * ----------------------------------------------------------------------
	 */

	DebugInfo(String fname, // Initial value for fileName.
			int line) // Initial value for cmdLine.
	{
		fileName = fname;
		cmdLine = line;
	}

} // end DebugInfo
