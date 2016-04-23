/*
 * Shell.java --
 *
 *	Implements the start up shell for Tcl.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Shell.java,v 1.16 2006/04/27 02:16:13 mdejong Exp $
 */

package tcl.lang;

import tcl.lang.cmd.EncodingCmd;


/**
 * The Shell class is similar to the Tclsh program: you can use it to execute a
 * Tcl script or enter Tcl command interactively at the command prompt.
 */

public class Shell {

	/**
	 * Set to true to force tcl_interactive to 0 in the shell
	 */
	public static boolean forceNonInteractive = false;
	
	/**
	 * Main program for tclsh and most other Tcl-based applications.
	 * 
	 * Results: None.
	 * 
	 * Side effects: This procedure initializes the Tcl world and then starts
	 * interpreting commands; almost anything could happen, depending on the
	 * script being interpreted.
	 * 
	 */

	public static void main(String args[]) {
		String fileName = null;
		String encoding = EncodingCmd.systemTclEncoding;
		
		// Create the interpreter. This will also create the built-in
		// Tcl commands.

		Interp interp = new Interp();

		// if first options are -encoding name, set the encoding of command line script
		int i = 0;
		if (args.length > i && args[i].equals("-encoding")) {
			++i;
			if (args.length > i) {
				encoding = args[i++];
			}
		}
		
		// Make command-line arguments available in the Tcl variables "argc"
		// and "argv". If the first argument doesn't start with a "-" then
		// strip it off and use it as the name of a script file to process.
		// We also set the argv0 and tcl_interactive vars here.

		
		if ((args.length > i) && !(args[i].startsWith("-"))) {
			fileName = args[i++];
		}

		TclObject argv = TclList.newInstance();
		argv.preserve();
		try {
			int argc = 0;
			if (fileName == null) {
				interp.setVar("argv0", "tcl.lang.Shell", TCL.GLOBAL_ONLY);
				interp.setVar("tcl_interactive", forceNonInteractive ? "0" : "1", TCL.GLOBAL_ONLY);
			} else {
				interp.setVar("argv0", fileName, TCL.GLOBAL_ONLY);
				interp.setVar("tcl_interactive", "0", TCL.GLOBAL_ONLY);
			}
			for (; i < args.length; i++) {
				TclList.append(interp, argv, TclString.newInstance(args[i]));
				++argc;
			}
			interp.setVar("argv", argv, TCL.GLOBAL_ONLY);
			interp.setVar("argc", java.lang.Integer.toString(argc),
					TCL.GLOBAL_ONLY);
		} catch (TclException e) {
			throw new TclRuntimeError("unexpected TclException: " + e);
		} finally {
			argv.release();
		}

		// Normally we would do application specific initialization here.
		// However, that feature is not currently supported.

		// If a script file was specified then just source that file
		// and quit.

		if (fileName != null) {
			int exitCode = 0;
			try {
				String javaEncoding = EncodingCmd.getJavaName(encoding);
				if (javaEncoding==null) {
					System.err.println("unknown encoding \""
							+ encoding + "\"");
					exitCode = 2;
				} else {
					interp.evalFile(fileName, javaEncoding);
				}
			} catch (TclException e) {
				int code = e.getCompletionCode();
				if (code == TCL.RETURN) {
					code = interp.updateReturnInfo();
					if (code != TCL.OK) {
						System.err
								.println("command returned bad code: " + code);
						exitCode = 2;
					}
				} else if (code == TCL.ERROR) {
					try {
						TclObject errorInfo = interp.getVar("errorInfo",TCL.GLOBAL_ONLY);
						System.err.println(errorInfo.toString());
					} catch (TclException e1) {
						System.err.println(interp.getResult().toString());				
					}
					exitCode = 1;
				} else {
					System.err.println("command returned bad code: " + code);
					exitCode = 2;
				}
			}

			// Note that if the above interp.evalFile() returns the main
			// thread will exit. This may bring down the VM and stop
			// the execution of Tcl.
			//
			// If the script needs to handle events, it must call
			// vwait or do something similar.
			//
			// Note that the script can create AWT widgets. This will
			// start an AWT event handling thread and keep the VM up. However,
			// the interpreter thread (the same as the main thread) would
			// have exited and no Tcl scripts can be executed.

			interp.dispose();
			System.exit(exitCode);
		}

		if (fileName == null) {
			// We are running in interactive mode. Start the ConsoleThread
			// that loops, grabbing stdin and passing it to the interp.

			ConsoleThread consoleThread = new ConsoleThread(interp);
			consoleThread.setDaemon(true);
			consoleThread.start();

			// Loop forever to handle user input events in the command line.
			// This method will loop until "exit" is called or the interp
			// is interrupted.

			Notifier notifier = interp.getNotifier();

			try {
				Notifier.processTclEvents(notifier);
			} finally {
				interp.dispose();
			}
		}
	}
} 
