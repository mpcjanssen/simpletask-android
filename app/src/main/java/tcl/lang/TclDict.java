/*
 * TclDict.java
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

package tcl.lang;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * This class implements the dict object type in Tcl.
 */
public class TclDict implements InternalRep {

	/**
	 * Internal representation of a dict value (key -> value)
	 */
	private final Map map;

	/**
	 * Map of key values to actual keys. The actual TclObject key must be
	 * released when removed from the value map.  (key -> key)
	 */
	private final Map keymap;

	/**
	 * Create a new empty Tcl dict.
	 */
	private TclDict() {
		map = new LinkedHashMap();
		keymap = new LinkedHashMap();

		if (TclObject.saveObjRecords) {
			String key = "TclDict";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Create a new empty Tcl dict, with the hashmap pre-allocated to the given
	 * size.
	 * 
	 * @param size
	 *            the number of slots pre-allocated in the map.
	 */
	private TclDict(int size) {
		map = new LinkedHashMap(size);
		keymap = new LinkedHashMap(size);

		if (TclObject.saveObjRecords) {
			String key = "TclDict";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Called to free any storage for the type's internal rep.
	 */
	public void dispose() {
		// Release the objects associated with each key/value pair.
		for (Object o : map.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			TclObject key = (TclObject) entry.getKey();
			TclObject val = (TclObject) entry.getValue();
			key.release();
			val.release();
		}
	}

	/**
	 * Returns a duplicate of the current object.
	 */
	public InternalRep duplicate() {
		final int size = map.size();
		TclDict newDict = new TclDict(size);

		for (Object o : map.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			TclObject key = (TclObject) entry.getKey();
			TclObject value = (TclObject) entry.getValue();
			key.preserve();
			value.preserve();
			newDict.map.put(key, value);
			newDict.keymap.put(key, key);
		}

		if (TclObject.saveObjRecords) {
			String key = "TclDict.duplicate()";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}

		return newDict;
	}

	/**
	 * Called to query the string representation of the Tcl object. This method
	 * is called only by TclObject.toString() when TclObject.stringRep is null.
	 * 
	 * @return the string representation of the Tcl object.
	 */
	public String toString() {
		final int size = map.size();
		if (size == 0) {
			return "";
		}
		int est = size * 8;

		StringBuffer sbuf = new StringBuffer((est > 64) ? est : 64);
		try {
			for (Object o : map.entrySet()) {
				Map.Entry entry = (Map.Entry) o;
				Object key = entry.getKey();
				Object val = entry.getValue();
				if (key != null) {
					Util.appendElement(null, sbuf, key.toString());
				} else {
					Util.appendElement(null, sbuf, "");
				}
				if (val != null) {
					Util.appendElement(null, sbuf, val.toString());
				} else {
					Util.appendElement(null, sbuf, "");
				}
			}
		} catch (TclException e) {
			throw new TclRuntimeError("unexpected TclException: " + e);
		}

		return sbuf.toString();
	}

	/**
	 * Creates a new instance of a TclObject with a TclDict internal rep.
	 * 
	 * @return the TclObject with the given list value.
	 */
	public static TclObject newInstance() {
		return new TclObject(new TclDict());
	}

	/**
	 * Called to convert the other object's internal rep to dict.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to convert to use the Dict internal rep.
	 * @exception TclException
	 *                if the object doesn't contain a valid dict.
	 */
	private static void setDictFromAny(Interp interp, TclObject tobj) throws TclException {
		TclDict tdict = new TclDict();
		splitDict(interp, tdict.map, tdict.keymap, tobj.toString());
		tobj.setInternalRep(tdict);

		if (TclObject.saveObjRecords) {
			String key = "TclString -> TclDict";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Splits a dict (in string rep) up into its constituent entries.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param map
	 *            store the dict elements into this Map.
	 * @param s
	 *            the string to convert into a dict.
	 * @exception TclException
	 *                if the object doesn't contain a valid dict.
	 */
	private static final void splitDict(Interp interp, Map map, Map keymap, String s) throws TclException {
		int len = s.length();
		int i = 0;
		FindElemResult res = new FindElemResult();

		// A dictionary is a Tcl list with an even number of elements (key
		// value ...).
		while (i < len) {
			TclObject key = null, val = null;
			if (!Util.findElement(interp, s, i, len, res)) {
				break;
			} else {
				key = TclString.newInstance(res.elem);
			}
			i = res.elemEnd;
			if (!Util.findElement(interp, s, i, len, res)) {
				throw new TclException(interp, "missing value to go with key");
			} else {
				val = TclString.newInstance(res.elem);
			}
			key.preserve();
			val.preserve();
			map.put(key, val);
			keymap.put(key, key);
			i = res.elemEnd;
		}
	}

	/**
	 * Tcl_DictObjGet.
	 */
	public static final TclObject get(Interp interp, TclObject dict, TclObject key) throws TclException {
		if (!(dict.getInternalRep() instanceof TclDict)) {
			setDictFromAny(interp, dict);
		}
		TclDict ir = (TclDict) dict.getInternalRep();
		return (TclObject) ir.map.get(key);
	}

	/**
	 * Tcl_DictObjPut.
	 */
	public static final void put(Interp interp, TclObject dict, TclObject key, TclObject value) throws TclException {
		if (dict.isShared()) {
			throw new TclRuntimeError("TclDict.put() called with shared object");
		}
		if (!(dict.getInternalRep() instanceof TclDict)) {
			setDictFromAny(interp, dict);
		}

		TclDict ir = (TclDict) dict.getInternalRep();
		TclObject oldValue = (TclObject) ir.map.remove(key);

		// TODO: This can probably be optimised (e.g. if
		// oldValue.equals(value)).
		key.preserve();
		value.preserve();
		if (oldValue != null) {
			oldValue.release();
			TclObject oldKey = (TclObject) ir.keymap.remove(key);
			oldKey.release();
		}
		ir.map.put(key, value);
		ir.keymap.put(key, key);
		dict.invalidateStringRep();
	}

	/**
	 * Tcl_DictObjRemove.
	 */
	public static final void remove(Interp interp, TclObject dict, TclObject key) throws TclException {
		if (dict.isShared()) {
			throw new TclRuntimeError("TclDict.remove() called with shared object");
		}
		if (!(dict.getInternalRep() instanceof TclDict)) {
			setDictFromAny(interp, dict);
		}
		dict.invalidateStringRep();

		TclDict ir = (TclDict) dict.getInternalRep();
		TclObject val = (TclObject) ir.map.remove(key);
		if (val != null) {
			val.release();
			TclObject oldKey = (TclObject) ir.keymap.remove(key);
			oldKey.release();
		}
	}

	/**
	 * Tcl_DictObjSize.
	 */
	public static final int size(Interp interp, TclObject dict) throws TclException {
		if (!(dict.getInternalRep() instanceof TclDict)) {
			setDictFromAny(interp, dict);
		}
		TclDict ir = (TclDict) dict.getInternalRep();
		return ir.map.size();
	}

	/**
	 * An interface to traverse (visit) each key-value pair in a Tcl dictionary
	 * object. This is an implementation of the standard Visitor Pattern, with
	 * added parameters for an <code>Interp</code> object and some arbitrary
	 * threaded state.
	 */
	public static interface Visitor {
		/**
		 * Called for each element in a TclDict.
		 */
		public Object visit(Interp interp, Object accum, TclObject key, TclObject value) throws TclException;
	}

	/**
	 * An internal iterator methods for traversing the elements of a Tcl
	 * dictionary object. The TclDict.Visitor object is invoked for each
	 * key/value pair in the dictionary. The <code>accum</code> argument can be
	 * used to thread some arbitrary state through the traversal, and in this
	 * way forms a <em>fold</em> operation over the dictionary, familiar from
	 * functional programming languages. The Visitor object can throw a
	 * <code>TclException</code> to terminate the loop early either with an
	 * error (which will propagate) or with a <code>TCL.BREAK</code> exception
	 * which will cause termination of the loop with the current accumulator
	 * value. This method will take care of cleaning up resources associated
	 * with the loop in the event of any exception &mdash; which is the reason
	 * this is structured as an internal iterator rather than a more usual (in
	 * Java) external iterator.
	 * 
	 * Equivalent in functionality to the
	 * <code>Tcl_DictObjFirst/Next/Done</code> C functions.
	 * 
	 * @param interp
	 *            A Tcl interpreter for reporting errors.
	 * @param accum
	 *            Arbitrary state to thread through the traversal.
	 * @param dict
	 *            The Tcl dictionary object.
	 * @param body
	 *            The Visitor object to invoke for each key-value pair.
	 * @return The final accumulator value.
	 * @throws TclException
	 *             if the visitor object throws one.
	 */
	public static final Object foreach(Interp interp, Object accum, TclObject dict, TclDict.Visitor body)
			throws TclException {
		if (!(dict.getInternalRep() instanceof TclDict)) {
			setDictFromAny(interp, dict);
		}
		dict.preserve();
		TclDict ir = (TclDict) dict.getInternalRep();
		// We need to preserve each element of the dictionary until the
		// iteration is complete, to ensure that the elements of the dict
		// aren't released before we have finished iterating. This can
		// happen e.g. if the dict is shimmered to a list (which would
		// release() all the elements).
		for (Object o : ir.map.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			TclObject key = (TclObject) entry.getKey();
			TclObject val = (TclObject) entry.getValue();
			key.preserve();
			val.preserve();
		}
		// Now, iterate through each element invoking the Visitor callback
		// for each one.
		Iterator elements = ir.map.entrySet().iterator();
		try {

			while (elements.hasNext()) {
				Map.Entry entry = (Map.Entry) elements.next();
				TclObject key = (TclObject) entry.getKey();
				TclObject val = (TclObject) entry.getValue();
				try {
					accum = body.visit(interp, accum, key, val);
				} catch (TclException e) {
					if (e.getCompletionCode() == TCL.BREAK) {
						break;
					} else if (e.getCompletionCode() == TCL.CONTINUE) {
						// Treat just as TCL.OK
						continue;
					} else {
						throw e;
					}
				}
			}
		} finally {
			// Release all elements again...
			elements = ir.map.entrySet().iterator();
			while (elements.hasNext()) {
				Map.Entry entry = (Map.Entry) elements.next();
				TclObject key = (TclObject) entry.getKey();
				TclObject val = (TclObject) entry.getValue();
				key.release();
				val.release();
			}
			dict.release();
		}
		return accum;
	}

	/**
	 * Tcl_DictObjPutKeyList.
	 */
	public static final void putKeyList(Interp interp, TclObject dict, TclObject[] keys, int start, int length,
			TclObject value) throws TclException {
		int end = start + length - 1;
		TclObject current = dict;
		if (dict.isShared()) {
			throw new TclRuntimeError("TclDict.putKeyList() called with shared object");
		}
		for (int i = start; i < end; ++i) {
			if (!(current.getInternalRep() instanceof TclDict)) {
				setDictFromAny(interp, current);
			}
			TclDict ir = (TclDict) current.getInternalRep();
			current.invalidateStringRep();
			TclObject next = (TclObject) ir.map.get(keys[i]);
			if (next == null) {
				// No mapping for this key: create a new one
				next = TclDict.newInstance();
				next.preserve();
				keys[i].preserve();
				ir.map.put(keys[i], next);
				ir.keymap.put(keys[i], keys[i]);
			} else if (next.isShared()) {
				// If the nested dict is shared then we need to take a copy
				// of it and store the fresh copy back in the containing
				// dict. This ensures that we have a chain of unshared
				// dictionary objects from the root dictionary up to the
				// leaf node.
				next = next.duplicate();
				next.preserve();
				keys[i].preserve();
				ir.map.put(keys[i], next);
				ir.keymap.put(keys[i], keys[i]);
			}
			// next is now a valid TclObject with refcount == 1
			current = next;
		}
		// current now points at the final dict object which we need to
		// update the key for.
		if (!(current.getInternalRep() instanceof TclDict)) {
			setDictFromAny(interp, current);
		}
		TclDict ir = (TclDict) current.getInternalRep();
		TclObject oldValue = (TclObject) ir.map.remove(keys[end]);
		if (oldValue != null) {
			oldValue.release();
			TclObject oldKey = (TclObject) ir.keymap.remove(keys[end]);
			oldKey.release();
		}
		keys[end].preserve();
		value.preserve();
		ir.map.put(keys[end], value);
		ir.keymap.put(keys[end], keys[end]);
		current.invalidateStringRep();
	}

	/**
	 * Tcl_DictObjRemoveKeyList.
	 */
	public static final void removeKeyList(Interp interp, TclObject dict, TclObject[] keys, int start, int length)
			throws TclException {
		int end = start + length - 1;
		TclObject current = dict;
		if (dict.isShared()) {
			throw new TclRuntimeError("TclDict.removeKeyList() called with shared object");
		}
		for (int i = start; i < end; ++i) {
			if (!(current.getInternalRep() instanceof TclDict)) {
				setDictFromAny(interp, current);
			}
			TclDict ir = (TclDict) current.getInternalRep();
			current.invalidateStringRep();
			TclObject next = (TclObject) ir.map.get(keys[i]);
			if (next == null) {
				throw new TclException(interp, "key \"" + keys[i].toString() + "\" not known in dictionary");
			} else if (next.isShared()) {
				// If the nested dict is shared then we need to take a copy
				// of it and store the fresh copy back in the containing
				// dict. This ensures that we have a chain of unshared
				// dictionary objects from the root dictionary up to the
				// leaf node.
				next = next.duplicate();
				next.preserve();
				keys[i].preserve();
				ir.map.put(keys[i], next);
				ir.keymap.put(keys[i], keys[i]);
			}
			// next is now a valid TclObject with refcount == 1
			current = next;
		}
		// current now points at the final dict object which we need to
		// remove the key for.
		if (!(current.getInternalRep() instanceof TclDict)) {
			setDictFromAny(interp, current);
		}
		TclDict ir = (TclDict) current.getInternalRep();
		TclObject oldValue = (TclObject) ir.map.remove(keys[end]);
		if (oldValue != null) {
			oldValue.release();
			TclObject oldKey = (TclObject) ir.keymap.remove(keys[end]);
			oldKey.release();
		}
		current.invalidateStringRep();
	}

	/**
	 * Appends the given strings to the given key in the given dictionary
	 * object.
	 */
	public static final void appendToKey(Interp interp, TclObject dict, TclObject key, TclObject[] objv, int start,
			int end) throws TclException {
		if (dict.isShared()) {
			throw new TclRuntimeError("TclDict.append() called with shared object");
		}
		if (!(dict.getInternalRep() instanceof TclDict)) {
			setDictFromAny(interp, dict);
		}

		TclDict ir = (TclDict) dict.getInternalRep();
		TclObject val = (TclObject) ir.map.get(key);
		if (val == null) {
			val = TclString.newInstance("");
			ir.keymap.put(key, key);
			key.preserve();
		} else if (val.isShared()) {
			val = val.duplicate();
		}
		for (int i = start; i < end; ++i) {
			TclString.append(val, objv[i].toString());
		}
		val.preserve();
		ir.map.put(key, val);
		dict.invalidateStringRep();
	}
}
