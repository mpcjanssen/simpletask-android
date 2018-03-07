/*
 * TclDouble.java --
 *
 *	Implements the TclDouble internal object representation, as well
 *	variable traces for the tcl_precision variable.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclDouble.java,v 1.11 2006/06/24 00:30:42 mdejong Exp $
 *
 */

package tcl.lang;

/*
 * This class implements the double object type in Tcl.
 */

public class TclDouble implements InternalRep {

	/*
	 * Internal representation of a double value. This field is package scoped
	 * so that the expr module can quickly read the value.
	 */

	public double value;

	// Extra debug checking

	private final static boolean validate = false;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TclDouble --
	 * 
	 * Construct a TclDouble representation with the given double value.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private TclDouble(double d) // Initial value.
	{
		value = d;

		if (TclObject.saveObjRecords) {
			String key = "TclDouble";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TclDouble --
	 * 
	 * Construct a TclDouble representation with the initial value taken from
	 * the given string.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private TclDouble(Interp interp, // Current interpreter.
			String str) // String that contains the initial value.
			throws TclException // If error occurs in string conversion.
	{
		value = Util.getDouble(interp, str);

		if (TclObject.saveObjRecords) {
			String key = "TclDouble";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * duplicate --
	 * 
	 * Duplicate the current object.
	 * 
	 * Results: A dupilcate of the current object.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public InternalRep duplicate() {
		if (TclObject.saveObjRecords) {
			String key = "TclDouble.duplicate()";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}

		return new TclDouble(value);
	}

	/**
	 * Implement this no-op for the InternalRep interface.
	 */

	public void dispose() {
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * newInstance --
	 * 
	 * Creates a new instance of a TclObject with a TclDouble internal
	 * representation.
	 * 
	 * Results: The newly created TclObject.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static TclObject newInstance(double d) // Initial value.
	{
		return new TclObject(new TclDouble(d));
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * setDoubleFromAny --
	 * 
	 * Called to convert a TclObject's internal rep to TclDouble.
	 * 
	 * Results: None.
	 * 
	 * Side effects: When successful, the internal representation of tobj is
	 * changed to TclDouble, if it is not already so.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static void setDoubleFromAny(Interp interp, // Current interpreter.
														// May be null.
			TclObject tobj) // The object to convert.
			throws TclException // If error occurs in type conversion.
	// Error message will be left inside
	// the interp if it's not null.

	{
		// This method is only ever invoked from TclDouble.get().
		// This method will never be invoked when the internal
		// rep is already a TclDouble. This method will always
		// reparse a double from the string rep so that tricky
		// special cases like "040" are handled correctly.

		if (validate) {
			if (tobj.getInternalRep() instanceof TclDouble) {
				throw new TclRuntimeError("should not be TclDouble, was a "
						+ tobj.getInternalRep().getClass().getName());
			}
		}

		tobj.setInternalRep(new TclDouble(interp, tobj.toString()));

		if (TclObject.saveObjRecords) {
			String key = "TclString -> TclDouble";
			Integer num = (Integer) TclObject.objRecordMap.get(key);
			if (num == null) {
				num = 1;
			} else {
				num = num.intValue() + 1;
			}
			TclObject.objRecordMap.put(key, num);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * get --
	 * 
	 * Returns the double value of the object.
	 * 
	 * Results: The double value of the object.
	 * 
	 * Side effects: When successful, the internal representation of tobj is
	 * changed to TclDouble, if it is not already so.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static double get(Interp interp, // Current interpreter. May be null.
			TclObject tobj) // The object to query.
			throws TclException // If the object does not have a TclDouble
	// representation and a conversion fails.
	// Error message will be left inside
	// the interp if it's not null.
	{
		TclDouble tdouble;

		if (!tobj.isDoubleType()) {
			if (Util.isJacl()) {
				// Try to convert to TclDouble. If the string can't be
				// parsed as a double, then raise a TclException here.

				setDoubleFromAny(interp, tobj);
				double dval;
				tdouble = (TclDouble) tobj.getInternalRep();
				dval = tdouble.value;

				// The string can be parsed as a double, but if it
				// can also be parsed as an integer then we need
				// to convert the internal rep back to TclInteger.
				// This logic handles the special case of an octal
				// string like "040". The most common path through
				// this code is a normal double like "1.0", so this
				// code will only attempt a conversion to TclInteger
				// when the string looks like an integer. This logic
				// is tricky, but it leads to a speedup in performance
				// critical expr code since the double value from a
				// TclDouble can be used without having to check to
				// see if the double looks like an integer.

				if (Util.looksLikeInt(tobj.toString())) {
					try {
						long ival = TclInteger.getLong(null, tobj);

						// A tricky octal like "040" can be parsed as
						// the double 40.0 or the integer 32, return
						// the value parsed as a double and leave
						// the object with a TclInteger internal rep.

						return dval;
					} catch (TclException te) {
						throw new TclRuntimeError("looksLikeInt() is true, "
								+ "but TclInteger.get() failed for \""
								+ tobj.toString() + "\"");
					}
				}

				if (validate) {
					// Double check that we did not just create a TclDouble
					// that looks like an integer.

					InternalRep tmp = tobj.getInternalRep();
					if (!(tmp instanceof TclDouble)) {
						throw new TclRuntimeError("not a TclDouble, is a "
								+ tmp.getClass().getName());
					}
					String stmp = tobj.toString();
					if (Util.looksLikeInt(stmp)) {
						throw new TclRuntimeError("looks like an integer");
					}
				}
			} else {
				setDoubleFromAny(interp, tobj);
				tdouble = (TclDouble) tobj.getInternalRep();
			}
		} else {
			tdouble = (TclDouble) tobj.getInternalRep();
		}

		return tdouble.value;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * set --
	 * 
	 * Changes the double value of the object.
	 * 
	 * Results: None.
	 * 
	 * Side effects: The internal representation of tobj is changed to
	 * TclDouble, if it is not already so.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static void set(TclObject tobj, // The object to modify.
			double d) // The new value for the object.
	{
		tobj.invalidateStringRep();

		if (tobj.isDoubleType()) {
			TclDouble tdouble = (TclDouble) tobj.getInternalRep();
			tdouble.value = d;
		} else {
			tobj.setInternalRep(new TclDouble(d));
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * toString --
	 * 
	 * Called to query the string representation of the Tcl object. This method
	 * is called only by TclObject.toString() when TclObject.stringRep is null.
	 * 
	 * Results: Returns the string representation of the TclDouble object.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public String toString() {
		return Util.printDouble(value);
	}

	/**
	 * This special helper method is used only by the Expression module. This
	 * method will change the internal rep to a TclDouble with the passed in
	 * double value. This method does not invalidate the string rep since the
	 * object's value is not being changed.
	 * 
	 * @param tobj
	 *            the object to operate on.
	 * @param d
	 *            the new double value.
	 */
	static void exprSetInternalRep(TclObject tobj, double d) {
		if (validate) {

			// Double check that the internal rep is not
			// already of type TclDouble.

			InternalRep rep = tobj.getInternalRep();

			if (rep instanceof TclDouble) {
				throw new TclRuntimeError(
						"exprSetInternalRep() called with object"
								+ " that is already of type TclDouble");
			}

			// Double check that the new int value and the
			// string rep would parse to the same integer.

			double d2;
			try {
				d2 = Util.getDouble(null, tobj.toString());
			} catch (TclException te) {
				throw new TclRuntimeError(
						"exprSetInternalRep() called with double"
								+ " value that could not be parsed from the string");
			}
			if (d != d2) {
				throw new TclRuntimeError(
						"exprSetInternalRep() called with double value " + d
								+ " that does not match parsed double value "
								+ d2 + ", parsed from str \"" + tobj.toString()
								+ "\"");
			}

			// It should not be possible to parse the TclObject's string
			// rep as an integer since we know it is a double. An object
			// that could be parsed as either a double or an integer
			// should have been parsed as an integer.

			try {
				long ival = Util.getInt(null, tobj.toString());
				throw new TclRuntimeError(
						"should not be able to parse string rep as int: "
								+ tobj.toString());
			} catch (TclException e) {
				// No-op
			}
		}

		tobj.setInternalRep(new TclDouble(d));
	}

	// This method is used to set the internal rep for a recycled
	// object to TclDouble, in the edge case where it might have
	// been changed. This method exists only because the
	// TclDouble ctor can't be made package access without
	// changing signature regression tests.

	static void setRecycledInternalRep(TclObject tobj) {
		tobj.setInternalRep(new TclDouble(0.0));
	}

} // end TclDouble

