/*
 * InternalRep.java
 *
 *	This file contains the abstract class declaration for the
 *	internal representations of TclObjects.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: InternalRep.java,v 1.4 2000/10/29 06:00:42 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This is the interface for implementing internal representation of Tcl
 * objects. A class that implements InternalRep should define the following:
 * 
 * (1) the two abstract methods specified in this base class: dispose()
 * duplicate()
 * 
 * (2) The method toString()
 * 
 * (3) class method(s) newInstance() if appropriate
 * 
 * (4) class method set<Type>FromAny() if appropriate
 * 
 * (5) class method get() if appropriate
 */

public interface InternalRep {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * dispose --
	 * 
	 * Free any state associated with the object's internal rep. This method
	 * should not be invoked by user code.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Leaves the object in an unusable state.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void dispose();

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * duplicate --
	 * 
	 * Make a copy of an object's internal representation. This method should
	 * not be invoked by user code.
	 * 
	 * Results: Returns a newly allocated instance of the appropriate type.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public InternalRep duplicate();

} // end InternalRep

