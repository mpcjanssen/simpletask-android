/*
 * JACL.java --
 *
 *	This class stores all the Jacl-specific package protected constants.
 *	The exact values should match those in tcl.h.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: JACL.java,v 1.1.1.1 1998/10/14 21:09:21 cvsadmin Exp $
 *
 */

package tcl.lang;

/*
 * This class holds all the Jacl-specific package protected constants.
 */

public class JACL {

	/*
	 * Platform constants. PLATFORM is not final because we may change it for
	 * testing purposes only.
	 */

	public static final int PLATFORM_UNIX = 0;
	public static final int PLATFORM_WINDOWS = 1;
	public static final int PLATFORM_MAC = 2;
	public static int PLATFORM = Util.getActualPlatform();

} // end JACL class
