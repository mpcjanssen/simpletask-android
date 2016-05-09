/*
 * ArrayCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ArrayCmd.java,v 1.7 2006/05/23 05:34:33 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.SearchId;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.TclVarException;
import tcl.lang.Util;
import tcl.lang.Var;

/**
 * This class implements the built-in "array" command in Tcl.
 */

public class ArrayCmd implements Command {
	static Class procClass = null;

	static final private String validCmds[] = { "anymore", "donesearch", "exists", "get", "names", "nextelement",
			"set", "size", "startsearch", "unset" };

	static final int OPT_ANYMORE = 0;
	static final int OPT_DONESEARCH = 1;
	static final int OPT_EXISTS = 2;
	static final int OPT_GET = 3;
	static final int OPT_NAMES = 4;
	static final int OPT_NEXTELEMENT = 5;
	static final int OPT_SET = 6;
	static final int OPT_SIZE = 7;
	static final int OPT_STARTSEARCH = 8;
	static final int OPT_UNSET = 9;

	/**
	 * This procedure is invoked to process the "array" Tcl command. See the
	 * user documentation for details on what it does.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		Var var = null, array = null;
		boolean notArray = false;
		String varName, msg;
		int index, result;

		if (objv.length < 3) {
			throw new TclNumArgsException(interp, 1, objv, "option arrayName ?arg ...?");
		}

		index = TclIndex.get(interp, objv[1], validCmds, "option", 0);

		// Locate the array variable (and it better be an array).

		varName = objv[2].toString();
		Var[] retArray = Var.lookupVar(interp, varName, null, 0, null, false, false);

		// Assign the values returned in the array
		if (retArray != null) {
			var = retArray[0];
			array = retArray[1];
		}

		if ((var == null) || !var.isVarArray() || var.isVarUndefined()) {
			notArray = true;
		}

		// fire array traces

		if (var != null && var.traces != null && (!var.isVarScalar() || var.isVarUndefined())) {
			msg = Var.callTraces(interp, array, var, varName, null, (TCL.LEAVE_ERR_MSG | TCL.NAMESPACE_ONLY
					| TCL.GLOBAL_ONLY | TCL.TRACE_ARRAY));
			if (msg != null) {
				throw new TclVarException(interp, varName, null, "trace array", msg);
			}
			// trace could have altered array, so lookup the var again.
			notArray = false;
			retArray = Var.lookupVar(interp, varName, null, 0, null, false, false);
			if (retArray != null) {
				var = retArray[0];
				array = retArray[1];
			}
			if ((var == null) || !var.isVarArray() || var.isVarUndefined()) {
				notArray = true;
			}
		}

		switch (index) {
		case OPT_ANYMORE: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName searchId");
			}
			if (notArray) {
				errorNotArray(interp, objv[2].toString());
			}

			if (var.sidVec == null) {
				errorIllegalSearchId(interp, objv[2].toString(), objv[3].toString());
			}

			Iterator iter = var.getSearch(objv[3].toString());
			if (iter == null) {
				errorIllegalSearchId(interp, objv[2].toString(), objv[3].toString());
			}

			if (iter.hasNext()) {
				interp.setResult(true);
			} else {
				interp.setResult(false);
			}
			break;
		}
		case OPT_DONESEARCH: {

			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName searchId");
			}
			if (notArray) {
				errorNotArray(interp, objv[2].toString());
			}

			boolean rmOK = true;
			if (var.sidVec != null) {
				rmOK = (var.removeSearch(objv[3].toString()));
			}
			if ((var.sidVec == null) || !rmOK) {
				errorIllegalSearchId(interp, objv[2].toString(), objv[3].toString());
			}
			break;
		}
		case OPT_EXISTS: {

			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName");
			}
			interp.setResult(!notArray);
			break;
		}
		case OPT_GET: {
			// Due to the differences in the hashtable implementation
			// from the Tcl core and Java, the output will be rearranged.
			// This is not a negative side effect, however, test results
			// will differ.

			if ((objv.length != 3) && (objv.length != 4)) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName ?pattern?");
			}
			if (notArray) {
				return;
			}

			String pattern = null;
			if (objv.length == 4) {
				pattern = objv[3].toString();
			}

			TclObject tobj = TclList.newInstance();
			String arrayName = objv[2].toString();

			/*
			 * Go through each key in the hash table. If there is a pattern,
			 * test for a match. Separate the collection of keys from values, in
			 * case the reading of a value triggers a read trace that modifies
			 * the array
			 */
			ArrayList<String> keysToReturn = new ArrayList<>();

			for (String key : var.getArrayMap().keySet()) {
				if (pattern != null && !Util.stringMatch(key, pattern)) {
					continue;
				}
				keysToReturn.add(key);
			}
			for (String key : keysToReturn) {
				Var var2 = var.getArrayMap().get(key);
				if (var2 == null || var2.isVarUndefined())
					continue;
				String strValue;
				try {
					strValue = interp.getVar(arrayName, key, 0).toString();
					TclList.append(interp, tobj, TclString.newInstance(key));
					TclList.append(interp, tobj, TclString.newInstance(strValue));
				} catch (TclException e) {
					// If our array doesn't exist anymore, throw an exception.
					// But if it's just an element missing, just go on
					retArray = Var.lookupVar(interp, varName, null, 0, null, false, false);
					if (retArray != null) {
						var = retArray[0];
						array = retArray[1];
					}
					if ((var == null) || !var.isVarArray() || var.isVarUndefined()) {
						throw e;
					}
				}
			}
			interp.setResult(tobj);
			break;
		}
		case OPT_NAMES: {

			if ((objv.length != 3) && (objv.length != 4)) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName ?pattern?");
			}
			if (notArray) {
				return;
			}

			String pattern = null;
			if (objv.length == 4) {
				pattern = objv[3].toString();
			}

			Map<String, Var> table = var.getArrayMap();
			TclObject tobj = TclList.newInstance();
			String key;

			// Go through each key in the hash table. If there is a
			// pattern, test for a match. Each valid key and its value
			// is written into sbuf, which is returned.

			for (Map.Entry<String, Var> stringVarEntry : table.entrySet()) {
				Map.Entry entry = (Map.Entry) stringVarEntry;
				key = (String) entry.getKey();
				Var elem = (Var) entry.getValue();
				if (!elem.isVarUndefined()) {
					if (pattern != null) {
						if (!Util.stringMatch(key, pattern)) {
							continue;
						}
					}
					TclList.append(interp, tobj, TclString.newInstance(key));
				}
			}
			interp.setResult(tobj);
			break;
		}
		case OPT_NEXTELEMENT: {

			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName searchId");
			}
			if (notArray) {
				errorNotArray(interp, objv[2].toString());
			}

			if (var.sidVec == null) {
				errorIllegalSearchId(interp, objv[2].toString(), objv[3].toString());
			}

			Iterator iter = var.getSearch(objv[3].toString());
			if (iter == null) {
				errorIllegalSearchId(interp, objv[2].toString(), objv[3].toString());
			}
			if (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
				Var elem = (Var) entry.getValue();

				if (!elem.isVarUndefined()) {
					interp.setResult(key);
				} else {
					interp.setResult("");
				}
			}
			break;
		}
		case OPT_SET: {

			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName list");
			}
			int size = TclList.getLength(interp, objv[3]);
			if (size % 2 != 0) {
				throw new TclException(interp, "list must have an even number of elements");
			}

			int i;
			String name1 = objv[2].toString();
			String name2, strValue;

			/*
			 * verify that name1 is not an array. Var.lookupVar can does this,
			 * but requires a part2
			 */
			if (name1.endsWith(")") && name1.contains("(")) {
				throw new TclVarException(interp, name1, null, "set", "variable isn't array");
			}

			/*
			 * Create the array if it does not exists; this is required if there
			 * are 0 elements to create: array set arrayname {}
			 */
			Var.lookupVar(interp, name1, "", 0, "set", true, false);

			// Set each of the array variable names in the interp

			for (i = 0; i < size; i++) {
				name2 = TclList.index(interp, objv[3], i++).toString();
				strValue = TclList.index(interp, objv[3], i).toString();
				interp.setVar(name1, name2, TclString.newInstance(strValue), 0);
			}
			break;
		}
		case OPT_SIZE: {

			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName");
			}
			if (notArray) {
				interp.setResult(0);
			} else {
				Map<String, Var> table = var.getArrayMap();
				int size = 0;

				for (Map.Entry<String, Var> stringVarEntry : table.entrySet()) {
					Map.Entry entry = (Map.Entry) stringVarEntry;
					String key = (String) entry.getKey();
					Var elem = (Var) entry.getValue();
					if (!elem.isVarUndefined()) {
						size++;
					}
				}
				interp.setResult(size);
			}
			break;
		}
		case OPT_STARTSEARCH: {

			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName");
			}
			if (notArray) {
				errorNotArray(interp, objv[2].toString());
			}

			if (var.sidVec == null) {
				var.sidVec = new ArrayList();
			}

			// Create a SearchId Object:
			// To create a new SearchId object, a unique string
			// identifier needs to be composed and we need to
			// create an Iterator of the array keys. The
			// unique string identifier is created from three
			// strings:
			//
			// "s-" is the default prefix
			// "i" is a unique number that is 1+ the greatest
			// SearchId index currently on the ArrayVar.
			// "name" is the name of the array
			//
			// Once the SearchId string is created we construct a
			// new SearchId object using the string and the
			// Iterator. From now on the string is used to
			// uniquely identify the SearchId object.

			int i = var.getNextIndex();
			String s = "s-" + i + "-" + objv[2].toString();
			Map table = var.getArrayMap();
			Iterator iter = table.entrySet().iterator();
			var.sidVec.add(new SearchId(iter, s, i));
			interp.setResult(s);
			break;
		}
		case OPT_UNSET: {
			String pattern;
			String name;

			if ((objv.length != 3) && (objv.length != 4)) {
				throw new TclNumArgsException(interp, 2, objv, "arrayName ?pattern?");
			}
			if (notArray) {
				return;
			}
			if (objv.length == 3) {
				// When no pattern is given, just unset the whole array

				interp.unsetVar(objv[2], 0);
			} else {
				pattern = objv[3].toString();
				Map table = var.getArrayMap();

				for (Iterator iter = table.entrySet().iterator(); iter.hasNext();) {
					Map.Entry entry = (Map.Entry) iter.next();
					name = (String) entry.getKey();
					Var elem = (Var) entry.getValue();
					if (elem.isVarUndefined()) {
						continue;
					}
					if (Util.stringMatch(name, pattern)) {
						interp.unsetVar(varName, name, 0);
						// Reset iterator in case unset
						// modified the table.
						iter = table.entrySet().iterator();
					}
				}
			}
			break;
		}
		}
	}

	/**
	 * Error meassage thrown when an invalid identifier is used to access an
	 * array.
	 * 
	 * @param interp
	 *            currrent interpreter.
	 * @param String
	 *            var is the string representation of the variable that was
	 *            passed in.
	 */

	private static void errorNotArray(Interp interp, String var) throws TclException {
		throw new TclException(interp, "\"" + var + "\" isn't an array");
	}

	/**
	 * Error message thrown when an invalid SearchId is used. The string used to
	 * reference the SearchId is parced to determine the reason for the failure.
	 * 
	 * @param interp
	 *            currrent interpreter.
	 * @param String
	 *            sid is the string represenation of the SearchId that was
	 *            passed in.
	 */

	static void errorIllegalSearchId(Interp interp, String varName, String sid) throws TclException {

		int val = validSearchId(sid.toCharArray(), varName);

		if (val == 1) {
			throw new TclException(interp, "couldn't find search \"" + sid + "\"");
		} else if (val == 0) {
			throw new TclException(interp, "illegal search identifier \"" + sid + "\"");
		} else {
			throw new TclException(interp, "search identifier \"" + sid + "\" isn't for variable \"" + varName + "\"");
		}
	}

	/**
	 * A valid SearchId is represented by the format s-#-arrayName. If the
	 * SearchId string does not match this format than it is illegal, else we
	 * cannot find it. This method is used by the ErrorIllegalSearchId method to
	 * determine the type of error message.
	 * 
	 * @param char pattern[] is the string use dto identify the SearchId
	 * @return 1 if its a valid searchID; 0 if it is not a valid searchId, but
	 *         it is for the array, -1 if it is not a valid searchId and NOT for
	 *         the array.
	 */

	private static int validSearchId(char pattern[], String varName) {
		int i;

		if ((pattern[0] != 's') || (pattern[1] != '-') || (pattern[2] < '0') || (pattern[2] > '9')) {
			return 0;
		}
		for (i = 3; (i < pattern.length && pattern[i] != '-'); i++) {
			if (pattern[i] < '0' || pattern[i] > '9') {
				return 0;
			}
		}
		if (++i >= pattern.length) {
			return 0;
		}
		if (varName.equals(new String(pattern, i, (pattern.length - i)))) {
			return 1;
		} else {
			return -1;

		}
	}
}
