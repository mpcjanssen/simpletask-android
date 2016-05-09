/* 
 * tclParseExpr.c -> ParseExpr.java
 *
 *	This file contains procedures that parse Tcl expressions. They
 *	do so in a general-purpose fashion that can be used for many
 *	different purposes, including compilation, direct execution,
 *	code analysis, etc.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 * Copyright (c) 1998-2000 by Scriptics Corporation.
 * Copyright (c) 2005 One Moon Scientific, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id: ParseExpr.java,v 1.6 2006/05/22 21:23:35 mdejong Exp $
 */

package tcl.lang;

class ParseExpr {

	// Definitions of the different lexemes that appear in expressions. The
	// order of these must match the corresponding entries in the
	// operatorStrings array below.

	static final int LITERAL = 0;
	static final int FUNC_NAME = 1;
	static final int OPEN_BRACKET = 2;
	static final int OPEN_BRACE = 3;
	static final int OPEN_PAREN = 4;
	static final int CLOSE_PAREN = 5;
	static final int DOLLAR = 6;
	static final int QUOTE = 7;
	static final int COMMA = 8;
	static final int END = 9;
	static final int UNKNOWN = 10;
	static final int UNKNOWN_CHAR = 11;

	// Binary operators:

	static final int MULT = 12;
	static final int DIVIDE = 13;
	static final int MOD = 14;
	static final int PLUS = 15;
	static final int MINUS = 16;
	static final int LEFT_SHIFT = 17;
	static final int RIGHT_SHIFT = 18;
	static final int LESS = 19;
	static final int GREATER = 20;
	static final int LEQ = 21;
	static final int GEQ = 22;
	static final int EQUAL = 23;
	static final int NEQ = 24;
	static final int BIT_AND = 25;
	static final int BIT_XOR = 26;
	static final int BIT_OR = 27;
	static final int AND = 28;
	static final int OR = 29;
	static final int QUESTY = 30;
	static final int COLON = 31;

	// Unary operators. Unary minus and plus are represented by the (binary)
	// lexemes MINUS and PLUS.

	static final int NOT = 32;
	static final int BIT_NOT = 33;

	static final int STREQ = 34;
	static final int STRNEQ = 35;

	// 8.5 ni and in
	public static final int IN = 36;
	public static final int NI = 37;

	// Mapping from lexemes to strings; used for debugging messages. These
	// entries must match the order and number of the lexeme definitions above.

	static String lexemeStrings[] = { "LITERAL", "FUNCNAME", "[", "{", "(",
			")", "$", "\"", ",", "END", "UNKNOWN", "*", "/", "%", "+", "-",
			"<<", ">>", "<", ">", "<=", ">=", "==", "!=", "&", "^", "|", "&&",
			"||", "?", ":", "!", "~", "eq", "ne", "in", "ni",};

	// The ParseInfo structure holds state while parsing an expression.
	// A pointer to an ParseInfo record is passed among the routines in
	// this module.

	static class ParseInfo {
		TclParse parseObj; // Object to fill in with
		// information about the expression.
		int lexeme; // Type of last lexeme scanned in expr.
		// See below for definitions. Corresponds to
		// size characters beginning at start.
		int start; // First character in lexeme.
		int size; // Number of chars in lexeme.
		int next; // Position of the next character to be
		// scanned in the expression string.
		int prevEnd; // Position of the character just after the
		// last one in the previous lexeme. Used to
		// compute size of subexpression tokens.
		char[] originalExpr; // When combined with originalExprStart, these
		// values provide the orignial script info
		// passed to Tcl_ParseExpr.
		int originalExprStart; // Index of original start_index in the array,
		int originalExprSize; // Number of chars in original expr
		int lastChar; // Index of last character of expr.

		ParseInfo() {
		}

		ParseInfo(TclParse parseObj, char[] script_array, int script_index,
				int length) {
			this.parseObj = parseObj;
			lexeme = UNKNOWN;
			originalExpr = script_array;
			originalExprStart = script_index;
			start = -1;
			originalExprSize = length;
			size = length;
			next = script_index;
			prevEnd = script_index;
			lastChar = script_index + length;
		}

		// Return the original expression as a string. The start and size fields
		// of a ParseInfo struct can be changed while parsing, so use special
		// fields to get the original expression.

		String getOriginalExpr() {
			return new String(originalExpr, originalExprStart, originalExprSize);
		}

		// Return a copy of this ParseInfo object.

		ParseInfo duplicate() {
			ParseInfo dup = new ParseInfo();
			dup.parseObj = this.parseObj;
			dup.lexeme = this.lexeme;
			dup.start = this.start;
			dup.size = this.size;
			dup.next = this.next;
			dup.prevEnd = this.prevEnd;
			dup.originalExpr = this.originalExpr;
			dup.originalExprStart = this.originalExprStart;
			dup.originalExprSize = this.originalExprSize;
			dup.lastChar = this.lastChar;
			return dup;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * Tcl_ParseExpr -> parseExpr
	 * 
	 * Given a string, this procedure parses the first Tcl expression in the
	 * string and returns information about the structure of the expression.
	 * This procedure is the top-level interface to the the expression parsing
	 * module.
	 * 
	 * Results: The return value is Tcl.OK if the command was parsed
	 * successfully and TCL_ERROR otherwise. If an error occurs and interp isn't
	 * NULL then an error message is left in its result. On a successful return,
	 * parseObj is filled in with information about the expression that was
	 * parsed.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the expression, then additional space is malloc-ed. If
	 * the procedure returns Tcl.OK then the caller must eventually invoke
	 * Tcl_FreeParse to release any additional space that was allocated.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static TclParse parseExpr(Interp interp, // Used for error reporting.
			char[] script_array, // References the script and contains an
			int script_index, // index to the next character to parse.
			int numChars) // Number of characters in script. If < 0, the
	// script consists of all characters up to the
	// first null character.
	{
		int code;
		char savedChar;
		ParseInfo info;
		String fileName = "unknown";
		int lineNum = 0;

		int script_length = script_array.length - 1;

		if (numChars < 0) {
			numChars = script_length - script_index;
		}
		int endIndex = script_index + numChars;
		if (endIndex > script_length) {
			endIndex = script_length;
		}

		TclParse parse = new TclParse(interp, script_array, endIndex, fileName,
				lineNum);

		// Initialize the ParseInfo structure that holds state while parsing
		// the expression.

		info = new ParseInfo(parse, script_array, script_index, numChars);

		try {
			// Get the first lexeme then parse the expression.

			GetLexeme(interp, info);

			// System.out.println("after lex "+new
			// String(info.originalExpr)+"  "+lexemeStrings[info.lexeme]);
			ParseCondExpr(interp, info);

			if (info.lexeme != END) {
				LogSyntaxError(info, "extra tokens at end of expression");
			}
		} catch (TclException te) {
			parse.result = TCL.ERROR;
			return parse;
		}

		if (parse.result != TCL.OK) {
			throw new TclRuntimeError(
					"non TCL.OK parse result in parseExpr(): "
							+ " TclException should have been raised");
		}

		parse.result = TCL.OK;
		return parse;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseCondExpr --
	 * 
	 * This procedure parses a Tcl conditional expression: condExpr ::= lorExpr
	 * ['?' condExpr ':' condExpr]
	 * 
	 * Note that this is the topmost recursive-descent parsing routine used by
	 * TclParseExpr to parse expressions. This avoids an extra procedure call
	 * since such a procedure would only return the result of calling
	 * ParseCondExpr. Other recursive-descent procedures that need to parse
	 * complete expressions also call ParseCondExpr.
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseCondExpr(Interp interp, ParseInfo info)
			throws TclException
	// info Holds the parse state for the expression being parsed.
	{
		TclParse parseObj = info.parseObj;
		TclToken token, firstToken, condToken;
		int firstIndex, numToMove, code;
		int srcStart;

		// HERE("condExpr", 1);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseLorExpr(interp, info);

		if (info.lexeme == QUESTY) {
			// Emit two tokens: one TCL_TOKEN_SUB_EXPR token for the entire
			// conditional expression, and a TCL_TOKEN_OPERATOR token for
			// the "?" operator. Note that these two tokens must be inserted
			// before the LOR operand tokens generated above.

			parseObj.insertInTokenArray(firstIndex, 2);
			parseObj.numTokens += 2;

			token = parseObj.getToken(firstIndex);
			token.type = Parser.TCL_TOKEN_SUB_EXPR;
			token.script_array = info.originalExpr;
			token.script_index = srcStart;
			token.size = 0;

			token = parseObj.getToken(firstIndex + 1);
			token.type = Parser.TCL_TOKEN_OPERATOR;
			token.script_array = info.originalExpr;
			token.script_index = info.start;
			token.size = 1;
			token.numComponents = 0;

			// Skip over the '?'.

			GetLexeme(interp, info);

			// Parse the "then" expression.

			ParseCondExpr(interp, info);
			if (info.lexeme != COLON) {
				LogSyntaxError(info, "missing colon from ternary conditional");
			}
			GetLexeme(interp, info); // skip over the ':'

			// Parse the "else" expression.

			ParseCondExpr(interp, info);

			// Now set the size-related fields in the '?' subexpression token.

			condToken = parseObj.getToken(firstIndex);
			condToken.script_array = info.originalExpr;
			condToken.size = (info.prevEnd - srcStart);
			condToken.numComponents = parseObj.numTokens - (firstIndex + 1);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseLorExpr --
	 * 
	 * This procedure parses a Tcl logical or expression: lorExpr ::= landExpr
	 * {'||' landExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseLorExpr(Interp interp, ParseInfo info) throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, code;
		int srcStart;
		int operator;

		// HERE("lorExpr", 2);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseLandExpr(interp, info);

		while (info.lexeme == OR) {
			operator = info.start;
			GetLexeme(interp, info); // skip over the '||'
			ParseLandExpr(interp, info);

			// Generate tokens for the LOR subexpression and the '||' operator.

			PrependSubExprTokens(operator, 2, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseLandExpr --
	 * 
	 * This procedure parses a Tcl logical and expression: landExpr ::=
	 * bitOrExpr {'&&' bitOrExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseLandExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, code;
		int srcStart, operator;

		// HERE("landExpr", 3);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseBitOrExpr(interp, info);

		while (info.lexeme == AND) {
			operator = info.start;
			GetLexeme(interp, info); // skip over the '&&'
			ParseBitOrExpr(interp, info);

			// Generate tokens for the LAND subexpression and the '&&' operator.

			PrependSubExprTokens(operator, 2, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseBitOrExpr --
	 * 
	 * This procedure parses a Tcl bitwise or expression: bitOrExpr ::=
	 * bitXorExpr {'|' bitXorExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseBitOrExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, code;
		int srcStart, operator;

		// HERE("bitOrExpr", 4);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseBitXorExpr(interp, info);

		while (info.lexeme == BIT_OR) {
			operator = info.start;
			GetLexeme(interp, info); // skip over the '|'

			ParseBitXorExpr(interp, info);

			// Generate tokens for the BITOR subexpression and the '|' operator.

			PrependSubExprTokens(operator, 1, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseBitXorExpr --
	 * 
	 * This procedure parses a Tcl bitwise exclusive or expression: bitXorExpr
	 * ::= bitAndExpr {'^' bitAndExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseBitXorExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, code;
		int srcStart, operator;

		// HERE("bitXorExpr", 5);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseBitAndExpr(interp, info);

		while (info.lexeme == BIT_XOR) {
			operator = info.start;
			GetLexeme(interp, info); // skip over the '^'

			ParseBitAndExpr(interp, info);

			// Generate tokens for the XOR subexpression and the '^' operator.

			PrependSubExprTokens(operator, 1, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseBitAndExpr --
	 * 
	 * This procedure parses a Tcl bitwise and expression: bitAndExpr ::=
	 * equalityExpr {'&' equalityExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseBitAndExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, code;
		int srcStart, operator;

		// HERE("bitAndExpr", 6);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseEqualityExpr(interp, info);

		while (info.lexeme == BIT_AND) {
			operator = info.start;
			GetLexeme(interp, info); // skip over the '&'
			ParseEqualityExpr(interp, info);

			// Generate tokens for the BITAND subexpression and '&' operator.

			PrependSubExprTokens(operator, 1, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseEqualityExpr --
	 * 
	 * This procedure parses a Tcl equality (inequality) expression:
	 * equalityExpr ::= relationalExpr {('==' | '!=' | 'ne' | 'eq')
	 * relationalExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseEqualityExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, lexeme, code;
		int srcStart, operator;

		// HERE("equalityExpr", 7);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseRelationalExpr(interp, info);

		lexeme = info.lexeme;
		while ((lexeme == EQUAL) || (lexeme == NEQ) || (lexeme == STREQ)
				|| (lexeme == STRNEQ) || (lexeme == IN) || (lexeme == NI)) {
			operator = info.start;
			GetLexeme(interp, info); // skip over ==, !=, 'eq' or 'ne'
			ParseRelationalExpr(interp, info);

			// Generate tokens for the subexpression and '==', '!=', 'eq' or
			// 'ne'
			// operator.

			PrependSubExprTokens(operator, 2, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
			lexeme = info.lexeme;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseRelationalExpr --
	 * 
	 * This procedure parses a Tcl relational expression: relationalExpr ::=
	 * shiftExpr {('<' | '>' | '<=' | '>=') shiftExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseRelationalExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, lexeme, operatorSize, code;
		int srcStart, operator;

		// HERE("relationalExpr", 8);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseShiftExpr(interp, info);

		lexeme = info.lexeme;
		while ((lexeme == LESS) || (lexeme == GREATER) || (lexeme == LEQ)
				|| (lexeme == GEQ)) {
			operator = info.start;
			if ((lexeme == LEQ) || (lexeme == GEQ)) {
				operatorSize = 2;
			} else {
				operatorSize = 1;
			}
			GetLexeme(interp, info); // skip over the operator
			ParseShiftExpr(interp, info);

			// Generate tokens for the subexpression and the operator.

			PrependSubExprTokens(operator, operatorSize, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
			lexeme = info.lexeme;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseShiftExpr --
	 * 
	 * This procedure parses a Tcl shift expression: shiftExpr ::= addExpr
	 * {('<<' | '>>') addExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseShiftExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, lexeme, code;
		int srcStart, operator;

		// HERE("shiftExpr", 9);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseAddExpr(interp, info);

		lexeme = info.lexeme;
		while ((lexeme == LEFT_SHIFT) || (lexeme == RIGHT_SHIFT)) {
			operator = info.start;
			GetLexeme(interp, info); // skip over << or >>
			ParseAddExpr(interp, info);

			// Generate tokens for the subexpression and '<<' or '>>' operator.

			PrependSubExprTokens(operator, 2, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
			lexeme = info.lexeme;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseAddExpr --
	 * 
	 * This procedure parses a Tcl addition expression: addExpr ::= multiplyExpr
	 * {('+' | '-') multiplyExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseAddExpr(Interp interp, ParseInfo info) throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, lexeme, code;
		int srcStart, operator;

		// HERE("addExpr", 10);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;
		// System.out.println("parse adda "+info.start+" "+info.size);

		ParseMultiplyExpr(interp, info);

		lexeme = info.lexeme;
		// System.out.println("parse add "+info.start);
		while ((lexeme == PLUS) || (lexeme == MINUS)) {
			// System.out.println("add while");
			operator = info.start;
			GetLexeme(interp, info); // skip over + or -
			// System.out.println("after getlex "+info.start+" "+info.size);
			ParseMultiplyExpr(interp, info);
			// System.out.println("parse after mult "+info.start);

			// Generate tokens for the subexpression and '+' or '-' operator.

			PrependSubExprTokens(operator, 1, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
			lexeme = info.lexeme;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseMultiplyExpr --
	 * 
	 * This procedure parses a Tcl multiply expression: multiplyExpr ::=
	 * unaryExpr {('*' | '/' | '%') unaryExpr}
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseMultiplyExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, lexeme, code;
		int srcStart, operator;

		// HERE("multiplyExpr", 11);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		ParseUnaryExpr(interp, info);

		lexeme = info.lexeme;
		while ((lexeme == MULT) || (lexeme == DIVIDE) || (lexeme == MOD)) {
			operator = info.start;
			GetLexeme(interp, info); // skip over * or / or %
			ParseUnaryExpr(interp, info);

			// Generate tokens for the subexpression and * or / or % operator.

			PrependSubExprTokens(operator, 1, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
			lexeme = info.lexeme;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseUnaryExpr --
	 * 
	 * This procedure parses a Tcl unary expression: unaryExpr ::= ('+' | '-' |
	 * '~' | '!') unaryExpr | primaryExpr
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParseUnaryExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		int firstIndex, lexeme, code;
		int srcStart, operator;

		// HERE("unaryExpr", 12);
		srcStart = info.start;
		firstIndex = parseObj.numTokens;

		lexeme = info.lexeme;
		if ((lexeme == PLUS) || (lexeme == MINUS) || (lexeme == BIT_NOT)
				|| (lexeme == NOT)) {
			operator = info.start;
			GetLexeme(interp, info); // skip over the unary operator
			// System.out.println("after getlex "+info.start+" "+info.size);
			ParseUnaryExpr(interp, info);

			// Generate tokens for the subexpression and the operator.

			PrependSubExprTokens(operator, 1, srcStart,
					(info.prevEnd - srcStart), firstIndex, info);
		} else { // must be a primaryExpr
			ParsePrimaryExpr(interp, info);
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParsePrimaryExpr --
	 * 
	 * This procedure parses a Tcl primary expression: primaryExpr ::= literal |
	 * varReference | quotedString | '[' command ']' | mathFuncCall | '('
	 * condExpr ')'
	 * 
	 * Results: The return value is Tcl.OK on a successful parse and TCL_ERROR
	 * on failure. If TCL_ERROR is returned, then the interpreter's result
	 * contains an error message.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void ParsePrimaryExpr(Interp interp, ParseInfo info)
			throws TclException {
		TclParse parseObj = info.parseObj;
		TclToken token, exprToken;
		TclParse nested;
		int dollar, stringStart, term, src;
		int lexeme, exprIndex, firstIndex, numToMove, code;
		// System.out.println("parse primary "+info.lexeme+" "+info.start+" "+info.size+" "+parseObj.numTokens);
		// System.out.println(info.originalExpr);

		// We simply recurse on parenthesized subexpressions.

		// HERE("primaryExpr", 13);
		lexeme = info.lexeme;
		if (lexeme == OPEN_PAREN) {
			GetLexeme(interp, info); // skip over the '('
			ParseCondExpr(interp, info);
			if (info.lexeme != CLOSE_PAREN) {
				LogSyntaxError(info, "looking for close parenthesis");
			}
			GetLexeme(interp, info); // skip over the ')'
			return;
		}

		// Start a TCL_TOKEN_SUB_EXPR token for the primary.

		if (parseObj.numTokens == parseObj.tokensAvailable) {
			parseObj.expandTokenArray(parseObj.numTokens);
		}
		exprIndex = parseObj.numTokens;
		exprToken = parseObj.getToken(exprIndex);
		exprToken.type = Parser.TCL_TOKEN_SUB_EXPR;
		exprToken.script_array = info.originalExpr;
		exprToken.script_index = info.start;
		parseObj.numTokens++;

		// Process the primary then finish setting the fields of the
		// TCL_TOKEN_SUB_EXPR token. Note that we can't use the pointer now
		// stored in "exprToken" in the code below since the token array
		// might be reallocated.

		firstIndex = parseObj.numTokens;
		switch (lexeme) {
		case LITERAL:
			// Int or double number.

			// tokenizeLiteral:
			if (parseObj.numTokens == parseObj.tokensAvailable) {
				parseObj.expandTokenArray(parseObj.numTokens);
			}
			// System.out.println("literal " + parseObj.numTokens);
			token = parseObj.getToken(parseObj.numTokens);
			token.type = Parser.TCL_TOKEN_TEXT;
			token.script_array = info.originalExpr;
			token.script_index = info.start;
			token.size = info.size;
			info.next = info.start + info.size;
			token.numComponents = 0;
			parseObj.numTokens++;

			exprToken.script_array = info.originalExpr;
			exprToken.size = info.size;
			exprToken.numComponents = 1;
			break;

		case DOLLAR:
			// $var variable reference.

			dollar = (info.next - 1);
			// System.out.println("dollar "+dollar+" "+info.lastChar);
			parseObj = Parser.parseVarName(interp, info.originalExpr, dollar,
					info.lastChar - dollar, parseObj, true);

			if (parseObj.result != TCL.OK) {
				throw new TclException(parseObj.result);
			}

			info.next = dollar + parseObj.getToken(firstIndex).size;

			exprToken = parseObj.getToken(exprIndex);
			exprToken.size = parseObj.getToken(firstIndex).size;
			exprToken.numComponents = parseObj.getToken(firstIndex).numComponents + 1;
			exprToken.script_array = info.originalExpr;
			break;

		case QUOTE:
			// '"' string '"'

			stringStart = info.next;

			// Raises a TclException on error
			parseObj = Parser.ParseQuotedString(interp, info.originalExpr,
					(info.next - 1), (info.lastChar - stringStart), parseObj,
					true);

			term = parseObj.extra;
			info.next = term;

			exprToken = parseObj.getToken(exprIndex);
			exprToken.size = (term - exprToken.script_index);
			exprToken.numComponents = parseObj.numTokens - firstIndex;
			exprToken.script_array = info.originalExpr;

			// If parsing the quoted string resulted in more than one token,
			// insert a TCL_TOKEN_WORD token before them. This indicates that
			// the quoted string represents a concatenation of multiple tokens.

			if (exprToken.numComponents > 1) {
				if (parseObj.numTokens >= parseObj.tokensAvailable) {
					parseObj.expandTokenArray(parseObj.numTokens + 1);
				}
				parseObj.insertInTokenArray(firstIndex, 1);
				parseObj.numTokens++;
				token = parseObj.getToken(firstIndex);

				exprToken = parseObj.getToken(exprIndex);
				exprToken.numComponents++;
				exprToken.script_array = info.originalExpr;

				token.type = Parser.TCL_TOKEN_WORD;
				token.script_array = info.originalExpr;
				token.script_index = exprToken.script_index;
				token.size = exprToken.size;
				token.numComponents = (exprToken.numComponents - 1);
			}
			break;

		case OPEN_BRACKET:
			// '[' command {command} ']'

			if (parseObj.numTokens == parseObj.tokensAvailable) {
				parseObj.expandTokenArray(parseObj.numTokens);
			}
			token = parseObj.getToken(parseObj.numTokens);
			token.type = Parser.TCL_TOKEN_COMMAND;
			token.script_array = info.originalExpr;
			token.script_index = info.start;
			token.numComponents = 0;
			parseObj.numTokens++;

			// Call Tcl_ParseCommand repeatedly to parse the nested command(s)
			// to find their end, then throw away that parse information.

			src = info.next;
			while (true) {
				nested = Parser.parseCommand(interp, info.originalExpr, src,
						parseObj.endIndex - src, parseObj.fileName,
						parseObj.lineNum, true);
				if (nested.result != TCL.OK) {
					parseObj.termIndex = nested.termIndex;
					parseObj.errorType = nested.errorType;
					parseObj.incomplete = nested.incomplete;
					parseObj.result = nested.result;
				}
				src = (nested.commandStart + nested.commandSize);

				// Check for the closing ']' that ends the command substitution.
				// It must have been the last character of the parsed command.

				if ((nested.termIndex < parseObj.endIndex)
						&& (info.originalExpr[nested.termIndex] == ']')
						&& !nested.incomplete) {
					break;
				}
				if (src == parseObj.endIndex) {
					parseObj.termIndex = token.script_index;
					parseObj.incomplete = true;
					parseObj.result = TCL.ERROR;
					throw new TclException(parseObj.interp,
							"missing close-bracket");
				}
			}
			token.size = src - token.script_index;
			info.next = src;

			exprToken = parseObj.getToken(exprIndex);
			exprToken.size = src - token.script_index;
			exprToken.numComponents = 1;
			exprToken.script_array = info.originalExpr;
			break;

		case OPEN_BRACE:
			// '{' string '}'

			parseObj = Parser.ParseBraces(interp, info.originalExpr,
					info.start, (info.lastChar - info.start), parseObj, true);
			term = parseObj.extra;
			info.next = term;

			exprToken = parseObj.getToken(exprIndex);
			exprToken.size = (term - info.start);
			exprToken.numComponents = parseObj.numTokens - firstIndex;
			// exprToken.script_array = info.originalExpr; // Does not appear in
			// C impl

			// If parsing the braced string resulted in more than one token,
			// insert a TCL_TOKEN_WORD token before them. This indicates that
			// the braced string represents a concatenation of multiple tokens.

			if (exprToken.numComponents > 1) {
				if (parseObj.numTokens >= parseObj.tokensAvailable) {
					parseObj.expandTokenArray(parseObj.numTokens + 1);
				}
				parseObj.insertInTokenArray(firstIndex, 1);
				parseObj.numTokens++;
				token = parseObj.getToken(firstIndex);

				exprToken = parseObj.getToken(exprIndex);
				// exprToken.script_array = info.originalExpr; // Does not
				// appear in C impl
				exprToken.numComponents++;

				token.type = Parser.TCL_TOKEN_WORD;
				token.script_array = exprToken.script_array;
				token.script_index = exprToken.script_index;
				token.size = exprToken.size;
				token.numComponents = exprToken.numComponents - 1;
			}
			break;

		case FUNC_NAME:
			// math_func '(' expr {',' expr} ')'

			ParseInfo savedInfo = info.duplicate();

			GetLexeme(interp, info); // skip over function name
			if (info.lexeme != OPEN_PAREN) {
				// StringBuffer functionName;
				TclObject obj = TclString
						.newInstance(new String(savedInfo.originalExpr,
								savedInfo.start, savedInfo.size));

				// Check for boolean literals (true, false, yes, no, on, off)
				obj.preserve();
				try {
					TclBoolean.get(interp, obj);

					// If we get this far, then boolean conversion worked
					info = savedInfo;

					// goto tokenizeLiteral;
					if (parseObj.numTokens == parseObj.tokensAvailable) {
						parseObj.expandTokenArray(parseObj.numTokens);
					}
					// System.out.println("literal " + parseObj.numTokens);
					token = parseObj.getToken(parseObj.numTokens);
					token.type = Parser.TCL_TOKEN_TEXT;
					token.script_array = info.originalExpr;
					token.script_index = info.start;
					token.size = info.size;
					info.next = info.start + info.size;
					token.numComponents = 0;
					parseObj.numTokens++;

					exprToken.script_array = info.originalExpr;
					exprToken.size = info.size;
					exprToken.numComponents = 1;

					break; // out of switch
				} catch (TclException ex) {
					// Do nothing when boolean conversion fails,
					// continue on and raise a syntax error.
				} finally {
					obj.release();
				}

				// FIXME: Implement function name vs var lookup error msg
				LogSyntaxError(info, null);
			}

			if (parseObj.numTokens == parseObj.tokensAvailable) {
				parseObj.expandTokenArray(parseObj.numTokens);
			}
			token = parseObj.getToken(parseObj.numTokens);
			token.type = Parser.TCL_TOKEN_OPERATOR;
			token.script_array = savedInfo.originalExpr;
			token.script_index = savedInfo.start;
			token.size = savedInfo.size;
			token.numComponents = 0;
			parseObj.numTokens++;

			GetLexeme(interp, info); // skip over '('

			while (info.lexeme != CLOSE_PAREN) {
				ParseCondExpr(interp, info);

				if (info.lexeme == COMMA) {
					GetLexeme(interp, info); // skip over ,
				} else if (info.lexeme != CLOSE_PAREN) {
					LogSyntaxError(info,
							"missing close parenthesis at end of function call");
				}
			}

			exprToken = parseObj.getToken(exprIndex);
			exprToken.size = (info.next - exprToken.script_index);
			// System.out.println("exprToken  size "+exprToken.size+" "+info.next+" "+exprToken.script_index);
			exprToken.numComponents = parseObj.numTokens - firstIndex;
			exprToken.script_array = info.originalExpr;
			break;

		case COMMA:
			LogSyntaxError(info, "commas can only separate function arguments");
		case END:
			LogSyntaxError(info, "premature end of expression");
		case UNKNOWN:
			LogSyntaxError(info,
					"single equality character not legal in expressions");
		case UNKNOWN_CHAR:
			LogSyntaxError(info, "character not legal in expressions");
		case QUESTY:
			LogSyntaxError(info, "unexpected ternary 'then' separator");
		case COLON:
			LogSyntaxError(info, "unexpected ternary 'else' separator");
		case CLOSE_PAREN:
			LogSyntaxError(info, "unexpected close parenthesis");

		default:
			String msg = "unexpected operator " + lexemeStrings[lexeme];
			LogSyntaxError(info, msg);
		}

		// Advance to the next lexeme before returning.

		GetLexeme(interp, info);
		parseObj.termIndex = info.next;
		return;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * GetLexeme --
	 * 
	 * Lexical scanner for Tcl expressions: scans a single operator or other
	 * syntactic element from an expression string.
	 * 
	 * Results: Tcl.OK is returned unless an error occurred. In that case a
	 * standard Tcl error code is returned and, if info.parseObj.interp is
	 * non-NULL, the interpreter's result is set to hold an error message.
	 * TCL_ERROR is returned if an integer overflow, or a floating-point
	 * overflow or underflow occurred while reading in a number. If the lexical
	 * analysis is successful, info.lexeme refers to the next symbol in the
	 * expression string, and info.next is advanced past the lexeme. Also, if
	 * the lexeme is a LITERAL or FUNC_NAME, then info.start is set to the first
	 * character of the lexeme; otherwise it is set NULL.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold all the
	 * information about the subexpression, then additional space is malloc-ed..
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void GetLexeme(Interp interp, ParseInfo info) throws TclException
	// info; Holds state needed to parse the expr, including the resulting
	// lexeme.
	{
		int src; // Points to current source char.
		int term; // Points to char terminating a literal.
		double doubleValue; // Value of a scanned double literal.
		char c, c2;
		boolean startsWithDigit;
		int offset, length;
		TclParse parseObj = info.parseObj;
		char ch;
		info.lexeme = UNKNOWN;
		// System.out.println("getlex");

		// Record where the previous lexeme ended. Since we always read one
		// lexeme ahead during parsing, this helps us know the source length of
		// subexpression tokens.

		info.prevEnd = info.next;

		// Scan over leading white space at the start of a lexeme. Note that a
		// backslash-newline is treated as a space.

		src = info.next;
		if (src >= info.lastChar) {
			info.lexeme = END;
			info.next = src;
			return;
		}
		c = info.originalExpr[src];
		// System.out.println(new String(info.originalExpr,src,info.size));
		// FIXME: This code should invoke Parser.ParseWhiteSpace()
		// to handle embedded nulls properly. It is disabled for now.
		// See parseExpr-1.1 in parseRxpr.test for a test case.
		while ((c == ' ') || Character.isWhitespace(c) || (c == '\\')) { // INTL:
																			// ISO
																			// space
			if (c == '\\') {
				if (info.originalExpr[src + 1] == '\n') {
					src += 2;
				} else {
					break; // no longer white space
				}
			} else {
				src++;
			}
			c = info.originalExpr[src];
		}
		parseObj.termIndex = src;
		if (src >= info.lastChar) {
			info.lexeme = END;
			info.next = src;
			return;
		}
		// System.out.println(new String(info.originalExpr,src,info.size));

		// Try to parse the lexeme first as an integer or floating-point
		// number. Don't check for a number if the first character c is
		// "+" or "-". If we did, we might treat a binary operator as unary
		// by mistake, which would eventually cause a syntax error.

		if ((c != '+') && (c != '-')) {
			startsWithDigit = Character.isDigit(c); // INTL: digit
			String s = new String(info.originalExpr, src, info.lastChar - src);
			if (startsWithDigit
					&& Expression.looksLikeInt(s, s.length(), 0, false)) {
				StrtoulResult res = interp.strtoulResult;
				Util.strtoul(s, 0, 0, res);
				if (res.errno == 0) {
					term = src + res.index;
					info.lexeme = LITERAL;
					info.start = src;
					info.size = (term - src);
					info.next = term;
					parseObj.termIndex = term;
					return;
				} else {
					parseObj.errorType = Parser.TCL_PARSE_BAD_NUMBER;
					if (res.errno == TCL.INTEGER_RANGE) {
						Expression.IntegerTooLarge(interp);
					} else {
						throw new TclException(interp, "parse bad number");
					}
				}
			} else if ((length = ParseMaxDoubleLength(info.originalExpr, src,
					info.lastChar)) > 0) {

				// There are length characters that could be a double.
				// Let strtod() tells us for sure.

				s = new String(info.originalExpr, src, length);

				StrtodResult res = interp.strtodResult;
				Util.strtod(s, 0, -1, res);
				if (res.index > 0) {
					if (res.errno != 0) {
						parseObj.errorType = Parser.TCL_PARSE_BAD_NUMBER;
						if (res.errno == TCL.DOUBLE_RANGE) {
							if (res.value != 0) {
								Expression.DoubleTooLarge(interp);
							} else {
								Expression.DoubleTooSmall(interp);
							}
						} else {
							throw new TclException(interp, "parse bad number");
						}
					}

					// string was the start of a valid double, copied
					// from src.

					term = src + res.index;
					info.lexeme = LITERAL;
					info.start = src;
					info.size = (term - src);
					if (info.size > length) {
						info.size = length;
					}
					info.next = src + info.size;
					parseObj.termIndex = info.next;
					return;
				}
			}
		}

		// Not an integer or double literal. Initialize the lexeme's fields
		// assuming the common case of a single character lexeme.

		c = info.originalExpr[src];
		c2 = info.originalExpr[src + 1];
		info.start = src;
		info.size = 1;
		info.next = src + 1;
		parseObj.termIndex = info.next;

		switch (c) {
		case '[':
			info.lexeme = OPEN_BRACKET;
			return;

		case '{':
			info.lexeme = OPEN_BRACE;
			return;

		case '(':
			info.lexeme = OPEN_PAREN;
			return;

		case ')':
			info.lexeme = CLOSE_PAREN;
			return;

		case '$':
			info.lexeme = DOLLAR;
			return;

		case '\"':
			info.lexeme = QUOTE;
			return;

		case ',':
			info.lexeme = COMMA;
			return;

		case '*':
			info.lexeme = MULT;
			return;

		case '/':
			info.lexeme = DIVIDE;
			return;

		case '%':
			info.lexeme = MOD;
			return;

		case '+':
			info.lexeme = PLUS;
			return;

		case '-':
			info.lexeme = MINUS;
			return;

		case '?':
			info.lexeme = QUESTY;
			return;

		case ':':
			info.lexeme = COLON;
			return;

		case '<':
			switch (c2) {
			case '<':
				info.lexeme = LEFT_SHIFT;
				info.size = 2;
				info.next = src + 2;
				break;
			case '=':
				info.lexeme = LEQ;
				info.size = 2;
				info.next = src + 2;
				break;
			default:
				info.lexeme = LESS;
				break;
			}
			parseObj.termIndex = info.next;
			return;

		case '>':
			switch (c2) {
			case '>':
				info.lexeme = RIGHT_SHIFT;
				info.size = 2;
				info.next = src + 2;
				break;
			case '=':
				info.lexeme = GEQ;
				info.size = 2;
				info.next = src + 2;
				break;
			default:
				info.lexeme = GREATER;
				break;
			}
			parseObj.termIndex = info.next;
			return;

		case '=':
			if (c2 == '=') {
				info.lexeme = EQUAL;
				info.size = 2;
				info.next = src + 2;
			} else {
				info.lexeme = UNKNOWN;
			}
			parseObj.termIndex = info.next;
			return;

		case '!':
			if (c2 == '=') {
				info.lexeme = NEQ;
				info.size = 2;
				info.next = src + 2;
			} else {
				info.lexeme = NOT;
			}
			parseObj.termIndex = info.next;
			return;

		case '&':
			if (c2 == '&') {
				info.lexeme = AND;
				info.size = 2;
				info.next = src + 2;
			} else {
				info.lexeme = BIT_AND;
			}
			parseObj.termIndex = info.next;
			return;

		case '^':
			info.lexeme = BIT_XOR;
			return;

		case '|':
			if (c2 == '|') {
				info.lexeme = OR;
				info.size = 2;
				info.next = src + 2;
			} else {
				info.lexeme = BIT_OR;
			}
			parseObj.termIndex = info.next;
			return;

		case '~':
			info.lexeme = BIT_NOT;
			return;

		case 'e':
			if (c2 == 'q') {
				info.lexeme = STREQ;
				info.size = 2;
				info.next = src + 2;
				parseObj.termIndex = info.next;
				return;
			} else {
				checkFuncName(interp, info, src);
				return;
			}

		case 'i':
			if (c2 == 'n') {
				info.lexeme = IN;
				info.size = 2;
				info.next = src + 2;
				parseObj.termIndex = info.next;
				return;
			} else {
				checkFuncName(interp, info, src);
				return;
			}

		case 'n':
			if (c2 == 'e') {
				info.lexeme = STRNEQ;
				info.size = 2;
				info.next = src + 2;
				parseObj.termIndex = info.next;
				return;
			} else if (c2 == 'i') {
				info.lexeme = NI;
				info.size = 2;
				info.next = src + 2;
				parseObj.termIndex = info.next;
				return;
			} else {
				checkFuncName(interp, info, src);
				return;
			}

		default:
			checkFuncName(interp, info, src);
			return;
		}
	}

	static void checkFuncName(Interp interp, ParseInfo info, int src) {
		char c = info.originalExpr[src];
		if (Character.isLetter(c)) { // INTL: ISO only.
			info.lexeme = FUNC_NAME;
			while (Character.isLetterOrDigit(c) || (c == '_')) { // INTL: ISO
																	// only.
				src++;
				c = info.originalExpr[src];
			}
			info.size = (src - info.start);
			info.next = src;
			info.parseObj.termIndex = info.next;
			String s = new String(info.originalExpr, info.start, info.size);

			// Check for boolean literals (true, false, yes, no, on, off)

			c = info.originalExpr[info.start];
			switch (c) {
			case 'f':
				if (info.size == 5 && s.equals("false")) {
					info.lexeme = LITERAL;
					return;
				}
				break;
			case 'n':
				if (info.size == 2 && s.equals("no")) {
					info.lexeme = LITERAL;
					return;
				}
				break;
			case 'o':
				if (info.size == 3 && s.equals("off")) {
					info.lexeme = LITERAL;
					return;
				} else if (info.size == 2 && s.equals("on")) {
					info.lexeme = LITERAL;
					return;
				}
				break;
			case 't':
				if (info.size == 4 && s.equals("true")) {
					info.lexeme = LITERAL;
					return;
				}
				break;
			case 'y':
				if (info.size == 3 && s.equals("yes")) {
					info.lexeme = LITERAL;
					return;
				}
				break;
			}
		} else {
			info.lexeme = UNKNOWN_CHAR;
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * PrependSubExprTokens --
	 * 
	 * This procedure is called after the operands of an subexpression have been
	 * parsed. It generates two tokens: a TCL_TOKEN_SUB_EXPR token for the
	 * subexpression, and a TCL_TOKEN_OPERATOR token for its operator. These two
	 * tokens are inserted before the operand tokens.
	 * 
	 * Results: None.
	 * 
	 * Side effects: If there is insufficient space in parseObj to hold the new
	 * tokens, additional space is malloc-ed.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void PrependSubExprTokens(int op, int opBytes, int src,
			int srcBytes, int firstIndex, ParseInfo info)
	/*
	 * op; Points to first byte of the operator in the source script.
	 */
	/* opBytes; Number of bytes in the operator. */
	/*
	 * src; /* Points to first byte of the subexpression in the source script.
	 */
	/*
	 * srcBytes; Number of bytes in subexpression's source.
	 */
	/*
	 * firstIndex; Index of first token already emitted for operator's first (or
	 * only) operand.
	 */
	/*
	 * info; /* Holds the parse state for the expression being parsed.
	 */
	{
		// System.out.println("prepend "+firstIndex+" "+srcBytes+" "+src);
		TclParse parseObj = info.parseObj;
		TclToken token, firstToken;
		int numToMove;

		if ((parseObj.numTokens + 1) >= parseObj.tokensAvailable) {
			parseObj.expandTokenArray(parseObj.numTokens + 1);
		}
		parseObj.insertInTokenArray(firstIndex, 2);
		parseObj.numTokens += 2;

		token = parseObj.getToken(firstIndex);
		token.type = Parser.TCL_TOKEN_SUB_EXPR;
		token.script_index = src;
		token.script_array = info.originalExpr;
		token.size = srcBytes;
		token.numComponents = parseObj.numTokens - (firstIndex + 1);

		token = parseObj.getToken(firstIndex + 1);
		token.type = Parser.TCL_TOKEN_OPERATOR;
		token.script_index = op;
		token.script_array = info.originalExpr;
		token.size = opBytes;
		token.numComponents = 0;
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * LogSyntaxError --
	 * 
	 * This procedure is invoked after an error occurs when parsing an
	 * expression. It sets the interpreter result to an error message describing
	 * the error.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Sets the interpreter result to an error message describing
	 * the expression that was being parsed when the error occurred.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static void LogSyntaxError(ParseInfo info, // Holds the parse state for the
			// expression being parsed.
			String extraInfo) // String to provide extra information
			// about the syntax error.
			throws TclException {
		// int numChars = (info.lastChar - info.originalExprStart);
		String expr = info.getOriginalExpr();
		if (expr.length() > 60) {
			expr = expr.substring(0, 60) + "...";
		}
		StringBuilder msg = new StringBuilder();
		msg.append("syntax error in expression \"");
		msg.append(expr);
		msg.append("\"");

		// Extra info is disabled for now until the parser test cases are
		// updated to
		// match Tcl 8.4 parser error messages.
		// if (extraInfo != null) {
		// msg.append(": ");
		// msg.append(extraInfo);
		// }

		info.parseObj.errorType = Parser.TCL_PARSE_SYNTAX;
		info.parseObj.termIndex = info.start;

		if (info.parseObj.interp != null) {
			info.parseObj.interp.resetResult();
		}
		throw new TclException(info.parseObj.interp, msg.toString());
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * ParseMaxDoubleLength -> ParseMaxDoubleLength
	 * 
	 * Scans a sequence of characters checking that the characters could be in a
	 * string rep of a double.
	 * 
	 * Results: Returns the number of characters starting with string, runing
	 * to, but not including end, all of which could be part of a string rep. of
	 * a double. Only character identity is used, no actual parsing is done.
	 * 
	 * The legal bytes are '0' - '9', 'A' - 'F', 'a' - 'f', '.', '+', '-', 'i',
	 * 'I', 'n', 'N', 'p', 'P', 'x', and 'X'. This covers the values "Inf" and
	 * "Nan" as well as the decimal and hexadecimal representations recognized
	 * by a C99-compliant strtod().
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	static int ParseMaxDoubleLength(char[] script_array, int script_index,
			int end) {
		int p = script_index;
		done: {
			while (p < end) {
				switch (script_array[p]) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'E':
				case 'F':
				case 'I':
				case 'N':
				case 'P':
				case 'X':
				case 'a':
				case 'b':
				case 'c':
				case 'd':
				case 'e':
				case 'f':
				case 'i':
				case 'n':
				case 'p':
				case 'x':
				case '.':
				case '+':
				case '-':
					p++;
					break;
				default:
					break done;
				}
			}
		} // end done block
		return (p - script_index);
	}

} // end class ParseExpr

