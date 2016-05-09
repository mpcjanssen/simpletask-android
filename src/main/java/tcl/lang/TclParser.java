/* 
 * tclpro/tclparser/tclParser.c -> TclParser.java
 *
 *	This is a Tcl language parser as a Tcl dynamically loadable
 *	extension.
 *
 * Copyright (c) 1996 by Sun Microsystems, Inc.
 * Copyright (c) 2000 Ajuba Solutions
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: TclParser.java,v 1.5 2005/11/22 22:10:02 mdejong Exp $
 */

package tcl.lang;

import tcl.lang.cmd.StringCmd;

public class TclParser implements Command {

	static final private String[] options = { "command", "expr", "varname",
			"list", "getrange", "getstring", "charindex", "charlength",
			"countnewline" };

	static final private int PARSE_COMMAND = 0;
	static final private int PARSE_EXPR = 1;
	static final private int PARSE_VARNAME = 2;
	static final private int PARSE_LIST = 3;
	static final private int PARSE_GET_RANGE = 4;
	static final private int PARSE_GET_STR = 5;
	static final private int PARSE_CHAR_INDEX = 6;
	static final private int PARSE_CHAR_LEN = 7;
	static final private int PARSE_COUNT_NWLNE = 8;

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseObjCmd -> cmdProc
	 * 
	 * This function implements the Tcl "parse" command.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Arguments to command
			throws TclException {
		int option, index, length, scriptLength;

		if (objv.length < 3) {
			throw new TclNumArgsException(interp, 1, objv,
					"option arg ?arg ...?");
		}
		option = TclIndex.get(interp, objv[1], options, "option", 0);

		// If the script argument holds a cached UTF8CharPointer internal rep
		// then grab it and use it. Otherwise, create a new UTF8CharPointer
		// and set it as the internal rep.

		TclObject tobj = objv[2];
		UTF8CharPointer script;
		InternalRep irep = tobj.getInternalRep();
		if (irep instanceof UTF8CharPointer) {
			script = (UTF8CharPointer) irep;
		} else {
			script = new UTF8CharPointer(tobj.toString());
			tobj.setInternalRep(script);
		}
		if (script == null) {
			System.out.println(script); // For debugging only
		}
		scriptLength = script.getByteLength();

		// Check the number arguments passed to the command and
		// extract information (script, index, length) depending
		// upon the option selected.

		switch (option) {
		case PARSE_GET_RANGE: {
			if (objv.length == 3) {
				index = 0;
				length = scriptLength;
			} else if (objv.length == 5) {
				index = TclInteger.getInt(interp, objv[3]);
				length = TclInteger.getInt(interp, objv[4]);

				if (index < 0) {
					index = 0;
				} else if (index >= scriptLength) {
					index = scriptLength - 1;
				}
				if (length < 0) {
					length = 0;
				} else if (length > (scriptLength - index)) {
					length = scriptLength - index;
				}
			} else {
				throw new TclNumArgsException(interp, 2, objv,
						"string ?index length?");
			}
			interp.setResult(ParseMakeRange(script, index, length));
			return;
		}
		case PARSE_COMMAND:
		case PARSE_EXPR:
		case PARSE_VARNAME:
		case PARSE_LIST:
		case PARSE_GET_STR:
		case PARSE_CHAR_INDEX:
		case PARSE_CHAR_LEN: {
			if (objv.length != 4) {
				throw new TclNumArgsException(interp, 2, objv, "string range");
			}
			ParseGetIndexAndLengthResult result = new ParseGetIndexAndLengthResult();
			ParseGetIndexAndLength(interp, objv[3], scriptLength, result);
			index = result.indexPtr;
			length = result.lengthPtr;

			switch (option) {
			case PARSE_COMMAND:
				ParseCommand(interp, script, index, length);
				return;
			case PARSE_EXPR:
				ParseExpr(interp, script, index, length);
				return;
			case PARSE_VARNAME:
				ParseVarName(interp, script, index, length);
				return;
			case PARSE_LIST:
				ParseList(interp, script, index, length);
				return;
			case PARSE_GET_STR:
				ParseGetString(interp, script, index, length);
				return;
			case PARSE_CHAR_INDEX:
				ParseCharIndex(interp, script, index, length);
				return;
			case PARSE_CHAR_LEN:
				ParseCharLength(interp, script, index, length);
				return;
			case PARSE_GET_RANGE:
			case PARSE_COUNT_NWLNE:
				// No Op - This will suppress compiler warnings
				break;
			}
			break;
		}
		case PARSE_COUNT_NWLNE: {
			TclObject range2;
			if (objv.length == 5) {
				range2 = objv[4];
			} else if (objv.length == 4) {
				range2 = null;
			} else {
				throw new TclNumArgsException(interp, 2, objv,
						"string range ?range?");
			}
			ParseCountNewline(interp, script, scriptLength, objv[3], range2);
			return;
		}
		}
		throw new TclException(interp, "unmatched option");
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseCommand --
	 * 
	 * This function parses a script into Tcl commands by calling the
	 * Tcl_ParseCommand function. This routine returns a list of the following
	 * form: <commentRange> <commandRange> <restRange> <parseTree> The first
	 * range refers to any leading comments before the command. The second range
	 * refers to the command itself. The third range contains the remainder of
	 * the original range that appears after the command range. The parseTree is
	 * a list representation of the parse tree where each node is a list in the
	 * form: <type> <range> <subTree>.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseCommand(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			int index, // Index to the starting point of the
			// script, in bytes.
			int length) // Length of script be parsed, in bytes.
			throws TclException {
		TclObject resultPtr, listPtr, tokenPtr;
		TclParse parse;
		int i;
		int endCharIndex;
		int endByteIndex;

		// Convert byte index and range into char index and range
		int charIndex = script.getCharIndex(index);
		int charLength = script.getCharRange(index, length);

		parse = Parser.parseCommand(interp, script.array, charIndex,
				charLength, null, -1, false);

		if (parse.result != TCL.OK) {
			ParseSetErrorCode(interp, script, parse);
		}

		resultPtr = TclList.newInstance();
		if (parse.commentStart != -1) {
			TclList.append(interp, resultPtr, ParseMakeByteRange(script,
					parse.commentStart, parse.commentSize));
		} else {
			TclList.append(interp, resultPtr, ParseMakeRange(script,
					script.index, 0));
		}
		TclList.append(interp, resultPtr, ParseMakeByteRange(script,
				parse.commandStart, parse.commandSize));
		endCharIndex = parse.commandStart + parse.commandSize;
		TclList.append(interp, resultPtr, ParseMakeByteRange(script,
				endCharIndex, (charLength - (endCharIndex - charIndex))));

		listPtr = TclList.newInstance();
		ParseMakeTokenListResult result = new ParseMakeTokenListResult();
		i = 0;
		while (i < parse.numTokens) {
			i = ParseMakeTokenList(script, parse, i, result);
			tokenPtr = result.newList;
			TclList.append(null, listPtr, tokenPtr);
		}
		TclList.append(interp, resultPtr, listPtr);
		interp.setResult(resultPtr);
		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseExpr --
	 * 
	 * This function parses a Tcl expression into a tree representation.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseExpr(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			int index, // Index to the starting point of the
			// script, in bytes.
			int length) // Length of script be parsed, in bytes.
			throws TclException {
		TclParse parse;

		int charIndex = script.getCharIndex(index);
		int charLength = script.getCharRange(index, length);

		parse = ParseExpr
				.parseExpr(interp, script.array, charIndex, charLength);

		if (parse.result != TCL.OK) {
			ParseSetErrorCode(interp, script, parse);
		}

		// There is only one top level token, so just return it.

		ParseMakeTokenListResult lresult = new ParseMakeTokenListResult();
		ParseMakeTokenList(script, parse, 0, lresult);
		interp.setResult(lresult.newList);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseList --
	 * 
	 * This function parses a Tcl list into a list of ranges.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseList(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			int index, // Index to the starting point of the
			// script, in bytes.
			int length) // Length of script be parsed, in bytes.
			throws TclException {
		TclObject resultPtr;
		int size;
		char c;
		String list;
		int elementIndex;
		int listIndex, prevListIndex, lastListIndex;
		FindElemResult fer = new FindElemResult();
		int charIndex, charLength, charListOffset;
		boolean found;

		charIndex = script.getCharIndex(index);
		charListOffset = (charIndex - script.index);

		resultPtr = TclList.newInstance();
		list = script.getByteRangeAsString(index, length);
		charLength = list.length();

		lastListIndex = charLength;
		listIndex = 0;

		for (;;) {
			prevListIndex = listIndex;

			try {
				found = Util.findElement(interp, list, listIndex, charLength,
						fer);
			} catch (TclException te) {
				TclObject errorCode = TclList.newInstance();
				TclList.append(interp, errorCode, TclString
						.newInstance("PARSE"));
				TclList
						.append(interp, errorCode, TclString
								.newInstance("list"));
				// Convert to byte range
				int byteRange = script.getByteRange(script.index,
						charListOffset + listIndex);
				TclList.append(interp, errorCode, TclInteger
						.newInstance(byteRange));
				TclList.append(interp, errorCode, interp.getResult());
				interp.setErrorCode(errorCode);
				throw te;
			}
			if (!found) {
				break;
			}
			listIndex = fer.elemEnd;
			// charLength -= (listIndex - prevListIndex);
			elementIndex = fer.elemStart;
			size = fer.size;

			// Check to see if this element was in quotes or braces.
			// If it is, ensure that the range includes the quotes/braces
			// so the parser can make decisions based on this fact.

			if (elementIndex > 0) {
				c = list.charAt(elementIndex - 1);
			} else {
				c = '\0';
			}
			if (c == '{' || c == '\"') {
				elementIndex--;
				size += 2;
			}
			TclList.append(interp, resultPtr, ParseMakeByteRange(script,
					charListOffset + elementIndex, size));
		}

		interp.setResult(resultPtr);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseVarName --
	 * 
	 * This function parses a Tcl braced word into a tree representation.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseVarName(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			int index, // Index to the starting point of the
			// script, in bytes.
			int length) // Length of script be parsed, in bytes.
			throws TclException {
		TclParse parse;

		// Convert byte index and range into char index and range
		int charIndex = script.getCharIndex(index);
		int charLength = script.getCharRange(index, length);

		parse = Parser.parseVarName(interp, script.array, charIndex,
				charLength, null, false);
		if (parse.result != TCL.OK) {
			ParseSetErrorCode(interp, script, parse);
		}

		// There is only one top level token, so just return it.

		ParseMakeTokenListResult lresult = new ParseMakeTokenListResult();
		ParseMakeTokenList(script, parse, 0, lresult);
		interp.setResult(lresult.newList);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseSetErrorCode --
	 * 
	 * Set the errorCode variable the standard parser error form and raise a
	 * TclException. This method is invoked after something goes wrong in a
	 * parse operation.
	 * 
	 * Results: None.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseSetErrorCode(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			TclParse parse) // Parse state.
			throws TclException {
		TclObject tlist;
		String type;

		switch (parse.errorType) {
		case Parser.TCL_PARSE_QUOTE_EXTRA:
			type = "quoteExtra";
			break;
		case Parser.TCL_PARSE_BRACE_EXTRA:
			type = "braceExtra";
			break;
		case Parser.TCL_PARSE_MISSING_BRACE:
			type = "missingBrace";
			break;
		case Parser.TCL_PARSE_MISSING_BRACKET:
			type = "missingBracket";
			break;
		case Parser.TCL_PARSE_MISSING_PAREN:
			type = "missingParen";
			break;
		case Parser.TCL_PARSE_MISSING_QUOTE:
			type = "missingQuote";
			break;
		case Parser.TCL_PARSE_MISSING_VAR_BRACE:
			type = "missingVarBrace";
			break;
		case Parser.TCL_PARSE_SYNTAX:
			type = "syntax";
			break;
		case Parser.TCL_PARSE_BAD_NUMBER:
			type = "badNumber";
			break;
		default:
			throw new TclException(interp,
					"unexpected error type from Tcl_ParseCommand");
		}
		tlist = TclList.newInstance();
		TclList.append(interp, tlist, TclString.newInstance("PARSE"));
		TclList.append(interp, tlist, TclString.newInstance(type));
		if (parse.termIndex > 0) {
			// Convert to byte range
			int byteRange = script.getByteRange(script.index, parse.termIndex);
			TclList.append(interp, tlist, TclInteger.newInstance(byteRange));
		} else {
			TclList.append(interp, tlist, TclInteger.newInstance(0));
		}
		TclList.append(interp, tlist, interp.getResult());
		interp.setErrorCode(tlist);
		throw new TclException(interp, interp.getResult().toString());
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseMakeTokenList --
	 * 
	 * Make the list representation of a token. Each token is represented as a
	 * list where the first element is a token type, the second element is a
	 * range, and the third element is a list of subtokens.
	 * 
	 * Results: Returns the next token offset and stores a newly allocated list
	 * object in the location referred to by result.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static class ParseMakeTokenListResult {
		TclObject newList;
	}

	static int ParseMakeTokenList(UTF8CharPointer script, // Pointer to start of
															// script being
															// parsed.
			TclParse parse, // Parse information.
			int index, // Index of token to append.
			ParseMakeTokenListResult result)
	// Location where resulting list
			// object is to be stored.
			throws TclException {
		TclToken token = parse.tokenList[index];
		TclObject resultList, resultIndexList;
		int start;
		String type;

		switch (token.type) {
		case Parser.TCL_TOKEN_WORD:
			type = "word";
			break;
		case Parser.TCL_TOKEN_SIMPLE_WORD:
			type = "simple";
			break;
		case Parser.TCL_TOKEN_TEXT:
			type = "text";
			break;
		case Parser.TCL_TOKEN_BS:
			type = "backslash";
			break;
		case Parser.TCL_TOKEN_COMMAND:
			type = "command";
			break;
		case Parser.TCL_TOKEN_VARIABLE:
			type = "variable";
			break;
		case Parser.TCL_TOKEN_SUB_EXPR:
			type = "subexpr";
			break;
		case Parser.TCL_TOKEN_OPERATOR:
			type = "operator";
			break;
		default:
			type = "unknown";
			break;
		}
		resultList = TclList.newInstance();
		TclList.append(null, resultList, TclString.newInstance(type));
		TclList.append(null, resultList, ParseMakeByteRange(script,
				token.script_index, token.size));
		resultIndexList = TclList.newInstance();
		TclList.append(null, resultList, resultIndexList);
		start = index;
		index++;
		ParseMakeTokenListResult lresult = new ParseMakeTokenListResult();
		while (index <= start + token.numComponents) {
			index = ParseMakeTokenList(script, parse, index, lresult);
			TclList.append(null, resultIndexList, lresult.newList);
		}

		result.newList = resultList;
		return index;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseMakeRange --
	 * 
	 * Construct a new range object. This method depends on the script.index
	 * being set to the starting index of the entire script.
	 * 
	 * Results: Returns a newly allocated Tcl object.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static TclObject ParseMakeRange(UTF8CharPointer script, // Pointer to the
															// start of whole
															// script.
			int start, // Index of start of the range, in bytes.
			int length) // The length of the range, in bytes.
			throws TclException {
		int scriptByteIndex = script.getByteIndex(script.index);

		TclObject tlist = TclList.newInstance();
		TclList.append(null, tlist, TclInteger.newInstance(start
				- scriptByteIndex));
		TclList.append(null, tlist, TclInteger.newInstance(length));
		return tlist;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseMakeByteRange --
	 * 
	 * Construct a new range object containing a byte range given a start and
	 * length in characters.
	 * 
	 * Results: Returns a newly allocated Tcl object.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static TclObject ParseMakeByteRange(UTF8CharPointer script, // Pointer to
																// the start of
																// whole script.
			int start, // Index of start of the range, in chars.
			int length) // The length of the range, in chars.
			throws TclException {
		if (start < 0) {
			throw new TclRuntimeError("char index can't be < 0, was " + start);
		}
		if (length < 0) {
			throw new TclRuntimeError("char length can't be < 0, was " + length);
		}
		int byteStart = script.getByteIndex(start);
		int byteLength = script.getByteRange(start, length);
		return ParseMakeRange(script, byteStart, byteLength);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseGetString --
	 * 
	 * Extract the string from the script within the boundaries of byte oriented
	 * index and length.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: The interp's result is set.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseGetString(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			int index, // Index to the starting point of the
			// script, in bytes
			int length) // Length of script in bytes.
			throws TclException {
		String str = script.getByteRangeAsString(index, length);
		interp.setResult(str);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseCharIndex --
	 * 
	 * Converts byte oriented index values into character oriented index values.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: The interp's result is set.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseCharIndex(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			int index, // Index to the starting point of the
			// script, in bytes.
			int length) // Length of script be parsed, in bytes.
			throws TclException {
		// Count number of characters from the start of the
		// script to the given byte index.

		int charIndex = script.getCharIndex(index);
		interp.setResult(charIndex - script.index);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseCharLength --
	 * 
	 * Converts the given byte length into a character count.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: The interp's result is set.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseCharLength(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			int index, // Index to the starting point of the
			// script, in bytes.
			int length) // Length of script be parsed, in bytes.
			throws TclException {
		// Count number of characters from the byte index
		// to the byte length.

		int charLength = script.getCharRange(index, length);
		interp.setResult(charLength);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseCountNewline --
	 * 
	 * Count the number of newlines between a range of bytes in a script. If two
	 * ranges are passed to this function, calculate the number of newlines from
	 * the beginning index of the first range up to, but not including, the
	 * beginning of the second range. If one range is passed in, count the
	 * number of newlines from the beginning of the first range through the last
	 * character in the range.
	 * 
	 * It is assumed that the indices and lengths are within the boundaries of
	 * the script. No error checking is done to verify this. Use the
	 * ParseGetIndexAndRange to validate the data.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: The interp's result is set to the number of newlines
	 * counted.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseCountNewline(Interp interp, // Current interpreter.
			UTF8CharPointer script, // Script to parse.
			int scriptLength, // Lengths of script in bytes.
			TclObject rangePtr1, // Begin counting newlines with this range.
			TclObject rangePtr2) // Possibly null, otherwise used to terminate
			// newline counting
			throws TclException {
		int subStrIndex, endStrIndex;
		int offset, index1, index2 = 0;
		int length, length1, length2;
		int listLen1, listLen2;
		int numNewline;

		listLen1 = TclList.getLength(interp, rangePtr1);
		ParseGetIndexAndLengthResult result = new ParseGetIndexAndLengthResult();
		ParseGetIndexAndLength(interp, rangePtr1, scriptLength, result);
		index1 = result.indexPtr;
		length1 = result.lengthPtr;

		if (rangePtr2 != null) {
			listLen2 = TclList.getLength(interp, rangePtr2);
			ParseGetIndexAndLength(interp, rangePtr2, scriptLength, result);
			index2 = result.indexPtr;
			length2 = result.lengthPtr;
		} else {
			listLen2 = 0;
		}

		if ((listLen1 == 0) && (listLen2 == 2)) {
			// Counting from the beginning of the file to
			// the beginning of the second range.
			//
			// example: parse count script {} r2

			offset = 0;
			length = index2;
		} else if ((listLen1 == 2) && (listLen2 == 2)) {
			// Counting from the beginning of the first
			// range to the beginning of the second range.
			//
			// example: parse count script r1 r2

			offset = index1;
			length = (index2 - offset);
		} else {
			// Counting from the beginning of the first
			// range to the end of the first range. If
			// the arg passed was an empty string it
			// will count the whole script.
			//
			// example: parse count script {}
			// parse count script r1

			offset = index1;
			length = length1;
		}

		subStrIndex = offset;
		endStrIndex = subStrIndex + length;
		numNewline = 0;

		// Get byte range as a String and count the number of
		// newlines found in that range.

		String range = script.getByteRangeAsString(subStrIndex, length);
		final int range_length = range.length();
		for (int i = 0; i < range_length; i++) {
			if (range.charAt(i) == '\n') {
				numNewline++;
			}
		}

		interp.setResult(numNewline);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseGetIndexAndLength --
	 * 
	 * Extract the index and length from a Tcl Object. If the Tcl Object does
	 * not contain data, return the beginning of the script as the index and the
	 * length of the script for the length. If the data in the script is out of
	 * the scripts range (e.g. < 0 or > scriptLength,) and scriptLen is >= 0,
	 * set the value to the closest point. Note that indexes and ranges are in
	 * terms of bytes.
	 * 
	 * Results: A standard Tcl result.
	 * 
	 * Side effects: The values are written to the result argument. If scriptLen
	 * is >= 0, the values will be normalized based on the length of the script.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static class ParseGetIndexAndLengthResult {
		int indexPtr; // Index to the starting point of the
		// script.
		int lengthPtr; // Byte length of script be parsed.
	}

	static void ParseGetIndexAndLength(Interp interp, // Current interpreter.
			TclObject rangePtr, int scriptLen, // Length of script in bytes. If
												// >= 0, then try
			// to normalize index and length based
			// on the length of the script.
			ParseGetIndexAndLengthResult result) throws TclException {
		TclObject itemPtr;
		int listLen;

		listLen = TclList.getLength(interp, rangePtr);
		if ((listLen != 0) && (listLen != 2)) {
			throw new TclException(interp,
					"invalid range input: incorrect list size");
		}
		if ((listLen == 0) && (scriptLen < 0)) {
			throw new TclException(interp,
					"empty range: no index or length values");
		}

		// If the range argument is null, then set 'index' to zero
		// and 'length' to the string length of the script. Otherwise
		// extract 'index' and 'length' from the list. If index or length
		// is < 0 then set it to 0, if index or length is > then the scripts
		// length, set it to the end of the script.

		if (listLen == 0) {
			result.indexPtr = 0;
			result.lengthPtr = scriptLen;
		} else {
			int len;
			String bytes;
			itemPtr = TclList.index(interp, rangePtr, 0);
			result.indexPtr = TclInteger.getInt(interp, itemPtr);
			itemPtr = TclList.index(interp, rangePtr, 1);
			bytes = itemPtr.toString();
			len = bytes.length();

			if (bytes.equals("end")) {
				result.lengthPtr = scriptLen;
			} else {
				result.lengthPtr = TclInteger.getInt(interp, itemPtr);
			}
			if (scriptLen >= 0) {
				if (result.indexPtr < 0) {
					result.indexPtr = 0;
				}
				if (result.lengthPtr < 0) {
					result.lengthPtr = 0;
				}
				if (result.indexPtr >= scriptLen) {
					result.indexPtr = scriptLen;
				}
				if (result.indexPtr + result.lengthPtr >= scriptLen) {
					result.lengthPtr = scriptLen - result.indexPtr;
				}
			}
		}
		return;
	}

} // end class TclParser

// This class is used to map UTF8 oriented byte indexes used in
// the Tcl API for the parser extension into character oriented
// index used within Jacl.

// String "Foo\u00c7bar"
// Chars: 0123 456

// Bytes: charToByteIndex byteToCharIndex
// [0] = 'f' [0] = 0 [0] = 0
// [1] = '0' [1] = 1 [1] = 1
// [2] = 'o' [2] = 2 [2] = 2
// [3] = '?' [3] = 3 [3] = 3
// [4] = '?' [4] = 3
// [5] = 'b' [4] = 5 [5] = 4
// [6] = 'a' [5] = 6 [6] = 5
// [7] = 'r' [6] = 7 [7] = 6

class UTF8CharPointer extends CharPointer implements InternalRep {
	int[] charToByteIndex; // Map char index to byte index
	int[] byteToCharIndex; // Map byte index to char index
	byte[] bytes;
	String orig;

	UTF8CharPointer(String s) {
		super(s);
		orig = s;
		getByteInfo();
	}

	void getByteInfo() {
		int charIndex, byteIndex, bytesThisChar, bytesTotal;

		try {
			// First, loop over the characters to see if each of the characters
			// can be represented as a single UTF8 byte. In this special
			// case there is no need to worry about mapping bytes to charaters
			// or vice versa.

			char c;
			boolean singleBytes = true;

			for (char anArray : array) {
				c = anArray;
				if (c == '\0') {
					// Ignore encoding issues related to null byte in Java vs
					// UTF8
					bytesThisChar = 1;
				} else {
					bytesThisChar = StringCmd.Utf8Count(c);
				}
				if (bytesThisChar != 1) {
					singleBytes = false;
					break;
				}
			}

			// When each character maps to a single byte, bytes is null

			if (singleBytes) {
				bytes = null;
				return;
			}

			// When multiple byte UTF8 characters are found, convert to
			// a byte array and save mapping info.

			String chars = new String(array); // Get string including trailing
												// null
			bytes = chars.getBytes("UTF8");

			if (chars == null) { // For debugging only
				System.out.println("chars is \"" + chars + "\" len = "
						+ chars.length());
				String bstr = new String(bytes, 0, bytes.length, "UTF8");
				System.out.println("bytes is \"" + bstr + "\" len = "
						+ bytes.length);
			}

			// Count UTF8 bytes for each character, map char to byte index

			charToByteIndex = new int[array.length];

			for (charIndex = 0, byteIndex = 0; charIndex < charToByteIndex.length; charIndex++) {
				charToByteIndex[charIndex] = byteIndex;

				c = array[charIndex];
				if (c == '\0') {
					// Ignore encoding issues related to null byte in Java vs
					// UTF8
					bytesThisChar = 1;
				} else {
					bytesThisChar = StringCmd.Utf8Count(c);
				}
				byteIndex += bytesThisChar;
			}

			// Double check that the number of expected bytes
			// was generated.
			bytesTotal = byteIndex;

			if (bytes.length != bytesTotal) {
				throw new TclRuntimeError("generated " + bytes.length
						+ " but expected to generate " + bytesTotal + " bytes");
			}

			// Count Utf8 bytes for each character, map byte to char index

			byteToCharIndex = new int[bytes.length];
			for (charIndex = 0, byteIndex = 0, bytesThisChar = 0; byteIndex < byteToCharIndex.length; byteIndex++, bytesThisChar--) {
				if (byteIndex > 0 && bytesThisChar == 0) {
					charIndex++;
				}
				byteToCharIndex[byteIndex] = charIndex;

				c = array[charIndex];
				if (bytesThisChar == 0) {
					if (c == '\0') {
						// Ignore encoding issues related to null byte in Java
						// vs UTF8
						bytesThisChar = 1;
					} else {
						bytesThisChar = StringCmd.Utf8Count(c);
					}
				}
			}
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new TclRuntimeError("UTF8 encoding not supported");
		}
	}

	// Return bytes in the given byte range as a String

	String getByteRangeAsString(int byteIndex, int byteLength) {
		if (bytes == null) {
			// One byte for each character
			return orig.substring(byteIndex, byteIndex + byteLength);
		}

		try {
			return new String(bytes, byteIndex, byteLength, "UTF8");
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new TclRuntimeError("UTF8 encoding not supported");
		}
	}

	// Convert char index into a byte index.

	int getByteIndex(int charIndex) {
		if (bytes == null) {
			// One byte for each character
			return charIndex;
		}

		return charToByteIndex[charIndex];
	}

	// Given a char index and range, return the number of
	// bytes in the range.

	int getByteRange(int charIndex, int charRange) {
		if (bytes == null) {
			// One byte for each character
			return charRange;
		}

		return charToByteIndex[charIndex + charRange]
				- charToByteIndex[charIndex];
	}

	// Get number of bytes for the given char index

	int getBytesAtIndex(int charIndex) {
		if (bytes == null) {
			// One byte for each character
			return 1;
		}

		return charToByteIndex[charIndex + 1] - charToByteIndex[charIndex];
	}

	// Return length of script in bytes

	int getByteLength() {
		if (bytes == null) {
			// One byte for each character
			return orig.length();
		}

		return bytes.length - 1;
	}

	// Given a byte index, return the char index.

	int getCharIndex(int byteIndex) {
		if (bytes == null) {
			// One byte for each character
			return byteIndex;
		}

		return byteToCharIndex[byteIndex];
	}

	// Given a byte index and range, return the number of
	// chars in the range.

	int getCharRange(int byteIndex, int byteRange) {
		if (bytes == null) {
			// One byte for each character
			return byteRange;
		}

		return byteToCharIndex[byteIndex + byteRange]
				- byteToCharIndex[byteIndex];
	}

	// This API is used for debugging, it would never be invoked as part
	// of the InternalRep interface since a TclObject would always have
	// a string rep when the UTF8CharPointer is created and it should
	// never be invalidated.

	public String toString() {
		if (bytes == null) {
			// One byte for each character
			return "1 byte for each character with length " + orig.length();
		}

		StringBuilder sb = new StringBuilder();

		int max_char = array.length - 1;
		int max_byte = bytes.length - 1;
		int max = max_char;
		if (max_byte > max) {
			max = max_byte;
		}
		sb.append("index char/byte array: (sizes = " + max_char + " "
				+ max_byte + ")\n");

		for (int i = 0; i < max; i++) {
			String char_ind = "   ", byte_ind = "   ";
			if (i < max_char) {
				char_ind = "'" + array[i] + "'";
			}
			if (i < max_byte) {
				byte_ind = "'" + ((char) bytes[i]) + "'";
			}

			sb.append("[" + i + "] = " + char_ind + " " + byte_ind + "\n");
		}
		sb.append("\n");

		sb.append("charToByteIndex array:\n");
		for (int i = 0; i < charToByteIndex.length - 1; i++) {
			sb.append("[" + i + "] = " + charToByteIndex[i] + "\n");
		}
		sb.append("\n");

		sb.append("byteToCharIndex array:\n");
		for (int i = 0; i < byteToCharIndex.length - 1; i++) {
			sb.append("[" + i + "] = " + byteToCharIndex[i] + "\n");
		}
		sb.append("\n");

		return sb.toString();
	}

	// InternalRep interfaces

	// Called to free any storage for the type's internal rep.

	public void dispose() {
	}

	// duplicate

	public InternalRep duplicate() {
		// A UTF8CharPointer is read-only, so just dup the ref
		return this;
	}

}
