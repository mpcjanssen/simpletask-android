/* 
 * EventuallyFreed.java --
 *
 *	This class makes sure that certain objects
 *	aren't disposed when there are nested procedures that
 *	depend on their existence.
 *
 * Copyright (c) 1991-1994 The Regents of the University of California.
 * Copyright (c) 1994-1998 Sun Microsystems, Inc.
 * Copyright (c) 2000 Christian Krone.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: EventuallyFreed.java,v 1.3 2006/08/03 23:24:02 mdejong Exp $
 */

package tcl.lang;

abstract class EventuallyFreed {

	// Number of preserve() calls in effect for this object.

	int refCount = 0;

	// True means dispose() was called while a preserve()
	// call was in effect, so the object must be disposed
	// when refCount becomes zero.

	boolean mustFree = false;

	// Procedure to call to dispose.

	abstract void eventuallyDispose();

	// Set to true when tracking down tricky refCount issues

	final boolean debug = false;

	/**
	 *----------------------------------------------------------------------
	 * 
	 * Tcl_Preserve -> preserve
	 * 
	 * This method is used by another method to declare its interest in this
	 * particular object, so that the object will not be disposed until a
	 * matching call to release() has been made.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Information is retained so that the object will not be
	 * disposed until at least the matching call to release().
	 * 
	 *----------------------------------------------------------------------
	 */

	public void preserve() {
		// Just increment its reference count.

		refCount++;

		if (debug) {
			System.out.println("Incremented refCount to " + refCount + " for "
					+ this);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_Release -> release
	 * 
	 * This method is called to cancel a previous call to preserve(), thereby
	 * allowing an object to be disposed (if no one else cares about it).
	 * 
	 * Results: None.
	 * 
	 * Side effects: If dispose() has been called for this object, and if no
	 * other call to preserve() is still in effect, this object is disposed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void release() {
		refCount--;

		if (debug) {
			System.out.println("Decremented refCount to " + refCount + " for "
					+ this);
		}

		if (refCount == 0) {
			if (mustFree) {
				if (debug) {
					System.out.println("Invoking subclass dispose()");
				}

				dispose();
			}
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_EventuallyFree -> dispose
	 * 
	 * Dispose an object, unless a call to preserve() is in effect for that
	 * object. In this case, defer the disposal until all calls to preserve()
	 * have been undone by matching calls to release().
	 * 
	 * Results: None.
	 * 
	 * Side effects: The object may be disposed by calling eventuallyDispose().
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void dispose() {
		if (debug) {
			System.out.println("EventuallyFreed.dispose() for " + this);
		}

		// See if there is a reference for this pointer. If so, set its
		// "mustFree" flag (the flag had better not be set already!).

		if (refCount >= 1) {
			if (mustFree) {
				throw new TclRuntimeError("eventuallyDispose() called twice");
			}
			mustFree = true;

			if (debug) {
				System.out.println("set mustFree flag for " + this);
				System.out.println("refCount was " + refCount);
			}

			return;
		}

		// No reference for this block. Free it now.

		if (debug) {
			System.out
					.println("Invoking EventuallyFreed.eventuallyDispose() for "
							+ this);
		}

		eventuallyDispose();
	}

} // end EventuallyFreed

