/*
 * TclObjectBase.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclObjectBase.java,v 1.18 2009/07/29 16:50:27 rszulgo Exp $
 *
 */

package tcl.lang;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class implements the basic notion of an "object" in Tcl. The fundamental
 * representation of an object is its string value. However, an object can also
 * have an internal representation, which is a "cached" reprsentation of this
 * object in another form. The type of the internal rep of Tcl objects can
 * mutate. This class provides the storage of the string rep and the internal
 * rep, as well as the facilities for mutating the internal rep. The Jacl or
 * TclBlend specific implementation of TclObject will extend this abstract base
 * class.
 */

abstract class TclObjectBase {

	// Internal representation of the object. A valid TclObject
	// will always have a non-null internal rep.

	protected InternalRep internalRep;

	// Reference count of this object. When 0 the object will be deallocated.

	protected int refCount;

	// String representation of the object.

	protected String stringRep;

	// Setting to true will enable a feature that keeps
	// track of TclObject allocation, internal rep allocation,
	// and transitions from one internal rep to another.

	static final boolean saveObjRecords = false;
	static Hashtable objRecordMap = (saveObjRecords ? new Hashtable() : null);

	// Only set this to true if running test code and
	// the user wants to run extra ref count checks.
	// Setting this to true will make key methods larger.

	static final boolean validate = false;
	
	protected final static String DEALLOCATED_MSG = "TclObject has been deallocated";

	/**
	 * The ivalue field is used for a TclObject that contains
	 * an integer value. This implementation uses less
	 * memory than the previous approach that stored an
	 * int value in a internal rep of type TclInteger.
	 * This implementation executes integer operations
	 * more quickly since instanceof and upcast operations
	 * are no longer needed in the critical execution path.
	 * The ivalue field is always set after a call to
	 * setInternalRep() in the TclInteger class.
	 */
	public long ivalue;

	/*
	 * Override of java.lang.Object#equals method. Needed in TclList#sort method
	 * to check if arraylist already contains the same TclObjects. We compare
	 * string representation of objects.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		// Not strictly necessary, but often a good optimization
		if (this == obj)
			return true;
		if ((obj == null) || (obj.getClass() != this.getClass()))
			return false;

		// object must be TclObjectBase at this point
		TclObjectBase tob = (TclObjectBase) obj;
		return (stringRep == tob.stringRep || (stringRep != null && stringRep
				.equals(tob.stringRep)));
	}

	/*
	 * Override of java.lang.Object#hashCode method. This method returns the
	 * hash code value for the object on which this method is invoked. This
	 * method returns the hash code value as an integer and is supported for the
	 * benefit of hashing based collection classes such as Hashtable, HashMap,
	 * HashSet etc.
	 * 
	 * This method must be overridden in every class that overrides the equals
	 * method.
	 * 
	 * Since the equals method compares string reps of objects, hashCode gets
	 * hashCodes of string reps.
	 * 
	 * @see java.lang.Object#hashCode(java.lang.Object)
	 */
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (null == stringRep ? 0 : stringRep.hashCode());
		return hash;

	}

	// Note that the isIntType() and isDoubleType()
	// methods are public because they need to be
	// invoked by TJC compiled code. User code
	// should not need to query the internal rep of
	// a TclObject before operating on it.

	/**
	 * @return true if the TclObject contains an int.
	 */
	public final boolean isIntType() {
		return (internalRep == TclInteger.dummy);
	}

	/**
	 *  @return true if the TclObject contains a TclString.
	 */
	public final boolean isStringType() {
		return (internalRep instanceof TclString);
	}

	/**
	 *@return true if the TclObject contains a TclDouble
	 */
	public final boolean isDoubleType() {
		return (internalRep instanceof TclDouble);
	}

	/**
	 * @return true if the TclObject contains a TclWideInteger.
	 * 
	 */
	public final boolean isWideIntType() {
		return isIntType();
	}

	/**
	 * @return true if the TclObject contains a TclList.
	 * 
	 */
	public final boolean isListType() {
		return (internalRep instanceof TclList);
	}
	
	/**
	 * @return true if the TclObject contains a TclByteArray
	 */
	public final boolean isByteArrayType() {
		return (internalRep instanceof TclByteArray);
	}

	/**
	 * Creates a TclObject with the given InternalRep. This method should be
	 * called only by an InternalRep implementation.
	 * 
	 * @param rep
	 *            the initial InternalRep for this object.
	 */
	protected TclObjectBase(final InternalRep rep) {
		if (validate) {
			if (rep == null) {
				throw new TclRuntimeError("null InternalRep");
			}
		}
		internalRep = rep;
		// ivalue = 0;
		// stringRep = null;
		// refCount = 0;

		if (validate) {
			if (internalRep == null) {
				throw new TclRuntimeError("null internalRep");
			}
			if (ivalue != 0) {
				throw new TclRuntimeError("non-zero ivalue");
			}
			if (stringRep != null) {
				throw new TclRuntimeError("non-null stringRep");
			}
			if (refCount != 0) {
				throw new TclRuntimeError("non-zero refCount");
			}
		}

		if (TclObjectBase.saveObjRecords) {
			String key = "TclObject";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Creates a TclObject with the given InternalRep and stringRep. This
	 * constructor is used by the TclString class only. No other code should
	 * call this constructor.
	 * 
	 * @param rep
	 *            the initial InternalRep for this object.
	 * @param s
	 *            the initial string rep for this object.
	 */
	protected TclObjectBase(final TclString rep, final String s) {
		if (validate) {
			if (rep == null) {
				throw new TclRuntimeError("null InternalRep");
			}
			if (s == null) {
				throw new TclRuntimeError("null String");
			}
		}
		internalRep = rep;
		// ivalue = 0;
		stringRep = s;
		// refCount = 0;

		if (validate) {
			if (internalRep == null) {
				throw new TclRuntimeError("null internalRep");
			}
			if (ivalue != 0) {
				throw new TclRuntimeError("non-zero ivalue");
			}
			if (stringRep == null) {
				throw new TclRuntimeError("null stringRep");
			}
			if (refCount != 0) {
				throw new TclRuntimeError("non-zero refCount");
			}
		}

		if (TclObjectBase.saveObjRecords) {
			String key = "TclObject";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Creates a TclObject with the given integer value. This constructor is
	 * used by the TclInteger class only. No other code should call this
	 * constructor.
	 * 
	 * @param ivalue
	 *            the integer value
	 */
	protected TclObjectBase(final long ivalue) {
		internalRep = TclInteger.dummy;
		this.ivalue = ivalue;
		// stringRep = null;
		// refCount = 0;

		if (validate) {
			if (internalRep == null) {
				throw new TclRuntimeError("null internalRep");
			}
			if (stringRep != null) {
				throw new TclRuntimeError("non-null stringRep");
			}
			if (refCount != 0) {
				throw new TclRuntimeError("non-zero refCount");
			}
		}

		if (TclObjectBase.saveObjRecords) {
			String key = "TclObject";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/**
	 * Returns the handle to the current internal rep. This method should be
	 * called only by an InternalRep implementation.
	 * 
	 * @return the handle to the current internal rep.
	 */
	public final InternalRep getInternalRep() {
		if (validate) {
			if (internalRep == null) {
				disposedError();
			}
		}

		return internalRep;
	}

	/**
	 * Change the internal rep of the object. The old internal rep will be
	 * deallocated as a result. This method should be called only by an
	 * InternalRep implementation.
	 * 
	 * @param rep
	 *            the new internal rep.
	 */
	public void setInternalRep(InternalRep rep) {
		if (internalRep == null) {
			disposedError();
		}
		if (rep == null) {
			throw new TclRuntimeError("null InternalRep");
		}
		if (rep == internalRep) {
			return;
		}

		// System.out.println("TclObject setInternalRep for \"" + stringRep +
		// "\"");
		// System.out.println("from \"" + internalRep.getClass().getName() +
		// "\" to \"" + rep.getClass().getName() + "\"");
		internalRep.dispose();
		internalRep = rep;
		ivalue = 0;
	}

	/**
	 * Returns the string representation of the object.
	 * 
	 * @return the string representation of the object.
	 */

	public final String toString() {
		if (stringRep == null) {
			// If this object has been deallocated, then
			// the stringRep and the internalRep would
			// have both been set to null. Generate
			// a specific error in this case.

			if (internalRep == null) {
				disposedError();
			}

			if (isIntType()) {
				stringRep = Long.toString(ivalue);
			} else {
				stringRep = internalRep.toString();
			}
		}
		return stringRep;
	}

	/**
	 * Sets the string representation of the object to null. Next time when
	 * toString() is called, getInternalRep().toString() will be called. This
	 * method should be called ONLY when an InternalRep is about to modify the
	 * value of a TclObject.
	 * 
	 * @exception TclRuntimeError
	 *                if object is not exclusively owned.
	 */
	public final void invalidateStringRep() throws TclRuntimeError {
		if (internalRep == null) {
			disposedError();
		}
		if (refCount > 1) {
			throw new TclRuntimeError("string representation of object \""
					+ toString() + "\" cannot be invalidated: refCount = "
					+ refCount);
		}
		stringRep = null;
	}

	/**
	 * Returns true if the TclObject is shared, false otherwise.
	 * 
	 */
	public final boolean isShared() {
		if (validate) {
			if (internalRep == null) {
				disposedError();
			}
		}

		return (refCount > 1);
	}

	/**
	 * Tcl_DuplicateObj -> duplicate
	 * 
	 * Duplicate a TclObject, this method provides the preferred means to deal
	 * with modification of a shared TclObject. It should be invoked in
	 * conjunction with isShared instead of using the deprecated takeExclusive
	 * method.
	 * 
	 * Example:
	 * 
	 * if (tobj.isShared()) { tobj = tobj.duplicate(); } TclString.append(tobj,
	 * "hello");
	 * 
	 * @return an TclObject with a refCount of 0.
	 */

	public final TclObject duplicate() {
		if (validate) {
			if (internalRep == null) {
				disposedError();
			}
		}
		if (isStringType() && (stringRep == null)) {
			stringRep = internalRep.toString();
		}
		TclObject newObj;
		if (isIntType()) {
			newObj = new TclObject(TclInteger.dummy);
			newObj.ivalue = ivalue;
		} else {
			newObj = new TclObject(internalRep.duplicate());
		}

		if (stringRep != null) {
			newObj.stringRep = stringRep;
		}
		// newObj.refCount = 0;
		return newObj;
	}

	/**
	 * @deprecated The takeExclusive method has been deprecated in favor of the
	 *             new duplicate() method. The takeExclusive method would modify
	 *             the ref count of the original object and return an object
	 *             with a ref count of 1 instead of 0. These two behaviors lead
	 *             to lots of useless duplication of objects that could be
	 *             modified directly. This method exists only for backwards
	 *             compatibility and will be removed at some point.
	 */

	public final TclObject takeExclusive() throws TclRuntimeError {
		if (internalRep == null) {
			disposedError();
		}
		if (refCount == 1) {
			return (TclObject) this;
		} else if (refCount > 1) {
			if (isStringType() && (stringRep == null)) {
				stringRep = internalRep.toString();
			}
			TclObject newObj;
			if (isIntType()) {
				newObj = new TclObject(TclInteger.dummy);
				newObj.ivalue = ivalue;
			} else {
				newObj = new TclObject(internalRep.duplicate());
			}

			newObj.stringRep = stringRep;
			newObj.refCount = 1;
			refCount--;
			return newObj;
		} else {
			throw new TclRuntimeError("takeExclusive() called on object \""
					+ toString() + "\" with: refCount = 0");
		}
	}

	/**
	 * Returns the refCount of this object.
	 * 
	 * @return refCount.
	 */
	public final int getRefCount() {
		return refCount;
	}

	/**
	 * Dispose of the TclObject when the refCount reaches 0.
	 * 
	 * @exception TclRuntimeError
	 *                if the object has already been deallocated.
	 */
	protected final void disposeObject() {
		if (internalRep == null) {
			throw new TclRuntimeError(DEALLOCATED_MSG);
		}
		internalRep.dispose();

		// Setting the internalRep to null means any further
		// use of the object will generate an error. Set the
		// refCount to -1 so that the preserve() method
		// can determine if the object has been deallocated
		// by looking only at the refCount.

		internalRep = null;
		stringRep = null;
		refCount = -1;
	}

	/**
	 * Raise a TclRuntimeError in the case where a TclObject was already
	 * disposed of because the last ref was released.
	 */
	protected final void disposedError() {
		throw new TclRuntimeError(DEALLOCATED_MSG);
	}

	/**
	 * Return a String that describes TclObject and internal rep type
	 * allocations and conversions. The string is in lines separated by
	 * newlines. The saveObjRecords needs to be set to true and Jacl recompiled
	 * for this method to return a useful value.
	 */

	static String getObjRecords() {
		if (TclObjectBase.saveObjRecords) {
			StringBuilder sb = new StringBuilder(64);
			for (Enumeration keys = TclObject.objRecordMap.keys(); keys
					.hasMoreElements();) {
				String key = (String) keys.nextElement();
				Integer num = (Integer) TclObject.objRecordMap.get(key);
				sb.append(key);
				sb.append(" ");
				sb.append(num.intValue());
				sb.append("\n");
			}
			TclObject.objRecordMap = new Hashtable();
			return sb.toString();
		} else {
			return "";
		}
	}

	// Return true if this TclObject has no string rep.
	// This could be the case with a "pure" integer
	// or double, or boolean, that was created from
	// a primitive value. Once the toString() method
	// has been invoked, this method will return true
	// unless the string rep is again invalidated.

	public final boolean hasNoStringRep() {
		return (stringRep == null);
	}
}
