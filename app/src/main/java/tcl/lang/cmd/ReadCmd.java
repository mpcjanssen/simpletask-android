/*
 * ReadCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ReadCmd.java,v 1.8 2003/03/08 03:42:44 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import java.io.IOException;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclByteArray;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclInteger;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.TclString;
import tcl.lang.channel.Channel;

/**
 * This class implements the built-in "read" command in Tcl.
 */

public class ReadCmd implements Command {

	/**
	 * This procedure is invoked to process the "read" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {

		Channel chan; // The channel being operated on this
		// method
		int i = 1; // Index to the next arg in argv
		int toRead = 0; // Number of bytes or chars to read from channel
		boolean readAll = true; // If true read-all else toRead
		boolean noNewline = false; // If true, strip the newline if there
		TclObject result;

		if ((argv.length != 2) && (argv.length != 3)) {
			errorWrongNumArgs(interp, argv[0].toString());
		}

		if (argv[i].toString().equals("-nonewline")) {
			noNewline = true;
			i++;
		}

		if (i == argv.length) {
			errorWrongNumArgs(interp, argv[0].toString());
		}

		chan = TclIO.getChannel(interp, argv[i].toString());
		if (chan == null) {
			throw new TclException(interp, "can not find channel named \""
					+ argv[i].toString() + "\"");
		}

		// Consumed channel name.

		i++;

		if (i==argv.length) {
			readAll = true;
		} else {
			readAll = false;
			if (Character.isDigit(argv[i].toString().charAt(0))) {
				toRead = TclInteger.getInt(interp, argv[i]);
			} else {
				// this is a wierd error, but that's what io-32.3, iocmd-4.9 tests want
				throw new TclException(interp, "bad argument \""+argv[i]+"\": should be \"nonewline\"");
			}
			
		}
	
		try {
			if (chan.getEncoding() == null && ! noNewline) {
				result = TclByteArray.newInstance();
			} else {
				result = TclString.newInstance(new StringBuffer(64));
			}
			if (readAll) {
				chan.read(interp, result, TclIO.READ_ALL, 0);

				// If -nonewline was specified, and we have not hit EOF
				// and the last char is a "\n", then remove it and return.

				if (noNewline && chan.eof()) {
					String inStr = result.toString();
					if (inStr.endsWith("\n")) {
						interp.setResult(inStr.substring(0, inStr.length()-1));
					} else {
						interp.setResult(result);
					}
				} else {
					interp.setResult(result);
				}
			} else {
				chan.read(interp, result, TclIO.READ_N_BYTES,
						toRead);
				interp.setResult(result);
			}

		} catch (IOException e) {
			throw new TclRuntimeError(
					"ReadCmd.cmdProc() Error: IOException when reading "
							+ chan.getChanName());
		}
	}

	/**
	 * A unique error msg is printed for read, therefore dont call this instead
	 * of the standard TclNumArgsException().
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param cmd
	 *            the name of the command (extracted form argv[0] of cmdProc)
	 */

	private void errorWrongNumArgs(Interp interp, String cmd)
			throws TclException {
		throw new TclException(interp, "wrong # args: should be \""
				+ "read channelId ?numChars?\" "
				+ "or \"read ?-nonewline? channelId\"");
	}

}
