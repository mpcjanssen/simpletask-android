/*
 * ExecCmd.java --
 *
 *	This file contains the Jacl implementation of the built-in Tcl "exec"
 *	command. The exec command is not available on the Mac.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ExecCmd.java,v 1.13 2006/06/30 07:57:18 mdejong Exp $
 */

package tcl.lang.cmd;

import java.io.IOException;
import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.Pipeline;
import tcl.lang.TclException;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.channel.TclByteArrayChannel;
import tcl.lang.process.Redirect;

/**
 * This class implements the built-in "exec" command in Tcl.
 */

public class ExecCmd implements Command {

	/**
	 * Invoked to process the "exec" Tcl command. See the user documentation for
	 * details on what it does.
	 * 
	 * @param interp
	 *            The current interpreter
	 * @param argv
	 *            The arguments to exec
	 * @throws TclException
	 *             on any exec error
	 */
	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {

		int firstWord; // Index to the first non-switch arg
		boolean keepNewline = false;

		// Check for a leading "-keepnewline" argument.
		label:
		for (firstWord = 1; firstWord < argv.length; firstWord++) {
			String argStr = argv[firstWord].toString();
			if ((argStr.length() > 0) && (argStr.charAt(0) == '-')) {
				switch (argStr) {
					case "-keepnewline":
						keepNewline = true;
						break;
					case "--":
						firstWord++;
						break label;
					default:
						throw new TclException(interp, "bad switch \"" + argStr + "\": must be -keepnewline or --");
				}
			} else {
				break;
			}
		}

		if (argv.length <= firstWord) {
			throw new TclNumArgsException(interp, 1, argv, "?switches? arg ?arg ...?");
		}

		/*
		 * Build the Pipeline from the argv arguments
		 */
		Pipeline pipeline = new Pipeline(interp, argv, firstWord);

		/*
		 * Inherit JVM's stdin if it wasn't redirected
		 */
		if (pipeline.getStdinRedirect() == null) {
			pipeline.setStdinRedirect(Redirect.inherit());
		}
		/*
		 * If standard output was not redirected, give Pipeline someplace for
		 * stdout if not running in background
		 */
		TclByteArrayChannel stdoutChannel = null;

		if (pipeline.getStdoutRedirect() == null && !pipeline.isExecInBackground()) {
			/* Collect stdout in a TclByteArray */
			stdoutChannel = new TclByteArrayChannel(interp);
			pipeline.setStdoutRedirect(new Redirect(stdoutChannel, false));
		}

		/*
		 * If standard error has not been redirected, do so if not running in
		 * background
		 */
		TclByteArrayChannel stderrChannel = null;

		if (pipeline.getStderrRedirect() == null && !pipeline.isExecInBackground()) {
			/* Collect stderr in a TclByteArray */
			stderrChannel = new TclByteArrayChannel(interp);
			pipeline.setStderrRedirect(new Redirect(stderrChannel, false));
		}

		/*
		 * And start the pipeline running.
		 */
		pipeline.exec();

		if (pipeline.isExecInBackground()) {
			/*
			 * If in background, return a list of fake PIDs of processes. JVM
			 * doesn't give us a way to know actual PIDs
			 */
			TclObject rv = TclList.newInstance();
			int[] pids = pipeline.getProcessIdentifiers();
			for (int pid : pids) {
				TclList.append(interp, rv, TclInteger.newInstance(pid));
			}
			interp.setResult(rv);
			/* Run waitForExitAndCleanup() in a separate thread */
			Thread t = new Thread(pipeline);
			t.setDaemon(true);
			t.setName("ExecCmd background pipeline (" + interp.toString() + ")");
			t.start();
			pipeline.throwAnyExceptions();
			return;
		}
		/* Otherwise, we are not in background mode */
		pipeline.waitForExitAndCleanup(false);

		boolean errorReturned = false;

		/* Collect any un-redirected output data */
		String stderrString = "";
		String stdoutString = "";
		if (stderrChannel != null) {
			try {
				stderrChannel.close();
			} catch (IOException e) {
				throw new TclException(interp, e.getMessage());
			}
			stderrString = stderrChannel.getTclString().toString();
			errorReturned = (stderrString.length() > 0) && !pipeline.isErrorRedirectedToResult();
		}
		if (stdoutChannel != null) {
			try {
				stdoutChannel.close();
			} catch (IOException e) {
				throw new TclException(interp, e.getMessage());
			}
			stdoutString = stdoutChannel.getTclString().toString();
		}

		/* Did any process return a non-zero status? */

		int[] pids = pipeline.getProcessIdentifiers();
		int[] exitValues = pipeline.getExitValues();
		for (int i = 0; i < exitValues.length; i++) {
			if (exitValues[i] != 0) {
				errorReturned = true;
				interp.setErrorCode(TclString.newInstance("CHILDSTATUS " + pids[i] + " " + exitValues[i]));
			}
		}

		if (errorReturned) {
			if (stderrString.length() == 0) {
				stderrString = "child process exited abnormally";
			}
			if (!keepNewline && stderrString.endsWith("\n")) {
				stderrString = stderrString.substring(0, stderrString.length() - 1);
			}
			throw new TclException(interp, stdoutString + stderrString);
		}

		/* Finally, return the result */
		if (!keepNewline && stdoutString.endsWith("\n")) {
			stdoutString = stdoutString.substring(0, stdoutString.length() - 1);
		}
		if (!keepNewline && stderrString.endsWith("\n")) {
			stderrString = stderrString.substring(0, stderrString.length() - 1);
		}
		if (pipeline.isErrorRedirectedToResult()) {
			interp.setResult(stderrString + stdoutString);
		} else {
			interp.setResult(stdoutString);
		}
		pipeline.throwAnyExceptions();
	}

}
