/*
 * TclInteger.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclInteger.java,v 1.17 2006/06/13 06:52:47 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the integer object type in Tcl.
 */

public class TclInteger implements InternalRep {

	// The int value for a TclInteger type is stored
	// in the TclObject instance. The TclObject API
	// requires that every TclObject have a non-null
	// internal rep, so this dummy value is used
	// for every TclObject with an int value.
	// The dummy value also maintains compatibility with
	// old code that might check for an internal
	// rep via:
	//
	// if (tobj.getInternalRep() instanceof TclInteger) {...}

	final static TclInteger dummy = new TclInteger();

	// Extra debug checking
	private static final boolean validate = false;

	private TclInteger() {
	}

	/**
	 * Should never be invoked.
	 */

	public InternalRep duplicate() {
		throw new TclRuntimeError(
				"TclInteger.duplicate() should not be invoked");
	}

	/**
	 * Implement this no-op for the InternalRep interface.
	 */

	public void dispose() {
	}

	/**
	 * Should never be invoked.
	 */
	public String toString() {
		throw new TclRuntimeError("TclInteger.toString() should not be invoked");
	}

	/**
	 * Tcl_NewIntObj -> TclInteger.newInstance
	 * 
	 * Creates a new instance of a TclObject with a TclInteger internal
	 * representation.
	 * 
	 * @param i
	 *            initial value of the integer object.
	 * @return the TclObject with the given integer value.
	 */

	public static TclObject newInstance(long i) {
		return new TclObject(i);
	}
	
	
	/**
	 * SetIntFromAny -> TclInteger.setIntegerFromAny
	 * 
	 * Called to convert the other object's internal rep to this type.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to convert to use the representation provided by
	 *            this class.
	 */

	private static void setIntegerFromAny(Interp interp, TclObject tobj)
			throws TclException {
		// Note that this method is never invoked when the
		// object is already an integer type. This method
		// does not check for a TclBoolean internal rep
		// since it would typically only be used only for
		// the "true" and "false" string values. An
		// int value like "1" or "0" that can be a boolean
		// value will not be converted to TclBoolean.
		//
		// This method also does not check for a TclDouble
		// internal rep since a double like "1.0" can't
		// be converted to an integer. An octal like
		// "040" could be parsed as both a double and
		// an integer, but the TclDouble module should
		// not allow conversion to TclDouble in that case.

		long ivalue = Util.getInt(interp, tobj.toString());
		tobj.setInternalRep(dummy);
		tobj.ivalue = ivalue;

		if (TclObject.saveObjRecords) {
			String key = "TclString -> TclInteger";
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
	 * Tcl_GetIntFromObj -> TclInteger.get
	 * 
	 * Returns the integer value of the object as a Java int.
	 * This method is @deprecated, because the internal representation
	 * is now long.  Use getLong() or getInt() instead.
	 *
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the object to operate on.
	 * @return the integer value of the object.  Use getLong() to return a Java long, or getInt() to return int.
	 */
	@Deprecated
	public static int get(final Interp interp, final TclObject tobj)
			throws TclException {
		return (int)getLong(interp, tobj);
	}
	
	/**
	 * Tcl_GetIntFromObj -> TclInteger.getValue
	 * 
	 * Returns the integer value of the object as a Java long.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the object to operate on.
	 * @return the integer value of the object.
	 * */
	public static long getLong(final Interp interp, final TclObject tobj)
		throws TclException {
		if (!tobj.isIntType()) {
			setIntegerFromAny(interp, tobj);
		}
		return tobj.ivalue;
	}

	/**
	 * Tcl_GetIntFromObj -> TclInteger.getValue
	 * 
	 * Returns the integer value of the object as a Java int.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the object to operate on.
	 * @return the integer value of the object as a Java int
	 * @throws TclException if value will exceed limits of a Java int
	 * */
	public static int getInt(final Interp interp, final TclObject tobj)
		throws TclException {
		long v = getLong(interp, tobj);
		if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
			throw new TclException(interp, "integer value too large to represent");
		}
		return (int)v;
	}

	/**
	 * Tests whether TclInteger.getLong() will return a valid Java long value
	 * 
	 * @param interp current interpreter, may be null
	 * @param tobj The TclObject to be tested
	 * @return true if getLong() will return a long.  false if object cannot be converted to a
	 * TclInteger, or if it is out of range for a long.
	 */
	public static boolean isWithinLongRange(final Interp interp, final TclObject tobj) {
		if (! tobj.isIntType()) {
			try {
				setIntegerFromAny(null, tobj);
			} catch (TclException e) {
				return false;
			}
		}
		return true; // always withing a long range if it can be converted to Integer
	}
	
	/**
	 * Tests whether TclInteger.getInt() will return a valid Java int value
	 * 
	 * @param interp current interpreturn, may be null
	 * @param tobj The TclObject to be tested
	 * @return true if getInt() will return an int; false if object cannot be converted to a TclInteger
	 * or if it is out of range for an int
	 */
	public static boolean isWithinIntRange(final Interp interp, final TclObject tobj) {
		return (isWithinLongRange(interp, tobj)
				&& tobj.ivalue >= Integer.MIN_VALUE
				&& tobj.ivalue <= Integer.MAX_VALUE);
	}
	
	/**
	 * Changes the integer value of the object.
	 * 
	 * @param tobj
	 *            the object to operate on.
	 * @param i
	 *            the new integer value.
	 */
	public static void set(TclObject tobj, long i) {
		if (!tobj.isIntType()) {
			// Change the internal rep if not an integer.
			// Note that this method does not reparse
			// an int value from the string rep.
			tobj.setInternalRep(dummy);
		}
		tobj.invalidateStringRep();
		tobj.ivalue = i;
	}

	
	/**
	 * Increments the integer value of the object by the given amount. One could
	 * implement this same operation by calling get() and then set(), this
	 * method provides an optimized implementation. This method is not public
	 * since it will only be invoked by the incr command.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the object to operate on.
	 * @param incrAmount
	 *            amount to increment
	 */
	public static void incr(final Interp interp, final TclObject tobj,
			final long incrAmount) throws TclException {
		if (!tobj.isIntType()) {
			setIntegerFromAny(interp, tobj);
		}
		tobj.invalidateStringRep();
		tobj.ivalue += incrAmount;
	}
	/**
	 * This special helper method is used only by the Expression module. This
	 * method will change the internal rep to a TclInteger with the passed in
	 * int value. This method does not invalidate the string rep since the
	 * object's value is not being changed.
	 * 
	 * @param tobj
	 *            the object to operate on.
	 * @param i
	 *            the new int value.
	 */

	static void exprSetInternalRep(final TclObject tobj, final long i) {
		if (validate) {

			// Double check that the internal rep is not
			// already of type TclInteger.

			if (tobj.isIntType()) {
				throw new TclRuntimeError(
						"exprSetInternalRep() called with object"
								+ " that is already of type TclInteger");
			}

			// Double check that the new int value and the
			// string rep would parse to the same integer.

			long i2;
			try {
				i2 = Util.getInt(null, tobj.toString());
			} catch (TclException te) {
				throw new TclRuntimeError(
						"exprSetInternalRep() called with int"
								+ " value that could not be parsed from the string");
			}
			if (i != i2) {
				throw new TclRuntimeError(
						"exprSetInternalRep() called with int value " + i
								+ " that does not match parsed int value " + i2
								+ ", parsed from str \"" + tobj.toString()
								+ "\"");
			}
		}

		tobj.setInternalRep(dummy);
		tobj.ivalue = i;
	}

}
