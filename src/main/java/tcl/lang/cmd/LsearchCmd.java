/*
 * LsearchCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-1999 by Scriptics Corporation.
 * Copyright (c) 2000 Christian Krone.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: LsearchCmd.java,v 1.6 2009/08/07 21:56:39 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import java.util.regex.PatternSyntaxException;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.Regex;
import tcl.lang.TCL;
import tcl.lang.TclDouble;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.Util;

/*
 * This class implements the built-in "lsearch" command in Tcl.
 */

public class LsearchCmd implements Command {

	static final private String[] options = { "-all", "-ascii", "-decreasing", "-dictionary", "-exact", "-glob",
			"-increasing", "-inline", "-integer", "-not", "-real", "-regexp", "-sorted", "-start" };
	static final int LSEARCH_ALL = 0;
	static final int LSEARCH_ASCII = 1;
	static final int LSEARCH_DECREASING = 2;
	static final int LSEARCH_DICTIONARY = 3;
	static final int LSEARCH_EXACT = 4;
	static final int LSEARCH_GLOB = 5;
	static final int LSEARCH_INCREASING = 6;
	static final int LSEARCH_INLINE = 7;
	static final int LSEARCH_INTEGER = 8;
	static final int LSEARCH_NOT = 9;
	static final int LSEARCH_REAL = 10;
	static final int LSEARCH_REGEXP = 11;
	static final int LSEARCH_SORTED = 12;
	static final int LSEARCH_START = 13;

	static final int ASCII = 0;
	static final int DICTIONARY = 1;
	static final int INTEGER = 2;
	static final int REAL = 3;

	static final int EXACT = 0;
	static final int GLOB = 1;
	static final int REGEXP = 2;
	static final int SORTED = 3;

	/**
	 * This procedure is invoked to process the "lsearch" Tcl command. See the
	 * user documentation for details on what it does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		int mode = GLOB;
		int dataType = ASCII;
		int offset = 0;
		boolean isIncreasing = true;
		boolean nocase = true;
		boolean allMatches = false;
		boolean inlineReturn = false;
		boolean negatedMatch = false;
		TclObject start = null;
		TclObject[] listv;
		TclObject resultList = null;
		Regex regexp;

		if (objv.length < 3) {
			throw new TclNumArgsException(interp, 1, objv, "?options? list pattern");
		}

		for (int i = 1; i < objv.length - 2; i++) {
			int opt = TclIndex.get(interp, objv[i], options, "option", 0);
			switch (opt) {
			case LSEARCH_ALL: // -all
				allMatches = true;
				break;
			case LSEARCH_ASCII: // -ascii
				dataType = ASCII;
				break;
			case LSEARCH_DECREASING: // -decreasing
				isIncreasing = false;
				break;
			case LSEARCH_DICTIONARY: // -dictionary
				dataType = DICTIONARY;
				break;
			case LSEARCH_EXACT: // -increasing
				mode = EXACT;
				break;
			case LSEARCH_GLOB: // -glob
				mode = GLOB;
				break;
			case LSEARCH_INCREASING: // -increasing
				isIncreasing = true;
				break;
			case LSEARCH_INLINE: // -inline
				inlineReturn = true;
				break;
			case LSEARCH_INTEGER: // -integer
				dataType = INTEGER;
				break;
			case LSEARCH_NOT: // -integer
				negatedMatch = true;
				break;
			case LSEARCH_REAL: // -real
				dataType = REAL;
				break;
			case LSEARCH_REGEXP: // -regexp
				mode = REGEXP;
				break;
			case LSEARCH_SORTED: // -sorted
				mode = SORTED;
				break;
			case LSEARCH_START: // - start
				if (i > objv.length - 4) {
					throw new TclException(interp, "missing starting index");
				}
				i++;
				start = objv[i].duplicate();
				break;
			}
		}

		// Make sure the list argument is a list object and get its length and
		// a pointer to its array of element pointers.

		try {
			listv = TclList.getElements(interp, objv[objv.length - 2]);
		} catch (TclException e) {
			throw e;
		}

		/*
		 * Get the user-specified start offset.
		 */

		if (start != null) {
			try {
				offset = Util.getIntForIndex(interp, start, listv.length - 1);
			} catch (TclException e) {
				throw e;
			}

			if (offset < 0) {
				offset = 0;
			}

			/*
			 * If the search started past the end of the list, we just return a
			 * "did not match anything at all" result straight away. [Bug
			 * 1374778]
			 */

			if (offset > listv.length - 1) {
				if (allMatches || inlineReturn) {
					interp.resetResult();
				} else {
					interp.setResult(TclInteger.newInstance(-1));
				}
				return;
			}
		}

		TclObject patObj = objv[objv.length - 1].duplicate();
		String patternBytes = null;
		int patInt = 0;
		double patDouble = 0.0;
		int length = 0;
		if (mode == EXACT || mode == SORTED) {
			switch (dataType) {
			case ASCII:
			case DICTIONARY:
				patternBytes = patObj.toString();
				length = patternBytes.length();
				break;
			case INTEGER:
				patInt = TclInteger.getInt(interp, patObj);
				break;
			case REAL:
				patDouble = TclDouble.get(interp, patObj);
				break;
			}
		} else {
			patternBytes = patObj.toString();
			length = patternBytes.length();
		}

		// Set default index value to -1, indicating failure; if we find the
		// item in the course of our search, index will be set to the correct
		// value.

		int index = -1;
		if (mode == SORTED && !allMatches && !negatedMatch) {
			/*
			 * If the data is sorted, we can do a more intelligent search. Note
			 * that there is no point in being smart when -all was specified; in
			 * that case, we have to look at all items anyway, and there is no
			 * sense in doing this when the match sense is inverted.
			 */
			int match = 0;
			int lower = offset - 1;
			int upper = listv.length;
			while (lower + 1 != upper) {
				int i = (lower + upper) >>> 1;
				switch (dataType) {
				case ASCII: {
					String bytes = listv[i].toString();
					match = patternBytes.compareTo(bytes);
					break;
				}
				case DICTIONARY: {
					String bytes = listv[i].toString();
					match = DictionaryCompare(patternBytes, bytes);
					break;
				}
				case INTEGER: {
					int objInt = TclInteger.getInt(interp, listv[i]);
					if (patInt == objInt) {
						match = 0;
					} else if (patInt < objInt) {
						match = -1;
					} else {
						match = 1;
					}
					break;
				}
				case REAL: {
					double objDouble = TclDouble.get(interp, listv[i]);
					if (patDouble == objDouble) {
						match = 0;
					} else if (patDouble < objDouble) {
						match = -1;
					} else {
						match = 1;
					}
					break;
				}
				}
				if (match == 0) {

					// Normally, binary search is written to stop when it
					// finds a match. If there are duplicates of an element in
					// the list, our first match might not be the first
					// occurance.
					// Consider: 0 0 0 1 1 1 2 2 2
					// To maintain consistancy with standard lsearch semantics,
					// we must find the leftmost occurance of the pattern in the
					// list. Thus we don't just stop searching here. This
					// variation means that a search always makes log n
					// comparisons (normal binary search might "get lucky" with
					// an early comparison).

					index = i;
					upper = i;
				} else if (match > 0) {
					if (isIncreasing) {
						lower = i;
					} else {
						upper = i;
					}
				} else {
					if (isIncreasing) {
						upper = i;
					} else {
						lower = i;
					}
				}
			}
		} else {
			/*
			 * We need to do a linear search, because (at least one) of: - our
			 * matcher can only tell equal vs. not equal - our matching sense is
			 * negated - we're building a list of all matched items
			 */

			if (allMatches) {
				resultList = TclList.newInstance();
			}

			for (int i = offset; i < listv.length; i++) {
				boolean match = false;
				switch (mode) {
				case SORTED:
					/* falls through */
				case EXACT: {
					switch (dataType) {
					case ASCII: {
						String bytes = listv[i].toString();
						int elemLen = bytes.length();
						if (length == elemLen) {
							match = bytes.equals(patternBytes);
						}
						break;
					}
					case DICTIONARY: {
						String bytes = listv[i].toString();
						match = (DictionaryCompare(bytes, patternBytes) == 0);
						break;
					}
					case INTEGER: {
						int objInt = TclInteger.getInt(interp, listv[i]);
						match = (objInt == patInt);
						break;
					}
					case REAL: {
						double objDouble = TclDouble.get(interp, listv[i]);
						match = (objDouble == patDouble);
						break;
					}
					}
					break;
				}
				case GLOB: {
					match = Util.stringMatch(listv[i].toString(), patternBytes);
					break;
				}
				case REGEXP: {

					try {
						regexp = new Regex(patObj.toString(), listv[i].toString(), 0);
						match = regexp.match();
					} catch (PatternSyntaxException ex) {
			            throw new TclException(interp, Regex.getPatternSyntaxMessage(ex));
					}
					break;
				}
				}

				/*
				 * Invert match condition for -not.
				 */

				if (negatedMatch) {
					match = !match;
				}

				if (match) {
					if (!allMatches) {
						index = i;
						break;
					} else if (inlineReturn) {
						/*
						 * Note that these appends are not expected to fail.
						 */
						TclList.append(interp, resultList, listv[i]);
					} else {
						TclList.append(interp, resultList, TclInteger.newInstance(i));
					}
				}
			}
		}

		/*
		 * Return everything or a single value.
		 */
		if (allMatches) {
			interp.setResult(resultList);
		} else if (!inlineReturn) {
			interp.setResult(index);
		} else if (index < 0) {
			/*
			 * Is this superfluous? The result should be a blank object by
			 * default...
			 */
			interp.resetResult();
		} else {
			interp.setResult(listv[index]);
		}
	}

	/**
	 * DictionaryCompare -> dictionaryCompare
	 * 
	 * This function compares two strings as if they were being used in an index
	 * or card catalog. The case of alphabetic characters is ignored, except to
	 * break ties. Thus "B" comes before "b" but after "a". Also, integers
	 * embedded in the strings compare in numerical order. In other words,
	 * "x10y" comes after "x9y", not before it as it would when using strcmp().
	 * 
	 * Results: A negative result means that the first element comes before the
	 * second, and a positive result means that the second element should come
	 * first. A result of zero means the two elements are equal and it doesn't
	 * matter which comes first.
	 * 
	 * Side effects: None.
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	
	private static int DictionaryCompare(String left, String right) {
		char[] leftArr = left.toCharArray();
		char[] rightArr = right.toCharArray();
		char leftChar, rightChar, leftLower, rightLower;
		int lInd = 0;
		int rInd = 0;
		int diff;
		int secondaryDiff = 0;

		while (true) {
			if ((rInd < rightArr.length) && (Character.isDigit(rightArr[rInd])) && (lInd < leftArr.length)
					&& (Character.isDigit(leftArr[lInd]))) {
				// There are decimal numbers embedded in the two
				// strings. Compare them as numbers, rather than
				// strings. If one number has more leading zeros than
				// the other, the number with more leading zeros sorts
				// later, but only as a secondary choice.

				int zeros = 0;
				while ((rightArr[rInd] == '0') && (rInd + 1 < rightArr.length)
						&& (Character.isDigit(rightArr[rInd + 1]))) {
					rInd++;
					zeros--;
				}
				while ((leftArr[lInd] == '0') && (lInd + 1 < leftArr.length) && (Character.isDigit(leftArr[lInd + 1]))) {
					lInd++;
					zeros++;
				}
				if (secondaryDiff == 0) {
					secondaryDiff = zeros;// end LsearchCmd
				}

				// The code below compares the numbers in the two
				// strings without ever converting them to integers. It
				// does this by first comparing the lengths of the
				// numbers and then comparing the digit values.

				diff = 0;
				while (true) {
					if ((diff == 0) && (lInd < leftArr.length) && (rInd < rightArr.length)) {
						diff = leftArr[lInd] - rightArr[rInd];
					}
					rInd++;
					lInd++;
					if (rInd >= rightArr.length || !Character.isDigit(rightArr[rInd])) {
						if (lInd < leftArr.length && Character.isDigit(leftArr[lInd])) {
							return 1;
						} else {
							// The two numbers have the same length. See
							// if their values are different.

							if (diff != 0) {
								return diff;
							}
							break;
						}
					} else if (lInd >= leftArr.length || !Character.isDigit(leftArr[lInd])) {
						return -1;
					}
				}
				continue;
			}

			// Convert character to Unicode for comparison purposes. If either
			// string is at the terminating null, do a byte-wise comparison and
			// bail out immediately.

			if ((lInd < leftArr.length) && (rInd < rightArr.length)) {

				// Convert both chars to lower for the comparison, because
				// dictionary sorts are case insensitve. Covert to lower, not
				// upper, so chars between Z and a will sort before A (where
				// most
				// other interesting punctuations occur)

				leftChar = leftArr[lInd++];
				rightChar = rightArr[rInd++];
				leftLower = Character.toLowerCase(leftChar);
				rightLower = Character.toLowerCase(rightChar);
			} else if (lInd < leftArr.length) {
				diff = -rightArr[rInd];
				break;
			} else if (rInd < rightArr.length) {
				diff = leftArr[lInd];
				break;
			} else {
				diff = 0;
				break;
			}

			diff = leftLower - rightLower;
			if (diff != 0) {
				return diff;
			} else if (secondaryDiff == 0) {
				if (Character.isUpperCase(leftChar) && Character.isLowerCase(rightChar)) {
					secondaryDiff = -1;
				} else if (Character.isUpperCase(rightChar) && Character.isLowerCase(leftChar)) {
					secondaryDiff = 1;
				}
			}
		}
		if (diff == 0) {
			diff = secondaryDiff;
		}
		return diff;
	}

}
