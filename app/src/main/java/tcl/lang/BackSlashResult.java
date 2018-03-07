/*
 * BackSlashResult.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: BackSlashResult.java,v 1.2 2005/10/26 19:17:08 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * Encapsulates the replacement for a backslash
 * in the parser
 *
 */
public class BackSlashResult {
	/**
	 * character to replace backslash sequence with
	 */
	public char c;
	/**
	 * script index that follows backslash sequence
	 */
	public int nextIndex;
	public boolean isWordSep;
	/**
	 * Number of characters in the backslash sequence
	 */
	int count;

	/**
	 * @param c character ro replace the backslash sequence with
	 * @param nextIndex index of char following bs sequence in script
	 * @param count number of chars in backslash sequence
	 */
	BackSlashResult(char c, int nextIndex, int count) {
		this.c = c;
		this.nextIndex = nextIndex;
		this.isWordSep = false;
		this.count = count;
	}

	BackSlashResult(char c, int nextIndex, boolean isWordSep, int count) {
		this.c = c;
		this.nextIndex = nextIndex;
		this.isWordSep = false;
		this.count = count;
	}
}
