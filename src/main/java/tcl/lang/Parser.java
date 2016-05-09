/* 
 * Parser.java --
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: Parser.java,v 1.30 2006/08/03 22:33:12 mdejong Exp $
 */

package tcl.lang;

import java.util.Arrays;

/**
 * 	This class contains methods that parse Tcl scripts.  They
 *	do so in a general-purpose fashion that can be used for many
 *	different purposes, including compilation, direct execution,
 *	code analysis, etc.  This class also includes a few additional
 *	procedures such as evalObjv, eval, and eval2, which allow 
 *	scripts to be evaluated directly, without compiling.
 */
public class Parser {

	/**
	 * Given a script, parse the first Tcl command in the string
	 * and returns information about the structure of the command.
	 * 
	 * @param interp Interpreter to use for error reporting; if null, then no error message 
	 *               is provided.
	 * @param script_array References the script
	 * @param script_index index of next character in script to parse
	 * @param numChars Total number of characters in string. If < 0,
	 *                 the script consists of all characters up to the
	 *                 first null character
	 * @param fileName Name of file from which script was loaded. Null means there is no
	 *                 known file for the script. Used for error messages.
	 * @param lineNum Line number of first byte in string. Used for error messages.
	 * @param nested True means that this is a nested command: close bracket should be considered
	 *               a command terminator. If false, then close bracket has no special meaning.
	 * @return a TclParse object that contains information
	 *         about the parsed command. If the command was parsed successfully, the
	 *         TclParse object's result variable is set to TCL_OK and TCL_ERROR
	 *         otherwise. If an error occurs and interp isn't null then an error message
	 *         is left in its result. Side effects: None.
	 */
	static TclParse parseCommand(Interp interp, char[] script_array, 
			int script_index, 
			int numChars, 
			String fileName, 
			int lineNum, 
			boolean nested) 
	{

		char cur; // the char we are currently parsing
		int type; // Result returned by charType(src.charAt()).
		TclToken token; // Pointer to token being filled in.
		int wordIndex; // Index of word token for current word.
		int level; // Nesting level of curly braces: gives
		// number of right braces we must find to
		// end word.
		TclParse parse; // Return value to fill in with information
		// about the parsed command.
		int terminators; // charType() bits that indicate the end
		// of a command.
		BackSlashResult bs; // Result of a call to backslash(...).
		int endIndex; // Index that points to the character after
		// the last character to be parsed.
		char savedChar; // To terminate the parsing correctly, the
		// character at endIndex is set to \0. This
		// stores the value to return when finished.

		final boolean debug = false;

		if (debug) {
			System.out.println();
			System.out.println("Entered Parser.parseCommand()");
			System.out.print("now to parse the string \"");
			for (int k = script_index; k < script_array.length; k++) {
				System.out.print(script_array[k]);
			}
			System.out.println("\"");
		}

		int saved_script_index = script_index; // save the original index

		int script_length = script_array.length - 1;

		int scanned;
		ParseWhitespaceResult pwsr;

		if (numChars < 0) {
			numChars = script_length - script_index;
		}
		endIndex = script_index + numChars;
		if (endIndex > script_length) {
			endIndex = script_length;
		}

		savedChar = script_array[endIndex];
		script_array[endIndex] = '\0';

		parse = new TclParse(interp, script_array, endIndex, fileName, lineNum);

		if (nested) {
			terminators = TYPE_COMMAND_END | TYPE_CLOSE_BRACK;
		} else {
			terminators = TYPE_COMMAND_END;
		}

		// Parse any leading space and comments before the first word of the
		// command.

		// FIXME: Use TclParseWhiteSpace to scan whitespace here!

		try {

			while (true) {

				cur = script_array[script_index];

				while (((cur <= TYPE_MAX) && (typeTable[cur] == TYPE_SPACE))
						|| (cur == '\n')) {
					cur = script_array[++script_index];
				}

				if ((cur == '\\')) {
					int eolCharCount = eolCharCount(script_array, script_index+1, parse.endIndex);
						if (eolCharCount > 0) {
	
						// Skip backslash-newline sequence: it should be treated
						// just like white space.
	
						if ((script_index + 1 + eolCharCount) == parse.endIndex) {
							parse.incomplete = true;
						}
	
						// this will add 2 to the offset and return to the top
						// of the while(true) loop which will get the next cur
	
						script_index += eolCharCount+1;
						continue;
					}
				}

				// If we have found the start of a command goto the word parsing
				// loop
				if (cur != '#') {
					break;
				}

				// Record the index where the comment starts
				if (parse.commentStart < 0) {
					parse.commentStart = script_index;
				}

				while (true) {
					cur = script_array[script_index];
					if (script_index == parse.endIndex) {
						if (nested) {
							parse.incomplete = true;
						}
						parse.commentSize = script_index - parse.commentStart;
						break;
					} else if (cur == '\\') {
						int eolCharCount = eolCharCount(script_array, script_index+1, parse.endIndex);
						if (eolCharCount > 0 && 
								 ((script_index + eolCharCount + 1) == parse.endIndex)) {
							parse.incomplete = true;
						}
						bs = backslash(script_array, script_index);
						script_index = bs.nextIndex;
					} else if (cur == '\n') {
						script_index++;
						parse.commentSize = script_index - parse.commentStart;
						break;
					} else {
						script_index++;
					}
				}
			}

			// The following loop parses the words of the command, one word
			// in each iteration through the loop.

			parse.commandStart = script_index;

			while (true) {

				// Create the token for the word.
				wordIndex = parse.numTokens;

				token = parse.getToken(wordIndex);
				token.type = TCL_TOKEN_WORD;

				// Skip white space before the word. Also skip a
				// backslash-newline
				// sequence: it should be treated just like white space.

				while (true) {
					cur = script_array[script_index];
					type = ((cur > TYPE_MAX) ? TYPE_NORMAL : typeTable[cur]);

					int eolCharCount = 0;
					if (type == TYPE_SPACE) {
						script_index++;
						continue;
					} else if ((cur == '\\')
							&& (eolCharCount = eolCharCount(script_array, script_index+1, parse.endIndex)) > 0) {
						if ((script_index + eolCharCount + 1) == parse.endIndex) {
							parse.incomplete = true;
						}
						bs = backslash(script_array, script_index);
						script_index = bs.nextIndex;
						continue;
					}
					break;
				}
				if ((type & terminators) != 0) {
					parse.termIndex = script_index;
					script_index++;
					break;
				}

				if (script_index == parse.endIndex) {
					if (nested && savedChar != ']') {
						// parse.termIndex = token.script_index;
						parse.incomplete = true;
						parse.errorType = Parser.TCL_PARSE_MISSING_BRACKET;
						throw new TclException(interp, "missing close-bracket");
					}
					break;
				}

				token.script_array = script_array;
				token.script_index = script_index;

				parse.numTokens++;
				parse.numWords++;

				// At this point the word can have one of three forms: something
				// enclosed in quotes, something enclosed in braces, or an
				// unquoted word (anything else).

				cur = script_array[script_index];

				if (cur == '"') {
					script_index++;
					parse = parseTokens(script_array, script_index, TYPE_QUOTE,
							parse);
					if (parse.result != TCL.OK) {
						throw new TclException(parse.result);
					}
					if (parse.string[parse.termIndex] != '"') {
						parse.termIndex = script_index - 1;
						parse.incomplete = true;
						parse.errorType = Parser.TCL_PARSE_MISSING_QUOTE;
						throw new TclException(parse.interp, "missing \"");
					}
					script_index = parse.termIndex + 1;
				} else if (cur == '{') {
					// Find the matching right brace that terminates the word,
					// then generate a single token for everything between the
					// braces.

					script_index++;
					token = parse.getToken(parse.numTokens);
					token.type = TCL_TOKEN_TEXT;
					token.script_array = script_array;
					token.script_index = script_index;
					token.numComponents = 0;
					level = 1;
					while (true) {
						cur = script_array[script_index];

						// get the current char in the array and lookup its type
						while (((cur > TYPE_MAX) ? TYPE_NORMAL : typeTable[cur]) == TYPE_NORMAL) {
							cur = script_array[++script_index];
						}
						if (script_array[script_index] == '}') {
							level--;
							if (level == 0) {
								break;
							}
							script_index++;
						} else if (script_array[script_index] == '{') {
							level++;
							script_index++;
						} else if (script_array[script_index] == '\\') {
							bs = backslash(script_array, script_index);
							int eolCharCount = eolCharCount(script_array, script_index+1, parse.endIndex);
							if (eolCharCount > 0) {
								// A backslash-newline sequence requires special
								// treatment: it must be collapsed, even inside
								// braces, so we have to split the word into
								// multiple tokens so that the backslash-newline
								// can be represented explicitly.

								if ((script_index + eolCharCount + 1) == parse.endIndex) {
									parse.incomplete = true;
								}
								token.size = script_index - token.script_index;
								if (token.size != 0) {
									parse.numTokens++;
								}
								token = parse.getToken(parse.numTokens);
								token.type = TCL_TOKEN_BS;
								token.script_array = script_array;
								token.script_index = script_index;
								token.size = bs.nextIndex - script_index;
								token.numComponents = 0;
								parse.numTokens++;
								script_index = bs.nextIndex;
								token = parse.getToken(parse.numTokens);
								token.type = TCL_TOKEN_TEXT;
								token.script_array = script_array;
								token.script_index = script_index;
								token.numComponents = 0;
							} else {
								script_index = bs.nextIndex;
							}
						} else if (script_index == parse.endIndex) {
							parse.termIndex = parse.getToken(wordIndex).script_index;
							parse.incomplete = true;
							parse.errorType = Parser.TCL_PARSE_MISSING_BRACE;
							throw new TclException(interp,
									"missing close-brace");
						} else {
							script_index++;
						}
					}
					if ((script_index != token.script_index)
							|| (parse.numTokens == (wordIndex + 1))) {
						token.size = script_index - token.script_index;
						parse.numTokens++;
					}
					script_index++;
				} else {
					// This is an unquoted word. Call parseTokens and let it do
					// all of the work.

					parse = parseTokens(script_array, script_index, TYPE_SPACE
							| terminators, parse);
					if (parse.result != TCL.OK) {
						throw new TclException(parse.result);
					}
					script_index = parse.termIndex;
				}

				// Finish filling in the token for the word and check for the
				// special case of a word consisting of a single range of
				// literal text.

				token = parse.getToken(wordIndex);
				token.size = script_index - token.script_index;
				token.numComponents = parse.numTokens - (wordIndex + 1);
				if ((token.numComponents == 1)
						&& (parse.getToken(wordIndex + 1).type == TCL_TOKEN_TEXT)) {
					token.type = TCL_TOKEN_SIMPLE_WORD;
				}

				// Do two additional checks: (a) make sure we're really at the
				// end of a word (there might have been garbage left after a
				// quoted or braced word), and (b) check for the end of the
				// command.

				numChars = endIndex - script_index;
				pwsr = ParseWhiteSpace(script_array, script_index, numChars,
						parse);
				scanned = pwsr.numScanned;
				type = pwsr.type;

				if (scanned > 0) {
					script_index += scanned;
					numChars -= scanned;
					continue;
				}

				// if (numChars == 0) {
				// parse.termIndex = script_index;
				// break;
				// }

				if ((type & terminators) != 0) {
					parse.termIndex = script_index;
					script_index++;
					break;
				}

				if (script_index == parse.endIndex) {
					if (nested && savedChar != ']') {
						// parse.termIndex = token.script_index;
						parse.incomplete = true;
						parse.errorType = Parser.TCL_PARSE_MISSING_BRACKET;
						throw new TclException(interp, "missing close-bracket");
					}
					break;
				}

				parse.termIndex = script_index;
				if (script_array[script_index - 1] == '"') {
					parse.errorType = Parser.TCL_PARSE_QUOTE_EXTRA;
					throw new TclException(interp,
							"extra characters after close-quote");
				} else {
					parse.errorType = Parser.TCL_PARSE_BRACE_EXTRA;
					throw new TclException(interp,
							"extra characters after close-brace");
				}
			}
		} catch (TclException e) {
			script_array[endIndex] = savedChar;
			if (parse.commandStart < 0) {
				parse.commandStart = saved_script_index;
			}
			parse.commandSize = parse.termIndex - parse.commandStart;
			parse.result = TCL.ERROR;
			return parse;
		}

		script_array[endIndex] = savedChar;
		parse.commandSize = script_index - parse.commandStart;
		parse.result = TCL.OK;
		return parse;
	}
	
	/**
	 * Detects an eol character sequence in script_array
	 * 
	 * @param script_array the char array containing the script
	 * @param script_index index into script_array at which to start looking for an eol char sequence
	 * @param end_index last character index + 1 in the script array
	 * @return number of eol chars at script_index; if 0 there are no eol chars
	 */
	static int eolCharCount(char [] script_array, int script_index, int end_index) {
		if (script_index < end_index && script_array[script_index]=='\n') return 1;
		if (script_index < end_index+1 && script_array[script_index]=='\r'
				&& script_array[script_index+1]=='\n') return 2;
		return 0;
	}
	/**
	 * Test whether a script can be parsed, without actually doing any execution
	 * @param script script to parse
	 * @param nested script is nested; should consider bracket as close 
	 * @return true if script has no parsing errors; false otherwise
	 */
	public static boolean isParseableScript(String script, boolean nested) {
		TclParse parse = null;
		CharPointer src = new CharPointer(script);
		int len = script.length();
		int parseError = Parser.TCL_PARSE_SUCCESS;

		do {
			parse = parseCommand(null, src.array, src.index, len, null, 0, nested);
			parseError = parse.errorType;
			src.index = parse.commandStart + parse.commandSize;
			parse.release(); // Release parser resources
			if (src.index >= len) {
				break;
			}
		} while (parseError == Parser.TCL_PARSE_SUCCESS);
		
		return (parseError == Parser.TCL_PARSE_SUCCESS);
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * parseTokens --
	 * 
	 * This procedure forms the heart of the Tcl parser. It parses one or more
	 * tokens from a string, up to a termination point specified by the caller.
	 * This procedure is used to parse unquoted command words (those not in
	 * quotes or braces), words in quotes, and array indices for variables.
	 * 
	 * Results: Tokens are added to parse and parse.termIndex is filled in with
	 * the index of the character that terminated the parse (the first one that
	 * has a type matching the mask or the character at parse.endIndex). The
	 * return value is TclParse object that contains information about the
	 * parsed command. If the command was parsed successfully, the TclParse
	 * object's result variable is set to TCL_OK and TCL_ERROR otherwise. If an
	 * error occurs and interp isn't null then an error message is left in its
	 * result.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static TclParse parseTokens(char[] script_array, // The text to parse
			int script_index, // Index of first character to parse.

			int mask, // Specifies when to stop parsing. The
			// parse stops at the first unquoted
			// character whose charType() contains
			// any of the bits in mask.
			TclParse parse) // Information about parse in progress.
	// Updated with additional tokens and
	// termination information.
	{
		char cur;
		int type, originalTokens, varToken;
		TclToken token;
		TclParse nested;
		BackSlashResult bs;

		final boolean debug = false;

		if (debug) {
			System.out.println();
			System.out.println("Entered Parser.parseTokens()");
			System.out.print("now to parse the string \"");
			for (int k = script_index; k < script_array.length; k++) {
				System.out.print(script_array[k]);
			}
			System.out.println("\"");
		}

		// Each iteration through the following loop adds one token of
		// type TCL_TOKEN_TEXT, TCL_TOKEN_BS, TCL_TOKEN_COMMAND, or
		// TCL_TOKEN_VARIABLE to parsePtr. For TCL_TOKEN_VARIABLE additional,
		// tokens tokens are added for the parsed variable name.

		originalTokens = parse.numTokens;
		while (true) {
			token = parse.getToken(parse.numTokens);
			token.script_array = script_array;
			token.script_index = script_index;
			token.numComponents = 0;

			if (debug) {
				System.out.println();
				System.out.println("Now on index " + script_index);
				char tmp_c = script_array[script_index];
				System.out.println("Char is '" + tmp_c + "'");
				System.out.println("Unicode id is " + ((int) tmp_c));
				int tmp_i = ((int) ((tmp_c > TYPE_MAX) ? TYPE_NORMAL
						: typeTable[tmp_c]));
				System.out.println("Type is " + tmp_i);
				System.out.println("Mask is " + mask);
				System.out
						.println("(type & mask) is " + ((int) (tmp_i & mask)));
				System.out.println("orig token.size is " + token.size);
			}

			cur = script_array[script_index];
			type = ((cur > TYPE_MAX) ? TYPE_NORMAL : typeTable[cur]);

			if ((type & mask) != 0) {
				if (debug) {
					System.out.println("mask break");
				}
				break;
			}

			if ((type & TYPE_SUBS) == 0) {
				// This is a simple range of characters. Scan to find the end
				// of the range.

				if (debug) {
					System.out.println("simple range");
				}

				while (true) {
					cur = script_array[++script_index];
					type = ((cur > TYPE_MAX) ? TYPE_NORMAL : typeTable[cur]);

					if (debug) {
						System.out.println("skipping '" + cur + "'");
					}

					if ((type & (mask | TYPE_SUBS)) != 0) {
						break;
					}
				}
				token.type = TCL_TOKEN_TEXT;
				token.size = script_index - token.script_index;
				parse.numTokens++;

				if (debug) {
					System.out.println("end simple range");
					System.out.println("token.size is " + token.size);
					System.out.println("parse.numTokens is " + parse.numTokens);
					System.out.println();
				}

			} else if (cur == '$') {
				// This is a variable reference. Call parseVarName to do
				// all the dirty work of parsing the name.

				if (debug) {
					System.out.println("dollar sign");
				}

				varToken = parse.numTokens;
				parse = parseVarName(parse.interp, script_array, script_index,
						parse.endIndex - script_index, parse, true);
				if (parse.result != TCL.OK) {
					return parse;
				}
				script_index += parse.getToken(varToken).size;
			} else if (cur == '[') {
				// Command substitution. Call parseCommand recursively
				// (and repeatedly) to parse the nested command(s), then
				// throw away the parse information.

				if (debug) {
					System.out.println("command");
				}

				script_index++;
				while (true) {
					nested = parseCommand(parse.interp, script_array,
							script_index, parse.endIndex - script_index,
							parse.fileName, parse.lineNum, true);
					if (nested.result != TCL.OK) {
						parse.termIndex = nested.termIndex;
						parse.incomplete = nested.incomplete;
						parse.errorType = nested.errorType;
						parse.result = nested.result;
						return parse;
					}
					script_index = nested.commandStart + nested.commandSize;
					if ((script_array[script_index - 1] == ']')
							&& !nested.incomplete) {
						break;
					}
					if (script_index == parse.endIndex) {
						if (parse.interp != null) {
							parse.interp.setResult("missing close-bracket");
						}
						parse.termIndex = token.script_index;
						parse.incomplete = true;
						parse.errorType = Parser.TCL_PARSE_MISSING_BRACKET;
						parse.result = TCL.ERROR;
						return parse;
					}
				}
				token.type = TCL_TOKEN_COMMAND;
				token.size = script_index - token.script_index;
				parse.numTokens++;
			} else if (cur == '\\') {
				// Backslash substitution.

				if (debug) {
					System.out.println("backslash");
				}
				int eolCharCount = eolCharCount(script_array, script_index+1, parse.endIndex);
				if (eolCharCount > 0) {
					if ((script_index + eolCharCount + 1) == parse.endIndex) {
						parse.incomplete = true;
					}

					// Note: backslash-newline is special in that it is
					// treated the same as a space character would be. This
					// means that it could terminate the token.

					if ((mask & TYPE_SPACE) != 0) {
						break;
					}
				}
				
				bs = backslash(script_array, script_index);
				token.type = TCL_TOKEN_BS;

				// token.size = bs.nextIndex - script_index;
				token.size = bs.count;
				if (token.size == 1) {
					// Just a backslash, due to end of string
					token.type = TCL_TOKEN_TEXT;
					parse.numTokens++;
					script_index++;
					// numBytes--;
					continue;
				}
				// FIXME: Add code for backslash newline processing
				parse.numTokens++;
				script_index += token.size;
			} else if (cur == '\0') {
				// We encountered a null character. If it is the null
				// character at the end of the string, then return.
				// Otherwise generate a text token for the single
				// character.

				if (debug) {
					System.out.println("null char");
					System.out.println("script_index is " + script_index);
					System.out.println("parse.endIndex is " + parse.endIndex);
				}

				if (script_index == parse.endIndex) {
					break;
				}

				token.type = TCL_TOKEN_TEXT;
				token.size = 1;
				parse.numTokens++;
				script_index++;
			} else {
				throw new TclRuntimeError(
						"parseTokens encountered unknown character");
			}
		} // end while (true)

		if (parse.numTokens == originalTokens) {
			// There was nothing in this range of text. Add an empty token
			// for the empty range, so that there is always at least one
			// token added.

			if (debug) {
				System.out.println("empty token");
			}

			token.type = TCL_TOKEN_TEXT;
			token.size = 0;
			parse.numTokens++;
		} else {
			if (debug) {
				System.out.println("non empty token case");
			}
		}

		parse.termIndex = script_index;
		parse.result = TCL.OK;

		if (debug) {
			System.out.println();
			System.out.println("Leaving Parser.parseTokens()");

			System.out.println("after parse, parse.numTokens is "
					+ parse.numTokens);
			System.out.println("after parse, token.size is " + token.size);
			System.out.println("after parse, token.hashCode() is "
					+ token.hashCode());

			// System.out.println( parse.toString() );

			System.out.print("printing " + (parse.numTokens - originalTokens)
					+ " token(s)");

			for (int k = originalTokens; k < parse.numTokens; k++) {
				token = parse.getToken(k);
				System.out.println(token);
			}

			System.out.println("done printing tokens");

		}

		return parse;
	}

	/**
	 * This procedure evaluates a Tcl command that has already been parsed into
	 * words, with one TclObject holding each word.
	 * 
	 * Results: A result or error message is left in interp's result. If an
	 * error occurs, this procedure does NOT add any information to the
	 * errorInfo variable.
	 * 
	 * @param interp
	 *            Interpreter in which to evaluate the ommand. Also used for
	 *            errorreporting.
	 *@param objv
	 *            An array of pointers to objects that are the words that make
	 *            up the command.
	 * @param length
	 *            Number of characters in command; if -1, all characters up to
	 *            the first null character are used. Currently unused.
	 * @param flags
	 *            Collection of OR-ed bits that control the evaluation of the
	 *            script. Only TCL.EVAL_GLOBAL is currently supported.
	 */

	static void evalObjv(Interp interp, TclObject[] objv, int length,  int flags)
			throws TclException {
		WrappedCommand cmd;
		TclObject[] newObjv = null;
		int i;
		CallFrame savedVarFrame; // Saves old copy of interp.varFrame
		// in case TCL.EVAL_GLOBAL was set.

		if (objv.length == 0) {
			interp.resetResult();
			return;
		}

		// Reset result, check for deleted interp, and check nest level

		interp.ready();

		interp.nestLevel++;
		savedVarFrame = interp.varFrame;

		try {
			// Find the procedure to execute this command. If there isn't one,
			// then see if there is a command "unknown". If so, create a new
			// word array with "unknown" as the first word and the original
			// command words as arguments. Then call ourselves recursively
			// to execute it.

			cmd = interp.getWrappedCommand(objv[0].toString());
			if (cmd == null) {
				newObjv = Parser.grabObjv(interp, objv.length + 1);
				for (i = (objv.length - 1); i >= 0; i--) {
					newObjv[i + 1] = objv[i];
				}
				newObjv[0] = TclString.newInstance("::unknown");
				newObjv[0].preserve();
				cmd = interp.getWrappedCommand("::unknown");
				if (cmd == null) {
					throw new TclException(interp, "invalid command name \""
							+ objv[0].toString() + "\"");
				} else {
					
					evalObjv(interp, newObjv, length, 0);
				}
				newObjv[0].release();
				Parser.releaseObjv(interp, newObjv, newObjv.length);
				return;
			}

			// Finally, invoke the Command's cmdProc.

			interp.cmdCount++;

			if ((flags & TCL.EVAL_GLOBAL) != 0) {
				interp.varFrame = null;
			}

			if (cmd.mustCallInvoke(interp)) cmd.invoke(interp, objv);
			else cmd.cmd.cmdProc(interp, objv);

			// (TODO)
			//
			// if (AsyncReady()) {
			// code = AsyncInvoke(interp, code);
			// }
		} finally {
			interp.varFrame = savedVarFrame;
			interp.nestLevel--;
		}
	}

	
	/**
	 * This procedure is invoked after an error occurs in an interpreter. It
	 * adds information to the "errorInfo" variable to describe the command that
	 * was being executed when the error occurred. Side effects: Information
	 * about the command is added to errorInfo and the line number stored
	 * internally in the interpreter is set. If this is the first call to this
	 * procedure or interp.addErrorInfo since an error occurred, then old
	 * information in errorInfo is deleted.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param script_array
	 *            script to be logged
	 * @param script_index
	 *            first character in script containing command; must be <=
	 *            cmdIndex
	 * @param cmdIndex
	 *            first character in command that generated the error
	 * @param length
	 *            number of bytes in command, or -1 to use all bytes up to first
	 *            null byte
	 * @param e
	 *            exception caused by the script evaluation
	 */
	public static void logCommandInfo(Interp interp, 
			char[] script_array,
			int script_index,
			int cmdIndex,
			int length,
			TclException e) 
	{
		String ellipsis;
		String msg=null;
		int offset;
		int pIndex;

		if (interp.errAlreadyLogged) {
			// Someone else has already logged error information for this
			// command; we shouldn't add anything more.

			return;
		}

		// Compute the line number where the error occurred.
		// Note: The script array must be accessed directly
		// because we want to count from the beginning of
		// the script, not the current index.

		interp.errorLine = 1;

		for (pIndex = 0; pIndex < cmdIndex; pIndex++) {
			if (script_array[pIndex] == '\n') {
				interp.errorLine++;
			}
		}

		// Create an error message to add to errorInfo, including up to a
		// maximum number of characters of the command.

		if (length < 0) {
			// take into account the trailing '\0'
			int script_length = script_array.length - 1;

			length = script_length - cmdIndex;
		}
		if (length > 150) {
			offset = 150;
			ellipsis = "...";
		} else {
			offset = length;
			ellipsis = "";
		}


		msg = new String(script_array, cmdIndex, offset);

		/*
		 * Modern TCL  has all these messages changed to
		 * "while compiling"; JTCL has some issues deciding which to use
		 * which causes test fails
		 */
		if (!(interp.errInProgress)) {
			interp.addErrorInfo("\n    while executing\n\"" + msg + ellipsis
					+ "\"");
		} else {
			interp.addErrorInfo("\n    invoked from within\n\"" + msg
					+ ellipsis + "\"");
		}
		interp.errAlreadyLogged = false;
		e.errIndex = cmdIndex + offset;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_EvalTokensStandard -> evalTokens
	 * 
	 * Given an array of tokens parsed from a Tcl command (e.g., the tokens that
	 * make up a word or the index for an array variable) this procedure
	 * evaluates the tokens and concatenates their values to form a single
	 * result value.
	 * 
	 * Results: The return value is a pointer to a newly allocated TclObject
	 * containing the value of the array of tokens. The reference count of the
	 * returned object has been incremented. If an error occurs in evaluating
	 * the tokens then a TclException is generated.
	 * 
	 * Side effects: A new object is allocated to hold the result.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static TclObject evalTokens(Interp interp, // Interpreter in which to lookup
			// variables, execute nested commands,
			// and report errors.
			TclToken[] tokenList, // Token list from a TclParse object.
			int tIndex, // Index to first token to evaluate
			// and concatenate.
			int count) // Number of tokens to consider at tIndex.
			// Must be at least 1.
			throws TclException {
		TclObject result, index, value;
		TclToken token;
		String p = null;
		String varName;
		BackSlashResult bs;

		// The only tricky thing about this procedure is that it attempts to
		// avoid object creation and string copying whenever possible. For
		// example, if the value is just a nested command, then use the
		// command's result object directly.

		result = null;
		for (; count > 0; count--) {
			token = tokenList[tIndex];

			// The switch statement below computes the next value to be
			// concat to the result, as either a range of text or an
			// object.

			value = null;
			switch (token.type) {
			case TCL_TOKEN_TEXT:
				p = token.getTokenString();
				break;

			case TCL_TOKEN_BS:
				bs = backslash(token.script_array, token.script_index);
				if (bs.isWordSep) {
					p = "\\" + bs.c;
				} else {
					Character ch = bs.c;
					p = ch.toString();
				}
				break;

			case TCL_TOKEN_COMMAND:
				interp.evalFlags |= Parser.TCL_BRACKET_TERM;
				token.script_index++;

				// should the nest level be changed???
				// interp.nestLevel++;

				eval2(interp, token.script_array, token.script_index,
						token.size - 2, 0);

				token.script_index--;
				// interp.nestLevel--;
				value = interp.getResult();
				break;

			case TCL_TOKEN_VARIABLE:
				if (token.numComponents == 1) {
					index = null;
				} else {
					index = evalTokens(interp, tokenList, tIndex + 2,
							token.numComponents - 1);
					if (index == null) {
						return null;
					}
				}
				varName = tokenList[tIndex + 1].getTokenString();

				// In order to get the existing expr parser to work with the
				// new Parser, we test the interp.noEval flag which is set
				// by the expr parser. If it is != 0, then we do not evaluate
				// the variable. This should be removed when the new expr
				// parser is implemented.

				if (interp.noEval == 0) {
					if (index != null) {
						try {
							value = interp.getVar(varName, index.toString(), 0);
						} finally {
							index.release();
						}
					} else {
						value = interp.getVar(varName, null, 0);
					}
				} else {
					value = TclString.newInstance("");
					value.preserve();
				}
				count -= token.numComponents;
				tIndex += token.numComponents;
				break;

			default:
				throw new TclRuntimeError("unexpected token type in evalTokens");
			}

			// If value isn't null, the next piece of text comes from that
			// object; otherwise, take value of p.

			if (result == null) {
				if (value != null) {
					result = value;
				} else {
					result = TclString.newInstance(p);
				}
				result.preserve();
			} else {
				if (result.isShared()) {
					result.release();
					result = result.duplicate();
					result.preserve();
				}
				if (value != null) {
					p = value.toString();
				}
				TclString.append(result, p);
			}
			tIndex++;
		}
		return result;
	}

	/**
	 * This procedure evaluates a Tcl script without using the compiler or
	 * byte-code interpreter. It just parses the script, creates values for each
	 * word of each command, then calls evalObjv to execute each command.
	 * 
	 * Results: A result or error message is left in interp's result.
	 * 
	 * @param interp
	 *            interpreter in which to evaluate the script and report errors
	 * @param script_array
	 *            array of characters containing script
	 * @param script_index
	 *            starting index into script array
	 * @param numChars
	 *            number of characters in script; if < 0, the script consists of
	 *            all characters up to the end
	 * @param flags
	 *            Tcl.EVAL_GLOBAL or 0
	 * @throws TclException          
	 */

	static void eval2(Interp interp,
			char[] script_array,
			int script_index, 
			int numChars,
			int flags) 
			throws TclException {
		int i;
		int objUsed = 0;
		int nextIndex, tokenIndex;
		int commandLength, charsLeft;
		boolean nested;
		TclObject[] objv;
		TclObject obj;
		TclParse parse = null;
		TclToken token;

		// Saves old copy of interp.varFrame in case TCL.EVAL_GLOBAL was set
		CallFrame savedVarFrame;

		// Take into account the trailing '\0'
		int script_length = script_array.length - 1;

		// These are modified instead of script_array and script_index
		char[] src_array = script_array;
		int src_index = script_index;

		// System.out.println("call to eval2");

		final boolean debug = false;

		if (debug) {
			System.out.println();
			System.out.println("Entered eval2()");
			System.out.print("now to eval2 the string \"");
			for (int k = script_index; k < script_array.length; k++) {
				System.out.print(script_array[k]);
			}
			System.out.println("\"");
		}

		if (numChars < 0) {
			numChars = script_length - script_index;
		}
		interp.resetResult();
		savedVarFrame = interp.varFrame;
		if ((flags & TCL.EVAL_GLOBAL) != 0) {
			interp.varFrame = null;
		}

		// Each iteration through the following loop parses the next
		// command from the script and then executes it.

		charsLeft = numChars;

		// Init objv with the most commonly used array size
		objv = grabObjv(interp, 3);

		if ((interp.evalFlags & TCL_BRACKET_TERM) != 0) {
			nested = true;
		} else {
			nested = false;
		}
		interp.evalFlags &= ~TCL_BRACKET_TERM;

		try {

			do {
				parse = parseCommand(interp, src_array, src_index, charsLeft,
						null, 0, nested);

				if (parse.result != TCL.OK) {
					throw new TclException(parse.result);
				}

				// The test on noEval is temporary. As soon as the new expr
				// parser is implemented it should be removed.

				if (parse.numWords > 0 && interp.noEval == 0) {
					// Generate an array of objects for the words of the
					// command.

					try {
						tokenIndex = 0;
						token = parse.getToken(tokenIndex);

						// Test to see if new space needs to be allocated. If
						// objv
						// is the EXACT size of parse.numWords, then no
						// allocation
						// needs to be performed.

						if (objv.length != parse.numWords) {
							// System.out.println("need new size " +
							// objv.length);
							releaseObjv(interp, objv, objv.length); // let go of
																	// resource
							objv = grabObjv(interp, parse.numWords); // get new
																		// resource
						} else {
							// System.out.println("reusing size " +
							// objv.length);
						}

						for (objUsed = 0; objUsed < parse.numWords; objUsed++) {
							obj = evalTokens(interp, parse.tokenList,
									tokenIndex + 1, token.numComponents);
							if (obj == null) {
								throw new TclException(TCL.ERROR);
							} else {
								objv[objUsed] = obj;
							}
							tokenIndex += (token.numComponents + 1);
							token = parse.getToken(tokenIndex);
						}

						// Execute the command and free the objects for its
						// words.
						try {
							evalObjv(interp, objv, /* src, */charsLeft, 0);
						} catch (StackOverflowError e) {
							Parser.infiniteLoopException(interp);
						}
					} catch (TclException e) {
						// Generate various pieces of error information, such
						// as the line number where the error occurred and
						// information to add to the errorInfo variable. Then
						// free resources that had been allocated
						// to the command.

						if (e.getCompletionCode() == TCL.ERROR
								&& !(interp.errAlreadyLogged)) {
							commandLength = parse.commandSize;

							char term = script_array[parse.commandStart
									+ commandLength - 1];
							int type = charType(term);
							int terminators;
							if (nested) {
								terminators = TYPE_COMMAND_END
										| TYPE_CLOSE_BRACK;
							} else {
								terminators = TYPE_COMMAND_END;
							}
							if ((type & terminators) != 0) {
								// The command where the error occurred didn't
								// end
								// at the end of the script (i.e. it ended at a
								// terminator character such as ";". Reduce the
								// length by one so that the error message
								// doesn't include the terminator character.

								commandLength -= 1;
							}
							interp.varFrame = savedVarFrame;
							logCommandInfo(interp, script_array, script_index,
									parse.commandStart, commandLength, e);
						}
						/*
						 *  set the interp.termOffset even on exception, so 'subst' can use
						 *  when a continue, break or return exception occurs
						 */
						interp.termOffset =  parse.commandStart + parse.commandSize - script_index;
						throw e;
					} finally {
						for (i = 0; i < objUsed; i++) {
							objv[i].release();
							objv[i] = null;
						}
						objUsed = 0;

						parse.release(); // Cleanup parser resources
					}
				}

				// Advance to the next command in the script.

				nextIndex = parse.commandStart + parse.commandSize;
				charsLeft -= (nextIndex - src_index);
				src_index = nextIndex;
				if (nested && (src_index > 1)
						&& (src_array[src_index - 1] == ']')) {

					// We get here in the special case where the
					// TCL_BRACKET_TERM
					// flag was set in the interpreter and we reached a close
					// bracket in the script. Return immediately.

					interp.termOffset = (src_index - 1) - script_index;
					interp.varFrame = savedVarFrame;
					return;
				}
			} while (charsLeft > 0);

		} finally {
			if (parse != null) {
				parse.release(); // Let go of parser resources
			}
			releaseObjv(interp, objv, objv.length); // Let go of objv buffer
		}

		interp.termOffset = src_index - script_index;
		interp.varFrame = savedVarFrame;
		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * parseVarName --
	 * 
	 * Given a string starting with a $ sign, parse off a variable name and
	 * return information about the parse.
	 * 
	 * Results: The return value is TclParse object that contains information
	 * about the parsed command. If the command was parsed successfully, the
	 * TclParse object's result variable is set to TCL_OK and TCL_ERROR
	 * otherwise. If an error occurs and interp isn't null then an error message
	 * is left in its result. On a successful return, tokenList and numTokens
	 * fields of TclParse are filled in with information about the variable name
	 * that was parsed. The "size" field of the first new token gives the total
	 * number of bytes in the variable name. Other fields in TclParse are
	 * undefined.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static TclParse parseVarName(Interp interp, // Interpreter to use for error
												// reporting;
			// if NULL, then no error message is
			// provided.

			char[] script_array, // String containing variable name. First
			int script_index, // character must be "$".

			int numBytes, // Total number of bytes in string. If < 0,
			// the string consists of all bytes up to the
			// first null character.
			TclParse parse, // Structure to fill in with information
			// about the variable name.
			boolean append) // Non-zero means append tokens to existing
	// information in parsePtr; zero means ignore
	// existing tokens in parsePtr and reinitialize
	// it.
	{
		char cur;
		TclToken token, startToken;
		int endIndex, varIndex;

		final boolean debug = false;

		if (debug) {
			System.out.println();
			System.out.println("Entered parseVarName()");
			System.out.print("now to parse var off the string \"");
			for (int k = script_index; k < script_array.length; k++) {
				System.out.print(script_array[k]);
			}
			System.out.println("\"");
		}

		if (numBytes >= 0) {
			endIndex = script_index + numBytes;
		} else {
			endIndex = script_array.length - 1;
		}
		if (!append) {
			parse = new TclParse(interp, script_array, endIndex, null, -1);
		}

		// Generate one token for the variable, an additional token for the
		// name, plus any number of additional tokens for the index, if
		// there is one.

		token = parse.getToken(parse.numTokens);
		token.type = TCL_TOKEN_VARIABLE;
		token.script_array = script_array;
		token.script_index = script_index;
		varIndex = parse.numTokens;
		parse.numTokens++;
		script_index++;
		if (script_index >= endIndex) {
			// The dollar sign isn't followed by a variable name.
			// replace the TCL_TOKEN_VARIABLE token with a
			// TCL_TOKEN_TEXT token for the dollar sign.

			token.type = TCL_TOKEN_TEXT;
			token.size = 1;
			token.numComponents = 0;
			parse.result = TCL.OK;
			return parse;
		}
		startToken = token;
		token = parse.getToken(parse.numTokens);

		// The name of the variable can have three forms:
		// 1. The $ sign is followed by an open curly brace. Then
		// the variable name is everything up to the next close
		// curly brace, and the variable is a scalar variable.
		// 2. The $ sign is not followed by an open curly brace. Then
		// the variable name is everything up to the next
		// character that isn't a letter, digit, or underscore.
		// :: sequences are also considered part of the variable
		// name, in order to support namespaces. If the following
		// character is an open parenthesis, then the information
		// between parentheses is the array element name.
		// 3. The $ sign is followed by something that isn't a letter,
		// digit, or underscore: in this case, there is no variable
		// name and the token is just "$".

		if (script_array[script_index] == '{') {
			if (debug) {
				System.out.println("parsing curley var name");
			}

			script_index++;
			token.type = TCL_TOKEN_TEXT;
			token.script_array = script_array;
			token.script_index = script_index;
			token.numComponents = 0;

			while (true) {
				if (script_index == endIndex) {
					if (interp != null) {
						interp
								.setResult("missing close-brace for variable name");
					}
					parse.termIndex = token.script_index - 1;
					parse.incomplete = true;
					parse.errorType = Parser.TCL_PARSE_MISSING_VAR_BRACE;
					parse.result = TCL.ERROR;
					return parse;
				}
				if (script_array[script_index] == '}') {
					break;
				}
				script_index++;
			}
			token.size = script_index - token.script_index;
			startToken.size = script_index - startToken.script_index;
			parse.numTokens++;
			script_index++;
		} else {
			if (debug) {
				System.out.println("parsing non curley var name");
			}

			token.type = TCL_TOKEN_TEXT;
			token.script_array = script_array;
			token.script_index = script_index;
			token.numComponents = 0;
			while (script_index != endIndex) {
				cur = script_array[script_index];
				if ((Character.isLetterOrDigit(cur)) || (cur == '_')) {
					script_index++;
					continue;
				}
				if ((cur == ':')
						&& (((script_index + 1) != endIndex) && (script_array[script_index + 1] == ':'))) {
					script_index += 2;
					while ((script_index != endIndex)
							&& (script_array[script_index] == ':')) {
						script_index++;
					}
					continue;
				}
				break;
			}
			token.size = script_index - token.script_index;
			if (token.size == 0) {
				// The dollar sign isn't followed by a variable name.
				// replace the TCL_TOKEN_VARIABLE token with a
				// TCL_TOKEN_TEXT token for the dollar sign.

				if (debug) {
					System.out.println("single $ with no var name found");
				}

				startToken.type = TCL_TOKEN_TEXT;
				startToken.size = 1;
				startToken.numComponents = 0;
				parse.result = TCL.OK;
				return parse;
			}
			parse.numTokens++;
			if ((script_index != endIndex)
					&& (script_array[script_index] == '(')) {
				// This is a reference to an array element. Call
				// parseTokens recursively to parse the element name,
				// since it could contain any number of substitutions.

				if (debug) {
					System.out.println("parsing array element");
				}

				script_index++;
				parse = parseTokens(script_array, script_index,
						TYPE_CLOSE_PAREN, parse);
				if (parse.result != TCL.OK) {
					return parse;
				}
				if ((parse.termIndex == endIndex)
						|| (parse.string[parse.termIndex] != ')')) {
					if (interp != null) {
						interp.setResult("missing )");
					}
					parse.termIndex = script_index - 1;
					parse.incomplete = true;
					parse.errorType = Parser.TCL_PARSE_MISSING_PAREN;
					parse.result = TCL.ERROR;
					return parse;
				}
				script_index = parse.termIndex + 1;
			}
		}

		if (debug) {
			System.out.println("default end parse case");
			System.out.print("var token is \"");
			for (int k = startToken.script_index; k < script_index; k++) {
				System.out.print(script_array[k]);
			}
			System.out.println("\"");
		}

		startToken.size = script_index - startToken.script_index;
		startToken.numComponents = parse.numTokens - (varIndex + 1);
		parse.result = TCL.OK;
		return parse;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * parseVar --
	 * 
	 * Given a string starting with a $ sign, parse off a variable name and
	 * return its value.
	 * 
	 * Results: The return value is a ParseResult object that contains the
	 * contents of the variable given by the leading characters of string and a
	 * index to the character just after the last one in the variable specifier.
	 * If the variable doesn't exist, then a TclException is generated.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	public static ParseResult parseVar(Interp interp, // Context for looking up
														// variable.
			String string) // String containing variable name.
			// First character must be "$".
			throws TclException {
		TclParse parse;
		TclObject obj;

		final boolean debug = false;

		if (debug) {
			System.out.println();
			System.out.println("Entered parseVar()");
			System.out.print("now to parse var off the string \"" + string
					+ "\"");
		}

		CharPointer src = new CharPointer(string);
		parse = parseVarName(interp, src.array, src.index, -1, null, false);
		if (parse.result != TCL.OK) {
			throw new TclException(interp, interp.getResult().toString());
		}

		try {
			if (debug) {
				System.out.println();
				System.out.print("parsed " + parse.numTokens + " tokens");
			}

			if (parse.numTokens == 1) {
				// There isn't a variable name after all: the $ is just a $.
				return new ParseResult("$", 1);
			}

			obj = evalTokens(interp, parse.tokenList, 0, parse.numTokens);
			if (!obj.isShared()) {
				throw new TclRuntimeError(
						"parseVar got temporary object from evalTokens");
			}
			return new ParseResult(obj, parse.tokenList[0].size);
		} finally {
			parse.release(); // Release parser resources
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * commandComplete --
	 * 
	 * This procedure is shared by interp.commandComplete and
	 * objCommandComplete; it does all the real work of seeing whether a script
	 * is complete.
	 * 
	 * Results: True is returned if the script is complete, false if there are
	 * open delimiters such as " or (. True is also returned if there is a parse
	 * error in the script other than unmatched delimiters.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static boolean commandComplete(String string, // Script to check.
			int charLength) // Number of characters in script.
	{
		TclParse parse;

		CharPointer src = new CharPointer(string);

		do {
			parse = parseCommand(null, src.array, src.index, charLength, null,
					0, false);

			src.index = parse.commandStart + parse.commandSize;

			parse.release(); // Release parser resources

			if (src.index >= charLength) {
				break;
			}
		} while (parse.result == TCL.OK);

		if (parse.incomplete) {
			return false;
		}
		return true;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * objCommandComplete --
	 * 
	 * Given a partial or complete Tcl command in a Tcl object, this procedure
	 * determines whether the command is complete in the sense of having matched
	 * braces and quotes and brackets.
	 * 
	 * Results: True is returned if the command is complete, false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static boolean objCommandComplete(TclObject obj) // Points to object holding
														// script
	// to check.
	{
		String string = obj.toString();
		return commandComplete(string, string.length());
	}


	/**
	 * Figure out how to handle a backslash sequence. The script_index value
	 * must be the index of the first 
	 * 
	 * @param script_array script to parse
	 * @param script_index index of the first '\'
	 * @return an instance of BackSlashResult that contains
	 * the character that should be substituted in place of the backslash
	 * sequence that starts at script_index, and an index to the next character
	 * after the backslash sequence.
	 */
	static BackSlashResult backslash(char[] script_array, // script to parse
			int script_index) // index of first backslash
	{
		int result;
		char c;
		int count;
		int numChars; // Max number of characters to scan

		// FIXME: max number of chars to parse is needed
		// for embedded null support. This needs to be
		// calculated in the caller.
		numChars = script_array.length - script_index;

		/* now script_index will point to the char right after backslash */
		script_index++;
		int endIndex = script_array.length - 1;

		if (script_index == endIndex) {
			count = 1;
			return new BackSlashResult('\\', script_index, count);
		}

		count = 2;
		c = script_array[script_index];
		switch (c) {
		case 'a': {
			return new BackSlashResult((char) 0x7, script_index + 1, count);
		}
		case 'b': {
			return new BackSlashResult((char) 0x8, script_index + 1, count);
		}
		case 'f': {
			return new BackSlashResult((char) 0xc, script_index + 1, count);
		}
		case 'n': {
			return new BackSlashResult('\n', script_index + 1, count);
		}
		case 'r': {
			return new BackSlashResult('\r', script_index + 1, count);
		}
		case 't': {
			return new BackSlashResult('\t', script_index + 1, count);
		}
		case 'v': {
			return new BackSlashResult((char) 0xb, script_index + 1, count);
		}
		case 'x': {
			script_index++;
			ParseHexResult pr = ParseHex(script_array, script_index,
					numChars - 1);
			count += pr.numScanned;

			if (count == 2) {
				// No hexadigits -> This is just "x".
				c = 'x';
			} else {
				// Keep only the last byte (2 hex digits)
				c = (char) (pr.result & 0xff);
			}
			return new BackSlashResult(c, script_index + pr.numScanned, count);
		}
		case 'u': {
			script_index++;
			ParseHexResult pr = ParseHex(script_array, script_index,
					(numChars > 5) ? 4 : numChars - 1);
			count += pr.numScanned;

			if (count == 2) {
				// No hexadigits -> This is just "u".
				c = 'u';
			} else {
				c = (char) pr.result;
			}
			return new BackSlashResult(c, script_index + pr.numScanned, count);
		}
		case '\r':
		case '\n': {
			if (c == '\r') {
				if ((script_index + 1) < endIndex) {
					if (script_array[script_index + 1] == '\n') {					
						script_index++;
						count++;
					}
				}
			}
			count--;
			do {
				script_index++;
				count++;
				c = script_array[script_index];
			} while ((count < numChars)
					&& ((c == ' ') || (c == '\t')));
			return new BackSlashResult(' ', script_index, count);
		}
		case 0: {
			return new BackSlashResult('\\', script_index + 1, count);
		}
		default: {

			
			if ((c >= '0') && (c <= '7')) {
				// Convert it to an octal number. This implementation is
				// compatible with tcl 8.4+ - characters 8 and 9 are not allowed.

				result = c - '0';
				script_index++;

				getoctal: {
					if (script_index == endIndex) {
						break getoctal;
					}
					c = script_array[script_index];
					if (!((c >= '0') && (c <= '7'))) {
						break getoctal;
					}
					count++;
					result = (result * 8) + (c - '0');
					script_index++;

					if (script_index == endIndex) {
						break getoctal;
					}
					c = script_array[script_index];
					if (!((c >= '0') && (c <= '7'))) {
						break getoctal;
					}
					count++;
					result = (result * 8) + (c - '0');
					script_index++;
				}

				// We force result to be a 8-bit (ASCII) character so
				// that it compatible with Tcl 7.6.

				return new BackSlashResult((char) (result & 0xff),
						script_index, count);
			} else {
				return new BackSlashResult(c, script_index + 1, count);
			}
		}
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_ParseBraces --
	 * 
	 * Given a string in braces such as a Tcl command argument or a string value
	 * in a Tcl expression, this procedure parses the string and returns
	 * information about the parse. No more than numChars characters will be
	 * scanned.
	 * 
	 * Results: The return value is a reference to a TclParse. The index of the
	 * braced string terminating close-brace is returned in the TclParse
	 * structure. If the parse was unsuccessful, then a TclException will be
	 * raised.
	 * 
	 * Side effects: If there is insufficient space in parse to hold all the
	 * information about the command, then additional space is allocated.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static TclParse ParseBraces(Interp interp, // Interpreter to use for error
												// reporting;
			// if null, then no error message is
			// provided.
			char[] script_array, // Script containing the string in braces.
			// The first character must be '{'.
			int script_index, // Script containing the string in braces.
			// The first character must be '{'.
			int numChars, // Total number of characters in string. If < 0,
			// the string consists of all characters up to
			// the first null character.
			TclParse parse,
			// Structure to fill in with information
			// about the string.
			boolean append) // true means append tokens to existing
			// information in parse; false means
			// ignore existing tokens in parse and
			// reinitialize it.
			throws TclException {
		TclToken token;
		int src;
		int startIndex, level, length;
		char cur;
		int type;
		BackSlashResult bs;

		if ((numChars == 0) || (script_array == null)) {
			throw new TclException(interp, "empty script");
		}

		int script_length = script_array.length - 1;

		if (numChars < 0) {
			numChars = script_length - script_index;
		}
		int endIndex = script_index + numChars;
		if (endIndex > script_length) {
			endIndex = script_length;
		}

		if (script_array[script_index] != '{') {
			throw new TclRuntimeError(
					"expected open brace character at script_index");
		}

		if (!append) {
			parse = new TclParse(interp, script_array, endIndex, null, -1);
		}

		src = script_index;
		startIndex = parse.numTokens;

		if (parse.numTokens == parse.tokensAvailable) {
			parse.expandTokenArray(parse.numTokens + 1);
		}
		token = parse.getToken(startIndex);
		token.type = TCL_TOKEN_TEXT;
		token.script_array = script_array;
		token.script_index = src + 1;
		token.numComponents = 0;
		level = 1;
		while (true) {
			for (src++, numChars--; numChars > 0; src++, numChars--) {
				cur = script_array[src];
				type = ((cur > TYPE_MAX) ? TYPE_NORMAL : typeTable[cur]);
				if (type != TYPE_NORMAL) {
					break;
				}
			}
			if (numChars == 0) {
				boolean openBrace = false;

				parse.errorType = TCL_PARSE_MISSING_BRACE;
				parse.termIndex = script_index;
				parse.incomplete = true;

				String msg = "missing close-brace";

				error: {
					if (interp == null) {
						break error; // Skip to exception code
					}

					// Guess if the problem is due to comments by searching
					// the source string for a possible open brace within the
					// context of a comment. Since we aren't performing a
					// full Tcl parse, just look for an open brace preceded
					// by a '<whitespace>#' on the same line.

					for (; src > script_index; src--) {
						switch (script_array[src]) {
						case '{':
							openBrace = true;
							break;
						case '\n':
							openBrace = false;
							break;
						case '#':
							cur = script_array[src - 1];
							if (openBrace && Character.isWhitespace(cur)) {
								msg = msg
										+ ": possible unbalanced brace in comment";
								break error;
							}
							break;
						}
					}
				} // end error block

				parse.release();
				throw new TclException(interp, msg);
			}
			cur = script_array[src];
			switch (cur) {
			case '{':
				level++;
				break;
			case '}':
				if (--level == 0) {

					// Decide if we need to finish emitting a
					// partially-finished token. There are 3 cases:
					// {abc \newline xyz} or {xyz}
					// - finish emitting "xyz" token
					// {abc \newline}
					// - don't emit token after \newline
					// {} - finish emitting zero-sized token
					//
					// The last case ensures that there is a token
					// (even if empty) that describes the braced string.

					if ((src != token.script_index)
							|| (parse.numTokens == startIndex)) {
						token.size = (src - token.script_index);
						parse.numTokens++;
					}
					parse.extra = src + 1;
					return parse;
				}
				break;
			case '\\':
				bs = backslash(script_array, src);
				length = bs.count;
				if ((length > 1) && (script_array[src + 1] == '\n')) {
					// A backslash-newline sequence must be collapsed, even
					// inside braces, so we have to split the word into
					// multiple tokens so that the backslash-newline can be
					// represented explicitly.

					if (numChars == 2) {
						parse.incomplete = true;
					}
					token.size = (src - token.script_index);
					if (token.size != 0) {
						parse.numTokens++;
					}
					if ((parse.numTokens + 1) >= parse.tokensAvailable) {
						parse.expandTokenArray(parse.numTokens + 1);
					}
					token = parse.getToken(parse.numTokens);
					token.type = TCL_TOKEN_BS;
					token.script_array = script_array;
					token.script_index = src;
					token.size = length;
					token.numComponents = 0;
					parse.numTokens++;

					src += length - 1;
					numChars -= length - 1;
					token = parse.getToken(parse.numTokens);
					token.type = TCL_TOKEN_TEXT;
					token.script_array = script_array;
					token.script_index = src + 1;
					token.numComponents = 0;
				} else {
					src += length - 1;
					numChars -= length - 1;
				}
				break;
			}
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_ParseQuotedString -> ParseQuotedString
	 * 
	 * Given a double-quoted string such as a quoted Tcl command argument or a
	 * quoted value in a Tcl expression, this procedure parses the string and
	 * returns information about the parse. No more than numBytes bytes will be
	 * scanned.
	 * 
	 * Results: The return value is a reference to a TclParse. The index of the
	 * quoted string's terminating close-quote is returned in the TclParse
	 * structure. If the parse was unsuccessful, then a TclException will be
	 * raised.
	 * 
	 * Side effects: If there is insufficient space in parse to hold all the
	 * information about the command, then additional space is allocated.
	 * ----------------------------------------------------------------------
	 */

	// Note: This method is ported from Tcl 8.4, it is not used by the parser
	// yet. Currently, this methods is only used in the expr parser.

	static TclParse ParseQuotedString(Interp interp, // Interpreter to use for
														// error reporting;
			// if null, then no error message is
			// provided.
			char[] script_array, // String containing the quoted string.
			// The first character must be '"'.
			int script_index, // Index of first character in script_array.
			int numBytes, // Total number of bytes in string. If < 0,
			// the string consists of all bytes up to
			// the first null character.
			TclParse parse,
			// Structure to fill in with information
			// about the string.
			boolean append) // true means append tokens to existing
			// information in parsePtr; false means
			// ignore existing tokens in parsePtr and
			// reinitialize it.
			throws TclException {
		if ((numBytes == 0) || (script_array == null)) {
			throw new TclException(interp, "empty script");
		}

		int script_length = script_array.length - 1;

		if (numBytes < 0) {
			numBytes = script_length - script_index;
		}
		int endIndex = script_index + numBytes;
		if (endIndex > script_length) {
			endIndex = script_length;
		}

		if (script_array[script_index] != '"') {
			throw new TclRuntimeError(
					"expected quote character at script_index");
		}

		if (!append) {
			parse = new TclParse(interp, script_array, endIndex, null, -1);
		}

		// FIXME: numBytes-1 not passed since field not supported by
		// parseTokens().
		parse = Parser.parseTokens(script_array, script_index + 1, TYPE_QUOTE,
				parse);
		if (parse.result != TCL.OK) {
			// FIXME: look for other locations where parse.release() is not
			// invoked!
			parse.release(); // Tcl_FreeParse()
			throw new TclException(parse.result);
		}
		if (script_array[parse.termIndex] != '"') {
			parse.release(); // Tcl_FreeParse()
			parse.errorType = Parser.TCL_PARSE_MISSING_QUOTE;
			parse.termIndex = script_index;
			parse.incomplete = true;
			throw new TclException(interp, "missing \"");
		}
		parse.extra = parse.termIndex + 1;
		return parse;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TclParseWhiteSpace -> ParseWhiteSpace
	 * 
	 * Scans up to numChars characters starting at script_index, consuming white
	 * space as defined by Tcl's parsing rules.
	 * 
	 * Results: Returns a ParseWhitespaceResult that indicates the number of
	 * characters of type whitespace along with the type of non-whitespace
	 * character that terminated the scan. Will set the incomplete member of the
	 * parse argument if the scanning indicates an incomplete command.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	// FIXME: Create a more general parse result object that is
	// returned by any of the parse operations, also make this
	// result an interp member so that an allocation is not needed
	// for each parse operation.

	static class ParseWhitespaceResult {
		int numScanned;
		int type;
	}

	static ParseWhitespaceResult ParseWhiteSpace(char[] script_array, // Array
																		// of
																		// characters
																		// to
																		// parse.
			int script_index, // First index in character array.
			int numChars, // Max number of characters to scan
			TclParse parse) // Updated if parsing indicates
	// an incomplete command.
	{
		int type = TYPE_NORMAL;
		int p = script_index;
		char c;

		while (true) {
			while (numChars > 0) {
				c = script_array[p];
				type = ((c > TYPE_MAX) ? TYPE_NORMAL : typeTable[c]);
				if ((type & TYPE_SPACE) == 0) {
					break;
				}
				numChars--;
				p++;
			}
			if ((numChars > 0) && ((type & TYPE_SUBS) != 0)) {
				c = script_array[p];
				if (c != '\\') {
					break;
				}
				if (--numChars == 0) {
					break;
				}
				c = script_array[p + 1];
				if (c != '\n') {
					break;
				}
				p += 2;
				if (--numChars == 0) {
					parse.incomplete = true;
					break;
				}
				continue;
			}
			break;
		}
		ParseWhitespaceResult pwsr = new ParseWhitespaceResult();
		pwsr.type = type;
		pwsr.numScanned = (p - script_index);
		return pwsr;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * TclParseHex -> ParseHex
	 * 
	 * Scans a hexadecimal number as a character value. (e.g., for parsing hex
	 * and unicode escape sequences). At most numChars chars are scanned.
	 * 
	 * Results: A ParseHexResult containing the character value and the number
	 * of characters consumed is returned.
	 * 
	 * Notes: Relies on the following properties of the ASCII character set,
	 * with which UTF-8 is compatible:
	 * 
	 * The digits '0' .. '9' and the letters 'A' .. 'Z' and 'a' .. 'z' occupy
	 * consecutive code points, and '0' < 'A' < 'a'.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static class ParseHexResult {
		int result;
		int numScanned;
	}

	static ParseHexResult ParseHex(char[] script_array, // Array of characters
														// to parse.
			int script_index, // First index in character array.
			int numChars) // Max number of characters to scan
	{
		int result = 0;
		char digit;
		int p = script_index;

		for (; numChars > 0; numChars--) {
			digit = script_array[p];

			if (((digit >= '0') && (digit <= '9'))
					|| ((digit >= 'A') && (digit <= 'F'))
					|| ((digit >= 'a') && (digit <= 'f'))) {
				// This is a hex character
			} else {
				break;
			}

			p++;
			result <<= 4;

			if (digit >= 'a') {
				result |= (10 + digit - 'a');
			} else if (digit >= 'A') {
				result |= (10 + digit - 'A');
			} else {
				result |= (digit - '0');
			}
		}

		ParseHexResult pr = new ParseHexResult();
		pr.result = result;
		pr.numScanned = p - script_index;
		return pr;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * charType --
	 * 
	 * Looks into the typeTable to determine the character type. Possible types
	 * are TYPE_NORMAL, TYPE_SPACE, TYPE_COMMAND_END, TYPE_SUBS, etc. See below
	 * for more detail.
	 * 
	 * Results: A char that specifies the character type
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static char charType(char c) {
		return ((c > TYPE_MAX) ? TYPE_NORMAL : typeTable[c]);
	}

	// The following table provides parsing information about each possible
	// character.
	//
	// The method charType is used to index into the table and return
	// information about its character argument. The following return
	// values are defined.
	//
	// TYPE_NORMAL - All characters that don't have special significance
	// to the Tcl parser.
	// TYPE_SPACE - The character is a whitespace character other
	// than newline.
	// TYPE_COMMAND_END - Character is newline or semicolon.
	// TYPE_SUBS - Character begins a substitution or has other
	// special meaning in parseTokens: backslash, dollar
	// sign, open bracket, or null.
	// TYPE_QUOTE - Character is a double quote.
	// TYPE_CLOSE_PAREN - Character is a right parenthesis.
	// TYPE_CLOSE_BRACK - Character is a right square bracket.
	// TYPE_BRACE - Character is a curly brace (either left or right).

	static final char TYPE_NORMAL = 0;
	static final char TYPE_SPACE = 0x1;
	static final char TYPE_COMMAND_END = 0x2;
	static final char TYPE_SUBS = 0x4;
	static final char TYPE_QUOTE = 0x8;
	static final char TYPE_CLOSE_PAREN = 0x10;
	static final char TYPE_CLOSE_BRACK = 0x20;
	static final char TYPE_BRACE = 0x40;

	// This is the largest value in the type table. If a
	// char value is larger then the char type is TYPE_NORMAL.
	// Lookup -> ((c > TYPE_MAX) ? TYPE_NORMAL : typeTable[c])

	static final char TYPE_MAX = 127;

	static char[] typeTable = {
			// Character values, from 0-127:

			TYPE_SUBS, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_SPACE,
			TYPE_COMMAND_END, TYPE_SPACE, TYPE_SPACE, TYPE_SPACE, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_SPACE, TYPE_NORMAL, TYPE_QUOTE,
			TYPE_NORMAL, TYPE_SUBS, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_CLOSE_PAREN, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_COMMAND_END, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_SUBS, TYPE_SUBS,
			TYPE_CLOSE_BRACK, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL, TYPE_NORMAL,
			TYPE_NORMAL, TYPE_BRACE, TYPE_NORMAL, TYPE_BRACE, TYPE_NORMAL,
			TYPE_NORMAL, };

	// Type values defined for TclToken structures. These values are
	// defined as mask bits so that it's easy to check for collections of
	// types.
	//
	// TCL_TOKEN_WORD - The token describes one word of a command,
	// from the first non-blank character of
	// the word (which may be " or {) up to but
	// not including the space, semicolon, or
	// bracket that terminates the word.
	// NumComponents counts the total number of
	// sub-tokens that make up the word. This
	// includes, for example, sub-tokens of
	// TCL_TOKEN_VARIABLE tokens.
	// TCL_TOKEN_SIMPLE_WORD - This token is just like TCL_TOKEN_WORD
	// except that the word is guaranteed to
	// consist of a single TCL_TOKEN_TEXT
	// sub-token.
	// TCL_TOKEN_TEXT - The token describes a range of literal
	// text that is part of a word.
	// NumComponents is always 0.
	// TCL_TOKEN_BS - The token describes a backslash sequence
	// that must be collapsed. NumComponents
	// is always 0.
	// TCL_TOKEN_COMMAND - The token describes a command whose result
	// must be substituted into the word. The
	// token includes the enclosing brackets.
	// NumComponents is always 0.
	// TCL_TOKEN_VARIABLE - The token describes a variable
	// substitution, including the dollar sign,
	// variable name, and array index (if there
	// is one) up through the right
	// parentheses. NumComponents tells how
	// many additional tokens follow to
	// represent the variable name. The first
	// token will be a TCL_TOKEN_TEXT token
	// that describes the variable name. If
	// the variable is an array reference then
	// there will be one or more additional
	// tokens, of type TCL_TOKEN_TEXT,
	// TCL_TOKEN_BS, TCL_TOKEN_COMMAND, and
	// TCL_TOKEN_VARIABLE, that describe the
	// array index; numComponents counts the
	// total number of nested tokens that make
	// up the variable reference, including
	// sub-tokens of TCL_TOKEN_VARIABLE tokens.
	// TCL_TOKEN_SUB_EXPR - The token describes one subexpression of a
	// expression, from the first non-blank
	// character of the subexpression up to but not
	// including the space, brace, or bracket
	// that terminates the subexpression.
	// NumComponents counts the total number of
	// following subtokens that make up the
	// subexpression; this includes all subtokens
	// for any nested TCL_TOKEN_SUB_EXPR tokens.
	// For example, a numeric value used as a
	// primitive operand is described by a
	// TCL_TOKEN_SUB_EXPR token followed by a
	// TCL_TOKEN_TEXT token. A binary subexpression
	// is described by a TCL_TOKEN_SUB_EXPR token
	// followed by the TCL_TOKEN_OPERATOR token
	// for the operator, then TCL_TOKEN_SUB_EXPR
	// tokens for the left then the right operands.
	// TCL_TOKEN_OPERATOR - The token describes one expression operator.
	// An operator might be the name of a math
	// function such as "abs". A TCL_TOKEN_OPERATOR
	// token is always preceeded by one
	// TCL_TOKEN_SUB_EXPR token for the operator's
	// subexpression, and is followed by zero or
	// more TCL_TOKEN_SUB_EXPR tokens for the
	// operator's operands. NumComponents is
	// always 0.

	static final int TCL_TOKEN_WORD = 1;
	static final int TCL_TOKEN_SIMPLE_WORD = 2;
	static final int TCL_TOKEN_TEXT = 4;
	static final int TCL_TOKEN_BS = 8;
	static final int TCL_TOKEN_COMMAND = 16;
	static final int TCL_TOKEN_VARIABLE = 32;
	static final int TCL_TOKEN_SUB_EXPR = 64;
	static final int TCL_TOKEN_OPERATOR = 128;

	// Parsing error types. On any parsing error, one of these values
	// will be stored in the error field of the TclParse class.

	static final int TCL_PARSE_SUCCESS = 0;
	static final int TCL_PARSE_QUOTE_EXTRA = 1;
	static final int TCL_PARSE_BRACE_EXTRA = 2;
	static final int TCL_PARSE_MISSING_BRACE = 3;
	static final int TCL_PARSE_MISSING_BRACKET = 4;
	static final int TCL_PARSE_MISSING_PAREN = 5;
	static final int TCL_PARSE_MISSING_QUOTE = 6;
	static final int TCL_PARSE_MISSING_VAR_BRACE = 7;
	static final int TCL_PARSE_SYNTAX = 8;
	static final int TCL_PARSE_BAD_NUMBER = 9;

	// Note: Most of the variables below will not be used until the
	// Compilier is implemented, but are left for consistency.

	// A structure of the following type is filled in by parseCommand.
	// It describes a single command parsed from an input string.

	// evalFlag bits for Interp structures:
	//
	// TCL_BRACKET_TERM 1 means that the current script is terminated by
	// a close bracket rather than the end of the string.
	// TCL_ALLOW_EXCEPTIONS 1 means it's OK for the script to terminate with
	// a code other than TCL_OK or TCL_ERROR; 0 means
	// codes other than these should be turned into errors.

	public static final int TCL_BRACKET_TERM = 1;
	static final int TCL_ALLOW_EXCEPTIONS = 4;

	// Flag bits for Interp structures:
	//
	// DELETED: Non-zero means the interpreter has been deleted:
	// don't process any more commands for it, and destroy
	// the structure as soon as all nested invocations of
	// Tcl_Eval are done.
	// ERR_IN_PROGRESS: Non-zero means an error unwind is already in
	// progress. Zero means a command proc has been
	// invoked since last error occured.
	// ERR_ALREADY_LOGGED: Non-zero means information has already been logged
	// in $errorInfo for the current Tcl_Eval instance,
	// so Tcl_Eval needn't log it (used to implement the
	// "error message log" command).
	// ERROR_CODE_SET: Non-zero means that Tcl_SetErrorCode has been
	// called to record information for the current
	// error. Zero means Tcl_Eval must clear the
	// errorCode variable if an error is returned.
	// EXPR_INITIALIZED: Non-zero means initialization specific to
	// expressions has been carried out.
	// DONT_COMPILE_CMDS_INLINE: Non-zero means that the bytecode compiler
	// should not compile any commands into an inline
	// sequence of instructions. This is set 1, for
	// example, when command traces are requested.
	// RAND_SEED_INITIALIZED: Non-zero means that the randSeed value of the
	// interp has not be initialized. This is set 1
	// when we first use the rand() or srand() functions.
	// SAFE_INTERP: Non zero means that the current interp is a
	// safe interp (ie it has only the safe commands
	// installed, less priviledge than a regular interp).
	// USE_EVAL_DIRECT: Non-zero means don't use the compiler or byte-code
	// interpreter; instead, have Tcl_EvalObj call
	// Tcl_EvalDirect. Used primarily for testing the
	// new parser.

	static final int DELETED = 1;
	static final int ERR_IN_PROGRESS = 2;
	static final int ERR_ALREADY_LOGGED = 4;
	static final int ERROR_CODE_SET = 8;
	static final int EXPR_INITIALIZED = 0x10;
	static final int DONT_COMPILE_CMDS_INLINE = 0x20;
	static final int RAND_SEED_INITIALIZED = 0x40;
	static final int SAFE_INTERP = 0x80;
	static final int USE_EVAL_DIRECT = 0x100;

	// These are private read only values that are used by the parser
	// class to implement a TclObject[] cache

	// Max size of array to cache (1..N-1)
	private static final int OBJV_CACHE_MAX = 11;

	// The number of array to cache for each size
	// for example if the number of 3 elements is set to 5
	// an array of 5 TclObject[] objects
	// which will each be 3 elements long

	private static final int[] OBJV_CACHE_SIZES = { 0, 12, 12, 10, 6, 4, 4, 4,
			2, 1, 1 };

	// private static final int[] OBJV_CACHE_HITS = {0,0,0,0,0,0,0,0,0,0,0};
	// private static final int[] OBJV_CACHE_MISSES = {0,0,0,0,0,0,0,0,0,0,0};

	static void init(Interp interp) {
		// System.out.println("called Parser.init()");

		TclObject[][][] OBJV = new TclObject[OBJV_CACHE_MAX][][];
		int[] USED = new int[OBJV_CACHE_MAX];

		int i, j, size;

		for (i = 0; i < OBJV_CACHE_MAX; i++) {
			size = OBJV_CACHE_SIZES[i];
			// System.out.println("size " + i + " has " + size +
			// " cache blocks");
			OBJV[i] = new TclObject[size][];
			USED[i] = 0;
			for (j = 0; j < size; j++) {
				// Java arrays are allocated with all null values
				OBJV[i][j] = new TclObject[i];
			}
		}

		interp.parserObjv = OBJV;
		interp.parserObjvUsed = USED;
	}

	// Get a TclObject[] array of a given size. The array
	// is always filled with null values.

	public static TclObject[] grabObjv(final Interp interp, final int size) {
		// Get number of used markers for this size
		int OPEN;

		if ((size < OBJV_CACHE_MAX)
				&& ((OPEN = interp.parserObjvUsed[size]) < OBJV_CACHE_SIZES[size])) {
			// Found an open cache slot
			if (false) {
				// System.out.println("cache hit for objv of size " + size);
				// OBJV_CACHE_HITS[i] = OBJV_CACHE_HITS[i] + 1;
			}
			interp.parserObjvUsed[size] += 1;
			return interp.parserObjv[size][OPEN];
		} else {
			// Did not find a free cache array of this size
			if (false) {
				if (size >= OBJV_CACHE_MAX) {
					// System.out.println("cache allocate for big objv of size "
					// + size);
				} else {
					// System.out.println("cache miss for objv of size " +
					// size);
					// OBJV_CACHE_MISS[i] = OBJV_CACHE_MISS[i] + 1;
				}
			}
			return new TclObject[size];
		}

	}

	// Return a TclObject[] array of a given size to the cache.
	// The size argument is the length of the array, it is never zero.

	public static void releaseObjv(final Interp interp, final TclObject[] objv,
			final int size) {
		if (size < OBJV_CACHE_MAX) {
			int OPEN = interp.parserObjvUsed[size];

			if (OPEN > 0) {
				OPEN--;
				interp.parserObjvUsed[size] = OPEN;
				// Optimize nulling out of array, the
				// most common cases are handled here.
				switch (size) {
				case 1:
					objv[0] = null;
					break;
				case 2:
					objv[0] = null;
					objv[1] = null;
					break;
				case 3:
					objv[0] = null;
					objv[1] = null;
					objv[2] = null;
					break;
				case 4:
					objv[0] = null;
					objv[1] = null;
					objv[2] = null;
					objv[3] = null;
					break;
				case 5:
					objv[0] = null;
					objv[1] = null;
					objv[2] = null;
					objv[3] = null;
					objv[4] = null;
					break;
				default:
					Arrays.fill(objv, null);
					break;
				}
				interp.parserObjv[size][OPEN] = objv;
			}
		}
	}

	// Raise an infinite loop TclException

	static void infiniteLoopException(Interp interp) throws TclException {
		throw new TclException(interp,
				"too many nested evaluations (infinite loop?)");
	}

} // end class Parser
