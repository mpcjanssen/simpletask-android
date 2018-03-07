/*
 * InterpCmd.java --
 *
 *	Implements the built-in "interp" Tcl command.
 *
 * Copyright (c) 2000 Christian Krone.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: InterpCmd.java,v 1.4 2009/06/19 08:03:22 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import java.util.Iterator;
import java.util.Map;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclIndex;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.channel.Channel;

/**
 * This class implements the built-in "interp" command in Tcl.
 */

public class InterpCmd implements Command {

	static final private String options[] = { "alias", "aliases", "create",
			"delete", "eval", "exists", "expose", "hide", "hidden", "issafe",
			"invokehidden", "marktrusted", "recursionlimit", "slaves", "share",
			"target", "transfer" };
	static final private int OPT_ALIAS = 0;
	static final private int OPT_ALIASES = 1;
	static final private int OPT_CREATE = 2;
	static final private int OPT_DELETE = 3;
	static final private int OPT_EVAL = 4;
	static final private int OPT_EXISTS = 5;
	static final private int OPT_EXPOSE = 6;
	static final private int OPT_HIDE = 7;
	static final private int OPT_HIDDEN = 8;
	static final private int OPT_ISSAFE = 9;
	static final private int OPT_INVOKEHIDDEN = 10;
	static final private int OPT_MARKTRUSTED = 11;
	static final private int OPT_RECURSIONLMT = 12;
	static final private int OPT_SLAVES = 13;
	static final private int OPT_SHARE = 14;
	static final private int OPT_TARGET = 15;
	static final private int OPT_TRANSFER = 16;

	static final private String createOptions[] = { "-safe", "--" };
	static final private int OPT_CREATE_SAFE = 0;
	static final private int OPT_CREATE_LAST = 1;

	static final private String hiddenOptions[] = { "-global", "--" };
	static final private int OPT_HIDDEN_GLOBAL = 0;
	static final private int OPT_HIDDEN_LAST = 1;

	/**
	 *----------------------------------------------------------------------
	 * 
	 * Tcl_InterpObjCmd -> cmdProc
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "interp" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 *----------------------------------------------------------------------
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
		case OPT_ALIAS: {
			if (objv.length >= 4) {
				Interp slaveInterp = getInterp(interp, objv[2]);

				if (objv.length == 4) {
					InterpAliasCmd.describe(interp, slaveInterp, objv[3]);
					return;
				}
				if ((objv.length == 5) && ("".equals(objv[4].toString()))) {
					InterpAliasCmd.delete(interp, slaveInterp, objv[3]);
					return;
				}
				if (objv.length > 5) {
					Interp masterInterp = getInterp(interp, objv[4]);
					if ("".equals(objv[5].toString())) {
						if (objv.length == 6) {
							InterpAliasCmd.delete(interp, slaveInterp, objv[3]);
							return;
						}
					} else {
						InterpAliasCmd.create(interp, slaveInterp,
								masterInterp, objv[3], objv[5], 6, objv);
						return;
					}
				}
			}
			throw new TclNumArgsException(interp, 2, objv,
					"slavePath slaveCmd ?masterPath masterCmd? ?args ..?");
		}
		case OPT_ALIASES: {
			Interp slaveInterp = getInterp(interp, objv);
			InterpAliasCmd.list(interp, slaveInterp);
			break;
		}
		case OPT_CREATE: {

			// Weird historical rules: "-safe" is accepted at the end, too.

			boolean safe = interp.isSafe;

			TclObject slaveNameObj = null;
			boolean last = false;
			for (int i = 2; i < objv.length; i++) {
				if ((!last) && (objv[i].toString().charAt(0) == '-')) {
					int index = TclIndex.get(interp, objv[i], createOptions,
							"option", 0);
					switch (index) {
					case OPT_CREATE_SAFE:
						safe = true;
						break;
					case OPT_CREATE_LAST:
						last = true;
						break;
					}
					continue;
				}
				if (slaveNameObj != null) {
					throw new TclNumArgsException(interp, 2, objv,
							"?-safe? ?--? ?path?");
				}
				slaveNameObj = objv[i];
			}
			if (slaveNameObj == null) {

				// Create an anonymous interpreter -- we choose its name and
				// the name of the command. We check that the command name
				// that we use for the interpreter does not collide with an
				// existing command in the master interpreter.

				int i = 0;
				while (interp.getCommand("interp" + i) != null) {
					i++;
				}
				slaveNameObj = TclString.newInstance("interp" + i);
			}
			InterpSlaveCmd.create(interp, slaveNameObj, safe);
			interp.setResult(slaveNameObj);
			break;
		}
		case OPT_DELETE: {
			for (int i = 2; i < objv.length; i++) {
				Interp slaveInterp = getInterp(interp, objv[i]);

				if (slaveInterp == interp) {
					throw new TclException(interp,
							"cannot delete the current interpreter");
				}
				InterpSlaveCmd slave = slaveInterp.slave;
				slave.masterInterp.deleteCommandFromToken(slave.interpCmd);
			}
			break;
		}
		case OPT_EVAL: {
			if (objv.length < 4) {
				throw new TclNumArgsException(interp, 2, objv,
						"path arg ?arg ...?");
			}
			Interp slaveInterp = getInterp(interp, objv[2]);
			InterpSlaveCmd.eval(interp, slaveInterp, 3, objv);
			break;
		}
		case OPT_EXISTS: {
			boolean exists = true;

			try {
				getInterp(interp, objv);
			} catch (TclException e) {
				if (objv.length > 3) {
					throw e;
				}
				exists = false;
			}
			interp.setResult(exists);
			break;
		}
		case OPT_EXPOSE: {
			if (objv.length < 4 || objv.length > 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"path hiddenCmdName ?cmdName?");
			}
			Interp slaveInterp = getInterp(interp, objv[2]);
			InterpSlaveCmd.expose(interp, slaveInterp, 3, objv);
			break;
		}
		case OPT_HIDE: {
			if (objv.length < 4 || objv.length > 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"path cmdName ?hiddenCmdName?");
			}
			Interp slaveInterp = getInterp(interp, objv[2]);
			InterpSlaveCmd.hide(interp, slaveInterp, 3, objv);
			break;
		}
		case OPT_HIDDEN: {
			Interp slaveInterp = getInterp(interp, objv);
			InterpSlaveCmd.hidden(interp, slaveInterp);
			break;
		}
		case OPT_ISSAFE: {
			Interp slaveInterp = getInterp(interp, objv);
			interp.setResult(slaveInterp.isSafe);
			break;
		}
		case OPT_INVOKEHIDDEN: {
			boolean global = false;
			int i;
			for (i = 3; i < objv.length; i++) {
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
						"path ?-global? ?--? cmd ?arg ..?");
			}
			Interp slaveInterp = getInterp(interp, objv[2]);
			InterpSlaveCmd.invokeHidden(interp, slaveInterp, global, i, objv);
			break;
		}
		case OPT_MARKTRUSTED: {
			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "path");
			}
			Interp slaveInterp = getInterp(interp, objv[2]);
			InterpSlaveCmd.markTrusted(interp, slaveInterp);
			break;
		}
		case OPT_RECURSIONLMT: {
			if (objv.length != 3 && objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv,
						"path ?newlimit?");
			}
			Interp slaveInterp = getInterp(interp, objv[2]);
			InterpSlaveCmd.recursionLimit(interp, slaveInterp, objv.length - 3,
					objv);
			break;
		}
		case OPT_SLAVES: {
			Interp slaveInterp = getInterp(interp, objv);

			TclObject result = TclList.newInstance();
			for (Object o : slaveInterp.slaveTable.entrySet()) {
				Map.Entry entry = (Map.Entry) o;
				String string = (String) entry.getKey();
				TclList.append(interp, result, TclString.newInstance(string));
			}
			interp.setResult(result);

			break;
		}
		case OPT_SHARE: {
			if (objv.length != 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"srcPath channelId destPath");
			}
			TclIO.giveChannel(getInterp(interp, objv[2]), getInterp(interp,objv[4]), objv[3].toString(), false);
			break;
		}
		case OPT_TARGET: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "path alias");
			}

			Interp slaveInterp = getInterp(interp, objv[2]);
			String aliasName = objv[3].toString();
			Interp targetInterp = InterpAliasCmd.getTargetInterp(slaveInterp,
					aliasName);
			if (targetInterp == null) {
				throw new TclException(interp, "alias \"" + aliasName
						+ "\" in path \"" + objv[2].toString() + "\" not found");
			}
			if (!getInterpPath(interp, targetInterp)) {
				throw new TclException(interp,
						"target interpreter for alias \"" + aliasName
								+ "\" in path \"" + objv[2].toString()
								+ "\" is not my descendant");
			}
			break;
		}
		case OPT_TRANSFER: {
			if (objv.length != 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"srcPath channelId destPath");
			}
			TclIO.giveChannel(getInterp(interp, objv[2]), getInterp(interp,objv[4]), objv[3].toString(), true);
			break;
		}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * -
	 * 
	 * GetInterp2 -> getInterp
	 * 
	 * Helper function for Tcl_InterpObjCmd() to convert the interp name
	 * potentially specified on the command line to an Tcl_Interp.
	 * 
	 * Results: The return value is the interp specified on the command line, or
	 * the interp argument itself if no interp was specified on the command
	 * line. If the interp could not be found or the wrong number of arguments
	 * was specified on the command line, the return value is NULL and an error
	 * message is left in the interp's result.
	 * 
	 * Side effects: None.
	 * 
	 * 
	 * 
	 * --------------------------------------------------------------------------
	 * -
	 */

	private static Interp getInterp(Interp interp, // Default interp if no
			// interp was specified
			// on the command line.
			TclObject[] objv) // Argument objects.
			throws TclException {
		if (objv.length == 2) {
			return interp;
		} else if (objv.length == 3) {
			return getInterp(interp, objv[2]);
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?path?");
		}
	}

	/**
	 *----------------------------------------------------------------------
	 * 
	 * Tcl_GetInterpPath -> getInterpPath
	 * 
	 * Sets the result of the asking interpreter to a proper Tcl list containing
	 * the names of interpreters between the asking and target interpreters. The
	 * target interpreter must be either the same as the asking interpreter or
	 * one of its slaves (including recursively).
	 * 
	 * Results: true if the target interpreter is the same as, or a descendant
	 * of, the asking interpreter; false otherwise. This way one can distinguish
	 * between the case where the asking and target interps are the same (true
	 * is returned) and when the target is not a descendant of the asking
	 * interpreter (in which case false is returned).
	 * 
	 * Side effects: None.
	 * 
	 *----------------------------------------------------------------------
	 */

	private static boolean getInterpPath(Interp askingInterp, // Interpreter to
			// start search
			// from.
			Interp targetInterp) // Interpreter to find.
			throws TclException {
		if (targetInterp == askingInterp) {
			return true;
		}
		if (targetInterp == null || targetInterp.slave == null) {
			return false;
		}

		if (!getInterpPath(askingInterp, targetInterp.slave.masterInterp)) {
			return false;
		}
		askingInterp.appendElement(targetInterp.slave.path);
		return true;
	}

	/**
	 *----------------------------------------------------------------------
	 * 
	 * getInterp --
	 * 
	 * Helper function to find a slave interpreter given a pathname.
	 * 
	 * Results: Returns the slave interpreter known by that name in the calling
	 * interpreter, or NULL if no interpreter known by that name exists.
	 * 
	 * Side effects: Assigns to the pointer variable passed in, if not NULL.
	 * 
	 *----------------------------------------------------------------------
	 */

	static Interp getInterp(Interp interp, // Interp. to start search from.
			TclObject path) // List object containing name of interp. to
			// be found.
			throws TclException {
		TclObject[] objv = TclList.getElements(interp, path);
		Interp searchInterp = interp; // Interim storage for interp. to find.

		for (TclObject anObjv : objv) {
			String name = anObjv.toString();
			if (!searchInterp.slaveTable.containsKey(name)) {
				searchInterp = null;
				break;
			}
			InterpSlaveCmd slave = (InterpSlaveCmd) searchInterp.slaveTable
					.get(name);
			searchInterp = slave.slaveInterp;
			if (searchInterp == null) {
				break;
			}
		}

		if (searchInterp == null) {
			throw new TclException(interp, "could not find interpreter \""
					+ path.toString() + "\"");
		}

		return searchInterp;
	}

} // end InterpCmd
