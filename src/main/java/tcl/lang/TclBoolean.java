/*
 * TclBoolean.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclBoolean.java,v 1.9 2006/06/13 06:52:47 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the boolean object type in Tcl.
 */

public class TclBoolean implements InternalRep {
	/**
	 * Internal representations for all TclBooleans.
	 */

	private final static TclBoolean trueRep = new TclBoolean(true);
	private final static TclBoolean falseRep = new TclBoolean(false);

	private final boolean value;

	/**
	 * Construct a TclBoolean representation with the given boolean value. Note
	 * that only two TclBoolean internal rep instances will ever exist.
	 * 
	 * @param b
	 *            initial boolean value.
	 */
	private TclBoolean(boolean b) {
		value = b;
	}

	/**
	 * Returns a dupilcate of the current object.
	 */
	public InternalRep duplicate() {
		return this;
	}

	/**
	 * Implement this no-op for the InternalRep interface.
	 */

	public void dispose() {
	}

	/**
	 * Called to query the string representation of the Tcl object. This method
	 * is called only by TclObject.toString() when TclObject.stringRep is null.
	 * 
	 * @return the string representation of the Tcl object.
	 */
	public String toString() {
		if (value) {
			return "1";
		} else {
			return "0";
		}
	}

	/**
	 * Creates a new instance of a TclObject with a TclBoolean internal
	 * representation.
	 * 
	 * @param b
	 *            initial value of the boolean object.
	 * @return the TclObject with the given boolean value.
	 */

	public static TclObject newInstance(boolean b) {
		return new TclObject(b ? trueRep : falseRep);
	}

	/**
	 * SetBooleanFromAny -> setBooleanFromAny
	 * 
	 * Called to convert the other object's internal rep to boolean.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to convert to use the representation provided by
	 *            this class.
	 */
	private static void setBooleanFromAny(Interp interp, TclObject tobj)
			throws TclException {
		// Get the string representation. Make it up-to-date if necessary.
		String string = tobj.toString();

		if (tobj.isIntType()) {
			long i = TclInteger.getLong(interp, tobj);
			if (i == 0) {
				tobj.setInternalRep(falseRep);
			} else {
				tobj.setInternalRep(trueRep);
			}

			if (TclObject.saveObjRecords) {
				String key = "TclInteger -> TclBoolean";
				Integer num = (Integer) TclObject.objRecordMap.get(key);
				if (num == null) {
					num = 1;
				} else {
					num = num.intValue() + 1;
				}
				TclObject.objRecordMap.put(key, num);
			}
		} else if (tobj.isDoubleType()) {
			double d = TclDouble.get(interp, tobj);
			if (d == 0.0) {
				tobj.setInternalRep(falseRep);
			} else {
				tobj.setInternalRep(trueRep);
			}

			if (TclObject.saveObjRecords) {
				String key = "TclDouble -> TclBoolean";
				Integer num = (Integer) TclObject.objRecordMap.get(key);
				if (num == null) {
					num = 1;
				} else {
					num = num.intValue() + 1;
				}
				TclObject.objRecordMap.put(key, num);
			}
		} else {
			// Copy the string converting its characters to lower case.

			string = string.toLowerCase();
			String lowerCase = string.toLowerCase();

			// Parse the string as a boolean. We use an implementation here that
			// doesn't report errors in interp if interp is null.

			boolean b;
			boolean badBoolean = false;

			try {
				b = Util.getBoolean(interp, lowerCase);
			} catch (TclException te) {
				// Boolean values can be extracted from ints or doubles. Note
				// that we don't use strtoul or strtoull here because we don't
				// care about what the value is, just whether it is equal to
				// zero or not.

				badBoolean = true;
				b = false; // Always reassigned below
				if (interp != null) {
					interp.resetResult();
				}

				try {
					b = (Util.getInt(interp, lowerCase) != 0);
					badBoolean = false;
				} catch (TclException te2) {
				}

				if (badBoolean) {
					try {
						b = (Util.getDouble(interp, lowerCase) != 0.0);
						badBoolean = false;
					} catch (TclException te2) {
					}
				}
			}
			if (badBoolean) {
				if (interp != null) {
					interp.resetResult();
				}
				throw new TclException(interp,
						"expected boolean value but got \"" + string + "\"");
			}
			if (b) {
				tobj.setInternalRep(trueRep);
			} else {
				tobj.setInternalRep(falseRep);
			}

			if (TclObject.saveObjRecords) {
				String key = "TclString -> TclBoolean";
				Integer num = (Integer) TclObject.objRecordMap.get(key);
				if (num == null) {
					num = 1;
				} else {
					num = num.intValue() + 1;
				}
				TclObject.objRecordMap.put(key, num);
			}
		}
	}

	/**
	 * Returns the value of the object as an boolean.
	 * 
	 * An object with a TclBoolean internal rep has a boolean value. An object
	 * with a TclInteger internal rep and has the int value "0" or "1" is also a
	 * valid boolean value.
	 * 
	 * @param interp
	 *            current interpreter.
	 * @param tobj
	 *            the TclObject to use as an boolean.
	 * @return the boolean value of the object.
	 * @exception TclException
	 *                if the object cannot be converted into a boolean.
	 */
	public static boolean get(final Interp interp, final TclObject tobj)
			throws TclException {
		if (tobj.isIntType()) {
			// An integer with the value 0 or 1 can be
			// considered a boolean value, so there is
			// no need to change the internal rep.
			long ival = tobj.ivalue; // Inline TclInteger.get()
			if (ival == 0) {
				return false;
			} else if (ival == 1) {
				return true;
			}
		}

		InternalRep rep = tobj.getInternalRep();
		TclBoolean tbool;

		if (!(rep instanceof TclBoolean)) {
			setBooleanFromAny(interp, tobj);
			tbool = (TclBoolean) tobj.getInternalRep();
		} else {
			tbool = (TclBoolean) rep;
		}
		return tbool.value;
	}
}
