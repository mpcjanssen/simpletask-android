/*
 * InfoCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: InfoCmd.java,v 1.16 2009/06/18 18:16:05 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import tcl.lang.CallFrame;
import tcl.lang.Command;
import tcl.lang.Expression;
import tcl.lang.Interp;
import tcl.lang.Namespace;
import tcl.lang.Procedure;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.Util;
import tcl.lang.Var;
import tcl.lang.WrappedCommand;

/**
 * This class implements the built-in "info" command in Tcl.
 */

public class InfoCmd implements Command {
	static final private String validCmds[] = { "args", "body", "cmdcount", "commands", "complete", "default",
			"exists", "functions", "globals", "hostname", "level", "library", "loaded", "locals", "nameofexecutable",
			"patchlevel", "procs", "script", "sharedlibextension", "tclversion", "vars" };

	static final int OPT_ARGS = 0;
	static final int OPT_BODY = 1;
	static final int OPT_CMDCOUNT = 2;
	static final int OPT_COMMANDS = 3;
	static final int OPT_COMPLETE = 4;
	static final int OPT_DEFAULT = 5;
	static final int OPT_EXISTS = 6;
	static final int OPT_FUNCTIONS = 7;
	static final int OPT_GLOBALS = 8;
	static final int OPT_HOSTNAME = 9;
	static final int OPT_LEVEL = 10;
	static final int OPT_LIBRARY = 11;
	static final int OPT_LOADED = 12;
	static final int OPT_LOCALS = 13;
	static final int OPT_NAMEOFEXECUTABLE = 14;
	static final int OPT_PATCHLEVEL = 15;
	static final int OPT_PROCS = 16;
	static final int OPT_SCRIPT = 17;
	static final int OPT_SHAREDLIBEXTENSION = 18;
	static final int OPT_TCLVERSION = 19;
	static final int OPT_VARS = 20;

	/**
	 * Tcl_InfoObjCmd -> InfoCmd.cmdProc
	 * 
	 * This procedure is invoked to process the "info" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * @param interp
	 *            the current interpreter.
	 * @param objv
	 *            command arguments.
	 * @exception TclException
	 *                if wrong # of args or invalid argument(s).
	 */
	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		int index;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv, "option ?arg arg ...?");
		}
		index = TclIndex.get(interp, objv[1], validCmds, "option", 0);

		switch (index) {
		case OPT_ARGS:
			InfoArgsCmd(interp, objv);
			break;
		case OPT_BODY:
			InfoBodyCmd(interp, objv);
			break;
		case OPT_CMDCOUNT:
			InfoCmdCountCmd(interp, objv);
			break;
		case OPT_COMMANDS:
			InfoCommandsCmd(interp, objv);
			break;
		case OPT_COMPLETE:
			InfoCompleteCmd(interp, objv);
			break;
		case OPT_DEFAULT:
			InfoDefaultCmd(interp, objv);
			break;
		case OPT_EXISTS:
			InfoExistsCmd(interp, objv);
			break;
		case OPT_FUNCTIONS:
			InfoFunctionsCmd(interp, objv);
			break;
		case OPT_GLOBALS:
			InfoGlobalsCmd(interp, objv);
			break;
		case OPT_HOSTNAME:
			InfoHostnameCmd(interp, objv);
			break;
		case OPT_LEVEL:
			InfoLevelCmd(interp, objv);
			break;
		case OPT_LIBRARY:
			InfoLibraryCmd(interp, objv);
			break;
		case OPT_LOADED:
			InfoLoadedCmd(interp, objv);
			break;
		case OPT_LOCALS:
			InfoLocalsCmd(interp, objv);
			break;
		case OPT_NAMEOFEXECUTABLE:
			InfoNameOfExecutableCmd(interp, objv);
			break;
		case OPT_PATCHLEVEL:
			InfoPatchLevelCmd(interp, objv);
			break;
		case OPT_PROCS:
			InfoProcsCmd(interp, objv);
			break;
		case OPT_SCRIPT:
			InfoScriptCmd(interp, objv);
			break;
		case OPT_SHAREDLIBEXTENSION:
			InfoSharedlibCmd(interp, objv);
			break;
		case OPT_TCLVERSION:
			InfoTclVersionCmd(interp, objv);
			break;
		case OPT_VARS:
			InfoVarsCmd(interp, objv);
			break;
		}
		return;
	}

	/**
	 * Called to implement the "info args" command that returns the argument list for a procedure. Handles the following
	 * syntax:
	 * 
	 * info args procName
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoArgsCmd(Interp interp, TclObject[] objv) throws TclException {
		String name;
		Procedure proc;
		TclObject listObj;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "procname");
		}
		name = objv[2].toString();
		proc = Procedure.findProc(interp, name);
		if (proc == null) {
			throw new TclException(interp, "\"" + name + "\" isn't a procedure");
		}

		// Build a return list containing the arguments.

		listObj = TclList.newInstance();
		for (int i = 0; i < proc.argList.length; i++) {
			TclObject s = TclString.newInstance(proc.argList[i][0]);
			TclList.append(interp, listObj, s);
		}
		interp.setResult(listObj);
		return;
	}

	/**
	 * Called to implement the "info body" command that returns the body for a procedure. Handles the following syntax:
	 * 
	 * info body procName
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoBodyCmd(Interp interp, TclObject[] objv) throws TclException {
		String name;
		Procedure proc;
		TclObject body, result;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "procname");
		}
		name = objv[2].toString();
		proc = Procedure.findProc(interp, name);
		if (proc == null) {
			throw new TclException(interp, "\"" + name + "\" isn't a procedure");
		}

		interp.setResult(proc.body.toString());
		return;
	}

	/**
	 * Called to implement the "info cmdcount" command that returns the number of commands that have been executed.
	 * Handles the following syntax:
	 * 
	 * info cmdcount
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoCmdCountCmd(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, null);
		}
		interp.setResult(interp.cmdCount);
		return;
	}

	/**
	 * Called to implement the "info commands" command that returns the list of commands in the interpreter that match
	 * an optional pattern. The pattern, if any, consists of an optional sequence of namespace names separated by "::"
	 * qualifiers, which is followed by a glob-style pattern that restricts which commands are returned. Handles the
	 * following syntax:
	 * 
	 * info commands ?pattern?
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoCommandsCmd(Interp interp, TclObject[] objv) throws TclException {
		String cmdName, pattern, simplePattern;
		Namespace ns;
		Namespace globalNs = Namespace.getGlobalNamespace(interp);
		Namespace currNs = Namespace.getCurrentNamespace(interp);
		TclObject list, elemObj;
		boolean specificNsInPattern = false; // Init. to avoid compiler warning.
		WrappedCommand cmd;

		// Get the pattern and find the "effective namespace" in which to
		// list commands.

		if (objv.length == 2) {
			simplePattern = null;
			ns = currNs;
			specificNsInPattern = false;
		} else if (objv.length == 3) {
			// From the pattern, get the effective namespace and the simple
			// pattern (no namespace qualifiers or ::'s) at the end. If an
			// error was found while parsing the pattern, return it. Otherwise,
			// if the namespace wasn't found, just leave ns NULL: we will
			// return an empty list since no commands there can be found.

			pattern = objv[2].toString();

			Namespace.GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
			Namespace.getNamespaceForQualName(interp, pattern, null, 0, gnfqnr);
			ns = gnfqnr.ns;
			simplePattern = gnfqnr.simpleName;

			if (ns != null) { // we successfully found the pattern's ns
				specificNsInPattern = (simplePattern.compareTo(pattern) != 0);
			}
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?pattern?");
		}

		// Scan through the effective namespace's command table and create a
		// list with all commands that match the pattern. If a specific
		// namespace was requested in the pattern, qualify the command names
		// with the namespace name.

		list = TclList.newInstance();

		if (ns != null) {
			for (Entry<String, WrappedCommand> stringWrappedCommandEntry1 : ns.cmdTable.entrySet()) {
				Entry entry = (Entry) stringWrappedCommandEntry1;
				cmdName = (String) entry.getKey();

				if ((simplePattern == null) || Util.stringMatch(cmdName, simplePattern)) {
					if (specificNsInPattern) {
						cmd = (WrappedCommand) entry.getValue();
						elemObj = TclString.newInstance(interp.getCommandFullName(cmd));
					} else {
						elemObj = TclString.newInstance(cmdName);
					}
					TclList.append(interp, list, elemObj);
				}
			}

			// If the effective namespace isn't the global :: namespace, and a
			// specific namespace wasn't requested in the pattern, then add in
			// all global :: commands that match the simple pattern. Of course,
			// we add in only those commands that aren't hidden by a command in
			// the effective namespace.

			if ((ns != globalNs) && !specificNsInPattern) {
				for (Entry<String, WrappedCommand> stringWrappedCommandEntry : globalNs.cmdTable.entrySet()) {
					Entry entry = (Entry) stringWrappedCommandEntry;
					cmdName = (String) entry.getKey();
					if ((simplePattern == null) || Util.stringMatch(cmdName, simplePattern)) {
						if (ns.cmdTable.get(cmdName) == null) {
							TclList.append(interp, list, TclString.newInstance(cmdName));
						}
					}
				}
			}
		}

		interp.setResult(list);
		return;
	}

	/**
	 * Called to implement the "info complete" command that determines whether a string is a complete Tcl command.
	 * Handles the following syntax:
	 * 
	 * info complete command
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoCompleteCmd(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "command");
		}

		interp.setResult(Interp.commandComplete(objv[2].toString()));
		return;
	}

	/**
	 * Called to implement the "info default" command that returns the default value for a procedure argument. Handles
	 * the following syntax:
	 * 
	 * info default procName arg varName
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoDefaultCmd(Interp interp, TclObject[] objv) throws TclException {
		String procName, argName, varName;
		Procedure proc;
		TclObject valueObj;

		if (objv.length != 5) {
			throw new TclNumArgsException(interp, 2, objv, "procname arg varname");
		}

		procName = objv[2].toString();
		argName = objv[3].toString();
		proc = Procedure.findProc(interp, procName);
		if (proc == null) {
			throw new TclException(interp, "\"" + procName + "\" isn't a procedure");
		}

		for (int i = 0; i < proc.argList.length; i++) {
			if (argName.equals(proc.argList[i][0].toString())) {
				varName = objv[4].toString();
				try {
					if (proc.argList[i][1] != null) {
						interp.setVar(varName, proc.argList[i][1], 0);
						interp.setResult(1);
					} else {
						interp.setVar(varName, "", 0);
						interp.setResult(0);
					}
				} catch (TclException excp) {
					throw new TclException(interp, "couldn't store default value in variable \"" + varName + "\"");
				}
				return;
			}
		}
		throw new TclException(interp, "procedure \"" + procName + "\" doesn't have an argument \"" + argName + "\"");
	}

	/**
	 * Called to implement the "info exists" command that determines whether a variable exists. Handles the following
	 * syntax:
	 * 
	 * info exists varName
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoExistsCmd(Interp interp, TclObject[] objv) throws TclException {
		String varName;
		Var var = null;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "varName");
		}

		varName = objv[2].toString();
		Var[] result = Var.lookupVar(interp, varName, null, 0, "access", false, false);
		if (result != null) {
			var = result[0];
		}

		if ((var != null) && !var.isVarUndefined()) {
			interp.setResult(true);
		} else {
			interp.setResult(false);
		}

		return;
	}

	/**
	 * Called to implement the "info functions" command that returns the list of math functions currently defined
	 * matching an optional pattern. Handles the following syntax:
	 * 
	 * info functions ?pattern?*
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private void InfoFunctionsCmd(Interp interp, TclObject[] objv) throws TclException {
		String varName, pattern;
		Expression mathFns = new Expression();
		Var var;
		TclObject list;

		if (objv.length == 2) {
			pattern = null;
		} else if (objv.length == 3) {
			pattern = objv[2].toString();
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?pattern?");
		}

		// Scan through the global :: namespace's variable table and create a
		// list of all global variables that match the pattern.

		list = TclList.newInstance();

		Set set = mathFns.mathFuncTable.entrySet();

		for (Object aSet : set) {
			Entry entry = (Entry) aSet;
			varName = (String) entry.getKey();

			if ((pattern == null) || Util.stringMatch(varName, pattern)) {
				TclList.append(interp, list, TclString.newInstance(varName));
			}
		}

		interp.setResult(list);
		return;
	}

	/**
	 * Called to implement the "info globals" command that returns the list of global variables matching an optional
	 * pattern. Handles the following syntax:
	 * 
	 * info globals ?pattern?*
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoGlobalsCmd(Interp interp, TclObject[] objv) throws TclException {
		String varName, pattern;
		Namespace globalNs = Namespace.getGlobalNamespace(interp);
		Var var;
		TclObject list;

		if (objv.length == 2) {
			pattern = null;
		} else if (objv.length == 3) {
			pattern = NamespaceCmd.tail(objv[2].toString());
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?pattern?");
		}

		// Scan through the global :: namespace's variable table and create a
		// list of all global variables that match the pattern.

		list = TclList.newInstance();

		for (Entry<String, Var> stringVarEntry : globalNs.varTable.entrySet()) {
			Entry entry = (Entry) stringVarEntry;
			varName = (String) entry.getKey();
			var = (Var) entry.getValue();
			if (var.isVarUndefined()) {
				continue;
			}
			if ((pattern == null) || Util.stringMatch(varName, pattern)) {
				TclList.append(interp, list, TclString.newInstance(varName));
			}
		}

		interp.setResult(list);
		return;
	}

	/**
	 * Called to implement the "info hostname" command that returns the host name. Handles the following syntax:
	 * 
	 * info hostname
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoHostnameCmd(Interp interp, TclObject[] objv) throws TclException {
		String name = null;

		if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, null);
		}

		try {
			name = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
		}

		if (name != null) {
			interp.setResult(name);
			return;
		} else {
			interp.setResult("unable to determine name of host");
			return;
		}
	}

	/**
	 * Called to implement the "info level" command that returns information about the call stack. Handles the following
	 * syntax:
	 * 
	 * info level ?number?
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoLevelCmd(Interp interp, TclObject[] objv) throws TclException {
		int level;
		CallFrame frame;
		TclObject list;

		if (objv.length == 2) { // just "info level"
			if (interp.varFrame == null) {
				interp.setResult(0);
			} else {
				interp.setResult(interp.varFrame.level);
			}
			return;
		} else if (objv.length == 3) {
			level = TclInteger.getInt(interp, objv[2]);

			if (level <= 0) {
				if (interp.varFrame == null) {
					throw new TclException(interp, "bad level \"" + objv[2].toString() + "\"");
				}

				level += interp.varFrame.level;
			}

			for (frame = interp.varFrame; frame != null; frame = frame.callerVar) {
				if (frame.level == level) {
					break;
				}
			}
			if ((frame == null) || frame.objv == null) {
				throw new TclException(interp, "bad level \"" + objv[2].toString() + "\"");
			}

			list = TclList.newInstance();
			for (int i = 0; i < frame.objv.length; i++) {
				TclList.append(interp, list, TclString.newInstance(frame.objv[i]));
			}
			interp.setResult(list);
			return;
		}

		throw new TclNumArgsException(interp, 2, objv, "?number?");
	}

	/**
	 * Called to implement the "info library" command that returns the library directory for the Tcl installation.
	 * Handles the following syntax:
	 * 
	 * info library
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoLibraryCmd(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, null);
		}
		try {
			interp.setResult(interp.getVar("tcl_library", TCL.GLOBAL_ONLY));
			return;
		} catch (TclException e) {
			// If the variable has not been defined
			throw new TclException(interp, "no library has been specified for Tcl");
		}
	}

	/**
	 * Called to implement the "info loaded" command that returns the packages that have been loaded into an
	 * interpreter. Handles the following syntax:
	 * 
	 * info loaded ?interp?
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoLoadedCmd(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length != 2 && objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "?interp?");
		}
		if (objv.length == 3) {
			Interp slaveInterp = InterpCmd.getInterp(interp, objv[2]);
			if (slaveInterp == null) {
				throw new TclException(interp, "could not find interpreter \"" + objv[2].toString() + "\"");
			}
		}
		// Since the 'load' command is not supported, always
		// return a null list for 'info loaded'
		interp.setResult(TclString.newInstance(""));
	}

	/**
	 * Called to implement the "info locals" command to return a list of local variables that match an optional pattern.
	 * Handles the following syntax:
	 * 
	 * info locals ?pattern?
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoLocalsCmd(Interp interp, TclObject[] objv) throws TclException {
		String pattern;
		TclObject list;

		if (objv.length == 2) {
			pattern = null;
		} else if (objv.length == 3) {
			pattern = objv[2].toString();
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?pattern?");
		}

		if (interp.varFrame == null || !interp.varFrame.isProcCallFrame) {
			return;
		}

		// Return a list containing names of first the compiled locals (i.e. the
		// ones stored in the call frame), then the variables in the local hash
		// table (if one exists).

		list = TclList.newInstance();
		Var.AppendLocals(interp, list, pattern, false);
		interp.setResult(list);
		return;
	}

	/**
	 * Called to implement the "info nameofexecutable" command that returns the name of the binary file running this
	 * application. This implementation attempts to locate the Java VM executable, creates a new temporary file as a
	 * shell/bat script to invoke the JVM executable with the current classpath and shell, and returns the temporary
	 * file name.
	 * 
	 * @param interp
	 *            Interpreter in which to evaluate command
	 * @param objv
	 *            arguments 'info nameofexecutable'
	 * @throws TclException
	 *             on any error
	 */

	private static void InfoNameOfExecutableCmd(Interp interp, TclObject[] objv) throws TclException {

		if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, null);
		}

		String nameOfExecutable = interp.getNameOfExecutable();
		boolean createExecFile = false;
		File execFile = null;

		if (nameOfExecutable == null) {
			// executable file name not set
			createExecFile = true;
		} else {
			// check if temporary executable file still exists
			execFile = new File(nameOfExecutable);
			if (!execFile.exists()) {
				createExecFile = true;
			}
		}

		if (createExecFile) {
			// create a new temp file to execute JTcl interpreter
			boolean isWindows = Util.isWindows();
			String eol = isWindows ? "\r\n" : "\n";
			String suffix = isWindows ? ".bat" : ".sh";
			try {
				execFile = File.createTempFile("jtcl", suffix);
				nameOfExecutable = execFile.getCanonicalPath();
			} catch (IOException e) {
				throw new TclException(interp, "Could not create temp file for [info nameofexecutable]");
			}

			// The JDK provides direct no means to learn the name of the executable
			// that launched the application. We'll try to find the java vm, but use
			// 'java' as a default.

			String javaExecutable = "java";
			String javaHome = System.getProperty("java.home");
			if (javaHome != null) {
				// test java.home/bin/java
				String execSuffix = isWindows ? ".exe" : "";
				File javaPath = new File(new File(new File(javaHome), "bin"), "java" + execSuffix);
				if (javaPath.exists()) {
					javaExecutable = javaPath.getPath();
				} else {
					// IBM's JDK on AIX uses strange locations for the executables (from Apache Maven 'bin/mvn')
					// test java.home/jre/sh/java
					javaPath = new File(new File(new File(new File(javaHome), "jre"), "sh"), "java");
					if (javaPath.exists()) {
						javaExecutable = javaPath.getPath();
					}
				}
			}

			String execLine = isWindows ? "@echo off" + eol : "#!" + eol + "exec ";
			execLine += "\"" + javaExecutable + "\"";
			String classpath = System.getProperty("java.class.path");
			if (classpath != null) {
				execLine += " -classpath \"" + classpath + "\" ";
			}

			// add the top level class
			execLine += interp.getShellClassName();

			// add shell parameters
			execLine += (isWindows ? " %*" : " ${1+\"$@\"}") + eol;

			// write the file and set delete on exit
			FileWriter out = null;
			try {
				out = new FileWriter(execFile);
				out.write(execLine);
				out.close();
			} catch (Exception e) {
				throw new TclException(interp, "Could not write temp file [info nameofexecutable]");
			}
			execFile.deleteOnExit();

			// set as executable
			if (!isWindows) {
				try {
					Process process = Runtime.getRuntime().exec("chmod +x " + execFile.getPath());
					process.waitFor();
					process.destroy();
				} catch (Exception e) {
					throw new TclException(interp, "Couldn't set executable file as executable [name of executable]");
				}
			}

			// cache this temp file name in Interp
			interp.setNameOfExecutable(nameOfExecutable);
		}

		interp.setResult(nameOfExecutable);
		return;
	}

	/**
	 * Called to implement the "info patchlevel" command that returns the default value for an argument to a procedure.
	 * Handles the following syntax:
	 * 
	 * info patchlevel
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoPatchLevelCmd(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, null);
		}

		interp.setResult(interp.getVar("tcl_patchLevel", TCL.GLOBAL_ONLY));
		return;
	}

	/**
	 * Called to implement the "info procs" command that returns the procedures in the current namespace that match an
	 * optional pattern. Handles the following syntax:
	 * 
	 * info procs ?pattern?
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoProcsCmd(Interp interp, TclObject[] objv) throws TclException {
		String cmdName, pattern;
		Namespace namespace = Namespace.getCurrentNamespace(interp);
		WrappedCommand cmd, realCmd;
		TclObject list;
		String namespaceName = null;

		if (objv.length == 2) {
			pattern = null;
		} else if (objv.length == 3) {
			pattern = objv[2].toString();
			/* Is the pattern qualified with a namespace? */
			int nsEnd = pattern.lastIndexOf("::");
			if (nsEnd >= 0) {
				namespaceName = pattern.substring(0, nsEnd + 2);
				namespace = Namespace.findNamespace(interp, namespaceName, null, 0);
				pattern = pattern.substring(nsEnd + 2);
			}
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?pattern?");
		}

		// Scan through the current namespace's command table and return a list
		// of all procs that match the pattern.

		list = TclList.newInstance();
		if (namespace != null) {
			for (Entry<String, WrappedCommand> entry : namespace.cmdTable.entrySet()) {
				cmdName = entry.getKey();
				cmd = entry.getValue();

				// If the command isn't itself a proc, it still might be an
				// imported command that points to a "real" proc in a different
				// namespace.

				realCmd = Namespace.getOriginalCommand(cmd);

				if (Procedure.isProc(cmd) || ((realCmd != null) && Procedure.isProc(realCmd))) {
					if ((pattern == null) || Util.stringMatch(cmdName, pattern)) {
						String result = (namespaceName == null ? "" : namespace.toString() + "::") + cmdName;
						if (result.startsWith("::::")) {
							// global namespace may have added it's own :: namespace separator,
							// if so, strip off beginning ::
							result = result.substring(2);
						}
						TclList.append(interp, list, TclString.newInstance(result));
					}
				}
			}
		}

		interp.setResult(list);
		return;
	}

	/**
	 * Called to implement the "info script" command that returns the script file that is currently being evaluated.
	 * Handles the following syntax:
	 * 
	 * info script ?filename?
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoScriptCmd(Interp interp, TclObject[] objv) throws TclException {

		if (objv.length == 3) {
			interp.scriptFile = objv[2].toString();
		} else if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, "?filename?");
		}

		interp.setResult(interp.scriptFile);
		return;
	}

	/**
	 * Called to implement the "info sharedlibextension" command that returns the file extension used for shared
	 * libraries. Handles the following syntax:
	 * 
	 * info sharedlibextension
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoSharedlibCmd(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, null);
		}
		interp.setResult(".jar");
		return;
	}

	/**
	 * Called to implement the "info tclversion" command that returns the version number for this Tcl library. Handles
	 * the following syntax:
	 * 
	 * info tclversion
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoTclVersionCmd(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, null);
		}

		interp.setResult(interp.getVar("tcl_version", TCL.GLOBAL_ONLY));
		return;
	}

	/**
	 * Called to implement the "info vars" command that returns the list of variables in the interpreter that match an
	 * optional pattern. The pattern, if any, consists of an optional sequence of namespace names separated by "::"
	 * qualifiers, which is followed by a glob-style pattern that restricts which variables are returned. Handles the
	 * following syntax:
	 * 
	 * info vars ?pattern?
	 * 
	 * @param interp
	 * @param objv
	 * @throws TclException
	 */

	private static void InfoVarsCmd(Interp interp, TclObject[] objv) throws TclException {
		String varName, pattern, simplePattern;
		Var var;
		Namespace ns;
		Namespace globalNs = Namespace.getGlobalNamespace(interp);
		Namespace currNs = Namespace.getCurrentNamespace(interp);
		TclObject list, elemObj;
		boolean specificNsInPattern = false; // Init. to avoid compiler warning.

		// Get the pattern and find the "effective namespace" in which to
		// list variables. We only use this effective namespace if there's
		// no active Tcl procedure frame.

		if (objv.length == 2) {
			simplePattern = null;
			ns = currNs;
			specificNsInPattern = false;
		} else if (objv.length == 3) {
			// From the pattern, get the effective namespace and the simple
			// pattern (no namespace qualifiers or ::'s) at the end. If an
			// error was found while parsing the pattern, return it. Otherwise,
			// if the namespace wasn't found, just leave ns = null: we will
			// return an empty list since no variables there can be found.

			pattern = objv[2].toString();

			Namespace.GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
			Namespace.getNamespaceForQualName(interp, pattern, null, 0, gnfqnr);
			ns = gnfqnr.ns;
			simplePattern = gnfqnr.simpleName;

			if (ns != null) { // we successfully found the pattern's ns
				specificNsInPattern = (simplePattern.compareTo(pattern) != 0);
			}
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?pattern?");
		}

		// If the namespace specified in the pattern wasn't found, just return.

		if (ns == null) {
			return;
		}

		list = TclList.newInstance();

		if ((interp.varFrame == null) || !interp.varFrame.isProcCallFrame || specificNsInPattern) {
			// There is no frame pointer, the frame pointer was pushed only
			// to activate a namespace, or we are in a procedure call frame
			// but a specific namespace was specified. Create a list containing
			// only the variables in the effective namespace's variable table.

			for (Entry<String, Var> stringVarEntry1 : ns.varTable.entrySet()) {
				Entry entry = (Entry) stringVarEntry1;
				varName = (String) entry.getKey();
				var = (Var) entry.getValue();

				if (!var.isVarUndefined() || var.isVarNamespace()) {
					if ((simplePattern == null) || Util.stringMatch(varName, simplePattern)) {
						if (specificNsInPattern) {
							elemObj = TclString.newInstance(Var.getVariableFullName(interp, var));
						} else {
							elemObj = TclString.newInstance(varName);
						}
						TclList.append(interp, list, elemObj);
					}
				}
			}

			// If the effective namespace isn't the global :: namespace, and a
			// specific namespace wasn't requested in the pattern (i.e., the
			// pattern only specifies variable names), then add in all global ::
			// variables that match the simple pattern. Of course, add in only
			// those variables that aren't hidden by a variable in the effective
			// namespace.

			if ((ns != globalNs) && !specificNsInPattern) {
				for (Entry<String, Var> stringVarEntry : globalNs.varTable.entrySet()) {
					Entry entry = (Entry) stringVarEntry;
					varName = (String) entry.getKey();
					var = (Var) entry.getValue();

					if (!var.isVarUndefined() || var.isVarNamespace()) {
						if ((simplePattern == null) || Util.stringMatch(varName, simplePattern)) {

							// Skip vars defined in current namespace
							if (ns.varTable.get(varName) == null) {
								TclList.append(interp, list, TclString.newInstance(varName));
							}
						}
					}
				}
			}
		} else {
			Var.AppendLocals(interp, list, simplePattern, true);
		}

		interp.setResult(list);
		return;
	}
}
