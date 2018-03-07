/* 
 * CharPointer.java --
 *
 *	Used in the Parser, this class implements the functionality
 * 	of a C character pointer.  CharPointers referencing the same
 *	script share a reference to one array, while maintaining there
 * 	own current index into the array.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: CharPointer.java,v 1.5 2005/10/19 23:37:38 mdejong Exp $
 */

package tcl.lang;

/**
*	Used in the Parser, this class implements the functionality
* 	of a C character pointer.  CharPointers referencing the same
*	script share a reference to one array, while maintaining there
* 	own current index into the array.
*/
public class CharPointer {

	/**
	 *  A string of characters.
	 */
	char[] array;

	/**
	 *  The current index into the array.
	 */

	int index;

	/**
	 * Default initialization.
	 */
	CharPointer() {
		this.array = null;
		this.index = -1;
	}

	/**
	 * Make a "copy" of the argument. This is used when the index of the
	 * original CharPointer shouldn't change.
	 */

	CharPointer(CharPointer c) {
		this.array = c.array;
		this.index = c.index;
	}

	/**
	 * Create an array of chars that is one char more than the length of str.
	 * This is used to store \0 after the last char in the string without
	 * causing exceptions.
	 */

	CharPointer(String str) {
		int len = str.length();
		this.array = new char[len + 1];
		str.getChars(0, len, this.array, 0);
		this.array[len] = '\0';
		this.index = 0;
	}

	/**
	 * Used to map C style '*ptr' into Java.
	 * 
	 * @return   character at the current index
	 */

	char charAt() {
		return (array[index]);
	}

	/**
	 * charAt -- Used to map C style 'ptr[x]' into Java.
	 * 
	 * @param x offset from current index of character to return
	 * @return character at the current index plus some value.
	 */
	char charAt(int x) {
		return (array[index + x]);
	}

	/**
	 * Since a '\0' char is stored at the end of the script the true length of
	 * the string is one less than the length of array.
	 * 
	 * @return The true size of the string.
	*/

	int length() {
		return (array.length - 1);
	}

	/**
	 * Get the entire string held in this CharPointer's array.
	 * 
	 * @return A String used for debug.
	 */
	public String toString() {
		return new String(array, 0, array.length - 1);
	}
} // end CharPointer
