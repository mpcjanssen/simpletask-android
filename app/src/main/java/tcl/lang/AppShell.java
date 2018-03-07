/*
 * Shell.java --
 *
 *	Implements the start up shell for Tcl by reading resource name from Manifest attribute "JTcl-Main"
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 */

package tcl.lang;

import java.io.InputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The AppShell class expects to load and execute a Tcl resource file by
 * retrieving the JTcl-Main attribute from the manifest. See paraffin.tcl to
 * build a conforming JAR file.
 */

public class AppShell {

	/**
	 * Set to true to force tcl_interactive to 0 in the shell
	 */
	public static boolean forceNonInteractive = false;

	/**
	 * The attribute in META-INF/MANIFEST.MF denoting the resource file to
	 * execute.
	 */
	private static String JTCL_MAIN = "JTcl-Main";

	/**
	 * Main program for AppShell.
	 * 
	 * Results: None.
	 * 
	 * Side effects: This procedure initializes the Tcl world and then starts
	 * interpreting commands; almost anything could happen, depending on the
	 * script being interpreted.
	 * 
	 */

	public static void main(String args[]) {
		Manifest mf;
		try {
			InputStream inputStream = AppShell.class.getProtectionDomain().getClassLoader()
					.getResourceAsStream("META-INF/MANIFEST.MF");
			mf = new Manifest(inputStream);
			inputStream.close();
		} catch (Exception e) {
			throw new TclRuntimeError("META-INF/MANIFEST.MF does not exist or not running from a .jar file: " + e);
		}

		String fileName = mf.getMainAttributes().getValue(JTCL_MAIN); // only in
																		// 1.6+

		if (fileName == null) {
			// java 1.5 - have to read the app jar file directly
			ZipInputStream zipin = null;
			try {
				InputStream inputStream = AppShell.class.getProtectionDomain().getCodeSource().getLocation()
						.openConnection().getInputStream();
				zipin = new ZipInputStream(inputStream);
				ZipEntry entry = zipin.getNextEntry();
				while (entry != null) {
					if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
						mf = new Manifest(zipin);
						break;
					}
					entry = zipin.getNextEntry();
				}
			} catch (Exception e) {
				// ignore
			} finally {
				try {
					zipin.close();
				} catch (Exception e) {
					// ignore
				}
			}
			fileName = mf.getMainAttributes().getValue(JTCL_MAIN);
		}

		if (fileName == null) {
			throw new TclRuntimeError("META-INF/MANIFEST.MF does not contain \"JTcl-Main\" attribute");
		} else {
			while (fileName.startsWith("/")) {
				fileName = fileName.substring(1);
			}
			while (fileName.endsWith("/")) {
				fileName = fileName.substring(0, fileName.length() - 1);
			}
		}

		// Create the interpreter. This will also create the built-in
		// Tcl commands.

		Interp interp = new Interp();

		TclObject argv = TclList.newInstance();
		argv.preserve();
		try {
			int i = 0;
			int argc = args.length;

			interp.setVar("argv0", "resource:/" + fileName, TCL.GLOBAL_ONLY);
			interp.setVar("tcl_interactive", "0", TCL.GLOBAL_ONLY);

			for (; i < args.length; i++) {
				TclList.append(interp, argv, TclString.newInstance(args[i]));
			}
			interp.setVar("argv", argv, TCL.GLOBAL_ONLY);
			interp.setVar("argc", java.lang.Integer.toString(argc), TCL.GLOBAL_ONLY);

			int lastSlash = fileName.lastIndexOf('/');
			String dir = "resource:/" + (lastSlash >= 0 ? fileName.substring(0, lastSlash) : "");
			TclObject auto_path = null;
			try {
				auto_path = interp.getVar("auto_path", TCL.GLOBAL_ONLY);
			} catch (TclException e) {
				// ignore
			}
			if (auto_path == null) {
				interp.setVar("auto_path", TclString.newInstance(dir), TCL.GLOBAL_ONLY);
			} else {
				TclList.append(interp, auto_path, TclString.newInstance(dir));
			}

		} catch (TclException e) {
			throw new TclRuntimeError("unexpected TclException: " + e);
		} finally {
			argv.release();
		}

		int exitCode = 0;
		try {
			interp.evalResource(fileName);
		} catch (TclException e) {
			int code = e.getCompletionCode();
			if (code == TCL.RETURN) {
				code = interp.updateReturnInfo();
				if (code != TCL.OK) {
					System.err.println("command returned bad code: " + code);
					exitCode = 2;
				}
			} else if (code == TCL.ERROR) {
				try {
					TclObject errorInfo = interp.getVar("errorInfo", TCL.GLOBAL_ONLY);
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
}
