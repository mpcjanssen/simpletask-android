/*
 * StrtodResult.java --
 *
 *	Stores the result of the Util.strtod() method.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: StrtodResult.java,v 1.3 2005/11/19 01:09:06 mdejong Exp $
 *
 */

package tcl.lang;

/*
 * This class stores the result of the Util.strtod() method.
 */

public class StrtodResult {

	/*
	 * If the conversion is successful, errno = 0;
	 * 
	 * If the number cannot be converted to a valid signed 64-bit double,
	 * contains the error code (TCL.DOUBLE_RANGE or TCL.UNVALID_DOUBLE).
	 */

	public int errno;

	/*
	 * If errno is 0, points to the character right after the number
	 */

	public int index;

	/*
	 * If errno is 0, contains the value of the number.
	 */

	public double value;

	// Update a StrtodResult. Note that there is typically
	// just one StrtodResult for each interp.

	void update(double v, // value for the value field.
			int i, // value for the index field.
			int e) // value for the errno field.
	{
		value = v;
		index = i;
		errno = e;
	}

} // end StrtodResult

