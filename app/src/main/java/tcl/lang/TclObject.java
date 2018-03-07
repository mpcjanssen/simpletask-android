/*
 * TclObject.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclObject.java,v 1.8 2006/06/24 00:30:42 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.Hashtable;

/**
 * This class extends TclObjectBase to implement the basic notion of an object
 * in Tcl.
 */

public final class TclObject extends TclObjectBase {

	static final boolean saveObjRecords = TclObjectBase.saveObjRecords;
	static Hashtable objRecordMap = TclObjectBase.objRecordMap;

	/**
	 * Creates a TclObject with the given InternalRep. This method should be
	 * called only by an InternalRep implementation.
	 * 
	 * @param rep
	 *            the initial InternalRep for this object.
	 */
	public TclObject(final InternalRep rep) {
		super(rep);
	}

	/**
	 * Creates a TclObject with the given InternalRep and stringRep. This
	 * constructor is used by the TclString class only. No other place should
	 * call this constructor.
	 * 
	 * @param rep
	 *            the initial InternalRep for this object.
	 * @param s
	 *            the initial string rep for this object.
	 */
	protected TclObject(final TclString rep, final String s) {
		super(rep, s);
	}

	/**
	 * Creates a TclObject with the given integer value. This constructor is
	 * used by the TclInteger class only. No other place should call this
	 * constructor.
	 * 
	 * @param ivalue
	 *            the integer value
	 */
	protected TclObject(final long ivalue) {
		super(ivalue);
	}

	/**
	 * Tcl_IncrRefCount -> preserve
	 * 
	 * Increments the refCount to indicate the caller's intent to preserve the
	 * value of this object. Each preserve() call must be matched by a
	 * corresponding release() call. This method is Jacl specific and is
	 * intended to be easily inlined in calling code.
	 * 
	 * @exception TclRuntimeError
	 *                if the object has already been deallocated.
	 */
	public final void preserve() {
		if (refCount < 0) {
			throw new TclRuntimeError(DEALLOCATED_MSG);
		}
		refCount++;
	}

	/**
	 * Tcl_DecrRefCount -> release
	 * 
	 * Decrements the refCount to indicate that the caller is no longer
	 * interested in the value of this object. If the refCount reaches 0, the
	 * object will be deallocated. This method is Jacl specific an is intended
	 * to be easily inlined in calling code.
	 * 
	 * @exception TclRuntimeError
	 *                if the object has already been deallocated.
	 */
	public final void release() {
		if (--refCount <= 0) {
			disposeObject();
		}
	}

	/**
	 * Return a String that describes TclObject and internal rep type
	 * allocations and conversions. The string is in lines separated by
	 * newlines. The saveObjRecords needs to be set to true and Jacl recompiled
	 * for this method to return a useful value.
	 */

	public static String getObjRecords() {
		return TclObjectBase.getObjRecords();
	}

	/**
	 * Modify a "recycled" int value so that it contains a new int value. This
	 * optimized logic is like TclInteger.set() except that it does not invoke
	 * invalidateStringRep(). This method is only invoked from the Interp class
	 * when the TclObject is known to have a refCount of 1 or when the refCount
	 * is 2 but the interp result holds the other ref. An object with a refCount
	 * of 2 would normally raise an exception in invalidateStringRep, but this
	 * optimized case is worth it for this common case.
	 */

	final void setRecycledIntValue(long i) {
		if (validate) {
			if ((refCount != 1) && (refCount != 2)) {
				throw new TclRuntimeError("Invalid refCount " + refCount);
			}
		}

		if (internalRep != TclInteger.dummy) {
			setInternalRep(TclInteger.dummy);
		}
		ivalue = i;
		stringRep = null;
	}

	/**
	 * Modify a "recycled" double value so that it contains a new double value.
	 * This optimized logic is like TclDouble.set() except that it does not
	 * invoke invalidateStringRep(). This method is only invoked from the Interp
	 * class when the TclObject is known to have a refCount of 1 or when the
	 * refCount is 2 but the interp result holds the other ref. An object with a
	 * refCount of 2 would normally raise an exception in invalidateStringRep,
	 * but this optimized case is worth it for this common case.
	 */

	final void setRecycledDoubleValue(double d) {
		if (validate) {
			if ((refCount != 1) && (refCount != 2)) {
				throw new TclRuntimeError("Invalid refCount " + refCount);
			}
		}

		if (!isDoubleType()) {
			TclDouble.setRecycledInternalRep(this);
		}
		((TclDouble) internalRep).value = d;
		stringRep = null;
	}

	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		return (o instanceof TclObject && ((TclObject) o).toString().equals(toString()));
	}

	public int hashCode() {
		return toString().hashCode();
	}

}
