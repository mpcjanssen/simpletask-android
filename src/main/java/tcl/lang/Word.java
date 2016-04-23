/*
 * Word.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: Word.java,v 1.1.1.1 1998/10/14 21:09:21 cvsadmin Exp $
 *
 */

package tcl.lang;


/**
 * This class is used to store a word during the parsing of Tcl commands.
 */
class Word {
	StringBuffer sbuf;
	TclObject obj;

	/**
	 * Number of objects that have been concatenated into this word.
	 */
	int objCount;

	Word() {
		sbuf = null;
		obj = null;
		objCount = 0;
	}

	void append(TclObject o) {
		/*
		 * The object inside a word must be preserved. Otherwise code like the
		 * following will fail (prints out 2020 instead of 1020):
		 * 
		 * set a 10 puts $a[set a 20]
		 */

		if (sbuf != null) {
			sbuf.append(o.toString());
		} else if (obj != null) {
			sbuf = new StringBuffer(obj.toString());
			obj.release();
			obj = null;
			sbuf.append(o.toString());
		} else {
			obj = o;
			obj.preserve();
		}

		objCount++;
	}

	void append(char c) {
		if (sbuf != null) {
			sbuf.append(c);
		} else if (obj != null) {
			sbuf = new StringBuffer(obj.toString());
			obj.release();
			obj = null;
			sbuf.append(c);
		} else {
			sbuf = new StringBuffer();
			sbuf.append(c);
		}

		objCount++;
	}

	TclObject getTclObject() {
		if (sbuf != null) {
			obj = TclString.newInstance(sbuf.toString());
			obj.preserve();
			return obj;
		} else if (obj != null) {
			return obj;
		} else {
			obj = TclString.newInstance("");
			obj.preserve();
			return obj;
		}
	}
}
