/*
 * InterpSlaveCmd.java --
 *
 *	Implements the built-in "interp" Tcl command.
 *
 * Copyright (c) 2000 Christian Krone.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: InterpSlaveCmd.java,v 1.8 2009/06/19 08:03:21 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import java.util.Iterator;
import java.util.Map;

import tcl.lang.AssocData;
import tcl.lang.CommandWithDispose;
import tcl.lang.Interp;
import tcl.lang.Namespace;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.TclString;
import tcl.lang.WrappedCommand;
import tcl.lang.channel.Channel;
import tcl.lang.channel.StdChannel;

/**
 * This class implements the slave interpreter commands, which are created in
 * response to the built-in "interp create" command in Tcl.
 * 
 * It is also used by the "interp" command to record and find information about
 * slave interpreters. Maps from a command name in the master to information
 * about a slave interpreter, e.g. what aliases are defined in it.
 */

public class InterpSlaveCmd implements CommandWithDispose, AssocData {

	static final private String options[] = { "alias", "aliases", "eval",
			"expose", "hide", "hidden", "issafe", "invokehidden",
			"marktrusted", "recursionlimit" };
	static final private int OPT_ALIAS = 0;
	static final private int OPT_ALIASES = 1;
	static final private int OPT_EVAL = 2;
	static final private int OPT_EXPOSE = 3;
	static final private int OPT_HIDE = 4;
	static final private int OPT_HIDDEN = 5;
	static final private int OPT_ISSAFE = 6;
	static final private int OPT_INVOKEHIDDEN = 7;
	static final private int OPT_MARKTRUSTED = 8;
	static final private int OPT_RECURSIONLMT = 9;

	static final private String hiddenOptions[] = { "-global", "--" };
	static final private int OPT_HIDDEN_GLOBAL = 0;
	static final private int OPT_HIDDEN_LAST = 1;

	// Master interpreter for this slave.

	Interp masterInterp;

	// Hash entry in masters slave table for this slave interpreter.
	// Used to find this record, and used when deleting the slave interpreter
	// to delete it from the master's table.

	String path;

	// The slave interpreter.

	Interp slaveInterp;

	// Interpreter object command.

	WrappedCommand interpCmd;

	// Debug child interp alloc/dispose calls

	static final boolean debug = false;

	/**
	 * Command to manipulate an interpreter, e.g. to send commands to it to be
	 * evaluated. One such command exists for each slave interpreter.
	 * 
	 * Results: A standard Tcl results
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Argument list.
			throws TclException // A standard Tcl exception.
	{
		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv, "cmd ?arg ...?");
		}
		int cmd = TclIndex.get(interp, objv[1], options, "option", 0);

		switch (cmd) {
		case OPT_ALIAS:
			if (objv.length == 3) {
				InterpAliasCmd.describe(interp, slaveInterp, objv[2]);
				return;
			}
			if (objv.length==4 && "".equals(objv[3].toString())) {
				InterpAliasCmd.delete(interp, slaveInterp, objv[2]);
				return;
			} else if (objv.length >= 4) {
				InterpAliasCmd.create(interp, slaveInterp, interp, objv[2],
						objv[3], 4, objv);
				return;
			}
			throw new TclNumArgsException(interp, 2, objv,
					"aliasName ?targetName? ?args..?");
		case OPT_ALIASES:
			if (objv.length != 2) {
				throw new TclNumArgsException(interp, 2, objv, null);
			}
			InterpAliasCmd.list(interp, slaveInterp);
			break;
		case OPT_EVAL:
			if (objv.length < 3) {
				throw new TclNumArgsException(interp, 2, objv, "arg ?arg ...?");
			}
			eval(interp, slaveInterp, 2, objv);
			break;
		case OPT_EXPOSE:
			if (objv.length < 3 || objv.length > 4) {
				throw new TclNumArgsException(interp, 2, objv,
						"hiddenCmdName ?cmdName?");
			}
			expose(interp, slaveInterp, 2, objv);
			break;
		case OPT_HIDE:
			if (objv.length < 3 || objv.length > 4) {
				throw new TclNumArgsException(interp, 2, objv,
						"cmdName ?hiddenCmdName?");
			}
			hide(interp, slaveInterp, 2, objv);
			break;
		case OPT_HIDDEN:
			if (objv.length != 2) {
				throw new TclNumArgsException(interp, 2, objv, null);
			}
			InterpSlaveCmd.hidden(interp, slaveInterp);
			break;
		case OPT_ISSAFE:
			if (objv.length != 2) {
				throw new TclNumArgsException(interp, 2, objv, null);
			}
			interp.setResult(slaveInterp.isSafe);
			break;
		case OPT_INVOKEHIDDEN:
			boolean global = false;
			int i;
			for (i = 2; i < objv.length; i++) {
				if (objv[i].toString().charAt(0) != '-') {
					break;
				}
				int index = TclIndex.get(interp, objv[i], hiddenOptions,
						"option", 0);
				if (index == OPT_HIDDEN_GLOBAL) {
					global = true;
				} else {
					i++;
					break;
				}
			}
			if (objv.length - i < 1) {
				throw new TclNumArgsException(interp, 2, objv,
						"?-global? ?--? cmd ?arg ..?");
			}
			InterpSlaveCmd.invokeHidden(interp, slaveInterp, global, i, objv);
			break;
		case OPT_MARKTRUSTED:
			if (objv.length != 2) {
				throw new TclNumArgsException(interp, 2, objv, null);
			}
			markTrusted(interp, slaveInterp);
			break;

		case OPT_RECURSIONLMT:
			if (objv.length != 2 && objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "?newlimit?");
			}
			recursionLimit(interp, slaveInterp, objv.length - 2, objv);
			break;
		}
	}

	/**
	 *----------------------------------------------------------------------
	 * 
	 * disposeCmd --
	 * 
	 * Invoked when an object command for a slave interpreter is deleted; cleans
	 * up all state associated with the slave interpreter and destroys the slave
	 * interpreter.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Cleans up all state associated with the slave interpreter
	 * and destroys the slave interpreter.
	 * 
	 *----------------------------------------------------------------------
	 */

	public void disposeCmd() {
		// Unlink the slave from its master interpreter.

		masterInterp.slaveTable.remove(path);

		// Set to null so that when the InterpInfo is cleaned up in the slave
		// it does not try to delete the command causing all sorts of grief.
		// See SlaveRecordDeleteProc().

		interpCmd = null;

		if (slaveInterp != null) {
			if (debug) {
				System.out.println("slaveInterp with path \"" + path
						+ "\" disposed of " + slaveInterp);
			}
			slaveInterp.dispose();
		} else {
			if (debug) {
				System.out.println("slaveInterp with path \"" + path
						+ "\" is null");
			}
		}
	}

	/**
	 * Invoked when an interpreter is being deleted. It releases all storage
	 * used by the master/slave/safe interpreter facilities.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Cleans up storage. Sets the interpInfoPtr field of the
	 * interp to NULL.
	 */

	public void disposeAssocData(Interp interp) // Current interpreter.
	{
		// There shouldn't be any commands left.

		if (!interp.slaveTable.isEmpty()) {
			throw new TclRuntimeError("disposeAssocData: commands still exist");
		}
		interp.slaveTable = null;

		// Tell any interps that have aliases to this interp that they should
		// delete those aliases. If the other interp was already dead, it
		// would have removed the target record already.

		for (Object o : interp.targetTable.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			WrappedCommand slaveCmd = (WrappedCommand) entry.getKey();
			Interp slaveInterp = (Interp) entry.getValue();
			slaveInterp.deleteCommandFromToken(slaveCmd);
		}

		interp.targetTable = null;

		if (interp.interpChanTable != null) {
			// Tear down channel table, be careful to pull the first element
			// from
			// the front of the table until empty since the table is modified
			// by unregisterChannel().
			Channel channel;
			while ((channel = (Channel) Namespace
					.FirstHashEntry(interp.interpChanTable)) != null) {
				TclIO.unregisterChannel(interp, channel);
			}
		}

		if (interp.slave.interpCmd != null) {
			// Tcl_DeleteInterp() was called on this interpreter, rather
			// "interp delete" or the equivalent deletion of the command in the
			// master. First ensure that the cleanup callback doesn't try to
			// delete the interp again.

			interp.slave.slaveInterp = null;
			interp.slave.masterInterp
					.deleteCommandFromToken(interp.slave.interpCmd);
		}

		// There shouldn't be any aliases left.

		if (!interp.aliasTable.isEmpty()) {
			throw new TclRuntimeError("disposeAssocData: aliases still exist");
		}
		interp.aliasTable = null;
	}

	/**
	 * Helper function to do the actual work of creating a slave interp and new
	 * object command. Also optionally makes the new slave interpreter "safe".
	 * 
	 * Results: Returns the new Tcl_Interp * if successful or NULL if not. If
	 * failed, the result of the invoking interpreter contains an error message.
	 * 
	 * Side effects: Creates a new slave interpreter and a new object command.
	 * 
	 * @param interp interpreter to start search from
	 * @param path path of slave to create
	 * @param safe true if it should be made safe
	 * @return new slave interpreter
	 * @throws TclException
	 */
	static Interp create(Interp interp, // Interp. to start search from.
			TclObject path, // Path (name) of slave to create.
			boolean safe) // Should we make it "safe"?
			throws TclException // A standard Tcl exception.
	{
		Interp masterInterp;
		String pathString;

		TclObject[] objv = TclList.getElements(interp, path);

		if (objv.length < 2) {
			masterInterp = interp;
			pathString = path.toString();
		} else {
			TclObject obj = TclList.newInstance();

			TclList.insert(interp, obj, 0, objv, 0, objv.length - 2);
			masterInterp = InterpCmd.getInterp(interp, obj);
			pathString = objv[objv.length - 1].toString();
		}
		if (!safe) {
			safe = masterInterp.isSafe;
		}

		if (masterInterp.slaveTable.containsKey(pathString)) {
			throw new TclException(interp, "interpreter named \"" + pathString
					+ "\" already exists, cannot create");
		}

		Interp slaveInterp = new Interp();
		InterpSlaveCmd slave = new InterpSlaveCmd();

		slaveInterp.setMaxNestingDepth(masterInterp.getMaxNestingDepth());
		slaveInterp.slave = slave;
		slaveInterp.setAssocData("InterpSlaveCmd", slave);
		slaveInterp.setWorkingDir(interp.getWorkingDir().getPath());

		slave.masterInterp = masterInterp;
		slave.path = pathString;
		slave.slaveInterp = slaveInterp;

		masterInterp.createCommand(pathString, slaveInterp.slave);
		slaveInterp.slave.interpCmd = Namespace.findCommand(masterInterp,
				pathString, null, 0);

		masterInterp.slaveTable.put(pathString, slaveInterp.slave);

		if (debug) {
			System.out.println("slaveTable entry for \"" + pathString
					+ "\" created (" + slaveInterp + ")");
		}

		slaveInterp.setVar("tcl_interactive", "0", TCL.GLOBAL_ONLY);

		// Inherit the recursion limit.

		// slaveInterp.maxNestingDepth = masterInterp.maxNestingDepth;

		if (safe) {
			try {
				makeSafe(slaveInterp);
			} catch (TclException e) {
				e.printStackTrace();
			}
		} else {
			// Tcl_Init(slaveInterp);
		}

		return slaveInterp;
	}

	/**
	 * Helper function to evaluate a command in a slave interpreter.
	 * 
	 * @param interp interpreter for error return
	 * @param slaveInterp slave interpreter in which command will be evaluated
	 * @param objIx number of arguments to ignore
	 * @param objv argument objects
	 * @throws TclException
	 */
	static void eval(Interp interp, // Interp for error return.
			Interp slaveInterp, // The slave interpreter in which command
			// will be evaluated.
			int objIx, // Number of arguments to ignored.
			TclObject objv[]) // Argument objects.
			throws TclException {
		int result;

		slaveInterp.preserve();

		try {
			slaveInterp.allowExceptions();

			try {
				if (objIx + 1 == objv.length) {
					slaveInterp.eval(objv[objIx], 0);
				} else {
					TclObject obj = TclList.newInstance();
					for (int ix = objIx; ix < objv.length; ix++) {
						TclList.append(interp, obj, objv[ix]);
					}
					obj.preserve();
					slaveInterp.eval(obj, 0);
					obj.release();
				}
				result = slaveInterp.returnCode;
			} catch (TclException e) {
				result = e.getCompletionCode();
			}

			interp.transferResult(slaveInterp, result);
		} finally {
			slaveInterp.release();
		}
	}

	/**
	 * Helper function to expose a command in a slave interpreter.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: After this call scripts in the slave will be able to invoke
	 * the newly exposed command.
	 * 
	 * 	@param interp interpreter for error return
	 * @param slaveInterp slave interpreter in which command will be evaluated
	 * @param objIx number of arguments to ignore
	 * @param objv argument objects
	 * @throws TclException
	 */

	static void expose(Interp interp, // Interp for error return.
			Interp slaveInterp, // Interp in which command will be exposed.
			int objIx, // Number of arguments to ignored.
			TclObject objv[]) // Argument objects.
			throws TclException {
		if (interp.isSafe) {
			throw new TclException(interp, "permission denied: "
					+ "safe interpreter cannot expose commands");
		}

		int nameIdx = objv.length - objIx == 1 ? objIx : objIx + 1;

		try {
			slaveInterp.exposeCommand(objv[objIx].toString(), objv[nameIdx]
					.toString());
		} catch (TclException e) {
			interp.transferResult(slaveInterp, e.getCompletionCode());
			throw e;
		}
	}

	/**
	 * Helper function to hide a command in a slave interpreter.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: After this call scripts in the slave will no longer be able
	 * to invoke the named command.
	 * 
	 * @param interp interpreter for error return
	 * @param slaveInterp slave interpreter in which command will be evaluated
	 * @param objIx number of arguments to ignore
	 * @param objv argument objects
	 * @throws TclException
	 */

	static void hide(Interp interp, // Interp for error return.
			Interp slaveInterp, // Interp in which command will be hidden.
			int objIx, // Number of arguments to ignored.
			TclObject objv[]) // Argument objects.
			throws TclException {
		if (interp.isSafe) {
			throw new TclException(interp, "permission denied: "
					+ "safe interpreter cannot hide commands");
		}

		int nameIdx = objv.length - objIx == 1 ? objIx : objIx + 1;

		try {
			slaveInterp.hideCommand(objv[objIx].toString(), objv[nameIdx]
					.toString());
		} catch (TclException e) {
			interp.transferResult(slaveInterp, e.getCompletionCode());
			throw e;
		}
	}

	/**
	 * Helper function to compute list of hidden commands in a slave
	 * interpreter.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: None.
	 * 
	 * @param interp interp for data return
	 * @param slaveInterp interp whose hidden commands to query
	 * @throws TclException
	 */
	static void hidden(Interp interp, // Interp for data return.
			Interp slaveInterp) // Interp whose hidden commands to query.
			throws TclException {
		if (slaveInterp.hiddenCmdTable == null) {
			return;
		}

		TclObject result = TclList.newInstance();
		for (Object o : slaveInterp.hiddenCmdTable.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			String cmdName = (String) entry.getKey();
			TclList.append(interp, result, TclString.newInstance(cmdName));
		}
		interp.setResult(result);
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * SlaveInvokeHidden -> invokeHidden
	 * 
	 * Helper function to invoke a hidden command in a slave interpreter.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: Whatever the hidden command does.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void invokeHidden(Interp interp, // Interp for error return.
			Interp slaveInterp, // The slave interpreter in which command
			// will be invoked.
			boolean global, // True to invoke in global namespace.
			int objIx, // Number of arguments to ignored.
			TclObject[] objv) // Argument objects.
			throws TclException {
		int result;

		if (interp.isSafe) {
			throw new TclException(interp, "not allowed to "
					+ "invoke hidden commands from safe interpreter");
		}

		slaveInterp.preserve();

		try {
			slaveInterp.allowExceptions();

			TclObject localObjv[] = new TclObject[objv.length - objIx];
			for (int i = 0; i < objv.length - objIx; i++) {
				localObjv[i] = objv[i + objIx];
			}

			try {
				if (global) {
					slaveInterp.invokeGlobal(localObjv, Interp.INVOKE_HIDDEN);
				} else {
					slaveInterp.invoke(localObjv, Interp.INVOKE_HIDDEN);
				}
				result = slaveInterp.returnCode;
			} catch (TclException e) {
				result = e.getCompletionCode();
			}

			interp.transferResult(slaveInterp, result);
		} finally {
			slaveInterp.release();
		}
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * SlaveMarkTrusted -> markTrusted
	 * 
	 * Helper function to mark a slave interpreter as trusted (unsafe).
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: After this call the hard-wired security checks in the core
	 * no longer prevent the slave from performing certain operations.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void markTrusted(Interp interp, // Interp for error return.
			Interp slaveInterp) // The slave interpreter which will be
			// marked trusted.
			throws TclException {
		if (interp.isSafe) {
			throw new TclException(interp, "permission denied: "
					+ "safe interpreter cannot mark trusted");
		}
		slaveInterp.isSafe = false;
	}

	/**
	 * Helper function to set/query the Recursion limit of an interp
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: When (objc == 1), slaveInterp will be set to a new
	 * recursion limit of objv[0].
	 * 
	 * @param interp the current interpreter
	 * @param slaveInterp interpreter to query/set recursion limit
	 * @param objc if non-zero, set to recursion limit to new value in objv
	 * @param objv argument objects
	 */

	static void recursionLimit(Interp interp, Interp slaveInterp, int objc, // Set
			// or
			// Query,
			TclObject objv[]) // Argument objects.
			throws TclException {
		int limit;

		if (objc != 0) {
			if (interp.isSafe) {
				throw new TclException(interp, "permission denied: "
						+ "safe interpreters cannot change recursion limit");
			}

			limit = TclInteger.getInt(interp, objv[objv.length - 1]);

			if (limit <= 0) {
				throw new TclException(interp, "recursion limit must be > 0");
			}

			slaveInterp.setMaxNestingDepth(limit);

			if (interp == slaveInterp && interp.nestLevel > limit) {
				throw new TclException(interp,
						"falling back due to new recursion limit");

			}
			interp.setResult(objv[objv.length - 1]);
		} else {
			limit = slaveInterp.setMaxNestingDepth(0);
			interp.setResult(TclInteger.newInstance(limit));
		}
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * makeSafe --
	 * 
	 * Makes its argument interpreter contain only functionality that is defined
	 * to be part of Safe Tcl. Unsafe commands are hidden, the env array is
	 * unset, and the standard channels are removed.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Hides commands in its argument interpreter, and removes
	 * settings and channels.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void makeSafe(Interp interp) // Interpreter to be made safe.
			throws TclException {
		Channel chan; // Channel to remove from safe interpreter.

		interp.hideUnsafeCommands();

		interp.isSafe = true;

		// Unsetting variables : (which should not have been set
		// in the first place, but...)

		// No env array in a safe slave.

		try {
			interp.unsetVar("env", TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		}
		;

		// Remove unsafe parts of tcl_platform

		try {
			interp.unsetVar("tcl_platform", "os", TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		}
		;
		try {
			interp.unsetVar("tcl_platform", "osVersion", TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		}
		;
		try {
			interp.unsetVar("tcl_platform", "machine", TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		}
		;
		try {
			interp.unsetVar("tcl_platform", "user", TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		}
		;

		// Unset path informations variables
		// (the only one remaining is [info nameofexecutable])

		try {
			interp.unsetVar("tclDefaultLibrary", TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		}
		;
		try {
			interp.unsetVar("tcl_library", TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		}
		;
		try {
			interp.unsetVar("tcl_pkgPath", TCL.GLOBAL_ONLY);
		} catch (TclException e) {
		}
		;

		// Remove the standard channels from the interpreter; safe interpreters
		// do not ordinarily have access to stdin, stdout and stderr.
		//
		// NOTE: These channels are not added to the interpreter by the
		// Tcl_CreateInterp call, but may be added later, by another I/O
		// operation. We want to ensure that the interpreter does not have
		// these channels even if it is being made safe after being used for
		// some time..

		chan = TclIO.getStdChannel(StdChannel.STDIN);
		if (chan != null) {
			TclIO.unregisterChannel(interp, chan);
		}
		chan = TclIO.getStdChannel(StdChannel.STDOUT);
		if (chan != null) {
			TclIO.unregisterChannel(interp, chan);
		}
		chan = TclIO.getStdChannel(StdChannel.STDERR);
		if (chan != null) {
			TclIO.unregisterChannel(interp, chan);
		}
	}

	/**
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_GetSlave, GetInterp -> getSlave
	 * 
	 * Finds a slave interpreter by its path name.
	 * 
	 * Results: Returns a Interp for the named interpreter or raises a
	 * TclException if the interpreter is not found.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static Interp getSlave(Interp interp, // Interpreter to start search from.
			TclObject slavePath) // Path of slave to find.
			throws TclException {
		Interp searchInterp;
		final int len = TclList.getLength(interp, slavePath);

		searchInterp = interp;
		for (int i = 0; i < len; i++) {
			TclObject slavePathIndex = TclList.index(interp, slavePath, i);
			InterpSlaveCmd isc = (InterpSlaveCmd) searchInterp.slaveTable
					.get(slavePathIndex.toString());
			if (isc == null) {
				searchInterp = null;
				break;
			}
			searchInterp = isc.slaveInterp;
			if (searchInterp == null) {
				break;
			}
		}
		if (searchInterp == null) {
			throw new TclException(interp, "could not find interpreter \""
					+ slavePath + "\"");
		}
		return searchInterp;
	}

} // end InterpSlaveCmd

