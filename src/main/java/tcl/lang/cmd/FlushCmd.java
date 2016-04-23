/*
 * FlushCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FlushCmd.java,v 1.2 2005/07/24 02:14:45 mdejong Exp $
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
import tcl.lang.channel.Channel;

/**
 * This class implements the built-in "flush" command in Tcl.
 */

public class FlushCmd implements Command {

	/**
	 * This procedure is invoked to process the "flush" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param argv
	 *            command arguments.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {

		Channel chan; /* The channel being operated on this method */

		if (argv.length != 2) {
			throw new TclNumArgsException(interp, 1, argv, "channelId");
		}

		chan = TclIO.getChannel(interp, argv[1].toString());
		if (chan == null) {
			throw new TclException(interp, "can not find channel named \""
					+ argv[1].toString() + "\"");
		}

		try {
			chan.flush(interp);
		} catch (IOException e) {
			throw new TclPosixException(interp, e, true, "error flushing \""+argv[1].toString()+"\"");
		}
	}
}
