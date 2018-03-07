/* 
 * ParseAdaptor.java --
 *
 *	Temporary adaptor class that creates the interface from the 
 *	current expression parser to the new Parser class.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: ParseAdaptor.java,v 1.6 2003/02/05 09:24:40 mdejong Exp $
 */

package tcl.lang;

class ParseAdaptor {

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * parseVar --
	 * 
	 * Extract the variable from the String and return it's value. The current
	 * expr parser passes an index after $, while the new Parser.parseVar
	 * expects the index to be at the $. The ParseResult returned by
	 * Parser.parseVar contains a nextIndex relative to the beginning of the
	 * variable. Reset nextIndex to be from the beginning of the string.
	 * 
	 * Results: A ParseResult that contains the value of the variable and an
	 * index to the character after the varaible.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static ParseResult parseVar(Interp interp, // The current Interp.
			String string, // The script containing the variable.
			int index, // An index into string that points to.
			// the character just after the $.
			int length) // The length of the string.
			throws TclException {
		ParseResult result;

		index--;
		result = Parser.parseVar(interp, string.substring(index, length));
		result.nextIndex += index;
		return (result);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * parseNestedCmd --
	 * 
	 * Parse the nested command in string. The index points to the character
	 * after the [. Set the interp flag to denote a nested evaluation.
	 * 
	 * Results: A ParseResult with the value of the executed command and an
	 * index into string that points to the character after the ].
	 * 
	 * Side effects: The call to eval2 may alter the state of the interp.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static ParseResult parseNestedCmd(Interp interp, // The current Interp.
			String string, // The script containing the nested command.
			int index, // An index into string that points to.
			// the character just after the [.
			int length) // The length of the string.
			throws TclException {
		CharPointer script;
		TclObject obj;

		// Check for the easy case where the last character in the string is
		// '['.
		if (index == length) {
			throw new TclException(interp, "missing close-bracket");
		}

		script = new CharPointer(string);
		script.index = index;

		interp.evalFlags |= Parser.TCL_BRACKET_TERM;
		Parser.eval2(interp, script.array, script.index, length - index, 0);
		obj = interp.getResult();
		obj.preserve();
		return (new ParseResult(obj, index + interp.termOffset + 1));
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * parseQuotes --
	 * 
	 * Use the new Parser to parse the quoted string. Index points to the
	 * character in string just after the first double-quotes
	 * 
	 * Note: Before the new parser, there used to be a specific function to
	 * parse quoted strings. Since this is gone now, we need to initialize a
	 * parse object before Parser.parseTokens is called. If this is not done,
	 * the call to Parser.evalTokens will fail.
	 * 
	 * Results: A ParseResult with the value of the quoted string and an index
	 * into string that points to the character after the double-quotes.
	 * 
	 * Side effects: If the quotes contain a command it will be evaluated.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static ParseResult parseQuotes(Interp interp, // The current Interp.
			String string, // The script containing the variable.
			int index, // An index into string that points to.
			// the character just after the double-qoute.
			int length) // The length of the string.
			throws TclException {
		TclObject obj;
		TclParse parse = null;
		TclToken token;
		CharPointer script;

		final boolean debug = false;

		try {

			script = new CharPointer(string);
			script.index = index;

			parse = new TclParse(interp, script.array, length, null, 0);

			if (debug) {
				System.out.println("string is \"" + string + "\"");
				System.out.println("script.array is \""
						+ new String(script.array) + "\"");

				System.out.println("index is " + index);
				System.out.println("length is " + length);

				System.out.println("parse.endIndex is " + parse.endIndex);
			}

			parse.commandStart = script.index;
			token = parse.getToken(0);
			token.type = Parser.TCL_TOKEN_WORD;
			token.script_array = script.array;
			token.script_index = script.index;
			parse.numTokens++;
			parse.numWords++;
			parse = Parser.parseTokens(script.array, script.index,
					Parser.TYPE_QUOTE, parse);

			// Check for the error condition where the parse did not end on
			// a '"' char. Is this happened raise an error.

			if (script.array[parse.termIndex] != '"') {
				throw new TclException(interp, "missing \"");
			}

			// if there was no error then parsing will continue after the
			// last char that was parsed from the string

			script.index = parse.termIndex + 1;

			// Finish filling in the token for the word and check for the
			// special case of a word consisting of a single range of
			// literal text.

			token = parse.getToken(0);
			token.size = script.index - token.script_index;
			token.numComponents = parse.numTokens - 1;
			if ((token.numComponents == 1)
					&& (parse.getToken(1).type == Parser.TCL_TOKEN_TEXT)) {
				token.type = Parser.TCL_TOKEN_SIMPLE_WORD;
			}
			parse.commandSize = script.index - parse.commandStart;
			if (parse.numTokens > 0) {
				obj = Parser.evalTokens(interp, parse.tokenList, 1,
						parse.numTokens - 1);
			} else {
				throw new TclRuntimeError("parseQuotes error: null obj result");
			}

		} finally {
			parse.release();
		}

		return (new ParseResult(obj, script.index));
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * parseBraces --
	 * 
	 * The new Parser dosen't handle simple parsing of braces. This method
	 * extracts tokens until a close brace is found.
	 * 
	 * Results: A ParseResult with the contents inside the brace and an index
	 * after the closing brace.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static ParseResult parseBraces(Interp interp, // The current Interp.
			String str, // The script containing the variable.
			int index, // An index into string that points to.
			// the character just after the {.
			int length) // The length of the string.
			throws TclException {
		char[] arr = str.toCharArray();
		int level = 1;

		for (int i = index; i < length;) {
			if (Parser.charType(arr[i]) == Parser.TYPE_NORMAL) {
				i++;
			} else if (arr[i] == '}') {
				level--;
				if (level == 0) {
					str = new String(arr, index, i - index);
					return new ParseResult(str, i + 1);
				}
				i++;
			} else if (arr[i] == '{') {
				level++;
				i++;
			} else if (arr[i] == '\\') {
				BackSlashResult bs = Parser.backslash(arr, i);
				i = bs.nextIndex;
			} else {
				i++;
			}
		}

		// if you run off the end of the string you went too far
		throw new TclException(interp, "missing close-brace");
	}
} // end ParseAdaptor
