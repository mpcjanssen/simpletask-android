/*
 * ScanCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ScanCmd.java,v 1.6 2009/07/23 10:42:15 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.StrtoulResult;
import tcl.lang.TCL;
import tcl.lang.TclDouble;
import tcl.lang.TclException;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclString;
import tcl.lang.Util;

/**
 * This class implements the built-in "scan" command in Tcl.
 */

public class ScanCmd implements Command {

	/*
	 * The code in this class is almost a literal translation 8.4 C Tcl's
	 * tclScan.c, with changes to eliminate goto and pointer arithmetic. We also
	 * assume that %i, %d, %x will always be resolved to a 64-bit long,
	 * regardless of %l or %L. This is consistent with a 64-bit compiled C TCL.
	 */
	/** Don't skip blanks. */
	private final int SCAN_NOSKIP = 0x1;
	/** Suppress assignment. */
	private final int SCAN_SUPPRESS = 0x2;
	/** Read an unsigned value. */
	private final int SCAN_UNSIGNED = 0x4;
	/** A width value was supplied. */
	private final int SCAN_WIDTH = 0x8;
	/** Asked for a wide value. */
	private final int SCAN_LONGER = 0x400;
	/** asked for bignum value */
	private final int SCAN_BIG = 0x800;

	/**
	 * This procedure is invoked to process the "scan" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 */

	public void cmdProc(Interp interp, TclObject objv[]) throws TclException {
		String format;
		int numVars = -1;
		int totalVars = -1;

		if (objv.length < 3) {
			throw new TclNumArgsException(interp, 1, objv, "string format ?varName varName ...?");
		}

		format = objv[2].toString();
		numVars = objv.length - 3;

		/*
		 * Check for errors in the format string
		 */
		totalVars = validateFormat(interp, format, numVars);

		/*
		 * Allocate space for the result objects
		 */
		TclObject[] objs = new TclObject[totalVars];
		for (int i = 0; i < totalVars; i++) {
			objs[i] = null;
		}

		String string = objv[1].toString();

		/*
		 * Iterate over the format string filling in the result objects until we
		 * reach the end of input, the end of the format string, or there is a
		 * mismatch.
		 */
		int objIndex = 0;
		int nconversions = 0;
		int formatIndex = 0;
		int stringIndex = 0;
		boolean underflow = false;

		DONE: while (formatIndex < format.length()) {
			char sch;
			char ch = format.charAt(formatIndex++);

			int flags = 0;

			/*
			 * If we see whitespace in the format, skip whitespace in the
			 * string.
			 */
			if (Character.isWhitespace(ch)) {
				while (stringIndex < string.length() && Character.isWhitespace(string.charAt(stringIndex))) {
					++stringIndex;
				}
				if (stringIndex == string.length()) {
					break DONE;
				}
				continue;
			}

			boolean isLiteral;

			if (ch == '%') {
				ch = format.charAt(formatIndex++);
				isLiteral = (ch == '%');
			} else {
				isLiteral = true;
			}
			if (isLiteral) {
				if (stringIndex == string.length()) {
					underflow = true;
					break DONE;
				}
				sch = string.charAt(stringIndex++);
				if (ch != sch) {
					break DONE;
				}
				continue;
			}

			/*
			 * Check for assignment suppression ('*') or an XPG3-style
			 * assignment ('%n$').
			 */
			long value;
			if (ch == '*') {
				flags |= SCAN_SUPPRESS;
				ch = format.charAt(formatIndex++);
			} else if ((ch < 0x80) && Character.isDigit(ch)) { /*
																 * INTL: "C"
																 * locale.
																 */
				Util.strtoul(format, formatIndex - 1, 10, interp.strtoulResult);
				value = interp.strtoulResult.value;
				if (format.charAt(interp.strtoulResult.index) == '$') {
					formatIndex = interp.strtoulResult.index + 1;
					ch = format.charAt(formatIndex++);
					objIndex = (int) (value - 1);
				}
			}

			/*
			 * Parse any width specifier.
			 */
			int width;
			if ((ch < 0x80) && Character.isDigit(ch)) { /* INTL: "C" locale. */
				Util.strtoul(format, formatIndex - 1, 10, interp.strtoulResult);
				width = (int) interp.strtoulResult.value;
				formatIndex = interp.strtoulResult.index;
				ch = format.charAt(formatIndex++);
			} else {
				width = 0;
			}

			/*
			 * Handle any size specifier.
			 */

			switch (ch) {
		

			 case 'l':
					// Rest of JTCL does not have bignum support, so we won't add it
					// here
				// if (formatIndex < format.length() &&
				// format.charAt(formatIndex) == 'l') {
				// flags |= SCAN_BIG;
				// formatIndex++;
				// ch = format.charAt(formatIndex++);
				// break;
				// }
			case 'L':
				flags |= SCAN_LONGER;
				/*
				 * Fall through so we skip to the next character.
				 */
			case 'h':
				ch = format.charAt(formatIndex++);
			}

			/*
			 * Handle the various field types.
			 */
			char op = ' ';

			int radix = 0;
			switch (ch) {
			case 'n':
				if ((flags & SCAN_SUPPRESS) == 0) {
					TclObject objPtr = TclInteger.newInstance(stringIndex);
					objs[objIndex++] = objPtr;
				}
				nconversions++;
				continue;

			case 'd':
				op = 'i';
				radix = 10;
				break;
			case 'i':
				op = 'i';
				radix = 0; // get it from prefix
				break;
			case 'o':
				op = 'i';
				radix = 8;
				break;
			case 'x':
				op = 'i';
				radix = 16;
				break;
			case 'b':
				op = 'i';
				radix = 2;
				break;
			case 'u':
				op = 'i';
				radix = 10;
				flags |= SCAN_UNSIGNED;
				break;

			case 'f':
			case 'e':
			case 'g':
				op = 'f';
				break;

			case 's':
				op = 's';
				break;

			case 'c':
				op = 'c';
				flags |= SCAN_NOSKIP;
				break;
			case '[':
				op = '[';
				flags |= SCAN_NOSKIP;
				break;
			}

			/*
			 * At this point, we will need additional characters from the string
			 * to proceed.
			 */

			if (stringIndex >= string.length()) {
				underflow = true;
				break DONE;
			}

			/*
			 * Skip any leading whitespace at the beginning of a field unless
			 * the format suppresses this behavior.
			 */

			if ((flags & SCAN_NOSKIP) == 0) {
				while (stringIndex < string.length()) {
					if (Character.isWhitespace(string.charAt(stringIndex))) {
						++stringIndex;
					} else {
						break;
					}
				}
				if (stringIndex == string.length()) {
					underflow = true;
					break DONE;
				}
			}

			/*
			 * Perform the requested scanning operation.
			 */

			switch (op) {
			case 's':
				/*
				 * Scan a string up to width characters or whitespace.
				 */
				if (width == 0) {
					width = Integer.MAX_VALUE;
				}
				int end = stringIndex;
				while (end < string.length()) {
					sch = string.charAt(end);
					if (Character.isWhitespace(sch)) {
						break;
					}
					end++;
					if (--width == 0) {
						break;
					}
				}

				if ((flags & SCAN_SUPPRESS) == 0) {
					TclObject objPtr = TclString.newInstance(string.substring(stringIndex, end));
					objs[objIndex++] = objPtr;
				}
				stringIndex = end;
				break;

			case '[': {
				CharSet cset = new CharSet(format, formatIndex);
				formatIndex = cset.getEndOfFormat();

				if (width == 0) {
					width = Integer.MAX_VALUE;
				}
				end = stringIndex;

				while (end < string.length()) {
					sch = string.charAt(end);
					if (!cset.charInSet(sch)) {
						break;
					}
					++end;
					if (--width == 0) {
						break;
					}
				}

				if (stringIndex == end) {
					/*
					 * Nothing matched the range, stop processing.
					 */
					break DONE;
				}

				if ((flags & SCAN_SUPPRESS) == 0) {
					TclObject objPtr = TclString.newInstance(string.substring(stringIndex, end));
					objs[objIndex++] = objPtr;
				}
				stringIndex = end;
				break;
			}

			case 'c':
				/*
				 * Scan a single Unicode character.
				 */
				sch = string.charAt(stringIndex++);
				if ((flags & SCAN_SUPPRESS) == 0) {
					TclObject objPtr = TclInteger.newInstance(sch);
					objs[objIndex++] = objPtr;
				}
				break;

			case 'i':
				/*
				 * Scan an unsigned or signed integer.
				 */
				if (width == 0)
					Util.strtoul(string, stringIndex, radix, interp.strtoulResult);
				else {
					if (stringIndex + width > string.length()) {
						underflow = true;
						break DONE;
					}
					String truncString = string.substring(0, stringIndex + width);
					Util.strtoul(truncString, stringIndex, radix, interp.strtoulResult);
				}
				if (interp.strtoulResult.errno != TCL.INVALID_INTEGER) {
					long v;
					if (interp.strtoulResult.errno == TCL.INTEGER_RANGE) {
						v = -1;
					} else {
						v = interp.strtoulResult.value;
					}
					stringIndex = interp.strtoulResult.index;
					if ((flags & SCAN_SUPPRESS) == 0) {
						objs[objIndex++] = TclInteger.newInstance(v);
					}
				} else {
					if (width == 1 || string.length() == 1) {
						/* special case when we underflow; see scan-4.44 */
						underflow = (string.charAt(stringIndex) == '-' || string.charAt(stringIndex) == '+');
					}
					break DONE;
				}

				break;

			case 'f':
				/*
				 * scan a floating point number
				 */

				if (width == 0)
					Util.strtod(string, stringIndex, -1, interp.strtodResult);
				else {
					if (stringIndex + width > string.length()) {
						underflow = true;
						break DONE;
					}
					String truncString = string.substring(0, stringIndex + width);
					Util.strtod(truncString, stringIndex, -1, interp.strtodResult);
				}

				if (interp.strtodResult.errno == 0) {
					stringIndex = interp.strtodResult.index;
					if ((flags & SCAN_SUPPRESS) == 0) {
						objs[objIndex++] = TclDouble.newInstance(interp.strtodResult.value);
					}
				} else {
					if (width == 1 || string.length() == 1) {
						/* special case when we underflow; see scan-4.55 */
						underflow = (string.charAt(stringIndex) == '-' || string.charAt(stringIndex) == '+');
					}
					break DONE;
				}

				break;
			}
			nconversions++;
		}

		int result = 0;
		if (numVars > 0) {
			/*
			 * In this case, variables were specified (classic scan).
			 */
			StringBuffer varErrors = null;
			for (int i = 0; i < totalVars; i++) {
				if (objs[i] == null) {
					continue;
				}
				result++;
				try {
					interp.setVar(objv[i + 3].toString(), objs[i], 0);
				} catch (TclException e) {
					// custom error message
					if (varErrors == null)
						varErrors = new StringBuffer();
					// yes, errors really do get appended with no spaces
					// between. See scan.test scan-4.61
					varErrors.append("couldn't set variable \"").append(objv[i + 3].toString()).append('"');
				}
			}
			if (varErrors != null)
				throw new TclException(interp, varErrors.toString());
		} else {
			/*
			 * Here no vars were specified, we want a list returned (inline
			 * scan)
			 */
			TclObject objPtr = TclList.newInstance();
			for (int i = 0; i < totalVars; i++) {
				if (objs[i] != null) {
					TclList.append(interp, objPtr, objs[i]);
				} else {
					/*
					 * More %-specifiers than matching chars, so we just spit
					 * out empty strings for these.
					 */
					TclList.append(interp, objPtr, TclString.newInstance(""));
				}
			}
			interp.setResult(objPtr);
		}
		if (underflow && (nconversions == 0)) {
			if (numVars > 0) {
				interp.setResult(-1);
			} else {
				interp.setResult("");
			}
		} else if (numVars > 0) {
			interp.setResult(result);
		}
	}

	/**
	 * Parse the format string and verify that it is properly formed and that
	 * there are exactly enough variables on the command line.
	 * 
	 * @param interp
	 * 			   The current interpreter
	 * @param format
	 *            The format string
	 * @param numVars
	 *            The number of variables passed to the scan command
	 * @return Number of variables that will be required.
	 * @throws TclException
	 *             if any invalid scan format is encountered.
	 */
	private int validateFormat(Interp interp, String format, int numVars) throws TclException {
		boolean gotXpg = false;
		boolean gotSequential = false;
		int xpgSize = 0;
		int objIndex = 0;
		int flags = 0;
		int[] nassign = new int[numVars == 0 ? 1 : numVars];
		int value;

		/*
		 * Initialize an array that records the number of times a variable is
		 * assigned to by the format string. We use this to detect if a variable
		 * is multiply assigned or left unassigned.
		 */
		for (int i = 0; i < nassign.length; i++) {
			nassign[i] = 0;
		}
		int formatIndex = 0;
		while (formatIndex < format.length()) {
			char ch = format.charAt(formatIndex++);

			flags = 0;

			if (ch != '%') {
				continue;
			}

			ch = format.charAt(formatIndex++);
			if (ch == '%') {
				continue;
			}
			if (ch == '*') {
				flags |= SCAN_SUPPRESS;
				ch = format.charAt(formatIndex++);
			} else {
				if (ch < 0x80 && Character.isDigit(ch)) {
					/*
					 * Check for an XPG3-style %n$ specification. Note: there
					 * must not be a mixture of XPG3 specs and non-XPG3 specs in
					 * the same format string.
					 */
					StrtoulResult result = new StrtoulResult();
					Util.strtoul(format, formatIndex - 1, 10, result);
					int endIndex = result.index;
					if (endIndex >= format.length()) {
						// ran out of format characters before finding conversion character
						throw new TclException(interp, "bad scan conversion character \"\"");
					}
					if (format.charAt(endIndex) != '$') {
						/* notXpg */
						gotSequential = true;
						if (gotXpg) {
							errorBadField(interp, '$');
						}
					} else {
						formatIndex = endIndex + 1;
						ch = format.charAt(formatIndex++);
						gotXpg = true;
						if (gotSequential) {
							errorBadField(interp, '$');
						}
						value = (int) result.value;
						objIndex = value - 1;
						if ((objIndex < 0) || (numVars != 0 && (objIndex >= numVars))) {
							errorDiffVars(interp, gotXpg);
						} else if (numVars == 0) {
							/*
							 * In the case where no vars are specified, the user
							 * can specify %9999$ legally, so we have to
							 * consider special rules for growing the assign
							 * array. 'value' is guaranteed to be > 0.
							 */
							xpgSize = (xpgSize > (int) result.value) ? xpgSize : (int) result.value;

						}

					}
				} else {
					/* notGpg: */
					gotSequential = true;
					if (gotXpg) {
						errorCannotMix(interp, '$');
					}
				}
			}

			/* xpgCheckDone: */

			/*
			 * Parse any width specifier.
			 */

			if ((ch < 0x80) && Character.isDigit(ch)) {
				StrtoulResult result = new StrtoulResult();
				Util.strtoul(format, formatIndex - 1, 10, result);
				if (result.errno != 0) {
					value = 0;
				} else {
					value = (int) result.value;
				}
				formatIndex = result.index;
				flags |= SCAN_WIDTH;
				ch = format.charAt(formatIndex++);
			}

			/*
			 * Handle any size specifier.
			 */

			switch (ch) {
			 
			 case 'l':
//				don't support bignum yet, since rest of JTCL does not
//			 if (format.charAt(formatIndex)=='l') {
//			 flags |= SCAN_BIG;
//			 ++formatIndex;
//			 ch = format.charAt(formatIndex++);
//			 break;
//			 }
			case 'L':
				flags |= SCAN_LONGER;
			case 'h':
				ch = format.charAt(formatIndex++);
			}

			if (!((flags & SCAN_SUPPRESS) != 0) && numVars > 0 && (objIndex >= numVars)) {
				errorDiffVars(interp, gotXpg);
			}

			/*
			 * Handle the various field types.
			 */

			switch (ch) {

			case 'c':
				if ((flags & SCAN_WIDTH) != 0) {
					errorCharFieldWidth(interp);
				}
				// Fall through !

			case 'n':
			case 's':
				if ((flags & (SCAN_LONGER | SCAN_BIG)) != 0) {
					errorLonger(interp, ch);
				}
				// Fall through !
			case 'd':
			case 'e':
			case 'f':
			case 'g':
			case 'i':
			case 'o':
			case 'x':
			case 'b':
				break;
			case 'u':
				if ((flags & SCAN_BIG) != 0) {
					throw new TclException(interp, "unsigned bignum scans are invalid");
				}
				break;

			/*
			 * Bracket terms need special checking
			 */
			case '[':
				if ((flags & (SCAN_LONGER | SCAN_BIG)) != 0) {
					errorLonger(interp, ch);
				}
				if (formatIndex >= format.length()) {
					errorBadSet(interp);
				}
				ch = format.charAt(formatIndex++);
				if (ch == '^') {
					if (formatIndex >= format.length()) {
						errorBadSet(interp);
					}
					ch = format.charAt(formatIndex++);
				}
				if (ch == ']') {
					if (formatIndex >= format.length()) {
						errorBadSet(interp);
					}
					ch = format.charAt(formatIndex++);
				}
				while (ch != ']') {
					if (formatIndex >= format.length()) {
						errorBadSet(interp);
					}
					ch = format.charAt(formatIndex++);
				}

				break;

			default:
				errorBadConvChar(interp, ch);
			}

			if (!((flags & SCAN_SUPPRESS) != 0)) {
				if (objIndex >= nassign.length) {
					/*
					 * Expand the nassign buffer. If we are using XPG
					 * specifiers, make sure that we grow to a large enough
					 * size. xpgSize is guaranteed to be at least one larger
					 * than objIndex.
					 */
					int nspace;
					if (xpgSize > 0)
						nspace = xpgSize;
					else
						nspace = nassign.length + 16;

					int[] newNassign = new int[nspace];
					System.arraycopy(nassign, 0, newNassign, 0, nassign.length);
					for (int i = nassign.length; i < newNassign.length; i++) {
						newNassign[i] = 0;
					}
					nassign = newNassign;
				}
				nassign[objIndex]++;
				objIndex++;
			}

		}

		/*
		 * Verify that all of the variable were assigned exactly once.
		 */

		if (numVars == 0) {
			if (xpgSize != 0) {
				numVars = xpgSize;
			} else {
				numVars = objIndex;
			}
		}

		for (int i = 0; i < numVars; i++) {
			if (nassign[i] > 1) {
				errorMultipleAssignments(interp);
			} else if (xpgSize == 0 && nassign[i] == 0) {
				/*
				 * If the space is empty, and xpgSize is 0 (means XPG wasn't
				 * used, and/or numVars != 0), then too many vars were given
				 */
				errorNotAssigned(interp);
			}
		}

		return numVars; /* equiv to C Tcl *totalSubs */
	}

	/**
	 * Called whenever the number of varName args do not match the number of
	 * found and valid formatSpecifiers (matched and unmatched).
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static final void errorDiffVars(Interp interp, boolean gotXpg) throws TclException {

		if (gotXpg) {
			throw new TclException(interp, "\"%n$\" argument index out of range");
		} else {
			throw new TclException(interp, "different numbers of variable names and field specifiers");
		}
	}

	/**
	 * Called whenever the current char in the frmtArr is erroneous
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 * @param fieldSpecifier
	 *            - The erroneous character
	 */

	private static final void errorCannotMix(Interp interp, char fieldSpecifier) throws TclException {
		throw new TclException(interp, "cannot mix \"%\" and \"%n" + fieldSpecifier + "\" conversion specifiers");
	}

	/**
	 * Called whenever the current char in the frmtArr is erroneous
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 * @param fieldSpecifier
	 *            - The erroneous character
	 */

	private static final void errorBadField(Interp interp, char fieldSpecifier) throws TclException {
		throw new TclException(interp, "cannot mix \"%\" and \"%n" + fieldSpecifier + "\" conversion specifiers");
	}

	/**
	 * Called whenever the a width field is used in a char ('c') format
	 * specifier
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static final void errorCharFieldWidth(Interp interp) throws TclException {
		throw new TclException(interp, "field width may not be specified in %c conversion");
	}

	/**
	 * Called whenever a long conversion is invalid specifier
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static final void errorLonger(Interp interp, char ch) throws TclException {
		throw new TclException(interp, "'l' modifier may not be specified in " + ch + " conversion");
	}

	/**
	 * Called whenever a set is invalid
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static final void errorBadSet(Interp interp) throws TclException {
		throw new TclException(interp, "unmatched [ in format string");
	}

	/**
	 * Called whenever a bad conversion character is found
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static final void errorBadConvChar(Interp interp, char ch) throws TclException {
		throw new TclException(interp, "bad scan conversion character \"" + ch + "\"");
	}

	/**
	 * Called whenever a variable is assigned multiple times
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static final void errorMultipleAssignments(Interp interp) throws TclException {
		throw new TclException(interp, "variable is assigned by multiple \"%n$\" conversion specifiers");
	}

	/**
	 * Called whenever a variable is not assigned
	 * 
	 * @param interp
	 *            - The TclInterp which called the cmdProc method .
	 */

	private static final void errorNotAssigned(Interp interp) throws TclException {
		throw new TclException(interp, "variable is not assigned by any conversion specifiers");
	}

	/**
	 * Encapsulates a character set such as [ab0-9]
	 * 
	 */
	private static class CharSet {
		/**
		 * True if CharSet starts with '^' indicating it has chars to be
		 * excluded
		 */
		boolean exclude = false;
		/**
		 * List of chars in the character set
		 */
		String chars = null;
		/**
		 * Ranges of chars in the character set; may be null
		 */
		Range[] ranges = null;
		/**
		 * Index of the first character after the end of the charset's ']' in
		 * the format string passed to the constructor
		 */
		int endOfFormat;

		/**
		 * Create a new CharSet
		 * 
		 * @param format
		 *            Format string which may contain other format information
		 * @param formatIndex
		 *            index of first character in format after opening '[' of
		 *            Charset
		 */
		CharSet(String format, int formatIndex) {
			char ch;
			int offset = 0;
			int endIndex = 0;

			ch = format.charAt(formatIndex);
			offset = 1;
			if (ch == '^') {
				exclude = true;
				formatIndex += offset;
				ch = format.charAt(formatIndex);
				offset = 1;
			}
			endIndex = formatIndex + offset;
			/*
			 * Find the close bracket, but get past the first one
			 */
			if (ch == ']') {
				ch = format.charAt(endIndex++);
			}
			int nranges = 0;

			while (ch != ']' && endIndex < format.length()) {
				if (ch == '-')
					nranges++;
				ch = format.charAt(endIndex++);
			}
			StringBuilder charsbuf = new StringBuilder();
			if (nranges > 0) {
				ranges = new Range[nranges];
			} else {
				ranges = null;
			}
			nranges = 0;

			ch = format.charAt(formatIndex++);
			char start = ch;
			if (ch == ']' || ch == '-') {
				charsbuf.append(ch);
				ch = format.charAt(formatIndex++);
			}
			while (ch != ']') {
				char nextChar = (formatIndex < format.length()) ? format.charAt(formatIndex) : 0;
				if (nextChar == '-') {
					/*
					 * This may be first char of a range, so don't add it yet
					 */
					start = ch;
				} else if (ch == '-') {
					/*
					 * Check to see if this is the last character in the set, in
					 * which case it is not a range and we should add the
					 * previous character as well as the dash.
					 */
					if (nextChar == ']') {
						charsbuf.append(start);
						charsbuf.append(ch);
					} else {
						ch = format.charAt(formatIndex++);
						ranges[nranges] = new Range(start, ch);
						nranges++;
					}
				} else {
					charsbuf.append(ch);
				}

				ch = format.charAt(formatIndex++);
			}
			endOfFormat = formatIndex;
			chars = charsbuf.toString();
		}

		/**
		 * @return the next character to continue processing from in the format
		 *         string
		 */
		int getEndOfFormat() {
			return endOfFormat;
		}

		/**
		 * 
		 * @param ch
		 *            character to test
		 * @return true if a character is included in the character set
		 */
		private boolean charInSet(char ch) {
			boolean match = false;
			if (chars.indexOf(ch) >= 0) {
				match = true;
			} else if (ranges != null) {
				for (Range range : ranges) {
					if (range != null && range.isInRange(ch)) {
						match = true;
						break;
					}
				}
			}

			return exclude ? !match : match;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			if (exclude)
				sb.append('^');
			if (chars != null)
				sb.append(chars);
			if (ranges != null) {
				for (Range r : ranges) {
					if (r != null)
						sb.append(r.start).append('-').append(r.end);
				}
			}
			sb.append(']');
			return sb.toString();
		}
	}

	/**
	 * Represents a range of characters, such as [a-z]
	 * 
	 */
	private static class Range {
		char start;
		char end;

		/**
		 * Create a new Range of characters
		 * 
		 * @param a
		 *            one character in range
		 * @param b
		 *            other character in range
		 */            
		Range(char a, char b) {
			if (a < b) {
				start = a;
				end = b;
			} else {
				start = b;
				end = a;
			}
		}

		/**
		 * Test if a character is within the range of this Range
		 * 
		 * @param c
		 *            character to test
		 * @return true if character is inclusively within this Range
		 */
		final boolean isInRange(char c) {
			return (c >= start && c <= end);
		}
	}
}