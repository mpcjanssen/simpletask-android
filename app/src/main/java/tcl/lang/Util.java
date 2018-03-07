/* 
 * Util.java --
 *
 *	This class provides useful Tcl utility methods.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997-1999 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: Util.java,v 1.33 2010/02/12 03:43:50 mdejong Exp $
 */

package tcl.lang;

import tcl.lang.cmd.FormatCmd;

public class Util {

	static final int TCL_DONT_USE_BRACES = 1;
	static final int USE_BRACES = 2;
	static final int BRACES_UNMATCHED = 4;

	// Some error messages.

	static final String intTooBigCode = "ARITH IOVERFLOW {integer value too large to represent}";
	static final String fpTooBigCode = "ARITH OVERFLOW {floating-point value too large to represent}";

	// This table below is used to convert from ASCII digits to a
	// numerical equivalent. It maps from '0' through 'z' to integers
	// (100 for non-digit characters).

	static char cvtIn[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 	// '0' - '9'
			100, 100, 100, 100, 100, 100, 100, 				// punctuation
			10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 		// 'A' - 'Z'
			20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 100, 100, 100, 100, 100, 100, // punctuation
			10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 		// 'a' - 'z'
			20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35 };

	// Largest possible base 10 exponent. Any
	// exponent larger than this will already
	// produce underflow or overflow, so there's
	// no need to worry about additional digits.

	static final int maxExponent = 511;

	// Table giving binary powers of 10. Entry
	// is 10^2^i. Used to convert decimal
	// exponents into floating-point numbers.

	static final double powersOf10[] = { 10., 100., 1.0e4, 1.0e8, 1.0e16, 1.0e32, 1.0e64, 1.0e128, 1.0e256 };

	// Default precision for converting floating-point values to strings.

	static final int DEFAULT_PRECISION = 12;

	// The following variable determine the precision used when converting
	// floating-point values to strings. This information is linked to all
	// of the tcl_precision variables in all interpreters inside a JVM via
	// PrecTraceProc.
	//
	// Note: since multiple threads may change precision concurrently, race
	// conditions may occur.
	//
	// It should be modified only by the PrecTraceProc class.

	static int precision = DEFAULT_PRECISION;

	/**
	 * Util -- Dummy constructor to keep Java from automatically creating a
	 * default public constructor for the Util class.
	 * 
	 * Side effects: None.
	 * 
	 */

	private Util() {
		// Do nothing. This should never be called.
	}

	/**
	 * Implements functionality of the strtoul() function used in the C Tcl
	 * library. This method will parse digits from what should be a 64-bit
	 * (signed) integer and report the index of the character immediately
	 * following the digits.
	 * 
	 * E.g.: "0x7fffffff" -> 2147483647 "0x80000000" -> -2147483648
	 * "-0xFF" -> -255
	 * 
	 * This method behaves like the strtoul() function in NativeTcl. This method
	 * will return a signed 64-bit Java long type in the strtoulResult argument.
	 * This value is used as a signed integer in the expr module. A leading
	 * signed character like '+' or '-' is supported. Leading spaces are
	 * skipped.
	 * 
	 * Results: The strtoulResult argument will be populated with the parsed
	 * value and the index of the character following the digits. If an error is
	 * detected, then the strtoulResult errno value will be set accordingly.
	 * 
	 * Side effects: None.
	 * 
	 * @param s
	 *            String of ASCII digits, possibly preceded by white space. For
	 *            bases greater than 10, either lower- or upper-case digits may
	 *            be used.
	 * 
	 * @param start
	 *            The index of s where the number starts.
	 * @param base
	 *            Base for conversion. Must be less than 37. If 0, then the base
	 *            is chosen from the leading characters of string: "0x" means
	 *            hex, "0" means octal, anything else means decimal.
	 * @param strtoulResult
	 *            Location to store results
	 */

	public static void strtoul(String s, int start, int base, StrtoulResult strtoulResult) {
		long result = 0;
		int digit;
		boolean anyDigits = false;
		boolean negative = false;
		int len = s.length();
		int i = start;
		char c = '\0';

		// Skip any leading blanks.

		while (i < len && (((c = s.charAt(i)) == ' ') || Character.isWhitespace(c))) {
			i++;
		}
		if (i >= len) {
			strtoulResult.update(0, 0, TCL.INVALID_INTEGER);
			return;
		}

		if (c == '-') {
			negative = true;
		}
		if (c == '-' || c == '+') {
			i++;
			if (i >= len) {
				strtoulResult.update(0, 0, TCL.INVALID_INTEGER);
				return;
			}
			c = s.charAt(i);
		}

		// If no base was provided, pick one from the leading characters
		// of the string.

		if (base == 0) {
			if (c == '0') {
				if (i < len - 2) { /* must be at least two more chars to consider X a hex indicator */
					i++;
					c = s.charAt(i);
					if (c == 'x' || c == 'X') { // FIXME: RS: ?? if ((c == 'x'
						// || c == 'X') && i < len -1) {
						i++;
						base = 16;
					}
				} 
				if (base == 0) {
					// Must set anyDigits here, otherwise "0" produces a
					// "no digits" error.

					anyDigits = true;
					base = 8;
				}
			} else {
				base = 10;
			}
		} else if (base == 16) {
			if (i < len - 2) {
				// Skip a leading "0x" from hex numbers.

				if ((c == '0') && (s.charAt(i + 1) == 'x')) {
					i += 2;
				}
			}
		}

		boolean overflowed = false;
		long previousResult = 0;
		int digitCount = 0;
		
		for (;; i += 1) {
			if (i >= len) {
				break;
			}
			digitCount++;
			digit = s.charAt(i) - '0';
			if (digit < 0 || digit > ('z' - '0')) {
				break;
			}
			digit = cvtIn[digit];
			if (digit >= base) {
				break;
			}

			switch (base) {
			case 2:
				result = (result << 1) | (long)digit;
				overflowed = (digitCount > 64);
				break;
			case 8:
				result = (result << 3) | (long)digit;
				overflowed = ((digitCount == 22 && digit>1) || digitCount > 22);
				break;
			case 16:
				result = (result << 4) | (long)digit;
				overflowed = (digitCount>16);
				break;
			default:
				result = (result * base) + digit;
				/* result should never decrease, otherwise we've overflowed */
				if (result < previousResult) {
					overflowed = true;
				}
				previousResult = result;		
				break;
			}
			anyDigits = true;
		}

		// See if there were any digits at all.
		if (negative) {
			result = -result;
		}

		if (!anyDigits) {
			strtoulResult.update(0, 0, TCL.INVALID_INTEGER);
		} else if (overflowed) {
			strtoulResult.update(result, i, TCL.INTEGER_RANGE);
		} else {
			strtoulResult.update(result, i, 0);
		}
	}

	/**
	 * Converts an ASCII string to an integer.
	 * 
	 * Results: The integer value of the string.
	 * 
	 * Side effects: None.
	 * 
	 * @param interp
	 *            The current interpreter. Can be null
	 * @param s
	 *            The string to convert from. Must be in valid Tcl integer
	 *            format.
	 * @return integer value
	 * @throws TclException
	 */
	public static long getInt(Interp interp, String s) throws TclException {
		int len = s.length();
		int i = 0;
		char c;

		StrtoulResult res;
		if (interp == null) {
			res = new StrtoulResult();
		} else {
			res = interp.strtoulResult;
		}
		Util.strtoul(s, i, 0, res);

		if (res.errno < 0) {
			if (res.errno == TCL.INTEGER_RANGE) {
				if (interp != null) {
					interp.setErrorCode(TclString.newInstance(intTooBigCode));
				}
				throw new TclException(interp, "integer value too large to represent");
			} else {
				throw new TclException(interp, "expected integer but got \"" + s + "\"" + checkBadOctal(interp, s));
			}
		} else if (res.index < len) {
			for (i = res.index; i < len; i++) {
				if (((c = s.charAt(i)) != ' ') && !Character.isWhitespace(c)) {
					throw new TclException(interp, "expected integer but got \"" + s + "\"" + checkBadOctal(interp, s));
				}
			}
		}

		return res.value;
	}

	/**
	 * Converts an ASCII string to a wide integer.
	 * 
	 * @param interp
	 * @param str
	 * @return
	 * @throws TclException
	 */
	public static long getWideInt(Interp interp, String str) throws TclException {
		return getInt(interp, str);
	}

	/**
	 * TclGetIntForIndex -> Util.getIntForIndex
	 * 
	 * This procedure returns an integer corresponding to the list index held in
	 * a Tcl object. The Tcl object's value is expected to be either an integer
	 * or a string of the form "end([+-]integer)?".
	 * 
	 * Results: The return value is the index that is found from the string. If
	 * the Tcl object referenced by tobj has the value "end", the value stored
	 * is endValue. If tobj's value is not of the form "end([+-]integer)?" and
	 * it can not be converted to an integer, an exception is raised.
	 * 
	 * Side effects: The object referenced by tobj might be converted to an
	 * integer object.
	 * 
	 * @param interp
	 *            interp, can be null
	 * @param tobj
	 *            the index object, an integer, "end", or "end-n"
	 * @param endValue
	 *            the index value to use as "end"
	 */
	public static final int getIntForIndex(Interp interp, TclObject tobj, int endValue) throws TclException {
		int length, offset;

		if (tobj.isIntType()) {
			return (int)TclInteger.getLong(interp, tobj);
		}

		String bytes = tobj.toString();
		length = bytes.length();

		if ((length == 0) || !"end".regionMatches(0, bytes, 0, (length > 3) ? 3 : length)) {
			// make sure bytes string is all digits
			for (int i = 0; i < length; i++) {
				if (!Character.isDigit(bytes.charAt(i)) && bytes.charAt(i) != '-') {
					throw new TclException(interp, "bad index \"" + bytes + "\": must be integer or end?-integer?");
				}
			}
			try {
				offset = (int)TclInteger.getLong(null, tobj);
			} catch (TclException e) {
				throw new TclException(interp, "bad index \"" + bytes + "\": must be integer or end?-integer?"
						+ checkBadOctal(interp, bytes));
			}
			return offset;
		}

		if (length <= 3) {
			return endValue;
		} else if ((length > 4) && (bytes.charAt(3) == '-')) {
			// This is our limited string expression evaluator
			// Pass everything after "end-" to then reverse for offset.

			String offsetStr = bytes.substring(4);
			// make sure offsetStr is all digits
			for (int i = 0; i < offsetStr.length(); i++) {
				if (!Character.isDigit(offsetStr.charAt(i)) && offsetStr.charAt(i) != '-') {
					throw new TclException(interp, "bad index \"" + bytes + "\": must be integer or end?-integer?");
				}
			}
			try {
				offset = (int)Util.getInt(interp, offsetStr);
				offset = -offset;
				return endValue + offset;
			} catch (TclException ex) {
				// Fall through to bad index error
			}
		}

		throw new TclException(interp, "bad index \"" + bytes + "\": must be integer or end?-integer?"
				+ checkBadOctal(interp, bytes.substring(3)));
	}

	/*
	 * TclCheckBadOctal -> Util.checkBadOctal
	 * 
	 * This procedure checks for a bad octal value and returns a meaningful
	 * error that should be appended to the interp's result.
	 * 
	 * Results: Returns error message if it was a bad octal.
	 * 
	 * Side effects: None.
	 * 
	 * @param interp Interpreter to use for error reporting, can be null.
	 * 
	 * @param value the value to check
	 * 
	 * @return
	 */
	public static final String checkBadOctal(Interp interp, String value) {
		int p = 0;
		final int len = value.length();

		// A frequent mistake is invalid octal values due to an unwanted
		// leading zero. Try to generate a meaningful error message.

		while (p < len && Character.isWhitespace(value.charAt(p))) {
			p++;
		}
		if ((p < len) && (value.charAt(p) == '+' || value.charAt(p) == '-')) {
			p++;
		}
		if ((p < len) && (value.charAt(p) == '0')) {
			while ((p < len) && Character.isDigit(value.charAt(p))) { // INTL:
				// digit.
				p++;
			}
			while ((p < len) && Character.isWhitespace(value.charAt(p))) { // INTL:
				// ISO
				// space.
				p++;
			}
			if (p >= len) {
				// Reached end of string
				if (interp != null) {
					return " (looks like invalid octal number)";
				}
			}
		}
		return "";
	}

	/**
	 * strtod --
	 * 
	 * Converts the leading decimal digits of a string into double and report
	 * the index of the character immediately following the digits.
	 * 
	 * Results: Converts the leading decimal digits of a string into double and
	 * report the index of the character immediately following the digits.
	 * 
	 * Side effects: None.
	 * 
	 * @param s
	 *            String of ASCII digits, possibly preceded by white space. For
	 *            bases greater than 10, either lower- or upper-case digits may
	 *            be used.
	 * @param start
	 *            The index of the string to start on.
	 * @param len
	 *            The string length, or -1
	 * @param strtodResult
	 *            place to store results
	 */
	public static void strtod(String s, final int start, int len, StrtodResult strtodResult) {
		int decPt = -1; // Number of mantissa digits BEFORE decimal
		// point.
		int si;
		int i = (start < 0 ? 0 : start);
		boolean negative = false;
		char c = '\0';
		String sub;

		if (len < 0) {
			len = s.length();
		}

		// Skip any leading blanks.

		while (i < len && (((c = s.charAt(i)) == ' ') || Character.isWhitespace(c))) {
			i++;
		}
		if (i >= len) {
			strtodResult.update(0, 0, TCL.INVALID_DOUBLE);
			return;
		}

		// Return special value for the string "NaN"

		if (c == 'N' || c == 'n') {
			sub = (i == 0 ? s : s.substring(i));
			if (sub.toLowerCase().startsWith("nan")) {
				strtodResult.update(Double.NaN, i + 3, 0);
				return;
			}
		}

		if (c == '-') {
			negative = true;
		}
		if (c == '-' || c == '+') {
			i++;
			if (i >= len) {
				strtodResult.update(0, 0, TCL.INVALID_DOUBLE);
				return;
			}
			c = s.charAt(i);
		}

		// The strings "Inf", "-Inf", "Infinity", and "-Infinity"
		// map to special double values.

		if (c == 'I') {
			int infLen = 0;

			sub = (i == 0 ? s : s.substring(i));

			if (sub.startsWith("Infinity")) {
				infLen = "Infinity".length();
			} else if (sub.startsWith("Inf")) {
				infLen = "Inf".length();
			}

			if (infLen > 0) {
				strtodResult.update((negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY), i + infLen, 0);
				return;
			}
		}

		// Index of first digit now known

		si = i;

		// Count the number of digits in the mantissa (including the decimal
		// point), and also locate the decimal point.

		boolean maybeZero = true;

		for (int mantSize = 0;; mantSize += 1) {
			if ((c >= '0' && c <= '9') || Character.isDigit(c)) {
				// This is a digit
			} else {
				if ((c != '.') || (decPt >= 0)) {
					break;
				}
				decPt = mantSize;
			}
			if (c != '0' && c != '.') {
				maybeZero = false; // non zero digit found...
			}
			i++;
			if (i >= len) {
				break;
			} else {
				c = s.charAt(i);
			}
		}

		// Skim off the exponent.

		if (i < len) {
			if (si != i) { // same c value as when above for loop was entered
				c = s.charAt(i);
			}
			if (((c == 'E') || (c == 'e')) && i < len - 1) {
				i++;
				if (i < len) {
					c = s.charAt(i);
					if (c == '-') {
						i++;
					} else if (c == '+') {
						i++;
					}

					boolean notdigit = false;
					if (i < len) {
						c = s.charAt(i);
						if ((c >= '0' && c <= '9') || Character.isDigit(c)) {
							// This is a digit
						} else {
							notdigit = true;
						}
					}

					if (i >= len || notdigit) {
						// A number like 1E+ or 1eq2 is not a double
						// with an exponent part. In this case, return
						// the number up to the 'E' or 'e'.
						if (c == '-' || c == '+') {
							i--;
						}
						i--;
					} else {
						for (; i < len; i++) {
							c = s.charAt(i);
							if ((c >= '0' && c <= '9') || Character.isDigit(c)) {
								// This is a digit
							} else {
								break;
							}
						}
					}
				}
			}
		}

		// Avoid pointless substring or NumberFormatException
		// for an empty string or for a string like "abc"
		// that has no digits.

		if (si == i) {
			strtodResult.update(0, 0, TCL.INVALID_DOUBLE);
			return;
		}

		s = s.substring(si, i);
		double result = 0;

		try {
			result = Double.valueOf(s);
		} catch (NumberFormatException e) {
			strtodResult.update(0, 0, TCL.INVALID_DOUBLE);
			return;
		}

		if ((result == Double.NEGATIVE_INFINITY) || (result == Double.POSITIVE_INFINITY)
				|| (result == 0.0 && !maybeZero)) {
			strtodResult.update(result, i, TCL.DOUBLE_RANGE);
			return;
		}

		if (Double.isNaN(result)) {
			strtodResult.update(0, 0, TCL.INVALID_DOUBLE);
			return;
		}

		strtodResult.update((negative ? -result : result), i, 0);
		return;
	}

	/**
	 * 
	 * getDouble --
	 * 
	 * Converts an ASCII string to a double.
	 * 
	 * Results: The double value of the string.
	 * 
	 * Side effects: None.
	 * 
	 * @param interp
	 *            The current interpreter, can be null
	 * @param s
	 *            The string to convert from. Must be in valid Tcl double
	 *            format.
	 * @return the double value
	 * @throws TclException
	 */
	public static double getDouble(Interp interp, String s) throws TclException {
		int len = s.length();
		int i = 0;
		char c;

		StrtodResult res;
		if (interp == null) {
			res = new StrtodResult();
		} else {
			res = interp.strtodResult;
		}
		Util.strtod(s, i, len, res);

		if (res.errno != 0) {
			if (res.errno == TCL.DOUBLE_RANGE) {
				if (interp != null) {
					interp.setErrorCode(TclString.newInstance(fpTooBigCode));
				}
				throw new TclException(interp, "floating-point value too large to represent");
			} else {
				throw new TclException(interp, "expected floating-point number but got \"" + s + "\"");
			}
		} else if (res.index < len) {
			for (i = res.index; i < len; i++) {
				if ((c = s.charAt(i)) != ' ' && !Character.isWhitespace(c)) {
					throw new TclException(interp, "expected floating-point number but got \"" + s + "\"");
				}
			}
		}

		return res.value;
	}

	/**
	 * Tcl_ConcatObj -> concat
	 * 
	 * Concatenate the strings from a set of objects into a single string object
	 * with spaces between the original strings.
	 * 
	 * Results: The return value is a new string object containing a
	 * concatenation of the strings in objv. Its ref count is zero.
	 * 
	 * Side effects: None.
	 * 
	 * @param from
	 *            The starting index.
	 * @param to
	 *            The ending index (inclusive).
	 * @param objv
	 *            Array of objects to concatenate.
	 * @return new String object
	 * @throws TclException
	 */
	public static TclObject concat(int from, int to, TclObject[] objv) throws TclException {
		int allocSize, elemLength, i, j;
		String element;
		StringBuffer concatStr;
		TclObject obj, tlist;
		boolean allList;

		if (from > objv.length) {
			return TclString.newInstance("");
		}
		if (to <= objv.length) {
			to = objv.length - 1;
		}

		// Check first to see if all the items are of list type. If so,
		// we will concat them together as lists, and return a list object.
		// This is only valid when the lists have no current string
		// representation, since we don't know what the original type was.
		// An original string rep may have lost some whitespace info when
		// converted which could be important.

		allList = true;
		for (i = from; i <= to; i++) {
			obj = objv[i];
			if (obj.hasNoStringRep() && obj.isListType()) {
				// A pure list element
			} else {
				allList = false;
				break;
			}
		}
		if (allList) {
			tlist = TclList.newInstance();
			for (i = from; i <= to; i++) {
				// Tcl_ListObjAppendList could be used here, but this saves
				// us a bit of type checking (since we've already done it)
				// Use of MAX_VALUE tells us to always put the new stuff on
				// the end. It will be set right in Tcl_ListObjReplace.

				obj = objv[i];
				TclObject[] elements = TclList.getElements(null, obj);
				TclList.replace(null, tlist, Integer.MAX_VALUE, 0, elements, 0, elements.length - 1);
			}
			return tlist;
		}

		allocSize = 0;
		for (i = from; i <= to; i++) {
			obj = objv[i];
			element = obj.toString();
			elemLength = element.length();
			if ((element != null) && (elemLength > 0)) {
				allocSize += (elemLength + 1);
			}
		}
		if (allocSize == 0) {
			allocSize = 1;
		}

		// Allocate storage for the concatenated result.

		concatStr = new StringBuffer(allocSize);

		// Now concatenate the elements. Clip white space off the front and back
		// to generate a neater result, and ignore any empty elements.

		for (i = from; i <= to; i++) {
			obj = objv[i];
			element = obj.toString();
			element = TrimLeft(element, " ");
			elemLength = element.length();

			// Trim trailing white space. But, be careful not to trim
			// a space character if it is preceded by a backslash: in
			// this case it could be significant.

			for (j = elemLength - 1; j >= 0; j--) {
				char c = element.charAt(j);
				if (c == ' ' || Character.isWhitespace(c)) {
					// A whitespace char
					if (j > 0 && element.charAt(j - 1) == '\\') {
						// Don't trim backslash space
						break;
					}
				} else {
					// Not a whitespace char
					break;
				}
			}
			if (j != (elemLength - 1)) {
				element = element.substring(0, j + 1);
			}
			if (element.length() == 0) {
				/* Don't leave extra space in the buffer */
				if (i == to && (concatStr.length() > 0)) {
					concatStr.setLength(concatStr.length() - 1);
				}
				continue;
			}
			concatStr.append(element);
			if (i < to) {
				concatStr.append(' ');
			}
		}

		return TclString.newInstance(concatStr);
	}

	/**
	 * 
	 * stringMatch --
	 * 
	 * See if a particular string matches a particular pattern. The matching
	 * operation permits the following special characters in the pattern: *?\[]
	 * (see the manual entry for details on what these mean).
	 * 
	 * Results: True if the string matches with the pattern.
	 * 
	 * Side effects: None.
	 * 
	 * @param str
	 *            String to compare pattern against
	 * @param pat
	 *            Pattern which may contain special characters.
	 * @return true if string matches within the pattern
	 */
	public static final boolean stringMatch(String str, String pat) {
		char[] strArr = str.toCharArray();
		char[] patArr = pat.toCharArray();
		int strLen = str.length(); // Cache the len of str.
		int patLen = pat.length(); // Cache the len of pat.
		int pIndex = 0; // Current index into patArr.
		int sIndex = 0; // Current index into patArr.
		char strch; // Stores current char in string.
		char ch1; // Stores char after '[' in pat.
		char ch2; // Stores look ahead 2 char in pat.
		boolean incrIndex = false; // If true it will incr both p/sIndex.

		while (true) {

			if (incrIndex == true) {
				pIndex++;
				sIndex++;
				incrIndex = false;
			}

			// See if we're at the end of both the pattern and the string.
			// If so, we succeeded. If we're at the end of the pattern
			// but not at the end of the string, we failed.

			if (pIndex == patLen) {
				return sIndex == strLen;
			}
			if ((sIndex == strLen) && (patArr[pIndex] != '*')) {
				return false;
			}

			// Check for a "*" as the next pattern character. It matches
			// any substring. We handle this by calling ourselves
			// recursively for each postfix of string, until either we
			// match or we reach the end of the string.

			if (patArr[pIndex] == '*') {
				pIndex++;
				if (pIndex == patLen) {
					return true;
				}
				while (true) {
					if (stringMatch(str.substring(sIndex), pat.substring(pIndex))) {
						return true;
					}
					if (sIndex == strLen) {
						return false;
					}
					sIndex++;
				}
			}

			// Check for a "?" as the next pattern character. It matches
			// any single character.

			if (patArr[pIndex] == '?') {
				incrIndex = true;
				continue;
			}

			// Check for a "[" as the next pattern character. It is followed
			// by a list of characters that are acceptable, or by a range
			// (two characters separated by "-").

			if (patArr[pIndex] == '[') {
				pIndex++;
				while (true) {
					if ((pIndex == patLen) || (patArr[pIndex] == ']')) {
						return false;
					}
					if (sIndex == strLen) {
						return false;
					}
					ch1 = patArr[pIndex];
					strch = strArr[sIndex];
					if (((pIndex + 1) != patLen) && (patArr[pIndex + 1] == '-')) {
						if ((pIndex += 2) == patLen) {
							return false;
						}
						ch2 = patArr[pIndex];
						if (((ch1 <= strch) && (ch2 >= strch)) || ((ch1 >= strch) && (ch2 <= strch))) {
							break;
						}
					} else if (ch1 == strch) {
						break;
					}
					pIndex++;
				}

				for (pIndex++; ((pIndex != patLen) && (patArr[pIndex] != ']')); pIndex++) {
				}
				if (pIndex == patLen) {
					pIndex--;
				}
				incrIndex = true;
				continue;
			}

			// If the next pattern character is '\', just strip off the '\'
			// so we do exact matching on the character that follows.

			if (patArr[pIndex] == '\\') {
				pIndex++;
				if (pIndex == patLen) {
					return false;
				}
			}

			// There's no special character. Just make sure that the next
			// characters of each string match.

			if ((sIndex == strLen) || (patArr[pIndex] != strArr[sIndex])) {
				return false;
			}
			incrIndex = true;
		}
	}

	/**
	 * Tcl_UtfToTitle -> toTitle --
	 * 
	 * Changes the first character of a string to title case or uppercase and
	 * the rest of the string to lowercase.
	 * 
	 * Results: Returns the generated string.
	 * 
	 * Side effects: None.
	 * 
	 * @param str
	 *            String to convert.
	 * @return new string
	 */
	public static String toTitle(String str) {
		// Capitalize the first character and then lowercase the rest of the
		// characters until we get to the end of string.

		int length = str.length();
		if (length == 0) {
			return "";
		}
		StringBuilder buf = new StringBuilder(length);
		buf.append(Character.toTitleCase(str.charAt(0)));
		buf.append(str.substring(1).toLowerCase());
		return buf.toString();
	}

	/**
	 * 
	 * regExpMatch --
	 * 
	 * See if a string matches a regular expression.
	 * 
	 * Results: Returns a boolean whose value depends on whether a match was
	 * made.
	 * 
	 * Side effects: None.
	 * 
	 * @param interp
	 *            Current interpreter
	 * @param string
	 *            The string to match.
	 * @param pattern
	 *            The regular expression.
	 * @return true if matched
	 * @throws TclException
	 */
	public static final boolean regExpMatch(Interp interp, String string, TclObject pattern) throws TclException {
		Regex r = TclRegexp.compile(interp, pattern, string);
		return r.match();
	}

	/**
	 * 
	 * appendElement --
	 * 
	 * Append a string to the string buffer. If the string buffer is not empty,
	 * append a space before appending "s".
	 * 
	 * Results: None.
	 * 
	 * Side effects: The value of "sbuf" is changesd.
	 * 
	 * @param interp
	 *            Current interpreter.
	 * @param sbuf
	 *            The buffer to append to.
	 * @param s
	 *            The string to append.
	 * @throws TclException
	 */
	public static final void appendElement(Interp interp, StringBuffer sbuf, String s) throws TclException {
		if (sbuf.length() > 0) {
			sbuf.append(' ');
		}

		int flags = scanElement(interp, s);
		convertElement(s, flags, sbuf);
	}

	/**
	 * 
	 * findElement --
	 * 
	 * Given a String that contains a Tcl list, locate the first (or next)
	 * element in the list.
	 * 
	 * Results: This method returns true and populates the FindElemResult if an
	 * element was found. If no element was found, false will be returned. The
	 * FindElemResult contains the index of the first and last characters of the
	 * element and the string value of the element.
	 * 
	 * Side effects: None.
	 * 
	 * @param interp
	 *            Current interpreter, can be null.
	 * @param s
	 *            The string to locate an element.
	 * @param i
	 *            The index inside s to start locating an element.
	 * @param len
	 *            The length of the string.
	 * @param fer
	 *            The result object to populate.
	 * @return true if found.
	 * @throws TclException
	 */
	public static final boolean findElement(Interp interp, String s, int i, int len, FindElemResult fer) throws TclException {
		int openBraces = 0;
		boolean inQuotes = false;
		char c = '\0';
		int elemStart, elemEnd;
		int size = 0;

		while (i < len && (((c = s.charAt(i)) == ' ') || Character.isWhitespace(c))) {
			i++;
		}
		if (i >= len) {
			return false;
		}
		if (c == '{') {
			openBraces = 1;
			i++;
		} else if (c == '"') {
			inQuotes = true;
			i++;
		}

		// An element typically consist of a range of characters
		// that are a substring of the string s, so s.substring()
		// can be used in most cases. If an element requires
		// backslashes then use a StringBuffer.

		StringBuffer sbuf = null;
		int simpleStart;
		String elem;

		elemStart = i;
		simpleStart = i;

		while (true) {
			if (i >= len) {
				elemEnd = i;
				size = (elemEnd - elemStart);
				if (openBraces != 0) {
					throw new TclException(interp, "unmatched open brace in list");
				} else if (inQuotes) {
					throw new TclException(interp, "unmatched open quote in list");
				}
				if (sbuf == null) {
					elem = s.substring(elemStart, elemEnd);
				} else {
					sbuf.append(s.substring(simpleStart, elemEnd));
					elem = sbuf.toString();
				}
				fer.update(elemStart, elemEnd, elem, size);
				return true;
			}

			c = s.charAt(i);
			switch (c) {
			// Open brace: don't treat specially unless the element is
			// in braces. In this case, keep a nesting count.

			case '{':
				if (openBraces != 0) {
					openBraces++;
				}
				i++;
				break;

			// Close brace: if element is in braces, keep nesting
			// count and quit when the last close brace is seen.

			case '}':
				if (openBraces == 1) {
					elemEnd = i;
					size = (elemEnd - elemStart);
					if (i == len - 1 || Character.isWhitespace(s.charAt(i + 1))) {
						if (sbuf == null) {
							elem = s.substring(elemStart, elemEnd);
						} else {
							sbuf.append(s.substring(simpleStart, elemEnd));
							elem = sbuf.toString();
						}
						fer.update(elemStart, elemEnd + 1, elem, size);
						return true;
					} else {
						int errEnd;
						for (errEnd = i + 1; errEnd < len; errEnd++) {
							if (Character.isWhitespace(s.charAt(errEnd))) {
								break;
							}
						}
						throw new TclException(interp, "list element in braces followed by \""
								+ s.substring(i + 1, errEnd) + "\" instead of space");
					}
				} else if (openBraces != 0) {
					openBraces--;
				}
				i++;
				break;

			// Backslash: skip over everything up to the end of the
			// backslash sequence.

			case '\\':
				BackSlashResult bs = Interp.backslash(s, i, len);
				if (openBraces > 0) {
					// Backslashes are ignored in brace-quoted stuff

				} else {
					if (sbuf == null) {
						sbuf = new StringBuffer();
					}
					sbuf.append(s.substring(simpleStart, i));
					sbuf.append(bs.c);
					simpleStart = bs.nextIndex;
				}
				i = bs.nextIndex;

				break;

			// Space: ignore if element is in braces or quotes; otherwise
			// terminate element.

			case ' ':
			case '\f':
			case '\n':
			case '\r':
			case '\t':
				if ((openBraces == 0) && !inQuotes) {
					elemEnd = i;
					size = (elemEnd - elemStart);
					if (sbuf == null) {
						elem = s.substring(elemStart, elemEnd);
					} else {
						sbuf.append(s.substring(simpleStart, elemEnd));
						elem = sbuf.toString();
					}
					fer.update(elemStart, elemEnd, elem, size);
					return true;
				} else {
					i++;
				}
				break;

			// Double-quote: if element is in quotes then terminate it.

			case '"':
				if (inQuotes) {
					elemEnd = i;
					size = (elemEnd - elemStart);
					if (i == len - 1 || Character.isWhitespace(s.charAt(i + 1))) {
						if (sbuf == null) {
							elem = s.substring(elemStart, elemEnd);
						} else {
							sbuf.append(s.substring(simpleStart, elemEnd));
							elem = sbuf.toString();
						}
						fer.update(elemStart, elemEnd + 1, elem, size);
						return true;
					} else {
						int errEnd;
						for (errEnd = i + 1; errEnd < len; errEnd++) {
							if (Character.isWhitespace(s.charAt(errEnd))) {
								break;
							}
						}
						throw new TclException(interp, "list element in quotes followed by \""
								+ s.substring(i + 1, errEnd) + "\" instead of space");
					}
				} else {
					i++;
				}
				break;

			default:
				i++;
			}
		}
	}

	/**
	 * Tcl_ScanElement -> scanElement
	 * 
	 * This procedure is a companion procedure to convertElement. It scans a
	 * string to see what needs to be done to it (e.g. add backslashes or
	 * enclosing braces) to make the string into a valid Tcl list element.
	 * 
	 * Results: The flags needed by Tcl_ConvertElement when doing the actual
	 * conversion.
	 * 
	 * Side effects: None.
	 * 
	 * @param interp
	 *            The current interpreter.
	 * @param string
	 *            The String to scan. (could be null)
	 * @return flags
	 * @throws TclException
	 */
	public static int scanElement(Interp interp, String string) throws TclException {
		int flags, nestingLevel;
		char c;
		int len;
		int i;

		// This procedure and Tcl_ConvertElement together do two things:
		//
		// 1. They produce a proper list, one that will yield back the
		// argument strings when evaluated or when disassembled with
		// Tcl_SplitList. This is the most important thing.
		// 
		// 2. They try to produce legible output, which means minimizing the
		// use of backslashes (using braces instead). However, there are
		// some situations where backslashes must be used (e.g. an element
		// like "{abc": the leading brace will have to be backslashed. For
		// each element, one of three things must be done:
		//
		// (a) Use the element as-is (it doesn't contain anything special
		// characters). This is the most desirable option.
		//
		// (b) Enclose the element in braces, but leave the contents alone.
		// This happens if the element contains embedded space, or if it
		// contains characters with special interpretation ($, [, ;, or \),
		// or if it starts with a brace or double-quote, or if there are
		// no characters in the element.
		//
		// (c) Don't enclose the element in braces, but add backslashes to
		// prevent special interpretation of special characters. This is a
		// last resort used when the argument would normally fall under case
		// (b) but contains unmatched braces. It also occurs if the last
		// character of the argument is a backslash or if the element contains
		// a backslash followed by newline.
		//
		// The procedure figures out how many bytes will be needed to store
		// the result (actually, it overestimates). It also collects
		// information about the element in the form of a flags word.

		final boolean debug = false;

		nestingLevel = 0;
		flags = 0;

		i = 0;
		if (string == null) {
			string = "";
		}
		len = string.length();

		if (debug) {
			System.out.println("scanElement string is \"" + string + "\"");
		}

		if (i == len) {
			// string length is zero
			flags |= USE_BRACES;
		} else {
			c = string.charAt(i);
			if ((c == '{') || (c == '"')) {
				flags |= USE_BRACES;
			}
		}
		for (; i < len; i++) {
			if (debug) {
				System.out.println("getting char at index " + i);
				System.out.println("char is '" + string.charAt(i) + "'");
			}

			c = string.charAt(i);
			switch (c) {
			case '{':
				nestingLevel++;
				break;
			case '}':
				nestingLevel--;
				if (nestingLevel < 0) {
					flags |= TCL_DONT_USE_BRACES | BRACES_UNMATCHED;
				}
				break;
			case '[':
			case '$':
			case ';':
			case ' ':
			case '\f':
			case '\n':
			case '\r':
			case '\t':
			case 0x0b:

				// 0x0b is the character '\v' -- this escape sequence is
				// not available in Java, so we hard-code it. We need to
				// support \v to provide compatibility with native Tcl.

				flags |= USE_BRACES;
				break;
			case '\\':
				if ((i >= len - 1) || (string.charAt(i + 1) == '\n')) {
					flags = TCL_DONT_USE_BRACES | BRACES_UNMATCHED;
				} else {
					BackSlashResult bs = Interp.backslash(string, i, len);

					// Subtract 1 because the for loop will automatically
					// add one on the next iteration.

					i = (bs.nextIndex - 1);
					flags |= USE_BRACES;
				}
				break;
			}
		}
		if (nestingLevel != 0) {
			flags = TCL_DONT_USE_BRACES | BRACES_UNMATCHED;
		}

		return flags;
	}

	/**
	 * 
	 * Tcl_ConvertElement -> convertElement
	 * 
	 * This is a companion procedure to scanElement. Given the information
	 * produced by scanElement, this procedure converts a string to a list
	 * element equal to that string.
	 * 
	 * Results: Conterts a string so to a new string so that Tcl List
	 * information is not lost.
	 * 
	 * Side effects: None.
	 * 
	 * @param s
	 *            Source information for list element.
	 * @param flags
	 *            Flags produced by scanElement
	 * @param sbuf
	 *            Buffer to write element to
	 */
	public static void convertElement(String s, int flags, StringBuffer sbuf) {
		int i = 0;
		char c;
		final int len = (s == null ? 0 : s.length());

		// See the comment block at the beginning of the ScanElement
		// code for details of how this works.

		if (len == 0) {
			sbuf.append("{}");
			return;
		}

		if (((flags & USE_BRACES) != 0) && ((flags & TCL_DONT_USE_BRACES) == 0)) {
			sbuf.append('{');
			sbuf.append(s);
			sbuf.append('}');
		} else {
			c = s.charAt(0);
			if (c == '{') {
				// Can't have a leading brace unless the whole element is
				// enclosed in braces. Add a backslash before the brace.
				// Furthermore, this may destroy the balance between open
				// and close braces, so set BRACES_UNMATCHED.

				sbuf.append('\\');
				sbuf.append('{');
				i++;
				flags |= BRACES_UNMATCHED;
			}

			for (; i < len; i++) {
				c = s.charAt(i);
				switch (c) {
				case ']':
				case '[':
				case '$':
				case ';':
				case ' ':
				case '\\':
				case '"':
					sbuf.append('\\');
					break;

				case '{':
				case '}':
					// It may not seem necessary to backslash braces, but
					// it is. The reason for this is that the resulting
					// list element may actually be an element of a sub-list
					// enclosed in braces (e.g. if Tcl_DStringStartSublist
					// has been invoked), so there may be a brace mismatch
					// if the braces aren't backslashed.

					if ((flags & BRACES_UNMATCHED) != 0) {
						sbuf.append('\\');
					}
					break;

				case '\f':
					sbuf.append('\\');
					sbuf.append('f');
					continue;

				case '\n':
					sbuf.append('\\');
					sbuf.append('n');
					continue;

				case '\r':
					sbuf.append('\\');
					sbuf.append('r');
					continue;

				case '\t':
					sbuf.append('\\');
					sbuf.append('t');
					continue;
				case 0x0b:
					// 0x0b is the character '\v' -- this escape sequence is
					// not available in Java, so we hard-code it. We need to
					// support \v to provide compatibility with native Tcl.

					sbuf.append('\\');
					sbuf.append('v');
					continue;
				}

				sbuf.append(c);
			}
		}

		return;
	}

	/**
	 * 
	 * TrimLeft --
	 * 
	 * Trim characters in "pattern" off the left of a string If pattern isn't
	 * supplied, whitespace is trimmed
	 * 
	 * Results: |>None.<|
	 * 
	 * Side effects: |>None.<|
	 * 
	 * @param str
	 *            The string to trim
	 * @param pattern
	 *            The pattern string used to trim.
	 * @return string 
	 */
	public static String TrimLeft(String str, String pattern) {
		int i, j;
		char c, p;
		char[] strArray = str.toCharArray();
		char[] patternArray = pattern.toCharArray();
		final int strLen = strArray.length;
		final int patLen = patternArray.length;
		boolean done;

		for (i = 0; i < strLen; i++) {
			c = str.charAt(i);
			done = true;
			for (j = 0; j < patLen; j++) {
				p = pattern.charAt(j);
				if (c == p || (p == ' ' && Character.isWhitespace(c))) {
					done = false;
					break;
				}
			}
			if (done) {
				break;
			}
		}
		return str.substring(i, strLen);
	}

	/**
	 * 
	 * TrimLeft --
	 * 
	 * Trims whitespace on the left side of a strin.g
	 * 
	 * Results: The trimmed string.
	 * 
	 * 
	 * @param str
	 *            The string to trim.
	 * @return The trimmed string.
	 */
	public static String TrimLeft(String str) {
		return TrimLeft(str, " \n\t\r");
	}

	/**
	 * 
	 * TrimRight --
	 * 
	 * Trim characters in "pattern" off the right of a string If pattern isn't
	 * supplied, whitespace is trimmed
	 * 
	 * Results: |>None.<|
	 * 
	 * @param str
	 *            The string to trim.
	 * @param pattern
	 *            The pattern to trim.
	 * @return The trimmed string.
	 */
	public static String TrimRight(String str, String pattern) {
		char[] strArray = str.toCharArray();
		char[] patternArray = pattern.toCharArray();
		final int patLen = patternArray.length;
		char c, p;
		int j;
		boolean done;
		int last = strArray.length - 1;

		// Remove trailing characters...

		while (last >= 0) {
			c = strArray[last];
			done = true;
			for (j = 0; j < patLen; j++) {
				p = patternArray[j];
				if (c == p || (p == ' ' && Character.isWhitespace(c))) {
					done = false;
					break;
				}
			}
			if (done) {
				break;
			}
			last--;
		}
		return str.substring(0, last + 1);
	}

	public static String TrimRight(String str) {
		return TrimRight(str, " \n\t\r");
	}

	/**
	 * 
	 * getBoolean --
	 * 
	 * Given a string, return a boolean value corresponding to the string.
	 * 
	 * Results:
	 * 
	 * 
	 * Side effects: None.
	 * 
	 * @param interp
	 *            The current interpreter.
	 * @param string
	 *            The string representation of the boolean.
	 * @return boolean value of string
	 * @throws TclException
	 *             For malformed boolean values.
	 */
	public static boolean getBoolean(Interp interp, String string) throws TclException {
		String s = string.toLowerCase();

		// The length of 's' needs to be > 1 if it begins with 'o',
		// in order to compare between "on" and "off".

		int slen = s.length();

		if (slen > 0) {
			char c = s.charAt(0);
			switch (c) {
			case '0':
				if (slen == 1) {
					return false;
				}
				break;
			case '1':
				if (slen == 1) {
					return true;
				}
				break;
			case 'f':
				if ("false".startsWith(s)) {
					return false;
				}
				break;
			case 'o':
				if (slen > 1 && "on".startsWith(s)) {
					return true;
				}
				if (slen > 1 && "off".startsWith(s)) {
					return false;
				}
				break;
			case 'n':
				if ("no".startsWith(s)) {
					return false;
				}
				break;
			case 't':
				if ("true".startsWith(s)) {
					return true;
				}
				break;
			case 'y':
				if ("yes".startsWith(s)) {
					return true;
				}
				break;
			}
		}

		throw new TclException(interp, "expected boolean value but got \"" + string + "\"");
	}

	/**
	 * 
	 * getActualPlatform --
	 * 
	 * This static procedure returns the integer code for the actual platform on
	 * which Jacl is running.
	 * 
	 * Results: Returns and integer.
	 * 
	 * Side effects: None.
	 * 
	 * @return Platform int
	 */
	public final static int getActualPlatform() {
		if (Util.isWindows()) {
			return JACL.PLATFORM_WINDOWS;
		}
		if (Util.isMac()) {
			return JACL.PLATFORM_MAC;
		}
		return JACL.PLATFORM_UNIX;
	}

	/**
	 * 
	 * isUnix --
	 * 
	 * Returns true if running on a Unix platform.
	 * 
	 * Results: Returns a boolean.
	 * 
	 * Side effects: None.
	 * 
	 * @return true if Unix
	 */
	public final static boolean isUnix() {
		if (isMac() || isWindows()) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * isMac --
	 * 
	 * Returns true if running on a Mac platform. Note that this method returns
	 * false for Mac OSX.
	 * 
	 * Results: Returns a boolean.
	 * 
	 * Side effects: None.
	 * 
	 * @return true if Mac
	 */
	public final static boolean isMac() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith("mac") && !os.endsWith("x")) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * isWindows --
	 * 
	 * Returns true if running on a Windows platform.
	 * 
	 * Results: Returns a boolean.
	 * 
	 * Side effects: None.
	 * 
	 * @return true if Windows
	 */
	public final static boolean isWindows() {
		String os = System.getProperty("os.name");
		if (os.toLowerCase().startsWith("win")) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * isJacl --
	 * 
	 * Returns true if running in Jacl. This method is used by conditional logic
	 * in the tcljava module.
	 * 
	 * Results: Returns a boolean.
	 * 
	 * Side effects: None.
	 * 
	 * @return true if jacl
	 */
	static boolean isJacl() {
		return true;
	}

	/**
	 * 
	 * looksLikeInt --
	 * 
	 * Returns true when isJacl() is true and this string looks like an integer.
	 * 
	 * Results: Returns a boolean.
	 * 
	 * Side effects: None.
	 * 
	 * @param s
	 *            String to check
	 * @return true if looks like an integer
	 */
	public static boolean looksLikeInt(String s) {
		return Expression.looksLikeInt(s, s.length(), 0, true);
	}

	/**
	 * 
	 * setupPrecisionTrace --
	 * 
	 * Sets up the variable trace of the tcl_precision variable.
	 * 
	 * Results: None.
	 * 
	 * Side effects: A variable trace is set up for the tcl_precision global
	 * variable.
	 * 
	 * @param interp
	 */
	static void setupPrecisionTrace(Interp interp) {
		try {
			interp.traceVar("tcl_precision", new PrecTraceProc(), TCL.GLOBAL_ONLY | TCL.TRACE_WRITES | TCL.TRACE_READS
					| TCL.TRACE_UNSETS);
		} catch (TclException e) {
			throw new TclRuntimeError("unexpected TclException: " + e);
		}
	}

	/**
	 * 
	 * printDouble --
	 * 
	 * Returns the string form of a double number. The exact formatting of the
	 * string depends on the tcl_precision variable.
	 * 
	 * Results: Returns the string form of double number.
	 * 
	 * Side effects: None.
	 * 
	 * @param number
	 *            The number to format into a string
	 * @return String rep
	 */
	public static String printDouble(double number) {
		String s = FormatCmd.toString(number, precision, 10);
		int length = s.length();
		for (int i = 0; i < length; i++) {
			if ((s.charAt(i) == '.') || Character.isLetter(s.charAt(i))) {
				return s;
			}
		}
		return s + ".0";
	}

	/**
	 * 
	 * tryGetSystemProperty --
	 * 
	 * Tries to get a system property. If it fails because of security
	 * exceptions, then return the default value.
	 * 
	 * Results: The value of the system property. If it fails because of
	 * security exceptions, then return the default value.
	 * 
	 * Side effects: None.
	 * 
	 * @param propName
	 *            Name of a property
	 * @param defautlValue
	 *            Default if property not found
	 * @return property value
	 */
	static String tryGetSystemProperty(String propName, String defautlValue) {
		try {
			return System.getProperty(propName);
		} catch (SecurityException e) {
			return defautlValue;
		}
	}

}

/**
 * 
 * The PrecTraceProc class is used to implement variable traces for the
 * tcl_precision variable to control precision used when converting
 * floating-point values to strings.
 * 
 */

final class PrecTraceProc implements VarTrace {

	// Maximal precision supported by Tcl.

	static final int TCL_MAX_PREC = 17;

	/**
	 * 
	 * traceProc --
	 * 
	 * This function gets called when the tcl_precision variable is accessed in
	 * the given interpreter.
	 * 
	 * Results: None.
	 * 
	 * Side effects: If the new value doesn't make sense then this procedure
	 * undoes the effect of the variable modification. Otherwise it modifies
	 * Util.precision that's used by Util.printDouble().
	 * 
	 * @see tcl.lang.VarTrace#traceProc(tcl.lang.Interp, java.lang.String,
	 *      java.lang.String, int)
	 * @throws If
	 *             the action is a TCL.TRACES_WRITE and the new value doesn't
	 *             make sense.
	 */
	public void traceProc(Interp interp, String name1, String name2, int flags) throws TclException {
		// If the variable is unset, then recreate the trace and restore
		// the default value of the format string.
		if ((flags & TCL.TRACE_UNSETS) != 0) {
			if (((flags & TCL.TRACE_DESTROYED) != 0) && ((flags & TCL.INTERP_DESTROYED) == 0)) {
				interp.traceVar(name1, name2, new PrecTraceProc(), TCL.GLOBAL_ONLY | TCL.TRACE_WRITES | TCL.TRACE_READS
						| TCL.TRACE_UNSETS);
				// unset doesn't change value of precision
			}
			return;
		}

		// When the variable is read, reset its value from our shared
		// value. This is needed in case the variable was modified in
		// some other interpreter so that this interpreter's value is
		// out of date.

		if ((flags & TCL.TRACE_READS) != 0) {
			interp.setVar(name1, name2, Util.precision, flags & TCL.GLOBAL_ONLY);
			return;
		}

		// The variable is being written. Check the new value and disallow
		// it if it isn't reasonable.
		//
		// Disallow it if this is a safe interpreter (we don't want
		// safe interpreters messing up the precision of other
		// interpreters).
		if (interp.isSafe) {
			throw new TclException(interp,"can't modify precision from a safe interpreter");
		}
		TclObject tobj = null;
		try {
			tobj = interp.getVar(name1, name2, (flags & TCL.GLOBAL_ONLY));
		} catch (TclException e) {
			// Do nothing when var does not exist.
		}

		String value;

		if (tobj != null) {
			value = tobj.toString();
		} else {
			value = "";
		}

		StrtoulResult r = interp.strtoulResult;
		Util.strtoul(value, 0, 10, r);

		if ((r.value <= 0) || (r.value > TCL_MAX_PREC) || (r.value > 100) || (r.index == 0)
				|| (r.index != value.length())) {
			interp.setVar(name1, name2, Util.precision, TCL.GLOBAL_ONLY);
			throw new TclException(interp, "improper value for precision");
		}

		Util.precision = (int) r.value;
	}

}
