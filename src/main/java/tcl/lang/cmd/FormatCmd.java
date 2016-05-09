/*
 * FormatCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FormatCmd.java,v 1.13 2006/05/14 22:07:49 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.StrtoulResult;
import tcl.lang.TclDouble;
import tcl.lang.TclException;
import tcl.lang.TclInteger;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "format" command in Tcl.
 */

public class FormatCmd implements Command {

	private static final int LEFT_JUSTIFY = 1;
	private static final int SHOW_SIGN = 2;
	private static final int SPACE_OR_SIGN = 4;
	private static final int PAD_W_ZERO = 8;
	private static final int ALT_OUTPUT = 16;
	private static final int SIGNED_VALUE = 32;
	private static final int RADIX = 1; // Integer types. %d, %x, %o
	private static final int FLOAT = 2; // Floating point. %f
	private static final int EXP = 3; // Exponentional. %e and %E
	private static final int GENERIC = 4; // Floating or exponential,

	// depending on exponent. %g

	/**
	 * This procedure is invoked to process the "format" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * The first argument to the cmdProc is the formatString. The cmdProc simply
	 * copies all the chars into the sbuf until a '%' is found. At this point
	 * the cmdProc parces the formatString and determines the format parameters.
	 * The parcing of the formatString can be broken into six possible phases:
	 * 
	 * Phase 0 - Simply Print: If the next char is % Phase 1 - XPG3 Position
	 * Specifier: If the format [1-n]$ is used Phase 2 - A Set of Flags: One or
	 * more of the following + - [space] 0 # Phase 3 - A Minimun Field Width
	 * Either [integer] or * Phase 4 - A Precision If the format .[integer] or
	 * .* Phase 5 - A Length Modifier If h is present Phase 6 - A Conversion
	 * Character If one of the following is used d u i o x X c s f E g G
	 * 
	 * Any phase can skip ahead one or more phases, but are not allowed to move
	 * back to previous phases. Once the parameters are determined the cmdProc
	 * calls one of three private methods that returns a fully formatted string.
	 * This loop occurs for ever '%' in the formatString.
	 */

	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {

		StringBuffer sbuf; // Stores the return value of the parsed
		// format string
		StrtoulResult stoul; // A result object to pass to strtoul call
		char[] format; // The format argument is converted to a char
		// array and manipulated as such
		int phase; // Stores the current phase of the parsing
		int width; // Minimum field width
		int precision; // Field precision from field specifier
		int fmtFlags; // Used to store the format flags ( #,+,etc)
		int argIndex; // Index of argument to substitute next.
		int fmtIndex; // Used to locate end of the format fields.
		int endIndex; // Used to locate end of numerical fields.
		int intValue; // Generic storage variable
		long lngValue; // Store the TclInteger.get() result
		double dblValue; // Store the TclDouble.get() result
		boolean noPercent; // Special case for speed: indicates there's
		// no field specifier, just a string to copy.
		boolean xpgSet; // Indicates that xpg has been used for the
		// particular format of the main while loop
		boolean gotXpg; // True means that an XPG3 %n$-style
		// specifier has been seen.
		boolean gotSequential; // True means that a regular sequential
		// (non-XPG3) conversion specifier has
		// been seen.
		boolean useShort; // Value to be printed is short
		// (half word).
		boolean useLong;  // Value to be printed is long
		boolean precisionSet; // Used for f, e, and E conversions
		boolean cont; // Used for phase 3

		if (argv.length < 2) {
			throw new TclNumArgsException(interp, 1, argv,
					"formatString ?arg arg ...?");
		}

		argIndex = 2;
		fmtIndex = 0;
		gotXpg = gotSequential = false;
		format = argv[1].toString().toCharArray();
		sbuf = new StringBuffer();

		// So, what happens here is to scan the format string one % group
		// at a time, making many individual appends to the StringBuffer.

		while (fmtIndex < format.length) {
			fmtFlags = phase = width = 0;
			noPercent = true;
			xpgSet = precisionSet = useShort = false;
			useLong = false;
			precision = -1;

			// Append all characters to sbuf that are not used for the
			// format specifier.

			if (format[fmtIndex] != '%') {
				int i;
				for (i = fmtIndex; (i < format.length); i++) {
					if (format[i] == '%') {
						noPercent = false;
						break;
					}
				}
				sbuf.append(new String(format, fmtIndex, i - fmtIndex));
				fmtIndex = i;
				if (noPercent) {
					break;
				}
			}

			// If true, then a % has been indicated but we are at the end
			// of the format string. Call function to throw exception.

			if (fmtIndex + 1 >= format.length) {
				errorEndMiddle(interp);
			}

			// Phase 0:
			// Check for %%. If true then simply write a single '%'
			// to the list.

			checkOverFlow(interp, format, fmtIndex + 1);
			if (format[fmtIndex + 1] == '%') {
				sbuf.append("%");
				fmtIndex += 2;
				// Re-enter the loop

				continue;
			}

			fmtIndex++;
			checkOverFlow(interp, format, fmtIndex);
			if (Character.isDigit(format[fmtIndex])) {
				// Parse the format array looking for the end of
				// the number.

				stoul = FormatCmd.strtoul(interp, format, fmtIndex);
				intValue = (int) stoul.value;
				endIndex = stoul.index;
				stoul = null;

				if (format[endIndex] == '$') {
					if (intValue == 0) {
						errorBadIndex(interp, true);
					}

					// Phase 1:
					// Check for an XPG3-style %n$ specification.
					// Note: there must not be a mixture of XPG3
					// specs and non-XPG3 specs in the same format string.

					if (gotSequential) {
						errorMixedXPG(interp);
					}
					gotXpg = true;
					xpgSet = true;
					phase = 2;
					fmtIndex = endIndex + 1;
					argIndex = intValue + 1;
					if ((argIndex < 2) || (argIndex >= argv.length)) {
						errorBadIndex(interp, gotXpg);
					}

				} else {
					// Phase 3:
					// Format jumped straight to phase 3; Setting
					// width field. Again, verify that all format
					// specifiers are sequential.

					if (gotXpg) {
						errorMixedXPG(interp);
					}
					gotSequential = true;
					if (format[fmtIndex] != '0') {
						fmtIndex = endIndex;
						width = intValue;
						phase = 4;
					}
				}
			} else {
				if (gotXpg) {
					errorMixedXPG(interp);
				}
				gotSequential = true;
			}

			// Phase 2:
			// Setting the Format Flags. At this point the phase value
			// can be either zero or three. Anything greater is an
			// incorrect format.

			if (phase < 3) {
				checkOverFlow(interp, format, fmtIndex);
				char ch = format[fmtIndex];
				cont = true;
				while (cont) {
					switch (ch) {
					case '-': {
						fmtFlags |= LEFT_JUSTIFY;
						break;
					}
					case '#': {
						fmtFlags |= ALT_OUTPUT;
						break;
					}
					case '0': {
						fmtFlags |= PAD_W_ZERO;
						break;
					}
					case ' ': {
						fmtFlags |= SPACE_OR_SIGN;
						break;
					}
					case '+': {
						fmtFlags |= SHOW_SIGN;
						break;
					}
					default: {
						cont = false;
					}
					}
					if (cont) {
						fmtIndex++;
						checkOverFlow(interp, format, fmtIndex);
						ch = format[fmtIndex];
					}
				}
				phase = 3;
			}

			// Phase 3:
			// Setting width field. Partially redundant code from the
			// Phase 1 if/else statement, but this is made to run fast.

			checkOverFlow(interp, format, fmtIndex);
			if (Character.isDigit(format[fmtIndex])) {
				stoul = FormatCmd.strtoul(interp, format, fmtIndex);
				width = (int) stoul.value;
				fmtIndex = stoul.index;
				stoul = null;
			} else if (format[fmtIndex] == '*') {
				if (argv.length > argIndex) {
					width = TclInteger.getInt(interp, argv[argIndex]);
					if (width < 0) {
						width = -width;
						fmtFlags |= LEFT_JUSTIFY;
					}
					argIndex++;
					fmtIndex++;
				}
			}

			// Phase 4:
			// Setting the precision field.

			checkOverFlow(interp, format, fmtIndex);
			if (format[fmtIndex] == '.') {
				fmtIndex++;
				checkOverFlow(interp, format, fmtIndex);
				if (Character.isDigit(format[fmtIndex])) {
					precisionSet = true;
					stoul = FormatCmd.strtoul(interp, format, fmtIndex);
					precision = (int) stoul.value;
					fmtIndex = stoul.index;
					stoul = null;
				} else if (format[fmtIndex] == '*') {
					if (argv.length > argIndex) {
						precisionSet = true;
						precision = TclInteger.getInt(interp, argv[argIndex]);
						argIndex++;
						fmtIndex++;
						checkOverFlow(interp, format, fmtIndex);
					}
				} else {
					// Format field had a '.' without an integer or '*'
					// preceeding it (eg %2.d or %2.-5d)
					precision = 0;
					//errorBadField(interp, format[fmtIndex]);
				}
			}

			// Phase 5:
			// Setting the length modifier.

			if (format[fmtIndex] == 'h') {
				fmtIndex++;
				checkOverFlow(interp, format, fmtIndex);
				useShort = true;
			} else if (format[fmtIndex] == 'l') {
				fmtIndex++;
				checkOverFlow(interp, format, fmtIndex);
				useLong = true;
			}

			if ((argIndex < 2) || (argIndex >= argv.length)) {
				errorBadIndex(interp, gotXpg);
			}

			// Phase 6:
			// Setting conversion field.
			// At this point, variables are initialized as follows:
			//
			// width The specified field width. This is always
			// non-negative. Zero is the default.
			// precision The specified precision. The default
			// is -1.
			// argIndex The argument index from the argv array
			// for the appropriate arg.
			// fmtFlags The format flags are set via bitwise
			// operations. Below are the bits
			// and their meanings.

			// ALT_OUTPUT set if a '#' is present.
			// SHOW_SIGN set if a '+' is present.
			// SPACE_OR_SIGN set if a ' ' is present.
			// LEFT_JUSTIFY set if a '-' is present or if the
			// field width was negative.
			// PAD_W_ZERO set if a '0' is present

			String strValue = "";
			char index = format[fmtIndex];

			switch (index) {
			case 'u':
			case 'd':
			case 'o':
			case 'x':
			case 'X':
			case 'i': {
				lngValue = TclInteger.getLong(interp, argv[argIndex]);
				if (index != 'u') fmtFlags |= SIGNED_VALUE;

				// If the useShort option has been selected, we need
				// to clear all but the first 16 bits.

				if (useShort) {
					lngValue &= 0xFFFFL;
				} 
				
				if (index == 'o') {
					sbuf.append(cvtLngToStr(lngValue, width, precision,
							fmtFlags, 8, "01234567".toCharArray(), "0"));
				} else if (index == 'x') {
					sbuf.append(cvtLngToStr(lngValue, width, precision,
							fmtFlags, 16, "0123456789abcdef".toCharArray(),
							"0x"));
				} else if (index == 'X') {
					sbuf.append(cvtLngToStr(lngValue, width, precision,
							fmtFlags, 16, "0123456789ABCDEF".toCharArray(),
							"0X"));
				} else {
					sbuf.append(cvtLngToStr(lngValue, width, precision,
							fmtFlags, 10, "0123456789".toCharArray(), ""));
				}
				break;
			}
			case 'c': {
				intValue = 0;
				char arr[] = { (char) TclInteger.getInt(interp, argv[argIndex]) };
				strValue = new String(arr);
				sbuf.append(cvtStrToStr(strValue, width, precision, fmtFlags));
				break;
			}
			case 's': {
				strValue = argv[argIndex].toString();
				sbuf.append(cvtStrToStr(strValue, width, precision, fmtFlags));
				break;
			}
			case 'f': {
				dblValue = TclDouble.get(interp, argv[argIndex]);
				sbuf.append(cvtDblToStr(dblValue, width, precision, fmtFlags,
						10, "0123456789".toCharArray(), "", FLOAT));
				break;
			}
			case 'e': {
				dblValue = TclDouble.get(interp, argv[argIndex]);
				sbuf.append(cvtDblToStr(dblValue, width, precision, fmtFlags,
						10, "e".toCharArray(), "", EXP));
				break;
			}
			case 'E': {
				dblValue = TclDouble.get(interp, argv[argIndex]);
				sbuf.append(cvtDblToStr(dblValue, width, precision, fmtFlags,
						10, "E".toCharArray(), "", EXP));
				break;
			}
			case 'g': {
				dblValue = TclDouble.get(interp, argv[argIndex]);
				sbuf.append(cvtDblToStr(dblValue, width, precision, fmtFlags,
						10, "e".toCharArray(), "", GENERIC));
				break;
			}
			case 'G': {
				dblValue = TclDouble.get(interp, argv[argIndex]);
				sbuf.append(cvtDblToStr(dblValue, width, precision, fmtFlags,
						10, "E".toCharArray(), "", GENERIC));
				break;
			}
			default: {
				errorBadField(interp, format[fmtIndex]);
			}
			}
			fmtIndex++;
			argIndex++;
		}
		interp.setResult(sbuf.toString());
	}

	/**
	 * This procedure is invoked in "phase 6" od the Format cmdProc. It converts
	 * the lngValue to a string with a specified format determined by the other
	 * input variables.
	 * 
	 * @param lngValue
	 *            - Is the value of the argument input
	 * @param width
	 *            - The minimum width of the string.
	 * @param precision
	 *            - The minimum width if the integer. If the string len is less
	 *            than precision, leading 0 are appended.
	 * @param flags
	 *            - Specifies various formatting to the string representation
	 *            (-, +, space, 0, #)
	 * @param base
	 *            - The base of the integer (8, 10, 16)
	 * @param charSet
	 *            - The char set to use for the conversion to ascii OR The char
	 *            used for sci notation.
	 * @param altPrefix
	 *            - If not empty, str to append on the beginnig of the resulting
	 *            string (eg 0 or 0x or 0X ).
	 * @return String representation of the long.
	 */

	private String cvtLngToStr(long lngValue, int width, int precision,
			int flags, int base, char[] charSet, String altPrefix) {
		StringBuilder sbuf = new StringBuilder(100);
		StringBuilder tmpBuf = new StringBuilder(100);

		int i;
		int length;
		int nspace;
		int prefixSize = 0;
		char prefix = 0;

		// For the format %#x, the value zero is printed "0" not "0x0".
		// I think this is stupid.

		if (lngValue == 0) {
			flags = (flags | ALT_OUTPUT);
		}

		if ((flags & SIGNED_VALUE) != 0) {
			if (lngValue < 0) {
				/* preserve sign bit in base 8 and 16 */
				if (base != 8 && base !=16) {
					lngValue = -lngValue;
					prefix = '-';
					prefixSize = 1;
				}
			} else if ((flags & SHOW_SIGN) != 0) {
				prefix = '+';
				prefixSize = 1;
			} else if ((flags & SPACE_OR_SIGN) != 0) {
				prefix = ' ';
				prefixSize = 1;
			}
		}

		if (((PAD_W_ZERO & flags) != 0) && (precision < width - prefixSize)) {
			precision = width - prefixSize;
		}

		// Convert to ascii

		do {
			/* treat base 8 and 16 separately, to preserve sign bit */
			int digit;
			switch (base) {
			case 8:
				digit = (int)(lngValue & 07L);
				break;
			case 16:
				digit = (int)(lngValue & 0xfL);
				break;
			default:
				digit = (int)(lngValue % base);
				break;
			}
			sbuf.insert(0, charSet[digit]);
			switch (base) {
			case 8:
				lngValue = lngValue >>> 3;
				break;
			case 16:
				lngValue = lngValue >>> 4;
				break;
			default:
				lngValue = lngValue / base;
				break;
			}
		} while (lngValue != 0);

		length = sbuf.length();
		for (i = (precision - length); i > 0; i--) {
			sbuf.insert(0, '0');
		}
		if (prefix != 0) {
			sbuf.insert(0, prefix);
		}
		if ((flags & ALT_OUTPUT) != 0) {
			if ((altPrefix.length() > 0)
					&& (sbuf.charAt(0) != altPrefix.charAt(0))) {
				sbuf.insert(0, altPrefix);
			}
		}

		// The text of the conversion is pointed to by "bufpt" and is
		// "length" characters long. The field width is "width". Do
		// the output.

		nspace = width - sbuf.length();
		if (nspace > 0) {
			tmpBuf.ensureCapacity(nspace);
			for (i = 0; i < nspace; i++) {
				tmpBuf.append(' ');
			}
		}

		if ((LEFT_JUSTIFY & flags) != 0) {
			// left justified

			sbuf.append(tmpBuf);
			return sbuf.toString();
		} else {
			// right justified

			tmpBuf.append(sbuf);
			return tmpBuf.toString();
		}
	}

	// Convert a double value to a Java String.

	public static String toString(double dblValue, int precision, int base) {
		return cvtDblToStr(dblValue, 0, precision, 0, base, "e".toCharArray(),
				null, GENERIC);
	}

	/**
	 * This procedure is invoked in "phase 6" od the Format cmdProc. It converts
	 * the lngValue to a string with a specified format determined by the other
	 * input variables.
	 * 
	 * @param dblValue
	 *            - Is the value of the argument input
	 * @param width
	 *            - The minimum width of the string.
	 * @param precision
	 *            - The minimum width if the integer. If the string len is less
	 *            than precision, leading 0 are appended.
	 * @param flags
	 *            - Specifies various formatting to the string representation
	 *            (-, +, space, 0, #)
	 * @param base
	 *            - The base of the integer (8, 10, 16)
	 * @param charSet
	 *            - The char set to use for the conversion to ascii OR The char
	 *            used for sci notation.
	 * @param altPrefix
	 *            - If not empty, str to append on the beginnig of the resulting
	 *            string (eg 0 or 0x or 0X ).
	 * @param xtype
	 *            - Either FLOAT, EXP, or GENERIC depending on the format
	 *            specifier.
	 * @return String representation of the long.
	 */

	private static String cvtDblToStr(double dblValue, int width,
			int precision, int flags, int base, char[] charSet,
			String altPrefix, int xtype) {
		StringBuilder sbuf = new StringBuilder(100);
		int i;
		int exp;
		int length;
		int count;
		int digit;
		int prefixSize = 0;
		char prefix = 0;
		double rounder;
		boolean flag_exp = false; // Flag for exponential representation
		boolean flag_rtz = true; // Flag for "remove trailing zeros"
		boolean flag_dp = true; // Flag for remove "decimal point"

		if (Double.isNaN(dblValue)) {
			return "NaN";
		}
		if (dblValue == Double.NEGATIVE_INFINITY) {
			return "-Inf";
		}
		if (dblValue == Double.POSITIVE_INFINITY) {
			return "Inf";
		}

		// If precision < 0 (eg -1) then the precision defaults

		if (precision < 0) {
			precision = 6;
		}

		if (dblValue < 0.0) {
			dblValue = -dblValue;
			prefix = '-';
			prefixSize = 1;
		} else if (dblValue == 0.0
				&& (Double.valueOf(dblValue)).equals((-0.0))) {
			// Handle -0.0
			//
			// 15.19.1 "Numerical Comparison Operators <, <=, >, and >= "
			// of the Java Language Spec says:
			// "Positive zero and negative zero are considered
			// equal. Therefore, -0.0<0.0 is false, for example, but
			// -0.0<=0.0 is true."
			//
			// The Double.equal man page says:
			// "If d1 represents +0.0 while d2 represents -0.0, or
			// vice versa, the equal test has the value false, even
			// though +0.0==-0.0 has the value true. This allows
			// hashtables to operate properly.

			dblValue = -dblValue;
			prefix = '-';
			prefixSize = 1;
		} else if ((flags & SHOW_SIGN) != 0) {
			prefix = '+';
			prefixSize = 1;
		} else if ((flags & SPACE_OR_SIGN) != 0) {
			prefix = ' ';
			prefixSize = 1;
		}

		// For GENERIC xtypes the precision includes the ones digit
		// so just decrement to get the correct precision.

		if (xtype == GENERIC && precision > 0) {
			precision--;
		}

		// Rounding works like BSD when the constant 0.4999 is used. Wierd!

		for (i = precision, rounder = 0.4999; i > 0; i--, rounder *= 0.1)
			;

		if (xtype == FLOAT) {
			dblValue += rounder;
		}

		// Normalize dblValue to within 10.0 > dblValue >= 1.0

		exp = 0;
		if (dblValue > 0.0) {
			int k = 0;
			while ((dblValue >= 1e8) && (k++ < 100)) {
				dblValue *= 1e-8;
				exp += 8;
			}
			while ((dblValue >= 10.0) && (k++ < 100)) {
				dblValue *= 0.1;
				exp++;
			}
			while ((dblValue < 1e-8) && (k++ < 100)) {
				dblValue *= 1e8;
				exp -= 8;
			}
			while ((dblValue < 1.0) && (k++ < 100)) {
				dblValue *= 10.0;
				exp--;
			}
			if (k >= 100) {
				return "NaN";
			}
		}

		// If the field type is GENERIC, then convert to either EXP
		// or FLOAT, as appropriate.

		flag_exp = xtype == EXP;
		if (xtype != FLOAT) {
			dblValue += rounder;
			if (dblValue >= 10.0) {
				dblValue *= 0.1;
				exp++;
			}
		}
		if (xtype == GENERIC) {
			flag_rtz = !((flags & ALT_OUTPUT) != 0);
			if ((exp < -4) || (exp > precision)) {
				xtype = EXP;
			} else {
				precision = (precision - exp);
				xtype = FLOAT;
			}
		} else {
			flag_rtz = false;
		}

		// The "exp+precision" test causes output to be of type EXP if
		// the precision is too large to fit in buf[].

		count = 0;
		if (xtype == FLOAT) {
			flag_dp = ((precision > 0) || ((flags & ALT_OUTPUT) != 0));
			if (prefixSize > 0) {
				// Sign

				sbuf.append(prefix);
			}
			if (exp < 0) {
				// Digits before "."

				sbuf.append('0');
			}
			for (; exp >= 0; exp--) {
				if (count++ >= 16) {
					sbuf.append('0');
				} else {
					digit = (int) dblValue;
					dblValue = (dblValue - digit) * 10.0;
					sbuf.append(digit);
				}
			}
			if (flag_dp) {
				sbuf.append('.');
			}
			for (exp++; (exp < 0) && (precision > 0); precision--, exp++) {
				sbuf.append('0');
			}
			while ((precision--) > 0) {
				if (count++ >= 16) {
					sbuf.append('0');
				} else {
					digit = (int) dblValue;
					dblValue = (dblValue - digit) * 10.0;
					sbuf.append(digit);
				}
			}

			if (flag_rtz && flag_dp) {
				// Remove trailing zeros and "."

				int len, index = 0;
				for (len = sbuf.length() - 1; (len >= 0)
						&& (sbuf.charAt(len) == '0'); len--, index++) {
				}

				if ((len >= 0) && (sbuf.charAt(len) == '.')) {
					index++;
				}

				if (index > 0) {
					sbuf.setLength(sbuf.length() - index);
				}
			}
		} else {
			// EXP or GENERIC

			flag_dp = ((precision > 0) || ((flags & ALT_OUTPUT) != 0));

			if (prefixSize > 0) {
				sbuf.append(prefix);
			}
			digit = (int) dblValue;
			dblValue = (dblValue - digit) * 10.0;
			sbuf.append(digit);
			if (flag_dp) {
				sbuf.append('.');
			}
			while (precision-- > 0) {
				if (count++ >= 16) {
					sbuf.append('0');
				} else {
					digit = (int) dblValue;
					dblValue = (dblValue - digit) * 10.0;
					sbuf.append(digit);
				}
			}

			if (flag_rtz && flag_dp) {
				// Remove trailing zeros and "."

				for (i = 0, length = (sbuf.length() - 1); (length >= 0)
						&& (sbuf.charAt(length) == '0'); length--, i++)
					;

				if ((length >= 0) && (sbuf.charAt(length) == '.')) {
					i++;
				}

				if (i > 0) {
					sbuf.setLength(sbuf.length() - i);
				}
			}
			if ((exp != 0) || flag_exp) {
				sbuf.append(charSet[0]);
				if (exp < 0) {
					sbuf.append('-');
					exp = -exp;
				} else {
					sbuf.append('+');
				}
				if (exp >= 100) {
					sbuf.append((exp / 100));
					exp %= 100;
				} else {
					sbuf.append('0');
				}
				sbuf.append(exp / 10);
				sbuf.append(exp % 10);
			}
		}

		// The converted number is in sbuf. Output it.
		// Note that the number is in the usual order, not reversed as with
		// integer conversions.

		length = sbuf.length();

		// Special case: Add leading zeros if the PAD_W_ZERO flag is
		// set and we are not left justified

		if (((PAD_W_ZERO & flags) != 0) && ((LEFT_JUSTIFY & flags) == 0)) {
			int nPad = width - length;
			i = prefixSize;
			while ((nPad--) > 0) {
				sbuf.insert(prefixSize, '0');
			}
			length = width;
		}

		// Count the number of spaces remaining and create a StringBuffer
		// (tmpBuf) with the correct number of spaces.

		int nspace = width - length;
		StringBuilder tmpBuf = new StringBuilder(100 + nspace);
		if (nspace > 0) {
			for (i = 0; i < nspace; i++) {
				tmpBuf.append(' ');
			}
		}

		if ((LEFT_JUSTIFY & flags) != 0) {
			// left justified

			sbuf.append(tmpBuf);
			return sbuf.toString();
		} else {
			// right justified

			tmpBuf.append(sbuf);
			return tmpBuf.toString();
		}
	}

	/**
	 * This procedure is invoked in "phase 6" od the Format cmdProc. It converts
	 * the strValue to a string with a specified format determined by the other
	 * input variables.
	 * 
	 * @param strValue
	 *            - Is the String w/o formatting.
	 * @param width
	 *            - The minimum width of the string.
	 * @param precision
	 *            - The minimum width if the integer. If the string len is less
	 *            than precision, leading 0 are appended.
	 * @param flags
	 *            - Specifies various formatting to the string representation
	 *            (-, +, space, 0, #)
	 * @return String representation of the integer.
	 */

	private static String cvtStrToStr(String strValue, int width,
			int precision, int flags) {
		String left = "";
		String right = "";
		StringBuilder sbuf = new StringBuilder(100);

		if (precision >= 0 && precision < strValue.length()) {
			strValue = strValue.substring(0, precision);
		}

		if (width > strValue.length()) {
			sbuf.setLength(0);
			int index = width - strValue.length();
			for (int i = 0; i < index; i++) {
				if ((flags & PAD_W_ZERO) != 0) {
					sbuf.append('0');
				} else {
					sbuf.append(' ');
				}
			}
			if ((LEFT_JUSTIFY & flags) != 0) {
				right = sbuf.toString();
			} else {
				left = sbuf.toString();
			}
		}

		sbuf.setLength(0);
		sbuf.append(left);
		sbuf.append(strValue);
		sbuf.append(right);
		return sbuf.toString();
	}

	/**
	 * Search through the array while the current char is a digit. When end of
	 * array occurs or the char is not a digit, stop the loop, convert the
	 * sub-array into a long. At this point update a StrtoulResult object that
	 * contains the new long value and the current pointer to the array. Returns
	 * a StrtoulResult tmp object to hold result data.
	 * 
	 * @param interp
	 *            - the current interpreter (can't be null)
	 * @param arr
	 *            - the array that contains a string representation of an int.
	 * @param endIndex
	 *            - the arr index where the numeric value begins.
	 */

	private static StrtoulResult strtoul(Interp interp, char[] arr, int endIndex) {
		int orgIndex;
		StrtoulResult strtoulResult = interp.strtoulResult;

		orgIndex = endIndex;
		for (; endIndex < arr.length; endIndex++) {
			if (!Character.isDigit(arr[endIndex])) {
				break;
			}
		}
		long lval = Long.parseLong(new String(arr, orgIndex, endIndex
				- orgIndex));
		strtoulResult.update(lval, endIndex, 0);
		return strtoulResult;
	}

	/*
	 * 
	 * Error routines:
	 */

	/**
	 * Called whenever the fmtIndex in the cmdProc is changed. It verifies the
	 * the array index is still within the bounds of the array. If no throw
	 * error.
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 * @param arr
	 *            - The array to be checked.
	 * @param index
	 *            - The new value for the array index.
	 */

	private static void checkOverFlow(Interp interp, char[] arr, int index)
			throws TclException {
		if ((index >= arr.length) || (index < 0)) {
			throw new TclException(interp,
					"\"%n$\" argument index out of range");
		}
	}

	/**
	 * Called whenever Sequential format specifiers are interlaced with XPG
	 * format specifiers in one call to cmdProc.
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static void errorMixedXPG(Interp interp) throws TclException {
		throw new TclException(interp,
				"cannot mix \"%\" and \"%n$\" conversion specifiers");
	}

	/**
	 * Called whenever the argIndex access outside the argv array. If the type
	 * is an XPG then the error message is different.
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 * @param gotXpg
	 *            - Boolean the determines if the current format is of a XPG
	 *            type or Sequential
	 */

	private static void errorBadIndex(Interp interp, boolean gotXpg)
			throws TclException {
		if (gotXpg) {
			throw new TclException(interp,
					"\"%n$\" argument index out of range");
		} else {
			throw new TclException(interp,
					"not enough arguments for all format specifiers");
		}
	}

	/**
	 * Called whenever the current char in the format array is erroneous
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 * @param fieldSpecifier
	 *            - The erroneous character
	 */

	private static void errorBadField(Interp interp, char fieldSpecifier)
			throws TclException {
		throw new TclException(interp, "bad field specifier \""
				+ fieldSpecifier + "\"");
	}

	/**
	 * Called whenever the a '%' is found but then the format string ends.
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static void errorEndMiddle(Interp interp) throws TclException {
		throw new TclException(interp,
				"format string ended in middle of field specifier");
	}

}
