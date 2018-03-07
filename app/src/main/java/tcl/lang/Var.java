/*
 * Var.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Var.java,v 1.35 2009/07/10 14:05:41 rszulgo Exp $
 *
 */
package tcl.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

/*
 * Implements variables in Tcl. The Var class encapsulates most of the functionality
 * of the methods in generic/tclVar.c and the structure Tcl_Var from the C version.
 */

public class Var {

	/**
	 * Flag bits for variables. The first three (SCALAR, ARRAY, and LINK) are
	 * mutually exclusive and give the "type" of the variable. UNDEFINED is
	 * independent of the variable's type. Note that using an int field here
	 * instead of allocating 9 boolean members makes the resulting object take
	 * up less memory. If there were only 8 boolean fields then the size of the
	 * Var object would be the same.
	 * 
	 * SCALAR - 1 means this is a scalar variable and not an array or link. The
	 * tobj field contains the variable's value. ARRAY - 1 means this is an
	 * array variable rather than a scalar variable or link. The arraymap field
	 * points to the array's hashtable for its elements. LINK - 1 means this Var
	 * structure contains a reference to another Var structure that either has
	 * the real value or is itself another LINK pointer. Variables like this
	 * come about through "upvar" and "global" commands, or through references
	 * to variables in enclosing namespaces. UNDEFINED - 1 means that the
	 * variable is in the process of being deleted. An undefined variable
	 * logically does not exist and survives only while it has a trace, or if it
	 * is a global variable currently being used by some procedure. IN_HASHTABLE
	 * - 1 means this variable is in a hashtable. 0 if a local variable that was
	 * assigned a slot in a procedure frame by the compiler so the Var storage
	 * is part of the call frame. TRACE_EXISTS - 1 means that trace(s) exist on
	 * this scalar or array variable. This flag is set when (var.traces ==
	 * null), it is cleared when there are no more traces. TRACE_ACTIVE - 1
	 * means that trace processing is currently underway for a read or write
	 * access, so new read or write accesses should not cause trace procedures
	 * to be called and the variable can't be deleted. ARRAY_ELEMENT - 1 means
	 * that this variable is an array element, so it is not legal for it to be
	 * an array itself (the ARRAY flag had better not be set). NAMESPACE_VAR - 1
	 * means that this variable was declared as a namespace variable. This flag
	 * ensures it persists until its namespace is destroyed or until the
	 * variable is unset; it will persist even if it has not been initialized
	 * and is marked undefined. The variable's refCount is incremented to
	 * reflect the "reference" from its namespace. NO_CACHE - 1 means that code
	 * should not be able to hold a cached reference to this variable. This flag
	 * is only set for Var objects returned by a namespace or interp resolver.
	 * It is not possible to clear this flag, so the variable can't be cached as
	 * long as it is alive. NON_LOCAL - 1 means that the variable exists in the
	 * compiled local table, but it is not a local or imported local. This flag
	 * is only set in compiled code when scoped global var refs like $::myvar
	 * are found. These variables are not considered part of a variable frame
	 * and can't be found at runtime.
	 */

	static final int SCALAR = 0x1;
	static final int ARRAY = 0x2;
	static final int LINK = 0x4;
	static final int UNDEFINED = 0x8;
	static final int IN_HASHTABLE = 0x10;
	static final int TRACE_ACTIVE = 0x20;
	static final int ARRAY_ELEMENT = 0x40;
	static final int NAMESPACE_VAR = 0x80;
	static final int NO_CACHE = 0x100;
	static final int NON_LOCAL = 0x200;
	static final int TRACE_EXISTS = 0x400;

	// Flag used only with makeUpvar()
	public static final int EXPLICIT_LOCAL_NAME = 0x1000;

	// Methods to read various flag bits of variables.

	public final boolean isVarScalar() {
		return ((flags & SCALAR) != 0);
	}

	public final boolean isVarLink() {
		return ((flags & LINK) != 0);
	}

	public final boolean isVarArray() {
		return ((flags & ARRAY) != 0);
	}

	public final boolean isVarUndefined() {
		return ((flags & UNDEFINED) != 0);
	}

	public final boolean isVarArrayElement() {
		return ((flags & ARRAY_ELEMENT) != 0);
	}

	public final boolean isVarNamespace() {
		return ((flags & NAMESPACE_VAR) != 0);
	}

	public final boolean isVarInHashtable() {
		return ((flags & IN_HASHTABLE) != 0);
	}

	public final boolean isVarTraceExists() {
		return ((flags & TRACE_EXISTS) != 0);
	}

	public final boolean isVarNoCache() {
		return ((flags & NO_CACHE) != 0);
	}

	// True when a compiled local variable should
	// not be a member of the var frame.

	final boolean isVarNonLocal() {
		return ((flags & NON_LOCAL) != 0);
	}

	// Methods to ensure that various flag bits are set properly for variables.

	final void setVarScalar() {
		flags = (flags & ~(ARRAY | LINK)) | SCALAR;
	}

	final void setVarArray() {
		flags = (flags & ~(SCALAR | LINK)) | ARRAY;
	}

	final void setVarLink() {
		flags = (flags & ~(SCALAR | ARRAY)) | LINK;
	}

	final void setVarArrayElement() {
		flags = (flags & ~ARRAY) | ARRAY_ELEMENT;
	}

	final void setVarUndefined() {
		flags |= UNDEFINED;
	}

	public final void setVarNamespace() {
		flags |= NAMESPACE_VAR;
	}

	final void setVarInHashtable() {
		flags |= IN_HASHTABLE;
	}

	final void setVarNonLocal() {
		flags |= NON_LOCAL;
	}

	final void setVarNoCache() {
		flags |= NO_CACHE;
	}

	final void setVarTraceExists() {
		flags |= TRACE_EXISTS;
	}

	final void clearVarUndefined() {
		flags &= ~UNDEFINED;
	}

	public final void clearVarInHashtable() {
		flags &= ~IN_HASHTABLE;
	}

	final void clearVarTraceExists() {
		flags &= ~TRACE_EXISTS;
	}

	/**
	 * tobj is the object stored in the var if it is scalar. 
	 * always use getValue() and setValue()
	 */

	private TclObject tobj;
	
	/**
	 * Key/value pairs in array, if this is an array variable.
	 * Always use getArrayMap() 
	 */
	private Map<String, Var> arraymap;
	
	/**
	 * Reference to a linkto variable associated by this upvar
	 */
	Var linkto;

	/**
	 * List that holds the traces that were placed in this Var
	 */

	public ArrayList traces;

	public ArrayList sidVec;

	/**
	 * Miscellaneous bits of information about variable.
	 * 
	 * @see Var#SCALAR
	 * @see Var#ARRAY
	 * @see Var#LINK
	 * @see Var#UNDEFINED
	 * @see Var#IN_HASHTABLE
	 * @see Var#TRACE_ACTIVE
	 * @see Var#ARRAY_ELEMENT
	 * @see Var#NAMESPACE_VAR
	 */

	int flags;

	/**
	 * If variable is in a hashtable, either the hash table entry that refers to
	 * this variable or null if the variable has been detached from its hash
	 * table (e.g. an array is deleted, but some of its elements are still
	 * referred to in upvars). null if the variable is not in a hashtable. This
	 * is used to delete an variable from its hashtable if it is no longer
	 * needed.
	 */

	public Map table;

	/**
	 * The key under which this variable is stored in the hash table.
	 */

	public String hashKey;

	/**
	 * Counts number of active uses of this variable, not including its entry in
	 * the call frame or the hash table: 1 for each additional variable whose
	 * link points here, 1 for each nested trace active on variable, and 1 if
	 * the variable is a namespace variable. This record can't be deleted until
	 * refCount becomes 0.
	 */

	public int refCount;

	/**
	 * Reference to the namespace that contains this variable. This is set only
	 * for namespace variables. A local variable in a procedure will always have
	 * a null ns field.
	 */

	public Namespace ns;

	/**
	 * NewVar -> Var
	 * 
	 * Construct a variable and initialize its fields.
	 */

	public Var() {
		setValue(null);
		deleteArrayMap();
		linkto = null;
		// name = null; // Like hashKey in Jacl
		ns = null;
		hashKey = null; // Like hPtr in the C implementation
		table = null; // Like hPtr in the C implementation
		refCount = 0;
		traces = null;
		// search = null;
		sidVec = null; // Like search in the C implementation
		flags = (SCALAR | UNDEFINED | IN_HASHTABLE);
	}

	/**
	 * Set the value of this Var (either as a scalar or as an array element)
	 * 
	 * @param tobj
	 */
	public void setValue(TclObject tobj) {
		this.tobj = tobj;
	}
	
	/**
	 * @return the value of this Var (if it is a scalar or array element)
	 */
	public TclObject getValue() {
		return tobj;
	}
	
	/**
	 * @return the Map<String, Var> that contains the values if thiis is an array element
	 */
	public Map<String, Var> getArrayMap() {
		return arraymap;
	}
	
	/**
	 * Create a new array map in this Var
	 */
	public void createArrayMap() {
		this.arraymap = new HashMap<>();
	}
	
	/**
	 * Remove the existing array map in this var
	 */
	public void deleteArrayMap() {
		this.arraymap = null;
	}
	
	/**
	 * Used to create a String that describes this variable.
	 */

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (ns != null) {
			sb.append(ns.fullName);
			if (ns.fullName.equals("::")) {
				sb.append(hashKey);
			} else {
				sb.append("::");
				sb.append(hashKey);
			}
		} else {
			sb.append(hashKey);
		}

		if (isVarScalar()) {
			sb.append(" ");
			sb.append("SCALAR");
		}
		if (isVarLink()) {
			sb.append(" ");
			sb.append("LINK");
		}
		if (isVarArray()) {
			sb.append(" ");
			sb.append("ARRAY");
		}
		if (isVarUndefined()) {
			sb.append(" ");
			sb.append("UNDEFINED");
		}
		if (isVarArrayElement()) {
			sb.append(" ");
			sb.append("ARRAY_ELEMENT");
		}
		if (isVarNamespace()) {
			sb.append(" ");
			sb.append("NAMESPACE_VAR");
		}
		if (isVarInHashtable()) {
			sb.append(" ");
			sb.append("IN_HASHTABLE");
		}
		if (isVarTraceExists()) {
			sb.append(" ");
			sb.append("TRACE_EXISTS");
		}
		if (isVarNoCache()) {
			sb.append(" ");
			sb.append("NO_CACHE");
		}
		return sb.toString();
	}

	/**
	 * Used by ArrayCmd to create a unique searchId string. If the sidVec List
	 * is empty then simply return 1. Else return 1 plus the SearchId.index
	 * value of the last Object in the vector.
	 * 
	 * @return The int value for unique SearchId string.
	 */

	public int getNextIndex() {
		int size = sidVec.size();
		if (size == 0) {
			return 1;
		}
		SearchId sid = (SearchId) sidVec.get(size - 1);
		return (sid.getIndex() + 1);
	}

	/**
	 * Find the SearchId that in the sidVec List that is equal the unique String
	 * s and returns the iterator associated with that SearchId.
	 * 
	 * @param s
	 *            String that ia a unique identifier for a SearchId object
	 * @return Iterator if a match is found else null.
	 */

	public Iterator getSearch(String s) {
		SearchId sid;
		for (Object aSidVec : sidVec) {
			sid = (SearchId) aSidVec;
			if (sid.equals(s)) {
				return sid.getIterator();
			}
		}
		return null;
	}

	/**
	 * Find the SearchId object in the sidVec list and remove it.
	 * 
	 * @param sid
	 *            String that ia a unique identifier for a SearchId object.
	 */

	public boolean removeSearch(String sid) {
		SearchId curSid;

		for (int i = 0; i < sidVec.size(); i++) {
			curSid = (SearchId) sidVec.get(i);
			if (curSid.equals(sid)) {
				sidVec.remove(i);
				return true;
			}
		}
		return false;
	}

	// End of the instance method for the Var class, the rest of the methods
	// are Var related methods ported from the code in generic/tclVar.c

	// The strings below are used to indicate what went wrong when a
	// variable access is denied.

	static final String noSuchVar = "no such variable";
	static final String isArray = "variable is array";
	static final String needArray = "variable isn't array";
	static final String noSuchElement = "no such element in array";
	static final String danglingElement = "upvar refers to element in deleted array";
	static final String danglingVar = "upvar refers to variable in deleted namespace";
	static final String badNamespace = "parent namespace doesn't exist";
	static final String missingName = "missing variable name";

	// Return true if a variable name stored in a String
	// indicates an array element. For example, this
	// method would return true for "foo(bar) and false
	// for "foo".
	// bug 4744 - must be x() at a minimum to look like array

	public static final boolean isArrayVarname(String varName) {
		final int lastInd = varName.length() - 1;
		if (lastInd > 1 && varName.charAt(lastInd) == ')') {
			if (varName.indexOf('(') > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * TclLookupVar -> lookupVar
	 * 
	 * This procedure is used by virtually all of the variable code to locate a
	 * variable given its name(s).
	 * 
	 * @param part1
	 *            if part2 isn't NULL, this is the name of an array. Otherwise,
	 *            this is a full variable name that could include a
	 *            parenthesized array element or a scalar.
	 * @param part2
	 *            Name of an element within array, or null.
	 * @param flags
	 *            Only the TCL.GLOBAL_ONLY bit matters.
	 * @param msg
	 *            Verb to use in error messages, e.g. "read" or "set".
	 * @param createPart2
	 *            OR'ed combination of CRT_PART1 and CRT_PART2. Tells which
	 *            entries to create if they don't already exist.
	 * @return a two element array. a[0] is the variable indicated by part1 and
	 *         part2, or null if the variable couldn't be found and
	 *         throwException is false.
	 *         <p>
	 *         If the variable is found, a[1] is the array that contains the
	 *         variable (or null if the variable is a scalar). If the variable
	 *         can't be found and either createPart1 or createPart2 are true, a
	 *         new as-yet-undefined (VAR_UNDEFINED) variable instance is
	 *         created, entered into a hash table, and returned. Note: it's
	 *         possible that var.value of the returned variable may be null
	 *         (variable undefined), even if createPart1 or createPart2 are true
	 *         (these only cause the hash table entry or array to be created).
	 *         For example, the variable might be a global that has been unset
	 *         but is still referenced by a procedure, or a variable that has
	 *         been unset but it only being kept in existence by a trace.
	 * @exception TclException
	 *                if the variable cannot be found and throwException is
	 *                true.
	 * 
	 */

	public static Var[] lookupVar(Interp interp, String part1, String part2, int flags,String msg, boolean createPart1,boolean createPart2
	) throws TclException {
		CallFrame varFrame = interp.varFrame;
		// Reference to the procedure call frame whose
		// variables are currently in use. Same as
		// the current procedure's frame, if any,
		// unless an "uplevel" is executing.
		HashMap table; // to the hashtable, if any, in which
		// to look up the variable.
		Var var; // Used to search for global names.
		String elName; // Name of array element or null.
		int openParen;
		// If this procedure parses a name into
		// array and index, these point to the
		// parens around the index. Otherwise they
		// are -1. These are needed to restore
		// the parens after parsing the name.
		Namespace varNs, cxtNs;
		Interp.ResolverScheme res;

		var = null;
		varNs = null; // set non-null if a nonlocal variable

		// Parse part1 into array name and index.
		// Always check if part1 is an array element name and allow it only if
		// part2 is not given.
		// (if one does not care about creating array elements that can't be
		// used
		// from tcl, and prefer slightly better performance, one can put
		// the following in an if (part2 == null) { ... } block and remove
		// the part2's test and error reporting or move that code in array set)

		elName = part2;
		openParen = -1;
		int lastInd = part1.length() - 1;
		if ((lastInd > 0) && (part1.charAt(lastInd) == ')')) {
			openParen = part1.indexOf('(');
		}
		if (openParen != -1) {
			if (part2 != null) {
				if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
					throw new TclVarException(interp, part1, part2, msg,
							needArray);
				}
				return null;
			}
			elName = part1.substring(openParen + 1, lastInd);
			part2 = elName; // same as elName, only used in error reporting
			part1 = part1.substring(0, openParen);
		}

		// If this namespace has a variable resolver, then give it first
		// crack at the variable resolution. It may return a Var
		// value, it may signal to continue onward, or it may signal
		// an error.

		if (((flags & TCL.GLOBAL_ONLY) != 0) || (interp.varFrame == null)) {
			cxtNs = interp.globalNs;
		} else {
			cxtNs = interp.varFrame.ns;
		}

		if (cxtNs.resolver != null || interp.resolvers != null) {
			try {
				if (cxtNs.resolver != null) {
					var = cxtNs.resolver
							.resolveVar(interp, part1, cxtNs, flags);
					if (var != null) {
						var.setVarNoCache();
					}
				} else {
					var = null;
				}

				if (var == null && interp.resolvers != null) {
					for (ListIterator iter = interp.resolvers.listIterator(); var == null
							&& iter.hasNext();) {
						res = (Interp.ResolverScheme) iter.next();
						var = res.resolver.resolveVar(interp, part1, cxtNs,
								flags);
						if (var != null) {
							var.setVarNoCache();
						}
					}
				}
			} catch (TclException e) {
				var = null;
			}
		}

		// Look up part1. Look it up as either a namespace variable or as a
		// local variable in a procedure call frame (varFrame).
		// Interpret part1 as a namespace variable if:
		// 1) so requested by a TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY flag,
		// 2) there is no active frame (we're at the global :: scope),
		// 3) the active frame was pushed to define the namespace context
		// for a "namespace eval" or "namespace inscope" command,
		// 4) the name has namespace qualifiers ("::"s).
		// Otherwise, if part1 is a local variable, search first in the
		// frame's array of compiler-allocated local variables, then in its
		// hashtable for runtime-created local variables.
		//
		// If createPart1 and the variable isn't found, create the variable and,
		// if necessary, create varFrame's local var hashtable.

		if (((flags & (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY)) != 0)
				|| (varFrame == null) || !varFrame.isProcCallFrame
				|| (part1.contains("::"))) {
			String tail;

			// Don't pass TCL.LEAVE_ERR_MSG, we may yet create the variable,
			// or otherwise generate our own error!

			var = Namespace.findNamespaceVar(interp, part1, null, flags
					& ~TCL.LEAVE_ERR_MSG);
			if (var == null) {
				if (createPart1) { // var wasn't found so create it

					Namespace.GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
					Namespace.getNamespaceForQualName(interp, part1, null,
							flags, gnfqnr);
					varNs = gnfqnr.ns;
					tail = gnfqnr.simpleName;

					if (varNs == null) {
						if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
							throw new TclVarException(interp, part1, part2,
									msg, badNamespace);
						}
						return null;
					}
					if (tail == null) {
						if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
							throw new TclVarException(interp, part1, part2,
									msg, missingName);
						}
						return null;
					}
					var = new Var();
					varNs.varTable.put(tail, var);

					// There is no hPtr member in Jacl, The hPtr combines the
					// table
					// and the key used in a table lookup.
					var.hashKey = tail;
					var.table = varNs.varTable;

					var.ns = varNs;
				} else { // var wasn't found and not to create it
					if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
						throw new TclVarException(interp, part1, part2, msg,
								noSuchVar);
					}
					return null;
				}
			}
		} else { // local var: look in frame varFrame

			if (varFrame.compiledLocals != null) { // look in compiled local
													// array
				// Compiled local variable lookups would not
				// normally be done in compiled code via
				// lookupVar(). This lookup code would be
				// executed when a runtime get or set
				// operation is executed. A runtime get or
				// set operation could try to create a
				// var with the same name as a compiled local
				// var, so it would need to be created in the
				// compiled local array and not in the local
				// var hash table.

				Var[] compiledLocals = varFrame.compiledLocals;
				String[] compiledLocalsNames = varFrame.compiledLocalsNames;
				final int MAX = compiledLocals.length;

				for (int i = 0; i < MAX; i++) {
					if (compiledLocalsNames[i].equals(part1)) {
						Var clocal = compiledLocals[i];
						if (clocal == null) {
							// No compiled local with this name, init it.
							if (createPart1) {
								var = new Var();
								var.hashKey = part1;
								var.clearVarInHashtable();

								compiledLocals[i] = var;
							}
						} else {
							// Found existing compiled local var, make
							// sure it is a not a scoped non-local.

							if (clocal.isVarNonLocal()) {
								throw new TclRuntimeError(
										"can't lookup scoped variable \""
												+ part1 + "\" in local table");
							}
							var = clocal;
						}
						break;
					}
				}
			}

			if (var == null) { // look in the frame's var hash table
				table = varFrame.varTable;
				if (createPart1) {
					if (table == null) {
						table = new HashMap();
						varFrame.varTable = table;
					}
					var = (Var) table.get(part1);
					if (var == null) { // we are adding a new entry
						var = new Var();
						table.put(part1, var);

						// There is no hPtr member in Jacl, The hPtr combines
						// the table and the key used in a table lookup.
						var.hashKey = part1;
						var.table = table;
					}
				} else {
					if (table != null) {
						var = (Var) table.get(part1);
					}
					if (var == null) {
						if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
							throw new TclVarException(interp, part1, part2,
									msg, noSuchVar);
						}
						return null;
					}
				}
			}
		}

		// If var is a link variable, we have a reference to some variable
		// that was created through an "upvar" or "global" command. Traverse
		// through any links until we find the referenced variable.

		while (var.isVarLink()) {
			var = var.linkto;
		}

		// If we're not dealing with an array element, return var.

		if (elName == null) {
			Var[] ret = interp.lookupVarResult;
			ret[0] = var;
			ret[1] = null;
			return ret;
		}

		return Var.lookupArrayElement(interp, part1, elName, flags, msg,
				createPart1, createPart2, var);
	}

	/*
	 * Given a ref to an array Var object, lookup the element in the array
	 * indicated by "part2".
	 */

	static Var[] lookupArrayElement(Interp interp, // Interpreter to use for
													// lookup.
			String part1, // The name of an array, can't include elem.
			String part2, // Name of element within array, can't be null.
			int flags, // Only TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
			// and TCL.LEAVE_ERR_MSG bits matter.
			String msg, // Verb to use in error messages, e.g.
			// "read" or "set". Only needed if
			// TCL.LEAVE_ERR_MSG is set in flags.
			boolean createPart1, // If true, create hash table entry for part 1
			// of name, if it doesn't already exist. If
			// false, return error if it doesn't exist.
			boolean createPart2, // If true, create hash table entry for part 2
			// of name, if it doesn't already exist. If
			// false, throw exception if it doesn't exist.
			Var var // Resolved ref to the array variable.
	) throws TclException {
		// We're dealing with an array element. Make sure the variable is an
		// array and look up the element (create the element if desired).

		if (var.isVarUndefined() && !var.isVarArrayElement()) {
			if (!createPart1) {
				if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
					throw new TclVarException(interp, part1, part2, msg,
							noSuchVar);
				}
				return null;
			}

			// Make sure we are not resurrecting a namespace variable from a
			// deleted namespace!

			if (((var.flags & IN_HASHTABLE) != 0) && (var.table == null)) {
				if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
					throw new TclVarException(interp, part1, part2, msg,
							danglingVar);
				}
				return null;
			}

			var.setVarArray();
			var.clearVarUndefined();
			var.createArrayMap();
		} else if (!var.isVarArray()) {
			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
				throw new TclVarException(interp, part1, part2, msg, needArray);
			}
			return null;
		}

		Var arrayVar = var;
		Map<String, Var> arrayTable = var.getArrayMap();
		if (createPart2) {
			Var searchvar = (Var) arrayTable.get(part2);

			if (searchvar == null) { // new entry
				if (var.sidVec != null) {
					deleteSearches(var);
				}

				var = new Var();
				arrayTable.put(part2, var);

				// There is no hPtr member in Jacl, The hPtr combines the table
				// and the key used in a table lookup.
				var.hashKey = part2;
				var.table = arrayTable;

				var.ns = arrayVar.ns; // Will be null for local vars
				var.setVarArrayElement();
			} else {
				var = searchvar;
			}
		} else {
			var = (Var) arrayTable.get(part2);
			if (var == null) {
				if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
					throw new TclVarException(interp, part1, part2, msg,
							noSuchElement);
				}
				return null;
			}
		}

		Var[] ret = interp.lookupVarResult;
		ret[0] = var; // The Var in the array
		ret[1] = arrayVar; // The array Var
		return ret;
	}

	/**
	 * Tcl_GetVar2Ex -> getVar
	 * 
	 * Query the value of a variable, given a two-part name consisting of array
	 * name and element within array.
	 * 
	 * @param interp
	 *            the interp that holds the variable
	 * @param part1
	 *            1st part of the variable name.
	 * @param part2
	 *            2nd part of the variable name.
	 * @param flags
	 *            misc flags that control the actions of this method.
	 * @return the value of the variable.
	 */

	public static TclObject getVar(Interp interp, // interpreter to look for the
													// var in
			String part1, // Name of an array (if part2 is non-null)
			// or the name of a variable.
			String part2, // If non-null, gives the name of an element
			// in the array part1.
			int flags // OR-ed combination of TCL.GLOBAL_ONLY,
	// and TCL.LEAVE_ERR_MSG bits.
	) throws TclException {
		Var[] result = lookupVar(interp, part1, part2, flags, "read", false,
				true);

		if (result == null) {
			// lookupVar() returns null only if TCL.LEAVE_ERR_MSG is
			// not part of the flags argument, return null in this case.

			return null;
		}

		return getVarPtr(interp, result[0], result[1], part1, part2, flags);
	}

	/**
	 * TclPtrGetVar -> getVarPtr
	 * 
	 * Query the value of a variable, given refs to the variables Var objects.
	 * 
	 * @param interp
	 *            the interp that holds the variable
	 * @param part1
	 *            1st part of the variable name.
	 * @param part2
	 *            2nd part of the variable name.
	 * @param flags
	 *            misc flags that control the actions of this method.
	 * @return the value of the variable.
	 */

	static TclObject getVarPtr(Interp interp, // interpreter to look for the var
												// in
			Var var, Var array, String part1, // Name of an array (if part2 is
												// non-null)
			// or the name of a variable.
			String part2, // If non-null, gives the name of an element
			// in the array part1.
			int flags // OR-ed combination of TCL.GLOBAL_ONLY,
	// and TCL.LEAVE_ERR_MSG bits.
	) throws TclException {
		try {
			// Invoke any traces that have been set for the variable.

			if ((var.traces != null)
					|| ((array != null) && (array.traces != null))) {
				String msg = callTraces(interp, array, var, part1, part2,
						(flags & (TCL.NAMESPACE_ONLY | TCL.GLOBAL_ONLY))
								| TCL.TRACE_READS);
				if (msg != null) {
					if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
						throw new TclVarException(interp, part1, part2, "read",
								msg);
					}
					return null;
				}
			}

			if (var.isVarScalar() && !var.isVarUndefined()) {
				return var.getValue();
			}

			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
				String msg;
				if (var.isVarUndefined() && (array != null)
						&& !array.isVarUndefined()) {
					msg = noSuchElement;
				} else if (var.isVarArray()) {
					msg = isArray;
				} else {
					msg = noSuchVar;
				}
				throw new TclVarException(interp, part1, part2, "read", msg);
			}
		} finally {
			// If the variable doesn't exist anymore and no-one's using it,
			// then free up the relevant structures and hash table entries.

			if (var.isVarUndefined()) {
				cleanupVar(var, array);
			}
		}

		return null;
	}

	/**
	 * Tcl_SetVar2Ex -> setVar
	 * 
	 * Given a two-part variable name, which may refer either to a scalar
	 * variable or an element of an array, change the value of the variable to a
	 * new Tcl object value. See the setVarPtr() method for the arguments to be
	 * passed to this method.
	 */
	public static TclObject setVar(Interp interp, String part1,String part2, TclObject newValue, int flags) throws TclException {
		Var[] result = lookupVar(interp, part1, part2, flags, "set", true, true);
		if (result == null) {
			return null;
		}

		return setVarPtr(interp, result[0], result[1], part1, part2, newValue,
				flags);
	}

	/**
	 * TclPtrSetVar -> setVarPtr
	 * 
	 * This method implements setting of a variable value that has already been
	 * resolved into Var refrences. Pass the resolved var refrences and a
	 * two-part variable name, which may refer either to a scalar or an element
	 * of an array. This method will change the value of the variable to a new
	 * TclObject value. If the named scalar or array or element doesn't exist
	 * then this method will create one.
	 * 
	 * @param interp
	 *            the interp that holds the variable
	 * @param var
	 *            a resolved Var ref
	 * @param array
	 *            a resolved Var ref
	 * @param part1
	 *            1st part of the variable name.
	 * @param part2
	 *            2nd part of the variable name.
	 * @param newValue
	 *            the new value for the variable
	 * @param flags
	 *            misc flags that control the actions of this method
	 * 
	 *            Returns a pointer to the TclObject holding the new value of
	 *            the variable. If the write operation was disallowed because an
	 *            array was expected but not found (or vice versa), then null is
	 *            returned; if the TCL.LEAVE_ERR_MSG flag is set, then an
	 *            exception will be raised. Note that the returned object may
	 *            not be the same one referenced by newValue because variable
	 *            traces may modify the variable's value. The value of the given
	 *            variable is set. If either the array or the entry didn't exist
	 *            then a new variable is created.
	 * 
	 *            The reference count is decremented for any old value of the
	 *            variable and incremented for its new value. If the new value
	 *            for the variable is not the same one referenced by newValue
	 *            (perhaps as a result of a variable trace), then newValue's ref
	 *            count is left unchanged by Tcl_SetVar2Ex. newValue's ref count
	 *            is also left unchanged if we are appending it as a string
	 *            value: that is, if "flags" includes TCL.APPEND_VALUE but not
	 *            TCL.LIST_ELEMENT.
	 * 
	 *            The reference count for the returned object is _not_
	 *            incremented: if you want to keep a reference to the object you
	 *            must increment its ref count yourself.
	 */

	static TclObject setVarPtr(Interp interp, // interp to search for the var in
			Var var, Var array, String part1, // Name of an array (if part2 is
												// non-null)
			// or the name of a variable.
			String part2, // If non-null, gives the name of an element
			// in the array part1.
			TclObject newValue, // New value for variable.
			int flags // Various flags that tell how to set value:
	// any of TCL.GLOBAL_ONLY,
	// TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE,
	// TCL.LIST_ELEMENT or TCL.LEAVE_ERR_MSG.
	) throws TclException {
		TclObject oldValue;
		String bytes;

		// If the variable is in a hashtable and its table field is null, then
		// we
		// may have an upvar to an array element where the array was deleted
		// or an upvar to a namespace variable whose namespace was deleted.
		// Generate an error (allowing the variable to be reset would screw up
		// our storage allocation and is meaningless anyway).

		if (((var.flags & IN_HASHTABLE) != 0) && (var.table == null)) {
			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
				if (var.isVarArrayElement()) {
					throw new TclVarException(interp, part1, part2, "set",
							danglingElement);
				} else {
					throw new TclVarException(interp, part1, part2, "set",
							danglingVar);
				}
			}
			return null;
		}

		// It's an error to try to set an array variable itself.

		if (var.isVarArray() && !var.isVarUndefined()) {
			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
				throw new TclVarException(interp, part1, part2, "set", isArray);
			}
			return null;
		}

		// At this point, if we were appending, we used to call read traces: we
		// treated append as a read-modify-write. However, it seemed unlikely to
		// us that a real program would be interested in such reads being done
		// during a set operation.

		// Set the variable's new value. If appending, append the new value to
		// the variable, either as a list element or as a string. Also, if
		// appending, then if the variable's old value is unshared we can modify
		// it directly, otherwise we must create a new copy to modify: this is
		// "copy on write".

		try {
			oldValue = var.getValue();

			if ((flags & TCL.APPEND_VALUE) != 0) {
				if (var.isVarUndefined() && (oldValue != null)) {
					oldValue.release(); // discard old value
					var.setValue(null);
					oldValue = null;
				}
				if ((flags & TCL.LIST_ELEMENT) != 0) { // append list element
					if (oldValue == null) {
						oldValue = TclList.newInstance();
						var.setValue(oldValue);
						oldValue.preserve(); // since var is referenced
					} else if (oldValue.isShared()) { // append to copy
						var.setValue(oldValue.duplicate());
						oldValue.release();
						oldValue = var.getValue();
						oldValue.preserve(); // since var is referenced
					}
					TclList.append(interp, oldValue, newValue);
				} else { // append string
					// We append newValue's bytes but don't change its ref
					// count.

					bytes = newValue.toString();
					if (oldValue == null) {
						TclObject tobj = TclString.newInstance(bytes);
						var.setValue(tobj);
						tobj.preserve();
					} else {
						if (oldValue.isShared()) { // append to copy
							var.setValue(oldValue.duplicate());
							oldValue.release();
							oldValue = var.getValue();
							oldValue.preserve(); // since var is referenced
						}
						TclString.append(oldValue, bytes);
					}
				}
			} else {
				if ((flags & TCL.LIST_ELEMENT) != 0) { // set var to list
														// element
					int listFlags;

					// We set the variable to the result of converting
					// newValue's
					// string rep to a list element. We do not change newValue's
					// ref count.

					if (oldValue != null) {
						oldValue.release(); // discard old value
					}
					bytes = newValue.toString();
					listFlags = Util.scanElement(interp, bytes);
					StringBuffer sb = new StringBuffer(64);
					Util.convertElement(bytes, listFlags, sb);
					oldValue = TclString.newInstance(sb.toString());
					var.setValue(oldValue);
					var.getValue().preserve();
				} else if (newValue != oldValue) {
					var.setValue(newValue);
					newValue.preserve(); // var is another ref
					if (oldValue != null) {
						oldValue.release(); // discard old value
					}
				}
			}
			var.setVarScalar();
			var.clearVarUndefined();
			if (array != null) {
				array.clearVarUndefined();
			}

			// Invoke any write traces for the variable.

			if ((var.traces != null)
					|| ((array != null) && (array.traces != null))) {

				String msg = callTraces(interp, array, var, part1, part2,
						(flags & (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY))
								| TCL.TRACE_WRITES);
				if (msg != null) {
					if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
						throw new TclVarException(interp, part1, part2, "set",
								msg);
					}
					return null; // Same as "goto cleanup" in C verison
				}
			}

			// Return the variable's value unless the variable was changed in
			// some
			// gross way by a trace (e.g. it was unset and then recreated as an
			// array).

			if (var.isVarScalar() && !var.isVarUndefined()) {
				return var.getValue();
			}

			// A trace changed the value in some gross way. Return an empty
			// string
			// object.

			return TclString.newInstance("");
		} finally {
			// If the variable doesn't exist anymore and no-one's using it,
			// then free up the relevant structures and hash table entries.

			if (var.isVarUndefined()) {
				cleanupVar(var, array);
			}
		}
	}

	// This method is invoked to initialize a new compiled
	// local scalar variable when the compiled local slot
	// is null. Initializing a compiled local scalar is
	// a very common operation, so it is highly optimized here.

	public static TclObject initVarCompiledLocalScalar(final Interp interp, // interp
																			// to
																			// search
																			// for
																			// the
																			// var
																			// in
			final String varname, // Name of scalar variable.
			final TclObject newValue, // New value for scalar variable.
			final Var[] compiledLocals, // compiled local array
			final int localIndex) // index into compiled local array, 0 to N.
			throws TclException {
		// Extra checking
		final boolean validate = false;

		if (validate) {
			CallFrame varFrame = interp.varFrame;

			if (varFrame == null) {
				throw new TclRuntimeError("null interp.varFrame");
			}
			if (varFrame != interp.frame) {
				throw new TclRuntimeError(
						"interp.frame vs interp.varFrame mismatch");
			}
			if (varFrame.isProcCallFrame == false) {
				throw new TclRuntimeError("expected isProcCallFrame to be true");
			}
			if (varFrame.compiledLocals == null) {
				throw new TclRuntimeError("expected non-null compiledLocals");
			}

			// Double check that scalar name is not actually an array
			// name like "arr(foo)".

			if (Var.isArrayVarname(varname)) {
				throw new TclRuntimeError("unexpected array variable name \""
						+ varname + "\"");
			}

			// Look in local table, there should not be an entry
			HashMap table = varFrame.varTable;

			if (table != null && table.size() > 0) {
				Var var = (Var) table.get(varname);
				if (var != null) {
					throw new TclException(interp,
							"duplicate var found in local table for " + varname);
				}
			}

			if (compiledLocals[localIndex] != null) {
				throw new TclException(interp,
						"compiled local slot should be null for " + varname);
			}

			// A compiled local that is a scoped value would never
			// be initialized by this method.

			if (varname.contains("::")) {
				throw new TclRuntimeError(
						"scoped scalar should neve be initialized here "
								+ varname);
			}

		} // end if (validate) block

		// At this point, we know that a var with the
		// same name can't exist in the local table.
		// We also know that the compiled local slot
		// is null, so the var can't exist in an
		// undefined state. There are no funky state
		// issues like a var with traces set or
		// a var in another frame linked to this one.
		// The varname will always be a simple scalar.

		Var var = new Var();
		if (validate) {
			// Double check Var init state assumptions.
			if (var.flags != (SCALAR | UNDEFINED | IN_HASHTABLE)) {
				throw new TclRuntimeError("invalid Var flags state");
			}
			if (var.getValue() != null) {
				throw new TclRuntimeError("expected null Var tobj value");
			}
			if (var.getArrayMap() != null) {
				throw new TclRuntimeError("expected null Var arraymap value");
			}
			if (var.linkto != null) {
				throw new TclRuntimeError("expected null Var linkto value");
			}
			if (var.table != null) {
				throw new TclRuntimeError("expected null Var table");
			}
		}

		// Inline Var init and setVarPtr() logic that applies to
		// scalar variables.

		// var.setVarScalar();
		// var.clearVarInHashtable();
		// var.clearVarUndefined();
		var.flags = SCALAR;

		var.hashKey = varname;

		// Assign TclObject value for scalar and incr ref count

		var.setValue(newValue);
		newValue.preserve();

		// Add var to the compiled local array.

		compiledLocals[localIndex] = var;

		return newValue;
	}

	// This method is invoked to set a compiled local scalar
	// variable when the resolved var is invalid. It will
	// never be invoked when the compiled local slot is null.
	// This method is not in the critical execution path.

	public static TclObject setVarCompiledLocalScalarInvalid(Interp interp, // interp
																			// to
																			// search
																			// for
																			// the
																			// var
																			// in
			String varname, // Name of scalar variable.
			TclObject newValue) // New value for scalar variable.
			throws TclException {
		// Extra checking
		final boolean validate = false;

		if (validate) {
			// Lookup current variable frame on the stack. This method
			// is only even invoked after a CallFrame with a compiled
			// local array has already been pushed onto the stack.

			CallFrame varFrame = interp.varFrame;

			if (varFrame == null) {
				throw new TclRuntimeError("null interp.varFrame");
			}
			if (varFrame != interp.frame) {
				throw new TclRuntimeError(
						"interp.frame vs interp.varFrame mismatch");
			}
			if (varFrame.isProcCallFrame == false) {
				throw new TclRuntimeError("expected isProcCallFrame to be true");
			}

			// Double check that scalar name is not actually an array
			// name like "arr(foo)".

			if (Var.isArrayVarname(varname)) {
				throw new TclRuntimeError("unexpected array variable name \""
						+ varname + "\"");
			}

			// A scoped var name should always be initialized
			// as a link var. A non-global scoped link var
			// should never be pass in here.

			if (!varname.startsWith("::") && (varname.contains("::"))) {
				throw new TclRuntimeError("unexpected scoped scalar");
			}

		} // end if (validate) block

		// This method would never be invoked with a null
		// compiled locals slot. It could be invoked when
		// the compiled local is unset, has traces,
		// or when the link var is unset or has traces.

		return setVar(interp, varname, null, newValue, TCL.LEAVE_ERR_MSG);
	}

	// This method is invoked to get the value of a
	// scalar variable that does not have a valid
	// resolved ref. This method could be invoked
	// when traces are set on a compiled local,
	// for example. This method is only
	// ever invoked from a compiled proc
	// implementation. This method is not in the
	// critical execution path.

	public static TclObject getVarCompiledLocalScalarInvalid(Interp interp, // interp
																			// to
																			// search
																			// for
																			// the
																			// var
																			// in
			String varname) // Name of scalar variable.
			throws TclException {
		// Extra checking
		final boolean validate = false;

		if (validate) {
			CallFrame varFrame = interp.varFrame;

			if (varFrame == null) {
				throw new TclRuntimeError("null interp.varFrame");
			}
			if (varFrame != interp.frame) {
				throw new TclRuntimeError(
						"interp.frame vs interp.varFrame mismatch");
			}

			if (varFrame.isProcCallFrame == false) {
				throw new TclRuntimeError("expected isProcCallFrame to be true");
			}
			if (varFrame.compiledLocals == null) {
				throw new TclRuntimeError("expected non-null compiledLocals");
			}

			if (varname == null) {
				throw new TclRuntimeError("varname can't be null");
			}

			// Double check that varname is not actually an array
			// name like "arr(foo)".

			if ((varname.charAt(varname.length() - 1) == ')')
					&& (varname.indexOf('(') != -1)) {
				throw new TclRuntimeError("unexpected array variable name \""
						+ varname + "\"");
			}

			// A non-global scoped link var should never be passed in here.

			if (!varname.startsWith("::") && (varname.contains("::"))) {
				throw new TclRuntimeError("unexpected scoped scalar");
			}
		}

		return Var.getVar(interp, varname, null, TCL.LEAVE_ERR_MSG);
	}

	// This method is invoked to initialize a new compiled
	// local array variable when the compiled local slot
	// is null. Initializing a compiled local array is
	// a very common operation, so it is optimized.

	public static TclObject initVarCompiledLocalArray(final Interp interp, // interp
																			// to
																			// search
																			// for
																			// the
																			// var
																			// in
			final String varname, // Name of scalar variable.
			final String key, // Array key, can't be null
			final TclObject newValue, // New value for array entry variable.
			final Var[] compiledLocals, // compiled local array
			final int localIndex) // index into compiled local array, 0 to N.
			throws TclException {
		// Extra checking
		final boolean validate = false;

		if (validate) {
			// Lookup current variable frame on the stack. This method
			// is only even invoked after a CallFrame with a compiled
			// local array has already been pushed onto the stack.

			CallFrame varFrame = interp.varFrame;

			if (varFrame == null) {
				throw new TclRuntimeError("null interp.varFrame");
			}
			if (varFrame != interp.frame) {
				throw new TclRuntimeError(
						"interp.frame vs interp.varFrame mismatch");
			}
			if (varFrame.isProcCallFrame == false) {
				throw new TclRuntimeError("expected isProcCallFrame to be true");
			}
			if (varFrame.compiledLocals == null) {
				throw new TclRuntimeError("expected non-null compiledLocals");
			}

			// Double check that varname is not actually an array
			// name like "arr(foo)". A compiled array name should be
			// seperated into two elements.

			if (Var.isArrayVarname(varname)) {
				throw new TclRuntimeError("unexpected array variable name \""
						+ varname + "\"");
			}

			if (key == null) {
				throw new TclRuntimeError("null array key");
			}

			// Look in local table, there should not be an entry for this
			// varname
			HashMap table = varFrame.varTable;

			if (table != null && table.size() > 0) {
				Var var = (Var) table.get(varname);
				if (var != null) {
					throw new TclException(interp,
							"duplicate var found in local table for " + varname);
				}
			}

			if (compiledLocals[localIndex] != null) {
				throw new TclException(interp,
						"compiled local slot should be null for " + varname);
			}

			// A compiled local that is a scoped value would never
			// be initialized by this method.

			if (varname.contains("::")) {
				throw new TclRuntimeError(
						"scoped scalar should neve be initialized here "
								+ varname);
			}

		} // end if (validate)

		// At this point, we know that a var with the
		// same name can't exist in the local table.
		// We also know that the compiled local slot
		// is null, so the var can't exist in an
		// undefined state. There are no funky state
		// issues like a var with traces set or
		// a var in another frame linked to this one.
		// The varname will always be an unlinked array var.

		Var var = new Var();
		var.clearVarInHashtable();
		var.hashKey = varname;

		// Add var to the compiled local array
		// so that it will be found and used
		// by setVar().

		compiledLocals[localIndex] = var;

		// Set the variable, can't use setVarPtr()
		// since lookupVar() does the array element
		// create logic.

		return setVar(interp, varname, key, newValue, TCL.LEAVE_ERR_MSG);
	}

	// This method is invoked to set a compiled local array
	// variable when the resolved var is invalid. It will
	// never be invoked when the compiled local slot is null.
	// This method is not in the critical execution path.

	public static TclObject setVarCompiledLocalArrayInvalid(Interp interp, // interp
																			// to
																			// search
																			// for
																			// the
																			// var
																			// in
			String varname, // Name of scalar variable.
			String key, // Array key, can't be null
			TclObject newValue) // New value for scalar variable.
			throws TclException {
		// Extra checking
		final boolean validate = false;

		if (validate) {
			// Lookup current variable frame on the stack. This method
			// is only even invoked after a CallFrame with a compiled
			// local array has already been pushed onto the stack.

			CallFrame varFrame = interp.varFrame;

			if (varFrame == null) {
				throw new TclRuntimeError("null interp.varFrame");
			}
			if (varFrame != interp.frame) {
				throw new TclRuntimeError(
						"interp.frame vs interp.varFrame mismatch");
			}
			if (varFrame.isProcCallFrame == false) {
				throw new TclRuntimeError("expected isProcCallFrame to be true");
			}
			if (varFrame.compiledLocals == null) {
				throw new TclRuntimeError("expected non-null compiledLocals");
			}

			// Double check that varname is not actually an array
			// name like "arr(foo)".

			if (Var.isArrayVarname(varname)) {
				throw new TclRuntimeError("unexpected array variable name \""
						+ varname + "\"");
			}

			if (key == null) {
				throw new TclRuntimeError("null array key");
			}

			// Look in local table, there should not be an entry for this
			// varname
			HashMap table = varFrame.varTable;

			if (table != null && table.size() > 0) {
				Var var = (Var) table.get(varname);
				if (var != null) {
					throw new TclException(interp,
							"duplicate var found in local table for " + varname);
				}
			}

			// A compiled local that is a scoped value would never
			// be initialized by this method.

			if (varname.contains("::")) {
				throw new TclRuntimeError(
						"scoped scalar should neve be initialized here "
								+ varname);
			}

		} // end if (validate)

		// The compiled local array var already exists, but it
		// could not be resolved to a valid var. This can happen
		// when an undefined variable linked into another scope is
		// being defined for the first time. This can also
		// happen when the compiled local is not an array.
		// Handle these cases by invoking setVar() to either
		// raise the proper error or set the var.

		return setVar(interp, varname, key, newValue, TCL.LEAVE_ERR_MSG);
	}

	// This method is invoked to get the value of an
	// array variable that is associated with a compiled
	// local slot. This method is invoked when the
	// compiled local slot is null or when the var
	// can't be resolved to a valid array variable.
	// This method will only attempt to read a variable.
	// It will not init a compiled local slot.
	// This method is only ever invoked from a compiled
	// proc implementation. This method is not in the
	// critical execution path.

	public static TclObject getVarCompiledLocalArrayInvalid(Interp interp, // interp
																			// to
																			// search
																			// for
																			// the
																			// var
																			// in
			String varname, // Name of array variable.
			String key) // array key, can't be null
			throws TclException {
		// Extra checking
		final boolean validate = false;

		if (validate) {
			CallFrame varFrame = interp.varFrame;

			if (varFrame == null) {
				throw new TclRuntimeError("null interp.varFrame");
			}
			if (varFrame != interp.frame) {
				throw new TclRuntimeError(
						"interp.frame vs interp.varFrame mismatch");
			}
			if (varFrame.compiledLocals == null) {
				throw new TclRuntimeError("expected non-null compiledLocals");
			}

			if (key == null) {
				throw new TclRuntimeError("array key can't be null");
			}

			// Double check that varname is not actually an array
			// name like "arr(foo)".

			if ((varname.charAt(varname.length() - 1) == ')')
					&& (varname.indexOf('(') != -1)) {
				throw new TclRuntimeError("unexpected array variable name \""
						+ varname + "\"");
			}

		} // end if (validate) block

		return Var.getVar(interp, varname, key, TCL.LEAVE_ERR_MSG);
	}

	// This method is invoked to get the value of an
	// array variable that is associated with a compiled
	// local slot. This method is invoked when the
	// compiled local slot contains a valid resolved ref.
	// This method is only ever invoked from a compiled
	// proc implementation. This method is in the
	// critical execution path.

	public static TclObject getVarCompiledLocalArray(final Interp interp,
			final String varname, // name of array variable
			final String key, // array key, can't be null
			final Var resolved, // resolved array variable
			final boolean leaveErrMsg) // If true, will raise a
			// TclException when the array
			// element does not exist. If
			// false, then return null.
			throws TclException {
		int flags = 0;
		if (leaveErrMsg) {
			// If leaveErrMsg is true, will raise a TclException
			// in lookupArrayElement() if the array element does
			// not exist or is an undefined var. If leaveErrMsg is
			// false, then return null so that the calling code
			// can handle an array variable that does not exist
			// without throwing and catching an exception.

			flags = TCL.LEAVE_ERR_MSG;
		}

		Var[] result = Var.lookupArrayElement(interp, varname, key, flags,
				"read", false, false, resolved);
		if (result == null) {
			return null;
		}

		// Always pass TCL.LEAVE_ERR_MSG so that an exception
		// will be raised in case a trace raises an exception.

		return Var.getVarPtr(interp, result[0], result[1], varname, key,
				TCL.LEAVE_ERR_MSG);
	}

	// This method is invoked to set the value of an
	// array variable that is associated with a compiled
	// local slot. This method is invoked when the
	// compiled local slot can be resolved to an array.
	// This method is only ever invoked from a compiled
	// proc implementation. This method is in the
	// critical execution path.

	public static TclObject setVarCompiledLocalArray(final Interp interp,
			final String varname, // name of array variable
			final String key, // array key, can't be null
			final TclObject newValue, final Var resolved) // resolved array
															// variable
			throws TclException {
		// Raise TclException instead of returning null
		final int flags = TCL.LEAVE_ERR_MSG;

		Var[] result = lookupArrayElement(interp, varname, key, flags, "set",
				false, true, resolved);

		return setVarPtr(interp, result[0], result[1], varname, key, newValue,
				flags);
	}

	/**
	 * TclIncrVar2 -> incrVar
	 * 
	 * Given a two-part variable name, which may refer either to a scalar
	 * variable or an element of an array, increment the Tcl object value of the
	 * variable by a specified amount.
	 * 
	 * @param part1
	 *            1st part of the variable name.
	 * @param part2
	 *            2nd part of the variable name.
	 * @param incrAmount
	 *            Amount to be added to variable.
	 * @param flags
	 *            misc flags that control the actions of this method
	 * 
	 *            Results: Returns a reference to the TclObject holding the new
	 *            value of the variable. If the specified variable doesn't
	 *            exist, or there is a clash in array usage, or an error occurs
	 *            while executing variable traces, then a TclException will be
	 *            raised.
	 * 
	 *            Side effects: The value of the given variable is incremented
	 *            by the specified amount. If either the array or the entry
	 *            didn't exist then a new variable is created. The ref count for
	 *            the returned object is _not_ incremented to reflect the
	 *            returned reference; if you want to keep a reference to the
	 *            object you must increment its ref count yourself.
	 * 
	 *           
	 *            ----------------------------------------------------------------
	 *            ------
	 */

	public static TclObject incrVar(Interp interp, // Command interpreter in
													// which variable is
			// to be found.
			String part1, // Reference to a string holding the name of
			// an array (if part2 is non-null) or the
			// name of a variable.
			String part2, // If non-null, reference to a string holding
			// the name of an element in the array
			// part1.
			long incrAmount, // Amount to be added to variable.
			int flags // Various flags that tell how to incr value:
	// any of TCL.GLOBAL_ONLY,
	// TCL.NAMESPACE_ONLY, TCL.APPEND_VALUE,
	// TCL.LIST_ELEMENT, TCL.LEAVE_ERR_MSG.
	) throws TclException {
		TclObject varValue = null;
		boolean createdNewObj; // Set to true if var's value object is shared
		// so we must increment a copy (i.e. copy
		// on write).
		int i;
		boolean err;

		// There are two possible error conditions that depend on the setting of
		// TCL.LEAVE_ERR_MSG. an exception could be raised or null could be
		// returned
		err = false;
		try {
			varValue = getVar(interp, part1, part2, flags);
		} catch (TclException e) {
			err = true;
			throw e;
		} finally {
			// FIXME : is this the correct way to catch the error?
			if (err || varValue == null) {
				interp
						.addErrorInfo("\n    (reading value of variable to increment)");
			}
		}

		// Increment the variable's value. If the object is unshared we can
		// modify it directly, otherwise we must create a new copy to modify:
		// this is "copy on write". The incr() method will free the old
		// string rep since it is no longer valid.

		createdNewObj = false;
		if (varValue.isShared()) {
			varValue = varValue.duplicate();
			createdNewObj = true;
		}

		try {
			TclInteger.incr(interp, varValue, incrAmount);
		} catch (TclException e) {
			if (createdNewObj) {
				varValue.release(); // free unneeded copy
			}
			throw e;
		}

		// Store the variable's new value and run any write traces.

		return setVar(interp, part1, part2, varValue, flags);
	}

	/**
	 * Tcl_UnsetVar2 -> unsetVar
	 * 
	 * Unset a variable, given a two-part name consisting of array name and
	 * element within array.
	 * 
	 * @param part1
	 *            1st part of the variable name.
	 * @param part2
	 *            2nd part of the variable name.
	 * @param flags
	 *            misc flags that control the actions of this method.
	 * 
	 *            If part1 and part2 indicate a local or global variable in
	 *            interp, it is deleted. If part1 is an array name and part2 is
	 *            null, then the whole array is deleted.
	 * 
	 */

	static void unsetVar(Interp interp, // Command interpreter in which var is
			// to be looked up.
			String part1, // Name of variable or array.
			String part2, // Name of element within array or null.
			int flags // OR-ed combination of any of
	// TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
	// TCL.LEAVE_ERR_MSG.
	) throws TclException {
		Var dummyVar;
		Var var;
		Var array;
		// ActiveVarTrace active;
		TclObject obj;
		int result;

		// FIXME : what about the null return vs exception thing here?
		Var[] lookup_result = lookupVar(interp, part1, part2, flags, "unset",
				false, false);
		if (lookup_result == null) {
			throw new TclRuntimeError("unexpected null reference");
		}

		var = lookup_result[0];
		array = lookup_result[1];

		result = (var.isVarUndefined() ? TCL.ERROR : TCL.OK);

		if ((array != null) && (array.sidVec != null)) {
			deleteSearches(array);
		}

		// The code below is tricky, because of the possibility that
		// a trace procedure might try to access a variable being
		// deleted. To handle this situation gracefully, do things
		// in three steps:
		// 1. Copy the contents of the variable to a dummy variable
		// structure, and mark the original Var structure as undefined.
		// 2. Invoke traces and clean up the variable, using the dummy copy.
		// 3. If at the end of this the original variable is still
		// undefined and has no outstanding references, then delete
		// it (but it could have gotten recreated by a trace).

		dummyVar = new Var();
		// FIXME: Var class really should implement clone to make a bit copy.
		dummyVar.setValue(var.getValue());
		if (var.getArrayMap()!=null) {
			dummyVar.createArrayMap();
			dummyVar.getArrayMap().putAll(var.getArrayMap());
		}
		dummyVar.linkto = var.linkto;
		dummyVar.traces = var.traces;
		dummyVar.flags = var.flags;
		dummyVar.hashKey = var.hashKey;
		dummyVar.table = var.table;
		dummyVar.refCount = var.refCount;
		dummyVar.ns = var.ns;

		var.setVarUndefined();
		var.setVarScalar();
		var.setValue(null);  // dummyVar points to any value object
		var.deleteArrayMap();
		var.linkto = null;
		var.traces = null;
		var.sidVec = null;

		// Call trace procedures for the variable being deleted. Then delete
		// its traces. Be sure to abort any other traces for the variable
		// that are still pending. Special tricks:
		// 1. We need to increment var's refCount around this: CallTraces
		// will use dummyVar so it won't increment var's refCount itself.
		// 2. Turn off the TRACE_ACTIVE flag in dummyVar: we want to
		// call unset traces even if other traces are pending.

		if ((dummyVar.traces != null)
				|| ((array != null) && (array.traces != null))) {
			var.refCount++;
			dummyVar.flags &= ~TRACE_ACTIVE;
			callTraces(interp, array, dummyVar, part1, part2,
					(flags & (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY))
							| TCL.TRACE_UNSETS);

			dummyVar.traces = null;

			// Active trace stuff is not part of Jacl's interp

			var.refCount--;
		}

		// If the variable is an array, delete all of its elements. This must be
		// done after calling the traces on the array, above (that's the way
		// traces are defined). If it is a scalar, "discard" its object
		// (decrement the ref count of its object, if any).

		if (dummyVar.isVarArray() && !dummyVar.isVarUndefined()) {
			deleteArray(interp, part1, dummyVar,
					(flags & (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY))
							| TCL.TRACE_UNSETS);
		}
		if (dummyVar.isVarScalar() && (dummyVar.getValue() != null)) {
			obj = dummyVar.getValue();
			obj.release();
			dummyVar.setValue(null);
		}

		// If the variable was a namespace variable, decrement its reference
		// count.

		if ((var.flags & NAMESPACE_VAR) != 0) {
			var.flags &= ~NAMESPACE_VAR;
			var.refCount--;
		}

		// Finally, if the variable is truly not in use then free up its Var
		// structure and remove it from its hash table, if any. The ref count of
		// its value object, if any, was decremented above.

		cleanupVar(var, array);

		Var.setUndefinedToNull(interp, part1, part2);

		// It's an error to unset an undefined variable.

		if (result != TCL.OK) {
			if ((flags & TCL.LEAVE_ERR_MSG) != 0) {
				throw new TclVarException(interp, part1, part2, "unset",
						((array == null) ? noSuchVar : noSuchElement));
			}
		}
	}

	/**
	 * Tcl_TraceVar2 -> traceVar
	 * 
	 * Trace a variable, given a two-part name consisting of array name and
	 * element within array.
	 * 
	 * @param part1
	 *            1st part of the variable name.
	 * @param part2
	 *            2nd part of the variable name. null means part1 is a scalar or
	 *            whole array.
	 * @param flags
	 *            misc flags that control the actions of this method. OR-ed
	 *            collection of bits, including any  of TCL.TRACE_READS,
	 *            TCL.TRACE_WRITES,  TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY, // and
	 *            TCL.NAMESPACE_ONLY.
	 * @param proc
	 *            the trace to comand to add.
	 */

	public static void traceVar(Interp interp, String part1, String part2, int flags, VarTrace proc) throws TclException {
		Var[] result;
		Var var;

		// FIXME: what about the exception problem here?
		result = lookupVar(interp, part1, part2, (flags | TCL.LEAVE_ERR_MSG),
				"trace", true, true);
		if (result == null) {
			throw new TclException(interp, "");
		}

		var = result[0];

		// Set up trace information. Set a flag to indicate that traces
		// exists so that resolveScalar() can determine if traces
		// are set by checking only the Var flags filed. The rest of
		// the code in this module makes use of the var.traces field.

		if (var.traces == null) {
			var.setVarTraceExists();
			var.traces = new ArrayList();
		}

		TraceRecord rec = new TraceRecord();
		rec.trace = proc;
		rec.flags = flags
				& (TCL.TRACE_READS | TCL.TRACE_WRITES | TCL.TRACE_UNSETS | TCL.TRACE_ARRAY);

		var.traces.add(0, rec);
	}

	/**
	 * Tcl_UntraceVar2 -> untraceVar
	 * 
	 * Untrace a variable, given a two-part name consisting of array name and
	 * element within array. This will Remove a previously-created trace for a
	 * variable.
	 * 
	 * @param interp
	 *            Interpreter containing variable.
	 * @param part1
	 *            1st part of the variable name.
	 * @param part2
	 *            2nd part of the variable name.
	 * @param flags
	 *            misc flags that control the actions of this method.
	 * @param proc
	 *            the trace to delete.
	 */

	public static void untraceVar(Interp interp, // Interpreter containing
													// variable.
			String part1, // Name of variable or array.
			String part2, // Name of element within array; null means
			// trace applies to scalar variable or array
			// as-a-whole.
			int flags, // OR-ed collection of bits describing
			// current trace, including any of
			// TCL.TRACE_READS, TCL.TRACE_WRITES,
			// TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY,
			// and TCL.NAMESPACE_ONLY.
			VarTrace proc // Procedure assocated with trace.
	) {
		Var[] result = null;
		Var var;

		try {
			result = lookupVar(interp, part1, part2, flags
					& (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY), null, false,
					false);
			if (result == null) {
				return;
			}
		} catch (TclException e) {
			// FIXME: check for problems in exception in lookupVar

			// We have set throwException argument to false in the
			// lookupVar() call, so an exception should never be
			// thrown.

			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		var = result[0];

		if (var.traces != null) {
			int len = var.traces.size();
			for (int i = 0; i < len; i++) {
				TraceRecord rec = (TraceRecord) var.traces.get(i);
				if (rec.trace == proc) {
					var.traces.remove(i);
					break;
				}
			}

			// If there are no more traces, then null
			// the var.traces field since logic in this
			// module depends on a null traces field.

			if (var.traces.size() == 0) {
				var.traces = null;
				var.clearVarTraceExists();
			}
		}

		// If this is the last trace on the variable, and the variable is
		// unset and unused, then free up the variable.

		if (var.isVarUndefined()) {
			cleanupVar(var, null);
		}
	}

	/**
	 * Tcl_VarTraceInfo2 -> getTraces
	 * 
	 * @return the list of traces of a variable.
	 * 
	 * @param interp
	 *            Interpreter containing variable.
	 * @param part1
	 *            1st part of the variable name.
	 * @param part2
	 *            2nd part of the variable name (can be null).
	 * @param flags
	 *            misc flags that control the actions of this method.
	 */

	public static ArrayList getTraces(Interp interp, // Interpreter containing
														// variable.
			String part1, // Name of variable or array.
			String part2, // Name of element within array; null means
			// trace applies to scalar variable or array
			// as-a-whole.
			int flags // OR-ed combination of TCL.GLOBAL_ONLY,
	// TCL.NAMESPACE_ONLY.
	) throws TclException {
		Var[] result;

		result = lookupVar(interp, part1, part2, flags
				& (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY), null, false, false);

		if (result == null) {
			return null;
		}

		return result[0].traces;
	}

	/**
	 * MakeUpvar -> makeUpvar
	 * 
	 * Create a reference of a variable in otherFrame in the current CallFrame,
	 * given a two-part name consisting of array name and element within array.
	 * 
	 * @param interp
	 *            Interp containing the variables
	 * @param frame
	 *            CallFrame containing "other" variable. null means use global
	 *            context.
	 * @param otherP1
	 *            the 1st part name of the variable in the "other" frame.
	 * @param otherP2
	 *            the 2nd part name of the variable in the "other" frame.
	 * @param otherFlags
	 *            the flags for scaope of "other" variable
	 * @param myName
	 *            Name of scalar variable which will refer to otherP1/otherP2.
	 * @param myFlags
	 *            only the TCL.GLOBAL_ONLY bit matters, indicating the scope of
	 *            myName.
	 * @exception TclException
	 *                if the upvar cannot be created.
	 */

	// FIXME: Tcl 8.4 implements resolver logic specific to upvar with the flag
	// LOOKUP_FOR_UPVAR. Port this logic and test cases to Jacl.

	public static void makeUpvar(Interp interp, // Interpreter containing
												// variables. Used
			// for error messages, too.
			CallFrame frame, // Call frame containing "other" variable.
			// null means use global :: context.
			String otherP1, // Two-part name of variable in framePtr.
			String otherP2, int otherFlags, // 0, TCL.GLOBAL_ONLY or
											// TCL.NAMESPACE_ONLY:
			// indicates scope of "other" variable.
			String myName, // Name of variable which will refer to
			// otherP1/otherP2. Must be a scalar.
			int myFlags, // 0, TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY:
			// indicates scope of myName. Also accepts
			// the special Var.EXPLICIT_LOCAL_NAME flag
			// which is used to ignore namespace lookup
			// rules for myname.
			int localIndex // If != -1, this is the index into the
	// compiledLocals array where the upvar
	// variable could be stored.
	) throws TclException {
		Var other, var, array;
		Var[] result = null;
		CallFrame varFrame;
		CallFrame savedFrame = null;
		HashMap table;
		Namespace ns, altNs;
		String tail;
		boolean newvar = false;
		boolean foundInCompiledLocalsArray = false;
		boolean foundInLocalTable = false;
		Var[] compiledLocals = null;

		// Find "other" in "frame". If not looking up other in just the
		// current namespace, temporarily replace the current var frame
		// pointer in the interpreter in order to use TclLookupVar.

		try {
			if ((otherFlags & TCL.NAMESPACE_ONLY) == 0) {
				savedFrame = interp.varFrame;
				interp.varFrame = frame;
			}

			// If the special EXPLICIT_LOCAL_NAME flag is passed, then
			// do nothing instead of raising a TclException when the
			// other lookup fails.

			int otherLookupFlags = (otherFlags | TCL.LEAVE_ERR_MSG);
			if ((myFlags & EXPLICIT_LOCAL_NAME) != 0) {
				otherLookupFlags = otherFlags;
			}

			result = Var.lookupVar(interp, otherP1, otherP2, otherLookupFlags,
					"access", true, true);
		} finally {
			// Reset interp.varFrame

			if ((otherFlags & TCL.NAMESPACE_ONLY) == 0) {
				interp.varFrame = savedFrame;
			}
		}

		if (result == null) {
			// EXPLICIT_LOCAL_NAME flag passed and lookup failed.
			return;
		}

		other = result[0];
		array = result[1];

		if (other == null) {
			// Should not be returned since TCL.LEAVE_ERR_MSG
			// was passed to generate a TclException.
			throw new TclRuntimeError("unexpected null reference");
		}

		// This module assumes that a link target will never
		// be a link var. In this way, a link var is
		// always resolved to either a scalar or an array
		// by following a single link. The lookupVar() method
		// should always return either a scalar or an array.

		if (other.isVarLink()) {
			throw new TclRuntimeError("other var resolved to a link var");
		}

		// Now create a variable entry for "myName". Create it as either a
		// namespace variable or as a local variable in a procedure call
		// frame. Interpret myName as a namespace variable if:
		// 1) so requested by a TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY flag,
		// 2) there is no active frame (we're at the global :: scope),
		// 3) the active frame was pushed to define the namespace context
		// for a "namespace eval" or "namespace inscope" command,
		// 4) the name has namespace qualifiers ("::"s), unless
		// the special EXPLICIT_LOCAL_NAME flag is set.
		// If creating myName in the active procedure, look in its
		// hashtable for runtime-created local variables. Create that
		// procedure's local variable hashtable if necessary.

		varFrame = interp.varFrame;
		if (((myFlags & (TCL.GLOBAL_ONLY | TCL.NAMESPACE_ONLY)) != 0)
				|| (varFrame == null)
				|| !varFrame.isProcCallFrame
				|| ((myName.contains("::")) && ((myFlags & EXPLICIT_LOCAL_NAME) == 0))) {

			Namespace.GetNamespaceForQualNameResult gnfqnr = interp.getnfqnResult;
			Namespace.getNamespaceForQualName(interp, myName, null, myFlags,
					gnfqnr);
			ns = gnfqnr.ns;
			altNs = gnfqnr.altNs;
			tail = gnfqnr.simpleName;

			if (ns == null) {
				ns = altNs;
			}
			if (ns == null) {
				throw new TclException(interp,"can't create \"" + myName + "\": parent namespace doesn't exist");
			}

			// Check that we are not trying to create a namespace var linked to
			// a local variable in a procedure. If we allowed this, the local
			// variable in the shorter-lived procedure frame could go away
			// leaving the namespace var's reference invalid.

			if (((otherP2 != null) ? array.ns : other.ns) == null) {
				throw new TclException(
						interp,
						"bad variable name \""
								+ myName
								+ "\": upvar won't create namespace variable that refers to procedure variable");
			}

			var = (Var) ns.varTable.get(tail);
			if (var == null) { // we are adding a new entry
				newvar = true;
				var = new Var();
				ns.varTable.put(tail, var);

				// There is no hPtr member in Jacl, The hPtr combines the table
				// and the key used in a table lookup.
				var.hashKey = tail;
				var.table = ns.varTable;

				var.ns = ns; // Namespace var
			}
		} else {
			var = null;

			compiledLocals = varFrame.compiledLocals;
			if (compiledLocals != null) { // look in compiled local array
				// A compiled local array is defined, so
				// check in the compiled local array for
				// a variable with this name. In the case
				// where the compiled local index is known,
				// we only need to check one index. Note
				// that an explicit non-local name should
				// be matched.

				if (localIndex == -1) {
					// compiled local slot is not known, search by name.
					final int MAX = compiledLocals.length;
					String[] compiledLocalsNames = varFrame.compiledLocalsNames;

					for (int i = 0; i < MAX; i++) {
						if (compiledLocalsNames[i].equals(myName)) {
							foundInCompiledLocalsArray = true;
							localIndex = i;
							var = compiledLocals[i];
							break;
						}
					}
				} else {
					// Slot the var should live in is known at compile time.
					// Check to see if compiled local var exists already.

					foundInCompiledLocalsArray = true;
					var = compiledLocals[localIndex];
				}
			}

			if (!foundInCompiledLocalsArray) { // look in frame's local var
												// hashtable
				table = varFrame.varTable;

				// Note: Don't create var in the local table when it
				// should be created in the compiledLocals array.

				if (table == null) {
					table = new HashMap();
					varFrame.varTable = table;
				}

				if (table != null) {
					var = (Var) table.get(myName);
				}

				if (var == null) { // we are adding a new entry
					newvar = true;
					var = new Var();
					table.put(myName, var);

					var.hashKey = myName;
					var.table = table;
				}
				if (var != null) {
					foundInLocalTable = true;
				}
			}

			// Var should live in the compiled local
			// array, but it does not exist yet. Create
			// a new Var instance that will be assigned
			// to the compiled local array slot. This
			// Var instance will become a link var.

			if (foundInCompiledLocalsArray && (var == null)) {
				newvar = true;
				var = new Var();
				var.hashKey = myName;
				var.clearVarInHashtable();
			}
		}

		if (!newvar) {
			// The variable already exists. Make sure this variable "var"
			// isn't the same as "other" (avoid circular links). Also, if
			// it's not an upvar then it's an error. If it is an upvar, then
			// just disconnect it from the thing it currently refers to.

			if (var == other) {
				throw new TclException(interp,
						"can't upvar from variable to itself");
			}
			if (var.isVarLink()) {
				Var link = var.linkto;
				if (link == other) {
					// Already linked to the variable, no-op
					return;
				}
				link.refCount--;
				if (link.isVarUndefined()) {
					cleanupVar(link, null);
				}
			} else if (!var.isVarUndefined()) {
				throw new TclException(interp, "variable \"" + myName
						+ "\" already exists");
			} else if (var.traces != null) {
				throw new TclException(interp, "variable \"" + myName
						+ "\" has traces: can't use for upvar");
			}
			// FIXME: Is it possible that the other var
			// would not be a var link type but it would
			// be undefined waiting to be cleaned up?
			// For example, a linked var in another scope?
			// Add a test case for this.
		}

		var.setVarLink();
		var.clearVarUndefined();
		var.linkto = other;
		other.refCount++;

		// If the link var should be stored in the compiledLocals
		// array then do that now. A variable with this same
		// name would never appear in the local table. Also mark
		// this variable as non-local for scoped global.

		if (foundInCompiledLocalsArray) {
			if (newvar) {
				compiledLocals[localIndex] = var;
			}

			if ((myFlags & EXPLICIT_LOCAL_NAME) != 0) {
				var.setVarNonLocal();
			}
		}

		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_GetVariableFullName -> getVariableFullName
	 * 
	 * Given a Var token returned by Namespace.FindNamespaceVar, this procedure
	 * appends to an object the namespace variable's full name, qualified by a
	 * sequence of parent namespace names.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The variable's fully-qualified name is returned.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static String getVariableFullName(Interp interp, // Interpreter
															// containing the
															// variable.
			Var var // Token for the variable returned by a
	// previous call to Tcl_FindNamespaceVar.
	) {
		StringBuilder buff = new StringBuilder();

		// Add the full name of the containing namespace (if any), followed by
		// the "::" separator, then the variable name.

		if (var != null) {
			if (!var.isVarArrayElement()) {
				if (var.ns != null) {
					buff.append(var.ns.fullName);
					if (var.ns != interp.globalNs) {
						buff.append("::");
					}
				}
				// Jacl's Var class does not include the "name" member
				// We use the "hashKey" member which is equivalent

				if (var.hashKey != null) {
					buff.append(var.hashKey);
				}
			}
		}

		return buff.toString();
	}

	/**
	 * CallTraces -> callTraces
	 * 
	 * This procedure is invoked to find and invoke relevant trace procedures
	 * associated with a particular operation on a variable. This procedure
	 * invokes traces both on the variable and on its containing array (where
	 * relevant).
	 * 
	 * @param interp
	 *            Interpreter containing variable.
	 * @param array
	 *            array variable that contains the variable, or null if the
	 *            variable isn't an element of an array.
	 * @param var
	 *            Variable whose traces are to be invoked.
	 * @param part1
	 *            the first part of a variable name.
	 * @param part2
	 *            the second part of a variable name.
	 * @param flags
	 *            Flags to pass to trace procedures: indicates what's happening
	 *            to variable, plus other stuff like TCL.GLOBAL_ONLY,
	 *            TCL.NAMESPACE_ONLY, and TCL.INTERP_DESTROYED.
	 * @return null if no trace procedures were invoked, or if all the invoked
	 *         trace procedures returned successfully. The return value is
	 *         non-null if a trace procedure returned an error (in this case no
	 *         more trace procedures were invoked after the error was returned).
	 *         In this case the return value is a pointer to a string describing
	 *         the error.
	 */

	public static String callTraces(Interp interp, Var array, Var var,
			String part1, String part2, int flags) {
		TclObject oldResult;
		int i;

		// If there are already similar trace procedures active for the
		// variable, don't call them again.

		if ((var.flags & Var.TRACE_ACTIVE) != 0) {
			return null;
		}
		var.flags |= Var.TRACE_ACTIVE;
		var.refCount++;

		// If the variable name hasn't been parsed into array name and
		// element, do it here. If there really is an array element,
		// make a copy of the original name so that nulls can be
		// inserted into it to separate the names (can't modify the name
		// string in place, because the string might get used by the
		// callbacks we invoke).

		// FIXME : a util method that parsed an array variable
		// name into array and element component parts could
		// be useful. There are a number of places where this
		// is done inline in the code.
		if (part2 == null) {
			int len = part1.length();

			if (len > 0) {
				if (part1.charAt(len - 1) == ')') {
					for (i = 0; i < len - 1; i++) {
						if (part1.charAt(i) == '(') {
							break;
						}
					}
					if (i < len - 1) {
						if (i < len - 2) {
							part2 = part1.substring(i + 1, len - 1);
							part1 = part1.substring(0, i);
						}
					}
				}
			}
		}

		oldResult = interp.getResult();
		oldResult.preserve();
		interp.resetResult();

		try {
			// Invoke traces on the array containing the variable, if relevant.

			if (array != null) {
				array.refCount++;
			}
			if ((array != null) && (array.traces != null)) {
				for (i = 0; (array.traces != null) && (i < array.traces.size()); i++) {
					TraceRecord rec = (TraceRecord) array.traces.get(i);
					if ((rec.flags & flags) != 0) {
						try {
							rec.trace.traceProc(interp, part1, part2, flags);
						} catch (TclException e) {
							if ((flags & TCL.TRACE_UNSETS) == 0) {
								return interp.getResult().toString();
							}
						}
					}
				}
			}

			// Invoke traces on the variable itself.

			if ((flags & TCL.TRACE_UNSETS) != 0) {
				flags |= TCL.TRACE_DESTROYED;
			}

			for (i = 0; (var.traces != null) && (i < var.traces.size()); i++) {
				TraceRecord rec = (TraceRecord) var.traces.get(i);
				if ((rec.flags & flags) != 0) {
					try {
						rec.trace.traceProc(interp, part1, part2, flags);
					} catch (TclException e) {
						if ((flags & TCL.TRACE_UNSETS) == 0) {
							return interp.getResult().toString();
						}
					}
				}
			}

			return null;
		} finally {
			if (array != null) {
				array.refCount--;
			}
			var.flags &= ~TRACE_ACTIVE;
			var.refCount--;

			interp.setResult(oldResult);
			oldResult.release();
		}
	}

	/**
	 * DeleteSearches -> deleteSearches
	 * 
	 * This procedure is called to free up all of the searches associated with
	 * an array variable.
	 * 
	 * @param arrayVar
	 *            the array variable to delete searches from.
	 */

	static protected void deleteSearches(Var arrayVar) // Variable whose
														// searches are to be
														// deleted.
	{
		arrayVar.sidVec = null;
	}

	/**
	 * TclDeleteVars -> deleteVars
	 * 
	 * This procedure is called to recycle all the storage space associated with
	 * a table of variables. For this procedure to work correctly, it must not
	 * be possible for any of the variables in the table to be accessed from Tcl
	 * commands (e.g. from trace procedures).
	 * 
	 * @param interp
	 *            Interpreter containing array.
	 * @param table
	 *            HashMap that holds the Vars to delete
	 */

	public static void deleteVars(Interp interp, HashMap table) {
		int flags;
		Namespace currNs = Namespace.getCurrentNamespace(interp);

		// Determine what flags to pass to the trace callback procedures.

		flags = TCL.TRACE_UNSETS;
		if (table == interp.globalNs.varTable) {
			flags |= (TCL.INTERP_DESTROYED | TCL.GLOBAL_ONLY);
		} else if (table == currNs.varTable) {
			flags |= TCL.NAMESPACE_ONLY;
		}

		for (Object o : table.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			deleteVar(interp, (Var) entry.getValue(), flags);
		}
		table.clear();
	}

	/**
	 * // FIXME: Make more like TclDeleteCompiledLocalVars() TclDeleteVars ->
	 * deleteVars
	 * 
	 * This procedure is called to recycle all the storage space associated with
	 * an array of variables. For this procedure to work correctly, it must not
	 * be possible for any of the variables in the array to be accessed from Tcl
	 * commands (e.g. from trace procedures).
	 * 
	 * @param interp
	 *            Interpreter containing array.
	 * @param compiledLocals
	 *            array of compiled local variables
	 */

	static protected void deleteVars(Interp interp, Var[] compiledLocals) {
		// A compiled local array only ever exists for a compiled
		// proc, so flags is always the same.

		final int flags = TCL.TRACE_UNSETS;

		final int max = compiledLocals.length;

		for (int i = 0; i < max; i++) {
			Var clocal = compiledLocals[i];
			if (clocal != null) {
				// Cleanup the Var instance and then
				// null out the compiled local slots.

				deleteVar(interp, clocal, flags);
				compiledLocals[i] = null;
			}
		}
	}

	/**
	 * deleteVar
	 * 
	 * This procedure is called to recycle all the storage space associated with
	 * a single Var instance.
	 * 
	 * @param interp
	 *            Interpreter containing array.
	 * @param var
	 *            A Var refrence to be deleted
	 * @param flags
	 *            flags to pass to trace callbacks.
	 */

	static protected void deleteVar(Interp interp, Var var, int flags) {
		// For global/upvar variables referenced in procedures, decrement
		// the reference count on the variable referred to, and free
		// the referenced variable if it's no longer needed. Note that
		// we always delete the link in another table, this should be
		// fine since this method is invoked after the regular variables
		// are deleted.

		if ((var.flags & LINK) != 0) {
			// Follow link to either scalar or array variable
			Var link = var.linkto;
			link.refCount--;
			if ((link.refCount == 0)
					&& (link.traces == null)
					// (link.isVarUndefined() && link.isVarInHashtable())
					&& ((link.flags & (UNDEFINED | IN_HASHTABLE)) == (UNDEFINED | IN_HASHTABLE))) {
				if (link.hashKey == null) {
					var.linkto = null; // Drops reference to the link Var
				} else if (link.table != var.table) {
					link.table.remove(link.hashKey);
					link.table = null; // Drops the link var's table reference
					var.linkto = null; // Drops reference to the link Var
				}
			}
		}

		// free up the variable's space (no need to free the hash entry
		// here, unless we're dealing with a global variable: the
		// hash entries will be deleted automatically when the whole
		// table is deleted). Note that we give callTraces the variable's
		// fully-qualified name so that any called trace procedures can
		// refer to these variables being deleted.  Also, set the
		// variable undefined before the trace, so an error is generated
		// if the variable is read in the trace.  This is consistent with
		// unset.
		var.setVarUndefined();

		if (var.traces != null) {
			String fullname = getVariableFullName(interp, var);

			callTraces(interp, null, var, fullname, null, flags);

			// The var.traces = null statement later will drop all the
			// references to the traces which will free them up
		}

		if ((var.flags & ARRAY) != 0) {
			deleteArray(interp, var.hashKey, var, flags);
			var.deleteArrayMap();
		} else if (((var.flags & SCALAR) != 0) && (var.getValue() != null)) {
			TclObject obj = var.getValue();
			obj.release();
			var.setValue(null);
		}

		var.hashKey = null;
		var.table = null;
		var.traces = null;
		var.setVarUndefined();
		var.setVarScalar();

		// If the variable was a namespace variable, decrement its
		// reference count. We are in the process of destroying its
		// namespace so that namespace will no longer "refer" to the
		// variable.

		if ((var.flags & NAMESPACE_VAR) != 0) {
			var.flags &= ~NAMESPACE_VAR;
			var.refCount--;
		}

		// Recycle the variable's memory space if there aren't any upvar's
		// pointing to it. If there are upvars to this variable, then the
		// variable will get freed when the last upvar goes away. This
		// variable could still be alive after this method finishes since
		// it could be refrenced in a namespace. The code will catch
		// that case by looking at the IN_HASHTABLE flag and seeing
		// if the table member is null.

		// if (var.refCount == 0) {
		// // When we drop the last reference it will be freeded
		// }
	}

	/**
	 * DeleteArray -> deleteArray
	 * 
	 * This procedure is called to free up everything in an array variable. It's
	 * the caller's responsibility to make sure that the array is no longer
	 * accessible before this procedure is called.
	 * 
	 * @param interp
	 *            Interpreter containing array.
	 * @param arrayName
	 *            name of array (used for trace callbacks).
	 * @param var
	 *            the array variable to delete.
	 * @param flags
	 *            Flags to pass to CallTraces.
	 */

	static protected void deleteArray(Interp interp, // Interpreter containing
														// array.
			String arrayName, // Name of array (used for trace callbacks)
			Var var, // Reference to Var instance
			int flags // Flags to pass to callTraces:
	// TCL.TRACE_UNSETS and sometimes
	// TCL.INTERP_DESTROYED,
	// TCL.NAMESPACE_ONLY, or
	// TCL.GLOBAL_ONLY.
	) {
		Var el;
		TclObject obj;

		deleteSearches(var);
		Map<String, Var> table = var.getArrayMap();

		for (Map.Entry<String, Var> stringVarEntry : table.entrySet()) {
			Map.Entry entry = (Map.Entry) stringVarEntry;
			// String key = (String) entry.getKey();
			el = (Var) entry.getValue();

			if (el.isVarScalar() && (el.getValue() != null)) {
				obj = el.getValue();
				;
				obj.release();
				el.setValue(null);
			}

			String tmpkey = (String) el.hashKey;
			// There is no hPtr member in Jacl, The hPtr combines the table
			// and the key used in a table lookup.
			el.hashKey = null;
			el.table = null;
			if (el.traces != null) {
				el.flags &= ~TRACE_ACTIVE;
				// FIXME : Old Jacl impl passed a dummy var to callTraces,
				// should we?
				callTraces(interp, null, el, arrayName, tmpkey, flags);
				el.traces = null;
				// Active trace stuff is not part of Jacl
			}
			el.setVarUndefined();
			el.setVarScalar();
			if (el.refCount == 0) {
				// We are no longer using the element
				// element Vars are IN_HASHTABLE
			}
		}
		table.clear();
		var.deleteArrayMap();
	}

	/**
	 * CleanupVar -> cleanupVar
	 * 
	 * This procedure is called when it looks like it may be OK to free up the
	 * variable's record and hash table entry, and those of its containing
	 * parent. It's called, for example, when a trace on a variable deletes the
	 * variable.
	 * 
	 * @param var
	 *            variable that may be a candidate for being expunged.
	 * @param array
	 *            Array that contains the variable, or NULL if this variable
	 *            isn't an array element.
	 */

	static protected void cleanupVar(Var var, Var array) {
		if (var.isVarUndefined() && (var.refCount == 0) && (var.traces == null)
				&& ((var.flags & IN_HASHTABLE) != 0)) {
			if (var.table != null) {
				var.table.remove(var.hashKey);
				var.table = null;
				var.hashKey = null;
			}
		}
		if (array != null) {
			if (array.isVarUndefined() && (array.refCount == 0)
					&& (array.traces == null)
					&& ((array.flags & IN_HASHTABLE) != 0)) {
				if (array.table != null) {
					array.table.remove(array.hashKey);
					array.table = null;
					array.hashKey = null;
				}
			}
		}
	}

	// CompiledLocal utilitiy methods.

	// The resolveScalar() method will
	// attempt to resolve a scalar Var ref
	// in the compiled local array.
	// If the Var can't be resolved as
	// a valid scalar, then null will
	// be returned.
	//
	// A resolved scalar is invalid when any
	// of the following conditions is true.
	//
	// When the variable is not a scalar.
	// When the variable is an array element.
	// When the variable is undefined.
	// When the variable has traces.
	// When the variable can't be cached.

	public static Var resolveScalar(Var v) {
		int flags = v.flags;
		if ((flags & LINK) != 0) {
			v = v.linkto;
			flags = v.flags;
		}

		// Can't resolve var if it is not a scalar, if it is
		// an array element, if it is undefined, if it has traces,
		// or it can't be cached because it is a resolver var.

		if ((flags & (LINK | ARRAY | ARRAY_ELEMENT | UNDEFINED | TRACE_EXISTS | NO_CACHE)) != 0) {
			return null;
		}
		return v;
	}

	// The resolveArray() method will
	// attempt to resolve an array Var ref
	// in the compiled local array.
	// If the Var can't be resolved as
	// a valid array variable, then null will
	// be returned.
	//
	// A resolved array is invalid when any
	// of the following conditions is true.
	//
	// When the variable is not an array.
	// When the variable is undefined.
	//
	// Note that an array with traces is
	// considered valid since the array
	// methods support invoking traces.

	public static Var resolveArray(Var v) {
		int flags = v.flags;
		if ((flags & LINK) != 0) {
			v = v.linkto;
			flags = v.flags;
		}

		// Can't resolve var if it is not an array,
		// if it is undefined, or it can't be cached
		// because it is a resolver var. An array
		// var with traces can be resolved.

		if ((flags & (LINK | SCALAR | UNDEFINED | NO_CACHE)) != 0) {
			return null;
		}
		return v;
	}

	// Helper method invoked to null out a compiled local
	// slot for a non-linked local variable that is now
	// undefined. The unset command has no way of knowing
	// if a variable being unset lives in the compiled
	// local array, so this method is invoked after
	// each unset operation to keep the compiled local
	// array up to date. Variables linked into another
	// scope can be undefined, so ignore those.

	static void setUndefinedToNull(Interp interp, String part1, String part2) {
		CallFrame varFrame = interp.varFrame;
		if (varFrame == null) {
			return; // Invoked from global scope
		}

		if (varFrame.compiledLocals != null) {
			Var[] compiledLocals = varFrame.compiledLocals;
			final int MAX = compiledLocals.length;

			for (int i = 0; i < MAX; i++) {
				Var clocal = compiledLocals[i];
				if (clocal != null && !clocal.isVarNonLocal()
						&& clocal.hashKey.equals(part1)) {
					if (!clocal.isVarLink() && clocal.isVarUndefined()) {
						// Set the compiled local slot to null
						// if there are no other vars linked to
						// this var.
						if (clocal.refCount == 0) {
							compiledLocals[i] = null;
						}
					}
					return;
				}
			}
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * AppendLocals --
	 * 
	 * Append the local variables for the current frame to the specified list
	 * object. This method is used by InfoCmd.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static void AppendLocals(Interp interp, // Current interp
			TclObject list, // list to append to
			String pattern, // Pattern to match against.
			boolean includeLinks) // true if upvars should be included
			throws TclException {
		Var var;
		String varName;
		CallFrame frame = interp.varFrame;

		HashMap localVarTable = frame.varTable;
		if (localVarTable != null) {
			for (Object o : localVarTable.entrySet()) {
				Map.Entry entry = (Map.Entry) o;
				varName = (String) entry.getKey();
				var = (Var) entry.getValue();
				if (!var.isVarUndefined() && (includeLinks || !var.isVarLink())) {
					if ((pattern == null) || Util.stringMatch(varName, pattern)) {
						TclList.append(interp, list, TclString
								.newInstance(varName));
					}
				}
			}
		}

		Var[] compiledLocals = frame.compiledLocals;
		if (compiledLocals != null) {
			final int max = compiledLocals.length;
			for (Var clocal : compiledLocals) {
				if (clocal != null && !clocal.isVarNonLocal()) {
					var = clocal;
					varName = (String) var.hashKey;

					if (!var.isVarUndefined()
							&& (includeLinks || !var.isVarLink())) {
						if ((pattern == null)
								|| Util.stringMatch(varName, pattern)) {
							TclList.append(interp, list, TclString
									.newInstance(varName));
						}
					}
				}
			}
		}
	}

} // End of Var class

