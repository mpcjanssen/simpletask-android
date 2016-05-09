/*
 * DictCmd.java
 *
 * Copyright (c) 2006 Neil Madden.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id$
 *
 */

package tcl.lang.cmd;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclBoolean;
import tcl.lang.TclDict;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.Util;

/**
 * This class implements the built-in "dict" command in Tcl.
 */

public class DictCmd implements Command {

	static final private String options[] = { "append", "create", "exists", "filter", "for", "get", "incr", "info",
			"keys", "lappend", "merge", "remove", "replace", "set", "size", "unset", "update", "values", "with" };
	
	private final static Command[] subcommands = { new AppendCmd(), new CreateCmd(), new ExistsCmd(), new FilterCmd(),
			new ForCmd(), new GetCmd(), new IncrCmd(), new InfoCmd(), new KeysCmd(), new LappendCmd(), new MergeCmd(),
			new RemoveCmd(), new ReplaceCmd(), new SetCmd(), new SizeCmd(), new UnsetCmd(), new UpdateCmd(),
			new ValuesCmd(), new WithCmd() };

	/**
	 * This procedure is invoked to process the "dict" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
	 */
	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv, "subcommand ?arg ...?");
		}
		int index = TclIndex.get(interp, objv[1], options, "subcommand", 0);

		subcommands[index].cmdProc(interp, objv);
	}

	/*
	 * Sub-command implementations.
	 */

	/**
	 * dict append dictVar key ?string ..?.
	 * 
	 * This appends the given string (or strings) to the value that the given
	 * key maps to in the dictionary value contained in the variable, writing
	 * the resulting dictionary value back to that variable. Non-existent keys
	 * are treated as if they map to an empty string.
	 */
	private final static class AppendCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 4) {
				throw new TclNumArgsException(interp, 2, objv, "varName key ?value ...?");
			}
			TclObject dictObj = null;
			try {
				dictObj = interp.getVar(objv[2], 0);
			} catch (TclException _) {
				// Var doesn't exist: create it
				dictObj = TclDict.newInstance();
				dictObj.preserve();
			}
			if (objv.length == 4) {
				// Query only
				interp.setResult(dictObj);
				return;
			}
			if (dictObj.isShared()) {
				dictObj = dictObj.duplicate();
				dictObj.preserve();
			}
			TclObject key = objv[3];
			TclDict.appendToKey(interp, dictObj, key, objv, 4, objv.length);
			// Write the result back to the variable
			interp.setResult(interp.setVar(objv[2], dictObj, 0));
		}
	}

	/**
	 * dict create ?key value ...?.
	 * 
	 * Create a new dictionary that contains each of the key/value mappings
	 * listed as arguments (keys and values alternating, with each key being
	 * followed by its associated value).
	 */
	private final static class CreateCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if ((objv.length % 2) != 0) {
				throw new TclNumArgsException(interp, 2, objv, "?key value ...?");
			}
			TclObject dict = TclDict.newInstance();
			if (objv.length > 2) {
				for (int i = 2; i < objv.length; i += 2) {
					TclDict.put(interp, dict, objv[i], objv[i + 1]);
				}
			}
			interp.setResult(dict);
		}
	}

	/**
	 * dict exists dictionaryValue key ?key ...?.
	 * 
	 * This returns a boolean value indicating whether the given key (or path of
	 * keys through a set of nested dictionaries) exists in the given dictionary
	 * value. This returns a true value exactly when dict get on that path will
	 * succeed.
	 */
	private final static class ExistsCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 4) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary key ?key ...?");
			}
			TclObject dict = objv[2];
			for (int i = 3; i < objv.length; ++i) {
				dict = TclDict.get(interp, dict, objv[i]);
				if (dict == null) {
					interp.setResult(TclBoolean.newInstance(false));
					return;
				}
			}
			interp.setResult(TclBoolean.newInstance(true));
		}
	}

	/**
	 * dict filter dictionaryValue filterType arg ?arg ...?.
	 * 
	 * This takes a dictionary value and returns a new dictionary that contains
	 * just those key/value pairs that match the specified filter type (which
	 * may be abbreviated.) Supported filter types are:
	 * 
	 * <dl>
	 * <dt>dict filter dictionaryValue <em>key</em> globPattern</dt>
	 * <dd>The key rule only matches those key/value pairs whose keys match the
	 * given pattern (in the style of string match.)</dd>
	 * <dt>dict filter dictionaryValue <em>script</em> {keyVar valueVar} script</dt>
	 * <dd>The script rule tests for matching by assigning the key to the keyVar
	 * and the value to the valueVar, and then evaluating the given script which
	 * should return a boolean value (with the key/value pair only being
	 * included in the result of the dict filter when a true value is returned.)
	 * Note that the first argument after the rule selection word is a
	 * two-element list. If the script returns with a condition of TCL.BREAK, no
	 * further key/value pairs are considered for inclusion in the resulting
	 * dictionary, and a condition of TCL.CONTINUE is equivalent to a false
	 * result. The order in which the key/value pairs are tested is undefined.</dd>
	 * <dt>dict filter dictionaryValue <em>value</em> globPattern</dt>
	 * <dd>The value rule only matches those key/value pairs whose values match
	 * the given pattern (in the style of string match.)</dd>
	 * </dl>
	 */
	private final static class FilterCmd implements Command {
		private final static String[] filterTypes = { "key", "script", "value" };
		private final static int FILTER_KEY = 0, FILTER_SCRIPT = 1, FILTER_VALUE = 2;

		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 4) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary filterType ...");
			}
			int index = TclIndex.get(interp, objv[3], filterTypes, "filterType", 0);
			switch (index) {
			case FILTER_KEY:
				if (objv.length != 5) {
					throw new TclNumArgsException(interp, 2, objv, "dictionary key globPattern");
				}
				filterKey(interp, objv);
				break;
			case FILTER_SCRIPT:
				if (objv.length != 6) {
					throw new TclNumArgsException(interp, 2, objv, "dictionary script {keyVar valueVar} filterScript");
				}
				filterScript(interp, objv);
				break;
			case FILTER_VALUE:
				if (objv.length != 5) {
					throw new TclNumArgsException(interp, 2, objv, "dictionary value globPattern");
				}
				filterValue(interp, objv);
				break;
			default:
				throw new TclRuntimeError("unexpected index");
			}
		}

		private void filterKey(Interp interp, TclObject[] objv) throws TclException {
			final TclObject srcDict = objv[2];
			final TclObject retDict = TclDict.newInstance();
			final String glob = objv[4].toString();
			TclDict.foreach(interp, null, srcDict, new TclDict.Visitor() {
				public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
					if (Util.stringMatch(key.toString(), glob)) {
						TclDict.put(interp, retDict, key, val);
					}
					return null;
				}
			});
			interp.setResult(retDict);
		}

		private void filterScript(Interp interp, TclObject[] objv) throws TclException {
			TclObject srcDict = objv[2];
			final TclObject retDict = TclDict.newInstance();
			final TclObject[] vars = TclList.getElements(interp, objv[4]);
			final TclObject script = objv[5];
			if (vars.length != 2) {
				throw new TclException(interp, "must have exactly two variable names");
			}
			TclDict.foreach(interp, null, srcDict, new TclDict.Visitor() {
				public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
					interp.setVar(vars[0], key, 0);
					interp.setVar(vars[1], val, 0);

					try {
						interp.eval(script, 0);
					} catch (TclException e) {
						if (e.getCompletionCode() == TCL.ERROR) {
							interp.addErrorInfo("\n    (\"dict filter\" script line " + interp.errorLine + ")");
						}
						throw e;
					}

					// If the script evaluated to true then store this
					// key/value pair in the result.
					if (TclBoolean.get(interp, interp.getResult())) {
						TclDict.put(interp, retDict, key, val);
					}
					return null;
				}
			});
			interp.setResult(retDict);
		}

		private void filterValue(Interp interp, TclObject[] objv) throws TclException {
			final TclObject srcDict = objv[2];
			final TclObject retDict = TclDict.newInstance();
			final String glob = objv[4].toString();
			TclDict.foreach(interp, null, srcDict, new TclDict.Visitor() {
				public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
					if (Util.stringMatch(val.toString(), glob)) {
						TclDict.put(interp, retDict, key, val);
					}
					return null;
				}
			});
			interp.setResult(retDict);
		}
	}

	/**
	 * dict for {keyVar valueVar} dictionaryValue body.
	 * 
	 * This command takes three arguments, the first a two-element list of
	 * variable names (for the key and value respectively of each mapping in the
	 * dictionary), the second the dictionary value to iterate across, and the
	 * third a script to be evaluated for each mapping with the key and value
	 * variables set appropriately (in the manner of foreach.) The result of the
	 * command is an empty string. If any evaluation of the body generates a
	 * TCL.BREAK result, no further pairs from the dictionary will be iterated
	 * over and the dict for command will terminate successfully immediately. If
	 * any evaluation of the body generates a TCL.CONTINUE result, this shall be
	 * treated exactly like a normal TCL.OK result. The order of iteration is
	 * undefined.
	 */
	private final static class ForCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length != 5) {
				throw new TclNumArgsException(interp, 2, objv, "{keyVar valueVar} dictionary script");
			}
			final TclObject[] vars = TclList.getElements(interp, objv[2]);
			final TclObject dict = objv[3];
			final TclObject script = objv[4];
			if (vars.length != 2) {
				throw new TclException(interp, "must have exactly two variable names");
			}

			TclDict.foreach(interp, null, dict, new TclDict.Visitor() {
				public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
					interp.setVar(vars[0], key, 0);
					interp.setVar(vars[1], val, 0);

					// Evaluate script
					try {
						interp.eval(script, 0);
					} catch (TclException e) {
						if (e.getCompletionCode() == TCL.ERROR) {
							interp.addErrorInfo("\n    (\"dict for\" body line " + interp.errorLine + ")");
						}
						throw e;
					}
					return null;
				}
			});
		}
	}

	/**
	 * dict get dictionaryValue ?key ...?.
	 * 
	 * Given a dictionary value (first argument) and a key (second argument),
	 * this will retrieve the value for that key. Where several keys are
	 * supplied, the behaviour of the command shall be as if the result of dict
	 * get $dictVal $key was passed as the first argument to dict get with the
	 * remaining arguments as second (and possibly subsequent) arguments. This
	 * facilitates lookups in nested dictionaries. For example, the following
	 * two commands are equivalent:
	 * 
	 * <pre>
	 * dict get $dict foo bar spong
	 * dict get [dict get [dict get $dict foo] bar] spong
	 * </pre>
	 * 
	 * If no keys are provided, dict would return a list containing pairs of
	 * elements in a manner similar to array get. That is, the first element of
	 * each pair would be the key and the second element would be the value for
	 * that key.
	 * 
	 * It is an error to attempt to retrieve a value for a key that is not
	 * present in the dictionary.
	 */
	private final static class GetCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 3) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary ?key key ...?");
			}
			TclObject dict = objv[2];
			if (objv.length == 3) {
				interp.setResult(dict);
				return;
			}
			for (int i = 3; i < objv.length; ++i) {
				dict = TclDict.get(interp, dict, objv[i]);
				if (dict == null) {
					throw new TclException(interp, "key \"" + objv[i].toString() + "\" not known in dictionary");
				}
			}
			interp.setResult(dict);
		}
	}

	/**
	 * dict incr dictionaryVariable key ?increment?.
	 * 
	 * This adds the given increment value (an integer that defaults to 1 if not
	 * specified) to the value that the given key maps to in the dictionary
	 * value contained in the given variable, writing the resulting dictionary
	 * value back to that variable. Non-existent keys are treated as if they map
	 * to 0. It is an error to increment a value for an existing key if that
	 * value is not an integer.
	 */
	private final static class IncrCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 4 || objv.length > 5) {
				throw new TclNumArgsException(interp, 2, objv, "varName key ?increment?");
			}
			TclObject dictObj = null;
			try {
				dictObj = interp.getVar(objv[2], 0);
			} catch (TclException _) {
				// Var doesn't exist: create it
				dictObj = TclDict.newInstance();
				dictObj.preserve();
			}
			TclObject key = objv[3];
			int increment = 1;
			if (objv.length == 5) {
				increment = TclInteger.get(interp, objv[4]);
			}
			if (dictObj.isShared()) {
				dictObj = dictObj.duplicate();
			}
			TclObject value = TclDict.get(interp, dictObj, key);
			if (value == null) {
				value = TclInteger.newInstance(0);
			}
			if (value.isShared()) {
				value = value.duplicate();
			}
			TclInteger.incr(interp, value, increment);
			TclDict.put(interp, dictObj, key, value);
			interp.setResult(interp.setVar(objv[2], dictObj, 0));
		}
	}

	/**
	 * dict info dictionaryValue.
	 * 
	 * This returns information (intended for display to people) about the given
	 * dictionary though the format of this data is dependent on the
	 * implementation of the dictionary. For dictionaries that are implemented
	 * by hash tables, it is expected that this will return the string produced
	 * by Tcl_HashStats, similar to array info.
	 */
	private final static class InfoCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary");
			}
			final StringBuffer buf = new StringBuffer();
			int size = TclDict.size(interp, objv[2]);
			buf.append("" + size + " entries in table\n");
			buf.append("Entries (and ref-counts):\n");
			TclDict.foreach(interp, null, objv[2], new TclDict.Visitor() {
				public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
					buf.append(key.toString() + "(" + key.getRefCount() + ") = " + val.toString() + "("
							+ val.getRefCount() + ")\n");
					return null;
				}
			});
			interp.setResult(buf.toString());
			// That's all we can get from Java's HashMap implementation
		}
	}

	/**
	 * dict keys dictionaryValue ?globPattern?.
	 * 
	 * Return a list of all keys in the given dictionary value. If a pattern is
	 * supplied, only those keys that match it (according to the rules of string
	 * match) will be returned. The returned keys will be in an arbitrary
	 * implementation-specific order, though where no pattern is supplied the
	 * i'th key returned by dict keys will be the key for the i'th value
	 * returned by dict values applied to the same dictionary value.
	 */
	private final static class KeysCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 3 || objv.length > 4) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary ?pattern?");
			}
			TclObject dict = objv[2];
			final String glob = objv.length == 4 ? objv[3].toString() : null;
			final TclObject result = TclList.newInstance();
			TclDict.foreach(interp, null, dict, new TclDict.Visitor() {
				public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
					if (glob == null || Util.stringMatch(key.toString(), glob)) {
						TclList.append(interp, result, key);
					}
					return null;
				}
			});
			interp.setResult(result);
		}
	}

	/**
	 * dict lappend dictionaryVariable key ?value ...?.
	 * 
	 * This appends the given items to the list value that the given key maps to
	 * in the dictionary value contained in the given variable, writing the
	 * resulting dictionary value back to that variable. Non-existent keys are
	 * treated as if they map to an empty list, and it is legal for there to be
	 * no items to append to the list. It is an error for the value that the key
	 * maps to to not be representable as a list.
	 */
	private final static class LappendCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 4) {
				throw new TclNumArgsException(interp, 2, objv, "varName key ?value ...?");
			}
			TclObject dictObj = null;
			try {
				dictObj = interp.getVar(objv[2], 0);
			} catch (TclException _) {
				// Var doesn't exist: create it
				dictObj = TclDict.newInstance();
				dictObj.preserve();
			}
			if (objv.length == 4) {
				// Query only
				interp.setResult(dictObj);
				return;
			}
			if (dictObj.isShared()) {
				dictObj = dictObj.duplicate();
			}
			TclObject key = objv[3];
			TclObject list = TclDict.get(interp, dictObj, key);
			if (list == null) {
				list = TclList.newInstance();
				list.preserve();
			} else if (list.isShared()) {
				list = list.duplicate();
				list.preserve();
			}
			for (int i = 4; i < objv.length; ++i) {
				TclList.append(interp, list, objv[i]);
			}
			TclDict.put(interp, dictObj, key, list);
			dictObj.invalidateStringRep();
			interp.setResult(interp.setVar(objv[2], dictObj, 0));
		}
	}

	/**
	 * dict merge ?dictionaryValue ...?.
	 * 
	 * Return a dictionary that contains the contents of each of the
	 * dictionaryValue arguments. Where two (or more) dictionaries contain a
	 * mapping for the same key, the resulting dictionary maps that key to the
	 * value according to the last dictionary on the command line containing a
	 * mapping for that key.
	 */
	private final static class MergeCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			final TclObject retDict = TclDict.newInstance();
			// Loop through each dictionary given and add all keys. This can
			// probably be optimised.
			for (int i = 2; i < objv.length; ++i) {
				TclDict.foreach(interp, null, objv[i], new TclDict.Visitor() {
					public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
						TclDict.put(interp, retDict, key, val);
						return null;
					}
				});
			}
			interp.setResult(retDict);
		}
	}

	/**
	 * dict remove dictionaryValue ?key ...?.
	 * 
	 * Return a new dictionary that is a copy of an old one passed in as first
	 * argument except without mappings for each of the keys listed. It is legal
	 * for there to be no keys to remove, and it also legal for any of the keys
	 * to be removed to not be present in the input dictionary in the first
	 * place.
	 */
	private final static class RemoveCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 3) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary ?key ...?");
			}
			TclObject dict = objv[2];
			if (dict.isShared()) {
				dict = dict.duplicate();
			}
			for (int i = 3; i < objv.length; ++i) {
				TclDict.remove(interp, dict, objv[i]);
			}
			interp.setResult(dict);
		}
	}

	/**
	 * dict replace dictionaryValue ?key value ...?.
	 * 
	 * Return a new dictionary that is a copy of an old one passed in as first
	 * argument except with some values different or some extra key/value pairs
	 * added. It is legal for this command to be called with no key/value pairs,
	 * but illegal for this command to be called with a key but no value.
	 */
	private final static class ReplaceCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 3 || (objv.length % 2) != 1) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary ?key value ...?");
			}
			TclObject dict = objv[2];
			if (dict.isShared()) {
				dict = dict.duplicate();
			}
			for (int i = 3; i < objv.length; i += 2) {
				TclDict.put(interp, dict, objv[i], objv[i + 1]);
			}
			interp.setResult(dict);
		}
	}

	/**
	 * dict set dictionaryVariable key ?key ...? value.
	 * 
	 * This operation takes the name of a variable containing a dictionary value
	 * and places an updated dictionary value in that variable containing a
	 * mapping from the given key to the given value. When multiple keys are
	 * present, this operation creates or updates a chain of nested
	 * dictionaries.
	 */
	private final static class SetCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 5) {
				throw new TclNumArgsException(interp, 2, objv, "varName key ?key ...? value");
			}
			TclObject dict = null;
			try {
				dict = interp.getVar(objv[2], 0);
			} catch (TclException _) {
				// Var doesn't exist: create it
				dict = TclDict.newInstance();
				dict.preserve();
			}
			if (dict.isShared()) {
				dict = dict.duplicate();
			}
			TclObject value = objv[objv.length - 1];
			TclDict.putKeyList(interp, dict, objv, 3, objv.length - 4, value);
			dict.invalidateStringRep();
			interp.setResult(interp.setVar(objv[2], dict, 0));
		}
	}

	/**
	 * dict size dictionaryValue.
	 * 
	 * Return the number of key/value mappings in the given dictionary value.
	 */
	private final static class SizeCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary");
			}
			interp.setResult(TclDict.size(interp, objv[2]));
		}
	}

	/**
	 * dict unset dictionaryVariable key ?key ...?.
	 * 
	 * This operation (the companion to dict set) takes the name of a variable
	 * containing a dictionary value and places an updated dictionary value in
	 * that variable that does not contain a mapping for the given key. Where
	 * multiple keys are present, this describes a path through nested
	 * dictionaries to the mapping to remove. At least one key must be
	 * specified, but the last key on the key-path need not exist. All other
	 * components on the path must exist.
	 */
	private final static class UnsetCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 4) {
				throw new TclNumArgsException(interp, 2, objv, "varName key ?key ...?");
			}
			TclObject dict = null;
			try {
				dict = interp.getVar(objv[2], 0);
			} catch (TclException _) {
				// Var doesn't exist: create it (yes, even unset creates the
				// var!)
				dict = TclDict.newInstance();
				dict.preserve();
			}
			if (dict.isShared()) {
				dict = dict.duplicate();
			}
			TclDict.removeKeyList(interp, dict, objv, 3, objv.length - 3);
			dict.invalidateStringRep();
			interp.setResult(interp.setVar(objv[2], dict, 0));
		}
	}

	/**
	 * dict update dictionaryVariable key varName ?key varName ...? body.
	 * 
	 * Execute the Tcl script in body with the value for each key (as found by
	 * reading the dictionary value in dictionaryVariable) mapped to the
	 * variable varName. There may be multiple key/varName pairs. If a key does
	 * not have a mapping, that corresponds to an unset varName. When body
	 * terminates, any changes made to the varNames is reflected back to the
	 * dictionary within dictionaryVariable (unless dictionaryVariable itself
	 * becomes unreadable, when all updates are silently discarded), even if the
	 * result of body is an error or some other kind of exceptional exit. The
	 * result of dict update is (unless some kind of error occurs) the result of
	 * the evaluation of body. Note that the mapping of values to variables does
	 * not use traces; changes to the dictionaryVariable's contents only happen
	 * when body terminates.
	 */
	private final static class UpdateCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 5 || (objv.length % 2) != 0) {
				throw new TclNumArgsException(interp, 2, objv, "varName key varName ?key varName ...? script");
			}
			TclObject dict = null;
			try {
				dict = interp.getVar(objv[2], 0);
			} catch (TclException _) {
				// Var doesn't exist: create it
				dict = TclDict.newInstance();
				dict.preserve();
			}
			// Set each variable
			for (int i = 3; i < objv.length - 1; i += 2) {
				TclObject val = TclDict.get(interp, dict, objv[i]);
				if (val != null) {
					interp.setVar(objv[i + 1], val, 0);
				} else {
					// Make sure the var is unset
					try {
						interp.unsetVar(objv[i + 1], 0);
					} catch (TclException _) {
					}
				}
			}
			TclObject script = objv[objv.length - 1];

			// Evaluate the script
			try {
				interp.eval(script, 0);
			} catch (TclException e) {
				if (e.getCompletionCode() == TCL.ERROR) {
					interp.addErrorInfo("\n    (body of \"dict update\")");
				}
				throw e;
			} finally {
				// Update the dictionary variable even if an exception
				// occurred (this is the behaviour of C-Tcl).

				// Update the variable with new key values: but only if it is
				// still readable.
				try {
					interp.getVar(objv[2], 0);
				} catch (TclException _) {
					// No longer readable - ignore
					interp.resetResult();
					return;
				}

				if (dict.isShared()) {
					dict = dict.duplicate();
				}
				for (int i = 3; i < objv.length - 1; i += 2) {
					TclObject newVal = null;
					try {
						newVal = interp.getVar(objv[i + 1], 0);
					} catch (TclException _) {
						// Var was unset
						newVal = null;
					}
					// Put new value back into dictionary
					if (newVal != null) {
						TclDict.put(interp, dict, objv[i], newVal);
					} else {
						// Variable was unset: remove key from dictionary
						TclDict.remove(interp, dict, objv[i]);
					}
				}
				interp.setResult(interp.setVar(objv[2], dict, 0));
			}
		}
	}

	/**
	 * dict values dictionaryValue ?globPattern?.
	 * 
	 * Return a list of all values in the given dictionary value. If a pattern
	 * is supplied, only those values that match it (according to the rules of
	 * string match) will be returned. The returned values will be in an
	 * arbitrary implementation-specific order, though where no pattern is
	 * supplied the i'th key returned by dict keys will be the key for the i'th
	 * value returned by dict values applied to the same dictionary value.
	 */
	private final static class ValuesCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 3 || objv.length > 4) {
				throw new TclNumArgsException(interp, 2, objv, "dictionary ?pattern?");
			}
			TclObject dict = objv[2];
			final String glob = objv.length == 4 ? objv[3].toString() : null;
			final TclObject result = TclList.newInstance();
			TclDict.foreach(interp, null, dict, new TclDict.Visitor() {
				public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
					if (glob == null || Util.stringMatch(val.toString(), glob)) {
						TclList.append(interp, result, val);
					}
					return null;
				}
			});
			interp.setResult(result);
		}
	}

	/**
	 * dict with dictionaryVariable ?key ...? body.
	 * 
	 * Execute the Tcl script in body with the value for each key in
	 * dictionaryVariable mapped (in a manner similarly to dict update) to a
	 * variable with the same name. Where one or more keys are available, these
	 * indicate a chain of nested dictionaries, with the innermost dictionary
	 * being the one opened out for the execution of body. As with dict update,
	 * making dictionaryVariable unreadable will make the updates to the
	 * dictionary be discarded, and this also happens if the contents of
	 * dictionaryVariable are adjusted so that the chain of dictionaries no
	 * longer exists. The result of dict with is (unless some kind of error
	 * occurs) the result of the evaluation of body. Note that the mapping of
	 * values to variables does not use traces; changes to the
	 * dictionaryVariable's contents only happen when body terminates.
	 */
	private final static class WithCmd implements Command {
		public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
			if (objv.length < 4) {
				throw new TclNumArgsException(interp, 2, objv, "dictVar ?key ...? script");
			}
			// We don't try and auto-create variables for dict with.
			TclObject dict = interp.getVar(objv[2], 0);
			TclObject script = objv[objv.length - 1];

			// Find inner-most dictionary
			TclObject current = dict;
			for (int i = 3; i < objv.length - 1; ++i) {
				current = TclDict.get(interp, current, objv[i]);
				if (current == null) {
					throw new TclException(interp, "key \"" + objv[i].toString() + "\" not known in dictionary");
				}
			}

			dict.preserve();

			final List keyList = new LinkedList();
			// Add each key as a new variable in the environment
			TclDict.foreach(interp, null, current, new TclDict.Visitor() {
				public Object visit(Interp interp, Object accum, TclObject key, TclObject val) throws TclException {
					interp.setVar(key, val, 0);
					keyList.add(key);
					return null;
				}
			});

			// Evaluate the script
			try {
				interp.eval(script, 0);
			} catch (TclException e) {
				if (e.getCompletionCode() == TCL.ERROR) {
					interp.addErrorInfo("\n    (body of \"dict with\")");
				}
				throw e;
			}
			// Write back the updated values - but only if the variable is
			// still readable
			try {
				interp.getVar(objv[2], 0);
			} catch (TclException _) {
				interp.resetResult();
				return;
			}
			final TclObject tmp = TclDict.newInstance();
			// Get new key values -- using keys that were in the
			// dictionary when we started.
			for (Object aKeyList : keyList) {
				TclObject key = (TclObject) aKeyList;
				TclObject val = null;
				try {
					val = interp.getVar(key, 0);
				} catch (TclException _) {
					val = null;
				}
				if (val != null) {
					// Write new version back into dictionary
					TclDict.put(interp, tmp, key, val);
				}
			}

			if (objv.length == 4) {
				// No nesting - just return the current dict
				dict = tmp;
			} else {
				// Set the new dictionary at the appropriate position
				if (dict.isShared()) {
					dict = dict.duplicate();
					dict.preserve();
				}
				// Find inner-most dictionary
				current = dict;
				for (int i = 3; i < objv.length - 2; ++i) {
					TclObject next = TclDict.get(interp, current, objv[i]);
					if (next == null) {
						// Not readable - so ignore
						interp.resetResult();
						return;
					}
					if (next.isShared()) {
						next = next.duplicate();
						next.preserve();
						TclDict.put(interp, current, objv[i], next);
					}
					current = next;
				}
				TclDict.put(interp, current, objv[objv.length - 2], tmp);
			}

			// Return the new dictionary
			interp.setResult(interp.setVar(objv[2], dict, 0));
		}
	}
}
