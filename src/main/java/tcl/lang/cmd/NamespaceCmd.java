/*
 * NamespaceCmd.java
 *
 * Copyright (c) 1993-1997 Lucent Technologies.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-1999 by Scriptics Corporation.
 * Copyright (c) 1999 Moses DeJong
 *
 * Originally implemented by
 *   Michael J. McLennan
 *   Bell Labs Innovations for Lucent Technologies
 *   mmclennan@lucent.com
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: NamespaceCmd.java,v 1.22 2009/07/07 23:15:09 rszulgo Exp $
 */

package tcl.lang.cmd;

import java.util.Iterator;
import java.util.Map;

import tcl.lang.CallFrame;
import tcl.lang.Command;
import tcl.lang.InternalRep;
import tcl.lang.Interp;
import tcl.lang.Namespace;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.Util;
import tcl.lang.Var;
import tcl.lang.WrappedCommand;

/**
 * This class implements the built-in "namespace" command in Tcl. See the user
 * documentation for details on what it does.
 */

public class NamespaceCmd implements InternalRep, Command {
	// This value corresponds to the Tcl_Obj.otherValuePtr pointer used
	// in the C version of Tcl 8.1. Use it to keep track of a ResolvedNsName.

	Namespace.ResolvedNsName otherValue = null;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_NamespaceObjCmd -> cmdProc
	 * 
	 * Invoked to implement the "namespace" command that creates, deletes, or
	 * manipulates Tcl namespaces. Handles the following syntax:
	 * 
	 * namespace children ?name? ?pattern? namespace code arg namespace current
	 * namespace delete ?name name...? namespace eval name arg ?arg...?
	 * namespace exists name namespace export ?-clear? ?pattern pattern...?
	 * namespace forget ?pattern pattern...? namespace import ?-force? ?pattern
	 * pattern...? namespace inscope name arg ?arg...? namespace origin name
	 * namespace parent ?name? namespace qualifiers string namespace tail string
	 * namespace which ?-command? ?-variable? name
	 * 
	 * Results: Returns if the command is successful. Raises Exception if
	 * anything goes wrong.
	 * 
	 * Side effects: Based on the subcommand name (e.g., "import"), this
	 * procedure dispatches to a corresponding member commands in this class.
	 * This method's side effects depend on whatever that subcommand does.
	 * ----------------------------------------------------------------------
	 */

	private static final String[] validCmds = { "children", "code", "current",
			"delete", "eval", "exists", "export", "forget", "import",
			"inscope", "origin", "parent", "qualifiers", "tail", "which" };

	static final private int OPT_CHILDREN = 0;
	static final private int OPT_CODE = 1;
	static final private int OPT_CURRENT = 2;
	static final private int OPT_DELETE = 3;
	static final private int OPT_EVAL = 4;
	static final private int OPT_EXISTS = 5;
	static final private int OPT_EXPORT = 6;
	static final private int OPT_FORGET = 7;
	static final private int OPT_IMPORT = 8;
	static final private int OPT_INSCOPE = 9;
	static final private int OPT_ORIGIN = 10;
	static final private int OPT_PARENT = 11;
	static final private int OPT_QUALIFIERS = 12;
	static final private int OPT_TAIL = 13;
	static final private int OPT_WHICH = 14;

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {

		int  opt;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"subcommand ?arg ...?");
		}

		opt = TclIndex.get(interp, objv[1], validCmds, "option", 0);

		switch (opt) {
		case OPT_CHILDREN: {
			childrenCmd(interp, objv);
			return;
		}
		case OPT_CODE: {
			codeCmd(interp, objv);
			return;
		}
		case OPT_CURRENT: {
			currentCmd(interp, objv);
			return;
		}
		case OPT_DELETE: {
			deleteCmd(interp, objv);
			return;
		}
		case OPT_EVAL: {
			evalCmd(interp, objv);
			return;
		}
		case OPT_EXISTS: {
			existsCmd(interp, objv);
			return;
		}
		case OPT_EXPORT: {
			exportCmd(interp, objv);
			return;
		}
		case OPT_FORGET: {
			forgetCmd(interp, objv);
			return;
		}
		case OPT_IMPORT: {
			importCmd(interp, objv);
			return;
		}
		case OPT_INSCOPE: {
			inscopeCmd(interp, objv);
			return;
		}
		case OPT_ORIGIN: {
			originCmd(interp, objv);
			return;
		}
		case OPT_PARENT: {
			parentCmd(interp, objv);
			return;
		}
		case OPT_QUALIFIERS: {
			qualifiersCmd(interp, objv);
			return;
		}
		case OPT_TAIL: {
			tailCmd(interp, objv);
			return;
		}
		case OPT_WHICH: {
			whichCmd(interp, objv);
			return;
		}
		} // end switch(opt)

	}

	/**
	 * Invoked to implement the "namespace children" command that returns a list
	 * containing the fully-qualified names of the child namespaces of a given
	 * namespace. Handles the following syntax:
	 * 
	 * namespace children ?name? ?pattern?
	 * 
	 * Results: Nothing.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result is an error message.
	
	 */

	private static void childrenCmd(Interp interp, TclObject[] objv)
			throws TclException {
		Namespace ns, childNs;
		Namespace globalNs = Namespace.getGlobalNamespace(interp);
		String pattern = null;
		StringBuffer buffer;
		TclObject list, elem;

		// Get a pointer to the specified namespace, or the current namespace.

		if (objv.length == 2) {
			ns = Namespace.getCurrentNamespace(interp);
		} else if ((objv.length == 3) || (objv.length == 4)) {
			ns = getNamespaceFromObj(interp, objv[2]);
			if (ns == null) {
				throw new TclException(interp, "unknown namespace \""
						+ objv[2].toString()
						+ "\" in namespace children command");
			}
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?name? ?pattern?");
		}

		// Get the glob-style pattern, if any, used to narrow the search.

		buffer = new StringBuffer();
		if (objv.length == 4) {
			String name = objv[3].toString();

			if (name.startsWith("::")) {
				pattern = name;
			} else {
				buffer.append(ns.fullName);
				if (ns != globalNs) {
					buffer.append("::");
				}
				buffer.append(name);
				pattern = buffer.toString();
			}
		}

		// Create a list containing the full names of all child namespaces
		// whose names match the specified pattern, if any.

		list = TclList.newInstance();
		for (Map.Entry<String, Namespace> stringNamespaceEntry : ns.childTable.entrySet()) {
			Map.Entry entry = (Map.Entry) stringNamespaceEntry;
			childNs = (Namespace) entry.getValue();
			if ((pattern == null)
					|| Util.stringMatch(childNs.fullName, pattern)) {
				elem = TclString.newInstance(childNs.fullName);
				TclList.append(interp, list, elem);
			}
		}

		interp.setResult(list);
		return;
	}

	/**
	 * NamespaceCodeCmd -> codeCmd
	 * 
	 * Invoked to implement the "namespace code" command to capture the
	 * namespace context of a command. Handles the following syntax:
	 * 
	 * namespace code arg
	 * 
	 * Here "arg" can be a list. "namespace code arg" produces a result
	 * equivalent to that produced by the command
	 * 
	 * list namespace inscope [namespace current] $arg
	 * 
	 * However, if "arg" is itself a scoped value starting with
	 * "namespace inscope", then the result is just "arg".
	 * 
	 * Results: Nothing.
	 * 
	 * Side effects: If anything goes wrong, this procedure returns an error
	 * message as the result in the interpreter's result object.
	
	 */

	private static void codeCmd(Interp interp, TclObject[] objv)
			throws TclException {
		Namespace currNs;
		TclObject list, obj;
		String arg, p;
		int length;
		int p_ind;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "arg");
		}

		// If "arg" is already a scoped value, then return it directly.

		arg = objv[2].toString();
		length = arg.length();

		// FIXME : we need a test for this inscope code if there is not one
		// already!
		if ((length > 17) && (arg.charAt(0) == 'n')
				&& arg.startsWith("namespace")) {
			for (p_ind = 9; (p_ind < length) && (arg.charAt(p_ind) == ' '); p_ind++) {
				// empty body: skip over spaces
			}
			if (((length - p_ind) >= 7) && (arg.charAt(p_ind) == 'i')
					&& arg.startsWith("inscope", p_ind)) {
				interp.setResult(objv[2]);
				return;
			}
		}

		// Otherwise, construct a scoped command by building a list with
		// "namespace inscope", the full name of the current namespace, and
		// the argument "arg". By constructing a list, we ensure that scoped
		// commands are interpreted properly when they are executed later,
		// by the "namespace inscope" command.

		list = TclList.newInstance();
		TclList.append(interp, list, TclString.newInstance("::namespace"));
		TclList.append(interp, list, TclString.newInstance("inscope"));

		currNs = Namespace.getCurrentNamespace(interp);
		if (currNs == Namespace.getGlobalNamespace(interp)) {
			obj = TclString.newInstance("::");
		} else {
			obj = TclString.newInstance(currNs.fullName);
		}

		TclList.append(interp, list, obj);
		TclList.append(interp, list, objv[2]);

		interp.setResult(list);
		return;
	}

	/**
	 * Invoked to implement the "namespace current" command which returns the
	 * fully-qualified name of the current namespace. Handles the following
	 * syntax:
	 * 
	 * namespace current
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result is an error message.
	 */

	private static void currentCmd(Interp interp, TclObject[] objv)
			throws TclException {

		Namespace currNs;

		if (objv.length != 2) {
			throw new TclNumArgsException(interp, 2, objv, null);
		}

		// The "real" name of the global namespace ("::") is the null string,
		// but we return "::" for it as a convenience to programmers. Note that
		// "" and "::" are treated as synonyms by the namespace code so that it
		// is still easy to do things like:
		//
		// namespace [namespace current]::bar { ... }

		currNs = Namespace.getCurrentNamespace(interp);

		if (currNs == Namespace.getGlobalNamespace(interp)) {
			// FIXME : appending to te result really screws everything up!
			// need to figure out how to disallow this!
			// TclString.append(interp.getResult(), "::");
			interp.setResult("::");
		} else {
			// TclString.append(interp.getResult(), currNs.fullName);
			interp.setResult(currNs.fullName);
		}
	}

	/**
	 * Invoked to implement the "namespace delete" command to delete
	 * namespace(s). Handles the following syntax:
	 * 
	 * namespace delete ?name name...?
	 * 
	 * Each name identifies a namespace. It may include a sequence of namespace
	 * qualifiers separated by "::"s. If a namespace is found, it is deleted:
	 * all variables and procedures contained in that namespace are deleted. If
	 * that namespace is being used on the call stack, it is kept alive (but
	 * logically deleted) until it is removed from the call stack: that is, it
	 * can no longer be referenced by name but any currently executing procedure
	 * that refers to it is allowed to do so until the procedure returns. If the
	 * namespace can't be found, this procedure returns an error. If no
	 * namespaces are specified, this command does nothing.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Deletes the specified namespaces. If anything goes wrong,
	 * this procedure returns an error message in the interpreter's result
	 * object.
	 */

	private static void deleteCmd(Interp interp, TclObject[] objv)
			throws TclException {
		Namespace namespace;
		String name;
		int i;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 2, objv, "?name name...?");
		}

		// Destroying one namespace may cause another to be destroyed. Break
		// this into two passes: first check to make sure that all namespaces on
		// the command line are valid, and report any errors.

		for (i = 2; i < objv.length; i++) {
			name = objv[i].toString();
			namespace = Namespace.findNamespace(interp, name, null, 0);

			if (namespace == null) {
				throw new TclException(interp, "unknown namespace \""
						+ objv[i].toString() + "\" in namespace delete command");
			}
		}

		// Okay, now delete each namespace.

		for (i = 2; i < objv.length; i++) {
			name = objv[i].toString();
			namespace = Namespace.findNamespace(interp, name, null, 0);

			if (namespace != null) {
				Namespace.deleteNamespace(namespace);
			}
		}
	}

	/**
	 * Invoked to implement the "namespace eval" command. Executes commands in a
	 * namespace. If the namespace does not already exist, it is created.
	 * Handles the following syntax:
	 * 
	 * namespace eval name arg ?arg...?
	 * 
	 * If more than one arg argument is specified, the command that is executed
	 * is the result of concatenating the arguments together with a space
	 * between each argument.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns the result of the command in the interpreter's
	 * result object. If anything goes wrong, this procedure returns an error
	 * message as the result.
	
	 */

	private static void evalCmd(Interp interp, TclObject[] objv)
			throws TclException {
		Namespace namespace;
		CallFrame frame;
		String name;

		if (objv.length < 4) {
			throw new TclNumArgsException(interp, 2, objv, "name arg ?arg...?");
		}

		// Try to resolve the namespace reference, caching the result in the
		// namespace object along the way.

		namespace = getNamespaceFromObj(interp, objv[2]);

		// If the namespace wasn't found, try to create it.

		if (namespace == null) {
			name = objv[2].toString();
			namespace = Namespace.createNamespace(interp, name, null);
			if (namespace == null) {
				// FIXME : result hack, we get the interp result and throw it!
				throw new TclException(interp, interp.getResult().toString());
			}
		}

		// Make the specified namespace the current namespace and evaluate
		// the command(s).

		frame = interp.newCallFrame();
        // copy objv into frame so 'info level n' can report it
        frame.objv = new TclObject[objv.length];
        System.arraycopy(objv, 0, frame.objv, 0, objv.length);
		Namespace.pushCallFrame(interp, frame, namespace, false);

		try {
			if (objv.length == 4) {
				interp.eval(objv[3], 0);
			} else {
				TclObject obj = Util.concat(3, objv.length, objv);

				// eval() will delete the object when it decrements its
				// refcount after eval'ing it.

				interp.eval(obj, 0); // do not pass TCL_EVAL_DIRECT, for
				// compiler only
			}
		} catch (TclException ex) {
			if (ex.getCompletionCode() == TCL.ERROR) {
				interp.addErrorInfo("\n    (in namespace eval \""
						+ namespace.fullName + "\" script line "
						+ interp.errorLine + ")");
				interp.errAlreadyLogged = false;  // allow 'invoked from within' message to be appended
			}
			throw ex;
		} finally {
			Namespace.popCallFrame(interp);
		}

		return;
	}

	/**
	 * NamespaceExistsCmd -> existsCmd
	 * 
	 * Invoked to implement the "namespace exists" command that returns true if
	 * the given namespace currently exists, and false otherwise. Handles the
	 * following syntax:
	 * 
	 * namespace exists name
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result is an error message.
	 */
	private static void existsCmd(Interp interp, TclObject[] objv)
			throws TclException {
		Namespace namespace;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "name");
		}

		/*
		 * Check whether the given namespace exists
		 */
		try {
			namespace = getNamespaceFromObj(interp, objv[2]);
		} catch (TclException e) {
			throw e;
		}

		interp.setResult(namespace != null);
	}

	/**
	 * Invoked to implement the "namespace export" command that specifies which
	 * commands are exported from a namespace. The exported commands are those
	 * that can be imported into another namespace using "namespace import".
	 * Both commands defined in a namespace and commands the namespace has
	 * imported can be exported by a namespace. This command has the following
	 * syntax:
	 * 
	 * namespace export ?-clear? ?pattern pattern...?
	 * 
	 * Each pattern may contain "string match"-style pattern matching special
	 * characters, but the pattern may not include any namespace qualifiers:
	 * that is, the pattern must specify commands in the current (exporting)
	 * namespace. The specified patterns are appended onto the namespace's list
	 * of export patterns.
	 * 
	 * To reset the namespace's export pattern list, specify the "-clear" flag.
	 * 
	 * If there are no export patterns and the "-clear" flag isn't given, this
	 * command returns the namespace's current export list.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result is an error message.
	
	 */

	private static void exportCmd(Interp interp, TclObject[] objv)
			throws TclException {
		Namespace currNs = Namespace.getCurrentNamespace(interp);
		String pattern, string;
		boolean resetListFirst = false;
		int firstArg, patternCt, i;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 2, objv,
					"?-clear? ?pattern pattern...?");
		}

		// Process the optional "-clear" argument.

		firstArg = 2;
		if (firstArg < objv.length) {
			string = objv[firstArg].toString();
			if (string.equals("-clear")) {
				resetListFirst = true;
				firstArg++;
			}
		}

		// If no pattern arguments are given, and "-clear" isn't specified,
		// return the namespace's current export pattern list.

		patternCt = (objv.length - firstArg);
		if (patternCt == 0) {
			if (firstArg > 2) {
				return;
			} else { // create list with export patterns
				TclObject list = TclList.newInstance();
				Namespace.appendExportList(interp, currNs, list);
				interp.setResult(list);
				return;
			}
		}

		// Add each pattern to the namespace's export pattern list.

		for (i = firstArg; i < objv.length; i++) {
			pattern = objv[i].toString();
			Namespace.exportList(interp, currNs, pattern,
					((i == firstArg) ? resetListFirst : false));
		}
		return;
	}

	/**
	 * Invoked to implement the "namespace forget" command to remove imported
	 * commands from a namespace. Handles the following syntax:
	 * 
	 * namespace forget ?pattern pattern...?
	 * 
	 * Each pattern is a name like "foo::*" or "a::b::x*". That is, the pattern
	 * may include the special pattern matching characters recognized by the
	 * "string match" command, but only in the command name at the end of the
	 * qualified name; the special pattern characters may not appear in a
	 * namespace name. All of the commands that match that pattern are checked
	 * to see if they have an imported command in the current namespace that
	 * refers to the matched command. If there is an alias, it is removed.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Imported commands are removed from the current namespace.
	 * If anything goes wrong, this procedure returns an error message in the
	 * interpreter's result object.
	
	 */

	private static void forgetCmd(Interp interp, TclObject[] objv)
			throws TclException {

		String pattern;
		int i;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 2, objv,
					"?pattern pattern...?");
		}

		for (i = 2; i < objv.length; i++) {
			pattern = objv[i].toString();
			Namespace.forgetImport(interp, null, pattern);
		}
		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * NamespaceImportCmd -> importCmd
	 * 
	 * Invoked to implement the "namespace import" command that imports commands
	 * into a namespace. Handles the following syntax:
	 * 
	 * namespace import ?-force? ?pattern pattern...?
	 * 
	 * Each pattern is a namespace-qualified name like "foo::*", "a::b::x*", or
	 * "bar::p". That is, the pattern may include the special pattern matching
	 * characters recognized by the "string match" command, but only in the
	 * command name at the end of the qualified name; the special pattern
	 * characters may not appear in a namespace name. All of the commands that
	 * match the pattern and which are exported from their namespace are made
	 * accessible from the current namespace context. This is done by creating a
	 * new "imported command" in the current namespace that points to the real
	 * command in its original namespace; when the imported command is called,
	 * it invokes the real command.
	 * 
	 * If an imported command conflicts with an existing command, it is treated
	 * as an error. But if the "-force" option is included, then existing
	 * commands are overwritten by the imported commands.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Adds imported commands to the current namespace. If
	 * anything goes wrong, this procedure returns an error message in the
	 * interpreter's result object.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void importCmd(Interp interp, TclObject[] objv)
			throws TclException {

		boolean allowOverwrite = false;
		String string, pattern;
		int i;
		int firstArg;

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 2, objv,
					"?-force? ?pattern pattern...?");
		}

		// Skip over the optional "-force" as the first argument.

		firstArg = 2;
		if (firstArg < objv.length) {
			string = objv[firstArg].toString();
			if (string.equals("-force")) {
				allowOverwrite = true;
				firstArg++;
			}
		}

		// Handle the imports for each of the patterns.

		for (i = firstArg; i < objv.length; i++) {
			pattern = objv[i].toString();
			Namespace.importList(interp, null, pattern, allowOverwrite);
		}
		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * NamespaceInscopeCmd -> inscopeCmd
	 * 
	 * Invoked to implement the "namespace inscope" command that executes a
	 * script in the context of a particular namespace. This command is not
	 * expected to be used directly by programmers; calls to it are generated
	 * implicitly when programs use "namespace code" commands to register
	 * callback scripts. Handles the following syntax:
	 * 
	 * namespace inscope name arg ?arg...?
	 * 
	 * The "namespace inscope" command is much like the "namespace eval" command
	 * except that it has lappend semantics and the namespace must already
	 * exist. It treats the first argument as a list, and appends any arguments
	 * after the first onto the end as proper list elements. For example,
	 * 
	 * namespace inscope ::foo a b c d
	 * 
	 * is equivalent to
	 * 
	 * namespace eval ::foo [concat a [list b c d]]
	 * 
	 * This lappend semantics is important because many callback scripts are
	 * actually prefixes.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the Tcl interpreter's result object.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void inscopeCmd(Interp interp, TclObject[] objv)
			throws TclException {
		Namespace namespace;
		CallFrame frame;
		int i;

		if (objv.length < 4) {
			throw new TclNumArgsException(interp, 2, objv, "name arg ?arg...?");
		}

		// Resolve the namespace reference.

		namespace = getNamespaceFromObj(interp, objv[2]);
		if (namespace == null) {
			throw new TclException(interp, "unknown namespace \""
					+ objv[2].toString() + "\" in inscope namespace command");
		}

		// Make the specified namespace the current namespace.

		frame = interp.newCallFrame();
        // copy objv into frame so 'info level n' can report it
        frame.objv = new TclObject[objv.length];
        System.arraycopy(objv, 0, frame.objv, 0, objv.length);
		Namespace.pushCallFrame(interp, frame, namespace, false);

		// Execute the command. If there is just one argument, just treat it as
		// a script and evaluate it. Otherwise, create a list from the arguments
		// after the first one, then concatenate the first argument and the list
		// of extra arguments to form the command to evaluate.

		try {
			if (objv.length == 4) {
				interp.eval(objv[3], 0);
			} else {
				TclObject[] concatObjv = new TclObject[2];
				TclObject list;
				TclObject cmd;

				list = TclList.newInstance();
				for (i = 4; i < objv.length; i++) {
					try {
						TclList.append(interp, list, objv[i]);
					} catch (TclException ex) {
						list.release(); // free unneeded obj
						throw ex;
					}
				}

				concatObjv[0] = objv[3];
				concatObjv[1] = list;
				list.preserve();
				cmd = Util.concat(0, 1, concatObjv);
				try {
					interp.eval(cmd, 0); // do not pass TCL_EVAL_DIRECT, for
					// compiler only
				} finally {
					list.release(); // we're done with the list object
				}
			}
		} catch (TclException ex) {
			if (ex.getCompletionCode() == TCL.ERROR) {
				interp.addErrorInfo("\n    (in namespace inscope \""
						+ namespace.fullName + "\" script line "
						+ interp.errorLine + ")");
			}
			throw ex;
		} finally {
			Namespace.popCallFrame(interp);
		}

		return;
	}

	/**
	 * Invoked to implement the "namespace origin" command to return the
	 * fully-qualified name of the "real" command to which the specified
	 * "imported command" refers. Handles the following syntax:
	 * 
	 * namespace origin name
	 * 
	 * Results: An imported command is created in an namespace when that
	 * namespace imports a command from another namespace. If a command is
	 * imported into a sequence of namespaces a, b,...,n where each successive
	 * namespace just imports the command from the previous namespace, this
	 * command returns the fully-qualified name of the original command in the
	 * first namespace, a. If "name" does not refer to an alias, its
	 * fully-qualified name is returned. The returned name is stored in the
	 * interpreter's result object. This procedure returns TCL_OK if successful,
	 * and TCL_ERROR if anything goes wrong.
	 * 
	 * Side effects: If anything goes wrong, this procedure returns an error
	 * message in the interpreter's result object.
	 */

	private static void originCmd(Interp interp, TclObject[] objv)
			throws TclException {
		WrappedCommand command, origCommand;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "name");
		}

		// FIXME : is this the right way to search for a command?

		// command = Tcl_GetCommandFromObj(interp, objv[2]);
		command = Namespace.findCommand(interp, objv[2].toString(), null, 0);

		if (command == null) {
			throw new TclException(interp, "invalid command name \""
					+ objv[2].toString() + "\"");
		}

		origCommand = Namespace.getOriginalCommand(command);
		if (origCommand == null) {
			// The specified command isn't an imported command. Return the
			// command's name qualified by the full name of the namespace it
			// was defined in.

			interp.setResult(interp.getCommandFullName(command));
		} else {
			interp.setResult(interp.getCommandFullName(origCommand));
		}
		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * NamespaceParentCmd -> parentCmd
	 * 
	 * Invoked to implement the "namespace parent" command that returns the
	 * fully-qualified name of the parent namespace for a specified namespace.
	 * Handles the following syntax:
	 * 
	 * namespace parent ?name?
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result is an error message.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void parentCmd(Interp interp, TclObject[] objv)
			throws TclException {
		Namespace ns;

		if (objv.length == 2) {
			ns = Namespace.getCurrentNamespace(interp);
		} else if (objv.length == 3) {
			ns = getNamespaceFromObj(interp, objv[2]);
			if (ns == null) {
				throw new TclException(interp, "unknown namespace \""
						+ objv[2].toString() + "\" in namespace parent command");
			}
		} else {
			throw new TclNumArgsException(interp, 2, objv, "?name?");
		}

		// Report the parent of the specified namespace.

		if (ns.parent != null) {
			interp.setResult(ns.parent.fullName);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * NamespaceQualifiersCmd -> qualifiersCmd
	 * 
	 * Invoked to implement the "namespace qualifiers" command that returns any
	 * leading namespace qualifiers in a string. These qualifiers are namespace
	 * names separated by "::"s. For example, for "::foo::p" this command
	 * returns "::foo", and for "::" it returns "". This command is the
	 * complement of the "namespace tail" command. Note that this command does
	 * not check whether the "namespace" names are, in fact, the names of
	 * currently defined namespaces. Handles the following syntax:
	 * 
	 * namespace qualifiers string
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result is an error message.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void qualifiersCmd(Interp interp, TclObject[] objv)
			throws TclException {
		String name;
		int p;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "string");
		}

		// Find the end of the string, then work backward and find
		// the start of the last "::" qualifier.

		name = objv[2].toString();
		p = name.length();

		while (--p >= 0) {
			if ((name.charAt(p) == ':') && (p > 0)
					&& (name.charAt(p - 1) == ':')) {
				p -= 2; // back up over the ::
				while ((p >= 0) && (name.charAt(p) == ':')) {
					p--; // back up over the preceeding :
				}
				break;
			}
		}

		if (p >= 0) {
			interp.setResult(name.substring(0, p + 1));
		}
		// When no result is set the empty string is the result
		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * NamespaceTailCmd -> tailCmd
	 * 
	 * Invoked to implement the "namespace tail" command that returns the
	 * trailing name at the end of a string with "::" namespace qualifiers.
	 * These qualifiers are namespace names separated by "::"s. For example, for
	 * "::foo::p" this command returns "p", and for "::" it returns "". This
	 * command is the complement of the "namespace qualifiers" command. Note
	 * that this command does not check whether the "namespace" names are, in
	 * fact, the names of currently defined namespaces. Handles the following
	 * syntax:
	 * 
	 * namespace tail string
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result is an error message.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void tailCmd(Interp interp, TclObject[] objv)
			throws TclException {
		String name, tail;

		if (objv.length != 3) {
			throw new TclNumArgsException(interp, 2, objv, "string");
		}

		name = objv[2].toString();
		tail = NamespaceCmd.tail(name);
		interp.setResult(tail);
		return;
	}

	// Given a possibly qualified name, return the namespace tail
	// substring that appears after the last pair of colons "::".

	public static String tail(String qname) {

		// Find the last location of "::" in the string.

		int i = qname.lastIndexOf("::");
		String tail;

		if (i == -1) {
			tail = qname;
		} else {
			i += 2; // just after last "::"
			if (i >= qname.length()) {
				tail = "";
			} else {
				tail = qname.substring(i);
			}
		}
		return tail;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * NamespaceWhichCmd -> whichCmd
	 * 
	 * Invoked to implement the "namespace which" command that returns the
	 * fully-qualified name of a command or variable. If the specified command
	 * or variable does not exist, it returns "". Handles the following syntax:
	 * 
	 * namespace which ?-command? ?-variable? name
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result is an error message.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void whichCmd(Interp interp, TclObject[] objv)
			throws TclException {
		String arg;
		WrappedCommand cmd;
		Var variable;
		int argIndex, lookup;

		if (objv.length < 3) {
			throw new TclNumArgsException(interp, 2, objv,
					"?-command? ?-variable? name");
		}

		// Look for a flag controlling the lookup.

		argIndex = 2;
		lookup = 0; // assume command lookup by default
		arg = objv[2].toString();
		if ((arg.length() > 1) && (arg.charAt(0) == '-')) {
			switch (arg) {
				case "-command":
					lookup = 0;
					break;
				case "-variable":
					lookup = 1;
					break;
				default:
					throw new TclNumArgsException(interp, 2, objv,
							"?-command? ?-variable? name");
			}
			argIndex = 3;
		}
		if (objv.length != (argIndex + 1)) {
			throw new TclNumArgsException(interp, 2, objv,
					"?-command? ?-variable? name");
		}

		// FIXME : check that this implementation works!

		switch (lookup) {
		case 0: // -command
			arg = objv[argIndex].toString();

			// FIXME : is this the right way to lookup a Command token?
			// cmd = Tcl_GetCommandFromObj(interp, objv[argIndex]);
			cmd = Namespace.findCommand(interp, arg, null, 0);

			if (cmd == null) {
				return; // cmd not found, just return (no error)
			}
			interp.setResult(interp.getCommandFullName(cmd));
			return;

		case 1: // -variable
			arg = objv[argIndex].toString();
			variable = Namespace.findNamespaceVar(interp, arg, null, 0);
			if (variable != null) {
				interp.setResult(Var.getVariableFullName(interp, variable));
			}
			return;
		}

		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * FreeNsNameInternalRep -> dispose
	 * 
	 * Frees the resources associated with a object's internal representation.
	 * See src/tcljava/tcl/lang/InternalRep.java
	 * 
	 * Results: None.
	 * 
	 * Side effects: Decrements the ref count of any Namespace structure pointed
	 * to by the nsName's internal representation. If there are no more
	 * references to the namespace, it's structure will be freed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void dispose() {
		final boolean debug = false;
		if (debug) {
			System.out.println("dispose() called for namespace object "
					+ (otherValue == null ? null : otherValue.ns));
		}

		Namespace.ResolvedNsName resName = otherValue;
		Namespace ns;

		// Decrement the reference count of the namespace. If there are no
		// more references, free it up.

		if (resName != null) {
			resName.refCount--;
			if (resName.refCount == 0) {

				// Decrement the reference count for the cached namespace. If
				// the namespace is dead, and there are no more references to
				// it, free it.

				ns = resName.ns;
				ns.refCount--;
				if ((ns.refCount == 0) && ((ns.flags & Namespace.NS_DEAD) != 0)) {
					Namespace.free(ns);
				}
				otherValue = null;
			}
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * DupNsNameInternalRep -> duplicate
	 * 
	 * Get a copy of this Object for copy-on-write operations. We just increment
	 * its useCount and return the same ReflectObject because ReflectObject's
	 * cannot be modified, so they don't need copy-on-write protections.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public InternalRep duplicate() {
		final boolean debug = false;
		if (debug) {
			System.out.println("duplicate() called for namespace object "
					+ (otherValue == null ? null : otherValue.ns));
		}

		Namespace.ResolvedNsName resName = otherValue;

		if (resName != null) {
			resName.refCount++;
		}

		return this;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * SetNsNameFromAny -> setNsNameFromAny
	 * 
	 * Attempt to generate a nsName internal representation for a TclObject.
	 * 
	 * Results: Returns if the value could be converted to a proper namespace
	 * reference. Otherwise, raises TclException.
	 * 
	 * Side effects: If successful, the object is made a nsName object. Its
	 * internal rep is set to point to a ResolvedNsName, which contains a cached
	 * pointer to the Namespace. Reference counts are kept on both the
	 * ResolvedNsName and the Namespace, so we can keep track of their usage and
	 * free them when appropriate.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void setNsNameFromAny(Interp interp, // Reference to the namespace in
			// which to
			// resolve name. Also used for error
			// reporting if not null.
			TclObject tobj // The TclObject to convert.
	) throws TclException // If object could not be converted
	{
		String name;
		Namespace ns;
		Namespace.ResolvedNsName resName;

		// Get the string representation.
		name = tobj.toString();

		// Look for the namespace "name" in the current namespace. If there is
		// an error parsing the (possibly qualified) name, return an error.
		// If the namespace isn't found, we convert the object to an nsName
		// object with a null ResolvedNsName internal rep.

		Namespace.GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
		Namespace.getNamespaceForQualName(interp, name, null,
				Namespace.FIND_ONLY_NS, gnfqnr);
		ns = gnfqnr.ns;

		// If we found a namespace, then create a new ResolvedNsName structure
		// that holds a reference to it.

		if (ns != null) {
			Namespace currNs = Namespace.getCurrentNamespace(interp);

			ns.refCount++;
			resName = new Namespace.ResolvedNsName();
			resName.ns = ns;
			resName.nsId = ns.nsId;
			resName.refNs = currNs;
			resName.refCount = 1;
		} else {
			resName = null;
		}

		// By setting the new internal rep we free up the old one.

		// FIXME : should a NamespaceCmd wrap a ResolvedNsName?
		// this is confusing because it seems like the C code uses
		// a ResolvedNsName like it is the InternalRep.

		NamespaceCmd wrap = new NamespaceCmd();
		wrap.otherValue = resName;
		tobj.setInternalRep(wrap);

		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * GetNamespaceFromObj -> getNamespaceFromObj
	 * 
	 * Returns the namespace specified by the name in a TclObject.
	 * 
	 * Results: This method will return the Namespace object whose name is
	 * stored in the obj argument. If the namespace can't be found, a
	 * TclException is raised.
	 * 
	 * Side effects: May update the internal representation for the object,
	 * caching the namespace reference. The next time this procedure is called,
	 * the namespace value can be found quickly.
	 * 
	 * If anything goes wrong, an error message is left in the interpreter's
	 * result object.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static Namespace getNamespaceFromObj(Interp interp, // The current
			// interpreter.
			TclObject obj // The object to be resolved as the name
	// of a namespace.
	) throws TclException {
		Namespace.ResolvedNsName resName;
		Namespace ns;
		Namespace currNs = Namespace.getCurrentNamespace(interp);

		// Get the internal representation, converting to a namespace type if
		// needed. The internal representation is a ResolvedNsName that points
		// to the actual namespace.

		if (!(obj.getInternalRep() instanceof NamespaceCmd)) {
			NamespaceCmd.setNsNameFromAny(interp, obj);
		}
		resName = ((NamespaceCmd) obj.getInternalRep()).otherValue;

		// Check the context namespace of the resolved symbol to make sure that
		// it is fresh. If not, then force another conversion to the namespace
		// type, to discard the old rep and create a new one. Note that we
		// verify that the namespace id of the cached namespace is the same as
		// the id when we cached it; this insures that the namespace wasn't
		// deleted and a new one created at the same address.

		ns = null;
		if ((resName != null) && (resName.refNs == currNs)
				&& (resName.nsId == resName.ns.nsId)) {
			ns = resName.ns;
			if ((ns.flags & Namespace.NS_DEAD) != 0) {
				ns = null;
			}
		}
		if (ns == null) { // try again
			NamespaceCmd.setNsNameFromAny(interp, obj);
			resName = ((NamespaceCmd) obj.getInternalRep()).otherValue;
			if (resName != null) {
				ns = resName.ns;
				if ((ns.flags & Namespace.NS_DEAD) != 0) {
					ns = null;
				}
			}
		}
		return ns;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * UpdateStringOfNsName -> toString
	 * 
	 * Return the string representation for a nsName object. This method is
	 * called only by TclObject.toString() when TclObject.stringRep is null.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public String toString() {
		final boolean debug = false;

		if (debug) {
			System.out.println("toString() called for namespace object "
					+ (otherValue == null ? null : otherValue.ns));
		}

		Namespace.ResolvedNsName resName = otherValue;
		Namespace ns;
		String name = "";

		if ((resName != null) && (resName.nsId == resName.ns.nsId)) {
			ns = resName.ns;
			if ((ns.flags & Namespace.NS_DEAD) != 0) {
				ns = null;
			}
			if (ns != null) {
				name = ns.fullName;
			}
		}

		return name;
	}

}
