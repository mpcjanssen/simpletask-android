/*
 * ParseResult.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ParseResult.java,v 1.3 2003/01/09 02:15:39 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class stores a single word that's generated inside the Tcl parser inside
 * the Interp class.
 */
public class ParseResult {

	/**
	 * The value of a parse operation. For calls to Interp.intEval(), this
	 * variable is the same as interp.m_result. The ref count has been
	 * incremented, so the user will need to explicitly invoke release() to drop
	 * the ref.
	 */
	public TclObject value;

	/**
	 * Points to the next character to be parsed.
	 */
	public int nextIndex;

	/**
	 * Create an empty parsed word.
	 */
	ParseResult() {
		value = TclString.newInstance("");
		value.preserve();
	}

	ParseResult(String s, int ni) {
		value = TclString.newInstance(s);
		value.preserve();
		nextIndex = ni;
	}

	/**
	 * Assume that the caller has already preserve()'ed the TclObject.
	 */
	public ParseResult(TclObject o, int ni) {
		value = o;
		nextIndex = ni;
	}

	ParseResult(StringBuffer sbuf, int ni) {
		value = TclString.newInstance(sbuf.toString());
		value.preserve();
		nextIndex = ni;
	}

	public void release() {
		value.release();
	}
}
