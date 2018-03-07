/*
 * GetsCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: GetsCmd.java,v 1.6 2003/03/08 03:42:44 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import java.io.IOException;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclPosixException;
import tcl.lang.TclRuntimeError;
import tcl.lang.TclString;
import tcl.lang.channel.Channel;

/**
 * This class implements the built-in "gets" command in Tcl.
 */

public class GetsCmd implements Command {

	/**
	 * This procedure is invoked to process the "gets" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {

		boolean writeToVar = false; // If true write to var passes as arg
		String varName = ""; // The variable to write value to
		Channel chan; // The channel being operated on
		int lineLen;
		TclObject line;

		if ((argv.length < 2) || (argv.length > 3)) {
			throw new TclNumArgsException(interp, 1, argv,
					"channelId ?varName?");
		}

		if (argv.length == 3) {
			writeToVar = true;
			varName = argv[2].toString();
		}

		chan = TclIO.getChannel(interp, argv[1].toString());
		if (chan == null) {
			throw new TclException(interp, "can not find channel named \""
					+ argv[1].toString() + "\"");
		}

		try {
			line = TclString.newInstance(new StringBuffer(64));
			lineLen = chan.read(interp, line, TclIO.READ_LINE, 0);
			if (lineLen < 0) {
				// FIXME: Need more specific posix error codes!
				if (!chan.eof() && !chan.isBlocked(interp)) {
					throw new TclPosixException(interp, TclPosixException.EIO,
							true, "error reading \"" + argv[1].toString()
									+ "\"");
				}
				lineLen = -1;
			}
			if (writeToVar) {
				interp.setVar(varName, line, 0);
				interp.setResult(lineLen);
			} else {
				interp.setResult(line);
			}
		} catch (IOException e) {
			// e.printStackTrace(System.err);
			throw new TclRuntimeError(
					"GetsCmd.cmdProc() Error: IOException when getting "
							+ chan.getChanName() + ": " + e.getMessage());
		}

	}
}
