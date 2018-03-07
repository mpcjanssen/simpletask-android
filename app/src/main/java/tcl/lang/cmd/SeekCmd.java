/*
 * SeekCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: SeekCmd.java,v 1.3 2003/03/08 03:42:44 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import java.io.IOException;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.channel.Channel;

/**
 * This class implements the built-in "seek" command in Tcl.
 */

public class SeekCmd implements Command {

	static final private String validOrigins[] = { "start", "current", "end" };

	static final int OPT_START = 0;
	static final int OPT_CURRENT = 1;
	static final int OPT_END = 2;

	/**
	 * This procedure is invoked to process the "seek" Tcl command. See the user
	 * documentation for details on what it does.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {

		Channel chan; /* The channel being operated on this method */
		int mode; /*
				 * Stores the search mode, either beg, cur or end of file. See
				 * the TclIO class for more info
				 */

		if (argv.length != 3 && argv.length != 4) {
			throw new TclNumArgsException(interp, 1, argv,
					"channelId offset ?origin?");
		}

		// default is the beginning of the file

		mode = TclIO.SEEK_SET;
		if (argv.length == 4) {
			int index = TclIndex
					.get(interp, argv[3], validOrigins, "origin", 0);

			switch (index) {
			case OPT_START: {
				mode = TclIO.SEEK_SET;
				break;
			}
			case OPT_CURRENT: {
				mode = TclIO.SEEK_CUR;
				break;
			}
			case OPT_END: {
				mode = TclIO.SEEK_END;
				break;
			}
			}
		}

		chan = TclIO.getChannel(interp, argv[1].toString());
		if (chan == null) {
			throw new TclException(interp, "can not find channel named \""
					+ argv[1].toString() + "\"");
		}
		long offset = TclInteger.getLong(interp, argv[2]);

		try {
			chan.seek(interp, offset, mode);
		} catch (IOException e) {
			// FIXME: Need to figure out Tcl specific error conditions.
			// Should we also wrap an IOException in a ReflectException?
			throw new TclRuntimeError(
					"SeekCmd.cmdProc() Error: IOException when seeking "
							+ chan.getChanName() + ":" + e.toString());
		}
	}
}
