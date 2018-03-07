/*
 * CloseCmd.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: CloseCmd.java,v 1.2 2000/08/01 06:50:48 mo Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.Pipeline;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.channel.Channel;
import tcl.lang.channel.PipelineChannel;

/**
 * This class implements the built-in "close" command in Tcl.
 */

public class CloseCmd implements Command {
	/**
	 * This procedure is invoked to process the "close" Tcl command. See the
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
			throw new TclException(interp, "can not find channel named \"" + argv[1].toString() + "\"");
		}

		TclIO.unregisterChannel(interp, chan);

		if (chan.getBlocking() && chan instanceof PipelineChannel) {
			Pipeline pipeline = ((PipelineChannel) chan).getPipeline();
			
			int[] pids = pipeline.getProcessIdentifiers();
			int[] exitValues = pipeline.getExitValues();
			boolean errorReturned = false;

			// stuff any non-zero exit statuses into errorCode, just like exec
			for (int i = 0; i < exitValues.length; i++) {
				if (exitValues[i] != 0) {
					errorReturned = true;
					interp.setErrorCode(TclString.newInstance("CHILDSTATUS " + pids[i] + " " + exitValues[i]));
				}
			}

			// Return any stderr output in tcl exception
			String stderrOutput = ((PipelineChannel) chan).getStderrOutput();
			if (stderrOutput.length() > 0) {
				errorReturned = true;
				if (stderrOutput.endsWith("\n")) {
					stderrOutput = stderrOutput.substring(0, stderrOutput.length() - 1);
				}
			} else {
				if (errorReturned) {
					stderrOutput = "child process exited abnormally";
				}
			}
			if (errorReturned) {
				throw new TclException(interp, stderrOutput);
			}
			pipeline.throwAnyExceptions();
		}
	}
}
