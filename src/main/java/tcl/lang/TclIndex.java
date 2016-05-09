/*
 * TclIndex.java
 *
 *	This file implements objects of type "index".  This object type
 *	is used to lookup a keyword in a table of valid values and cache
 *	the index of the matching entry.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclIndex.java,v 1.6 2005/10/11 20:03:23 mdejong Exp $
 */

package tcl.lang;

public class TclIndex implements InternalRep {

	/**
	 * The variable slots for this object.
	 */
	private int index;

	/**
	 * Table of valid options.
	 */

	private String[] table;

	/**
	 * Construct a TclIndex representation with the given index & table.
	 */
	private TclIndex(int i, String[] tab) {
		index = i;
		table = tab;
	}

	/**
	 * Returns a dupilcate of the current object.
	 * 
	 */
	public InternalRep duplicate() {
		return new TclIndex(index, table);
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
		return table[index];
	}

	/**
	 * Tcl_GetIndexFromObj -> get
	 * 
	 * Gets the index into the table of the object. Generate an error it it
	 * doesn't occur. This also converts the object to an index which should
	 * catch the lookup for speed improvement.
	 * 
	 * @param interp
	 *            the interperter or null
	 * @param tobj
	 *            the object to operate on.
	 * @param table the list of commands
	 * @param msg used as part of any error messages
	 * @param flags may be TCL.EXACT.
	 */

	public static int get(Interp interp, TclObject tobj, String[] table,
			String msg, int flags) throws TclException {
		InternalRep rep = tobj.getInternalRep();

		if (rep instanceof TclIndex) {
			if (((TclIndex) rep).table == table) {
				return ((TclIndex) rep).index;
			}
		}

		String str = tobj.toString();
		int strLen = str.length();
		int tableLen = table.length;
		int index = -1;
		int numAbbrev = 0;

		checking: {
			if (strLen > 0) {

				for (int i = 0; i < tableLen; i++) {
					String option = table[i];

					if (((flags & TCL.EXACT) == TCL.EXACT)
							&& (option.length() != strLen)) {
						continue;
					}
					if (option.equals(str)) {
						// Found an exact match already. Return it.

						index = i;
						break checking;
					}
					if (option.startsWith(str)) {
						numAbbrev++;
						index = i;
					}
				}
			}
			if (numAbbrev != 1) {
				StringBuilder sbuf = new StringBuilder();
				if (numAbbrev > 1) {
					sbuf.append("ambiguous ");
				} else {
					sbuf.append("bad ");
				}
				sbuf.append(msg);
				sbuf.append(" \"");
				sbuf.append(str);
				sbuf.append("\"");
				sbuf.append(": must be ");
				sbuf.append(table[0]);
				for (int i = 1; i < tableLen; i++) {
					if (i == (tableLen - 1)) {
						sbuf.append((i > 1) ? ", or " : " or ");
					} else {
						sbuf.append(", ");
					}
					sbuf.append(table[i]);
				}
				throw new TclException(interp, sbuf.toString());
			}
		}

		// Create a new index object.

		tobj.setInternalRep(new TclIndex(index, table));
		return index;
	}

	/**
	 * Invoked only when testing the TclIndex implementation in TestObjCmd.java
	 */
	void testUpdateIndex(int index) {
		this.index = index;
	}

}
