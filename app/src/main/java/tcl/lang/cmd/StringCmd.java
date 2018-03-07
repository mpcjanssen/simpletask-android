/*
 * StringCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-2000 Scriptics Corporation.
 * Copyright (c) 2000 Christian Krone.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: StringCmd.java,v 1.16 2006/06/13 06:52:47 mdejong Exp $
 *
 */

package tcl.lang.cmd;

import tcl.lang.Command;
import tcl.lang.Expression;
import tcl.lang.Interp;
import tcl.lang.StrtodResult;
import tcl.lang.StrtoulResult;
import tcl.lang.TCL;
import tcl.lang.TclByteArray;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclRuntimeError;
import tcl.lang.TclString;
import tcl.lang.Util;

/**
 * This class implements the built-in "string" command in Tcl.
 */

public class StringCmd implements Command {

	static final private String options[] = { "bytelength", "compare", "equal",
			"first", "index", "is", "last", "length", "map", "match", "range",
			"repeat", "replace", "tolower", "toupper", "totitle", "trim",
			"trimleft", "trimright", "wordend", "wordstart" };
	static final private int STR_BYTELENGTH = 0;
	static final private int STR_COMPARE = 1;
	static final private int STR_EQUAL = 2;
	static final private int STR_FIRST = 3;
	static final private int STR_INDEX = 4;
	static final private int STR_IS = 5;
	static final private int STR_LAST = 6;
	static final private int STR_LENGTH = 7;
	static final private int STR_MAP = 8;
	static final private int STR_MATCH = 9;
	static final private int STR_RANGE = 10;
	static final private int STR_REPEAT = 11;
	static final private int STR_REPLACE = 12;
	static final private int STR_TOLOWER = 13;
	static final private int STR_TOUPPER = 14;
	static final private int STR_TOTITLE = 15;
	static final private int STR_TRIM = 16;
	static final private int STR_TRIMLEFT = 17;
	static final private int STR_TRIMRIGHT = 18;
	static final private int STR_WORDEND = 19;
	static final private int STR_WORDSTART = 20;

	static final private String isOptions[] = { "alnum", "alpha", "ascii",
			"control", "boolean", "digit", "double", "false", "graph",
			"integer", "lower", "print", "punct", "space", "true", "upper","wideinteger",
			"wordchar", "xdigit" };
	static final private int STR_IS_ALNUM = 0;
	static final private int STR_IS_ALPHA = 1;
	static final private int STR_IS_ASCII = 2;
	static final private int STR_IS_CONTROL = 3;
	static final private int STR_IS_BOOL = 4;
	static final private int STR_IS_DIGIT = 5;
	static final private int STR_IS_DOUBLE = 6;
	static final private int STR_IS_FALSE = 7;
	static final private int STR_IS_GRAPH = 8;
	static final private int STR_IS_INT = 9;
	static final private int STR_IS_LOWER = 10;
	static final private int STR_IS_PRINT = 11;
	static final private int STR_IS_PUNCT = 12;
	static final private int STR_IS_SPACE = 13;
	static final private int STR_IS_TRUE = 14;
	static final private int STR_IS_UPPER = 15;
	static final private int STR_IS_WIDEINTEGER = 16;
	static final private int STR_IS_WORD = 17;
	static final private int STR_IS_XDIGIT = 18;

	/**
	 * Java's Character class has a many boolean test functions to check the
	 * kind of a character (like isLowerCase() or isISOControl()). Unfortunately
	 * some are missing (like isPunct() or isPrint()), so here we define bitsets
	 * to compare the result of Character.getType().
	 */

	static final private int ALPHA_BITS = ((1 << Character.UPPERCASE_LETTER)
			| (1 << Character.LOWERCASE_LETTER)
			| (1 << Character.TITLECASE_LETTER)
			| (1 << Character.MODIFIER_LETTER) | (1 << Character.OTHER_LETTER));
	static final private int PUNCT_BITS = ((1 << Character.CONNECTOR_PUNCTUATION)
			| (1 << Character.DASH_PUNCTUATION)
			| (1 << Character.START_PUNCTUATION)
			| (1 << Character.END_PUNCTUATION) | (1 << Character.OTHER_PUNCTUATION));
	static final private int PRINT_BITS = (ALPHA_BITS
			| (1 << Character.DECIMAL_DIGIT_NUMBER)
			| (1 << Character.SPACE_SEPARATOR)
			| (1 << Character.LINE_SEPARATOR)
			| (1 << Character.PARAGRAPH_SEPARATOR)
			| (1 << Character.NON_SPACING_MARK)
			| (1 << Character.ENCLOSING_MARK)
			| (1 << Character.COMBINING_SPACING_MARK)
			| (1 << Character.LETTER_NUMBER) | (1 << Character.OTHER_NUMBER)
			| PUNCT_BITS | (1 << Character.MATH_SYMBOL)
			| (1 << Character.CURRENCY_SYMBOL)
			| (1 << Character.MODIFIER_SYMBOL) | (1 << Character.OTHER_SYMBOL));
	static final private int WORD_BITS = (ALPHA_BITS
			| (1 << Character.DECIMAL_DIGIT_NUMBER) | (1 << Character.CONNECTOR_PUNCTUATION));

	/**
	 *----------------------------------------------------------------------
	 * 
	 * Tcl_StringObjCmd -> StringCmd.cmdProc
	 * 
	 * This procedure is invoked to process the "string" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 *----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Argument list.
			throws TclException // A standard Tcl exception.
	{
		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"option arg ?arg ...?");
		}
		int index = TclIndex.get(interp, objv[1], options, "option", 0);

		switch (index) {
		case STR_EQUAL:
		case STR_COMPARE: {

			if (objv.length < 4 || objv.length > 7) {
				throw new TclNumArgsException(interp, 2, objv,
						"?-nocase? ?-length int? string1 string2");
			}

			boolean nocase = false;
			int reqlength = -1;
			for (int i = 2; i < objv.length - 2; i++) {
				String string2 = objv[i].toString();
				int length2 = string2.length();
				if ((length2 > 1) && "-nocase".startsWith(string2)) {
					nocase = true;
				} else if ((length2 > 1) && "-length".startsWith(string2)) {
					if (i + 1 >= objv.length - 2) {
						throw new TclNumArgsException(interp, 2, objv,
								"?-nocase? ?-length int? string1 string2");
					}
					reqlength = TclInteger.getInt(interp, objv[++i]);
				} else {
					throw new TclException(interp, "bad option \"" + string2
							+ "\": must be -nocase or -length");
				}
			}

			String string1 = objv[objv.length - 2].toString();
			String string2 = objv[objv.length - 1].toString();
			int length1 = string1.length();
			int length2 = string2.length();

			// This is the min length IN BYTES of the two strings

			int length = (length1 < length2) ? length1 : length2;

			int match;

			if (reqlength == 0) {
				// Anything matches at 0 chars, right?

				match = 0;
			} else if (nocase || ((reqlength > 0) && (reqlength <= length))) {
				// In Java, strings are always encoded in unicode, so we do
				// not need to worry about individual char lengths

				// Do the reqlength check again, against 0 as well for
				// the benfit of nocase

				if ((reqlength > 0) && (reqlength < length)) {
					length = reqlength;
				} else if (reqlength < 0) {
					// The requested length is negative, so we ignore it by
					// setting it to the longer of the two lengths.

					reqlength = (length1 > length2) ? length1 : length2;
				}
				if (nocase) {
					string1 = string1.toLowerCase();
					string2 = string2.toLowerCase();
				}
				match = string1.substring(0, length).compareTo(
						string2.substring(0, length));

				if ((match == 0) && (reqlength > length)) {
					match = length1 - length2;
				}

			} else {
				match = string1.substring(0, length).compareTo(
						string2.substring(0, length));
				if (match == 0) {
					match = length1 - length2;
				}
			}

			if (index == STR_EQUAL) {
				interp.setResult((match != 0) ? false : true);
			} else {
				interp.setResult(((match > 0) ? 1 : (match < 0) ? -1 : 0));
			}
			break;
		}

		case STR_FIRST: {
			if (objv.length < 4 || objv.length > 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"subString string ?startIndex?");
			}
			String string1 = objv[2].toString();
			String string2 = objv[3].toString();
			int length1 = string1.length();
			int length2 = string2.length();

			int start = 0;

			if (objv.length == 5) {
				// If a startIndex is specified, we will need to fast
				// forward to that point in the string before we think
				// about a match.

				start = Util.getIntForIndex(interp, objv[4], length2 - 1);
				if (start >= length2) {
					interp.setResult(-1);
					break;
				}
			}

			if (length1 == 0) {
				interp.setResult(-1);
			} else if (length1 == 1) {
				char c = string1.charAt(0);
				int result = string2.indexOf(c, start);
				interp.setResult(result);
			} else {
				int result = string2.indexOf(string1, start);
				interp.setResult(result);
			}
			break;
		}

		case STR_INDEX: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv,
						"string charIndex");
			}

			String string1 = objv[2].toString();
			int length1 = string1.length();

			int i = Util.getIntForIndex(interp, objv[3], length1 - 1);

			if ((i >= 0) && (i < length1)) {
				// Get char at the given index. Check for a
				// common TclObject that represents this
				// single character, and allocate a new
				// TclString if not found.

				TclObject obj = interp.checkCommonCharacter(string1.charAt(i));
				if (obj == null) {
					obj = TclString.newInstance(string1.substring(i, i + 1));
				}
				interp.setResult(obj);
			}
			break;
		}

		case STR_IS: {
			if (objv.length < 4 || objv.length > 7) {
				throw new TclNumArgsException(interp, 2, objv,
						"class ?-strict? ?-failindex var? str");
			}
			index = TclIndex.get(interp, objv[2], isOptions, "class", 0);

			boolean strict = false;
			TclObject failVarObj = null;

			if (objv.length != 4) {
				for (int i = 3; i < objv.length - 1; i++) {
					String string2 = objv[i].toString();
					int length2 = string2.length();
					if ((length2 > 1) && "-strict".startsWith(string2)) {
						strict = true;
					} else if ((length2 > 1)
							&& "-failindex".startsWith(string2)) {
						if (i + 1 >= objv.length - 1) {
							throw new TclNumArgsException(interp, 3, objv,
									"?-strict? ?-failindex var? str");
						}
						failVarObj = objv[++i];
					} else {
						throw new TclException(interp, "bad option \""
								+ string2 + "\": must be -strict or -failindex");
					}
				}
			}

			boolean result = true;
			int failat = 0;

			// We get the objPtr so that we can short-cut for some classes
			// by checking the object type (int and double), but we need
			// the string otherwise, because we don't want any conversion
			// of type occuring (as, for example, Tcl_Get*FromObj would do

			TclObject obj = objv[objv.length - 1];
			String string1 = obj.toString();
			int length1 = string1.length();
			if (length1 == 0) {
				if (strict) {
					result = false;
				}
				interp.setResult(result);
				return;
			}

			switch (index) {
			case STR_IS_BOOL:
			case STR_IS_TRUE:
			case STR_IS_FALSE: {
				try {
					boolean i = Util.getBoolean(null, string1);
					if (((index == STR_IS_TRUE) && !i)
							|| ((index == STR_IS_FALSE) && i)) {
						result = false;
					}
				} catch (TclException e) {
					result = false;
				}
				break;
			}
			case STR_IS_DOUBLE: {
				if (obj.isDoubleType() || obj.isIntType()) {
					break;
				}

				// This is adapted from Tcl_GetDouble
				//
				// The danger in this function is that
				// "12345678901234567890" is an acceptable 'double',
				// but will later be interp'd as an int by something
				// like [expr]. Therefore, we check to see if it looks
				// like an int, and if so we do a range check on it.
				// If strtoul gets to the end, we know we either
				// received an acceptable int, or over/underflow

				if (Expression.looksLikeInt(string1, length1, 0, false)) {
					StrtoulResult res = interp.strtoulResult;
					Util.strtoul(string1, 0, 0, res);
					if (res.index == length1) {
						if (res.errno == TCL.INTEGER_RANGE) {
							result = false;
							failat = -1;
						}
						break;
					}
				}

				StrtodResult res = interp.strtodResult;
				Util.strtod(string1, 0, -1, res);
				if (res.errno == TCL.DOUBLE_RANGE) {
					// if (errno == ERANGE), then it was an over/underflow
					// problem, but in this method, we only want to know
					// yes or no, so bad flow returns 0 (false) and sets
					// the failVarObj to the string length.

					result = false;
					failat = -1;
				} else if (res.index == 0) {
					// In this case, nothing like a number was found

					result = false;
					failat = 0;
				} else {
					// Go onto SPACE, since we are
					// allowed trailing whitespace

					failat = res.index;
					for (int i = res.index; i < length1; i++) {
						if (!Character.isWhitespace(string1.charAt(i))) {
							result = false;
							break;
						}
					}
				}
				break;
			}
			case STR_IS_INT: {
				if (obj.isIntType()) {
					if (! TclInteger.isWithinIntRange(interp, obj)) {
						failat = -1;
						result = false;
						break;
					} else {
						break;  // integer, in range, must be ok
					}
				}

				StrtoulResult res = interp.strtoulResult;
				Util.strtoul(string1, 0, 0, res);
				if (res.errno == TCL.INTEGER_RANGE || res.value > Integer.MAX_VALUE || res.value < Integer.MIN_VALUE) {
					// if (errno == ERANGE), then it was an over/underflow
					// problem, but in this method, we only want to know
					// yes or no, so bad flow returns false and sets
					// the failVarObj to the string length.

					result = false;
					failat = -1;
				} else if (res.index == 0) {
					// In this case, nothing like a number was found

					result = false;
					failat = 0;
				} else {
					// Go onto SPACE, since we are
					// allowed trailing whitespace

					failat = res.index;
					for (int i = res.index; i < length1; i++) {
						if (!Character.isWhitespace(string1.charAt(i))) {
							result = false;
							break;
						}
					}
				}
				break;
			}
			case STR_IS_WIDEINTEGER: {
				if (obj.isIntType()) {
					if (! TclInteger.isWithinLongRange(interp, obj)) {
						failat = -1;
						result = false;
						break;
					} else {
						break; // is integer, within long range, so it's ok
					}
				}

				StrtoulResult res = interp.strtoulResult;
				Util.strtoul(string1, 0, 0, res);
				if (res.errno == TCL.INTEGER_RANGE) {
					// if (errno == ERANGE), then it was an over/underflow
					// problem, but in this method, we only want to know
					// yes or no, so bad flow returns false and sets
					// the failVarObj to the string length.

					result = false;
					failat = -1;
				} else if (res.index == 0) {
					// In this case, nothing like a number was found

					result = false;
					failat = 0;
				} else {
					// Go onto SPACE, since we are
					// allowed trailing whitespace

					failat = res.index;
					for (int i = res.index; i < length1; i++) {
						if (!Character.isWhitespace(string1.charAt(i))) {
							result = false;
							break;
						}
					}
				}
				break;
			}
			default: {
				for (failat = 0; failat < length1; failat++) {
					char c = string1.charAt(failat);
					switch (index) {
					case STR_IS_ASCII:
						// This is a valid check in unicode, because
						// all bytes < 0xC0 are single byte chars
						// (but isascii limits that def'n to 0x80).

						result = c < 0x80;
						break;
					case STR_IS_ALNUM:
						result = Character.isLetterOrDigit(c);
						break;
					case STR_IS_ALPHA:
						result = Character.isLetter(c);
						break;
					case STR_IS_DIGIT:
						result = Character.isDigit(c);
						break;
					case STR_IS_GRAPH:
						result = ((1 << Character.getType(c)) & PRINT_BITS) != 0
								&& c != ' ';
						break;
					case STR_IS_PRINT:
						result = ((1 << Character.getType(c)) & PRINT_BITS) != 0;
						break;
					case STR_IS_PUNCT:
						result = ((1 << Character.getType(c)) & PUNCT_BITS) != 0;
						break;
					case STR_IS_UPPER:
						result = Character.isUpperCase(c);
						break;
					case STR_IS_SPACE:
						result = Character.isWhitespace(c);
						break;
					case STR_IS_CONTROL:
						result = Character.isISOControl(c);
						break;
					case STR_IS_LOWER:
						result = Character.isLowerCase(c);
						break;
					case STR_IS_WORD:
						result = ((1 << Character.getType(c)) & WORD_BITS) != 0;
						break;
					case STR_IS_XDIGIT:
						result = Character.digit(c, 16) >= 0;
						break;
					default:
						throw new TclRuntimeError("unimplemented");
					}
					if (!result) {
						break;
					}
				}
			}
			}

			// Only set the failVarObj when we will return 0
			// and we have indicated a valid fail index (>= 0)

			if ((!result) && (failVarObj != null)) {
				interp.setVar(failVarObj.toString(), null, failat, 0);
			}
			interp.setResult(result);
			break;
		}

		case STR_LAST: {
			if (objv.length < 4 || objv.length > 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"subString string ?startIndex?");
			}
			String string1 = objv[2].toString();
			String string2 = objv[3].toString();
			int length1 = string1.length();
			int length2 = string2.length();

			int last = 0;
			if (objv.length == 5) {
				// If a lastIndex is specified, we will need to restrict the
				// string range to that char index in the string.

				last = Util.getIntForIndex(interp, objv[4], length2 - 1);
				if (last < 0) {
					interp.setResult(-1);
					break;
				} else if (last < length2) {
					string2 = string2.substring(0, last + 1);
				}
			}

			if (length1 == 0) {
				interp.setResult(-1);
			} else if (length1 == 1) {
				char c = string1.charAt(0);
				int result = string2.lastIndexOf(c);
				interp.setResult(result);
			} else {
				int result = string2.lastIndexOf(string1);
				interp.setResult(result);
			}
			break;
		}

		case STR_BYTELENGTH:
			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "string");
			}
			if (objv[2].isByteArrayType()) {
				interp.setResult(TclByteArray.getLength(interp, objv[2]));
			} else {
				interp.setResult(Utf8Count(objv[2].toString()));
			}
			break;

		case STR_LENGTH: {
			if (objv.length != 3) {
				throw new TclNumArgsException(interp, 2, objv, "string");
			}
			if (objv[2].isByteArrayType()) {
				interp.setResult(TclByteArray.getLength(interp, objv[2]));
			} else {
				interp.setResult(objv[2].toString().length());
			}
			break;
		}

		case STR_MAP: {
			if (objv.length < 4 || objv.length > 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"?-nocase? charMap string");
			}

			boolean nocase = false;
			if (objv.length == 5) {
				String string2 = objv[2].toString();
				int length2 = string2.length();
				if ((length2 > 1) && "-nocase".startsWith(string2)) {
					nocase = true;
				} else {
					throw new TclException(interp, "bad option \"" + string2
							+ "\": must be -nocase");
				}
			}

			TclObject mapElemv[] = TclList.getElements(interp,
					objv[objv.length - 2]);
			if (mapElemv.length == 0) {
				// empty charMap, just return whatever string was given

				interp.setResult(objv[objv.length - 1]);
			} else if ((mapElemv.length % 2) != 0) {
				// The charMap must be an even number of key/value items

				throw new TclException(interp, "char map list unbalanced");
			}
			String string1 = objv[objv.length - 1].toString();
			String cmpString1;
			if (nocase) {
				cmpString1 = string1.toLowerCase();
			} else {
				cmpString1 = string1;
			}
			int length1 = string1.length();
			if (length1 == 0) {
				// Empty input string, just stop now

				break;
			}

			// Precompute pointers to the unicode string and length.
			// This saves us repeated function calls later,
			// significantly speeding up the algorithm.

			String mapStrings[] = new String[mapElemv.length];
			int mapLens[] = new int[mapElemv.length];
			for (int ix = 0; ix < mapElemv.length; ix++) {
				mapStrings[ix] = mapElemv[ix].toString();
				mapLens[ix] = mapStrings[ix].length();
			}
			String cmpStrings[];
			if (nocase) {
				cmpStrings = new String[mapStrings.length];
				for (int ix = 0; ix < mapStrings.length; ix++) {
					cmpStrings[ix] = mapStrings[ix].toLowerCase();
				}
			} else {
				cmpStrings = mapStrings;
			}

			TclObject result = TclString.newInstance("");
			int p, str1;
			for (p = 0, str1 = 0; str1 < length1; str1++) {
				for (index = 0; index < mapStrings.length; index += 2) {
					// Get the key string to match on

					String string2 = mapStrings[index];
					int length2 = mapLens[index];
					if ((length2 > 0)
							&& (cmpString1.substring(str1)
									.startsWith(cmpStrings[index]))) {
						if (p != str1) {
							// Put the skipped chars onto the result first

							TclString
									.append(result, string1.substring(p, str1));
							p = str1 + length2;
						} else {
							p += length2;
						}

						// Adjust len to be full length of matched string

						str1 = p - 1;

						// Append the map value to the unicode string

						TclString.append(result, mapStrings[index + 1]);
						break;
					}
				}
			}

			if (p != str1) {
				// Put the rest of the unmapped chars onto result

				TclString.append(result, string1.substring(p, str1));
			}
			interp.setResult(result);
			break;
		}

		case STR_MATCH: {
			if (objv.length < 4 || objv.length > 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"?-nocase? pattern string");
			}

			String string1, string2;
			if (objv.length == 5) {
				String string = objv[2].toString();
				if (!((string.length() > 1) && "-nocase".startsWith(string))) {
					throw new TclException(interp, "bad option \"" + string
							+ "\": must be -nocase");
				}
				string1 = objv[4].toString().toLowerCase();
				string2 = objv[3].toString().toLowerCase();
			} else {
				string1 = objv[3].toString();
				string2 = objv[2].toString();
			}

			interp.setResult(Util.stringMatch(string1, string2));
			break;
		}

		case STR_RANGE: {
			if (objv.length != 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"string first last");
			}

			String string1=null;
			int length1;
			if (objv[2].isByteArrayType()) {
				length1 = TclByteArray.getLength(interp, objv[2]);
			} else {
				string1 = objv[2].toString();
				length1 = string1.length();
			}

			int first = Util.getIntForIndex(interp, objv[3], length1 - 1);
			if (first < 0) {
				first = 0;
			}
			int last = Util.getIntForIndex(interp, objv[4], length1 - 1);
			if (last >= length1) {
				last = length1 - 1;
			}

			if (first > last) {
				interp.resetResult();
			} else {
				if (string1==null) {
					byte [] bytes = TclByteArray.getBytes(interp, objv[2]);
					TclObject rv = TclByteArray.newInstance(bytes, first, last+1-first);
					interp.setResult(rv);
				} else {
					interp.setResult(string1.substring(first, last + 1));
				}
			}
			break;
		}

		case STR_REPEAT: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "string count");
			}

			int count = TclInteger.getInt(interp, objv[3]);

			String string1 = objv[2].toString();
			if (string1.length() > 0) {
				TclObject tstr = TclString.newInstance("");
				for (index = 0; index < count; index++) {
					TclString.append(tstr, string1);
				}
				interp.setResult(tstr);
			}
			break;
		}

		case STR_REPLACE: {
			if (objv.length < 5 || objv.length > 6) {
				throw new TclNumArgsException(interp, 2, objv,
						"string first last ?string?");
			}

			String string1 = objv[2].toString();
			int length1 = string1.length() - 1;

			int first = Util.getIntForIndex(interp, objv[3], length1);
			int last = Util.getIntForIndex(interp, objv[4], length1);

			if ((last < first) || (first > length1) || (last < 0)) {
				interp.setResult(objv[2]);
			} else {
				if (first < 0) {
					first = 0;
				}
				String start = string1.substring(first);
				int ind = ((last > length1) ? length1 : last) - first + 1;
				String end;
				if (ind <= 0) {
					end = start;
				} else if (ind >= start.length()) {
					end = "";
				} else {
					end = start.substring(ind);
				}

				TclObject tstr = TclString.newInstance(string1.substring(0,
						first));

				if (objv.length == 6) {
					TclString.append(tstr, objv[5]);
				}
				if (last < length1) {
					TclString.append(tstr, end);
				}

				interp.setResult(tstr);
			}
			break;
		}

		case STR_TOLOWER:
		case STR_TOUPPER:
		case STR_TOTITLE: {
			if (objv.length < 3 || objv.length > 5) {
				throw new TclNumArgsException(interp, 2, objv,
						"string ?first? ?last?");
			}
			String string1 = objv[2].toString();

			if (objv.length == 3) {
				if (index == STR_TOLOWER) {
					interp.setResult(string1.toLowerCase());
				} else if (index == STR_TOUPPER) {
					interp.setResult(string1.toUpperCase());
				} else {
					interp.setResult(Util.toTitle(string1));
				}
			} else {
				int length1 = string1.length() - 1;
				int first = Util.getIntForIndex(interp, objv[3], length1);
				if (first < 0) {
					first = 0;
				}
				int last = first;
				if (objv.length == 5) {
					last = Util.getIntForIndex(interp, objv[4], length1);
				}
				if (last >= length1) {
					last = length1;
				}
				if (last < first) {
					interp.setResult(objv[2]);
					break;
				}

				String string2;
				StringBuilder buf = new StringBuilder();
				buf.append(string1.substring(0, first));
				if (last + 1 > length1) {
					string2 = string1.substring(first);
				} else {
					string2 = string1.substring(first, last + 1);
				}
				if (index == STR_TOLOWER) {
					buf.append(string2.toLowerCase());
				} else if (index == STR_TOUPPER) {
					buf.append(string2.toUpperCase());
				} else {
					buf.append(Util.toTitle(string2));
				}
				if (last + 1 <= length1) {
					buf.append(string1.substring(last + 1));
				}

				interp.setResult(buf.toString());
			}
			break;
		}

		case STR_TRIM: {
			if (objv.length == 3) {
				// Case 1: "string trim str" --
				// Remove leading and trailing white space

				interp.setResult(objv[2].toString().trim());
			} else if (objv.length == 4) {

				// Case 2: "string trim str chars" --
				// Remove leading and trailing chars in the chars set

				String tmp = Util.TrimLeft(objv[2].toString(), objv[3]
						.toString());
				interp.setResult(Util.TrimRight(tmp, objv[3].toString()));
			} else {
				// Case 3: Wrong # of args

				throw new TclNumArgsException(interp, 2, objv, "string ?chars?");
			}
			break;
		}

		case STR_TRIMLEFT: {
			if (objv.length == 3) {
				// Case 1: "string trimleft str" --
				// Remove leading and trailing white space

				interp.setResult(Util.TrimLeft(objv[2].toString()));
			} else if (objv.length == 4) {
				// Case 2: "string trimleft str chars" --
				// Remove leading and trailing chars in the chars set

				interp.setResult(Util.TrimLeft(objv[2].toString(), objv[3]
						.toString()));
			} else {
				// Case 3: Wrong # of args

				throw new TclNumArgsException(interp, 2, objv, "string ?chars?");
			}
			break;
		}

		case STR_TRIMRIGHT: {
			if (objv.length == 3) {
				// Case 1: "string trimright str" --
				// Remove leading and trailing white space

				interp.setResult(Util.TrimRight(objv[2].toString()));
			} else if (objv.length == 4) {
				// Case 2: "string trimright str chars" --
				// Remove leading and trailing chars in the chars set

				interp.setResult(Util.TrimRight(objv[2].toString(), objv[3]
						.toString()));
			} else {
				// Case 3: Wrong # of args

				throw new TclNumArgsException(interp, 2, objv, "string ?chars?");
			}
			break;
		}

		case STR_WORDEND: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "string index");
			}

			String string1 = objv[2].toString();
			char strArray[] = string1.toCharArray();
			int cur;
			int length1 = string1.length();
			index = Util.getIntForIndex(interp, objv[3], length1 - 1);

			if (index < 0) {
				index = 0;
			}
			if (index >= length1) {
				interp.setResult(length1);
				return;
			}
			for (cur = index; cur < length1; cur++) {
				char c = strArray[cur];
				if (((1 << Character.getType(c)) & WORD_BITS) == 0) {
					break;
				}
			}
			if (cur == index) {
				cur = index + 1;
			}
			interp.setResult(cur);
			break;
		}

		case STR_WORDSTART: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "string index");
			}

			String string1 = objv[2].toString();
			char strArray[] = string1.toCharArray();
			int cur;
			int length1 = string1.length();
			index = Util.getIntForIndex(interp, objv[3], length1 - 1);

			if (index > length1) {
				index = length1 - 1;
			}
			if (index < 0) {
				interp.setResult(0);
				return;
			}
			for (cur = index; cur >= 0; cur--) {
				char c = strArray[cur];
				if (((1 << Character.getType(c)) & WORD_BITS) == 0) {
					break;
				}
			}
			if (cur != index) {
				cur += 1;
			}
			interp.setResult(cur);
			break;
		}
		}
	}

	// return the number of Utf8 bytes that would be needed to store s

	public final static int Utf8Count(String s) {
		int p = 0;
		final int len = s.length();
		char c;
		int sum = 0;

		while (p < len) {
			c = s.charAt(p++);

			if ((c > 0) && (c < 0x80)) {
				sum += 1;
				continue;
			}
			if (c <= 0x7FF) {
				sum += 2;
				continue;
			}
			if (c <= 0xFFFF) {
				sum += 3;
				continue;
			}
		}

		return sum;
	}

	// return the number of Utf8 bytes for the character c

	public final static int Utf8Count(char c) {
		if ((c > 0) && (c < 0x80)) {
			return 1;
		} else if (c <= 0x7FF) {
			return 2;
		} else {
			return 3;
		}
	}

} // end StringCmd
