/*
 * PackageNameException.java
 *
 * Copyright (c) 2006 Moses DeJong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: PackageNameException.java,v 1.1 2006/04/13 07:36:50 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This exception is thrown by the TclClassLoader when an attempt to load a
 * class from any package that starts with the java.* or tcl.* prefix is made.
 * Classes in these packages can be loaded with the system class loader, but not
 * the TclClassLoader.
 */

public class PackageNameException extends RuntimeException {
	String className;

	public PackageNameException(String msg, String className) {
		super(msg);
		this.className = className;
	}
}
