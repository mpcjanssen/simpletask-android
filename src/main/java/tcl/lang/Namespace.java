/*
 * Namespace.java
 *
 * Copyright (c) 1993-1997 Lucent Technologies.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-1999 by Scriptics Corporation.
 * Copyright (c) 1999-2005 Moses DeJong
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
 * RCS: @(#) $Id: Namespace.java,v 1.7 2009/07/10 13:18:39 rszulgo Exp $
 */

package tcl.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * This structure contains a cached pointer to a namespace that is the result of
 * resolving the namespace's name in some other namespace. It is the internal
 * representation for a nsName object. It contains the pointer along with some
 * information that is used to check the cached pointer's validity. (ported
 * Tcl_Namespace to Namespace)
 */

public class Namespace {
	/**
	 * /The namespace's simple (unqualified)  name. This contains no ::'s.
	 * The name of the global namespace is "" although "::" is an synonym.
	 */
	public String name; 
	/**
	 * The namespace's fully qualified name.  This starts with ::.
	 */
	public String fullName; 
	/**
	 * method to invoke when namespace is deleted
	 */
	public DeleteProc deleteProc; 

	/**
	 * reference to the namespace that contains this one. null is this is the global namespace.
	 */
	public Namespace parent;
	
	/**
	 * Contains any child namespaces.
	 */
	public HashMap<String, Namespace> childTable;
	
	/**
	 * unique id for the namespace
	 */
	public long nsId; 

	/**
	 * The interpreter containing this namespace.
	 */
	public Interp interp; 

	/**
	 *  OR-ed combination of the namespace status flags NS_DYING and NS_DEAD (listed below)
	 */
	public int flags;

	/**
	 * Number of "activations" or active call frames for this namespace that
	 * are on the Tcl call stack. The namespace won't be freed until
	 * activationCount becomes zero.
	 */
	public int activationCount;

	/**
	 * Count of references by nsName  objects. The namespace can't be freed until refCount becomes zero.
	 */
	public int refCount; 

	/**
	 * Contains all the commands currently registered in the namespace. Indexed
	 * by strings; values have type (WrappedCommand). Commands imported by
	 * Tcl_Import have Command structures that point (via an ImportedCmdRef
	 * structure) to the Command structure in the source namespace's command
	 * table.
	 */
	public HashMap<String, WrappedCommand> cmdTable;
	
	/**
	 * Contains all the (global) variables currently in this namespace. Indexed
	 * by strings; values have type (Var).
	 */
	public HashMap<String, Var> varTable;

	/**
	 * Reference to an array of string patterns specifying which commands are
	 * exported.  A pattern may include "string match" style wildcard
	 * characters to specify multiple commands; however, no namespace //
	 * qualifiers are allowed. null if no export patterns are registered.
	 */
	public String[] exportArray;

	/**
	 * Number of export patterns currently registered using
	 * "namespace export".
	 */
	public int numExportPatterns; 

	/**
	 * Mumber of export patterns for which space is currently allocated.
	 */
	public int maxExportPatterns;

	/**
	 * If non-null, this object overrides the  usual command and variable
	 * resolution  mechanism in Tcl. This procedure is invoked within
	 * findCommand and findNamespaceVar to esolve all command and variable
	 * references  within the namespace.
	 */
	public Resolver resolver;



	/** 
     * @return the full namespace name string
     * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fullName;
	}

	/**
	 *  This interface is used to provide a callback when a namespace is deleted
	 */

	public static interface DeleteProc {
		public void delete();
	}

	// (ported ResolvedNsName to Namespace.ResolvedNsName)

	public static class ResolvedNsName {
		/**
		 * Reference to the namespace object
		 */
		public Namespace ns;
		/**
		 * sPtr's unique namespace id. Used to verify that ns is still valid
		 * (e.g., it's possible that the namespace was deleted and a new
		 * one created at the same address).
		 */
		public long nsId;

		/**
		 * reference to the namespace containing the reference (not the
		 * namespace that contains the referenced namespace).
		 */
		public Namespace refNs; 
		/**
		 * Reference count: 1 for each nsName  object that has a pointer to
		 * this  ResolvedNsName structure as its internal  rep. This
		 * structure can be freed when refCount becomes zero.
		 */
		public int refCount; 
	}

	// Flag passed to getNamespaceForQualName to indicate that it should
	// search for a namespace rather than a command or variable inside a
	// namespace. Note that this flag's value must not conflict with the values
	// of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, or CREATE_NS_IF_UNKNOWN.

	public static final int FIND_ONLY_NS = 0x1000;

	// Initial size of array of namespace refs - used in resetShadowedCmdRefs()

	private static final int NUM_TRAIL_ELEMS = 5;

	// Count of the number of namespaces created. This value is used as a
	// unique id for each namespace.

	private static long numNsCreated = 0;
	private static Object nsMutex = new Object();

	/**
	 * NS_DYING - 1 means deleteNamespace has been called to delete the
	 * namespace but there are still active call frames on the Tcl stack that
	 * refer to the namespace. When the last call frame referring to it has been
	 * popped, it's variables and command will be destroyed and it will be
	 * marked "dead" (NS_DEAD). The namespace can no longer be looked up by
	 * name.
	 */
	static final int NS_DYING = 0x01;
	
	/**
	 * NS_DEAD - 1 means deleteNamespace has been called to delete the namespace
	 * and no call frames still refer to it. Its variables and command have
	 * already been destroyed. This bit allows the namespace resolution code to
	 * recognize that the namespace is "deleted". When the last namespaceName
	 * object in any byte code code unit that refers to the namespace has been
	 * freed (i.e., when the namespace's refCount is 0), the namespace's storage
	 * will be freed.
	 */
	public static final int NS_DEAD = 0x02;

	/**
	 * Flag passed to getNamespaceForQualName to have it create all namespace
	 * components of a namespace-qualified name that cannot be found. The new
	 * namespaces are created within their specified parent. Note that this
	 * flag's value must not conflict with the values of the flags
	 * TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY, and FIND_ONLY_NS
	 */
	public static final int CREATE_NS_IF_UNKNOWN = 0x800;

	/**
	 * Returns a reference to an interpreter's currently active namespace.
	 * 
	 * @param interp
	 *            the current interpreter
	 * @return a reference to the interpreter's current namespace
	 */
	public static Namespace getCurrentNamespace(Interp interp) {
		if (interp.varFrame != null) {
			return interp.varFrame.ns;
		} else {
			return interp.globalNs;
		}
	}

	
	/**
	 * Returns a reference to an interpreter's global :: namespace.
	 * 
	 * @param interp
	 *            the current interpreter
	 * @return a reference to the interpreter's current namespace
	 */
	public static Namespace getGlobalNamespace(Interp interp) {
		return interp.globalNs;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_PushCallFrame -> pushCallFrame
	 * 
	 * Pushes a new call frame onto the interpreter's Tcl call stack. Called
	 * when executing a Tcl procedure or a "namespace eval" or
	 * "namespace inscope" command.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Modifies the interpreter's Tcl call stack.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static void pushCallFrame(Interp interp, // Interpreter in which the
													// new call frame
			// is to be pushed.
			CallFrame frame, // Points to a call frame object to
			// push. The call frame will be initialized
			// by this method. The caller can pop the frame
			// later with popCallFrame.
			Namespace namespace, // Points to the namespace in which the
			// interpreter's current namespace will
			// be used.
			boolean isProcCallFrame) // If true, the frame represents a
	// called Tcl procedure and may have local
	// vars. Vars will ordinarily be looked up
	// in the frame. If new variables are
	// created, they will be created in the
	// frame. If false, the frame is for a
	// "namespace eval" or "namespace inscope"
	// command and var references are treated
	// as references to namespace variables.
	{
		Namespace ns;

		if (namespace == null) {
			ns = getCurrentNamespace(interp);
		} else {
			ns = namespace;
			if ((ns.flags & NS_DEAD) != 0) {
				throw new TclRuntimeError(
						"Trying to push call frame for dead namespace");
			}
		}

		ns.activationCount++;
		frame.ns = ns;
		frame.isProcCallFrame = isProcCallFrame;
		if (isProcCallFrame) {
            // don't override objv for namespace callframes;
            // it is stuffed with the objv to namespace for
            // info level n reporting
            frame.objv = null;
        }

		frame.caller = interp.frame;
		frame.callerVar = interp.varFrame;

		if (interp.varFrame != null) {
			frame.level = (interp.varFrame.level + 1);
		} else {
			frame.level = 1;
		}

		// FIXME : does Jacl need a procPtr in the CallFrame class?
		// frame.procPtr = null; // no called procedure

		frame.varTable = null; // and no local variables

		// Compiled locals are not part of Jacl's CallFrame

		// Push the new call frame onto the interpreter's stack of procedure
		// call frames making it the current frame.

		interp.frame = frame;
		interp.varFrame = frame;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_PopCallFrame -> popCallFrame
	 * 
	 * Removes a call frame from the Tcl call stack for the interpreter. Called
	 * to remove a frame previously pushed by Tcl_PushCallFrame.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Modifies the call stack of the interpreter. Resets various
	 * fields of the popped call frame. If a namespace has been deleted and has
	 * no more activations on the call stack, the namespace is destroyed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static void popCallFrame(Interp interp) {
		CallFrame frame = interp.frame;
		int saveErrFlag;
		Namespace ns;

		// It's important to remove the call frame from the interpreter's stack
		// of call frames before deleting local variables, so that traces
		// invoked by the variable deletion don't see the partially-deleted
		// frame.

		interp.frame = frame.caller;
		interp.varFrame = frame.callerVar;

		// Delete the local variables. As a hack, we save then restore the
		// ERR_IN_PROGRESS flag in the interpreter. The problem is that there
		// could be unset traces on the variables, which cause scripts to be
		// evaluated. This will clear the ERR_IN_PROGRESS flag, losing stack
		// trace information if the procedure was exiting with an error. The
		// code below preserves the flag. Unfortunately, that isn't really
		// enough: we really should preserve the errorInfo variable too
		// (otherwise a nested error in the trace script will trash errorInfo).
		// What's really needed is a general-purpose mechanism for saving and
		// restoring interpreter state.

		saveErrFlag = (interp.flags & Parser.ERR_IN_PROGRESS);

		if (frame.varTable != null) {
			Var.deleteVars(interp, frame.varTable);
			frame.varTable = null;
		}

		interp.flags |= saveErrFlag;

		// Decrement the namespace's count of active call frames. If the
		// namespace is "dying" and there are no more active call frames,
		// call Tcl_DeleteNamespace to destroy it.

		ns = frame.ns;
		ns.activationCount--;
		if (((ns.flags & NS_DYING) != 0) && (ns.activationCount == 0)) {
			deleteNamespace(ns);
		}
		frame.ns = null;
	}

	/**
	 * Creates a new namespace with the given name. If there is no active
	 * namespace (i.e., the interpreter is being initialized), the global ::
	 * namespace is created and returned.
	 * 
	 * @param interp
	 *            interpreter in which to create new namespace
	 * @param name
	 *            name for the new namespace; may be a qualified name with the
	 *            ancestors separated with ::'s
	 * @param deleteProc
	 *            procedure called when namespace is deleted, or null if none
	 *            should be called
	 * 
	 * @return a reference to the new namespace if successful. If the namespace
	 *         already exists or if another error occurs, this routine returns
	 *         null, along with an error message in the interpreter's result
	 *         object.
	 * 
	 *         Side effects: If the name contains "::" qualifiers and a parent
	 *         namespace does not already exist, it is automatically created.
	 */

	public static Namespace createNamespace(Interp interp, 
			String name,
			DeleteProc deleteProc 
	) {
		Namespace ns, ancestor;
		Namespace parent;
		Namespace globalNs = getGlobalNamespace(interp);
		String simpleName;
		StringBuffer buffer1, buffer2;

		// If there is no active namespace, the interpreter is being
		// initialized.

		if ((globalNs == null) && (interp.varFrame == null)) {
			// Treat this namespace as the global namespace, and avoid
			// looking for a parent.

			parent = null;
			simpleName = "";
		} else if (name.length() == 0) {
			/*
			 * TclObject tobj = interp.getResult(); // FIXME : is there a test
			 * case to check this error result? TclString.append(tobj,
			 * "can't create namespace \"\": only global namespace can have empty name"
			 * );
			 */

			// FIXME : is there a test case to check this error result?
			interp
					.setResult("can't create namespace \"\": only global namespace can have empty name");
			return null;
		} else {
			// Find the parent for the new namespace.

			GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
			getNamespaceForQualName(interp, name, null,
					(CREATE_NS_IF_UNKNOWN | TCL.LEAVE_ERR_MSG), gnfqnr);
			parent = gnfqnr.ns;
			simpleName = gnfqnr.simpleName;

			// If the unqualified name at the end is empty, there were trailing
			// "::"s after the namespace's name which we ignore. The new
			// namespace was already (recursively) created and is referenced
			// by parent.

			if (simpleName.length() == 0) {
				return parent;
			}

			// Check for a bad namespace name and make sure that the name
			// does not already exist in the parent namespace.

			if (parent.childTable.get(simpleName) != null) {
				/*
				 * TclObject tobj = interp.getResult(); // FIXME : is there a
				 * test case to check this error result? TclString.append(tobj,
				 * "can't create namespace \"" + name + "\": already exists");
				 */

				// FIXME : is there a test case to check this error result?
				interp.setResult("can't create namespace \"" + name
						+ "\": already exists");
				return null;
			}
		}

		// Create the new namespace and root it in its parent. Increment the
		// count of namespaces created.

		ns = new Namespace();
		ns.name = simpleName;
		ns.fullName = null; // set below
		// ns.clientData = clientData;
		ns.deleteProc = deleteProc;
		ns.parent = parent;
		ns.childTable = new HashMap();
		synchronized (nsMutex) {
			numNsCreated++;
			ns.nsId = numNsCreated;
		}
		ns.interp = interp;
		ns.flags = 0;
		ns.activationCount = 0;
		// FIXME : there was a problem with the refcount because
		// when the namespace was deleted the refocount was 0.
		// We avoid this by just using a refcount of 1 for now.
		// We can do ignore the refCount because GC will reclaim mem.
		// ns.refCount = 0;
		ns.refCount = 1;
		ns.cmdTable = new HashMap();
		ns.varTable = new HashMap();
		ns.exportArray = null;
		ns.numExportPatterns = 0;
		ns.maxExportPatterns = 0;

		// Jacl does not use these tcl compiler specific members
		// ns.cmdRefEpoch = 0;
		// ns.resolverEpoch = 0;

		ns.resolver = null;

		if (parent != null) {
			parent.childTable.put(simpleName, ns);
		}

		// Build the fully qualified name for this namespace.

		buffer1 = new StringBuffer();
		buffer2 = new StringBuffer();
		for (ancestor = ns; ancestor != null; ancestor = ancestor.parent) {
			if (ancestor != globalNs) {
				buffer1.append("::");
				buffer1.append(ancestor.name);
			}
			buffer1.append(buffer2);

			buffer2.setLength(0);
			buffer2.append(buffer1);
			buffer1.setLength(0);
		}

		name = buffer2.toString();
		ns.fullName = name;

		// Return a reference to the new namespace.

		return ns;
	}

	/**
	 * Deletes a namespace and all of the commands, variables, and other
	 * namespaces within it.
	 * 
	 * Results: None.
	 * 
	 * Side effects: When a namespace is deleted, it is automatically removed as
	 * a child of its parent namespace. Also, all its commands, variables and
	 * child namespaces are deleted.
	 * 
	 * @param namespace namespace to delete
	 */
	public static void deleteNamespace(Namespace namespace) {
		Namespace ns = namespace;
		Interp interp = ns.interp;
		Namespace globalNs = getGlobalNamespace(interp);

		// If the namespace is on the call frame stack, it is marked as "dying"
		// (NS_DYING is OR'd into its flags): the namespace can't be looked up
		// by name but its commands and variables are still usable by those
		// active call frames. When all active call frames referring to the
		// namespace have been popped from the Tcl stack, popCallFrame will
		// call this procedure again to delete everything in the namespace.
		// If no nsName objects refer to the namespace (i.e., if its refCount
		// is zero), its commands and variables are deleted and the storage for
		// its namespace structure is freed. Otherwise, if its refCount is
		// nonzero, the namespace's commands and variables are deleted but the
		// structure isn't freed. Instead, NS_DEAD is OR'd into the structure's
		// flags to allow the namespace resolution code to recognize that the
		// namespace is "deleted".

		if (ns.activationCount > 0) {
			ns.flags |= NS_DYING;
			if (ns.parent != null) {
				ns.parent.childTable.remove(ns.name);
			}
			ns.parent = null;
		} else {
			// Delete the namespace and everything in it. If this is the global
			// namespace, then clear it but don't free its storage unless the
			// interpreter is being torn down.

			teardownNamespace(ns);

			if ((ns != globalNs) || ((interp.flags & Parser.DELETED) != 0)) {
				// If this is the global namespace, then it may have residual
				// "errorInfo" and "errorCode" variables for errors that
				// occurred while it was being torn down. Try to clear the
				// variable list one last time.

				Var.deleteVars(ns.interp, ns.varTable);

				ns.childTable.clear();
				ns.cmdTable.clear();

				// If the reference count is 0, then discard the namespace.
				// Otherwise, mark it as "dead" so that it can't be used.

				if (ns.refCount == 0) {
					free(ns);
				} else {
					ns.flags |= NS_DEAD;
				}
			}
		}
	}

	/**
	 * Used internally to dismantle and unlink a namespace when it is deleted.
	 * Divorces the namespace from its parent, and deletes all commands,
	 * variables, and child namespaces.
	 * 
	 * This is kept separate from Tcl_DeleteNamespace so that the global
	 * namespace can be handled specially. Global variables like "errorInfo" and
	 * "errorCode" need to remain intact while other namespaces and commands are
	 * torn down, in case any errors occur.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Removes this namespace from its parent's child namespace
	 * hashtable. Deletes all commands, variables and namespaces in this
	 * namespace. If this is the global namespace, the "errorInfo" and
	 * "errorCode" variables are left alone and deleted later.
	 *
	 * @param ns namespace to tear down
	 */
	static void teardownNamespace(Namespace ns) {
		Interp interp = ns.interp;
		Namespace childNs;
		WrappedCommand cmd;
		Namespace globalNs = getGlobalNamespace(interp);
		int i;

		/*
		 * Fire the command traces early, and then delete the traces so they don't 
		 * fire again when we actually delete the commands.  This prevents the teardown
		 * process from interfering with trace execution.  Since traces may delete other
		 * commands, separate the iteration through the command table from the trace
		 * execution.
		 */
		ArrayList<String> tracedCommands = new ArrayList<>();
		for (Map.Entry<String, WrappedCommand> entry : ns.cmdTable.entrySet()) {
			if (entry.getValue().hasCommandTraces()) {
				tracedCommands.add(entry.getKey());
			}
		}
		for (String cmdName : tracedCommands) {
			cmd = ns.cmdTable.get(cmdName);
			if (cmd != null) {
				cmd.callCommandTraces(CommandTrace.DELETE,"");
				cmd.removeAllCommandTraces();
			}
		}
		tracedCommands = null;
		
		
		if (ns == globalNs) {
			// This is the global namespace, so be careful to preserve the
			// "errorInfo" and "errorCode" variables. These might be needed
			// later on if errors occur while deleting commands. We are careful
			// to destroy and recreate the "errorInfo" and "errorCode"
			// variables, in case they had any traces on them.

			String errorInfoStr, errorCodeStr;

			try {
				errorInfoStr = interp.getVar("errorInfo", TCL.GLOBAL_ONLY)
						.toString();
			} catch (TclException e) {
				errorInfoStr = null;
			}

			try {
				errorCodeStr = interp.getVar("errorCode", TCL.GLOBAL_ONLY)
						.toString();
			} catch (TclException e) {
				errorCodeStr = null;
			}

			Var.deleteVars(interp, ns.varTable);

			if (errorInfoStr != null) {
				try {
					interp.setVar("errorInfo", errorInfoStr, TCL.GLOBAL_ONLY);
				} catch (TclException e) {
					// ignore an exception while setting this var
				}
			}
			if (errorCodeStr != null) {
				try {
					interp.setVar("errorCode", errorCodeStr, TCL.GLOBAL_ONLY);
				} catch (TclException e) {
					// ignore an exception while setting this var
				}
			}
		} else {
			// Variable table should be cleared.
			Var.deleteVars(interp, ns.varTable);
		}

		
		// Remove the namespace from its parent's child hashtable.

		if (ns.parent != null) {
			ns.parent.childTable.remove(ns.name);
		}
		ns.parent = null;

		// Delete all the child namespaces.
		//
		// BE CAREFUL: When each child is deleted, it will divorce
		// itself from its parent. You can't traverse a hash table
		// properly if its elements are being deleted. We use only
		// the Tcl_FirstHashEntry function to be safe.

		while ((childNs = (Namespace) FirstHashEntry(ns.childTable)) != null) {
			deleteNamespace(childNs);
		}

		// Delete all commands in this namespace. Be careful when traversing the
		// hash table: when each command is deleted, it removes itself from the
		// command table.

		while ((cmd = (WrappedCommand) FirstHashEntry(ns.cmdTable)) != null) {
			interp.deleteCommandFromToken(cmd);
		}

		// Free the namespace's export pattern array.

		if (ns.exportArray != null) {
			ns.exportArray = null;
			ns.numExportPatterns = 0;
			ns.maxExportPatterns = 0;
		}

		// Callback invoked when namespace is deleted

		if (ns.deleteProc != null) {
			ns.deleteProc.delete();
		}
		ns.deleteProc = null;

		// Reset the namespace's id field to ensure that this namespace won't
		// be interpreted as valid by, e.g., the cache validation code for
		// cached command references in Tcl_GetCommandFromObj.

		ns.nsId = 0;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * NamespaceFree -> free
	 * 
	 * Called after a namespace has been deleted, when its reference count
	 * reaches 0. Frees the data structure representing the namespace.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static void free(Namespace ns) {
		// Most of the namespace's contents are freed when the namespace is
		// deleted by Tcl_DeleteNamespace. All that remains is to free its names
		// (for error messages), and the structure itself.

		ns.name = null;
		ns.fullName = null;
	}

	/**
	 * Makes all the commands matching a pattern available to later be imported
	 * from the namespace specified by namespace (or the current namespace if
	 * namespace is null). The specified pattern is appended onto the
	 * namespace's export pattern list, which is optionally cleared beforehand.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Appends the export pattern onto the namespace's export
	 * list. Optionally reset the namespace's export pattern list.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param namespace
	 *            export commands from this namespace, or null for the current
	 *            namespace
	 * @param pattern
	 *            String pattern indicating which commands to export. This
	 *            pattern may not include any namespace qualifiers; only
	 *            commands // in the specified namespace may be exported.
	 * @param resetListFirst
	 *            If true, resets the namespace's export list before
	 *            appending. If false, return an error if an imported  cmd
	 *            conflicts
	 */

	public static void exportList(Interp interp,
			Namespace namespace,
			String pattern, 
			boolean resetListFirst 
	) throws TclException {
		final int INIT_EXPORT_PATTERNS = 5;
		Namespace ns, exportNs;
		Namespace currNs = getCurrentNamespace(interp);
		String simplePattern, patternCpy;
		int neededElems, len, i;

		// If the specified namespace is null, use the current namespace.

		if (namespace == null) {
			ns = currNs;
		} else {
			ns = namespace;
		}

		// If resetListFirst is true (nonzero), clear the namespace's export
		// pattern list.

		if (resetListFirst) {
			if (ns.exportArray != null) {
				for (i = 0; i < ns.numExportPatterns; i++) {
					ns.exportArray[i] = null;
				}
				ns.exportArray = null;
				ns.numExportPatterns = 0;
				ns.maxExportPatterns = 0;
			}
		}

		// Check that the pattern doesn't have namespace qualifiers.

		GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
		getNamespaceForQualName(interp, pattern, ns, TCL.LEAVE_ERR_MSG, gnfqnr);
		exportNs = gnfqnr.ns;
		simplePattern = gnfqnr.simpleName;

		if ((exportNs != ns) || (pattern.compareTo(simplePattern) != 0)) {
			throw new TclException(interp, "invalid export pattern \""
					+ pattern + "\": pattern can't specify a namespace");
		}

		// Make sure there is room in the namespace's pattern array for the
		// new pattern.

		neededElems = ns.numExportPatterns + 1;
		if (ns.exportArray == null) {
			ns.exportArray = new String[INIT_EXPORT_PATTERNS];
			ns.numExportPatterns = 0;
			ns.maxExportPatterns = INIT_EXPORT_PATTERNS;
		} else if (neededElems > ns.maxExportPatterns) {
			int numNewElems = 2 * ns.maxExportPatterns;
			String[] newArray = new String[numNewElems];
			System.arraycopy(ns.exportArray, 0, newArray, 0,
					ns.numExportPatterns);
			ns.exportArray = newArray;
			ns.maxExportPatterns = numNewElems;
		}

		// Add the pattern to the namespace's array of export patterns.

		ns.exportArray[ns.numExportPatterns] = pattern;
		ns.numExportPatterns++;
		return;
	}

	/**
	 * Appends onto the argument object the list of export patterns for the
	 * specified namespace.
	 * 
	 * Results: The method will return when successful; in this case the object
	 * referenced by obj has each export pattern appended to it. If an error
	 * occurs, an exception and the interpreter's result holds an error message.
	 * 
	 * Side effects: If necessary, the object referenced by obj is converted
	 * into a list object.
	 * 
	 * @param interp
	 *            the interpreter used for error reporting
	 * @param namespace
	 *            points to the namespace whose export pattern list is appended
	 *            onto obj; null for current namespace
	 * @param obj
	 *            Tcl object onto which current export pattern list is appended
	 */

	public static void appendExportList(Interp interp, Namespace namespace, TclObject obj ) throws TclException {
		Namespace ns;
		int i;

		// If the specified namespace is null, use the current namespace.

		if (namespace == null) {
			ns = getCurrentNamespace(interp);
		} else {
			ns = namespace;
		}

		if (ns.exportArray==null) return;
		
		/* Unique-fy the list */
		HashSet<String> unique = new HashSet<>(ns.exportArray.length);
		for (i = 0; i < ns.numExportPatterns; i++) {
			unique.add(ns.exportArray[i]);
		}
		// Append the export pattern list onto objPtr.
		for (String export : unique) {
			TclList.append(interp, obj, TclString.newInstance(export));
		}
		return;
	}

	/**
	 * Imports all of the commands matching a pattern into the namespace
	 * specified by namespace (or the current namespace if namespace is null).
	 * This is done by creating a new command (the "imported command") that
	 * points to the real command in its original namespace.
	 * 
	 * If matching commands are on the autoload path but haven't been loaded
	 * yet, this command forces them to be loaded, then creates the links to
	 * them. Side effects: Creates new commands in the importing namespace.
	 * These indirect calls back to the real command and are deleted if the real
	 * commands are deleted.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param namespace
	 *            reference to the namespace into which the commands are to be
	 *            imported. null for the current namespace.
	 * @param pattern
	 *            String pattern indicating which commands to import. This
	 *            pattern should be qualified by the name of the namespace from
	 *            which to import the command(s).
	 * @param allowOverwrite
	 *            If true, allow existing commands to  be overwritten by
	 *            imported commands. If 0, return an error if an imported 
	 *            cmd conflicts with an existing one. 
	 */

	public static void importList(Interp interp, Namespace namespace, String pattern, boolean allowOverwrite) throws TclException {
		Namespace ns, importNs;
		Namespace currNs = getCurrentNamespace(interp);
		String simplePattern, cmdName;
		WrappedCommand cmd, realCmd;
		ImportRef ref;
		WrappedCommand autoCmd, importedCmd;
		ImportedCmdData data;
		boolean wasExported;
		int i, result;

		// If the specified namespace is null, use the current namespace.

		if (namespace == null) {
			ns = currNs;
		} else {
			ns = namespace;
		}

		// First, invoke the "auto_import" command with the pattern
		// being imported. This command is part of the Tcl library.
		// It looks for imported commands in autoloaded libraries and
		// loads them in. That way, they will be found when we try
		// to create links below.

		autoCmd = findCommand(interp, "auto_import", null, TCL.GLOBAL_ONLY);

		if (autoCmd != null) {
			TclObject[] objv = new TclObject[2];

			objv[0] = TclString.newInstance("auto_import");
			objv[0].preserve();
			objv[1] = TclString.newInstance(pattern);
			objv[1].preserve();

			cmd = autoCmd;
			try {
				// Invoke the command with the arguments
				if (cmd.mustCallInvoke(interp)) cmd.invoke(interp, objv);
				else cmd.cmd.cmdProc(interp, objv);
			} finally {
				objv[0].release();
				objv[1].release();
			}
			interp.setResult("");
		}

		// From the pattern, find the namespace from which we are importing
		// and get the simple pattern (no namespace qualifiers or ::'s) at
		// the end.

		if (pattern.length() == 0) {
			throw new TclException(interp, "empty import pattern");
		}

		GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
		getNamespaceForQualName(interp, pattern, ns, TCL.LEAVE_ERR_MSG, gnfqnr);
		importNs = gnfqnr.ns;
		simplePattern = gnfqnr.simpleName;

		if (importNs == null) {
			throw new TclException(interp,
					"unknown namespace in import pattern \"" + pattern + "\"");
		}
		if (importNs == ns) {
			if (pattern.equals(simplePattern)) {
				throw new TclException(interp,
						"no namespace specified in import pattern \"" + pattern
								+ "\"");
			} else {
				throw new TclException(interp, "import pattern \"" + pattern
						+ "\" tries to import from namespace \""
						+ importNs.name + "\" into itself");
			}
		}

		/* Scan through the command table and make a list of any built-in
		 * commands that should be loaded before we import
		 */
		HashMap<String, AutoloadStub> toBeLoaded = new HashMap<>();
		for (Map.Entry<String, WrappedCommand> entry : importNs.cmdTable.entrySet()) {
			cmdName = entry.getKey();
			if (Util.stringMatch(cmdName, simplePattern)) {
				cmd = (WrappedCommand) importNs.cmdTable.get(cmdName);
				if (cmd.cmd instanceof AutoloadStub) {
					AutoloadStub autoloadCmd = (AutoloadStub) cmd.cmd;
					toBeLoaded.put(cmdName, autoloadCmd);
				}
			}
		}
		
		/* Now load all the commands that were found */

		for (Map.Entry<String, AutoloadStub> entry : toBeLoaded.entrySet()) {
			cmdName = entry.getKey();
			AutoloadStub stubcmd = entry.getValue();
			stubcmd.load(interp, cmdName);
		}
		// Scan through the command table in the source namespace and look for
		// exported commands that match the string pattern. Create an "imported
		// command" in the current namespace for each imported command; these
		// commands redirect their invocations to the "real" command.
		for (Map.Entry<String, WrappedCommand> entry : importNs.cmdTable.entrySet()) {
			cmdName = entry.getKey();

			if (Util.stringMatch(cmdName, simplePattern)) {
				// The command cmdName in the source namespace matches the
				// pattern. Check whether it was exported. If it wasn't,
				// we ignore it.

				wasExported = false;
				for (i = 0; i < importNs.numExportPatterns; i++) {
					if (Util.stringMatch(cmdName, importNs.exportArray[i])) {
						wasExported = true;
						break;
					}
				}
				if (!wasExported) {
					continue;
				}

				// Unless there is a name clash, create an imported command
				// in the current namespace that refers to cmdPtr.
				WrappedCommand oldCommand = ns.cmdTable.get(cmdName);
				if (oldCommand == null || allowOverwrite) {


					// Create the imported command and its client data.
					// To create the new command in the current namespace,
					// generate a fully qualified name for it.

					StringBuffer ds;

					ds = new StringBuffer();
					ds.append(ns.fullName);
					if (ns != interp.globalNs) {
						ds.append("::");
					}
					ds.append(cmdName);

					cmd = (WrappedCommand) importNs.cmdTable.get(cmdName);

					// Check whether creating the new imported command in the
					// current namespace would create a cycle of imported->real
					// command references that also would destroy an existing
					// "real" command already in the current namespace.
					realCmd = cmd;
					ArrayList<String> importPath = new ArrayList<>();
					importPath.add(ns.fullName + "::" + cmdName);
					while (realCmd.cmd instanceof ImportedCmdData) {
						realCmd = ((ImportedCmdData) realCmd.cmd).realCmd;
						/* What's the name of realCmd in it's namespace?  We could try the obvious */
						String cmdPath = "";
						WrappedCommand testcmd = realCmd.ns.cmdTable.get(cmdName);
						if (testcmd != realCmd) {
							/* Need to actually search for it */
							for (Map.Entry<String, WrappedCommand> entr : realCmd.ns.cmdTable.entrySet()) {
								if (entr.getValue() == realCmd) {
									cmdPath = realCmd.ns.fullName + "::" + entr.getKey();
									break;
								}
							}
						} else {
							cmdPath = realCmd.ns.fullName + "::" + cmdName;
						}

						if (importPath.contains(cmdPath)) {
							throw new TclException(interp,
									"import pattern \"" + pattern + "\" would create a loop containing command \""
											+ cmdPath + "\"");
						}
						importPath.add(cmdPath);
					}


					data = new ImportedCmdData();

					// Create the imported command inside the interp
					interp.createCommand(ds.toString(), data);

					// Lookup in the namespace for the new WrappedCommand
					importedCmd = findCommand(interp, ds.toString(), ns,
							(TCL.NAMESPACE_ONLY | TCL.LEAVE_ERR_MSG));

					data.realCmd = cmd;
					data.self = importedCmd;

					// Create an ImportRef structure describing this new import
					// command and add it to the import ref list in the "real"
					// command.

					ref = new ImportRef();
					ref.importedCmd = importedCmd;
					ref.next = cmd.importRef;
					cmd.importRef = ref;
				} else {
					/* Don't throw exception if oldCommand that already exists
					 * is simply this command, already imported into this namespace
					 * We should be allowed to do 'namespace import exported::*' twice without error
					 */
					boolean throwException = true;
					if (oldCommand.cmd instanceof ImportedCmdData) {
						WrappedCommand realOldCmd = getOriginalCommand(oldCommand);
						cmd = importNs.cmdTable.get(cmdName);
						if (realOldCmd == cmd) {
							throwException = false;
						}
					}
					if (throwException)
						throw new TclException(interp, "can't import command \""
								+ cmdName + "\": already exists");
				}
			}
		}
		return;
	}

	/**
	 * Deletes previously imported commands. 
	 * 
	 * @param interp
	 *            current interpreter
	 * @param namespace
	 *            Points to the namespace from which previously imported
	 *            commands should be removed. null for current namespace.
	 * @param pattern
	 *            String pattern indicating which importedcommands to
	 *            remove. This pattern may be qualified by the name of the
	 *            namespace from which the command(s) were imported.
	 */
	public static void forgetImport(Interp interp, Namespace namespace, String pattern) throws TclException {
		Namespace ns, importNs;
		String simplePattern, cmdName;
		WrappedCommand cmd;

		// set 'ns' to the namespace from which imported commands are to be forgotten.
		if (namespace == null) {
			ns = getCurrentNamespace(interp);
		} else {
			ns = namespace;
		}

		// From the pattern, find the namespace from the to-be-deleted commands were imported
		// and get the simple pattern (no namespace qualifiers or ::'s) at
		// the end.  If no namespace was specified in the pattern, 'importNs' becomes the same
		// as 'ns'.
		GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
		getNamespaceForQualName(interp, pattern, ns, TCL.LEAVE_ERR_MSG, gnfqnr);
		importNs = gnfqnr.ns;
		simplePattern = gnfqnr.simpleName;
		
		// FIXME : the above call passes TCL.LEAVE_ERR_MSG, but
		// it seems like this will be a problem when exception is raised!
		if (importNs == null) {
			throw new TclException(interp,
					"unknown namespace in namespace forget pattern \""
							+ pattern + "\"");
		}
		
		// Scan through the command table in the 'importNs'  namespace and look for
		// exported commands that match the string pattern.   The 'importNs' namespace
		// may actually be 'ns', if the pattern was not qualified with
		// a namespace.  Place commands that match the pattern in matchingCommands.
		
		ArrayList<WrappedCommand> matchingCommands = new ArrayList<>();

		for (Map.Entry<String, WrappedCommand> entry : importNs.cmdTable.entrySet()) {
			cmdName = entry.getKey();
			cmd = entry.getValue();

			if (Util.stringMatch(cmdName, simplePattern) && cmd != null) {
				matchingCommands.add(cmd);
			}
		}
		
		if (ns == importNs) { /* If the imported namespace was not specified, just delete all matching commands and return  */
			for (WrappedCommand c : matchingCommands) {
				interp.deleteCommandFromToken(c);
			}

		} else {  /* importNs was specified as part of the pattern, so only delete commands that are imported from it */
			
			/* Go through all the commands in 'ns' and find imported commands that are imported from importNs and
			 * are listed in the matchingCommands list
			 */
			ArrayList<WrappedCommand> commandsToDelete = new ArrayList<>(matchingCommands.size());
			for (Map.Entry<String, WrappedCommand> entry : ns.cmdTable.entrySet()) {
				cmd = entry.getValue();

				if (cmd.cmd instanceof ImportedCmdData) {
					ImportedCmdData importedCmdData = (ImportedCmdData) cmd.cmd;
					WrappedCommand originalCommand = getOriginalCommand(cmd);
					/* Delete command if the command was directly imported from importNs, or if the original command
					 * exists in importNs
					 */
					if ((importedCmdData.realCmd.ns == importNs && matchingCommands.contains(importedCmdData.realCmd))
							|| (originalCommand.ns == importNs && matchingCommands.contains(originalCommand))) {

						commandsToDelete.add(cmd);
					}

				}
			}
			/* Finally, delete the commands */			
			for (WrappedCommand c : commandsToDelete) {
				interp.deleteCommandFromToken(c);
			}

		}
		
		return;
	}

	
	/**
	 * An imported command is created in a namespace when a "real" command is
	 * imported from another namespace. If the specified command is an imported
	 * command, this procedure returns the original command it refers to.
	 * 
	 * @param command
	 *            the imported command for which the original command should be
	 *            returned
	 * @return If the command was imported into a sequence of namespaces a,
	 *         b,...,n where each successive namespace just imports the command
	 *         from the previous namespace, this procedure returns the
	 *         Tcl_Command token in the first namespace, a. Otherwise, if the
	 *         specified command is not an imported command, the procedure
	 *         returns null.
	 */
	public static WrappedCommand getOriginalCommand(WrappedCommand command ) {
		WrappedCommand cmd = command;
		ImportedCmdData data;

		if (!(cmd.cmd instanceof ImportedCmdData)) {
			return null;
		}

		while (cmd.cmd instanceof ImportedCmdData) {
			data = (ImportedCmdData) cmd.cmd;
			cmd = data.realCmd;
		}
		return cmd;
	}

	/**
	 * Invoked by Tcl whenever the user calls an imported command that was
	 * created by Tcl_Import. Finds the "real" command (in another namespace),
	 * and passes control to it.
	 * 
	 * Results: Returns if successful, raises TclException if something goes
	 * wrong.
	 * 
	 * Side effects: Returns a result in the interpreter's result object. If
	 * anything goes wrong, the result object is set to an error message.
	 * 
	 * @param interp
	 * @param data the data object for this imported command
	 * @param objv argument objects
	 */
	static void invokeImportedCmd(Interp interp, // Current interpreter.
			ImportedCmdData data, // The data object for this imported command
			TclObject[] objv // Argument objects
	) throws TclException {
		WrappedCommand realCmd = data.realCmd;
		if (realCmd.mustCallInvoke(interp)) realCmd.invoke(interp, objv);
		else realCmd.cmd.cmdProc(interp, objv);
	}

	/**
	 * Invoked by Tcl whenever an imported command is deleted. The "real"
	 * command keeps a list of all the imported commands that refer to it, so
	 * those imported commands can be deleted when the real command is deleted.
	 * This procedure removes the imported command reference from the real
	 * command's list, and frees up the memory associated with the imported
	 * command.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Removes the imported command from the real command's import
	 * list.
	 * 
	 * @param data the data object for the imported command
	 */

	static void deleteImportedCmd(ImportedCmdData data)
	{
		WrappedCommand realCmd = data.realCmd;
		WrappedCommand self = data.self;
		ImportRef ref, prev;

		prev = null;
		for (ref = realCmd.importRef; ref != null; ref = ref.next) {
			if (ref.importedCmd == self) {
				// Remove ref from real command's list of imported commands
				// that refer to it.

				if (prev == null) { // ref is first in list
					realCmd.importRef = ref.next;
				} else {
					prev.next = ref.next;
				}
				ref = null;
				data = null;
				return;
			}
			prev = ref;
		}

		throw new TclRuntimeError(
				"DeleteImportedCmd: did not find cmd in real cmd's list of import references");
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TclGetNamespaceForQualName -> getNamespaceForQualName
	 * 
	 * Given a qualified name specifying a command, variable, or namespace, and
	 * a namespace in which to resolve the name, this procedure returns a
	 * pointer to the namespace that contains the item. A qualified name
	 * consists of the "simple" name of an item qualified by the names of an
	 * arbitrary number of containing namespace separated by "::"s. If the
	 * qualified name starts with "::", it is interpreted absolutely from the
	 * global namespace. Otherwise, it is interpreted relative to the namespace
	 * specified by cxtNs if it is non-null. If cxtNs is null, the name is
	 * interpreted relative to the current namespace.
	 * 
	 * A relative name like "foo::bar::x" can be found starting in either the
	 * current namespace or in the global namespace. So each search usually
	 * follows two tracks, and two possible namespaces are returned. If the
	 * procedure sets either gnfqnr.ns or gnfqnr.altNs to null, then that path
	 * failed.
	 * 
	 * If "flags" contains TCL.GLOBAL_ONLY, the relative qualified name is
	 * sought only in the global :: namespace. The alternate search (also)
	 * starting from the global namespace is ignored and gnfqnr.altNs is set
	 * null.
	 * 
	 * If "flags" contains TCL.NAMESPACE_ONLY, the relative qualified name is
	 * sought only in the namespace specified by cxtNs. The alternate search
	 * starting from the global namespace is ignored and gnfqnr.altNs is set
	 * null. If both TCL.GLOBAL_ONLY and TCL.NAMESPACE_ONLY are specified,
	 * TCL.GLOBAL_ONLY is ignored and the search starts from the namespace
	 * specified by cxtNs.
	 * 
	 * If "flags" contains CREATE_NS_IF_UNKNOWN, all namespace components of the
	 * qualified name that cannot be found are automatically created within
	 * their specified parent. This makes sure that functions like
	 * Tcl_CreateCommand always succeed. There is no alternate search path, so
	 * gnfqnr.altNs is set null.
	 * 
	 * If "flags" contains FIND_ONLY_NS, the qualified name is treated as a
	 * reference to a namespace, and the entire qualified name is followed. If
	 * the name is relative, the namespace is looked up only in the current
	 * namespace. A pointer to the namespace is stored in gnfqnr.ns and null is
	 * stored in gnfqnr.simpleName. Otherwise, if FIND_ONLY_NS is not specified,
	 * only the leading components are treated as namespace names, and a pointer
	 * to the simple name of the final component is stored in gnfqnr.simpleName.
	 * 
	 * Results: It sets gnfqnr.ns and gnfqnr.altNs to point to the two possible
	 * namespaces which represent the last (containing) namespace in the
	 * qualified name. If the procedure sets either gnfqnr.ns or gnfqnr.altNs to
	 * null, then the search along that path failed. The procedure also stores a
	 * pointer to the simple name of the final component in gnfqnr.simpleName.
	 * If the qualified name is "::" or was treated as a namespace reference
	 * (FIND_ONLY_NS), the procedure stores a pointer to the namespace in
	 * gnfqnr.ns, null in gnfqnr.altNs, and sets gnfqnr.simpleName to an empty
	 * string.
	 * 
	 * If there is an error, this procedure returns TCL_ERROR. If "flags"
	 * contains TCL_LEAVE_ERR_MSG, an error message is returned in the
	 * interpreter's result object. Otherwise, the interpreter's result object
	 * is left unchanged.
	 * 
	 * gnfqnr.actualCxt is set to the actual context namespace. It is set to the
	 * input context namespace pointer in cxtNs. If cxtNs is null, it is set to
	 * the current namespace context. Note that the
	 * GetNamespaceForQualNameResult result object is defined below.
	 * 
	 * Side effects: If "flags" contains CREATE_NS_IF_UNKNOWN, new namespaces
	 * may be created.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static class GetNamespaceForQualNameResult {
		public Namespace ns;
		public Namespace altNs;
		public Namespace actualCxt;
		public String simpleName;
	}

	public static void getNamespaceForQualName(Interp interp, // Interpreter in
																// which to find
																// the
			// namespace containing qualName.
			String qualName, // A namespace-qualified name of an
			// command, variable, or namespace.
			Namespace cxtNs, // The namespace in which to start the
			// search for qualName's namespace. If null
			// start from the current namespace.
			// Ignored if TCL.GLOBAL_ONLY is set.
			int flags, // Flags controlling the search: an OR'd
			// combination of TCL.GLOBAL_ONLY,
			// TCL.NAMESPACE_ONLY,
			// CREATE_NS_IF_UNKNOWN, and
			// FIND_ONLY_NS.
			GetNamespaceForQualNameResult gnfqnr)

	// gnfqnr.ns // Where this procedure stores a pointer
	// to containing namespace if qualName is
	// found starting from cxtNs or, if
	// TCL_GLOBAL_ONLY is set, if qualName is
	// found in the global :: namespace. null
	// is stored otherwise. This is an array
	// of length 1, value is stored at index 0
	// gnfqnr.altNs // Where this procedure stores a pointer
	// to containing namespace if qualName is
	// found starting from the global ::
	// namespace. null is stored if qualName
	// isn't found starting from :: or if the
	// TCL_GLOBAL_ONLY, TCL_NAMESPACE_ONLY,
	// CREATE_NS_IF_UNKNOWN, FIND_ONLY_NS flag
	// is set. This is an array of length 1.
	// The value is stored at index 0
	// gnfqnr.actualCxt // Address where procedure stores a pointer
	// to the actual namespace from which the
	// search started. This is either cxtNs,
	// the :: namespace if TCL_GLOBAL_ONLY was
	// specified, or the current namespace if
	// cxtNs was null. This is an array of
	// length 1. The value is stored at index 0.
	// gnfqnr.simpleName // Where this procedure stores the
	// simple name at end of the qualName, or
	// null if qualName is "::" or the flag
	// FIND_ONLY_NS was specified. This is an
	// array of length 1, with value at index 0
	{
		gnfqnr.ns = null;
		gnfqnr.altNs = null;
		gnfqnr.actualCxt = null;
		gnfqnr.simpleName = null;

		Namespace ns = cxtNs;
		Namespace altNs;
		Namespace globalNs = getGlobalNamespace(interp);
		Namespace entryNs;
		String start, end;
		String nsName;
		int len;
		int start_ind, end_ind, name_len;

		// Determine the context namespace ns in which to start the primary
		// search. If the qualName name starts with a "::" or TCL_GLOBAL_ONLY
		// was specified, search from the global namespace. Otherwise, use the
		// namespace given in cxtNs, or if that is null, use the current
		// namespace context. Note that we always treat two or more
		// adjacent ":"s as a namespace separator.

		if ((flags & TCL.GLOBAL_ONLY) != 0) {
			ns = globalNs;
		} else if (ns == null) {
			if (interp.varFrame != null) {
				ns = interp.varFrame.ns;
			} else {
				ns = interp.globalNs;
			}
		}

		start_ind = 0;
		name_len = qualName.length();

		if ((name_len >= 2) && (qualName.charAt(0) == ':')
				&& (qualName.charAt(1) == ':')) {
			start_ind = 2; // skip over the initial ::

			while ((start_ind < name_len)
					&& (qualName.charAt(start_ind) == ':')) {
				start_ind++; // skip over a subsequent :
			}

			ns = globalNs;
			if (start_ind >= name_len) { // qualName is just two or more ":"s
				gnfqnr.ns = globalNs;
				gnfqnr.altNs = null;
				gnfqnr.actualCxt = globalNs;
				gnfqnr.simpleName = "";
				return;
			}
		}
		gnfqnr.actualCxt = ns;

		// Start an alternate search path starting with the global namespace.
		// However, if the starting context is the global namespace, or if the
		// flag is set to search only the namespace cxtNs, ignore the
		// alternate search path.

		altNs = globalNs;
		if ((ns == globalNs)
				|| ((flags & (TCL.NAMESPACE_ONLY | FIND_ONLY_NS)) != 0)) {
			altNs = null;
		}

		// Loop to resolve each namespace qualifier in qualName.

		end_ind = start_ind;

		while (start_ind < name_len) {
			// Find the next namespace qualifier (i.e., a name ending in "::")
			// or the end of the qualified name (i.e., a name ending in "\0").
			// Set len to the number of characters, starting from start,
			// in the name; set end to point after the "::"s or at the "\0".

			len = 0;
			for (end_ind = start_ind; end_ind < name_len; end_ind++) {
				if (((name_len - end_ind) > 1)
						&& (qualName.charAt(end_ind) == ':')
						&& (qualName.charAt(end_ind + 1) == ':')) {
					end_ind += 2; // skip over the initial ::
					while ((end_ind < name_len)
							&& (qualName.charAt(end_ind) == ':')) {
						end_ind++; // skip over a subsequent :
					}
					break;
				}
				len++;
			}

			if ((end_ind == name_len)
					&& !((end_ind - start_ind >= 2) && ((qualName
							.charAt(end_ind - 1) == ':') && (qualName
							.charAt(end_ind - 2) == ':')))) {

				// qualName ended with a simple name at start. If FIND_ONLY_NS
				// was specified, look this up as a namespace. Otherwise,
				// start is the name of a cmd or var and we are done.

				if ((flags & FIND_ONLY_NS) != 0) {
					// assign the string from start_ind to the end of the name
					// string
					nsName = qualName.substring(start_ind);
				} else {
					gnfqnr.ns = ns;
					gnfqnr.altNs = altNs;
					gnfqnr.simpleName = qualName.substring(start_ind);
					return;
				}
			} else {
				// start points to the beginning of a namespace qualifier ending
				// in "::". Create new string with the namespace qualifier.

				nsName = qualName.substring(start_ind, start_ind + len);
			}

			// Look up the namespace qualifier nsName in the current namespace
			// context. If it isn't found but CREATE_NS_IF_UNKNOWN is set,
			// create that qualifying namespace. This is needed for procedures
			// like Tcl_CreateCommand that cannot fail.

			if (ns != null) {
				entryNs = (Namespace) ns.childTable.get(nsName);
				if (entryNs != null) {
					ns = entryNs;
				} else if ((flags & CREATE_NS_IF_UNKNOWN) != 0) {
					CallFrame frame = interp.newCallFrame();

					pushCallFrame(interp, frame, ns, false);
					ns = createNamespace(interp, nsName, null);

					popCallFrame(interp);
					if (ns == null) {
						throw new RuntimeException(
								"Could not create namespace " + nsName);
					}
				} else {
					ns = null; // namespace not found and wasn't created
				}
			}

			// Look up the namespace qualifier in the alternate search path too.

			if (altNs != null) {
				altNs = (Namespace) altNs.childTable.get(nsName);
			}

			// If both search paths have failed, return null results.

			if ((ns == null) && (altNs == null)) {
				gnfqnr.ns = null;
				gnfqnr.altNs = null;
				gnfqnr.simpleName = null;
				return;
			}

			start_ind = end_ind;
		}

		// We ignore trailing "::"s in a namespace name, but in a command or
		// variable name, trailing "::"s refer to the cmd or var named {}.

		if (((flags & FIND_ONLY_NS) != 0)
				|| ((end_ind > start_ind) && (qualName.charAt(end_ind - 1) != ':'))) {
			gnfqnr.simpleName = null; // found namespace name
		} else {
			// FIXME : make sure this does not throw exception when end_ind is
			// at the end of the string
			gnfqnr.simpleName = qualName.substring(end_ind); // found cmd/var:
																// points to
																// empty string
		}

		// As a special case, if we are looking for a namespace and qualName
		// is "" and the current active namespace (ns) is not the global
		// namespace, return null (no namespace was found). This is because
		// namespaces can not have empty names except for the global namespace.

		if (((flags & FIND_ONLY_NS) != 0) && (name_len == 0)
				&& (ns != globalNs)) {
			ns = null;
		}

		gnfqnr.ns = ns;
		gnfqnr.altNs = altNs;
		return;
	}


	/**
	 * Searches for a namespace.
	 * 
	 * @param interp
	 *            the interpreter in which to find the namespace
	 * @param name
	 *            Namespace name. If it starts with "::", will be looked up in
	 *            global namespace. Else, looked up first in contextNs (current
	 *            namespace if contextNs is null), then in global namespace.
	 * @param contextNs
	 *            Ignored if TCL.GLOBAL_ONLY flag is set or if the name starts
	 *            with "::". Otherwise, points to namespace in which to resolve
	 *            name; if null, look up name in the current namespace.
	 * @param flags
	 *            Flags controlling namespace lookup: an OR'd combination of
	 *            TCL.GLOBAL_ONLY and TCL.LEAVE_ERR_MSG flags.
	 * @return Returns a reference to the namespace if it is found. Otherwise,
	 *         returns null and leaves an error message in the interpreter's
	 *         result object if "flags" contains TCL.LEAVE_ERR_MSG.
	 */
	public static Namespace findNamespace(Interp interp, 
			String name, 
			Namespace contextNs,
			int flags) 
	{
		Namespace ns;

		// Find the namespace(s) that contain the specified namespace name.
		// Add the FIND_ONLY_NS flag to resolve the name all the way down
		// to its last component, a namespace.

		GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
		getNamespaceForQualName(interp, name, contextNs,
				(flags | FIND_ONLY_NS), gnfqnr);
		ns = gnfqnr.ns;

		if (ns != null) {
			return ns;
		} else if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			/*
			 * interp.resetResult(); TclString.append(interp.getResult(),
			 * "unknown namespace \"" + name + "\"");
			 */

			// FIXME : is there a test case for this error?
            if (name.startsWith("::")) {
                interp.setResult("namespace \"" + name + "\" not found");
            } else {
                Namespace current = getCurrentNamespace(interp);
                interp.setResult("namespace \"" + name + "\" not found in \"" +
                        current.fullName + "\"");
		}
		}
		return null;
	}

	/**
	 * Tcl_FindCommand -> findCommand
	 * 
	 * Searches for a command.
	 * 
	 * @param interp
	 *            the interpreter in which to find the command
	 * @param name
	 *            the commands name; if it starts with "::" it will be looked up
	 *            in the global namespace, otherwise in contextNs, or the
	 *            current namespace if contextNs is null, and then the global
	 *            namespace
	 * @param contextNs
	 *            Ignored if TCL.GLOBAL_ONLY flag set. Otherwise, points to
	 *            namespace in which to resolve name. If null, look up name in
	 *            the current namespace.
	 * @param flags
	 *            An OR'd combination of flags: TCL.GLOBAL_ONLY (look up name
	 *            only in global namespace), TCL.NAMESPACE_ONLY (look up only in
	 *            contextNs, or the current namespace if contextNs is null), and
	 *            TCL.LEAVE_ERR_MSG. If both TCL.GLOBAL_ONLY and
	 *            TCL.NAMESPACE_ONLY are given, TCL.GLOBAL_ONLY is ignored.
	 * 
	 * @return a token for the command if it is found. Otherwise, if it can't be
	 *         found or there is an error, returns null and leaves an error
	 *         message in the interpreter's result object if "flags" contains
	 *         TCL.LEAVE_ERR_MSG.
	 */

	public static WrappedCommand findCommand(Interp interp,
			String name,
			Namespace contextNs,
			int flags //
	) throws TclException {
		Interp.ResolverScheme res;
		Namespace cxtNs;
		Namespace ns0, ns1;
		String simpleName;
		int search;
		WrappedCommand cmd;

		// If this namespace has a command resolver, then give it first
		// crack at the command resolution. If the interpreter has any
		// command resolvers, consult them next. The command resolver
		// procedures may return a Tcl_Command value, they may signal
		// to continue onward, or they may signal an error.

		if ((flags & TCL.GLOBAL_ONLY) != 0) {
			cxtNs = getGlobalNamespace(interp);
		} else if (contextNs != null) {
			cxtNs = contextNs;
		} else {
			cxtNs = getCurrentNamespace(interp);
		}

		if (cxtNs.resolver != null || interp.resolvers != null) {
			try {
				if (cxtNs.resolver != null) {
					cmd = cxtNs.resolver.resolveCmd(interp, name, cxtNs, flags);
				} else {
					cmd = null;
				}

				if (cmd == null && interp.resolvers != null) {
					for (ListIterator iter = interp.resolvers.listIterator(); cmd == null
							&& iter.hasNext();) {
						res = (Interp.ResolverScheme) iter.next();
						cmd = res.resolver.resolveCmd(interp, name, cxtNs,
								flags);
					}
				}

				if (cmd != null) {
					return cmd;
				}
			} catch (TclException e) {
				return null;
			}
		}

		// Find the namespace(s) that contain the command.

		GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
		getNamespaceForQualName(interp, name, contextNs, flags, gnfqnr);
		ns0 = gnfqnr.ns;
		ns1 = gnfqnr.altNs;
		cxtNs = gnfqnr.actualCxt;
		simpleName = gnfqnr.simpleName;

		// Look for the command in the command table of its namespace.
		// Be sure to check both possible search paths: from the specified
		// namespace context and from the global namespace.

		cmd = null;
		for (search = 0; (search < 2) && (cmd == null); search++) {
			Namespace ns;
			if (search == 0) {
				ns = ns0;
			} else if (search == 1) {
				ns = ns1;
			} else {
				throw new TclRuntimeError("bad search value" + search);
			}
			if ((ns != null) && (simpleName != null)) {
				cmd = (WrappedCommand) ns.cmdTable.get(simpleName);
			}
		}
		if (cmd != null) {
			return cmd;
		} else if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			throw new TclException(interp, "unknown command \"" + name + "\"");
		}

		return null;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_FindNamespaceVar -> findNamespaceVar
	 * 
	 * Searches for a namespace variable, a variable not local to a procedure.
	 * The variable can be either a scalar or an array, but may not be an
	 * element of an array.
	 * 
	 * Results: Returns a token for the variable if it is found. Otherwise, if
	 * it can't be found or there is an error, returns null and leaves an error
	 * message in the interpreter's result object if "flags" contains
	 * TCL.LEAVE_ERR_MSG.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static Var findNamespaceVar(Interp interp, // The interpreter in
														// which to find the
			// variable.
			String name, // Variable's name. name. If it starts with "::",
			// will be looked up in global namespace.
			// Else, looked up first in contextNs
			// (current namespace if contextNs is
			// null), then in global namespace.
			Namespace contextNs, // Ignored if TCL.GLOBAL_ONLY flag set.
			// Otherwise, points to namespace in which
			// to resolve name. If null, look up name
			// in the current namespace.
			int flags // An OR'd combination of flags:
	// TCL.GLOBAL_ONLY (look up name only in
	// global namespace), TCL.NAMESPACE_ONLY
	// (look up only in contextNs, or the
	// current namespace if contextNs is
	// null), and TCL.LEAVE_ERR_MSG. If both
	// TCL.GLOBAL_ONLY and TCL.NAMESPACE_ONLY
	// are given, TCL.GLOBAL_ONLY is ignored.
	) throws TclException {
		Interp.ResolverScheme res;
		Namespace cxtNs;
		Namespace ns0, ns1;
		String simpleName;
		int search;
		Var var;

		// If this namespace has a variable resolver, then give it first
		// crack at the variable resolution. It may return a Tcl_Var
		// value, it may signal to continue onward, or it may signal
		// an error.

		if ((flags & TCL.GLOBAL_ONLY) != 0) {
			cxtNs = getGlobalNamespace(interp);
		} else if (contextNs != null) {
			cxtNs = contextNs;
		} else {
			cxtNs = getCurrentNamespace(interp);
		}

		if (cxtNs.resolver != null || interp.resolvers != null) {
			try {
				if (cxtNs.resolver != null) {
					var = cxtNs.resolver.resolveVar(interp, name, cxtNs, flags);
				} else {
					var = null;
				}

				if (var == null && interp.resolvers != null) {
					for (ListIterator iter = interp.resolvers.listIterator(); var == null
							&& iter.hasNext();) {
						res = (Interp.ResolverScheme) iter.next();
						var = res.resolver.resolveVar(interp, name, cxtNs,
								flags);
					}
				}

				if (var != null) {
					return var;
				}
			} catch (TclException e) {
				return null;
			}
		}

		// Find the namespace(s) that contain the variable.

		GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
		getNamespaceForQualName(interp, name, contextNs, flags, gnfqnr);
		ns0 = gnfqnr.ns;
		ns1 = gnfqnr.altNs;
		cxtNs = gnfqnr.actualCxt;
		simpleName = gnfqnr.simpleName;

		// Look for the variable in the variable table of its namespace.
		// Be sure to check both possible search paths: from the specified
		// namespace context and from the global namespace.

		var = null;
		for (search = 0; (search < 2) && (var == null); search++) {
			Namespace ns;
			if (search == 0) {
				ns = ns0;
			} else if (search == 1) {
				ns = ns1;
			} else {
				throw new TclRuntimeError("bad search value" + search);
			}
			if ((ns != null) && (simpleName != null)) {
				var = (Var) ns.varTable.get(simpleName);
			}
		}
		if (var != null) {
			return var;
		} else if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
			/*
			 * interp.resetResult(); TclString.append(interp.getResult(),
			 * "unknown variable \"" + name + "\"");
			 */

			// FIXME : is there a test case for this error?
			interp.setResult("unknown variable \"" + name + "\"");
		}
		return null;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TclResetShadowedCmdRefs -> resetShadowedCmdRefs
	 * 
	 * Called when a command is added to a namespace to check for existing
	 * command references that the new command may invalidate. Consider the
	 * following cases that could happen when you add a command "foo" to a
	 * namespace "b": 1. It could shadow a command named "foo" at the global
	 * scope. If it does, all command references in the namespace "b" are
	 * suspect. 2. Suppose the namespace "b" resides in a namespace "a". Then to
	 * "a" the new command "b::foo" could shadow another command "b::foo" in the
	 * global namespace. If so, then all command references in "a" are suspect.
	 * The same checks are applied to all parent namespaces, until we reach the
	 * global :: namespace.
	 * 
	 * Results: None.
	 * 
	 * Side effects: If the new command shadows an existing command, then the
	 * epoch for each command in each namespace that sees the shadow is
	 * incremented. This invalidates any command caches inside each command in
	 * the namespace. The next time the command is used, cached refrences will
	 * be resolved from scratch.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void resetShadowedCmdRefs(Interp interp, // Interpreter containing
													// the new command
			WrappedCommand newCmd) // Command added to a namespace
	{
		String cmdName;
		Namespace ns, tmpNs;
		Namespace trailNs, shadowNs;
		Namespace globalNs = getGlobalNamespace(interp);
		int i;
		boolean found;
		WrappedCommand wcmd;

		// Array used to hold the trail list.

		Namespace[] trailArray = null;
		int trailFront = -1;
		int trailSize = NUM_TRAIL_ELEMS;

		// Start at the namespace containing the new command, and work up
		// through the list of parents. Stop just before the global namespace,
		// since the global namespace can't "shadow" its own entries.
		// 
		// The namespace "trail" list we build consists of the names of each
		// namespace that encloses the new command, in order from outermost to
		// innermost: for example, "a" then "b". Each iteration of this loop
		// eventually extends the trail upwards by one namespace, ns. We use
		// this trail list to see if ns (e.g. "a" in 2. above) could have
		// now-invalid cached command references. This will happen if ns
		// (e.g. "a") contains a sequence of child namespaces (e.g. "b")
		// such that there is a identically-named sequence of child namespaces
		// starting from :: (e.g. "::b") whose tail namespace contains a command
		// also named cmdName.

		cmdName = newCmd.hashKey;
		for (ns = newCmd.ns; (ns != null) && (ns != globalNs); ns = ns.parent) {
			// Find the maximal sequence of child namespaces contained in ns
			// such that there is a identically-named sequence of child
			// namespaces starting from ::. shadowNs will be the tail of this
			// sequence, or the deepest namespace under :: that might contain a
			// command now shadowed by cmdName. We check below if shadowNs
			// actually contains a command cmdName.

			found = true;
			shadowNs = globalNs;

			for (i = trailFront; i >= 0; i--) {
				trailNs = trailArray[i];
				tmpNs = (Namespace) shadowNs.childTable.get(trailNs.name);
				if (tmpNs != null) {
					shadowNs = tmpNs;
				} else {
					found = false;
					break;
				}
			}

			// If shadowNs contains a command named cmdName, we invalidate
			// all of the command refs cached in ns. As a boundary case,
			// shadowNs is initially :: and we check for case 1. above.

			if (found) {
				wcmd = (WrappedCommand) shadowNs.cmdTable.get(cmdName);
				if (wcmd != null) {
					// Invalidate cached command ref in each command
					// defined in this namespace.

					// nsPtr->cmdRefEpoch++;

					for (Map.Entry<String, WrappedCommand> stringWrappedCommandEntry : ns.cmdTable.entrySet()) {
						Map.Entry entry = (Map.Entry) stringWrappedCommandEntry;
						wcmd = (WrappedCommand) entry.getValue();
						wcmd.incrEpoch();
					}
				}
			}

			// Insert ns at the front of the trail list: i.e., at the end
			// of the trail array.

			if (trailArray == null) {
				trailArray = new Namespace[NUM_TRAIL_ELEMS];
			}

			trailFront++;
			if (trailFront == trailSize) {
				int size = trailSize * 2;
				Namespace[] tmp = new Namespace[size];
				System.arraycopy(trailArray, 0, tmp, 0, trailArray.length);
				trailArray = tmp;
				trailSize = size;
			}
			trailArray[trailFront] = ns;
		}
	}

	/**
	 *----------------------------------------------------------------------
	 * 
	 * Tcl_SetNamespaceResolvers -> setNamespaceResolver
	 * 
	 * Sets the command/variable resolution object for a namespace, thereby
	 * changing the way that command/variable names are interpreted. This allows
	 * extension writers to support different name resolution schemes, such as
	 * those for object-oriented packages.
	 * 
	 * Command resolution is handled by the following method:
	 * 
	 * resolveCmd (Interp interp, String name, Namespace context, int flags)
	 * throws TclException;
	 * 
	 * Whenever a command is executed or Namespace.findCommand is invoked within
	 * the namespace, this method is called to resolve the command name. If this
	 * method is able to resolve the name, it should return the corresponding
	 * WrappedCommand. Otherwise, the procedure can return null, and the command
	 * will be treated under the usual name resolution rules. Or, it can throw a
	 * TclException, and the command will be considered invalid.
	 * 
	 * Variable resolution is handled by the following method:
	 * 
	 * resolveVar (Interp interp, String name, Namespace context, int flags)
	 * throws TclException;
	 * 
	 * If this method is able to resolve the name, it should return the variable
	 * as Var object. The method may also return null, and the variable will be
	 * treated under the usual name resolution rules. Or, it can throw a
	 * TclException, and the variable will be considered invalid.
	 * 
	 * Results: See above.
	 * 
	 * Side effects: None.
	 * 
	 *----------------------------------------------------------------------
	 */

	public static void setNamespaceResolver(Namespace namespace, // Namespace
																	// whose
																	// resolution
																	// rules
			// are being modified.
			Resolver resolver) // command and variable resolution
	{
		// Plug in the new command resolver.

		namespace.resolver = resolver;
	}

	/**
	 *----------------------------------------------------------------------
	 * 
	 * Tcl_GetNamespaceResolvers -> getNamespaceResolver
	 * 
	 * Returns the current command/variable resolution object for a namespace.
	 * By default, these objects are null. New objects can be installed by
	 * calling setNamespaceResolver, to provide new name resolution rules.
	 * 
	 * Results: Returns the esolver object assigned to this namespace. Returns
	 * null otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *----------------------------------------------------------------------
	 */

	static Resolver getNamespaceResolver(Namespace namespace) // Namespace whose
																// resolution
																// rules
	// are being queried.
	{
		return namespace.resolver;
	}

	/**
	 * Return the first Object value contained in the given table. This method
	 * is used only when taking apart a table where entries in the table could
	 * be removed elsewhere. An Iterator is no longer valid once entries have
	 * been removed so it is not possible to take a table apart safely with a
	 * single iterator. This method returns null when there are no more elements
	 * in the table, so it should not be used with a table that contains null
	 * values. This method is not efficient, but it is required when dealing
	 * with a Java Iterator when the table being iterated could have elements
	 * added or deleted.
	 * 
	 * @param table a hash table
	 * @return first element in the hash table
	 */

	public static Object FirstHashEntry(HashMap table) {
		Object retVal;
		Set eset = table.entrySet();
		if (eset.size() == 0) {
			return null;
		}
		Iterator iter = eset.iterator();
		if (!iter.hasNext()) {
			throw new TclRuntimeError("no next() object but set size was "
					+ eset.size());
		}
		Map.Entry entry = (Map.Entry) iter.next();
		retVal = entry.getValue();
		if (retVal == null) {
			throw new TclRuntimeError("entry value should not be null");
		}
		return retVal;
	}

} // end class Namespace

