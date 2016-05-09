/*
 * TclList.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclString.java,v 1.12 2006/06/08 07:44:51 mdejong Exp $
 *
 */

package tcl.lang;

// This class implements the string object type in Tcl.

public class TclString implements InternalRep {

	// This dummy field is used as the internal rep for every
	// TclString that has not been modified via an append
	// operation. The most common case is for a TclString
	// to be created but never be appended to. This field
	// makes it possible to avoid allocating an internal
	// rep instance until a string is actually modified.

	private static TclString dummy = new TclString();

	// Used to perform "append" operations. After an append op,
	// sbuf.toString() will contain the latest value of the string and
	// tobj.stringRep will be set to null. This field is not private
	// since it will need to be accessed directly by Jacl's IO code.

	StringBuffer sbuf;

	private TclString() {
		sbuf = null;

		if (TclObject.saveObjRecords) {
			String key = "TclString";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	private TclString(StringBuffer sb) {
		sbuf = sb;

		if (TclObject.saveObjRecords) {
			String key = "TclString";
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
	 * Returns a dupilcate of the current object.
	 * 
	 */

	public InternalRep duplicate() {
		if (TclObject.saveObjRecords) {
			String key = "TclString.duplicate()";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}

		return dummy;
	}

	/**
	 * Return the internal StringBuffer. Used by TclInputStream as a shortcut.
	 * 
	 * @return sbuf, the internal StringBuffer.
	 */
	public StringBuffer getSbuf() {
		return sbuf;
	}

	/**
	 * Implement this no-op for the InternalRep interface.
	 */

	public void dispose() {
	}

	/**
	 * Called to query the string representation of the Tcl object. This method
	 * is called only by TclObject.toString() when TclObject.stringRep is null.
	 * 
	 * @return the string representation of the Tcl object.
	 */
	public String toString() {
		if (sbuf == null) {
			return "";
		} else {
			return sbuf.toString();
		}
	}

	/**
	 * Create a new TclObject that has a string representation with the given
	 * string value.
	 */
	public static TclObject newInstance(String str) {
		return new TclObject(dummy, str);
	}

	/**
	 * Create a new TclObject that makes use of the given StringBuffer object.
	 * The passed in StringBuffer should not be modified after it is passed to
	 * this method.
	 */
	public static TclObject newInstance(StringBuffer sb) {
		return new TclObject(new TclString(sb));
	}

	public static final TclObject newInstance(Object o) {
		return newInstance(o.toString());
	}

	/**
	 * Create a TclObject with an internal TclString representation whose
	 * initial value is a string with the single character.
	 * 
	 * @param c
	 *            initial value of the string.
	 */

	public static final TclObject newInstance(char c) {
		char charArray[] = new char[1];
		charArray[0] = c;
		return newInstance(new String(charArray));
	}

	/**
	 * Called to convert the other object's internal rep to string.
	 * 
	 * @param tobj
	 *            the TclObject to convert to use the TclString internal rep.
	 */
	private static void setStringFromAny(TclObject tobj) {
		// Create string rep if object did not have one already

		tobj.toString();

		// Change the type of the object to TclString.

		tobj.setInternalRep(dummy);

		if (TclObject.saveObjRecords) {
			String key = "String -> TclString";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/*
	 * public static String get(TclObject tobj) {;}
	 * 
	 * There is no "get" class method for TclString representations. Use
	 * tobj.toString() instead.
	 */

	/**
	 * Appends a string to a TclObject object. This method is equivalent to
	 * Tcl_AppendToObj() in Tcl 8.0.
	 * 
	 * @param tobj
	 *            the TclObject to append a string to.
	 * @param string
	 *            the string to append to the object.
	 */
	public static final void append(TclObject tobj, String string) {
		if (!tobj.isStringType()) {
			setStringFromAny(tobj);
		}

		TclString tstr = (TclString) tobj.getInternalRep();
		if (tstr == dummy) {
			tstr = new TclString();
			tobj.setInternalRep(tstr);
		}
		if (tstr.sbuf == null) {
			tstr.sbuf = new StringBuffer(tobj.toString());
		}
		tobj.invalidateStringRep();
		tstr.sbuf.append(string);
	}

	/**
	 * Appends an array of characters to a TclObject Object.
	 * Tcl_AppendUnicodeToObj() in Tcl 8.0.
	 * 
	 * @param tobj
	 *            the TclObject to append a string to.
	 * @param charArr
	 *            array of characters.
	 * @param offset
	 *            index of first character to append.
	 * @param length
	 *            number of characters to append.
	 */
	public static final void append(TclObject tobj, char[] charArr, int offset,
			int length) {
		if (!tobj.isStringType()) {
			setStringFromAny(tobj);
		}

		TclString tstr = (TclString) tobj.getInternalRep();
		if (tstr == dummy) {
			tstr = new TclString();
			tobj.setInternalRep(tstr);
		}
		if (tstr.sbuf == null) {
			tstr.sbuf = new StringBuffer(tobj.toString());
		}
		tobj.invalidateStringRep();
		tstr.sbuf.append(charArr, offset, length);
	}

	/**
	 * Appends a TclObject to a TclObject. This method is equivalent to
	 * Tcl_AppendToObj() in Tcl 8.0.
	 * 
	 * The type of the TclObject will be a TclString that contains the string
	 * value: tobj.toString() + tobj2.toString();
	 */
	public static final void append(TclObject tobj, TclObject tobj2) {
		append(tobj, tobj2.toString());
	}

	/**
	 * Appends the String values of multiple TclObject's to a TclObject. This is
	 * an optimized implementation that will measure the length of each string
	 * and expand the capacity as needed to limit reallocations.
	 * 
	 * @param tobj
	 *            the TclObject to append elements to.
	 * @param objv
	 *            array containing elements to append.
	 * @param startIdx
	 *            index to start appending values from
	 * @param endIdx
	 *            index to stop appending values at
	 */

	public static final void append(TclObject tobj, TclObject[] objv,
			final int startIdx, final int endIdx) {
		if (!tobj.isStringType()) {
			setStringFromAny(tobj);
		}

		TclString tstr = (TclString) tobj.getInternalRep();
		if (tstr == dummy) {
			tstr = new TclString();
			tobj.setInternalRep(tstr);
		}
		if (tstr.sbuf == null) {
			tstr.sbuf = new StringBuffer(tobj.toString());
		}
		StringBuffer sb = tstr.sbuf;
		int currentLen = tstr.sbuf.length();

		tobj.invalidateStringRep();

		for (int i = 0; i < endIdx; i++) {
			currentLen += objv[i].toString().length();
		}
		// Large enough to holds all bytes, plus a little extra
		if (currentLen > (1024 * 10)) {
			currentLen += (currentLen / 10);
		} else {
			currentLen += (currentLen / 4);
		}
		sb.ensureCapacity(currentLen);
		for (int i = 0; i < endIdx; i++) {
			sb.append(objv[i].toString());
		}
	}

	/**
	 * This procedure clears out an existing TclObject so that it has a string
	 * representation of "". This method is used only in the IO layer.
	 */

	public static void empty(TclObject tobj) {
		if (!tobj.isStringType()) {
			setStringFromAny(tobj);
		}

		TclString tstr = (TclString) tobj.getInternalRep();
		if (tstr == dummy) {
			tstr = new TclString();
			tobj.setInternalRep(tstr);
		}
		if (tstr.sbuf == null) {
			tstr.sbuf = new StringBuffer();
		} else {
			tstr.sbuf.setLength(0);
		}
		tobj.invalidateStringRep();
	}
}
